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

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.graphics.Image;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.Block;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.IFunctionBinding;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.JSdoc;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.Modifier;
import org.eclipse.wst.jsdt.core.dom.Name;
import org.eclipse.wst.jsdt.core.dom.SingleVariableDeclaration;
import org.eclipse.wst.jsdt.core.dom.SuperConstructorInvocation;
import org.eclipse.wst.jsdt.core.dom.TypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.corext.dom.Bindings;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;
import org.eclipse.wst.jsdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.JavaElementImageProvider;
import org.eclipse.wst.jsdt.ui.CodeGeneration;
import org.eclipse.wst.jsdt.ui.JavaScriptElementImageDescriptor;

public class ConstructorFromSuperclassProposal extends LinkedCorrectionProposal {

	private TypeDeclaration fTypeNode;
	private IFunctionBinding fSuperConstructor;

	public ConstructorFromSuperclassProposal(IJavaScriptUnit cu, TypeDeclaration typeNode, IFunctionBinding superConstructor, int relevance) {
		super("", cu, null, relevance, null); //$NON-NLS-1$
		fTypeNode= typeNode;
		fSuperConstructor= superConstructor;
	}

	/**
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getImage()
	 */
	public Image getImage() {
		return JavaScriptPlugin.getImageDescriptorRegistry().get(
			new JavaScriptElementImageDescriptor(JavaPluginImages.DESC_MISC_PUBLIC, JavaScriptElementImageDescriptor.CONSTRUCTOR, JavaElementImageProvider.SMALL_SIZE)
		);
	}

	/**
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getDisplayString()
	 */
	public String getDisplayString() {
		StringBuffer buf= new StringBuffer();
		buf.append(fTypeNode.getName().getIdentifier());
		buf.append('(');
		if (fSuperConstructor != null) {
			ITypeBinding[] paramTypes= fSuperConstructor.getParameterTypes();
			for (int i= 0; i < paramTypes.length; i++) {
				if (i > 0) {
					buf.append(',');
				}
				buf.append(paramTypes[i].getName());
			}
		}
		buf.append(')');
		return Messages.format(CorrectionMessages.ConstructorFromSuperclassProposal_description, buf.toString());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.ui.text.correction.ASTRewriteCorrectionProposal#getRewrite()
	 */
	protected ASTRewrite getRewrite() throws CoreException {
		AST ast= fTypeNode.getAST();

		ASTRewrite rewrite= ASTRewrite.create(ast);
		
		createImportRewrite((JavaScriptUnit) fTypeNode.getRoot());
		
		CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings(getCompilationUnit().getJavaScriptProject());
		if (!settings.createComments) {
			settings= null;
		}

		FunctionDeclaration newMethodDecl= createNewMethodDeclaration(ast, fSuperConstructor, rewrite, settings);
		rewrite.getListRewrite(fTypeNode, TypeDeclaration.BODY_DECLARATIONS_PROPERTY).insertFirst(newMethodDecl, null);

		addLinkedRanges(rewrite, newMethodDecl);

		return rewrite;
	}

	private void addLinkedRanges(ASTRewrite rewrite, FunctionDeclaration newStub) {
		List parameters= newStub.parameters();
		for (int i= 0; i < parameters.size(); i++) {
			SingleVariableDeclaration curr= (SingleVariableDeclaration) parameters.get(i);
			String name= curr.getName().getIdentifier();
			addLinkedPosition(rewrite.track(curr.getType()), false, "arg_type_" + name); //$NON-NLS-1$
			addLinkedPosition(rewrite.track(curr.getName()), false, "arg_name_" + name); //$NON-NLS-1$
		}
	}

	private FunctionDeclaration createNewMethodDeclaration(AST ast, IFunctionBinding binding, ASTRewrite rewrite, CodeGenerationSettings commentSettings) throws CoreException {
		String name= fTypeNode.getName().getIdentifier();
		FunctionDeclaration decl= ast.newFunctionDeclaration();
		decl.setConstructor(true);
		decl.setName(ast.newSimpleName(name));
		Block body= ast.newBlock();
		decl.setBody(body);

		SuperConstructorInvocation invocation= null;

		List parameters= decl.parameters();
		String[] paramNames= getArgumentNames(binding);

		ITypeBinding enclosingInstance= getEnclosingInstance();
		if (enclosingInstance != null) {
			invocation= addEnclosingInstanceAccess(rewrite, parameters, paramNames, enclosingInstance);
		}

		if (binding == null) {
			decl.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));
		} else {
			decl.modifiers().addAll(ASTNodeFactory.newModifiers(ast, binding.getModifiers()));

			ITypeBinding[] params= binding.getParameterTypes();
			for (int i= 0; i < params.length; i++) {
				SingleVariableDeclaration var= ast.newSingleVariableDeclaration();
				var.setType(getImportRewrite().addImport(params[i], ast));
				var.setName(ast.newSimpleName(paramNames[i]));
				parameters.add(var);
			}

			if (invocation == null) {
				invocation= ast.newSuperConstructorInvocation();
			}

			List arguments= invocation.arguments();
			for (int i= 0; i < paramNames.length; i++) {
				Name argument= ast.newSimpleName(paramNames[i]);
				arguments.add(argument);
				addLinkedPosition(rewrite.track(argument), false, "arg_name_" + paramNames[i]); //$NON-NLS-1$
			}
		}

		String bodyStatement= (invocation == null) ? "" : ASTNodes.asFormattedString(invocation, 0, String.valueOf('\n'), getCompilationUnit().getJavaScriptProject().getOptions(true)); //$NON-NLS-1$
		String placeHolder= CodeGeneration.getMethodBodyContent(getCompilationUnit(), name, name, true, bodyStatement, String.valueOf('\n'));
		if (placeHolder != null) {
			ASTNode todoNode= rewrite.createStringPlaceholder(placeHolder, ASTNode.RETURN_STATEMENT);
			body.statements().add(todoNode);
		}
		if (commentSettings != null) {
			String string= CodeGeneration.getMethodComment(getCompilationUnit(), name, decl, null, String.valueOf('\n'));
			if (string != null) {
				JSdoc javadoc= (JSdoc) rewrite.createStringPlaceholder(string, ASTNode.JSDOC);
				decl.setJavadoc(javadoc);
			}
		}
		return decl;
	}

	private SuperConstructorInvocation addEnclosingInstanceAccess(ASTRewrite rewrite, List parameters, String[] paramNames, ITypeBinding enclosingInstance) throws CoreException {
		AST ast= rewrite.getAST();
		SuperConstructorInvocation invocation= ast.newSuperConstructorInvocation();

		SingleVariableDeclaration var= ast.newSingleVariableDeclaration();
		var.setType(getImportRewrite().addImport(enclosingInstance, ast));
		String[] enclosingArgNames= StubUtility.getArgumentNameSuggestions(getCompilationUnit().getJavaScriptProject(), enclosingInstance.getTypeDeclaration().getName(), 0, paramNames);
		String firstName= enclosingArgNames[0];
		var.setName(ast.newSimpleName(firstName));
		parameters.add(var);

		Name enclosing= ast.newSimpleName(firstName);
		invocation.setExpression(enclosing);

		String key= "arg_name_" + firstName; //$NON-NLS-1$
		addLinkedPosition(rewrite.track(enclosing), false, key);
		for (int i= 0; i < enclosingArgNames.length; i++) {
			addLinkedPositionProposal(key, enclosingArgNames[i], null); // alternative names
		}
		return invocation;
	}

	private ITypeBinding getEnclosingInstance() {
		ITypeBinding currBinding= fTypeNode.resolveBinding();
		if (currBinding == null || Modifier.isStatic(currBinding.getModifiers())) {
			return null;
		}
		ITypeBinding superBinding= currBinding.getSuperclass();
		if (superBinding == null || superBinding.getDeclaringClass() == null || Modifier.isStatic(superBinding.getModifiers())) {
			return null;
		}
		ITypeBinding enclosing= superBinding.getDeclaringClass();

		while (currBinding != null) {
			if (Bindings.isSuperType(enclosing, currBinding)) {
				return null; // enclosing in scope
			}
			if (Modifier.isStatic(currBinding.getModifiers())) {
				return null; // no more enclosing instances
			}
			currBinding= currBinding.getDeclaringClass();
		}
		return enclosing;
	}


	private String[] getArgumentNames(IFunctionBinding binding) {
		if (binding == null) {
			return new String[0];
		}
		return StubUtility.suggestArgumentNames(getCompilationUnit().getJavaScriptProject(), binding);
	}
}
