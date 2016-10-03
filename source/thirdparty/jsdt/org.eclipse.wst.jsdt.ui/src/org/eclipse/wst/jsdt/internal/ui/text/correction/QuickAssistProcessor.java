/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sebastian Davids <sdavids@gmx.de> - Bug 37432 getInvertEqualsProposal
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.text.correction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.swt.graphics.Image;
import org.eclipse.wst.jsdt.core.Flags;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.compiler.IProblem;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.wst.jsdt.core.dom.ArrayCreation;
import org.eclipse.wst.jsdt.core.dom.ArrayInitializer;
import org.eclipse.wst.jsdt.core.dom.ArrayType;
import org.eclipse.wst.jsdt.core.dom.Assignment;
import org.eclipse.wst.jsdt.core.dom.Block;
import org.eclipse.wst.jsdt.core.dom.BodyDeclaration;
import org.eclipse.wst.jsdt.core.dom.CatchClause;
import org.eclipse.wst.jsdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.wst.jsdt.core.dom.ClassInstanceCreation;
import org.eclipse.wst.jsdt.core.dom.ConditionalExpression;
import org.eclipse.wst.jsdt.core.dom.DoStatement;
import org.eclipse.wst.jsdt.core.dom.Expression;
import org.eclipse.wst.jsdt.core.dom.ExpressionStatement;
import org.eclipse.wst.jsdt.core.dom.ForInStatement;
import org.eclipse.wst.jsdt.core.dom.ForStatement;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.FunctionInvocation;
import org.eclipse.wst.jsdt.core.dom.IBinding;
import org.eclipse.wst.jsdt.core.dom.IFunctionBinding;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.IVariableBinding;
import org.eclipse.wst.jsdt.core.dom.IfStatement;
import org.eclipse.wst.jsdt.core.dom.InfixExpression;
import org.eclipse.wst.jsdt.core.dom.Initializer;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.Modifier;
import org.eclipse.wst.jsdt.core.dom.Name;
import org.eclipse.wst.jsdt.core.dom.ParenthesizedExpression;
import org.eclipse.wst.jsdt.core.dom.PrimitiveType;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.SimpleType;
import org.eclipse.wst.jsdt.core.dom.SingleVariableDeclaration;
import org.eclipse.wst.jsdt.core.dom.Statement;
import org.eclipse.wst.jsdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.wst.jsdt.core.dom.ThisExpression;
import org.eclipse.wst.jsdt.core.dom.TryStatement;
import org.eclipse.wst.jsdt.core.dom.Type;
import org.eclipse.wst.jsdt.core.dom.TypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.VariableDeclaration;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationExpression;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationStatement;
import org.eclipse.wst.jsdt.core.dom.WhileStatement;
import org.eclipse.wst.jsdt.core.dom.WithStatement;
import org.eclipse.wst.jsdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.wst.jsdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.wst.jsdt.core.dom.rewrite.ListRewrite;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.corext.dom.Bindings;
import org.eclipse.wst.jsdt.internal.corext.dom.LinkedNodeFinder;
import org.eclipse.wst.jsdt.internal.corext.dom.Selection;
import org.eclipse.wst.jsdt.internal.corext.dom.SelectionAnalyzer;
import org.eclipse.wst.jsdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.wst.jsdt.internal.corext.fix.ControlStatementsFix;
import org.eclipse.wst.jsdt.internal.corext.fix.ConvertLoopFix;
import org.eclipse.wst.jsdt.internal.corext.fix.IFix;
import org.eclipse.wst.jsdt.internal.corext.fix.LinkedProposalModel;
import org.eclipse.wst.jsdt.internal.corext.fix.VariableDeclarationFix;
import org.eclipse.wst.jsdt.internal.corext.refactoring.code.ConvertAnonymousToNestedRefactoring;
import org.eclipse.wst.jsdt.internal.corext.refactoring.code.ExtractConstantRefactoring;
import org.eclipse.wst.jsdt.internal.corext.refactoring.code.ExtractTempRefactoring;
import org.eclipse.wst.jsdt.internal.corext.refactoring.code.InlineTempRefactoring;
import org.eclipse.wst.jsdt.internal.corext.refactoring.code.PromoteTempToFieldRefactoring;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.fix.ControlStatementsCleanUp;
import org.eclipse.wst.jsdt.internal.ui.fix.ConvertLoopCleanUp;
import org.eclipse.wst.jsdt.internal.ui.fix.ICleanUp;
import org.eclipse.wst.jsdt.internal.ui.fix.VariableDeclarationCleanUp;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.JavaElementImageProvider;
import org.eclipse.wst.jsdt.ui.text.java.IInvocationContext;
import org.eclipse.wst.jsdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.wst.jsdt.ui.text.java.IProblemLocation;
import org.eclipse.wst.jsdt.ui.text.java.IQuickAssistProcessor;

/**
  */
public class QuickAssistProcessor implements IQuickAssistProcessor {

	public static final String SPLIT_JOIN_VARIABLE_DECLARATION_ID= "org.eclipse.wst.jsdt.ui.correction.splitJoinVariableDeclaration.assist"; //$NON-NLS-1$
	public static final String CONVERT_FOR_LOOP_ID= "org.eclipse.wst.jsdt.ui.correction.convertForLoop.assist"; //$NON-NLS-1$
	public static final String ASSIGN_TO_LOCAL_ID= "org.eclipse.wst.jsdt.ui.correction.assignToLocal.assist"; //$NON-NLS-1$
	public static final String ASSIGN_TO_FIELD_ID= "org.eclipse.wst.jsdt.ui.correction.assignToField.assist"; //$NON-NLS-1$
	public static final String ASSIGN_PARAM_TO_FIELD_ID= "org.eclipse.wst.jsdt.ui.correction.assignParamToField.assist"; //$NON-NLS-1$
	public static final String ADD_BLOCK_ID= "org.eclipse.wst.jsdt.ui.correction.addBlock.assist"; //$NON-NLS-1$
	public static final String EXTRACT_LOCAL_ID= "org.eclipse.wst.jsdt.ui.correction.extractLocal.assist"; //$NON-NLS-1$
	public static final String EXTRACT_CONSTANT_ID= "org.eclipse.wst.jsdt.ui.correction.extractConstant.assist"; //$NON-NLS-1$
	public static final String INLINE_LOCAL_ID= "org.eclipse.wst.jsdt.ui.correction.inlineLocal.assist"; //$NON-NLS-1$
	public static final String CONVERT_LOCAL_TO_FIELD_ID= "org.eclipse.wst.jsdt.ui.correction.convertLocalToField.assist"; //$NON-NLS-1$
	public static final String CONVERT_ANONYMOUS_TO_LOCAL_ID= "org.eclipse.wst.jsdt.ui.correction.convertAnonymousToLocal.assist"; //$NON-NLS-1$
	
	public QuickAssistProcessor() {
		super();
	}

	public boolean hasAssists(IInvocationContext context) throws CoreException {
		ASTNode coveringNode= context.getCoveringNode();
		if (coveringNode != null) {
			return getCatchClauseToThrowsProposals(context, coveringNode, null)
				|| getRenameLocalProposals(context, coveringNode, null, false, null)
				|| getAssignToVariableProposals(context, coveringNode, null)
				|| getUnWrapProposals(context, coveringNode, null)
				|| getAssignParamToFieldProposals(context, coveringNode, null)
				|| getJoinVariableProposals(context, coveringNode, null)
				|| getAddFinallyProposals(context, coveringNode, null)
				|| getAddElseProposals(context, coveringNode, null)
				|| getSplitVariableProposals(context, coveringNode, null)
				|| getAddBlockProposals(context, coveringNode, null)
				|| getArrayInitializerToArrayCreation(context, coveringNode, null)
				|| getCreateInSuperClassProposals(context, coveringNode, null)
				|| getInvertEqualsProposal(context, coveringNode, null)
				|| getExtractLocalProposal(context, coveringNode, null)
				|| getInlineLocalProposal(context, coveringNode, null)
				|| getConvertLocalToFieldProposal(context, coveringNode, null)
				|| getConvertAnonymousToNestedProposal(context, coveringNode, null)
				|| getRemoveBlockProposals(context, coveringNode, null)
				|| getMakeVariableDeclarationFinalProposals(context, coveringNode, null);
		}
		return false;
	}

	public IJavaCompletionProposal[] getAssists(IInvocationContext context, IProblemLocation[] locations) throws CoreException {
		ASTNode coveringNode= context.getCoveringNode();
		if (coveringNode != null) {
			ArrayList resultingCollections= new ArrayList();
			boolean noErrorsAtLocation= noErrorsAtLocation(locations);
			
			// quick assists that show up also if there is an error/warning
			getRenameLocalProposals(context, coveringNode, locations, noErrorsAtLocation, resultingCollections);
			getAssignToVariableProposals(context, coveringNode, resultingCollections);
			getAssignParamToFieldProposals(context, coveringNode, resultingCollections);
			
			if (noErrorsAtLocation) {
				getCatchClauseToThrowsProposals(context, coveringNode, resultingCollections);
				getUnWrapProposals(context, coveringNode, resultingCollections);
				getSplitVariableProposals(context, coveringNode, resultingCollections);
				getJoinVariableProposals(context, coveringNode, resultingCollections);
				getAddFinallyProposals(context, coveringNode, resultingCollections);
				getAddElseProposals(context, coveringNode, resultingCollections);
				getAddBlockProposals(context, coveringNode, resultingCollections);
				getInvertEqualsProposal(context, coveringNode, resultingCollections);
				getArrayInitializerToArrayCreation(context, coveringNode, resultingCollections);
				getCreateInSuperClassProposals(context, coveringNode, resultingCollections);
				getExtractLocalProposal(context, coveringNode, resultingCollections);
				getInlineLocalProposal(context, coveringNode, resultingCollections);
				getConvertLocalToFieldProposal(context, coveringNode, resultingCollections);				
				getConvertAnonymousToNestedProposal(context, coveringNode, resultingCollections);
				getRemoveBlockProposals(context, coveringNode, resultingCollections);
				getMakeVariableDeclarationFinalProposals(context, coveringNode, resultingCollections);
			}
			return (IJavaCompletionProposal[]) resultingCollections.toArray(new IJavaCompletionProposal[resultingCollections.size()]);
		}
		return null;
	}

	private boolean noErrorsAtLocation(IProblemLocation[] locations) {
		if (locations != null) {
			for (int i= 0; i < locations.length; i++) {
				if (locations[i].isError()) {
					return false;
				}
			}
		}
		return true;
	}
	
	private static boolean getExtractLocalProposal(IInvocationContext context, ASTNode covering, Collection proposals) throws CoreException {
		ASTNode node= context.getCoveredNode();
		
		if (!(node instanceof Expression)) {
			return false;
		}
		final Expression expression= (Expression) node;
		
		ITypeBinding binding= expression.resolveTypeBinding();
		if (binding == null || Bindings.isVoidType(binding)) {
			return false;
		}
		if (proposals == null) {
			return true;
		}
		
		final IJavaScriptUnit cu= context.getCompilationUnit();
		final ExtractTempRefactoring extractTempRefactoring= new ExtractTempRefactoring(context.getASTRoot(), expression.getStartPosition(), expression.getLength());
		if (extractTempRefactoring.checkInitialConditions(new NullProgressMonitor()).isOK()) {
			String label= CorrectionMessages.QuickAssistProcessor_extract_to_local_description;
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_LOCAL);
			CUCorrectionProposal proposal= new CUCorrectionProposal(label, cu, 5, image) {
				protected TextChange createTextChange() throws CoreException {
					extractTempRefactoring.setTempName(extractTempRefactoring.guessTempName()); // expensive
					extractTempRefactoring.setLinkedProposalModel(getLinkedProposalModel());
					return extractTempRefactoring.createTextChange(new NullProgressMonitor());
				}
			};
			proposal.setCommandId(EXTRACT_LOCAL_ID);
			proposals.add(proposal);
		}
		final ExtractConstantRefactoring extractConstRefactoring= new ExtractConstantRefactoring(context.getASTRoot(), expression.getStartPosition(), expression.getLength());
		if (extractConstRefactoring.checkInitialConditions(new NullProgressMonitor()).isOK()) {
			String label= CorrectionMessages.QuickAssistProcessor_extract_to_constant_description;
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_LOCAL);
			CUCorrectionProposal proposal= new CUCorrectionProposal(label, cu, 4, image) {
				protected TextChange createTextChange() throws CoreException {
					extractConstRefactoring.setConstantName(extractConstRefactoring.guessConstantName()); // expensive
					extractConstRefactoring.setLinkedProposalModel(getLinkedProposalModel());
					return extractConstRefactoring.createTextChange(new NullProgressMonitor());
				}
			};
			proposal.setCommandId(EXTRACT_CONSTANT_ID);
			proposals.add(proposal);
		}
		return false;
	}
	

	private static boolean getConvertAnonymousToNestedProposal(IInvocationContext context, final ASTNode node, Collection proposals) throws CoreException {
		if (!(node instanceof Name))
			return false;
		
		ASTNode normalized= ASTNodes.getNormalizedNode(node);
		if (normalized.getLocationInParent() != ClassInstanceCreation.TYPE_PROPERTY)
			return false;
		
		final AnonymousClassDeclaration anonymTypeDecl= ((ClassInstanceCreation) normalized.getParent()).getAnonymousClassDeclaration();
		if (anonymTypeDecl == null || anonymTypeDecl.resolveBinding() == null) {
			return false;
		}
		
		if (proposals == null) {
			return true;
		}

		final IJavaScriptUnit cu= context.getCompilationUnit();
		final ConvertAnonymousToNestedRefactoring refactoring= new ConvertAnonymousToNestedRefactoring(anonymTypeDecl);
		String extTypeName= ASTNodes.getSimpleNameIdentifier((Name) node);
		
		refactoring.setClassName(Messages.format(CorrectionMessages.QuickAssistProcessor_name_extension_from_interface, extTypeName));

		if (refactoring.checkInitialConditions(new NullProgressMonitor()).isOK()) {
			LinkedProposalModel linkedProposalModel= new LinkedProposalModel();
			refactoring.setLinkedProposalModel(linkedProposalModel);
			
			String label= CorrectionMessages.QuickAssistProcessor_convert_anonym_to_nested;
			Image image= JavaScriptPlugin.getImageDescriptorRegistry().get(JavaElementImageProvider.getTypeImageDescriptor(true, false, Flags.AccPrivate, false));
			RefactoringCorrectionProposal proposal= new RefactoringCorrectionProposal(label, cu, refactoring, 5, image);
			proposal.setLinkedProposalModel(linkedProposalModel);
			proposal.setCommandId(CONVERT_ANONYMOUS_TO_LOCAL_ID);
			proposals.add(proposal);
		}
		return false;
	}

	private static boolean getJoinVariableProposals(IInvocationContext context, ASTNode node, Collection resultingCollections) {
		ASTNode parent= node.getParent();
		
		VariableDeclarationFragment fragment= null;
		boolean onFirstAccess= false;
		if (node instanceof SimpleName && node.getLocationInParent() == Assignment.LEFT_HAND_SIDE_PROPERTY) {
			onFirstAccess= true;
			SimpleName name= (SimpleName) node;
			IBinding binding= name.resolveBinding();
			if (!(binding instanceof IVariableBinding)) {
				return false;
			}
			ASTNode declaring= context.getASTRoot().findDeclaringNode(binding);
			if (declaring instanceof VariableDeclarationFragment) {
				fragment= (VariableDeclarationFragment) declaring;
			} else {
				return false;
			}
		} else if (parent instanceof VariableDeclarationFragment) {
			fragment= (VariableDeclarationFragment) parent;
		} else {
			return false;
		}

		IVariableBinding binding= fragment.resolveBinding();
		if (fragment.getInitializer() != null || binding == null || binding.isField()) {
			return false;
		}

		if (!(fragment.getParent() instanceof VariableDeclarationStatement)) {
			return false;
		}
		VariableDeclarationStatement statement= (VariableDeclarationStatement) fragment.getParent();

		SimpleName[] names= LinkedNodeFinder.findByBinding(statement.getParent(), binding);
		if (names.length <= 1 || names[0] != fragment.getName()) {
			return false;
		}
		SimpleName firstAccess= names[1];
		if (onFirstAccess) {
			if (firstAccess != node) {
				return false;
			}
		} else {
			if (firstAccess.getLocationInParent() != Assignment.LEFT_HAND_SIDE_PROPERTY) {
				return false;
			}
		}
		Assignment assignment= (Assignment) firstAccess.getParent();
		if (assignment.getLocationInParent() != ExpressionStatement.EXPRESSION_PROPERTY) {
			return false;
		}
		ExpressionStatement assignParent= (ExpressionStatement) assignment.getParent();

		if (resultingCollections == null) {
			return true;
		}

		AST ast= statement.getAST();
		ASTRewrite rewrite= ASTRewrite.create(ast);

		String label= CorrectionMessages.QuickAssistProcessor_joindeclaration_description;
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_LOCAL);
		LinkedCorrectionProposal proposal= new LinkedCorrectionProposal(label, context.getCompilationUnit(), rewrite, 1, image);
		proposal.setCommandId(SPLIT_JOIN_VARIABLE_DECLARATION_ID);
		
		Expression placeholder= (Expression) rewrite.createMoveTarget(assignment.getRightHandSide());
		rewrite.set(fragment, VariableDeclarationFragment.INITIALIZER_PROPERTY, placeholder, null);

		
		if (onFirstAccess) {
			// replace assignment with variable declaration
			rewrite.replace(assignParent, rewrite.createMoveTarget(statement), null);
		} else {
			// different scopes -> remove assignments, set variable initializer
			if (ASTNodes.isControlStatementBody(assignParent.getLocationInParent())) {
				Block block= ast.newBlock();
				rewrite.replace(assignParent, block, null);
			} else {
				rewrite.remove(assignParent, null);
			}
		}

		proposal.setEndPosition(rewrite.track(fragment.getName()));
		resultingCollections.add(proposal);
		return true;

	}

	private static boolean getSplitVariableProposals(IInvocationContext context, ASTNode node, Collection resultingCollections) {
		VariableDeclarationFragment fragment;
		if (node instanceof VariableDeclarationFragment) {
			fragment= (VariableDeclarationFragment) node;
		} else if (node.getLocationInParent() == VariableDeclarationFragment.NAME_PROPERTY) {
			fragment= (VariableDeclarationFragment) node.getParent();
		} else {
			return false;
		}

		if (fragment.getInitializer() == null) {
			return false;
		}

		Statement statement;
		ASTNode fragParent= fragment.getParent();
		if (fragParent instanceof VariableDeclarationStatement) {
			statement= (VariableDeclarationStatement) fragParent;
		} else if (fragParent instanceof VariableDeclarationExpression) {
			statement= (Statement) fragParent.getParent();
		} else {
			return false;
		}
		// statement is ForStatement or VariableDeclarationStatement

		ASTNode statementParent= statement.getParent();
		StructuralPropertyDescriptor property= statement.getLocationInParent();
		if (!property.isChildListProperty()) {
			return false;
		}

		List list= (List) statementParent.getStructuralProperty(property);

		if (resultingCollections == null) {
			return true;
		}

		AST ast= statement.getAST();
		ASTRewrite rewrite= ASTRewrite.create(ast);

		String label= CorrectionMessages.QuickAssistProcessor_splitdeclaration_description;
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_LOCAL);
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, 1, image);
		proposal.setCommandId(SPLIT_JOIN_VARIABLE_DECLARATION_ID);
		
		Statement newStatement;
		int insertIndex= list.indexOf(statement);

		Expression placeholder= (Expression) rewrite.createMoveTarget(fragment.getInitializer());
		ITypeBinding binding= fragment.getInitializer().resolveTypeBinding();
		if (placeholder instanceof ArrayInitializer && binding != null && binding.isArray()) {
			ArrayCreation creation= ast.newArrayCreation();
			creation.setInitializer((ArrayInitializer) placeholder);
			final ITypeBinding componentType= binding.getElementType();
			Type type= null;
			if (componentType.isPrimitive())
				type= ast.newPrimitiveType(PrimitiveType.toCode(componentType.getName()));
			else
				type= ast.newSimpleType(ast.newSimpleName(componentType.getName()));
			creation.setType(ast.newArrayType(type, binding.getDimensions()));
			placeholder= creation;
		}
		Assignment assignment= ast.newAssignment();
		assignment.setRightHandSide(placeholder);
		assignment.setLeftHandSide(ast.newSimpleName(fragment.getName().getIdentifier()));

		if (statement instanceof VariableDeclarationStatement) {
			newStatement= ast.newExpressionStatement(assignment);
			insertIndex+= 1; // add after declaration
		} else {
			rewrite.replace(fragment.getParent(), assignment, null);
			VariableDeclarationFragment newFrag= ast.newVariableDeclarationFragment();
			newFrag.setName(ast.newSimpleName(fragment.getName().getIdentifier()));
			newFrag.setExtraDimensions(fragment.getExtraDimensions());

			VariableDeclarationExpression oldVarDecl= (VariableDeclarationExpression) fragParent;

			VariableDeclarationStatement newVarDec= ast.newVariableDeclarationStatement(newFrag);
			newVarDec.setType((Type) ASTNode.copySubtree(ast, oldVarDecl.getType()));
			newVarDec.modifiers().addAll(ASTNodeFactory.newModifiers(ast, oldVarDecl.getModifiers()));
			newStatement= newVarDec;
		}

		ListRewrite listRewriter= rewrite.getListRewrite(statementParent, (ChildListPropertyDescriptor) property);
		listRewriter.insertAt(newStatement, insertIndex, null);

		resultingCollections.add(proposal);
		return true;
	}

	private static boolean getAssignToVariableProposals(IInvocationContext context, ASTNode node, Collection resultingCollections) {
		Statement statement= ASTResolving.findParentStatement(node);
		if (!(statement instanceof ExpressionStatement)) {
			return false;
		}
		ExpressionStatement expressionStatement= (ExpressionStatement) statement;

		Expression expression= expressionStatement.getExpression();
		if (expression.getNodeType() == ASTNode.ASSIGNMENT) {
			return false; // too confusing and not helpful
		}

		ITypeBinding typeBinding= expression.resolveTypeBinding();
		typeBinding= Bindings.normalizeTypeBinding(typeBinding);
		if (typeBinding == null) {
			return false;
		}
		if (resultingCollections == null) {
			return true;
		}

		IJavaScriptUnit cu= context.getCompilationUnit();

		AssignToVariableAssistProposal localProposal= new AssignToVariableAssistProposal(cu, AssignToVariableAssistProposal.LOCAL, expressionStatement, typeBinding, 2);
		localProposal.setCommandId(ASSIGN_TO_LOCAL_ID);
		resultingCollections.add(localProposal);

		ASTNode type= ASTResolving.findParentType(expression);
		if (type != null) {
			AssignToVariableAssistProposal fieldProposal= new AssignToVariableAssistProposal(cu, AssignToVariableAssistProposal.FIELD, expressionStatement, typeBinding, 1);
			fieldProposal.setCommandId(ASSIGN_TO_FIELD_ID);
			resultingCollections.add(fieldProposal);
		}
		return false;

	}

	private static boolean getAssignParamToFieldProposals(IInvocationContext context, ASTNode node, Collection resultingCollections) {
		node= ASTNodes.getNormalizedNode(node);
		ASTNode parent= node.getParent();
		if (!(parent instanceof SingleVariableDeclaration) || !(parent.getParent() instanceof FunctionDeclaration)) {
			return false;
		}
		SingleVariableDeclaration paramDecl= (SingleVariableDeclaration) parent;
		IVariableBinding binding= paramDecl.resolveBinding();

		FunctionDeclaration methodDecl= (FunctionDeclaration) parent.getParent();
		if (binding == null || methodDecl.getBody() == null) {
			return false;
		}
		ITypeBinding typeBinding= binding.getType();
		if (typeBinding == null) {
			return false;
		}
		
		if (resultingCollections == null) {
			return true;
		}
		
		ITypeBinding parentType= Bindings.getBindingOfParentType(node);
		if (parentType != null) {
			// assign to existing fields
			JavaScriptUnit root= context.getASTRoot();
			IVariableBinding[] declaredFields= parentType.getDeclaredFields();
			boolean isStaticContext= ASTResolving.isInStaticContext(node);
			for (int i= 0; i < declaredFields.length; i++) {
				IVariableBinding curr= declaredFields[i];
				if (isStaticContext == Modifier.isStatic(curr.getModifiers()) && typeBinding.isAssignmentCompatible(curr.getType())) {
					ASTNode fieldDeclFrag= root.findDeclaringNode(curr);
					if (fieldDeclFrag instanceof VariableDeclarationFragment) {
						VariableDeclarationFragment fragment= (VariableDeclarationFragment) fieldDeclFrag;
						if (fragment.getInitializer() == null) {
							resultingCollections.add(new AssignToVariableAssistProposal(context.getCompilationUnit(), paramDecl, fragment, typeBinding, 1));
						}
					}
				}
			}
		}

		AssignToVariableAssistProposal fieldProposal= new AssignToVariableAssistProposal(context.getCompilationUnit(), paramDecl, null, typeBinding, 3);
		fieldProposal.setCommandId(ASSIGN_PARAM_TO_FIELD_ID);
		resultingCollections.add(fieldProposal);
		return true;
	}

	private static boolean getAddFinallyProposals(IInvocationContext context, ASTNode node, Collection resultingCollections) {
		TryStatement tryStatement= ASTResolving.findParentTryStatement(node);
		if (tryStatement == null || tryStatement.getFinally() != null) {
			return false;
		}
		Statement statement= ASTResolving.findParentStatement(node);
		if (tryStatement != statement && tryStatement.getBody() != statement) {
			return false; // an node inside a catch or finally block
		}

		if (resultingCollections == null) {
			return true;
		}

		AST ast= tryStatement.getAST();
		ASTRewrite rewrite= ASTRewrite.create(ast);
		Block finallyBody= ast.newBlock();

		rewrite.set(tryStatement, TryStatement.FINALLY_PROPERTY, finallyBody, null);

		String label= CorrectionMessages.QuickAssistProcessor_addfinallyblock_description;
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_ADD);
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, 1, image);
		resultingCollections.add(proposal);
		return true;
	}

	private static boolean getAddElseProposals(IInvocationContext context, ASTNode node, Collection resultingCollections) {
		Statement statement= ASTResolving.findParentStatement(node);
		if (!(statement instanceof IfStatement)) {
			return false;
		}
		IfStatement ifStatement= (IfStatement) statement;
		if (ifStatement.getElseStatement() != null) {
			return false;
		}

		if (resultingCollections == null) {
			return true;
		}

		AST ast= statement.getAST();
		ASTRewrite rewrite= ASTRewrite.create(ast);
		Block body= ast.newBlock();

		rewrite.set(ifStatement, IfStatement.ELSE_STATEMENT_PROPERTY, body, null);

		String label= CorrectionMessages.QuickAssistProcessor_addelseblock_description;
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_ADD);
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, 1, image);
		resultingCollections.add(proposal);
		return true;
	}

	public static boolean getCatchClauseToThrowsProposals(IInvocationContext context, ASTNode node, Collection resultingCollections) {
		CatchClause catchClause= (CatchClause) ASTResolving.findAncestor(node, ASTNode.CATCH_CLAUSE);
		if (catchClause == null) {
			return false;
		}

		Statement statement= ASTResolving.findParentStatement(node);
		if (statement != catchClause.getParent() && statement != catchClause.getBody()) {
			return false; // selection is in a statement inside the body
		}

		Type type= catchClause.getException().getType();
		if (!type.isSimpleType()) {
			return false;
		}

		BodyDeclaration bodyDeclaration= ASTResolving.findParentBodyDeclaration(catchClause);
		if (!(bodyDeclaration instanceof FunctionDeclaration) && !(bodyDeclaration instanceof Initializer)) {
			return false;
		}

		if (resultingCollections == null) {
			return true;
		}

		AST ast= bodyDeclaration.getAST();

		if (bodyDeclaration instanceof FunctionDeclaration) {
			FunctionDeclaration methodDeclaration= (FunctionDeclaration) bodyDeclaration;

			ASTRewrite rewrite= ASTRewrite.create(ast);

			removeCatchBlock(rewrite, catchClause);

			ITypeBinding binding= type.resolveBinding();
			if (binding == null || isNotYetThrown(binding, methodDeclaration.thrownExceptions())) {
				Name name= ((SimpleType) type).getName();
				Name newName= (Name) ASTNode.copySubtree(ast, name);

				ListRewrite listRewriter= rewrite.getListRewrite(methodDeclaration, FunctionDeclaration.THROWN_EXCEPTIONS_PROPERTY);
				listRewriter.insertLast(newName, null);
			}

			String label= CorrectionMessages.QuickAssistProcessor_catchclausetothrows_description;
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_EXCEPTION);
			ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, 4, image);
			resultingCollections.add(proposal);
		}
		{  // for initializers or method declarations
			ASTRewrite rewrite= ASTRewrite.create(ast);

			removeCatchBlock(rewrite, catchClause);
			String label= CorrectionMessages.QuickAssistProcessor_removecatchclause_description;
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_EXCEPTION);
			ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, 5, image);
			resultingCollections.add(proposal);
		}

		return true;
	}

	private static void removeCatchBlock(ASTRewrite rewrite, CatchClause catchClause) {
		TryStatement tryStatement= (TryStatement) catchClause.getParent();
		if (tryStatement.catchClauses().size() > 1 || tryStatement.getFinally() != null) {
			rewrite.remove(catchClause, null);
		} else {
			Block block= tryStatement.getBody();
			List statements= block.statements();
			int nStatements= statements.size();
			if (nStatements == 1) {
				ASTNode first= (ASTNode) statements.get(0);
				rewrite.replace(tryStatement, rewrite.createCopyTarget(first), null);
			} else if (nStatements > 1) {
				ListRewrite listRewrite= rewrite.getListRewrite(block, Block.STATEMENTS_PROPERTY);
				ASTNode first= (ASTNode) statements.get(0);
				ASTNode last= (ASTNode) statements.get(statements.size() - 1);
				ASTNode newStatement= listRewrite.createCopyTarget(first, last);
				if (ASTNodes.isControlStatementBody(tryStatement.getLocationInParent())) {
					Block newBlock= rewrite.getAST().newBlock();
					newBlock.statements().add(newStatement);
					newStatement= newBlock;
				}
				rewrite.replace(tryStatement, newStatement, null);
			} else {
				rewrite.remove(tryStatement, null);
			}
		}
	}

	private static boolean isNotYetThrown(ITypeBinding binding, List thrownExcpetions) {
		for (int i= 0; i < thrownExcpetions.size(); i++) {
			Name name= (Name) thrownExcpetions.get(i);
			ITypeBinding elem= (ITypeBinding) name.resolveBinding();
			if (elem != null) {
				if (Bindings.isSuperType(elem, binding)) { // existing exception is base class of new
					return false;
				}
			}
		}
		return true;
	}


	private static boolean getRenameLocalProposals(IInvocationContext context, ASTNode node, IProblemLocation[] locations, boolean noErrorsAtLocation, Collection resultingCollections) {
		if (!(node instanceof SimpleName)) {
			return false;
		}
		SimpleName name= (SimpleName) node;
		IBinding binding= name.resolveBinding();
		if (binding != null && binding.getKind() == IBinding.PACKAGE) {
			return false;
		}

		if (locations != null) {
			for (int i= 0; i < locations.length; i++) {
				switch (locations[i].getProblemId()) {
					case IProblem.LocalVariableHidingLocalVariable:
					case IProblem.LocalVariableHidingField:
					case IProblem.FieldHidingLocalVariable:
					case IProblem.FieldHidingField:
					case IProblem.ArgumentHidingLocalVariable:
					case IProblem.ArgumentHidingField:
						return false;
				}
			}
		}

		if (resultingCollections == null) {
			return true;
		}

		LinkedNamesAssistProposal proposal= new LinkedNamesAssistProposal(context.getCompilationUnit(), name);
		if (!noErrorsAtLocation) {
			proposal.setRelevance(1);
		}
		
		resultingCollections.add(proposal);
		return true;
	}

	public static ASTNode getCopyOfInner(ASTRewrite rewrite, ASTNode statement, boolean toControlStatementBody) {
		if (statement.getNodeType() == ASTNode.BLOCK) {
			Block block= (Block) statement;
			List innerStatements= block.statements();
			int nStatements= innerStatements.size();
			if (nStatements == 1) {
				return rewrite.createCopyTarget(((ASTNode) innerStatements.get(0)));
			} else if (nStatements > 1) {
				if (toControlStatementBody) {
					return rewrite.createCopyTarget(block);
				}
				ListRewrite listRewrite= rewrite.getListRewrite(block, Block.STATEMENTS_PROPERTY);
				ASTNode first= (ASTNode) innerStatements.get(0);
				ASTNode last= (ASTNode) innerStatements.get(nStatements - 1);
				return listRewrite.createCopyTarget(first, last);
			}
			return null;
		} else {
			return rewrite.createCopyTarget(statement);
		}
	}


	private static boolean getUnWrapProposals(IInvocationContext context, ASTNode node, Collection resultingCollections) {
		ASTNode outer= node;

		Block block= null;
		if (outer.getNodeType() == ASTNode.BLOCK) {
			block= (Block) outer;
			outer= block.getParent();
		}

		ASTNode body= null;
		String label= null;
		if (outer instanceof IfStatement) {
			IfStatement ifStatement= (IfStatement) outer;
			Statement elseBlock= ifStatement.getElseStatement();
			if (elseBlock == null || ((elseBlock instanceof Block) && ((Block) elseBlock).statements().isEmpty())) {
				body= ifStatement.getThenStatement();
			}
			label= CorrectionMessages.QuickAssistProcessor_unwrap_ifstatement;
		} else if (outer instanceof WhileStatement) {
			body=((WhileStatement) outer).getBody();
			label= CorrectionMessages.QuickAssistProcessor_unwrap_whilestatement;
		} else if (outer instanceof ForStatement) {
			body=((ForStatement) outer).getBody();
			label= CorrectionMessages.QuickAssistProcessor_unwrap_forstatement;
		} else if (outer instanceof DoStatement) {
			body=((DoStatement) outer).getBody();
			label= CorrectionMessages.QuickAssistProcessor_unwrap_dostatement;
		} else if (outer instanceof TryStatement) {
			TryStatement tryStatement= (TryStatement) outer;
			if (tryStatement.catchClauses().isEmpty()) {
				body= tryStatement.getBody();
			}
			label= CorrectionMessages.QuickAssistProcessor_unwrap_trystatement;
		} else if (outer instanceof AnonymousClassDeclaration) {
			List decls= ((AnonymousClassDeclaration) outer).bodyDeclarations();
			for (int i= 0; i < decls.size(); i++) {
				ASTNode elem= (ASTNode) decls.get(i);
				if (elem instanceof FunctionDeclaration) {
					Block curr= ((FunctionDeclaration) elem).getBody();
					if (curr != null && !curr.statements().isEmpty()) {
						if (body != null) {
							return false;
						}
						body= curr;
					}
				} else if (elem instanceof TypeDeclaration) {
					return false;
				}
			}
			label= CorrectionMessages.QuickAssistProcessor_unwrap_anonymous;
			outer= ASTResolving.findParentStatement(outer);
			if (outer == null) {
				return false; // private Object o= new Object() { ... };
			}
		} else if (outer instanceof Block) {
			//	-> a block in a block
			body= block;
			outer= block;
			label= CorrectionMessages.QuickAssistProcessor_unwrap_block;
		} else if (outer instanceof ParenthesizedExpression) {
			//ParenthesizedExpression expression= (ParenthesizedExpression) outer;
			//body= expression.getExpression();
			//label= CorrectionMessages.getString("QuickAssistProcessor.unwrap.parenthesis");	 //$NON-NLS-1$
		} else if (outer instanceof FunctionInvocation) {
			FunctionInvocation invocation= (FunctionInvocation) outer;
			if (invocation.arguments().size() == 1) {
				body= (ASTNode) invocation.arguments().get(0);
				if (invocation.getParent().getNodeType() == ASTNode.EXPRESSION_STATEMENT) {
					int kind= body.getNodeType();
					if (kind != ASTNode.ASSIGNMENT && kind != ASTNode.PREFIX_EXPRESSION && kind != ASTNode.POSTFIX_EXPRESSION
							&& kind != ASTNode.FUNCTION_INVOCATION && kind != ASTNode.SUPER_METHOD_INVOCATION) {
						body= null;
					}
				}
				label= CorrectionMessages.QuickAssistProcessor_unwrap_methodinvocation;
			}
		}
		if (body == null) {
			return false;
		}
		ASTRewrite rewrite= ASTRewrite.create(outer.getAST());
		ASTNode inner= getCopyOfInner(rewrite, body, ASTNodes.isControlStatementBody(outer.getLocationInParent()));
		if (inner == null) {
			return false;
		}
		if (resultingCollections == null) {
			return true;
		}

		rewrite.replace(outer, inner, null);
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_EXCEPTION);
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, 1, image);
		resultingCollections.add(proposal);
		return true;
	}

	private static boolean isControlStatementWithBlock(ASTNode node) {
		switch (node.getNodeType()) {
			case ASTNode.IF_STATEMENT:
			case ASTNode.WHILE_STATEMENT:
			case ASTNode.FOR_STATEMENT:
			case ASTNode.FOR_IN_STATEMENT:
			case ASTNode.DO_STATEMENT:
				return true;
			default:
				return false;
		}
	}

	private static boolean getRemoveBlockProposals(IInvocationContext context, ASTNode coveringNode, Collection resultingCollections) {
		IFix[] fixes= ControlStatementsFix.createRemoveBlockFix(context.getASTRoot(), coveringNode);
		if (fixes != null) {
			if (resultingCollections == null) {
				return true;
			}
			Map options= new Hashtable();
			options.put(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS, CleanUpConstants.TRUE);
			options.put(CleanUpConstants.CONTROL_STATMENTS_USE_BLOCKS_NEVER, CleanUpConstants.TRUE);
			ICleanUp cleanUp= new ControlStatementsCleanUp(options);
			for (int i= 0; i < fixes.length; i++) {
				IFix fix= fixes[i];
				Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
				FixCorrectionProposal proposal= new FixCorrectionProposal(fix, cleanUp, 0, image, context);
				resultingCollections.add(proposal);
			}
			return true;
		}
		return false;
	}

	private static boolean getAddBlockProposals(IInvocationContext context, ASTNode node, Collection resultingCollections) {
		Statement statement= ASTResolving.findParentStatement(node);
		if (statement == null) {
			return false;
		}

		if (!isControlStatementWithBlock(statement)) {
			if (!isControlStatementWithBlock(statement.getParent())) {
				return false;
			}
			int statementStart= statement.getStartPosition();
			int statementEnd= statementStart + statement.getLength();

			int offset= context.getSelectionOffset();
			int length= context.getSelectionLength();
			if (length == 0) {
				if (offset != statementEnd) { // cursor at end
					return false;
				}
			} else {
				if (offset > statementStart || offset + length < statementEnd) { // statement selected
					return false;
				}
			}
			statement= (Statement) statement.getParent();
		}

		StructuralPropertyDescriptor childProperty= null;
		ASTNode child= null;
		switch (statement.getNodeType()) {
			case ASTNode.IF_STATEMENT:
				int selectionStart= context.getSelectionOffset();
				int selectionEnd= context.getSelectionOffset() + context.getSelectionLength();
				ASTNode then= ((IfStatement) statement).getThenStatement();
				if (selectionEnd <= then.getStartPosition() + then.getLength()) {
					if (!(then instanceof Block)) {
						childProperty= IfStatement.THEN_STATEMENT_PROPERTY;
						child= then;
					}
				} else if (selectionStart >=  then.getStartPosition() + then.getLength()) {
					ASTNode elseStatement= ((IfStatement) statement).getElseStatement();
					if (!(elseStatement instanceof Block)) {
						childProperty= IfStatement.ELSE_STATEMENT_PROPERTY;
						child= elseStatement;
					}
				}
				break;
			case ASTNode.WHILE_STATEMENT:
				ASTNode whileBody= ((WhileStatement) statement).getBody();
				if (!(whileBody instanceof Block)) {
					childProperty= WhileStatement.BODY_PROPERTY;
					child= whileBody;
				}
				break;
			case ASTNode.WITH_STATEMENT:
				ASTNode withBody= ((WithStatement) statement).getBody();
				if (!(withBody instanceof Block)) {
					childProperty= WithStatement.BODY_PROPERTY;
					child= withBody;
				}
				break;
			case ASTNode.FOR_STATEMENT:
				ASTNode forBody= ((ForStatement) statement).getBody();
				if (!(forBody instanceof Block)) {
					childProperty= ForStatement.BODY_PROPERTY;
					child= forBody;
				}
				break;
			case ASTNode.FOR_IN_STATEMENT:
				ASTNode forInBody= ((ForInStatement) statement).getBody();
				if (!(forInBody instanceof Block)) {
					childProperty= ForInStatement.BODY_PROPERTY;
					child= forInBody;
				}
				break;
			case ASTNode.DO_STATEMENT:
				ASTNode doBody= ((DoStatement) statement).getBody();
				if (!(doBody instanceof Block)) {
					childProperty= DoStatement.BODY_PROPERTY;
					child= doBody;
				}
				break;
			default:
		}
		if (child == null) {
			return false;
		}

		if (resultingCollections == null) {
			return true;
		}
		AST ast= statement.getAST();
		{
			ASTRewrite rewrite= ASTRewrite.create(ast);

			ASTNode childPlaceholder= rewrite.createMoveTarget(child);
			Block replacingBody= ast.newBlock();
			replacingBody.statements().add(childPlaceholder);
			rewrite.set(statement, childProperty, replacingBody, null);

			String label;
			if (childProperty == IfStatement.THEN_STATEMENT_PROPERTY) {
				label = CorrectionMessages.QuickAssistProcessor_replacethenwithblock_description;
			} else if (childProperty == IfStatement.ELSE_STATEMENT_PROPERTY) {
				label = CorrectionMessages.QuickAssistProcessor_replaceelsewithblock_description;
			} else {
				label = CorrectionMessages.QuickAssistProcessor_replacebodywithblock_description;
			}

			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			LinkedCorrectionProposal proposal= new LinkedCorrectionProposal(label, context.getCompilationUnit(), rewrite, 10, image);
			proposal.setCommandId(ADD_BLOCK_ID);
			proposal.setEndPosition(rewrite.track(child));
			resultingCollections.add(proposal);
		}

		if (statement.getNodeType() == ASTNode.IF_STATEMENT) {
			ASTRewrite rewrite= ASTRewrite.create(ast);
			
			while (statement.getLocationInParent() == IfStatement.ELSE_STATEMENT_PROPERTY) {
				statement= (Statement) statement.getParent();
			}
			
			boolean missingBlockFound= false;
			boolean foundElse= false;
			
			IfStatement ifStatement;
			Statement thenStatment;
			Statement elseStatment;
			do {
				ifStatement= (IfStatement) statement;
				thenStatment= ifStatement.getThenStatement();
				elseStatment= ifStatement.getElseStatement();

				if (!(thenStatment instanceof Block)) {
					ASTNode childPlaceholder1= rewrite.createMoveTarget(thenStatment);
					Block replacingBody1= ast.newBlock();
					replacingBody1.statements().add(childPlaceholder1);
					rewrite.set(ifStatement, IfStatement.THEN_STATEMENT_PROPERTY, replacingBody1, null);
					if (thenStatment != child) {
						missingBlockFound= true;
					}
				}
				if (elseStatment != null) {
					foundElse= true;
				}
				statement= elseStatment;
			} while (elseStatment instanceof IfStatement);
			
			if (elseStatment != null && !(elseStatment instanceof Block)) {
				ASTNode childPlaceholder2= rewrite.createMoveTarget(elseStatment);
				
				Block replacingBody2= ast.newBlock();
				replacingBody2.statements().add(childPlaceholder2);
				rewrite.set(ifStatement, IfStatement.ELSE_STATEMENT_PROPERTY, replacingBody2, null);
				if (elseStatment != child) {
					missingBlockFound= true;
				}
			}

			if (missingBlockFound && foundElse) {
				String label = CorrectionMessages.QuickAssistProcessor_replacethenelsewithblock_description;
				Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
				ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, 10, image);
				resultingCollections.add(proposal);
			}
		}
		return true;
	}

	private static boolean getInvertEqualsProposal(IInvocationContext context, ASTNode node, Collection resultingCollections) {
		ASTNode parent= node.getParent();
		if (!(parent instanceof FunctionInvocation)) {
			return false;
		}
		FunctionInvocation method= (FunctionInvocation) parent;
		if (!"equals".equals(method.getName().getIdentifier())) { //$NON-NLS-1$
			return false;
		}
		List arguments= method.arguments();
		if (arguments.size() != 1) { //overloaded equals w/ more than 1 argument
			return false;
		}
		Expression right= (Expression) arguments.get(0);
		ITypeBinding binding = right.resolveTypeBinding();
		if (binding != null && !(binding.isClass())) { //overloaded equals w/ non-class/interface argument or null
			return false;
		}
		if (resultingCollections == null) {
			return true;
		}

		Expression left= method.getExpression();

		AST ast= method.getAST();
		ASTRewrite rewrite= ASTRewrite.create(ast);
		if (left == null) { // equals(x) -> x.equals(this)
			FunctionInvocation replacement= ast.newFunctionInvocation();
			replacement.setName((SimpleName) rewrite.createCopyTarget(method.getName()));
			replacement.arguments().add(ast.newThisExpression());
			replacement.setExpression((Expression) rewrite.createCopyTarget(right));
			rewrite.replace(method, replacement, null);
		} else if (right instanceof ThisExpression) { // x.equals(this) -> equals(x)
			FunctionInvocation replacement= ast.newFunctionInvocation();
			replacement.setName((SimpleName) rewrite.createCopyTarget(method.getName()));
			replacement.arguments().add(rewrite.createCopyTarget(left));
			rewrite.replace(method, replacement, null);
		} else {
			ASTNode leftExpression= left;
			while (leftExpression instanceof ParenthesizedExpression) {
				leftExpression= ((ParenthesizedExpression) left).getExpression();
			}
			rewrite.replace(right, rewrite.createCopyTarget(leftExpression), null);

			if ((right instanceof Assignment)
				|| (right instanceof ConditionalExpression)
				|| (right instanceof InfixExpression)) {
				ParenthesizedExpression paren= ast.newParenthesizedExpression();
				paren.setExpression((Expression) rewrite.createCopyTarget(right));
				rewrite.replace(left, paren, null);
			} else {
				rewrite.replace(left, rewrite.createCopyTarget(right), null);
			}
		}

		String label= CorrectionMessages.QuickAssistProcessor_invertequals_description;
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);

		LinkedCorrectionProposal proposal= new LinkedCorrectionProposal(label, context.getCompilationUnit(), rewrite, 1, image);
		resultingCollections.add(proposal);
		return true;
	}

	private static boolean getArrayInitializerToArrayCreation(IInvocationContext context, ASTNode node, Collection resultingCollections) throws CoreException {
		if (!(node instanceof ArrayInitializer)) {
			return false;
		}
		ArrayInitializer initializer= (ArrayInitializer) node;

		ASTNode parent= initializer.getParent();
		while (parent instanceof ArrayInitializer) {
			initializer= (ArrayInitializer) parent;
			parent= parent.getParent();
		}
		ITypeBinding typeBinding= initializer.resolveTypeBinding();
		if (!(parent instanceof VariableDeclaration) || typeBinding == null || !typeBinding.isArray()) {
			return false;
		}
		if (resultingCollections == null) {
			return true;
		}

		AST ast= node.getAST();
		ASTRewrite rewrite= ASTRewrite.create(ast);

		String label= CorrectionMessages.QuickAssistProcessor_typetoarrayInitializer_description;
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);

		LinkedCorrectionProposal proposal= new LinkedCorrectionProposal(label, context.getCompilationUnit(), rewrite, 1, image);
		
		ImportRewrite imports= proposal.createImportRewrite(context.getASTRoot());
		String typeName= imports.addImport(typeBinding);

		ArrayCreation creation= ast.newArrayCreation();
		creation.setInitializer((ArrayInitializer) rewrite.createMoveTarget(initializer));
		creation.setType((ArrayType) ASTNodeFactory.newType(ast, typeName));

		rewrite.replace(initializer, creation, null);
		
		resultingCollections.add(proposal);
		return true;
	}


	public static boolean getCreateInSuperClassProposals(IInvocationContext context, ASTNode node, Collection resultingCollections) throws CoreException {
		if (!(node instanceof SimpleName) || !(node.getParent() instanceof FunctionDeclaration)) {
			return false;
		}
		FunctionDeclaration decl= (FunctionDeclaration) node.getParent();
		if (decl.getName() != node || decl.resolveBinding() == null || Modifier.isPrivate(decl.getModifiers())) {
			return false;
		}

		IJavaScriptUnit cu= context.getCompilationUnit();
		JavaScriptUnit astRoot= context.getASTRoot();

		IFunctionBinding binding= decl.resolveBinding();
		ITypeBinding[] paramTypes= binding.getParameterTypes();

		ITypeBinding[] superTypes= Bindings.getAllSuperTypes(binding.getDeclaringClass());
		if (resultingCollections == null) {
			for (int i= 0; i < superTypes.length; i++) {
				ITypeBinding curr= superTypes[i];
				if (curr.isFromSource() && Bindings.findOverriddenMethodInType(curr, binding) == null) {
					return true;
				}
			}
			return false;
		}
		List params= decl.parameters();
		String[] paramNames= new String[paramTypes.length];
		for (int i = 0; i < params.size(); i++) {
			SingleVariableDeclaration param= (SingleVariableDeclaration) params.get(i);
			paramNames[i]= param.getName().getIdentifier();
		}

		for (int i= 0; i < superTypes.length; i++) {
			ITypeBinding curr= superTypes[i];
			if (curr.isFromSource()) {
				IFunctionBinding method= Bindings.findOverriddenMethodInType(curr, binding);
				if (method == null) {
					ITypeBinding typeDecl= curr.getTypeDeclaration();
					IJavaScriptUnit targetCU= ASTResolving.findCompilationUnitForBinding(cu, astRoot, typeDecl);
					if (targetCU != null) {
						String label= Messages.format(CorrectionMessages.QuickAssistProcessor_createmethodinsuper_description, new String[] { curr.getName(), binding.getName() });
						resultingCollections.add(new NewDefiningMethodProposal(label, targetCU, astRoot, typeDecl, binding, paramNames, 6));
					}
				}
			}
		}
		return true;
	}

	private static boolean getConvertIterableLoopProposal(IInvocationContext context, ASTNode node, Collection resultingCollections) throws CoreException {
		ForStatement forStatement= getEnclosingForStatementHeader(node);
		if (forStatement == null)
			return false;

		if (resultingCollections == null)
			return true;
		
		IFix fix= ConvertLoopFix.createConvertIterableLoopToEnhancedFix(context.getASTRoot(), forStatement);
		if (fix == null)
			return false;
		
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		Map options= new HashMap();
		options.put(CleanUpConstants.CONTROL_STATMENTS_CONVERT_FOR_LOOP_TO_ENHANCED, CleanUpConstants.TRUE);
		ICleanUp cleanUp= new ConvertLoopCleanUp(options);
		FixCorrectionProposal proposal= new FixCorrectionProposal(fix, cleanUp, 1, image, context);
		proposal.setCommandId(CONVERT_FOR_LOOP_ID);
		
		resultingCollections.add(proposal);
		return true;
	}

	private static ForStatement getEnclosingForStatementHeader(ASTNode node) {
		if (node instanceof ForStatement)
			return (ForStatement) node;

		while (node != null) {
			ASTNode parent= node.getParent();
			if (parent instanceof ForStatement) {
				StructuralPropertyDescriptor locationInParent= node.getLocationInParent();
				if (locationInParent == ForStatement.EXPRESSION_PROPERTY
						|| locationInParent == ForStatement.INITIALIZERS_PROPERTY
						|| locationInParent == ForStatement.UPDATERS_PROPERTY)
					return (ForStatement) parent;
				else
					return null;
			}
			node= parent;
		}
		return null;
	}
	
	private static boolean getMakeVariableDeclarationFinalProposals(IInvocationContext context, ASTNode node, Collection resultingCollections) {
		SelectionAnalyzer analyzer= new SelectionAnalyzer(Selection.createFromStartLength(context.getSelectionOffset(), context.getSelectionLength()), false);
		context.getASTRoot().accept(analyzer);
		ASTNode[] selectedNodes= analyzer.getSelectedNodes();
		if (selectedNodes.length == 0)
			return false;
		
		IFix fix= VariableDeclarationFix.createChangeModifierToFinalFix(context.getASTRoot(), selectedNodes);
		if (fix == null)
			return false;
		
		if (resultingCollections == null)
			return true;

		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		Map options= new Hashtable();
		options.put(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL, CleanUpConstants.TRUE);
		options.put(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_LOCAL_VARIABLES, CleanUpConstants.TRUE);
		options.put(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PARAMETERS, CleanUpConstants.TRUE);
		options.put(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS, CleanUpConstants.TRUE);
		VariableDeclarationCleanUp cleanUp= new VariableDeclarationCleanUp(options);
		FixCorrectionProposal proposal= new FixCorrectionProposal(fix, cleanUp, 5, image, context);
		resultingCollections.add(proposal);
		return true;
	}
		
	private static boolean getInlineLocalProposal(IInvocationContext context, final ASTNode node, Collection proposals) throws CoreException {
		if (!(node instanceof SimpleName))
			return false;
		
		SimpleName name= (SimpleName) node;
		IBinding binding= name.resolveBinding();
		if (!(binding instanceof IVariableBinding))
			return false;
		IVariableBinding varBinding= (IVariableBinding) binding;
		if (varBinding.isField() || varBinding.isParameter())
			return false;
		ASTNode decl= context.getASTRoot().findDeclaringNode(varBinding);
		if (!(decl instanceof VariableDeclarationFragment) || decl.getLocationInParent() != VariableDeclarationStatement.FRAGMENTS_PROPERTY)
			return false;
		
		if (proposals == null) {
			return true;
		}
			
		InlineTempRefactoring refactoring= new InlineTempRefactoring((VariableDeclaration) decl);
		if (refactoring.checkInitialConditions(new NullProgressMonitor()).isOK()) {
			String label= CorrectionMessages.QuickAssistProcessor_inline_local_description;
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);		
			RefactoringCorrectionProposal proposal= new RefactoringCorrectionProposal(label, context.getCompilationUnit(), refactoring, 5, image);
			proposal.setCommandId(INLINE_LOCAL_ID);
			proposals.add(proposal);
			
		}
		return true;
	}
	
	private static boolean getConvertLocalToFieldProposal(IInvocationContext context, final ASTNode node, Collection proposals) throws CoreException {
		if (!(node instanceof SimpleName))
			return false;
		
		SimpleName name= (SimpleName) node;
		IBinding binding= name.resolveBinding();
		if (!(binding instanceof IVariableBinding) || name.getLocationInParent() != VariableDeclarationFragment.NAME_PROPERTY)
			return false;
		IVariableBinding varBinding= (IVariableBinding) binding;
		if (varBinding.isField() || varBinding.isParameter())
			return false;
		VariableDeclarationFragment decl= (VariableDeclarationFragment) name.getParent();
		if (decl.getLocationInParent() != VariableDeclarationStatement.FRAGMENTS_PROPERTY)
			return false;
		
		if (proposals == null) {
			return true;
		}
			
		PromoteTempToFieldRefactoring refactoring= new PromoteTempToFieldRefactoring(decl);
		if (refactoring.checkInitialConditions(new NullProgressMonitor()).isOK()) {
			String label= CorrectionMessages.QuickAssistProcessor_convert_local_to_field_description;
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			LinkedProposalModel linkedProposalModel= new LinkedProposalModel();
			refactoring.setLinkedProposalModel(linkedProposalModel);
			
			RefactoringCorrectionProposal proposal= new RefactoringCorrectionProposal(label, context.getCompilationUnit(), refactoring, 5, image);
			proposal.setLinkedProposalModel(linkedProposalModel);
			proposal.setCommandId(CONVERT_LOCAL_TO_FIELD_ID);
			proposals.add(proposal);
		}
		return true;
	}
	
	private static class RefactoringCorrectionProposal extends CUCorrectionProposal {
		private final Refactoring fRefactoring;

		public RefactoringCorrectionProposal(String name, IJavaScriptUnit cu, Refactoring refactoring, int relevance, Image image) {
			super(name, cu, null, relevance, image);
			fRefactoring= refactoring;
		}
		
		protected TextChange createTextChange() throws CoreException {
			return (TextChange) fRefactoring.createChange(new NullProgressMonitor());
		}
	}
}
