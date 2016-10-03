/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.corext.fix;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.text.edits.TextEditGroup;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ASTVisitor;
import org.eclipse.wst.jsdt.core.dom.Assignment;
import org.eclipse.wst.jsdt.core.dom.ClassInstanceCreation;
import org.eclipse.wst.jsdt.core.dom.ConditionalExpression;
import org.eclipse.wst.jsdt.core.dom.Expression;
import org.eclipse.wst.jsdt.core.dom.FunctionInvocation;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.InfixExpression;
import org.eclipse.wst.jsdt.core.dom.InfixExpression.Operator;
import org.eclipse.wst.jsdt.core.dom.InstanceofExpression;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.ParenthesizedExpression;
import org.eclipse.wst.jsdt.core.dom.PostfixExpression;
import org.eclipse.wst.jsdt.core.dom.PrefixExpression;
import org.eclipse.wst.jsdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

public class ExpressionsFix extends AbstractFix {

	private static final class MissingParenthesisVisitor extends ASTVisitor {
		
		private final ArrayList fNodes;

		private MissingParenthesisVisitor(ArrayList nodes) {
			fNodes= nodes;
		}

		public void postVisit(ASTNode node) {
			// check that parent is && or ||
			if (!(node.getParent() instanceof InfixExpression))
				return;
			
			// we want to add parenthesis around arithmetic operators and instanceof
			boolean needParenthesis = false;
			if (node instanceof InfixExpression) {
				InfixExpression expression = (InfixExpression) node;
				InfixExpression.Operator operator = expression.getOperator();

				InfixExpression parentExpression = (InfixExpression) node.getParent();
				InfixExpression.Operator parentOperator = parentExpression.getOperator();
				
				if (parentOperator == operator)
					return;
				
				needParenthesis= (operator == InfixExpression.Operator.LESS)
						|| (operator == InfixExpression.Operator.GREATER)
						|| (operator == InfixExpression.Operator.LESS_EQUALS)
						|| (operator == InfixExpression.Operator.GREATER_EQUALS)
						|| (operator == InfixExpression.Operator.EQUALS)
						|| (operator == InfixExpression.Operator.NOT_EQUALS)
						
						|| (operator == InfixExpression.Operator.CONDITIONAL_AND)
						|| (operator == InfixExpression.Operator.CONDITIONAL_OR);
			}
			if (node instanceof InstanceofExpression) {
				needParenthesis = true;
			}
			if (!needParenthesis) {
				return;
			}
			fNodes.add(node);
		}
	}
	
	private static final class UnnecessaryParenthesisVisitor extends ASTVisitor {
		private final ArrayList fNodes;

		private UnnecessaryParenthesisVisitor(ArrayList nodes) {
			fNodes= nodes;
		}

		public void postVisit(ASTNode node) {
			if (!(node instanceof ParenthesizedExpression)) {
				return;
			}
			ParenthesizedExpression parenthesizedExpression= (ParenthesizedExpression) node;
			Expression expression= parenthesizedExpression.getExpression();
			while (expression instanceof ParenthesizedExpression) {
				expression= ((ParenthesizedExpression) expression).getExpression();
			}
			// if this is part of another expression, check for this and parent precedences
			if (parenthesizedExpression.getParent() instanceof Expression) {
				Expression parentExpression= (Expression) parenthesizedExpression.getParent();
				int expressionPrecedence= getExpressionPrecedence(expression);
				int parentPrecedence= getExpressionPrecedence(parentExpression);
				if ((expressionPrecedence > parentPrecedence)
					&& !(parenthesizedExpression.getParent() instanceof ParenthesizedExpression)) {
					return;
				}
				// check for case when precedences for expression and parent are same
				if ((expressionPrecedence == parentPrecedence) && (parentExpression instanceof InfixExpression)) {
					//we have expr infix (expr infix expr) removing the parenthesis is equal to (expr infix expr) infix expr
					InfixExpression parentInfix= (InfixExpression) parentExpression;
 					Operator parentOperator= parentInfix.getOperator();
					if (parentInfix.getLeftOperand() == parenthesizedExpression) {
						fNodes.add(node);
					} else if (isAssoziative(parentOperator)) {
						if (parentOperator == InfixExpression.Operator.PLUS) {
							if (isStringExpression(parentInfix.getLeftOperand())
								|| isStringExpression(parentInfix.getRightOperand())) {
								return;
							}
							for (Iterator J= parentInfix.extendedOperands().iterator(); J.hasNext();) {
								Expression operand= (Expression) J.next();
								if (isStringExpression(operand)) {
									return;
								}
							}
						}
						fNodes.add(node);
					}
					return;
				} else if (expressionPrecedence == parentPrecedence && parentExpression instanceof ConditionalExpression) {
					if (((ConditionalExpression)parentExpression).getElseExpression() != parenthesizedExpression)
						return;
				}
			}
			fNodes.add(node);
		}

		//is e1 op (e2 op e3) == (e1 op e2) op e3 == e1 op e2 op e3 for 'operator'? 
		private boolean isAssoziative(Operator operator) {
			if (operator == InfixExpression.Operator.PLUS)
				return true;
			
			if (operator == InfixExpression.Operator.CONDITIONAL_AND)
				return true;
			
			if (operator == InfixExpression.Operator.CONDITIONAL_OR)
				return true;
			
			if (operator == InfixExpression.Operator.AND)
				return true;
			
			if (operator == InfixExpression.Operator.OR)
				return true;
			
			if (operator == InfixExpression.Operator.XOR)
				return true;
			
			if (operator == InfixExpression.Operator.TIMES)
				return true;
			
			return false;
		}

		private static int getExpressionPrecedence(Expression expression) {
			if (expression instanceof PostfixExpression || expression instanceof FunctionInvocation) {
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
			if ((operator == InfixExpression.Operator.EQUALS) || (operator == InfixExpression.Operator.NOT_EQUALS)) {
				return 7;
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
		
	}

	private static class AddParenthesisOperation extends AbstractFixRewriteOperation {

		private final Expression[] fExpressions;

		public AddParenthesisOperation(Expression[] expressions) {
			fExpressions= expressions;
		}

		/**
		 * {@inheritDoc}
		 */
		public void rewriteAST(CompilationUnitRewrite cuRewrite, List textEditGroups) throws CoreException {
			TextEditGroup group= createTextEditGroup(FixMessages.ExpressionsFix_addParanoiacParenthesis_description);
			textEditGroups.add(group);
			
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();

			for (int i= 0; i < fExpressions.length; i++) {
				// add parenthesis around expression
				Expression expression= fExpressions[i];
				
				ParenthesizedExpression parenthesizedExpression= ast.newParenthesizedExpression();
				parenthesizedExpression.setExpression((Expression) rewrite.createCopyTarget(expression));
				rewrite.replace(expression, parenthesizedExpression, group);
			}
		}
	}
	
	private static class RemoveParenthesisOperation extends AbstractFixRewriteOperation {

		private final HashSet/*<ParenthesizedExpression>*/ fExpressions;

		public RemoveParenthesisOperation(HashSet expressions) {
			fExpressions= expressions;
		}

		/**
		 * {@inheritDoc}
		 */
		public void rewriteAST(CompilationUnitRewrite cuRewrite, List textEditGroups) throws CoreException {
			TextEditGroup group= createTextEditGroup(FixMessages.ExpressionsFix_removeUnnecessaryParenthesis_description);
			textEditGroups.add(group);
			
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
						
			while (fExpressions.size() > 0) {
				ParenthesizedExpression parenthesizedExpression= (ParenthesizedExpression)fExpressions.iterator().next();
				fExpressions.remove(parenthesizedExpression);
				ParenthesizedExpression down= parenthesizedExpression;
				while (fExpressions.contains(down.getExpression())) {
					down= (ParenthesizedExpression)down.getExpression();
					fExpressions.remove(down);
				}
				
				ASTNode move= rewrite.createMoveTarget(down.getExpression());
				
				ParenthesizedExpression top= parenthesizedExpression;
				while (fExpressions.contains(top.getParent())) {
					top= (ParenthesizedExpression)top.getParent();
					fExpressions.remove(top);
				}
				
				rewrite.replace(top, move, group);
			}
		}
	}
	
	public static IFix createAddParanoidalParenthesisFix(JavaScriptUnit compilationUnit, ASTNode[] coveredNodes) throws CoreException {
		if (coveredNodes == null)
			return null;
		
		if (coveredNodes.length == 0)
			return null;
		// check sub-expressions in fully covered nodes
		final ArrayList changedNodes = new ArrayList();
		for (int i= 0; i < coveredNodes.length; i++) {
			ASTNode covered = coveredNodes[i];
			if (covered instanceof InfixExpression)
				covered.accept(new MissingParenthesisVisitor(changedNodes));
		}
		if (changedNodes.isEmpty())
			return null;
		

		IFixRewriteOperation op= new AddParenthesisOperation((Expression[])changedNodes.toArray(new Expression[changedNodes.size()]));
		return new ExpressionsFix(FixMessages.ExpressionsFix_addParanoiacParenthesis_description, compilationUnit, new IFixRewriteOperation[] {op});
	}
	
	public static IFix createRemoveUnnecessaryParenthesisFix(JavaScriptUnit compilationUnit, ASTNode[] nodes) {
		// check sub-expressions in fully covered nodes
		final ArrayList changedNodes= new ArrayList();
		for (int i= 0; i < nodes.length; i++) {
			ASTNode covered= nodes[i];
			if (covered instanceof ParenthesizedExpression || covered instanceof InfixExpression)
				covered.accept(new UnnecessaryParenthesisVisitor(changedNodes));
		}
		if (changedNodes.isEmpty())
			return null;
		
		HashSet expressions= new HashSet(changedNodes);
		RemoveParenthesisOperation op= new RemoveParenthesisOperation(expressions);
		return new ExpressionsFix(FixMessages.ExpressionsFix_removeUnnecessaryParenthesis_description, compilationUnit, new IFixRewriteOperation[] {op});
	}
	
	public static IFix createCleanUp(JavaScriptUnit compilationUnit, 
			boolean addParanoicParentesis,
			boolean removeUnnecessaryParenthesis) {
		
		if (addParanoicParentesis) {
			final ArrayList changedNodes = new ArrayList();
			compilationUnit.accept(new MissingParenthesisVisitor(changedNodes));

			if (changedNodes.isEmpty())
				return null;
			
			IFixRewriteOperation op= new AddParenthesisOperation((Expression[])changedNodes.toArray(new Expression[changedNodes.size()]));
			return new ExpressionsFix(FixMessages.ExpressionsFix_add_parenthesis_change_name, compilationUnit, new IFixRewriteOperation[] {op});
		} else if (removeUnnecessaryParenthesis) {
			final ArrayList changedNodes = new ArrayList();
			compilationUnit.accept(new UnnecessaryParenthesisVisitor(changedNodes));

			if (changedNodes.isEmpty())
				return null;
			
			HashSet expressions= new HashSet(changedNodes);
			IFixRewriteOperation op= new RemoveParenthesisOperation(expressions);
			return new ExpressionsFix(FixMessages.ExpressionsFix_remove_parenthesis_change_name, compilationUnit, new IFixRewriteOperation[] {op});
		}
		return null;
	}
	
	private static boolean isStringExpression(Expression expression) {
		ITypeBinding binding = expression.resolveTypeBinding();
		return binding.getQualifiedName().equals("String"); //$NON-NLS-1$
	}

	protected ExpressionsFix(String name, JavaScriptUnit compilationUnit, IFixRewriteOperation[] fixRewriteOperations) {
		super(name, compilationUnit, fixRewriteOperations);
	}

}
