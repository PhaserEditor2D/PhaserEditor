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

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.Block;
import org.eclipse.wst.jsdt.core.dom.FieldDeclaration;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.IBinding;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.IVariableBinding;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.SingleVariableDeclaration;
import org.eclipse.wst.jsdt.core.dom.Type;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationExpression;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationStatement;
import org.eclipse.wst.jsdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.wst.jsdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.wst.jsdt.core.dom.rewrite.ListRewrite;
import org.eclipse.wst.jsdt.internal.corext.dom.Bindings;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.BindingLabelProvider;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabels;

public class TypeChangeCompletionProposal extends LinkedCorrectionProposal {

	private IBinding fBinding;
	private JavaScriptUnit fAstRoot;
	private ITypeBinding fNewType;
	private boolean fOfferSuperTypeProposals;

	public TypeChangeCompletionProposal(IJavaScriptUnit targetCU, IBinding binding, JavaScriptUnit astRoot, ITypeBinding newType, boolean offerSuperTypeProposals, int relevance) {
		super("", targetCU, null, relevance, JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE)); //$NON-NLS-1$

		Assert.isTrue(binding != null && (binding.getKind() == IBinding.METHOD || binding.getKind() == IBinding.VARIABLE) && Bindings.isDeclarationBinding(binding));

		fBinding= binding; // must be generic method or (generic) variable
		fAstRoot= astRoot;
		fNewType= newType;
		fOfferSuperTypeProposals= offerSuperTypeProposals;

		String typeName= BindingLabelProvider.getBindingLabel(newType, JavaScriptElementLabels.ALL_DEFAULT);
		if (binding.getKind() == IBinding.VARIABLE) {
			IVariableBinding varBinding= (IVariableBinding) binding;

			String[] args= { varBinding.getName(),  typeName};
			if (varBinding.isField()) {
				setDisplayName(Messages.format(CorrectionMessages.TypeChangeCompletionProposal_field_name, args));
			} else if (astRoot.findDeclaringNode(binding) instanceof SingleVariableDeclaration) {
				setDisplayName(Messages.format(CorrectionMessages.TypeChangeCompletionProposal_param_name, args));
			} else {
				setDisplayName(Messages.format(CorrectionMessages.TypeChangeCompletionProposal_variable_name, args));
			}
		} else {
			String[] args= { binding.getName(), typeName };
			setDisplayName(Messages.format(CorrectionMessages.TypeChangeCompletionProposal_method_name, args));
		}
	}

	protected ASTRewrite getRewrite() throws CoreException {
		ASTNode boundNode= fAstRoot.findDeclaringNode(fBinding);
		ASTNode declNode= null;
		JavaScriptUnit newRoot= fAstRoot;
		if (boundNode != null) {
			declNode= boundNode; // is same CU
		} else {
			newRoot= ASTResolving.createQuickFixAST(getCompilationUnit(), null);
			declNode= newRoot.findDeclaringNode(fBinding.getKey());
		}
		if (declNode != null) {
			AST ast= declNode.getAST();
			ASTRewrite rewrite= ASTRewrite.create(ast);
			ImportRewrite imports= createImportRewrite(newRoot);

			Type type= imports.addImport(fNewType, ast);

			if (declNode instanceof FunctionDeclaration) {
				FunctionDeclaration methodDecl= (FunctionDeclaration) declNode;
				rewrite.set(methodDecl, FunctionDeclaration.RETURN_TYPE2_PROPERTY, type, null);
				rewrite.set(methodDecl, FunctionDeclaration.EXTRA_DIMENSIONS_PROPERTY, Integer.valueOf(0), null);
			} else if (declNode instanceof VariableDeclarationFragment) {
				ASTNode parent= declNode.getParent();
				if (parent instanceof FieldDeclaration) {
					FieldDeclaration fieldDecl= (FieldDeclaration) parent;
					if (fieldDecl.fragments().size() > 1 && (fieldDecl.getParent() instanceof AbstractTypeDeclaration)) { // split
						VariableDeclarationFragment placeholder= (VariableDeclarationFragment) rewrite.createMoveTarget(declNode);
						FieldDeclaration newField= ast.newFieldDeclaration(placeholder);
						newField.setType(type);
						AbstractTypeDeclaration typeDecl= (AbstractTypeDeclaration) fieldDecl.getParent();

						ListRewrite listRewrite= rewrite.getListRewrite(typeDecl, typeDecl.getBodyDeclarationsProperty());
						if (fieldDecl.fragments().indexOf(declNode) == 0) { // if it as the first in the list-> insert before
							listRewrite.insertBefore(newField, parent, null);
						} else {
							listRewrite.insertAfter(newField, parent, null);
						}
					} else {
						rewrite.set(fieldDecl, FieldDeclaration.TYPE_PROPERTY, type, null);
						rewrite.set(declNode, VariableDeclarationFragment.EXTRA_DIMENSIONS_PROPERTY, Integer.valueOf(0), null);
					}
				} else if (parent instanceof VariableDeclarationStatement) {
					VariableDeclarationStatement varDecl= (VariableDeclarationStatement) parent;
					if (varDecl.fragments().size() > 1 && (varDecl.getParent() instanceof Block)) { // split
						VariableDeclarationFragment placeholder= (VariableDeclarationFragment) rewrite.createMoveTarget(declNode);
						VariableDeclarationStatement newStat= ast.newVariableDeclarationStatement(placeholder);
						newStat.setType(type);

						ListRewrite listRewrite= rewrite.getListRewrite(varDecl.getParent(), Block.STATEMENTS_PROPERTY);
						if (varDecl.fragments().indexOf(declNode) == 0) { // if it as the first in the list-> insert before
							listRewrite.insertBefore(newStat, parent, null);
						} else {
							listRewrite.insertAfter(newStat, parent, null);
						}
					} else {
						rewrite.set(varDecl, VariableDeclarationStatement.TYPE_PROPERTY, type, null);
						rewrite.set(declNode, VariableDeclarationFragment.EXTRA_DIMENSIONS_PROPERTY, Integer.valueOf(0), null);
					}
				} else if (parent instanceof VariableDeclarationExpression) {
					VariableDeclarationExpression varDecl= (VariableDeclarationExpression) parent;

					rewrite.set(varDecl, VariableDeclarationExpression.TYPE_PROPERTY, type, null);
					rewrite.set(declNode, VariableDeclarationFragment.EXTRA_DIMENSIONS_PROPERTY, Integer.valueOf(0), null);
				}
			} else if (declNode instanceof SingleVariableDeclaration) {
				SingleVariableDeclaration variableDeclaration= (SingleVariableDeclaration) declNode;
				rewrite.set(variableDeclaration, SingleVariableDeclaration.TYPE_PROPERTY, type, null);
				rewrite.set(variableDeclaration, SingleVariableDeclaration.EXTRA_DIMENSIONS_PROPERTY, Integer.valueOf(0), null);
			}

			// set up linked mode
			final String KEY_TYPE= "type"; //$NON-NLS-1$
			addLinkedPosition(rewrite.track(type), true, KEY_TYPE);
			if (fOfferSuperTypeProposals) {
				ITypeBinding[] typeProposals= ASTResolving.getRelaxingTypes(ast, fNewType);
				for (int i= 0; i < typeProposals.length; i++) {
					addLinkedPositionProposal(KEY_TYPE, typeProposals[i]);
				}
			}
			return rewrite;
		}
		return null;
	}


}
