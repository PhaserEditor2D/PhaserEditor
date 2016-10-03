/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.wst.jsdt.internal.ui.text.correction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.graphics.Image;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.IBinding;
import org.eclipse.wst.jsdt.core.dom.IFunctionBinding;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.IVariableBinding;
import org.eclipse.wst.jsdt.core.dom.JSdoc;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.Name;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.SingleVariableDeclaration;
import org.eclipse.wst.jsdt.core.dom.TagElement;
import org.eclipse.wst.jsdt.core.dom.TextElement;
import org.eclipse.wst.jsdt.core.dom.Type;
import org.eclipse.wst.jsdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.wst.jsdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.wst.jsdt.core.dom.rewrite.ListRewrite;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.corext.dom.Bindings;
import org.eclipse.wst.jsdt.internal.corext.dom.LinkedNodeFinder;
import org.eclipse.wst.jsdt.internal.corext.dom.ScopeAnalyzer;

public class ChangeMethodSignatureProposal extends LinkedCorrectionProposal {

	public static interface ChangeDescription {
	}

	public static class SwapDescription implements ChangeDescription {
		final int index;
		public SwapDescription(int index) {
			this.index= index;
		}
	}

	public static class RemoveDescription implements ChangeDescription {
	}

	static class ModifyDescription implements ChangeDescription {
		public final String name;
		public final ITypeBinding type;
		Type resultingParamType;
		SimpleName[] resultingParamName;
		SimpleName resultingTagArg;

		private ModifyDescription(ITypeBinding type, String name) {
			this.type= type;
			this.name= name;
		}
	}

	public static class EditDescription extends ModifyDescription {
		String orginalName;

		public EditDescription(ITypeBinding type, String name) {
			super(type, name);
		}
	}

	public static class InsertDescription extends ModifyDescription {
		public InsertDescription(ITypeBinding type, String name) {
			super(type, name);
		}
	}

	private ASTNode fInvocationNode;
	private IFunctionBinding fSenderBinding;
	private ChangeDescription[] fParameterChanges;
	private ChangeDescription[] fExceptionChanges;

	public ChangeMethodSignatureProposal(String label, IJavaScriptUnit targetCU, ASTNode invocationNode, IFunctionBinding binding, ChangeDescription[] paramChanges, ChangeDescription[] exceptionChanges, int relevance, Image image) {
		super(label, targetCU, null, relevance, image);

		Assert.isTrue(binding != null && Bindings.isDeclarationBinding(binding));

		fInvocationNode= invocationNode;
		fSenderBinding= binding;
		fParameterChanges= paramChanges;
		fExceptionChanges= exceptionChanges;
	}

	protected ASTRewrite getRewrite() throws CoreException {
		JavaScriptUnit astRoot= (JavaScriptUnit) fInvocationNode.getRoot();
		ASTNode methodDecl= astRoot.findDeclaringNode(fSenderBinding);
		ASTNode newMethodDecl= null;
		boolean isInDifferentCU;
		if (methodDecl != null) {
			isInDifferentCU= false;
			newMethodDecl= methodDecl;
		} else {
			isInDifferentCU= true;
			astRoot= ASTResolving.createQuickFixAST(getCompilationUnit(), null);
			newMethodDecl= astRoot.findDeclaringNode(fSenderBinding.getKey());
		}
		createImportRewrite(astRoot);
		
		if (newMethodDecl instanceof FunctionDeclaration) {
			FunctionDeclaration decl= (FunctionDeclaration) newMethodDecl;

			ASTRewrite rewrite= ASTRewrite.create(astRoot.getAST());
			if (fParameterChanges != null) {
				modifyParameters(rewrite, decl, isInDifferentCU);
			}
			if (fExceptionChanges != null) {
				modifyExceptions(rewrite, decl);
			}
			return rewrite;
		}
		return null;
	}

	private void modifyParameters(ASTRewrite rewrite, FunctionDeclaration methodDecl, boolean isInDifferentCU) throws CoreException {
		AST ast= methodDecl.getAST();

		ArrayList usedNames= new ArrayList();
		boolean hasCreatedVariables= false;

		IVariableBinding[] declaredFields= fSenderBinding.getDeclaringClass().getDeclaredFields();
		for (int i= 0; i < declaredFields.length; i++) { // avoid to take parameter names that are equal to field names
			usedNames.add(declaredFields[i].getName());
		}

		ImportRewrite imports= getImportRewrite();
		ListRewrite listRewrite= rewrite.getListRewrite(methodDecl, FunctionDeclaration.PARAMETERS_PROPERTY);

		List parameters= methodDecl.parameters(); // old parameters
		int k= 0; // index over the oldParameters

		for (int i= 0; i < fParameterChanges.length; i++) {
			ChangeDescription curr= fParameterChanges[i];

			if (curr == null) {
				SingleVariableDeclaration oldParam= (SingleVariableDeclaration) parameters.get(k);
				usedNames.add(oldParam.getName().getIdentifier());
				k++;
			} else if (curr instanceof InsertDescription) {
				InsertDescription desc= (InsertDescription) curr;
				SingleVariableDeclaration newNode= ast.newSingleVariableDeclaration();
				newNode.setType(imports.addImport(desc.type, ast));
				newNode.setName(ast.newSimpleName("x")); //$NON-NLS-1$

				// remember to set name later
				desc.resultingParamName= new SimpleName[] {newNode.getName()};
				desc.resultingParamType= newNode.getType();
				hasCreatedVariables= true;

				listRewrite.insertAt(newNode, i, null);

				JSdoc javadoc= methodDecl.getJavadoc();
				if (javadoc != null) {
					TagElement newTagElement= ast.newTagElement();
					newTagElement.setTagName(TagElement.TAG_PARAM);
					SimpleName arg= ast.newSimpleName("x"); //$NON-NLS-1$
					newTagElement.fragments().add(arg);
					insertTabStop(rewrite, newTagElement.fragments(), "param_tagcomment" + i); //$NON-NLS-1$
					insertParamTag(rewrite.getListRewrite(javadoc, JSdoc.TAGS_PROPERTY), parameters, k, newTagElement);
					desc.resultingTagArg= arg; // set the name later
				} else {
					desc.resultingTagArg= null;
				}
			} else if (curr instanceof RemoveDescription) {
				SingleVariableDeclaration decl= (SingleVariableDeclaration) parameters.get(k);

				listRewrite.remove(decl, null);
				k++;

				TagElement tagNode= findParamTag(methodDecl, decl);
				if (tagNode != null) {
					rewrite.remove(tagNode, null);
				}
			} else if (curr instanceof EditDescription) {
				EditDescription desc= (EditDescription) curr;

				ITypeBinding newTypeBinding= desc.type;
				SingleVariableDeclaration decl= (SingleVariableDeclaration) parameters.get(k);
				
				if (k == parameters.size() - 1 && i == fParameterChanges.length - 1 && decl.isVarargs() && newTypeBinding.isArray()) {
					newTypeBinding= newTypeBinding.getElementType(); // stick with varargs if it was before
				} else {
					rewrite.set(decl, SingleVariableDeclaration.VARARGS_PROPERTY, Boolean.FALSE, null);
				}
				
				Type newType= imports.addImport(newTypeBinding, ast);
				rewrite.replace(decl.getType(), newType, null);
				rewrite.set(decl, SingleVariableDeclaration.EXTRA_DIMENSIONS_PROPERTY, Integer.valueOf(0), null);
				
				IBinding binding= decl.getName().resolveBinding();
				if (binding != null) {
					SimpleName[] names= LinkedNodeFinder.findByBinding(decl.getRoot(), binding);
					SimpleName[] newNames= new SimpleName[names.length];
					for (int j= 0; j < names.length; j++) {
						SimpleName newName= ast.newSimpleName("x"); //$NON-NLS-1$  // name will be set later
						newNames[j]= newName;
						rewrite.replace(names[j], newName, null); 
						
					}
					desc.resultingParamName= newNames;
				} else {
					SimpleName newName= ast.newSimpleName("x"); //$NON-NLS-1$  // name will be set later
					rewrite.replace(decl.getName(), newName, null); 
					// remember to set name later
					desc.resultingParamName= new SimpleName[] {newName};
				}

				desc.resultingParamType= newType;
				desc.orginalName= decl.getName().getIdentifier();
				hasCreatedVariables= true;

				k++;

				TagElement tagNode= findParamTag(methodDecl, decl);
				if (tagNode != null) {
					List fragments= tagNode.fragments();
					if (!fragments.isEmpty()) {
						SimpleName arg= ast.newSimpleName("x"); //$NON-NLS-1$
						rewrite.replace((ASTNode) fragments.get(0), arg, null);
						desc.resultingTagArg= arg;
					}
				}

			} else if (curr instanceof SwapDescription) {
				SingleVariableDeclaration decl1= (SingleVariableDeclaration) parameters.get(k);
				SingleVariableDeclaration decl2= (SingleVariableDeclaration) parameters.get(((SwapDescription) curr).index);

				rewrite.replace(decl1, rewrite.createCopyTarget(decl2), null);
				rewrite.replace(decl2, rewrite.createCopyTarget(decl1), null);

				usedNames.add(decl1.getName().getIdentifier());
				k++;

				TagElement tagNode1= findParamTag(methodDecl, decl1);
				TagElement tagNode2= findParamTag(methodDecl, decl2);
				if (tagNode1 != null && tagNode2 != null) {
					rewrite.replace(tagNode1, rewrite.createCopyTarget(tagNode2), null);
					rewrite.replace(tagNode2, rewrite.createCopyTarget(tagNode1), null);
				}
			}
		}
		if (!hasCreatedVariables) {
			return;
		}

		if (methodDecl.getBody() != null) {
			// avoid take a name of a local variable inside
			JavaScriptUnit root= (JavaScriptUnit) methodDecl.getRoot();
			IBinding[] bindings= (new ScopeAnalyzer(root)).getDeclarationsAfter(methodDecl.getBody().getStartPosition(), ScopeAnalyzer.VARIABLES);
			for (int i= 0; i < bindings.length; i++) {
				usedNames.add(bindings[i].getName());
			}
		}

		fixupNames(rewrite, usedNames, methodDecl, isInDifferentCU);
	}

	private void fixupNames(ASTRewrite rewrite, ArrayList usedNames, FunctionDeclaration methodDecl, boolean isInDifferentCU) {
		AST ast= rewrite.getAST();
		// set names for new parameters
		for (int i= 0; i < fParameterChanges.length; i++) {
			ChangeDescription curr= fParameterChanges[i];
			if (curr instanceof ModifyDescription) {
				ModifyDescription desc= (ModifyDescription) curr;

				String typeKey= getParamTypeGroupId(i);
				String nameKey= getParamNameGroupId(i);

				// collect name suggestions
				String favourite= null;
				String[] excludedNames= (String[]) usedNames.toArray(new String[usedNames.size()]);
				
				String suggestedName= desc.name;
				if (suggestedName != null) {
					favourite= StubUtility.suggestArgumentName(getCompilationUnit().getJavaScriptProject(), suggestedName, excludedNames);
					addLinkedPositionProposal(nameKey, favourite, null);
				}
				
				if (desc instanceof EditDescription) {
					addLinkedPositionProposal(nameKey, ((EditDescription)desc).orginalName, null);
				}
				
				Type type= desc.resultingParamType;
				String[] suggestedNames= StubUtility.getArgumentNameSuggestions(getCompilationUnit().getJavaScriptProject(), type, excludedNames);
				for (int k= 0; k < suggestedNames.length; k++) {
					addLinkedPositionProposal(nameKey, suggestedNames[k], null);
				}
				if (favourite == null) {
					favourite= suggestedNames[0];
				}		
				usedNames.add(favourite);
				
				SimpleName[] names= desc.resultingParamName;
				for (int j= 0; j < names.length; j++) {
					names[j].setIdentifier(favourite);
					addLinkedPosition(rewrite.track(names[j]), false, nameKey);
				}
				
				addLinkedPosition(rewrite.track(desc.resultingParamType), true, typeKey);

				// collect type suggestions
				ITypeBinding[] bindings= ASTResolving.getRelaxingTypes(ast, desc.type);
				for (int k= 0; k < bindings.length; k++) {
					addLinkedPositionProposal(typeKey, bindings[k]);
				}

				SimpleName tagArg= desc.resultingTagArg;
				if (tagArg != null) {
					tagArg.setIdentifier(favourite);
					addLinkedPosition(rewrite.track(tagArg), false, nameKey);
				}
			}
		}
	}

	private TagElement findParamTag(FunctionDeclaration decl, SingleVariableDeclaration param) {
		JSdoc javadoc= decl.getJavadoc();
		if (javadoc != null) {
			return JavadocTagsSubProcessor.findParamTag(javadoc, param.getName().getIdentifier());
		}
		return null;
	}

	private TagElement insertParamTag(ListRewrite tagRewriter, List parameters, int currentIndex, TagElement newTagElement) {
		HashSet previousNames= new HashSet();
		for (int n = 0; n < currentIndex; n++) {
			SingleVariableDeclaration var= (SingleVariableDeclaration) parameters.get(n);
			previousNames.add(var.getName().getIdentifier());
		}

		JavadocTagsSubProcessor.insertTag(tagRewriter, newTagElement, previousNames);
		return newTagElement;
	}

	private void modifyExceptions(ASTRewrite rewrite, FunctionDeclaration methodDecl) throws CoreException {
		AST ast= methodDecl.getAST();

		ImportRewrite imports= getImportRewrite();
		ListRewrite listRewrite= rewrite.getListRewrite(methodDecl, FunctionDeclaration.THROWN_EXCEPTIONS_PROPERTY);

		List exceptions= methodDecl.thrownExceptions(); // old exceptions
		int k= 0; // index over the old exceptions

		for (int i= 0; i < fExceptionChanges.length; i++) {
			ChangeDescription curr= fExceptionChanges[i];

			if (curr == null) {
				k++;
			} else if (curr instanceof InsertDescription) {
				InsertDescription desc= (InsertDescription) curr;
				String type= imports.addImport(desc.type);
				ASTNode newNode= ASTNodeFactory.newName(ast, type);

				listRewrite.insertAt(newNode, i, null);

				String key= getExceptionTypeGroupId(i);
				addLinkedPosition(rewrite.track(newNode), false, key);

				JSdoc javadoc= methodDecl.getJavadoc();
				if (javadoc != null) {
					TagElement newTagElement= ast.newTagElement();
					newTagElement.setTagName(TagElement.TAG_THROWS);
					ASTNode newRef= ASTNodeFactory.newName(ast, type);
					newTagElement.fragments().add(newRef);
					insertTabStop(rewrite, newTagElement.fragments(), "throws_tagcomment" + i); //$NON-NLS-1$
					insertThrowsTag(rewrite.getListRewrite(javadoc, JSdoc.TAGS_PROPERTY), exceptions, k, newTagElement);

					addLinkedPosition(rewrite.track(newRef), false, key);
				}

			} else if (curr instanceof RemoveDescription) {
				Name node= (Name) exceptions.get(k);

				listRewrite.remove(node, null);
				k++;

				TagElement tagNode= findThrowsTag(methodDecl, node);
				if (tagNode != null) {
					rewrite.remove(tagNode, null);
				}
			} else if (curr instanceof EditDescription) {
				EditDescription desc= (EditDescription) curr;

				Name oldNode= (Name) exceptions.get(k);

				String type= imports.addImport(desc.type);
				ASTNode newNode= ASTNodeFactory.newName(ast, type);

				listRewrite.replace(oldNode, newNode, null);
				String key= getExceptionTypeGroupId(i);
				addLinkedPosition(rewrite.track(newNode), false, key);

				k++;

				TagElement tagNode= findThrowsTag(methodDecl, oldNode);
				if (tagNode != null) {
					ASTNode newRef= ASTNodeFactory.newName(ast, type);
					rewrite.replace((ASTNode) tagNode.fragments().get(0), newRef, null);
					addLinkedPosition(rewrite.track(newRef), false, key);
				}

			} else if (curr instanceof SwapDescription) {
				Name decl1= (Name) exceptions.get(k);
				Name decl2= (Name) exceptions.get(((SwapDescription) curr).index);

				rewrite.replace(decl1, rewrite.createCopyTarget(decl2), null);
				rewrite.replace(decl2, rewrite.createCopyTarget(decl1), null);

				k++;

				TagElement tagNode1= findThrowsTag(methodDecl, decl1);
				TagElement tagNode2= findThrowsTag(methodDecl, decl2);
				if (tagNode1 != null && tagNode2 != null) {
					rewrite.replace(tagNode1, rewrite.createCopyTarget(tagNode2), null);
					rewrite.replace(tagNode2, rewrite.createCopyTarget(tagNode1), null);
				}
			}
		}
	}

	private void insertTabStop(ASTRewrite rewriter, List fragments, String linkedName) {
		TextElement textElement= rewriter.getAST().newTextElement();
		textElement.setText(""); //$NON-NLS-1$
		fragments.add(textElement);
		addLinkedPosition(rewriter.track(textElement), false, linkedName);
	}

	private TagElement findThrowsTag(FunctionDeclaration decl, Name exception) {
		JSdoc javadoc= decl.getJavadoc();
		if (javadoc != null) {
			String name= ASTNodes.getSimpleNameIdentifier(exception);
			return JavadocTagsSubProcessor.findThrowsTag(javadoc, name);
		}
		return null;
	}

	private TagElement insertThrowsTag(ListRewrite tagRewriter, List exceptions, int currentIndex, TagElement newTagElement) {
		HashSet previousNames= new HashSet();
		for (int n = 0; n < currentIndex; n++) {
			Name curr= (Name) exceptions.get(n);
			previousNames.add(ASTNodes.getSimpleNameIdentifier(curr));
		}

		JavadocTagsSubProcessor.insertTag(tagRewriter, newTagElement, previousNames);
		return newTagElement;
	}


	public String getParamNameGroupId(int idx) {
		return "param_name_" + idx; //$NON-NLS-1$
	}

	public String getParamTypeGroupId(int idx) {
		return "param_type_" + idx; //$NON-NLS-1$
	}

	public String getExceptionTypeGroupId(int idx) {
		return "exc_type_" + idx; //$NON-NLS-1$
	}

}
