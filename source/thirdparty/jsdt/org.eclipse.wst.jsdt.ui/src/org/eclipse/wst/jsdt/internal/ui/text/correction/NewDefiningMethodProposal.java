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
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.IFunctionBinding;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.Modifier;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.SingleVariableDeclaration;
import org.eclipse.wst.jsdt.core.dom.Type;
import org.eclipse.wst.jsdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.wst.jsdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.JavaElementImageProvider;

/**
 *
 */
public class NewDefiningMethodProposal extends AbstractMethodCompletionProposal {

	private final IFunctionBinding fMethod;
	private final String[] fParamNames;

	public NewDefiningMethodProposal(String label, IJavaScriptUnit targetCU, ASTNode invocationNode, ITypeBinding binding, IFunctionBinding method, String[] paramNames, int relevance) {
		super(label,targetCU,invocationNode,binding,relevance,null);
		fMethod= method;
		fParamNames= paramNames;

		ImageDescriptor desc= JavaElementImageProvider.getMethodImageDescriptor(false, method.getModifiers());
		setImage(JavaScriptPlugin.getImageDescriptorRegistry().get(desc));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.ui.text.correction.AbstractMethodCompletionProposal#isConstructor()
	 */
	protected boolean isConstructor() {
		return fMethod.isConstructor();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.ui.text.correction.AbstractMethodCompletionProposal#addNewParameters(org.eclipse.wst.jsdt.core.dom.rewrite.ASTRewrite, java.util.List, java.util.List)
	 */
	protected void addNewParameters(ASTRewrite rewrite, List takenNames, List params) throws CoreException {
		AST ast= rewrite.getAST();
		ImportRewrite importRewrite= getImportRewrite();
		ITypeBinding[] bindings= fMethod.getParameterTypes();

		IJavaScriptProject project= getCompilationUnit().getJavaScriptProject();
		String[][] paramNames= StubUtility.suggestArgumentNamesWithProposals(project, fParamNames);

		for (int i= 0; i < bindings.length; i++) {
			ITypeBinding curr= bindings[i];

			String[] proposedNames= paramNames[i];
			
			SingleVariableDeclaration newParam= ast.newSingleVariableDeclaration();

			newParam.setType(importRewrite.addImport(curr, ast));
			newParam.setName(ast.newSimpleName(proposedNames[0]));

			params.add(newParam);

			String groupId= "arg_name_" + i; //$NON-NLS-1$
			addLinkedPosition(rewrite.track(newParam.getName()), false, groupId);
			
			for (int k= 0; k < proposedNames.length; k++) {
				addLinkedPositionProposal(groupId, proposedNames[k], null);
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.ui.text.correction.AbstractMethodCompletionProposal#getNewName()
	 */
	protected SimpleName getNewName(ASTRewrite rewrite) {
		AST ast= rewrite.getAST();
		SimpleName nameNode= ast.newSimpleName(fMethod.getName());
		return nameNode;
	}

	private int evaluateModifiers() {
		int modifiers= fMethod.getModifiers();
		if (Modifier.isPrivate(modifiers)) {
			modifiers |= Modifier.PROTECTED;
		}
		return modifiers & (Modifier.PUBLIC | Modifier.PROTECTED | Modifier.ABSTRACT | Modifier.STRICTFP);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.ui.text.correction.AbstractMethodCompletionProposal#addNewModifiers(org.eclipse.wst.jsdt.core.dom.rewrite.ASTRewrite, java.util.List)
	 */
	protected void addNewModifiers(ASTRewrite rewrite, ASTNode targetTypeDecl, List modifiers) {
		modifiers.addAll(rewrite.getAST().newModifiers(evaluateModifiers()));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.ui.text.correction.AbstractMethodCompletionProposal#getNewMethodType(org.eclipse.wst.jsdt.core.dom.rewrite.ASTRewrite)
	 */
	protected Type getNewMethodType(ASTRewrite rewrite) throws CoreException {
		return getImportRewrite().addImport(fMethod.getReturnType(), rewrite.getAST());
	}
}
