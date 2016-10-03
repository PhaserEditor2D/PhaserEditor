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
import java.util.List;

import org.eclipse.ui.ISharedImages;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ASTVisitor;
import org.eclipse.wst.jsdt.core.dom.Assignment;
import org.eclipse.wst.jsdt.core.dom.ClassInstanceCreation;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.Expression;
import org.eclipse.wst.jsdt.core.dom.FieldAccess;
import org.eclipse.wst.jsdt.core.dom.FieldDeclaration;
import org.eclipse.wst.jsdt.core.dom.IBinding;
import org.eclipse.wst.jsdt.core.dom.IFunctionBinding;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.IVariableBinding;
import org.eclipse.wst.jsdt.core.dom.JSdoc;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.FunctionInvocation;
import org.eclipse.wst.jsdt.core.dom.PostfixExpression;
import org.eclipse.wst.jsdt.core.dom.PrefixExpression;
import org.eclipse.wst.jsdt.core.dom.QualifiedName;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.SingleVariableDeclaration;
import org.eclipse.wst.jsdt.core.dom.SuperMethodInvocation;
import org.eclipse.wst.jsdt.core.dom.TagElement;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationExpression;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationStatement;
import org.eclipse.wst.jsdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.corext.dom.LinkedNodeFinder;
import org.eclipse.wst.jsdt.internal.corext.dom.NodeFinder;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.ASTProvider;

public class RemoveDeclarationCorrectionProposal extends ASTRewriteCorrectionProposal {

	private static class SideEffectFinder extends ASTVisitor {

		private ArrayList fSideEffectNodes;

		public SideEffectFinder(ArrayList res) {
			fSideEffectNodes= res;
		}

		public boolean visit(Assignment node) {
			fSideEffectNodes.add(node);
			return false;
		}

		public boolean visit(PostfixExpression node) {
			fSideEffectNodes.add(node);
			return false;
		}

		public boolean visit(PrefixExpression node) {
			Object operator= node.getOperator();
			if (operator == PrefixExpression.Operator.INCREMENT || operator == PrefixExpression.Operator.DECREMENT) {
				fSideEffectNodes.add(node);
			}
			return false;
		}

		public boolean visit(FunctionInvocation node) {
			fSideEffectNodes.add(node);
			return false;
		}

		public boolean visit(ClassInstanceCreation node) {
			fSideEffectNodes.add(node);
			return false;
		}

		public boolean visit(SuperMethodInvocation node) {
			fSideEffectNodes.add(node);
			return false;
		}
	}


	private SimpleName fName;

	public RemoveDeclarationCorrectionProposal(IJavaScriptUnit cu, SimpleName name, int relevance) {
		super("", cu, null, relevance, JavaScriptPlugin.getDefault().getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE)); //$NON-NLS-1$
		fName= name;
	}

	public String getDisplayString() {
		IBinding binding= fName.resolveBinding();
		String name= fName.getIdentifier();
		switch (binding.getKind()) {
			case IBinding.TYPE:
				return Messages.format(CorrectionMessages.RemoveDeclarationCorrectionProposal_removeunusedtype_description, name);
			case IBinding.METHOD:
				if (((IFunctionBinding) binding).isConstructor()) {
					return Messages.format(CorrectionMessages.RemoveDeclarationCorrectionProposal_removeunusedconstructor_description, name);
				} else {
					return Messages.format(CorrectionMessages.RemoveDeclarationCorrectionProposal_removeunusedmethod_description, name);
				}
			case IBinding.VARIABLE:
				if (((IVariableBinding) binding).isField()) {
					return Messages.format(CorrectionMessages.RemoveDeclarationCorrectionProposal_removeunusedfield_description, name);
				} else {
					return Messages.format(CorrectionMessages.RemoveDeclarationCorrectionProposal_removeunusedvar_description, name);
				}
			default:
				return super.getDisplayString();
		}
	}

	/*(non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.ui.text.correction.ASTRewriteCorrectionProposal#getRewrite()
	 */
	protected ASTRewrite getRewrite() {
		IBinding binding= fName.resolveBinding();
		JavaScriptUnit root= (JavaScriptUnit) fName.getRoot();
		ASTRewrite rewrite;
		if (binding.getKind() == IBinding.METHOD) {
			IFunctionBinding decl= ((IFunctionBinding) binding).getMethodDeclaration();
			ASTNode declaration= root.findDeclaringNode(decl);
			rewrite= ASTRewrite.create(root.getAST());
			rewrite.remove(declaration, null);
		} else if (binding.getKind() == IBinding.TYPE) {
			ITypeBinding decl= ((ITypeBinding) binding).getTypeDeclaration();
			ASTNode declaration= root.findDeclaringNode(decl);
			rewrite= ASTRewrite.create(root.getAST());
			rewrite.remove(declaration, null);
		} else if (binding.getKind() == IBinding.VARIABLE) {
			// needs full AST
			JavaScriptUnit completeRoot= JavaScriptPlugin.getDefault().getASTProvider().getAST(getCompilationUnit(), ASTProvider.WAIT_YES, null);

			SimpleName nameNode= (SimpleName) NodeFinder.perform(completeRoot, fName.getStartPosition(), fName.getLength());

			rewrite= ASTRewrite.create(completeRoot.getAST());
			SimpleName[] references= LinkedNodeFinder.findByBinding(completeRoot, nameNode.resolveBinding());
			for (int i= 0; i < references.length; i++) {
				removeVariableReferences(rewrite, references[i]);
			}

			IVariableBinding bindingDecl= ((IVariableBinding) nameNode.resolveBinding()).getVariableDeclaration();
			ASTNode declaringNode= completeRoot.findDeclaringNode(bindingDecl);
			if (declaringNode instanceof SingleVariableDeclaration) {
				removeParamTag(rewrite, (SingleVariableDeclaration) declaringNode);
			}
		} else {
			throw new IllegalArgumentException("Unexpected binding"); //$NON-NLS-1$
		}
		return rewrite;
	}

	private void removeParamTag(ASTRewrite rewrite, SingleVariableDeclaration varDecl) {
		if (varDecl.getParent() instanceof FunctionDeclaration) {
			JSdoc javadoc= ((FunctionDeclaration) varDecl.getParent()).getJavadoc();
			if (javadoc != null) {
				TagElement tagElement= JavadocTagsSubProcessor.findParamTag(javadoc, varDecl.getName().getIdentifier());
				if (tagElement != null) {
					rewrite.remove(tagElement, null);
				}
			}
		}
	}

	/**
	 * Remove the field or variable declaration including the initializer.
	 */
	private void removeVariableReferences(ASTRewrite rewrite, SimpleName reference) {
		ASTNode parent= reference.getParent();
		while (parent instanceof QualifiedName) {
			parent= parent.getParent();
		}
		if (parent instanceof FieldAccess) {
			parent= parent.getParent();
		}

		int nameParentType= parent.getNodeType();
		if (nameParentType == ASTNode.ASSIGNMENT) {
			Assignment assignment= (Assignment) parent;
			Expression rightHand= assignment.getRightHandSide();

			ASTNode assignParent= assignment.getParent();
			if (assignParent.getNodeType() == ASTNode.EXPRESSION_STATEMENT && rightHand.getNodeType() != ASTNode.ASSIGNMENT) {
				removeVariableWithInitializer(rewrite, rightHand, assignParent);
			}	else {
				rewrite.replace(assignment, rewrite.createCopyTarget(rightHand), null);
			}
		} else if (nameParentType == ASTNode.SINGLE_VARIABLE_DECLARATION) {
			rewrite.remove(parent, null);
		} else if (nameParentType == ASTNode.VARIABLE_DECLARATION_FRAGMENT) {
			VariableDeclarationFragment frag= (VariableDeclarationFragment) parent;
			ASTNode varDecl= frag.getParent();
			List fragments;
			if (varDecl instanceof VariableDeclarationExpression) {
				fragments= ((VariableDeclarationExpression) varDecl).fragments();
			} else if (varDecl instanceof FieldDeclaration) {
				fragments= ((FieldDeclaration) varDecl).fragments();
			} else {
				fragments= ((VariableDeclarationStatement) varDecl).fragments();
			}
			if (fragments.size() == 1) {
				rewrite.remove(varDecl, null);
			} else {
				rewrite.remove(frag, null); // don't try to preserve
			}
		}
	}

	private void removeVariableWithInitializer(ASTRewrite rewrite, ASTNode initializerNode, ASTNode statementNode) {
		ArrayList sideEffectNodes= new ArrayList();
		initializerNode.accept(new SideEffectFinder(sideEffectNodes));
		int nSideEffects= sideEffectNodes.size();
		if (nSideEffects == 0) {
			if (ASTNodes.isControlStatementBody(statementNode.getLocationInParent())) {
				rewrite.replace(statementNode, rewrite.getAST().newBlock(), null);
			} else {
				rewrite.remove(statementNode, null);
			}
		} else {
			// do nothing yet
		}
	}

}
