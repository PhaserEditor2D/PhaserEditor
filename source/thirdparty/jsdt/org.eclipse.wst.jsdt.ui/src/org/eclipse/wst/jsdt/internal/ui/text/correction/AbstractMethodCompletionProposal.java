/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.graphics.Image;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.Block;
import org.eclipse.wst.jsdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.IVariableBinding;
import org.eclipse.wst.jsdt.core.dom.JSdoc;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.PrimitiveType;
import org.eclipse.wst.jsdt.core.dom.ReturnStatement;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.Type;
import org.eclipse.wst.jsdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.wst.jsdt.core.dom.rewrite.ListRewrite;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.corext.dom.Bindings;
import org.eclipse.wst.jsdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.wst.jsdt.ui.CodeGeneration;

public abstract class AbstractMethodCompletionProposal extends LinkedCorrectionProposal {

	private ASTNode fNode;
	private ITypeBinding fSenderBinding;

	public AbstractMethodCompletionProposal(String label, IJavaScriptUnit targetCU, ASTNode invocationNode, ITypeBinding binding, int relevance, Image image) {
		super(label, targetCU, null, relevance, image);

		Assert.isTrue(binding != null && Bindings.isDeclarationBinding(binding));

		fNode= invocationNode;
		fSenderBinding= binding;
	}

	protected ASTNode getInvocationNode() {
		return fNode;
	}

	/**
	 * @return The binding of the type declaration (generic type)
	 */
	protected ITypeBinding getSenderBinding() {
		return fSenderBinding;
	}

	protected ASTRewrite getRewrite() throws CoreException {
		JavaScriptUnit astRoot= ASTResolving.findParentCompilationUnit(fNode);
		ASTNode typeDecl= astRoot.findDeclaringNode(fSenderBinding);
		ASTNode newTypeDecl= null;
		boolean isInDifferentCU;
		if (typeDecl != null) {
			isInDifferentCU= false;
			newTypeDecl= typeDecl;
		} else {
			isInDifferentCU= true;
			astRoot= ASTResolving.createQuickFixAST(getCompilationUnit(), null);
			newTypeDecl= astRoot.findDeclaringNode(fSenderBinding.getKey());
		}
 		createImportRewrite(astRoot);
		
		if (newTypeDecl != null) {
			ASTRewrite rewrite= ASTRewrite.create(astRoot.getAST());

			FunctionDeclaration newStub= getStub(rewrite, newTypeDecl);

			ChildListPropertyDescriptor property= ASTNodes.getBodyDeclarationsProperty(newTypeDecl);
			List members= (List) newTypeDecl.getStructuralProperty(property);

			int insertIndex;
			if (isConstructor()) {
				insertIndex= findConstructorInsertIndex(members);
			} else if (!isInDifferentCU) {
				insertIndex= findMethodInsertIndex(members, fNode.getStartPosition());
			} else {
				insertIndex= members.size();
			}
			ListRewrite listRewriter= rewrite.getListRewrite(newTypeDecl, property);
			listRewriter.insertAt(newStub, insertIndex, null);

			return rewrite;
		}
		return null;
	}

	private FunctionDeclaration getStub(ASTRewrite rewrite, ASTNode targetTypeDecl) throws CoreException {
		AST ast= targetTypeDecl.getAST();
		FunctionDeclaration decl= ast.newFunctionDeclaration();

		SimpleName newNameNode= getNewName(rewrite);

		decl.setConstructor(isConstructor());
		
		addNewModifiers(rewrite, targetTypeDecl, decl.modifiers());

		ArrayList takenNames= new ArrayList();

		decl.setName(newNameNode);

		IVariableBinding[] declaredFields= fSenderBinding.getDeclaredFields();
		for (int i= 0; i < declaredFields.length; i++) { // avoid to take parameter names that are equal to field names
			takenNames.add(declaredFields[i].getName());
		}

		String bodyStatement= ""; //$NON-NLS-1$
		if (!isConstructor()) {
			Type returnType= getNewMethodType(rewrite);
			if (returnType == null) {
				decl.setReturnType2(ast.newPrimitiveType(PrimitiveType.VOID));
			} else {
				decl.setReturnType2(returnType);
			}
			if (returnType != null) {
				ReturnStatement returnStatement= ast.newReturnStatement();
				returnStatement.setExpression(ASTNodeFactory.newDefaultExpression(ast, returnType, 0));
				bodyStatement= ASTNodes.asFormattedString(returnStatement, 0, String.valueOf('\n'), getCompilationUnit().getJavaScriptProject().getOptions(true));
			}
		}
		
		addNewParameters(rewrite, takenNames, decl.parameters());

		Block body= ast.newBlock();
		String placeHolder= CodeGeneration.getMethodBodyContent(getCompilationUnit(), fSenderBinding.getName(), newNameNode.getIdentifier(), isConstructor(), bodyStatement, String.valueOf('\n'));
		if (placeHolder != null) {
			ASTNode todoNode= rewrite.createStringPlaceholder(placeHolder, ASTNode.RETURN_STATEMENT);
			body.statements().add(todoNode);
		}
		
		decl.setBody(body);

		CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings(getCompilationUnit().getJavaScriptProject());
		if (settings.createComments && !fSenderBinding.isAnonymous()) {
			String string= CodeGeneration.getMethodComment(getCompilationUnit(), fSenderBinding.getName(), decl, null, String.valueOf('\n'));
			if (string != null) {
				JSdoc javadoc= (JSdoc) rewrite.createStringPlaceholder(string, ASTNode.JSDOC);
				decl.setJavadoc(javadoc);
			}
		}
		return decl;
	}



	private int findMethodInsertIndex(List decls, int currPos) {
		int nDecls= decls.size();
		for (int i= 0; i < nDecls; i++) {
			ASTNode curr= (ASTNode) decls.get(i);
			if (curr instanceof FunctionDeclaration && currPos < curr.getStartPosition() + curr.getLength()) {
				return i + 1;
			}
		}
		return nDecls;
	}

	private int findConstructorInsertIndex(List decls) {
		int nDecls= decls.size();
		int lastMethod= 0;
		for (int i= nDecls - 1; i >= 0; i--) {
			ASTNode curr= (ASTNode) decls.get(i);
			if (curr instanceof FunctionDeclaration) {
				if (((FunctionDeclaration) curr).isConstructor()) {
					return i + 1;
				}
				lastMethod= i;
			}
		}
		return lastMethod;
	}

	protected abstract boolean isConstructor();

	protected abstract void addNewModifiers(ASTRewrite rewrite, ASTNode targetTypeDecl, List exceptions);
	protected abstract void addNewParameters(ASTRewrite rewrite, List takenNames, List params) throws CoreException;

	protected abstract SimpleName getNewName(ASTRewrite rewrite);
	protected abstract Type getNewMethodType(ASTRewrite rewrite) throws CoreException;


}
