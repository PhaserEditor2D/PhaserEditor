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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.Assignment;
import org.eclipse.wst.jsdt.core.dom.Block;
import org.eclipse.wst.jsdt.core.dom.BodyDeclaration;
import org.eclipse.wst.jsdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.Expression;
import org.eclipse.wst.jsdt.core.dom.ExpressionStatement;
import org.eclipse.wst.jsdt.core.dom.FieldAccess;
import org.eclipse.wst.jsdt.core.dom.FieldDeclaration;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.IVariableBinding;
import org.eclipse.wst.jsdt.core.dom.Initializer;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.Modifier;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.SingleVariableDeclaration;
import org.eclipse.wst.jsdt.core.dom.Statement;
import org.eclipse.wst.jsdt.core.dom.Type;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationExpression;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;
import org.eclipse.wst.jsdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.corext.dom.Bindings;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;

/**
 * Proposals for 'Assign to variable' quick assist
 * - Assign an expression from an ExpressionStatement to a local or field
 * - Assign a parameter to a field
 * */
public class AssignToVariableAssistProposal extends LinkedCorrectionProposal {

	public static final int LOCAL= 1;
	public static final int FIELD= 2;

	private final String KEY_NAME= "name";  //$NON-NLS-1$
	private final String KEY_TYPE= "type";  //$NON-NLS-1$

	private final int  fVariableKind;
	private final ASTNode fNodeToAssign; // ExpressionStatement or SingleVariableDeclaration
	private final ITypeBinding fTypeBinding;

	private VariableDeclarationFragment fExistingFragment;
	
	public AssignToVariableAssistProposal(IJavaScriptUnit cu, int variableKind, ExpressionStatement node, ITypeBinding typeBinding, int relevance) {
		super("", cu, null, relevance, null); //$NON-NLS-1$

		fVariableKind= variableKind;
		fNodeToAssign= node;
		
		fTypeBinding= typeBinding;
		if (variableKind == LOCAL) {
			setDisplayName(CorrectionMessages.AssignToVariableAssistProposal_assigntolocal_description);
			setImage(JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_LOCAL));
		} else {
			setDisplayName(CorrectionMessages.AssignToVariableAssistProposal_assigntofield_description);
			setImage(JavaPluginImages.get(JavaPluginImages.IMG_FIELD_PRIVATE));
		}
		createImportRewrite((JavaScriptUnit) node.getRoot());
	}

	public AssignToVariableAssistProposal(IJavaScriptUnit cu, SingleVariableDeclaration parameter, VariableDeclarationFragment existingFragment, ITypeBinding typeBinding, int relevance) {
		super("", cu, null, relevance, null); //$NON-NLS-1$

		fVariableKind= FIELD;
		fNodeToAssign= parameter;
		fTypeBinding= typeBinding;
		fExistingFragment= existingFragment;
		
		if (existingFragment == null) {
			setDisplayName(CorrectionMessages.AssignToVariableAssistProposal_assignparamtofield_description);
		} else {
			setDisplayName(Messages.format(CorrectionMessages.AssignToVariableAssistProposal_assigntoexistingfield_description, existingFragment.getName().getIdentifier()));
		}
		setImage(JavaPluginImages.get(JavaPluginImages.IMG_FIELD_PRIVATE));
	}

	protected ASTRewrite getRewrite() throws CoreException {
		if (fVariableKind == FIELD) {
			return doAddField();
		} else { // LOCAL
			return doAddLocal();
		}
	}

	private ASTRewrite doAddLocal() throws CoreException {
		Expression expression= ((ExpressionStatement) fNodeToAssign).getExpression();
		AST ast= fNodeToAssign.getAST();

		ASTRewrite rewrite= ASTRewrite.create(ast);
		
		createImportRewrite((JavaScriptUnit) fNodeToAssign.getRoot());

		String[] varNames= suggestLocalVariableNames(fTypeBinding, expression);
		for (int i= 0; i < varNames.length; i++) {
			addLinkedPositionProposal(KEY_NAME, varNames[i], null);
		}

		VariableDeclarationFragment newDeclFrag= ast.newVariableDeclarationFragment();
		newDeclFrag.setName(ast.newSimpleName(varNames[0]));
		newDeclFrag.setInitializer((Expression) rewrite.createCopyTarget(expression));

		// trick for bug 43248: use an VariableDeclarationExpression and keep the ExpressionStatement
		VariableDeclarationExpression newDecl= ast.newVariableDeclarationExpression(newDeclFrag);

		Type type= evaluateType(ast);
		newDecl.setType(type);

		rewrite.replace(expression, newDecl, null);

		addLinkedPosition(rewrite.track(newDeclFrag.getName()), true, KEY_NAME);
		addLinkedPosition(rewrite.track(newDecl.getType()), false, KEY_TYPE);
		setEndPosition(rewrite.track(fNodeToAssign)); // set cursor after expression statement

		return rewrite;
	}

	private ASTRewrite doAddField() throws CoreException {
		boolean isParamToField= fNodeToAssign.getNodeType() == ASTNode.SINGLE_VARIABLE_DECLARATION;

		ASTNode newTypeDecl= ASTResolving.findParentType(fNodeToAssign);
		if (newTypeDecl == null) {
			return null;
		}

		Expression expression= isParamToField ? ((SingleVariableDeclaration) fNodeToAssign).getName() : ((ExpressionStatement) fNodeToAssign).getExpression();

		AST ast= newTypeDecl.getAST();
		ASTRewrite rewrite= ASTRewrite.create(ast);

		createImportRewrite((JavaScriptUnit) fNodeToAssign.getRoot());
		
		BodyDeclaration bodyDecl= ASTResolving.findParentBodyDeclaration(fNodeToAssign);
		Block body;
		if (bodyDecl instanceof FunctionDeclaration) {
			body= ((FunctionDeclaration) bodyDecl).getBody();
		} else if (bodyDecl instanceof Initializer) {
			body= ((Initializer) bodyDecl).getBody();
		} else {
			return null;
		}

		boolean isAnonymous= newTypeDecl.getNodeType() == ASTNode.ANONYMOUS_CLASS_DECLARATION;
		boolean isStatic= Modifier.isStatic(bodyDecl.getModifiers()) && !isAnonymous;
		boolean isConstructorParam= isParamToField && fNodeToAssign.getParent() instanceof FunctionDeclaration && ((FunctionDeclaration) fNodeToAssign.getParent()).isConstructor();
		int modifiers= Modifier.PRIVATE;
		if (isStatic) {
			modifiers |= Modifier.STATIC;
		} else if (isConstructorParam) {
			modifiers |= Modifier.FINAL;
		}

		VariableDeclarationFragment newDeclFrag= addFieldDeclaration(rewrite, newTypeDecl, modifiers, expression);
		String varName= newDeclFrag.getName().getIdentifier();

		Assignment assignment= ast.newAssignment();
		assignment.setRightHandSide((Expression) rewrite.createCopyTarget(expression));

		boolean needsThis= StubUtility.useThisForFieldAccess(getCompilationUnit().getJavaScriptProject());
		if (isParamToField) {
			needsThis |= varName.equals(((SimpleName) expression).getIdentifier());
		}

		SimpleName accessName= ast.newSimpleName(varName);
		if (needsThis) {
			FieldAccess fieldAccess= ast.newFieldAccess();
			fieldAccess.setName(accessName);
			if (isStatic) {
				String typeName= ((AbstractTypeDeclaration) newTypeDecl).getName().getIdentifier();
				fieldAccess.setExpression(ast.newSimpleName(typeName));
			} else {
				fieldAccess.setExpression(ast.newThisExpression());
			}
			assignment.setLeftHandSide(fieldAccess);
		} else {
			assignment.setLeftHandSide(accessName);
		}

		ASTNode selectionNode;
		if (isParamToField) {
			// assign parameter to field
			ExpressionStatement statement= ast.newExpressionStatement(assignment);
			int insertIdx=  findAssignmentInsertIndex(body.statements());
			rewrite.getListRewrite(body, Block.STATEMENTS_PROPERTY).insertAt(statement, insertIdx, null);
			selectionNode= statement;

		} else {
			rewrite.replace(expression, assignment, null);
			selectionNode= fNodeToAssign;
		}

		addLinkedPosition(rewrite.track(newDeclFrag.getName()), false, KEY_NAME);
		if (!isParamToField) {
			FieldDeclaration fieldDeclaration= (FieldDeclaration) newDeclFrag.getParent();
			addLinkedPosition(rewrite.track(fieldDeclaration.getType()), false, KEY_TYPE);
		}
		addLinkedPosition(rewrite.track(accessName), true, KEY_NAME);
		setEndPosition(rewrite.track(selectionNode));

		return rewrite;
	}

	private VariableDeclarationFragment addFieldDeclaration(ASTRewrite rewrite, ASTNode newTypeDecl, int modifiers, Expression expression) throws CoreException {
		if (fExistingFragment != null) {
			return fExistingFragment;
		}
		
		ChildListPropertyDescriptor property= ASTNodes.getBodyDeclarationsProperty(newTypeDecl);
		List decls= (List) newTypeDecl.getStructuralProperty(property);
		AST ast= newTypeDecl.getAST();
		String[] varNames= suggestFieldNames(fTypeBinding, expression, modifiers);
		for (int i= 0; i < varNames.length; i++) {
			addLinkedPositionProposal(KEY_NAME, varNames[i], null);
		}
		String varName= varNames[0];

		VariableDeclarationFragment newDeclFrag= ast.newVariableDeclarationFragment();
		newDeclFrag.setName(ast.newSimpleName(varName));

		FieldDeclaration newDecl= ast.newFieldDeclaration(newDeclFrag);

		Type type= evaluateType(ast);
		newDecl.setType(type);
		newDecl.modifiers().addAll(ASTNodeFactory.newModifiers(ast, modifiers));
		
		ModifierCorrectionSubProcessor.installLinkedVisibilityProposals(getLinkedProposalModel(), rewrite, newDecl.modifiers(), false);

		int insertIndex= findFieldInsertIndex(decls, fNodeToAssign.getStartPosition());
		rewrite.getListRewrite(newTypeDecl, property).insertAt(newDecl, insertIndex, null);
		
		return newDeclFrag;
	}
	
	
	private Type evaluateType(AST ast) throws CoreException {
		ITypeBinding[] proposals= ASTResolving.getRelaxingTypes(ast, fTypeBinding);
		for (int i= 0; i < proposals.length; i++) {
			addLinkedPositionProposal(KEY_TYPE, proposals[i]);
		}
		return getImportRewrite().addImport(fTypeBinding, ast);
	}

	private String[] suggestLocalVariableNames(ITypeBinding binding, Expression expression) {
		IJavaScriptProject project= getCompilationUnit().getJavaScriptProject();
		return StubUtility.getVariableNameSuggestions(StubUtility.LOCAL, project, binding, expression, getUsedVariableNames());
	}

	private String[] suggestFieldNames(ITypeBinding binding, Expression expression, int modifiers) {
		IJavaScriptProject project= getCompilationUnit().getJavaScriptProject();
		int varKind= Modifier.isStatic(modifiers) ? StubUtility.STATIC_FIELD : StubUtility.INSTANCE_FIELD;
		return StubUtility.getVariableNameSuggestions(varKind, project, binding, expression, getUsedVariableNames());
	}

	private Collection getUsedVariableNames() {
		return Arrays.asList(ASTResolving.getUsedVariableNames(fNodeToAssign));
	}

	private int findAssignmentInsertIndex(List statements) {

		HashSet paramsBefore= new HashSet();
		List params = ((FunctionDeclaration) fNodeToAssign.getParent()).parameters();
		for (int i = 0; i < params.size() && (params.get(i) != fNodeToAssign); i++) {
			SingleVariableDeclaration decl= (SingleVariableDeclaration) params.get(i);
			paramsBefore.add(decl.getName().getIdentifier());
		}

		int i= 0;
		for (i = 0; i < statements.size(); i++) {
			Statement curr= (Statement) statements.get(i);
			switch (curr.getNodeType()) {
				case ASTNode.CONSTRUCTOR_INVOCATION:
				case ASTNode.SUPER_CONSTRUCTOR_INVOCATION:
					break;
				case ASTNode.EXPRESSION_STATEMENT:
					Expression expr= ((ExpressionStatement) curr).getExpression();
					if (expr instanceof Assignment) {
						Assignment assignment= (Assignment) expr;
						Expression rightHand = assignment.getRightHandSide();
						if (rightHand instanceof SimpleName && paramsBefore.contains(((SimpleName) rightHand).getIdentifier())) {
							IVariableBinding binding = Bindings.getAssignedVariable(assignment);
							if (binding == null || binding.isField()) {
								break;
							}
						}
					}
					return i;
				default:
					return i;

			}
		}
		return i;

	}

	private int findFieldInsertIndex(List decls, int currPos) {
		for (int i= decls.size() - 1; i >= 0; i--) {
			ASTNode curr= (ASTNode) decls.get(i);
			if (curr instanceof FieldDeclaration && currPos > curr.getStartPosition() + curr.getLength()) {
				return i + 1;
			}
		}
		return 0;
	}

	/**
	 * Returns the variable kind.
	 * @return int
	 */
	public int getVariableKind() {
		return fVariableKind;
	}


}
