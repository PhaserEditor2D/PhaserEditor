/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Konstantin Scheglov (scheglov_ke@nlmk.ru) - initial API and implementation
 *          (reports 71244 & 74746: New Quick Assist's [quick assist])
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.text.correction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.graphics.Image;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.Assignment;
import org.eclipse.wst.jsdt.core.dom.Block;
import org.eclipse.wst.jsdt.core.dom.BooleanLiteral;
import org.eclipse.wst.jsdt.core.dom.BreakStatement;
import org.eclipse.wst.jsdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.wst.jsdt.core.dom.ClassInstanceCreation;
import org.eclipse.wst.jsdt.core.dom.ConditionalExpression;
import org.eclipse.wst.jsdt.core.dom.ConstructorInvocation;
import org.eclipse.wst.jsdt.core.dom.ContinueStatement;
import org.eclipse.wst.jsdt.core.dom.DoStatement;
import org.eclipse.wst.jsdt.core.dom.Expression;
import org.eclipse.wst.jsdt.core.dom.ExpressionStatement;
import org.eclipse.wst.jsdt.core.dom.ForInStatement;
import org.eclipse.wst.jsdt.core.dom.ForStatement;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.FunctionInvocation;
import org.eclipse.wst.jsdt.core.dom.IBinding;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.IVariableBinding;
import org.eclipse.wst.jsdt.core.dom.IfStatement;
import org.eclipse.wst.jsdt.core.dom.InfixExpression;
import org.eclipse.wst.jsdt.core.dom.InstanceofExpression;
import org.eclipse.wst.jsdt.core.dom.Name;
import org.eclipse.wst.jsdt.core.dom.ParenthesizedExpression;
import org.eclipse.wst.jsdt.core.dom.PostfixExpression;
import org.eclipse.wst.jsdt.core.dom.PrefixExpression;
import org.eclipse.wst.jsdt.core.dom.PrimitiveType;
import org.eclipse.wst.jsdt.core.dom.QualifiedName;
import org.eclipse.wst.jsdt.core.dom.ReturnStatement;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.SingleVariableDeclaration;
import org.eclipse.wst.jsdt.core.dom.Statement;
import org.eclipse.wst.jsdt.core.dom.StringLiteral;
import org.eclipse.wst.jsdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.wst.jsdt.core.dom.SuperConstructorInvocation;
import org.eclipse.wst.jsdt.core.dom.SuperMethodInvocation;
import org.eclipse.wst.jsdt.core.dom.SwitchCase;
import org.eclipse.wst.jsdt.core.dom.SwitchStatement;
import org.eclipse.wst.jsdt.core.dom.Type;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationStatement;
import org.eclipse.wst.jsdt.core.dom.WhileStatement;
import org.eclipse.wst.jsdt.core.dom.InfixExpression.Operator;
import org.eclipse.wst.jsdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.wst.jsdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.wst.jsdt.core.dom.rewrite.ListRewrite;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.corext.dom.GenericVisitor;
import org.eclipse.wst.jsdt.internal.corext.dom.LinkedNodeFinder;
import org.eclipse.wst.jsdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.wst.jsdt.internal.corext.fix.ExpressionsFix;
import org.eclipse.wst.jsdt.internal.corext.fix.IFix;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;
import org.eclipse.wst.jsdt.internal.ui.fix.ExpressionsCleanUp;
import org.eclipse.wst.jsdt.ui.CodeStyleConfiguration;
import org.eclipse.wst.jsdt.ui.text.java.IInvocationContext;
import org.eclipse.wst.jsdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.wst.jsdt.ui.text.java.IProblemLocation;
import org.eclipse.wst.jsdt.ui.text.java.IQuickAssistProcessor;

/**
 */
public class AdvancedQuickAssistProcessor implements IQuickAssistProcessor {
	public AdvancedQuickAssistProcessor() {
		super();
	}
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.ui.text.correction.IAssistProcessor#hasAssists(org.eclipse.wst.jsdt.internal.ui.text.correction.IAssistContext)
	 */
	public boolean hasAssists(IInvocationContext context) throws CoreException {
		ASTNode coveringNode = context.getCoveringNode();
		if (coveringNode != null) {
			ArrayList coveredNodes= getFullyCoveredNodes(context, coveringNode);
			return getInverseIfProposals(context, coveringNode, null)
					|| getIfReturnIntoIfElseAtEndOfVoidMethodProposals(context, coveringNode, null)
					|| getInverseIfContinueIntoIfThenInLoopsProposals(context, coveringNode, null)
					|| getInverseIfIntoContinueInLoopsProposals(context, coveringNode, null)
					|| getInverseConditionProposals(context, coveringNode, coveredNodes, null)
					|| getRemoveExtraParenthesisProposals(context, coveringNode, coveredNodes, null)
					|| getAddParanoidalParenthesisProposals(context, coveringNode, coveredNodes, null)
					|| getJoinAndIfStatementsProposals(context, coveringNode, null)
					|| getSplitAndConditionProposals(context, coveringNode, null)
					|| getJoinOrIfStatementsProposals(context, coveringNode, coveredNodes, null)
					|| getSplitOrConditionProposals(context, coveringNode, null)
					|| getInverseConditionalExpressionProposals(context, coveringNode, null)
					|| getExchangeInnerAndOuterIfConditionsProposals(context, coveringNode, null)
					|| getExchangeOperandsProposals(context, coveringNode, null)
					|| getPickOutStringProposals(context, coveringNode, null)
					|| getReplaceIfElseWithConditionalProposals(context, coveringNode, null)
					|| getReplaceConditionalWithIfElseProposals(context, coveringNode, null)
					|| getInverseLocalVariableProposals(context, coveringNode, null)
					|| getPushNegationDownProposals(context, coveringNode, null)
					|| getPullNegationUpProposals(context, coveringNode, coveredNodes, null)
					|| getJoinIfListInIfElseIfProposals(context, coveringNode, coveredNodes, null)
					|| getConvertSwitchToIfProposals(context, coveringNode, null);
		}
		return false;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.ui.text.correction.IAssistProcessor#getAssists(org.eclipse.wst.jsdt.internal.ui.text.correction.IAssistContext, org.eclipse.wst.jsdt.internal.ui.text.correction.IProblemLocation[])
	 */
	public IJavaCompletionProposal[] getAssists(IInvocationContext context, IProblemLocation[] locations)
			throws CoreException {
		ASTNode coveringNode = context.getCoveringNode();
		if (coveringNode != null) {
			ArrayList coveredNodes = getFullyCoveredNodes(context, coveringNode);
			ArrayList resultingCollections = new ArrayList();
			if (noErrorsAtLocation(locations)) {
				getInverseIfProposals(context, coveringNode, resultingCollections);
				getIfReturnIntoIfElseAtEndOfVoidMethodProposals(context, coveringNode, resultingCollections);
				getInverseIfContinueIntoIfThenInLoopsProposals(context, coveringNode, resultingCollections);
				getInverseIfIntoContinueInLoopsProposals(context, coveringNode, resultingCollections);
				getInverseConditionProposals(context, coveringNode, coveredNodes, resultingCollections);
				getRemoveExtraParenthesisProposals(context, coveringNode, coveredNodes, resultingCollections);
				getAddParanoidalParenthesisProposals(context, coveringNode, coveredNodes, resultingCollections);
				getJoinAndIfStatementsProposals(context, coveringNode, resultingCollections);
				getSplitAndConditionProposals(context, coveringNode, resultingCollections);
				getJoinOrIfStatementsProposals(context, coveringNode, coveredNodes, resultingCollections);
				getSplitOrConditionProposals(context, coveringNode, resultingCollections);
				getInverseConditionalExpressionProposals(context, coveringNode, resultingCollections);
				getExchangeInnerAndOuterIfConditionsProposals(context, coveringNode, resultingCollections);
				getExchangeOperandsProposals(context, coveringNode, resultingCollections);
				getPickOutStringProposals(context, coveringNode, resultingCollections);
				getReplaceIfElseWithConditionalProposals(context, coveringNode, resultingCollections);
				getReplaceConditionalWithIfElseProposals(context, coveringNode, resultingCollections);
				getInverseLocalVariableProposals(context, coveringNode, resultingCollections);
				getPushNegationDownProposals(context, coveringNode, resultingCollections);
				getPullNegationUpProposals(context, coveringNode, coveredNodes, resultingCollections);
				getJoinIfListInIfElseIfProposals(context, coveringNode, coveredNodes, resultingCollections);
				getConvertSwitchToIfProposals(context, coveringNode, resultingCollections);
			}
			return (IJavaCompletionProposal[]) resultingCollections.toArray(new IJavaCompletionProposal[resultingCollections.size()]);
		}
		return null;
	}
	private static boolean noErrorsAtLocation(IProblemLocation[] locations) {
		if (locations != null) {
			for (int i = 0; i < locations.length; i++) {
				if (locations[i].isError()) {
					return false;
				}
			}
		}
		return true;
	}
	private static boolean getIfReturnIntoIfElseAtEndOfVoidMethodProposals(IInvocationContext context, ASTNode covering,
			Collection resultingCollections) {
		Statement coveringStatement = ASTResolving.findParentStatement(covering);
		if (!(coveringStatement instanceof IfStatement)) {
			return false;
		}
		IfStatement ifStatement = (IfStatement) coveringStatement;
		if (ifStatement.getElseStatement() != null) {
			return false;
		}
		// 'then' block should have 'return' as last statement
		Statement thenStatement = ifStatement.getThenStatement();
		if (!(thenStatement instanceof Block)) {
			return false;
		}
		Block thenBlock = (Block) thenStatement;
		List thenStatements = thenBlock.statements();
		if (thenStatements.isEmpty() || !(thenStatements.get(thenStatements.size() - 1) instanceof ReturnStatement)) {
			return false;
		}
		// method should return 'void'
		FunctionDeclaration coveringMetod = ASTResolving.findParentMethodDeclaration(covering);
		if (coveringMetod == null) {
			return false;
		}
		Type returnType = coveringMetod.getReturnType2();
		if (!(returnType instanceof PrimitiveType)
				|| ((PrimitiveType) returnType).getPrimitiveTypeCode() != PrimitiveType.VOID)
			return false;
		//
		List statements = coveringMetod.getBody().statements();
		int ifIndex = statements.indexOf(ifStatement);
		if (ifIndex == -1) {
			return false;
		}
		//  we could produce quick assist
		if (resultingCollections == null) {
			return true;
		}
		//
		AST ast = coveringStatement.getAST();
		ASTRewrite rewrite = ASTRewrite.create(ast);
		// remove last 'return' in 'then' block
		ListRewrite listRewriter = rewrite.getListRewrite(thenBlock,
			(ChildListPropertyDescriptor) ifStatement.getLocationInParent());
		listRewriter.remove((ASTNode) thenStatements.get(thenStatements.size() - 1), null);
		// prepare original nodes
		Expression conditionPlaceholder = (Expression) rewrite.createMoveTarget(ifStatement.getExpression());
		Statement thenPlaceholder = (Statement) rewrite.createMoveTarget(ifStatement.getThenStatement());
		// prepare 'else' block
		Block elseBlock = ast.newBlock();
		for (int i = ifIndex + 1; i < statements.size(); i++) {
			Statement statement = (Statement) statements.get(i);
			elseBlock.statements().add(rewrite.createMoveTarget(statement));
		}
		// prepare new 'if' statement
		IfStatement newIf = ast.newIfStatement();
		newIf.setExpression(conditionPlaceholder);
		newIf.setThenStatement(thenPlaceholder);
		newIf.setElseStatement(elseBlock);
		rewrite.replace(ifStatement, newIf, null);
		// add correction proposal
		String label = CorrectionMessages.AdvancedQuickAssistProcessor_convertToIfElse_description;
		Image image = JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		ASTRewriteCorrectionProposal proposal = new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(),
				rewrite, 1, image);
		resultingCollections.add(proposal);
		return true;
	}
	private static boolean getInverseIfProposals(IInvocationContext context, ASTNode covering, Collection resultingCollections) {
		Statement coveringStatement = ASTResolving.findParentStatement(covering);
		if (!(coveringStatement instanceof IfStatement)) {
			return false;
		}
		IfStatement ifStatement = (IfStatement) coveringStatement;
		if (ifStatement.getElseStatement() == null) {
			return false;
		}
		//  we could produce quick assist
		if (resultingCollections == null) {
			return true;
		}
		//
		AST ast = coveringStatement.getAST();
		ASTRewrite rewrite = ASTRewrite.create(ast);
		Statement thenStatement= ifStatement.getThenStatement();
		Statement elseStatement= ifStatement.getElseStatement();
		
		// prepare original nodes
		Expression inversedExpression = getInversedBooleanExpression(rewrite, ifStatement.getExpression());
		
		Statement newElseStatement = (Statement) rewrite.createMoveTarget(thenStatement);
		Statement newThenStatement = (Statement) rewrite.createMoveTarget(elseStatement);
		// set new nodes
		rewrite.set(ifStatement, IfStatement.EXPRESSION_PROPERTY, inversedExpression, null);
		
		if (elseStatement instanceof IfStatement) {// bug 79507 && bug 74580
			Block elseBlock = ast.newBlock();
			elseBlock.statements().add(newThenStatement);
			newThenStatement= elseBlock;
		}
		rewrite.set(ifStatement, IfStatement.THEN_STATEMENT_PROPERTY, newThenStatement, null);
		rewrite.set(ifStatement, IfStatement.ELSE_STATEMENT_PROPERTY, newElseStatement, null);
		// add correction proposal
		String label = CorrectionMessages.AdvancedQuickAssistProcessor_inverseIf_description;
		Image image = JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		ASTRewriteCorrectionProposal proposal = new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(),
				rewrite, 1, image);
		resultingCollections.add(proposal);
		return true;
	}
	private static boolean getInverseIfContinueIntoIfThenInLoopsProposals(IInvocationContext context, ASTNode covering,
			Collection resultingCollections) {
		Statement coveringStatement = ASTResolving.findParentStatement(covering);
		if (!(coveringStatement instanceof IfStatement)) {
			return false;
		}
		IfStatement ifStatement = (IfStatement) coveringStatement;
		if (ifStatement.getElseStatement() != null) {
			return false;
		}
		// check that 'then' is 'continue'
		if (!(ifStatement.getThenStatement() instanceof ContinueStatement)) {
			return false;
		}
		// check that 'if' statement is statement in block that is body of loop
		Block loopBlock = null;
		if ((ifStatement.getParent() instanceof Block) && (ifStatement.getParent().getParent() instanceof ForStatement)) {
			loopBlock = (Block) ifStatement.getParent();
		} else if ((ifStatement.getParent() instanceof Block)
				&& (ifStatement.getParent().getParent() instanceof WhileStatement)) {
			loopBlock = (Block) ifStatement.getParent();
		} else {
			return false;
		}
		if (resultingCollections == null) {
			return true;
		}
		//
		AST ast = coveringStatement.getAST();
		ASTRewrite rewrite = ASTRewrite.create(ast);
		// create inverted 'if' statement
		Expression inversedExpression = getInversedBooleanExpression(rewrite, ifStatement.getExpression());
		IfStatement newIf = ast.newIfStatement();
		newIf.setExpression(inversedExpression);
		// prepare 'then' for new 'if'
		Block thenBlock = ast.newBlock();
		int ifIndex = loopBlock.statements().indexOf(ifStatement);
		for (int i = ifIndex + 1; i < loopBlock.statements().size(); i++) {
			Statement statement = (Statement) loopBlock.statements().get(i);
			thenBlock.statements().add(rewrite.createMoveTarget(statement));
		}
		newIf.setThenStatement(thenBlock);
		// replace 'if' statement in loop
		rewrite.replace(ifStatement, newIf, null);
		// add correction proposal
		String label = CorrectionMessages.AdvancedQuickAssistProcessor_inverseIfContinue_description;
		Image image = JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		ASTRewriteCorrectionProposal proposal = new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(),
				rewrite, 1, image);
		resultingCollections.add(proposal);
		return true;
	}
	private static boolean getInverseIfIntoContinueInLoopsProposals(IInvocationContext context, ASTNode covering, Collection resultingCollections) {
		Statement coveringStatement = ASTResolving.findParentStatement(covering);
		if (!(coveringStatement instanceof IfStatement)) {
			return false;
		}
		IfStatement ifStatement = (IfStatement) coveringStatement;
		if (ifStatement.getElseStatement() != null) {
			return false;
		}
		// prepare outer control structure and block that contains 'if' statement
		ASTNode ifParent = ifStatement.getParent();
		Block ifParentBlock = null;
		ASTNode ifParentStructure = ifParent;
		if (ifParentStructure instanceof Block) {
			ifParentBlock = (Block) ifParent;
			ifParentStructure = ifParentStructure.getParent();
		}
		// check that control structure is loop and 'if' statement if last statement
		if (!(ifParentStructure instanceof ForStatement) && !(ifParentStructure instanceof WhileStatement)&& !(ifParentStructure instanceof ForInStatement)) {
			return false;
		}
		if ((ifParentBlock != null) && (ifParentBlock.statements().indexOf(ifStatement) != ifParentBlock.statements().size() - 1)) {
			return false;
		}
		//  we could produce quick assist
		if (resultingCollections == null) {
			return true;
		}
		//
		AST ast = coveringStatement.getAST();
		ASTRewrite rewrite = ASTRewrite.create(ast);
		// create inverted 'if' statement
		Expression inversedExpression = getInversedBooleanExpression(rewrite, ifStatement.getExpression());
		IfStatement newIf = ast.newIfStatement();
		newIf.setExpression(inversedExpression);
		newIf.setThenStatement(ast.newContinueStatement());
		//
		if (ifParentBlock == null) {
			// if there is no block, create it
			ifParentBlock = ast.newBlock();
			ifParentBlock.statements().add(newIf);
			for (Iterator iter = getUnwrappedStatements(ifStatement.getThenStatement()).iterator(); iter.hasNext();) {
				Statement statement = (Statement) iter.next();
				ifParentBlock.statements().add(rewrite.createMoveTarget(statement));
			}
			// replace 'if' statement as body with new block
			if (ifParentStructure instanceof ForStatement) {
				rewrite.set(ifParentStructure, ForStatement.BODY_PROPERTY, ifParentBlock, null);
			} else if (ifParentStructure instanceof WhileStatement) {
				rewrite.set(ifParentStructure, WhileStatement.BODY_PROPERTY, ifParentBlock, null);
			}
		} else {
			// if there was block, replace
			ListRewrite listRewriter = rewrite.getListRewrite(ifParentBlock,
				(ChildListPropertyDescriptor) ifStatement.getLocationInParent());
			listRewriter.replace(ifStatement, newIf, null);
			// add statements from 'then' to the end of block
			for (Iterator iter = getUnwrappedStatements(ifStatement.getThenStatement()).iterator(); iter.hasNext();) {
				Statement statement = (Statement) iter.next();
				listRewriter.insertLast(rewrite.createMoveTarget(statement), null);
			}
		}
		// add correction proposal
		String label = CorrectionMessages.AdvancedQuickAssistProcessor_inverseIfToContinue_description;
		Image image = JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		ASTRewriteCorrectionProposal proposal = new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(),
				rewrite, 1, image);
		resultingCollections.add(proposal);
		return true;
	}
	private static ArrayList getUnwrappedStatements(Statement body) {
		ArrayList statements = new ArrayList();
		if (body instanceof Block) {
			for (Iterator iter = ((Block) body).statements().iterator(); iter.hasNext();) {
				Statement statement = (Statement) iter.next();
				statements.add(statement);
			}
		} else {
			statements.add(body);
		}
		return statements;
	}
	private static boolean getInverseConditionProposals(IInvocationContext context, ASTNode covering, ArrayList coveredNodes, Collection resultingCollections) {
		if (coveredNodes.isEmpty()) {
			return false;
		}
		//
		final AST ast = covering.getAST();
		final ASTRewrite rewrite = ASTRewrite.create(ast);
		// check sub-expressions in fully covered nodes
		boolean hasChanges = false;
		for (Iterator iter = coveredNodes.iterator(); iter.hasNext();) {
			ASTNode covered = (ASTNode) iter.next();
			Expression coveredExpression= getBooleanExpression(covered);
			if (coveredExpression != null) {
				Expression inversedExpression = getInversedBooleanExpression(rewrite, coveredExpression);
				rewrite.replace(coveredExpression, inversedExpression, null);
				hasChanges = true;
			}
		}
		//
		if (!hasChanges) {
			return false;
		}
		if (resultingCollections == null) {
			return true;
		}
		// add correction proposal
		String label = CorrectionMessages.AdvancedQuickAssistProcessor_inverseConditions_description;
		Image image = JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		ASTRewriteCorrectionProposal proposal = new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(),
				rewrite, 1, image);
		resultingCollections.add(proposal);
		return true;
	}
	private static Expression getInversedBooleanExpression(ASTRewrite rewrite, Expression expression) {
		return getInversedBooleanExpression(rewrite, expression, null);
	}
	private interface SimpleNameRenameProvider {
		SimpleName getRenamed(SimpleName name);
	}
	private static Expression getRenamedNameCopy(SimpleNameRenameProvider provider,
			ASTRewrite rewrite,
			Expression expression) {
		if (provider != null) {
			if (expression instanceof SimpleName) {
				SimpleName name= (SimpleName) expression;
				SimpleName newName= provider.getRenamed(name);
				if (newName != null) {
					return newName;
				}
			}
		}
		return (Expression) rewrite.createCopyTarget(expression);
	}
	private static Expression getInversedBooleanExpression(ASTRewrite rewrite, Expression expression, SimpleNameRenameProvider provider) {
		if (!isBoolean(expression)) {
			return (Expression) rewrite.createCopyTarget(expression);
		}
		AST ast= rewrite.getAST();
		//
		if (expression instanceof BooleanLiteral) {
			return ast.newBooleanLiteral(!((BooleanLiteral) expression).booleanValue());
		}
		if (expression instanceof InfixExpression) {
			InfixExpression infixExpression= (InfixExpression) expression;
			InfixExpression.Operator operator= infixExpression.getOperator();
			if (operator == InfixExpression.Operator.LESS) {
				return getInversedInfixBooleanExpression(rewrite, infixExpression, InfixExpression.Operator.GREATER_EQUALS, provider);
			}
			if (operator == InfixExpression.Operator.GREATER) {
				return getInversedInfixBooleanExpression(rewrite, infixExpression, InfixExpression.Operator.LESS_EQUALS, provider);
			}
			if (operator == InfixExpression.Operator.LESS_EQUALS) {
				return getInversedInfixBooleanExpression(rewrite, infixExpression, InfixExpression.Operator.GREATER, provider);
			}
			if (operator == InfixExpression.Operator.GREATER_EQUALS) {
				return getInversedInfixBooleanExpression(rewrite, infixExpression, InfixExpression.Operator.LESS, provider);
			}
			if (operator == InfixExpression.Operator.EQUALS) {
				return getInversedInfixBooleanExpression(rewrite, infixExpression, InfixExpression.Operator.NOT_EQUALS, provider);
			}
			if (operator == InfixExpression.Operator.NOT_EQUALS) {
				return getInversedInfixBooleanExpression(rewrite, infixExpression, InfixExpression.Operator.EQUALS, provider);
			}
			if (operator == InfixExpression.Operator.CONDITIONAL_AND) {
				return getInversedAndOrExpression(rewrite, infixExpression, InfixExpression.Operator.CONDITIONAL_OR, provider);
			}
			if (operator == InfixExpression.Operator.CONDITIONAL_OR) {
				return getInversedAndOrExpression(rewrite, infixExpression, InfixExpression.Operator.CONDITIONAL_AND, provider);
			}
			if (operator == InfixExpression.Operator.AND) {
				return getInversedAndOrExpression(rewrite, infixExpression, InfixExpression.Operator.OR, provider);
			}
			if (operator == InfixExpression.Operator.OR) {
				return getInversedAndOrExpression(rewrite, infixExpression, InfixExpression.Operator.AND, provider);
			}
		}
		if (expression instanceof PrefixExpression) {
			PrefixExpression prefixExpression= (PrefixExpression) expression;
			if (prefixExpression.getOperator() == PrefixExpression.Operator.NOT) {
				return getRenamedNameCopy(provider, rewrite, prefixExpression.getOperand());
			}
		}
		if (expression instanceof InstanceofExpression) {
			PrefixExpression prefixExpression= ast.newPrefixExpression();
			prefixExpression.setOperator(PrefixExpression.Operator.NOT);
			ParenthesizedExpression parenthesizedExpression= ast.newParenthesizedExpression();
			parenthesizedExpression.setExpression((Expression) rewrite.createCopyTarget(expression));
			prefixExpression.setOperand(parenthesizedExpression);
			return prefixExpression;
		}
		if (expression instanceof ParenthesizedExpression) {
			ParenthesizedExpression parenthesizedExpression= (ParenthesizedExpression) expression;
			Expression innerExpression= parenthesizedExpression.getExpression();
			while (innerExpression instanceof ParenthesizedExpression) {
				innerExpression= ((ParenthesizedExpression) innerExpression).getExpression();
			}
			if (innerExpression instanceof InstanceofExpression) {
				return getInversedBooleanExpression(rewrite, innerExpression, provider);
			}
			parenthesizedExpression= ast.newParenthesizedExpression();
			parenthesizedExpression.setExpression(getInversedBooleanExpression(rewrite, innerExpression, provider));
			return parenthesizedExpression;
		}
		//
		PrefixExpression prefixExpression= ast.newPrefixExpression();
		prefixExpression.setOperator(PrefixExpression.Operator.NOT);
		prefixExpression.setOperand(getRenamedNameCopy(provider, rewrite, expression));
		return prefixExpression;
	}
	private static boolean isBoolean(Expression expression) {
		return expression.resolveTypeBinding() == expression.getAST().resolveWellKnownType("boolean"); //$NON-NLS-1$
	}
	private static Expression getInversedInfixBooleanExpression(ASTRewrite rewrite, InfixExpression expression, InfixExpression.Operator newOperator, SimpleNameRenameProvider provider) {
		InfixExpression newExpression = rewrite.getAST().newInfixExpression();
		newExpression.setOperator(newOperator);
		newExpression.setLeftOperand(getInversedBooleanExpression(rewrite, expression.getLeftOperand(), provider));
		newExpression.setRightOperand(getInversedBooleanExpression(rewrite, expression.getRightOperand(), provider));
		return newExpression;
	}
	
	private static Expression parenthesizeIfRequired(Expression operand, int newOperatorPrecedence) {
		if (newOperatorPrecedence < getExpressionPrecedence(operand)) {
			return getParenthesizedExpression(operand.getAST(), operand);
		}
		return operand;
	}
	
	private static Expression getInversedAndOrExpression(ASTRewrite rewrite, InfixExpression infixExpression, Operator newOperator, SimpleNameRenameProvider provider) {
		InfixExpression newExpression = rewrite.getAST().newInfixExpression();
		newExpression.setOperator(newOperator);
		
		int newOperatorPrecedence = getInfixOperatorPrecedence(newOperator);
		//
		Expression leftOperand = getInversedBooleanExpression(rewrite, infixExpression.getLeftOperand(), provider);
		newExpression.setLeftOperand(parenthesizeIfRequired(leftOperand, newOperatorPrecedence));
		
		Expression rightOperand = getInversedBooleanExpression(rewrite, infixExpression.getRightOperand(), provider);
		newExpression.setRightOperand(parenthesizeIfRequired(rightOperand, newOperatorPrecedence));
		
		List extraOperands= infixExpression.extendedOperands();
		List newExtraOperands= newExpression.extendedOperands();
		for (int i= 0; i < extraOperands.size(); i++) {
			Expression extraOperand = getInversedBooleanExpression(rewrite, (Expression) extraOperands.get(i), provider);
			newExtraOperands.add(parenthesizeIfRequired(extraOperand, newOperatorPrecedence));
		}
		return newExpression;
	}
	private static boolean getRemoveExtraParenthesisProposals(IInvocationContext context, ASTNode covering, ArrayList coveredNodes,
			Collection resultingCollections) {
		ArrayList nodes;
		if ((context.getSelectionLength() == 0) && (covering instanceof ParenthesizedExpression)) {
			nodes = new ArrayList();
			nodes.add(covering);
		} else {
			nodes= coveredNodes;
		}
		if (nodes.isEmpty())
			return false;
		
		IFix fix= ExpressionsFix.createRemoveUnnecessaryParenthesisFix(context.getASTRoot(), (ASTNode[])nodes.toArray(new ASTNode[nodes.size()]));
		if (fix == null)
			return false;
		
		if (resultingCollections == null)
			return true;
		
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_REMOVE);
		Map options= new Hashtable();
		options.put(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES, CleanUpConstants.TRUE);
		options.put(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER, CleanUpConstants.TRUE);
		FixCorrectionProposal proposal= new FixCorrectionProposal(fix, new ExpressionsCleanUp(options), 1, image, context);
		resultingCollections.add(proposal);
		return true;
	}
	private static int getExpressionPrecedence(Expression expression) {
		if (expression instanceof PostfixExpression) {
			return 0;
		}
		if (expression instanceof PrefixExpression) {
			return 1;
		}
		if (expression instanceof ClassInstanceCreation) {
			return 2;
		}
		if (expression instanceof InfixExpression) {
			InfixExpression infixExpression = (InfixExpression) expression;
			InfixExpression.Operator operator = infixExpression.getOperator();
			return getInfixOperatorPrecedence(operator);
		}
		if (expression instanceof InstanceofExpression) {
			return 6;
		}
		if (expression instanceof ConditionalExpression) {
			return 13;
		}
		if (expression instanceof Assignment) {
			return 14;
		}
		if (expression instanceof FunctionInvocation) {
			return 2;
		}
		return -1;
	}
	private static int getInfixOperatorPrecedence(InfixExpression.Operator operator) {
		if ((operator == InfixExpression.Operator.TIMES) || (operator == InfixExpression.Operator.DIVIDE)
				|| (operator == InfixExpression.Operator.REMAINDER)) {
			return 3;
		}
		if ((operator == InfixExpression.Operator.PLUS) || (operator == InfixExpression.Operator.MINUS)) {
			return 4;
		}
		if ((operator == InfixExpression.Operator.LEFT_SHIFT)
				|| (operator == InfixExpression.Operator.RIGHT_SHIFT_SIGNED)
				|| (operator == InfixExpression.Operator.RIGHT_SHIFT_UNSIGNED)) {
			return 5;
		}
		if ((operator == InfixExpression.Operator.LESS) || (operator == InfixExpression.Operator.GREATER)
				|| (operator == InfixExpression.Operator.LESS_EQUALS)
				|| (operator == InfixExpression.Operator.GREATER_EQUALS)) {
			return 6;
		}
		if ((operator == InfixExpression.Operator.EQUALS) || (operator == InfixExpression.Operator.NOT_EQUALS) 
			||(operator == InfixExpression.Operator.EQUAL_EQUAL_EQUAL) || (operator == InfixExpression.Operator.NOT_EQUAL_EQUAL)
			||(operator == InfixExpression.Operator.INSTANCEOF)) {			return 7;
		}
		if (operator == InfixExpression.Operator.AND) {
			return 8;
		}
		if (operator == InfixExpression.Operator.XOR) {
			return 9;
		}
		if (operator == InfixExpression.Operator.OR) {
			return 10;
		}
		if (operator == InfixExpression.Operator.CONDITIONAL_AND) {
			return 11;
		}
		if (operator == InfixExpression.Operator.CONDITIONAL_OR) {
			return 12;
		}
		return -1;
	}
	
	private static boolean getAddParanoidalParenthesisProposals(IInvocationContext context, ASTNode covering, ArrayList coveredNodes,
			Collection resultingCollections) throws CoreException {
		
		IFix fix= ExpressionsFix.createAddParanoidalParenthesisFix(context.getASTRoot(), (ASTNode[])coveredNodes.toArray(new ASTNode[coveredNodes.size()]));
		if (fix == null)
			return false;
		
		if (resultingCollections == null)
			return true;

		// add correction proposal
		Image image = JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		Map options= new Hashtable();
		options.put(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES, CleanUpConstants.TRUE);
		options.put(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_ALWAYS, CleanUpConstants.TRUE);
		FixCorrectionProposal proposal= new FixCorrectionProposal(fix, new ExpressionsCleanUp(options), 1, image, context);
		resultingCollections.add(proposal);
		return true;
	}
	
	private static ArrayList getFullyCoveredNodes(IInvocationContext context, ASTNode coveringNode) {
		final ArrayList coveredNodes = new ArrayList();
		final int selectionBegin = context.getSelectionOffset();
		final int selectionEnd = selectionBegin + context.getSelectionLength();
		coveringNode.accept(new GenericVisitor() {
			protected boolean visitNode(ASTNode node) {
				int nodeStart= node.getStartPosition();
				int nodeEnd= nodeStart + node.getLength();
				// if node does not intersects with selection, don't visit children
				if (nodeEnd < selectionBegin || selectionEnd < nodeStart) {
					return false;
				}
				// if node is fully covered, we don't need to visit children
				if (isCovered(node)) {
					ASTNode parent = node.getParent();
					if ((parent == null) || !isCovered(parent)) {
						coveredNodes.add(node);
						return false;
					}
				}
				// if node only partly intersects with selection, we try to find fully covered children
				return true;
			}
			private boolean isCovered(ASTNode node) {
				int begin = node.getStartPosition();
				int end = begin + node.getLength();
				return (begin >= selectionBegin) && (end <= selectionEnd);
			}
		});
		return coveredNodes;
	}
	private static boolean getJoinAndIfStatementsProposals(IInvocationContext context, ASTNode node,
			Collection resultingCollections) {
		Operator andOperator = InfixExpression.Operator.CONDITIONAL_AND;
		boolean result = false;
		//
		Statement statement = ASTResolving.findParentStatement(node);
		if (!(statement instanceof IfStatement)) {
			return false;
		}
		IfStatement ifStatement = (IfStatement) statement;
		if (ifStatement.getElseStatement() != null) {
			return false;
		}
		// case when current IfStatement is sole child of another IfStatement
		{
			IfStatement outerIf = null;
			if (ifStatement.getParent() instanceof IfStatement) {
				outerIf = (IfStatement) ifStatement.getParent();
			} else if (ifStatement.getParent() instanceof Block) {
				Block block = (Block) ifStatement.getParent();
				if ((block.getParent() instanceof IfStatement) && (block.statements().size() == 1)) {
					outerIf = (IfStatement) block.getParent();
				}
			}
			if ((outerIf != null) && (outerIf.getElseStatement() == null)) {
				if (resultingCollections == null) {
					return true;
				}
				//
				AST ast = statement.getAST();
				ASTRewrite rewrite = ASTRewrite.create(ast);
				// prepare condition parts, add parenthesis if needed
				Expression outerCondition = getParenthesizedForAndIfNeeded(ast, rewrite, outerIf.getExpression());
				Expression innerCondition = getParenthesizedForAndIfNeeded(ast, rewrite, ifStatement.getExpression());
				// create compound condition
				InfixExpression condition = ast.newInfixExpression();
				condition.setOperator(andOperator);
				condition.setLeftOperand(outerCondition);
				condition.setRightOperand(innerCondition);
				// create new IfStatement
				IfStatement newIf = ast.newIfStatement();
				newIf.setExpression(condition);
				Statement bodyPlaceholder = (Statement) rewrite.createCopyTarget(ifStatement.getThenStatement());
				newIf.setThenStatement(bodyPlaceholder);
				rewrite.replace(outerIf, newIf, null);
				// add correction proposal
				String label = CorrectionMessages.AdvancedQuickAssistProcessor_joinWithOuter_description;
				Image image = JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
				ASTRewriteCorrectionProposal proposal = new ASTRewriteCorrectionProposal(label,
						context.getCompilationUnit(), rewrite, 1, image);
				resultingCollections.add(proposal);
				result = true;
			}
		}
		// case when current IfStatement has another IfStatement as sole child
		{
			IfStatement innerIf = null;
			if (ifStatement.getThenStatement() instanceof IfStatement) {
				innerIf = (IfStatement) ifStatement.getThenStatement();
			} else if (ifStatement.getThenStatement() instanceof Block) {
				Block block = (Block) ifStatement.getThenStatement();
				if ((block.statements().size() == 1) && (block.statements().get(0) instanceof IfStatement)) {
					innerIf = (IfStatement) block.statements().get(0);
				}
			}
			if ((innerIf != null) && (innerIf.getElseStatement() == null)) {
				if (resultingCollections == null) {
					return true;
				}
				//
				AST ast = statement.getAST();
				ASTRewrite rewrite = ASTRewrite.create(ast);
				// prepare condition parts, add parenthesis if needed
				Expression outerCondition = getParenthesizedForAndIfNeeded(ast, rewrite, ifStatement.getExpression());
				Expression innerCondition = getParenthesizedForAndIfNeeded(ast, rewrite, innerIf.getExpression());
				// create compound condition
				InfixExpression condition = ast.newInfixExpression();
				condition.setOperator(andOperator);
				condition.setLeftOperand(outerCondition);
				condition.setRightOperand(innerCondition);
				// create new IfStatement
				IfStatement newIf = ast.newIfStatement();
				newIf.setExpression(condition);
				Statement bodyPlaceholder = (Statement) rewrite.createCopyTarget(innerIf.getThenStatement());
				newIf.setThenStatement(bodyPlaceholder);
				rewrite.replace(ifStatement, newIf, null);
				// add correction proposal
				String label = CorrectionMessages.AdvancedQuickAssistProcessor_joinWithInner_description;
				Image image = JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
				ASTRewriteCorrectionProposal proposal = new ASTRewriteCorrectionProposal(label,
						context.getCompilationUnit(), rewrite, 1, image);
				resultingCollections.add(proposal);
				result = true;
			}
		}
		return result;
	}
	private static Expression getParenthesizedForAndIfNeeded(AST ast, ASTRewrite rewrite, Expression expression) {
		boolean addParentheses = false;
		int nodeType = expression.getNodeType();
		if (nodeType == ASTNode.INFIX_EXPRESSION) {
			InfixExpression infixExpression = (InfixExpression) expression;
			addParentheses = infixExpression.getOperator() == InfixExpression.Operator.CONDITIONAL_OR;
		} else {
			addParentheses = nodeType == ASTNode.CONDITIONAL_EXPRESSION || nodeType == ASTNode.ASSIGNMENT
					|| nodeType == ASTNode.INSTANCEOF_EXPRESSION;
		}
		expression = (Expression) rewrite.createCopyTarget(expression);
		if (addParentheses) {
			return getParenthesizedExpression(ast, expression);
		}
		return expression;
	}
	private static Expression getParenthesizedExpression(AST ast, Expression expression) {
		ParenthesizedExpression parenthesizedExpression = ast.newParenthesizedExpression();
		parenthesizedExpression.setExpression(expression);
		return parenthesizedExpression;
	}
	private static boolean getSplitAndConditionProposals(IInvocationContext context, ASTNode node,
			Collection resultingCollections) {
		Operator andOperator = InfixExpression.Operator.CONDITIONAL_AND;
		// check that user invokes quick assist on infix expression
		if (!(node instanceof InfixExpression)) {
			return false;
		}
		InfixExpression infixExpression = (InfixExpression) node;
		if (infixExpression.getOperator() != andOperator) {
			return false;
		}
		int offset= isOperatorSelected(infixExpression, context.getSelectionOffset(), context.getSelectionLength());
		if (offset == -1) {
			return false;
		}

		// check that infix expression belongs to IfStatement
		Statement statement = ASTResolving.findParentStatement(node);
		if (!(statement instanceof IfStatement)) {
			return false;
		}
		IfStatement ifStatement = (IfStatement) statement;
		if (ifStatement.getElseStatement() != null) {
			return false;
		}
		// check that infix expression is part of first level && condition of IfStatement
		InfixExpression topInfixExpression = infixExpression;
		while ((topInfixExpression.getParent() instanceof InfixExpression)
				&& ((InfixExpression) topInfixExpression.getParent()).getOperator() == andOperator) {
			topInfixExpression = (InfixExpression) topInfixExpression.getParent();
		}
		if (ifStatement.getExpression() != topInfixExpression) {
			return false;
		}
		//
		if (resultingCollections == null) {
			return true;
		}
		AST ast = ifStatement.getAST();
		ASTRewrite rewrite = ASTRewrite.create(ast);
		
		// prepare left and right conditions
		Expression[] newOperands= { null, null };
		breakInfixOperationAtOperation(rewrite, topInfixExpression, andOperator, offset, true, newOperands);

		Expression leftCondition= newOperands[0];
		Expression rightCondition= newOperands[1];
		
		// replace condition in inner IfStatement
		rewrite.set(ifStatement, IfStatement.EXPRESSION_PROPERTY, rightCondition, null);
		// prepare outer IfStatement
		IfStatement outerIfStatement = ast.newIfStatement();
		outerIfStatement.setExpression(leftCondition);
		Block outerBlock = ast.newBlock();
		outerIfStatement.setThenStatement(outerBlock);
		ASTNode ifPlaceholder = rewrite.createMoveTarget(ifStatement);
		outerBlock.statements().add(ifPlaceholder);
		// replace ifStatement
		rewrite.replace(ifStatement, outerIfStatement, null);
		// add correction proposal
		String label = CorrectionMessages.AdvancedQuickAssistProcessor_splitAndCondition_description;
		Image image = JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		ASTRewriteCorrectionProposal proposal = new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(),
				rewrite, 1, image);
		resultingCollections.add(proposal);
		return true;
	}
	private static boolean isSelectingOperator(ASTNode n1, ASTNode n2, int offset, int length) {
		// between the nodes
		if (offset + length <= n2.getStartPosition() && offset >= ASTNodes.getExclusiveEnd(n1)) {
			return true;
		}
		// or exactly select the node (but not with infix expressions)
		if (n1.getStartPosition() == offset && ASTNodes.getExclusiveEnd(n2) == offset + length) {
			if (n1 instanceof InfixExpression || n2 instanceof InfixExpression) {
				return false;
			}
			return true;
		}
		return false;
	}
	
	private static int isOperatorSelected(InfixExpression infixExpression, int offset, int length) {
		ASTNode left= infixExpression.getLeftOperand();
		ASTNode right= infixExpression.getRightOperand();
		
		if (isSelectingOperator(left, right, offset, length)) {
			return ASTNodes.getExclusiveEnd(left);
		}
		List extended= infixExpression.extendedOperands();
		for (int i= 0; i < extended.size(); i++) {
			left= right;
			right= (ASTNode) extended.get(i);
			if (isSelectingOperator(left, right, offset, length)) {
				return ASTNodes.getExclusiveEnd(left);
			}
		}
		return -1;
	}
	
	private static boolean getJoinOrIfStatementsProposals(IInvocationContext context, ASTNode covering, ArrayList coveredNodes,
			Collection resultingCollections) {
		Operator orOperator = InfixExpression.Operator.CONDITIONAL_OR;
		if (coveredNodes.size() < 2)
			return false;
		// check that all covered nodes are IfStatement's with same 'then' statement and without 'else'
		String commonThenSource = null;
		for (Iterator iter = coveredNodes.iterator(); iter.hasNext();) {
			ASTNode node = (ASTNode) iter.next();
			if (!(node instanceof IfStatement))
				return false;
			//
			IfStatement ifStatement = (IfStatement) node;
			if (ifStatement.getElseStatement() != null)
				return false;
			//
			Statement thenStatement = ifStatement.getThenStatement();
			try {
				String thenSource = context.getCompilationUnit().getBuffer().getText(thenStatement.getStartPosition(),
					thenStatement.getLength());
				if (commonThenSource == null) {
					commonThenSource = thenSource;
				} else {
					if (!commonThenSource.equals(thenSource))
						return false;
				}
			} catch (Throwable e) {
				return false;
			}
		}
		if (resultingCollections == null) {
			return true;
		}
		//
		final AST ast = covering.getAST();
		final ASTRewrite rewrite = ASTRewrite.create(ast);
		// prepare OR'ed condition
		InfixExpression condition = null;
		boolean hasRightOperand = false;
		Statement thenStatement = null;
		for (Iterator iter = coveredNodes.iterator(); iter.hasNext();) {
			IfStatement ifStatement = (IfStatement) iter.next();
			if (thenStatement == null)
				thenStatement = (Statement) rewrite.createCopyTarget(ifStatement.getThenStatement());
			Expression ifCondition = getParenthesizedForOrIfNeeded(ast, rewrite, ifStatement.getExpression());
			if (condition == null) {
				condition = ast.newInfixExpression();
				condition.setOperator(orOperator);
				condition.setLeftOperand(ifCondition);
			} else if (!hasRightOperand) {
				condition.setRightOperand(ifCondition);
				hasRightOperand = true;
			} else {
				InfixExpression newCondition = ast.newInfixExpression();
				newCondition.setOperator(orOperator);
				newCondition.setLeftOperand(condition);
				newCondition.setRightOperand(ifCondition);
				condition = newCondition;
			}
		}
		// prepare new IfStatement with OR'ed condition
		IfStatement newIf = ast.newIfStatement();
		newIf.setExpression(condition);
		newIf.setThenStatement(thenStatement);
		//
		ListRewrite listRewriter = null;
		for (Iterator iter = coveredNodes.iterator(); iter.hasNext();) {
			IfStatement ifStatement = (IfStatement) iter.next();
			if (listRewriter == null) {
				Block sourceBlock = (Block) ifStatement.getParent();
				//int insertIndex = sourceBlock.statements().indexOf(ifStatement);
				listRewriter = rewrite.getListRewrite(sourceBlock,
					(ChildListPropertyDescriptor) ifStatement.getLocationInParent());
			}
			if (newIf != null) {
				listRewriter.replace(ifStatement, newIf, null);
				newIf = null;
			} else {
				listRewriter.remove(ifStatement, null);
			}
		}
		// add correction proposal
		String label = CorrectionMessages.AdvancedQuickAssistProcessor_joinWithOr_description;
		Image image = JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		ASTRewriteCorrectionProposal proposal = new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(),
				rewrite, 1, image);
		resultingCollections.add(proposal);
		return true;
	}
	private static Expression getParenthesizedForOrIfNeeded(AST ast, ASTRewrite rewrite, Expression expression) {
		boolean addParentheses = false;
		int nodeType = expression.getNodeType();
		addParentheses = nodeType == ASTNode.CONDITIONAL_EXPRESSION || nodeType == ASTNode.ASSIGNMENT
				|| nodeType == ASTNode.INSTANCEOF_EXPRESSION;
		expression = (Expression) rewrite.createCopyTarget(expression);
		if (addParentheses) {
			return getParenthesizedExpression(ast, expression);
		}
		return expression;
	}
	private static boolean getSplitOrConditionProposals(IInvocationContext context, ASTNode node,
			Collection resultingCollections) {
		Operator orOperator = InfixExpression.Operator.CONDITIONAL_OR;
		// check that user invokes quick assist on infix expression
		if (!(node instanceof InfixExpression)) {
			return false;
		}
		InfixExpression infixExpression = (InfixExpression) node;
		if (infixExpression.getOperator() != orOperator) {
			return false;
		}
		int offset= isOperatorSelected(infixExpression, context.getSelectionOffset(), context.getSelectionLength());
		if (offset == -1) {
			return false;
		}
		// check that infix expression belongs to IfStatement
		Statement statement = ASTResolving.findParentStatement(node);
		if (!(statement instanceof IfStatement)) {
			return false;
		}
		IfStatement ifStatement = (IfStatement) statement;
		if (ifStatement.getElseStatement() != null) {
			return false;
		}
		// check that infix expression is part of first level || condition of IfStatement
		InfixExpression topInfixExpression = infixExpression;
		while ((topInfixExpression.getParent() instanceof InfixExpression)
				&& ((InfixExpression) topInfixExpression.getParent()).getOperator() == orOperator) {
			topInfixExpression = (InfixExpression) topInfixExpression.getParent();
		}
		if (ifStatement.getExpression() != topInfixExpression) {
			return false;
		}
		//
		if (resultingCollections == null) {
			return true;
		}
		AST ast = ifStatement.getAST();
		ASTRewrite rewrite = ASTRewrite.create(ast);

		// prepare left and right conditions
		Expression[] newOperands= { null, null };
		breakInfixOperationAtOperation(rewrite, topInfixExpression, orOperator, offset, true, newOperands);
		
		Expression leftCondition= newOperands[0];
		Expression rightCondition= newOperands[1];
		
		// prepare first statement
		IfStatement firstIf = ast.newIfStatement();
		firstIf.setExpression(leftCondition);
		firstIf.setThenStatement((Statement) rewrite.createCopyTarget(ifStatement.getThenStatement()));
		// prepare second statement
		IfStatement secondIf = ast.newIfStatement();
		secondIf.setExpression(rightCondition);
		secondIf.setThenStatement((Statement) rewrite.createCopyTarget(ifStatement.getThenStatement()));
		// add first and second IfStatement's
		Block sourceBlock = (Block) ifStatement.getParent();
		int insertIndex = sourceBlock.statements().indexOf(ifStatement);
		ListRewrite listRewriter = rewrite.getListRewrite(sourceBlock,
			(ChildListPropertyDescriptor) statement.getLocationInParent());
		listRewriter.replace(ifStatement, firstIf, null);
		listRewriter.insertAt(secondIf, insertIndex + 1, null);
		// add correction proposal
		String label = CorrectionMessages.AdvancedQuickAssistProcessor_splitOrCondition_description;
		Image image = JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		ASTRewriteCorrectionProposal proposal = new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(),
				rewrite, 1, image);
		resultingCollections.add(proposal);
		return true;
	}
	private static boolean getInverseConditionalExpressionProposals(IInvocationContext context, ASTNode covering,
			Collection resultingCollections) {
		// try to find conditional expression as parent
		while (covering instanceof Expression) {
			if (covering instanceof ConditionalExpression)
				break;
			covering = covering.getParent();
		}
		if (!(covering instanceof ConditionalExpression)) {
			return false;
		}
		ConditionalExpression expression = (ConditionalExpression) covering;
		//  we could produce quick assist
		if (resultingCollections == null) {
			return true;
		}
		//
		AST ast = covering.getAST();
		ASTRewrite rewrite = ASTRewrite.create(ast);
		// prepare new conditional expression
		ConditionalExpression newExpression = ast.newConditionalExpression();
		newExpression.setExpression(getInversedBooleanExpression(rewrite, expression.getExpression()));
		newExpression.setThenExpression((Expression) rewrite.createCopyTarget(expression.getElseExpression()));
		newExpression.setElseExpression((Expression) rewrite.createCopyTarget(expression.getThenExpression()));
		// replace old expression with new
		rewrite.replace(expression, newExpression, null);
		// add correction proposal
		String label = CorrectionMessages.AdvancedQuickAssistProcessor_inverseConditionalExpression_description;
		Image image = JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		ASTRewriteCorrectionProposal proposal = new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(),
				rewrite, 1, image);
		resultingCollections.add(proposal);
		return true;
	}
	private static boolean getExchangeInnerAndOuterIfConditionsProposals(IInvocationContext context, ASTNode node,
			Collection resultingCollections) {
		boolean result = false;
		//
		Statement statement = ASTResolving.findParentStatement(node);
		if (!(statement instanceof IfStatement)) {
			return false;
		}
		IfStatement ifStatement = (IfStatement) statement;
		if (ifStatement.getElseStatement() != null) {
			return false;
		}
		// case when current IfStatement is sole child of another IfStatement
		{
			IfStatement outerIf = null;
			if (ifStatement.getParent() instanceof IfStatement) {
				outerIf = (IfStatement) ifStatement.getParent();
			} else if (ifStatement.getParent() instanceof Block) {
				Block block = (Block) ifStatement.getParent();
				if ((block.getParent() instanceof IfStatement) && (block.statements().size() == 1)) {
					outerIf = (IfStatement) block.getParent();
				}
			}
			if ((outerIf != null) && (outerIf.getElseStatement() == null)) {
				if (resultingCollections == null) {
					return true;
				}
				//
				AST ast = statement.getAST();
				ASTRewrite rewrite = ASTRewrite.create(ast);
				// prepare conditions
				Expression outerCondition = (Expression) rewrite.createCopyTarget(outerIf.getExpression());
				Expression innerCondition = (Expression) rewrite.createCopyTarget(ifStatement.getExpression());
				// exchange conditions
				rewrite.replace(outerIf.getExpression(), innerCondition, null);
				rewrite.replace(ifStatement.getExpression(), outerCondition, null);
				// add correction proposal
				String label = CorrectionMessages.AdvancedQuickAssistProcessor_exchangeInnerAndOuterIfConditions_description;
				Image image = JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
				ASTRewriteCorrectionProposal proposal = new ASTRewriteCorrectionProposal(label,
						context.getCompilationUnit(), rewrite, 1, image);
				resultingCollections.add(proposal);
				result = true;
			}
		}
		// case when current IfStatement has another IfStatement as sole child
		{
			IfStatement innerIf = null;
			if (ifStatement.getThenStatement() instanceof IfStatement) {
				innerIf = (IfStatement) ifStatement.getThenStatement();
			} else if (ifStatement.getThenStatement() instanceof Block) {
				Block block = (Block) ifStatement.getThenStatement();
				if ((block.statements().size() == 1) && (block.statements().get(0) instanceof IfStatement)) {
					innerIf = (IfStatement) block.statements().get(0);
				}
			}
			if ((innerIf != null) && (innerIf.getElseStatement() == null)) {
				if (resultingCollections == null) {
					return true;
				}
				//
				AST ast = statement.getAST();
				ASTRewrite rewrite = ASTRewrite.create(ast);
				// prepare conditions
				Expression innerCondition = (Expression) rewrite.createCopyTarget(innerIf.getExpression());
				Expression outerCondition = (Expression) rewrite.createCopyTarget(ifStatement.getExpression());
				// exchange conditions
				rewrite.replace(innerIf.getExpression(), outerCondition, null);
				rewrite.replace(ifStatement.getExpression(), innerCondition, null);
				// add correction proposal
				String label = CorrectionMessages.AdvancedQuickAssistProcessor_exchangeInnerAndOuterIfConditions_description;
				Image image = JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
				ASTRewriteCorrectionProposal proposal = new ASTRewriteCorrectionProposal(label,
						context.getCompilationUnit(), rewrite, 1, image);
				resultingCollections.add(proposal);
				result = true;
			}
		}
		return result;
	}
	private static boolean getExchangeOperandsProposals(IInvocationContext context, ASTNode node,
			Collection resultingCollections) {
		// check that user invokes quick assist on infix expression
		if (!(node instanceof InfixExpression)) {
			return false;
		}
		InfixExpression infixExpression = (InfixExpression) node;
		Operator operator = infixExpression.getOperator();
		if ((operator != InfixExpression.Operator.CONDITIONAL_AND) && (operator != InfixExpression.Operator.AND)
				&& (operator != InfixExpression.Operator.CONDITIONAL_OR) && (operator != InfixExpression.Operator.OR)
				&& (operator != InfixExpression.Operator.EQUALS) && (operator != InfixExpression.Operator.PLUS)
				&& (operator != InfixExpression.Operator.TIMES) && (operator != InfixExpression.Operator.XOR)) {
			return false;
		}
		
		int offset= isOperatorSelected(infixExpression, context.getSelectionOffset(), context.getSelectionLength());
		if (offset == -1) {
			return false;
		}
		
		//  we could produce quick assist
		if (resultingCollections == null) {
			return true;
		}
		AST ast = infixExpression.getAST();
		ASTRewrite rewrite = ASTRewrite.create(ast);
		// prepare left and right expressions
		Expression leftExpression = null;
		Expression rightExpression = null;
		InfixExpression currentExpression = infixExpression;
		leftExpression= combineOperands(rewrite, leftExpression, infixExpression.getLeftOperand(), true, operator);
		if (infixExpression.getRightOperand().getStartPosition() <= context.getSelectionOffset()) {
			leftExpression= combineOperands(rewrite, leftExpression, infixExpression.getRightOperand(), true, operator);
		} else {
			rightExpression= combineOperands(rewrite, rightExpression, infixExpression.getRightOperand(), true, operator);
		}
		for (Iterator iter= currentExpression.extendedOperands().iterator(); iter.hasNext();) {
			Expression extendedOperand= (Expression) iter.next();
			if (extendedOperand.getStartPosition() <= context.getSelectionOffset()) {
				leftExpression= combineOperands(rewrite, leftExpression, extendedOperand, true, operator);
			} else {
				rightExpression= combineOperands(rewrite, rightExpression, extendedOperand, true, operator);
			}
		}
		// create new infix expression
		InfixExpression newInfix = ast.newInfixExpression();
		newInfix.setOperator(operator);
		newInfix.setLeftOperand(rightExpression);
		newInfix.setRightOperand(leftExpression);
		rewrite.replace(infixExpression, newInfix, null);
		// add correction proposal
		String label = CorrectionMessages.AdvancedQuickAssistProcessor_exchangeOperands_description;
		Image image = JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		ASTRewriteCorrectionProposal proposal = new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, 1, image);
		resultingCollections.add(proposal);
		return true;
	}
	
	/**
	 * Breaks an infix operation with possible extended operators at the given operator and returns the new left and right operands.  
	 * a & b & c   ->  [[a' & b' ] & c' ]   (c' == copy of c)
	 * @param rewrite
	 * @param expression
	 * @param operator
	 * @param operatorOffset
	 */
	private static void breakInfixOperationAtOperation(ASTRewrite rewrite, Expression expression, Operator operator, int operatorOffset, boolean removeParenthesis, Expression[] res) {
		if (expression.getStartPosition() + expression.getLength() <= operatorOffset) {
			// add to the left
			res[0]= combineOperands(rewrite, res[0], expression, removeParenthesis, operator);
			return;
		}
		if (operatorOffset <= expression.getStartPosition()) {
			// add to the right
			res[1]= combineOperands(rewrite, res[1], expression, removeParenthesis, operator);
			return;
		}
		if (!(expression instanceof InfixExpression)) {
			throw new IllegalArgumentException("Cannot break up non-infix expression"); //$NON-NLS-1$
		}
		InfixExpression infixExpression= (InfixExpression) expression;
		if (infixExpression.getOperator() != operator) {
			throw new IllegalArgumentException("Incompatible operator"); //$NON-NLS-1$
		}
		breakInfixOperationAtOperation(rewrite, infixExpression.getLeftOperand(), operator, operatorOffset, removeParenthesis, res);
		breakInfixOperationAtOperation(rewrite, infixExpression.getRightOperand(), operator, operatorOffset, removeParenthesis, res);	
		
		List extended= infixExpression.extendedOperands();
		for (int i= 0; i < extended.size(); i++) {
			breakInfixOperationAtOperation(rewrite, (Expression) extended.get(i), operator, operatorOffset, removeParenthesis, res);	
		}
	}
	
	private static Expression combineOperands(ASTRewrite rewrite, Expression existing, Expression nodeToAdd, boolean removeParenthesis, Operator operator) {
		if (existing == null && removeParenthesis) {
			while (nodeToAdd instanceof ParenthesizedExpression) {
				nodeToAdd= ((ParenthesizedExpression) nodeToAdd).getExpression();
			}
		}
		Expression newRight= (Expression) rewrite.createMoveTarget(nodeToAdd);
		if (existing == null) {
			return newRight;
		}
		AST ast= rewrite.getAST();
		InfixExpression infix= ast.newInfixExpression();
		infix.setOperator(operator);
		infix.setLeftOperand(existing);
		infix.setRightOperand(newRight);
		return infix;
	}

//	private static boolean isNegated(Expression expression) {
//		if (!(expression.getParent() instanceof ParenthesizedExpression))
//			return false;
//		
//		ParenthesizedExpression parenthesis= (ParenthesizedExpression)expression.getParent();
//		if (!(parenthesis.getParent() instanceof PrefixExpression))
//			return false;
//		
//		PrefixExpression prefix= (PrefixExpression)parenthesis.getParent();
//		if (!(prefix.getOperator() == PrefixExpression.Operator.NOT))
//			return false;
//		
//		return true;
//	}
//	private static String[] suggestLocalVariableNames(IJavaScriptUnit cu, ITypeBinding binding) {
//		return StubUtility.getVariableNameSuggestions(StubUtility.LOCAL, cu.getJavaScriptProject(), binding, null, null);
//	}

	private static boolean getPickOutStringProposals(IInvocationContext context, ASTNode node, Collection resultingCollections) {
		// we work with String's
		if (!(node instanceof StringLiteral)) {
			return false;
		}
		// user should select part of String
		int selectionPos= context.getSelectionOffset();
		int selectionLen= context.getSelectionLength();
		if (selectionLen == 0) {
			return false;
		}
		int valueStart= node.getStartPosition() + 1;
		int valueEnd= node.getStartPosition() + node.getLength() - 1;

		// selection must be inside node and the quotes and not contain the full value
		if ((selectionPos < valueStart) || (selectionPos + selectionLen > valueEnd) || (valueEnd - valueStart == selectionLen)) {
			return false;
		}

		// prepare string parts positions
		StringLiteral stringLiteral= (StringLiteral) node;
		String stringValue= stringLiteral.getEscapedValue();

		int firstPos= selectionPos - node.getStartPosition();
		int secondPos= firstPos + selectionLen;


		// prepare new string literals

		AST ast= node.getAST();
		StringLiteral leftLiteral= ast.newStringLiteral();
		StringLiteral centerLiteral= ast.newStringLiteral();
		StringLiteral rightLiteral= ast.newStringLiteral();
		try {
			leftLiteral.setEscapedValue('"' + stringValue.substring(1, firstPos) + '"');
			centerLiteral.setEscapedValue('"' + stringValue.substring(firstPos, secondPos) + '"');
			rightLiteral.setEscapedValue('"' + stringValue.substring(secondPos, stringValue.length() - 1)  + '"');
		} catch (IllegalArgumentException e) {
			return false;
		}
		if (resultingCollections == null) {
			return true;
		}

		ASTRewrite rewrite= ASTRewrite.create(ast);

		// prepare new expression instead of StringLiteral
		InfixExpression expression= ast.newInfixExpression();
		expression.setOperator(InfixExpression.Operator.PLUS);
		if (firstPos != 1 ) {
			expression.setLeftOperand(leftLiteral);
		}


		if (firstPos == 1) {
			expression.setLeftOperand(centerLiteral);
		} else {
			expression.setRightOperand(centerLiteral);
		}

		if (secondPos < stringValue.length() - 1) {
			if (firstPos == 1) {
				expression.setRightOperand(rightLiteral);
			} else {
				expression.extendedOperands().add(rightLiteral);
			}
		}
		// use new expression instead of old StirngLiteral
		rewrite.replace(stringLiteral, expression, null);
		// add correction proposal
		String label= CorrectionMessages.AdvancedQuickAssistProcessor_pickSelectedString;
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		LinkedCorrectionProposal proposal= new LinkedCorrectionProposal(label, context.getCompilationUnit(), rewrite, 1, image);
		proposal.addLinkedPosition(rewrite.track(centerLiteral), true, "CENTER_STRING"); //$NON-NLS-1$
		resultingCollections.add(proposal);
		return true;
	}

	private static Statement getSingleStatement(Statement statement) {
		if (statement instanceof Block) {
			List blockStatements= ((Block) statement).statements();
			if (blockStatements.size() != 1) {
				return null;
			}
			return (Statement) blockStatements.get(0);
		}
		return statement;
	}

	private static boolean getReplaceIfElseWithConditionalProposals(IInvocationContext context, ASTNode node, Collection resultingCollections) {
		if (!(node instanceof IfStatement)) {
			return false;
		}
		IfStatement ifStatement= (IfStatement) node;
		Statement thenStatement= getSingleStatement(ifStatement.getThenStatement());
		Statement elseStatement= getSingleStatement(ifStatement.getElseStatement());
		if (thenStatement == null || elseStatement == null) {
			return false;
		}
		Expression assigned= null;
		Expression thenExpression= null;
		Expression elseExpression= null;
		
		ITypeBinding exprBinding= null;
		if (thenStatement instanceof ReturnStatement && elseStatement instanceof ReturnStatement) {
			thenExpression= ((ReturnStatement) thenStatement).getExpression();
			elseExpression= ((ReturnStatement) elseStatement).getExpression();
			FunctionDeclaration declaration= ASTResolving.findParentMethodDeclaration(node);
			if (declaration == null || declaration.isConstructor()) {
				return false;
			}
			if (declaration.getReturnType2() != null)
				exprBinding= declaration.getReturnType2().resolveBinding();
		} else if (thenStatement instanceof ExpressionStatement && elseStatement instanceof ExpressionStatement) {
			Expression inner1= ((ExpressionStatement) thenStatement).getExpression();
			Expression inner2= ((ExpressionStatement) elseStatement).getExpression();
			if (inner1 instanceof Assignment && inner2 instanceof Assignment) {
				Assignment assign1= (Assignment) inner1;
				Assignment assign2= (Assignment) inner2;
				Expression left1= assign1.getLeftHandSide();
				Expression left2= assign2.getLeftHandSide();
				if (left1 instanceof Name && left2 instanceof Name && assign1.getOperator() == assign2.getOperator()) {
					IBinding bind1= ((Name) left1).resolveBinding();
					IBinding bind2= ((Name) left2).resolveBinding();
					if (bind1 == bind2 && bind1 instanceof IVariableBinding) {
						assigned= left1;
						exprBinding= ((IVariableBinding) bind1).getType();
						thenExpression= assign1.getRightHandSide();
						elseExpression= assign2.getRightHandSide();
					}
				}
			}
		}
		if (thenExpression == null || elseExpression == null) {
			return false;
		}

		//  we could produce quick assist
		if (resultingCollections == null) {
			return true;
		}
		//
		AST ast= node.getAST();
		ASTRewrite rewrite= ASTRewrite.create(ast);
		
		String label= CorrectionMessages.AdvancedQuickAssistProcessor_replaceIfWithConditional;
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, 1, image);

		
		// prepare conditional expression
		ConditionalExpression conditionalExpression = ast.newConditionalExpression();
		Expression conditionCopy= (Expression) rewrite.createCopyTarget(ifStatement.getExpression());
		conditionalExpression.setExpression(conditionCopy);
		Expression thenCopy= (Expression) rewrite.createCopyTarget(thenExpression);
		Expression elseCopy= (Expression) rewrite.createCopyTarget(elseExpression);

		
		if (!JavaModelUtil.is50OrHigher(context.getCompilationUnit().getJavaScriptProject())) {
			ITypeBinding thenBinding= thenExpression.resolveTypeBinding();
			ITypeBinding elseBinding= elseExpression.resolveTypeBinding();
			if (thenBinding != null && elseBinding != null && exprBinding != null && !elseBinding.isAssignmentCompatible(thenBinding)) {
				proposal.createImportRewrite(context.getASTRoot());
			}
		}
		conditionalExpression.setThenExpression(thenCopy);
		conditionalExpression.setElseExpression(elseCopy);
		
		// replace 'if' statement with conditional expression
		if (assigned == null) {
			ReturnStatement returnStatement = ast.newReturnStatement();
			returnStatement.setExpression(conditionalExpression);
			rewrite.replace(ifStatement, returnStatement, null);
		} else {
			Assignment assignment= ast.newAssignment();
			assignment.setLeftHandSide((Expression) rewrite.createCopyTarget(assigned));
			assignment.setRightHandSide(conditionalExpression);
			assignment.setOperator(((Assignment) assigned.getParent()).getOperator());
			
			ExpressionStatement expressionStatement = ast.newExpressionStatement(assignment);
			rewrite.replace(ifStatement, expressionStatement, null);
		}

		// add correction proposal
		resultingCollections.add(proposal);
		return true;
	}

	private static ReturnStatement createReturnExpression(ASTRewrite rewrite, Expression expression) {
		AST ast= rewrite.getAST();
		ReturnStatement thenReturn = ast.newReturnStatement();
		thenReturn.setExpression((Expression) rewrite.createCopyTarget(expression));
		return thenReturn;
	}

	private static Statement createAssignmentStatement(ASTRewrite rewrite, Assignment.Operator assignmentOperator, Expression origAssignee, Expression origAssigned) {
		AST ast= rewrite.getAST();
		Assignment elseAssignment= ast.newAssignment();
		elseAssignment.setOperator(assignmentOperator);
		elseAssignment.setLeftHandSide((Expression) rewrite.createCopyTarget(origAssignee));
		elseAssignment.setRightHandSide((Expression) rewrite.createCopyTarget(origAssigned));
		ExpressionStatement statement = ast.newExpressionStatement(elseAssignment);
		return statement;
	}

	private static boolean getReplaceConditionalWithIfElseProposals(IInvocationContext context, ASTNode covering, Collection resultingCollections) {
		// check that parent statement is assignment
		while (!(covering instanceof ConditionalExpression) && covering instanceof Expression) {
			covering= covering.getParent();
		}
		if (!(covering instanceof ConditionalExpression)) {
			return false;
		}

		StructuralPropertyDescriptor locationInParent= covering.getLocationInParent();
		if (locationInParent == Assignment.RIGHT_HAND_SIDE_PROPERTY) {
			if (covering.getParent().getLocationInParent() != ExpressionStatement.EXPRESSION_PROPERTY) {
				return false;
			}
		} else if (locationInParent == VariableDeclarationFragment.INITIALIZER_PROPERTY) {
			ASTNode statement= covering.getParent().getParent();
			if (!(statement instanceof VariableDeclarationStatement) || statement.getLocationInParent() != Block.STATEMENTS_PROPERTY) {
				return false;
			}
		} else if (locationInParent != ReturnStatement.EXPRESSION_PROPERTY) {
			return false;
		}

		ConditionalExpression conditional= (ConditionalExpression) covering;
		//  we could produce quick assist
		if (resultingCollections == null) {
			return true;
		}
		//
		AST ast= covering.getAST();
		ASTRewrite rewrite= ASTRewrite.create(ast);
		// prepare new 'if' statement
		Expression expression= conditional.getExpression();
		while (expression instanceof ParenthesizedExpression) {
			expression= ((ParenthesizedExpression) expression).getExpression();
		}
		IfStatement ifStatement= ast.newIfStatement();
		ifStatement.setExpression((Expression) rewrite.createCopyTarget(expression));
		if (locationInParent == Assignment.RIGHT_HAND_SIDE_PROPERTY) {
			Assignment assignment= (Assignment) covering.getParent();
			Expression assignee= assignment.getLeftHandSide();
			Assignment.Operator op= assignment.getOperator();
			
			ifStatement.setThenStatement(createAssignmentStatement(rewrite, op, assignee, conditional.getThenExpression()));
			ifStatement.setElseStatement(createAssignmentStatement(rewrite, op, assignee, conditional.getElseExpression()));

			// replace return conditional expression with if/then/else/return
			rewrite.replace(covering.getParent().getParent(), ifStatement, null);

		} else if (locationInParent == ReturnStatement.EXPRESSION_PROPERTY) {
			ifStatement.setThenStatement(createReturnExpression(rewrite, conditional.getThenExpression()));
			ifStatement.setElseStatement(createReturnExpression(rewrite, conditional.getElseExpression()));
			//
			// replace return conditional expression with if/then/else/return
			rewrite.replace(conditional.getParent(), ifStatement, null);
		} else if (locationInParent == VariableDeclarationFragment.INITIALIZER_PROPERTY) {
			VariableDeclarationFragment frag= (VariableDeclarationFragment) covering.getParent();
			Assignment.Operator op= Assignment.Operator.ASSIGN;
			
			Expression assignee= frag.getName();
			ifStatement.setThenStatement(createAssignmentStatement(rewrite, op, assignee, conditional.getThenExpression()));
			ifStatement.setElseStatement(createAssignmentStatement(rewrite, op, assignee, conditional.getElseExpression()));

			rewrite.set(frag, VariableDeclarationFragment.INITIALIZER_PROPERTY, null, null); // clear initializer

			ASTNode statement= frag.getParent();
			rewrite.getListRewrite(statement.getParent(), Block.STATEMENTS_PROPERTY).insertAfter(ifStatement, statement, null);
		}

		// add correction proposal
		String label= CorrectionMessages.AdvancedQuickAssistProcessor_replaceConditionalWithIf;
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, 1, image);
		resultingCollections.add(proposal);
		return true;
	}
	private static boolean getInverseLocalVariableProposals(IInvocationContext context,
			ASTNode covering,
			Collection resultingCollections) {
		final AST ast= covering.getAST();
		// cursor should be placed on variable name
		if (!(covering instanceof SimpleName)) {
			return false;
		}
		SimpleName coveringName= (SimpleName) covering;
		if (!coveringName.isDeclaration()) {
			return false;
		}
		// prepare bindings
		final IBinding variableBinding= coveringName.resolveBinding();
		if (!(variableBinding instanceof IVariableBinding)) {
			return false;
		}
		IVariableBinding binding= (IVariableBinding) variableBinding;
		if (binding.isField()) {
			return false;
		}
		// we operate only on boolean variable
		if (!isBoolean(coveringName)) {
			return false;
		}
		//  we could produce quick assist
		if (resultingCollections == null) {
			return true;
		}
		// find linked nodes
		final FunctionDeclaration method= ASTResolving.findParentMethodDeclaration(covering);
		SimpleName[] linkedNodes= LinkedNodeFinder.findByBinding(method, variableBinding);
		//
		final ASTRewrite rewrite= ASTRewrite.create(ast);
		// create proposal
		String label= CorrectionMessages.AdvancedQuickAssistProcessor_inverseBooleanVariable;
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		final String KEY_NAME= "name"; //$NON-NLS-1$
		final LinkedCorrectionProposal proposal= new LinkedCorrectionProposal(label,
			context.getCompilationUnit(),
			rewrite,
			1,
			image);
		// prepare new variable identifier
		final String oldIdentifier= coveringName.getIdentifier();
		final String notString = Messages.format(CorrectionMessages.AdvancedQuickAssistProcessor_negatedVariableName, "");  //$NON-NLS-1$
		final String newIdentifier;
		if (oldIdentifier.startsWith(notString)) {
			int notLength= notString.length();
			if (oldIdentifier.length() > notLength) {
				newIdentifier= Character.toLowerCase(oldIdentifier.charAt(notLength)) + oldIdentifier.substring(notLength + 1);
			} else {
				newIdentifier= oldIdentifier;
			}
		} else {
			newIdentifier= Messages.format(CorrectionMessages.AdvancedQuickAssistProcessor_negatedVariableName, Character.toUpperCase(oldIdentifier.charAt(0)) + oldIdentifier.substring(1));
		}
		//
		proposal.addLinkedPositionProposal(KEY_NAME, newIdentifier, null);
		proposal.addLinkedPositionProposal(KEY_NAME, oldIdentifier, null);
		// iterate over linked nodes and replace variable references with negated reference
		final HashSet renamedNames= new HashSet();
		for (int i= 0; i < linkedNodes.length; i++) {
			SimpleName name= linkedNodes[i];
			if (renamedNames.contains(name)) {
				continue;
			}
			// prepare new name with new identifier
			SimpleName newName= ast.newSimpleName(newIdentifier);
			proposal.addLinkedPosition(rewrite.track(newName), name == coveringName, KEY_NAME);
			//
			StructuralPropertyDescriptor location= name.getLocationInParent();
			if (location == SingleVariableDeclaration.NAME_PROPERTY) {
				// set new name
				rewrite.replace(name, newName, null);
			} else if (location == Assignment.LEFT_HAND_SIDE_PROPERTY) {
				Assignment assignment= (Assignment) name.getParent();
				Expression expression= assignment.getRightHandSide();
				int exStart= expression.getStartPosition();
				int exEnd= exStart + expression.getLength();
				// collect all names that are used in assignments
				HashSet overlapNames= new HashSet();
				for (int j= 0; j < linkedNodes.length; j++) {
					SimpleName name2= linkedNodes[j];
					if (name2 == null) {
						continue;
					}
					int name2Start= name2.getStartPosition();
					if (exStart <= name2Start && name2Start < exEnd) {
						overlapNames.add(name2);
					}
				}
				// prepare inverted expression
				SimpleNameRenameProvider provider= new SimpleNameRenameProvider() {
					public SimpleName getRenamed(SimpleName simpleName) {
						if (simpleName.resolveBinding() == variableBinding) {
							renamedNames.add(simpleName);
							return ast.newSimpleName(newIdentifier);
						}
						return null;
					}
				};
				Expression inversedExpression= getInversedBooleanExpression(rewrite, expression, provider);
				// if any name was not renamed during expression inverting, we can not already rename it, so fail to create assist
				for (Iterator iter= overlapNames.iterator(); iter.hasNext();) {
					Object o= iter.next();
					if (!renamedNames.contains(o)) {
						return false;
					}
				}
				// check operator and replace if needed
				Assignment.Operator operator= assignment.getOperator();
				if (operator == Assignment.Operator.BIT_AND_ASSIGN) {
					Assignment newAssignment= ast.newAssignment();
					newAssignment.setLeftHandSide(newName);
					newAssignment.setRightHandSide(inversedExpression);
					newAssignment.setOperator(Assignment.Operator.BIT_OR_ASSIGN);
					rewrite.replace(assignment, newAssignment, null);
				} else if (operator == Assignment.Operator.BIT_OR_ASSIGN) {
					Assignment newAssignment= ast.newAssignment();
					newAssignment.setLeftHandSide(newName);
					newAssignment.setRightHandSide(inversedExpression);
					newAssignment.setOperator(Assignment.Operator.BIT_AND_ASSIGN);
					rewrite.replace(assignment, newAssignment, null);
				} else {
					rewrite.replace(expression, inversedExpression, null);
					// set new name
					rewrite.replace(name, newName, null);
				}
			} else if (location == VariableDeclarationFragment.NAME_PROPERTY) {
				// replace initializer for variable
				VariableDeclarationFragment vdf= (VariableDeclarationFragment) name.getParent();
				Expression expression= vdf.getInitializer();
				if (expression != null) {
					rewrite.replace(expression, getInversedBooleanExpression(rewrite, expression), null);
				}
				// set new name
				rewrite.replace(name, newName, null);
			} else if ((name.getParent() instanceof PrefixExpression)
				&& (((PrefixExpression) name.getParent()).getOperator() == PrefixExpression.Operator.NOT)) {
				rewrite.replace(name.getParent(), newName, null);
			} else {
				PrefixExpression expression= ast.newPrefixExpression();
				expression.setOperator(PrefixExpression.Operator.NOT);
				expression.setOperand(newName);
				rewrite.replace(name, expression, null);
			}
		}
		// add correction proposal
		resultingCollections.add(proposal);
		return true;
	}
	private static boolean getPushNegationDownProposals(IInvocationContext context,
			ASTNode covering,
			Collection resultingCollections) {
		PrefixExpression negationExpression= null;
		ParenthesizedExpression parenthesizedExpression= null;
		// check for case when cursor is on '!' before parentheses
		if (covering instanceof PrefixExpression) {
			PrefixExpression prefixExpression= (PrefixExpression) covering;
			if ((prefixExpression.getOperator() == PrefixExpression.Operator.NOT)
				&& (prefixExpression.getOperand() instanceof ParenthesizedExpression)) {
				negationExpression= prefixExpression;
				parenthesizedExpression= (ParenthesizedExpression) prefixExpression.getOperand();
			}
		}
		// check for case when cursor is on parenthesized expression that is negated
		if ((covering instanceof ParenthesizedExpression)
			&& (covering.getParent() instanceof PrefixExpression)
			&& (((PrefixExpression) covering.getParent()).getOperator() == PrefixExpression.Operator.NOT)) {
			negationExpression= (PrefixExpression) covering.getParent();
			parenthesizedExpression= (ParenthesizedExpression) covering;
		}
		//
		if (negationExpression == null) {
			return false;
		}
		//  we could produce quick assist
		if (resultingCollections == null) {
			return true;
		}
		//
		final AST ast= covering.getAST();
		final ASTRewrite rewrite= ASTRewrite.create(ast);
		// prepared inverted expression
		Expression inversedExpression= getInversedBooleanExpression(rewrite, parenthesizedExpression.getExpression());
		// check, may be we should keep parentheses
		boolean keepParentheses= false;
		if (negationExpression.getParent() instanceof Expression) {
			int parentPrecedence= getExpressionPrecedence((Expression) negationExpression.getParent());
			int inversedExpressionPrecedence= getExpressionPrecedence(inversedExpression);
			keepParentheses= parentPrecedence < inversedExpressionPrecedence;
		}
		// replace negated expression with inverted one
		if (keepParentheses) {
			ParenthesizedExpression pe= ast.newParenthesizedExpression();
			pe.setExpression(inversedExpression);
			rewrite.replace(negationExpression, pe, null);
		} else {
			rewrite.replace(negationExpression, inversedExpression, null);
		}
		// add correction proposal
		String label= CorrectionMessages.AdvancedQuickAssistProcessor_pushNegationDown;
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, 1, image);
		resultingCollections.add(proposal);
		return true;
	}
	
	private static Expression getBooleanExpression(ASTNode node) {
		if (!(node instanceof Expression)) {
			return null;
		}

		// check if the node is a location where it can be negated
		StructuralPropertyDescriptor locationInParent= node.getLocationInParent();
		if (locationInParent == QualifiedName.NAME_PROPERTY) {
			node= node.getParent();
			locationInParent= node.getLocationInParent();
		}
		while (locationInParent == ParenthesizedExpression.EXPRESSION_PROPERTY) {
			node= node.getParent();
			locationInParent= node.getLocationInParent();
		}
		Expression expression= (Expression) node;
		if (!isBoolean(expression)) {
			return null;
		}
		if (expression.getParent() instanceof InfixExpression) {
			return expression;
		}
		if (locationInParent == Assignment.RIGHT_HAND_SIDE_PROPERTY 
				|| locationInParent == IfStatement.EXPRESSION_PROPERTY 
				|| locationInParent == WhileStatement.EXPRESSION_PROPERTY 
				|| locationInParent == DoStatement.EXPRESSION_PROPERTY 
				|| locationInParent == ReturnStatement.EXPRESSION_PROPERTY 
				|| locationInParent == ForStatement.EXPRESSION_PROPERTY 
				|| locationInParent == FunctionInvocation.ARGUMENTS_PROPERTY 
				|| locationInParent == ConstructorInvocation.ARGUMENTS_PROPERTY 
				|| locationInParent == SuperMethodInvocation.ARGUMENTS_PROPERTY 
				|| locationInParent == SuperConstructorInvocation.ARGUMENTS_PROPERTY 
				|| locationInParent == ClassInstanceCreation.ARGUMENTS_PROPERTY 
				|| locationInParent == ConditionalExpression.EXPRESSION_PROPERTY 
				|| locationInParent == PrefixExpression.OPERAND_PROPERTY) {
			return expression;
		}
		return null;
	}
	
	
	
	private static boolean getPullNegationUpProposals(IInvocationContext context, ASTNode covering, ArrayList coveredNodes, Collection resultingCollections) {
		if (coveredNodes.size() != 1) {
			return false;
		}
		//
		ASTNode fullyCoveredNode= (ASTNode) coveredNodes.get(0);

		Expression expression= getBooleanExpression(fullyCoveredNode);
		if (expression == null) {
			return false;
		}
		//  we could produce quick assist
		if (resultingCollections == null) {
			return true;
		}
		//
		AST ast= expression.getAST();
		final ASTRewrite rewrite= ASTRewrite.create(ast);
		// prepared inverted expression
		Expression inversedExpression= getInversedBooleanExpression(rewrite, expression);
		// prepare ParenthesizedExpression
		ParenthesizedExpression parenthesizedExpression = ast.newParenthesizedExpression();
		parenthesizedExpression.setExpression(inversedExpression);
		// prepare NOT prefix expression
		PrefixExpression prefixExpression = ast.newPrefixExpression();
		prefixExpression.setOperator(PrefixExpression.Operator.NOT);
		prefixExpression.setOperand(parenthesizedExpression);
		// replace old expression
		rewrite.replace(expression, prefixExpression, null);
		// add correction proposal
		String label= CorrectionMessages.AdvancedQuickAssistProcessor_pullNegationUp;
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, 1, image);
		resultingCollections.add(proposal);
		return true;
	}
	private static boolean getJoinIfListInIfElseIfProposals(IInvocationContext context,
			ASTNode covering,
			ArrayList coveredNodes,
			Collection resultingCollections) {
		if (coveredNodes.isEmpty()) {
			return false;
		}
		// check that we have more than one covered statement
		if (coveredNodes.size() < 2) {
			return false;
		}
		// check that all selected nodes are 'if' statements with only 'then' statement
		for (Iterator iter= coveredNodes.iterator(); iter.hasNext();) {
			ASTNode node= (ASTNode) iter.next();
			if (!(node instanceof IfStatement)) {
				return false;
			}
			IfStatement ifStatement= (IfStatement) node;
			if (ifStatement.getElseStatement() != null) {
				return false;
			}
		}
		//  we could produce quick assist
		if (resultingCollections == null) {
			return true;
		}
		//
		final AST ast= covering.getAST();
		final ASTRewrite rewrite= ASTRewrite.create(ast);
		//
		IfStatement firstIfStatement= (IfStatement) coveredNodes.get(0);
		IfStatement firstNewIfStatement= null;
		//
		IfStatement prevIfStatement= null;
		for (Iterator iter= coveredNodes.iterator(); iter.hasNext();) {
			IfStatement ifStatement= (IfStatement) iter.next();
			// prepare new 'if' statement
			IfStatement newIfStatement= ast.newIfStatement();
			newIfStatement.setExpression((Expression) rewrite.createMoveTarget(ifStatement.getExpression()));
			// prepare 'then' statement and convert into block if needed
			Statement thenStatement= (Statement) rewrite.createMoveTarget(ifStatement.getThenStatement());
			if (ifStatement.getThenStatement() instanceof IfStatement) {
				IfStatement ifBodyStatement= (IfStatement) ifStatement.getThenStatement();
				if (ifBodyStatement.getElseStatement() == null) {
					Block thenBlock= ast.newBlock();
					thenBlock.statements().add(thenStatement);
					thenStatement= thenBlock;
				}
			}
			newIfStatement.setThenStatement(thenStatement);
			//
			if (prevIfStatement != null) {
				prevIfStatement.setElseStatement(newIfStatement);
				rewrite.remove(ifStatement, null);
			} else {
				firstNewIfStatement= newIfStatement;
			}
			prevIfStatement= newIfStatement;
		}
		rewrite.replace(firstIfStatement, firstNewIfStatement, null);
		// add correction proposal
		String label= CorrectionMessages.AdvancedQuickAssistProcessor_joinIfSequence;
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, 1, image);
		resultingCollections.add(proposal);
		return true;
	}
	private static boolean getConvertSwitchToIfProposals(IInvocationContext context,
			ASTNode covering,
			Collection resultingCollections) {
		if (!(covering instanceof SwitchStatement)) {
			return false;
		}
		//  we could produce quick assist (if all 'case' statements end with 'break')
		if (resultingCollections == null) {
			return true;
		}
		//
		final AST ast= covering.getAST();
		final ASTRewrite rewrite= ASTRewrite.create(ast);
		final ImportRewrite importRewrite= CodeStyleConfiguration.createImportRewrite(context.getASTRoot(), true);
		//
		SwitchStatement switchStatement= (SwitchStatement) covering;
		IfStatement firstIfStatement= null;
		IfStatement currentIfStatement= null;
		Block currentBlock= null;
		boolean hasStopAsLastExecutableStatement = false;
		Block defaultBlock= null;
		InfixExpression currentCondition= null;
		boolean defaultFound= false;
		int caseCount = 0;
		
		ArrayList allBlocks= new ArrayList();
		//
		for (Iterator iter= switchStatement.statements().iterator(); iter.hasNext();) {
			Statement statement= (Statement) iter.next();
			if (statement instanceof SwitchCase) {
				SwitchCase switchCase= (SwitchCase) statement;
				caseCount++;
				// special case: pass through
				if (currentBlock != null) {
					if (!hasStopAsLastExecutableStatement) {
						return false;
					}
					currentBlock= null;
				}
				// for 'default' we just will not create condition
				if (switchCase.isDefault()) {
					defaultFound= true;
					if (currentCondition != null) {
						// we can not convert one or more 'case' statements and 'default' nor in conditional if, nor in 'else' without code duplication
						return false;
					}
					continue;
				}
				if (defaultFound) {
					return false;
				}
				// prepare condition
				InfixExpression switchCaseCondition= createSwitchCaseCondition(ast, rewrite, importRewrite, switchStatement, switchCase);
				if (currentCondition == null) {
					currentCondition= switchCaseCondition;
				} else {
					InfixExpression condition= ast.newInfixExpression();
					condition.setOperator(InfixExpression.Operator.CONDITIONAL_OR);
					condition.setLeftOperand(currentCondition);
					condition.setRightOperand(switchCaseCondition);
					currentCondition= condition;
				}
			} else if (statement instanceof BreakStatement) {
				currentBlock= null;
			} else {
				// ensure that current block exists as 'then' statement of 'if'
				if (currentBlock == null) {
					defaultFound= false;
					if (currentCondition != null) {
						IfStatement ifStatement;
						if (firstIfStatement == null) {
							firstIfStatement= ast.newIfStatement();
							ifStatement= firstIfStatement;
						} else {
							ifStatement= ast.newIfStatement();
							currentIfStatement.setElseStatement(ifStatement);
						}
						currentIfStatement= ifStatement;
						ifStatement.setExpression(currentCondition);
						currentCondition= null;
						currentBlock= ast.newBlock();
						ifStatement.setThenStatement(currentBlock);
						allBlocks.add(currentBlock);
					} else {
						// case for default:
						defaultBlock= ast.newBlock();
						currentBlock= defaultBlock;
						allBlocks.add(currentBlock);
						// delay adding of default block
					}
				}
				// add current statement in current block
				{
					hasStopAsLastExecutableStatement = hasStopAsLastExecutableStatement(statement);
					Statement copyStatement= copyStatementExceptBreak(ast, rewrite, statement);
										
					
					currentBlock.statements().add(copyStatement);
				}
			}
		}
		// check, may be we have delayed default block
		if (defaultBlock != null) {
			currentIfStatement.setElseStatement(defaultBlock);
		}
		// remove unnecessary blocks in blocks
		for (int i= 0; i < allBlocks.size(); i++) {
			Block block= (Block) allBlocks.get(i);
			List statements= block.statements();
			if (statements.size() == 1 && statements.get(0) instanceof Block) {
				Block innerBlock= (Block) statements.remove(0);
				block.getParent().setStructuralProperty(block.getLocationInParent(), innerBlock);
			}
		}
		// replace 'switch' with single if-else-if statement
		rewrite.replace(switchStatement, firstIfStatement, null);
		// prepare label, specially for Daniel :-)
		String label= CorrectionMessages.AdvancedQuickAssistProcessor_convertSwitchToIf;

		// add correction proposal
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label,
			context.getCompilationUnit(),
			rewrite,
			1,
			image);
		proposal.setImportRewrite(importRewrite);
		resultingCollections.add(proposal);
		return true;
	}
	private static InfixExpression createSwitchCaseCondition(AST ast, ASTRewrite rewrite, ImportRewrite importRewrite,
	                                                      SwitchStatement switchStatement, SwitchCase switchCase) {
		InfixExpression condition= ast.newInfixExpression();
		condition.setOperator(InfixExpression.Operator.EQUALS);
		//
		Expression leftExpression= (Expression) rewrite.createCopyTarget(switchStatement.getExpression());
		condition.setLeftOperand(leftExpression);
		//
		Expression rightExpression= null;
		Expression expression= switchCase.getExpression();
		if (expression instanceof SimpleName && ((SimpleName) expression).resolveBinding() instanceof IVariableBinding) {
			((SimpleName) expression).resolveBinding();
		}
		condition.setRightOperand(rightExpression);
		//
		return condition;
	}
	
	
	private static boolean hasStopAsLastExecutableStatement(Statement lastStatement) {
		if ((lastStatement instanceof ReturnStatement) || (lastStatement instanceof BreakStatement)) {
			return true;
		}
		if (lastStatement instanceof Block) {
			Block block= (Block)lastStatement;
			lastStatement = (Statement) block.statements().get(block.statements().size() - 1);
			return hasStopAsLastExecutableStatement(lastStatement);
		}
		return false;
	}
	private static Statement copyStatementExceptBreak(AST ast, ASTRewrite rewrite, Statement source) {
		if (source instanceof Block) {
			Block block= (Block) source;
			Block newBlock= ast.newBlock();
			for (Iterator iter= block.statements().iterator(); iter.hasNext();) {
				Statement statement= (Statement) iter.next();
				if (statement instanceof BreakStatement) {
					continue;
				}
				newBlock.statements().add(copyStatementExceptBreak(ast, rewrite, statement));
			}
			return newBlock;
		}
		return (Statement) rewrite.createMoveTarget(source);
	}
}
