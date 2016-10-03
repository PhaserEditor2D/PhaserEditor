/*******************************************************************************
 * Copyright (c) 2006, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Michael Spector <spektom@gmail.com>  Bug 242754
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.compiler.ast;

import org.eclipse.wst.jsdt.core.ast.IASTNode;
import org.eclipse.wst.jsdt.core.ast.ICombinedBinaryExpression;
import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowContext;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowInfo;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeIds;

/**
 * CombinedBinaryExpression is an implementation of BinaryExpression that
 * specifically attempts to mitigate the issues raised by expressions which
 * have a very deep leftmost branch. It does so by maintaining a table of
 * direct references to its subexpressions, and implementing non-recursive
 * variants of the most significant recursive algorithms of its ancestors.
 * The subexpressions table only holds intermediate binary expressions. Its
 * role is to provide the reversed navigation through the left relationship
 * of BinaryExpression to Expression. To cope with potentially very deep
 * left branches, an instance of CombinedBinaryExpression is created once in
 * a while, using variable thresholds held by {@link #arityMax}.
 * As a specific case, the topmost node of all binary expressions that are
 * deeper than one is a CombinedBinaryExpression, but it has no references
 * table.<br>
 * Notes:
 * <ul>
 * <li>CombinedBinaryExpression is not meant to behave in other ways than
 *     BinaryExpression in any observable respect;</li>
 * <li>visitors that implement their own traversal upon binary expressions
 *     should consider taking advantage of combined binary expressions, or
 *     else face a risk of StackOverflowError upon deep instances;</li>
 * <li>callers that need to change the operator should rebuild the expression
 *     from scratch, or else amend the references table as needed to cope with
 *     the resulting, separated expressions.</li>
 * </ul>
 */
public class CombinedBinaryExpression extends BinaryExpression implements ICombinedBinaryExpression {

	/**
	 * The number of consecutive binary expressions of this' left branch that
	 * bear the same operator as this.<br>
	 * Notes:
	 * <ul><li>the presence of a CombinedBinaryExpression instance resets
	 *         arity, even when its operator is compatible;</li>
	 *	   <li>this property is maintained by the parser.</li>
	 * </ul>
	 */
	public int arity;

	/**
	 * The threshold that will trigger the creation of the next full-fledged
	 * CombinedBinaryExpression. This field is only maintained for the
	 * topmost binary expression (it is 0 otherwise). It enables a variable
	 * policy, which scales better with very large expressions.
	 */
	public int arityMax;

	/**
	 * Upper limit for {@link #arityMax}.
	 */
	public static final int ARITY_MAX_MAX = 160;

	/**
	 * Default lower limit for {@link #arityMax}.
	 */
	public static final int ARITY_MAX_MIN = 20;

	/**
	 * Default value for the first term of the series of {@link #arityMax}
	 * values. Changing this allows for experimentation. Not meant to be
	 * changed during a parse operation.
	 */
	public static int defaultArityMaxStartingValue = ARITY_MAX_MIN;

	/**
	 * A table of references to the binary expressions of this' left branch.
	 * Instances of CombinedBinaryExpression are not repeated here. Instead,
	 * the left subexpression of referencesTable[0] may be a combined binary
	 * expression, if appropriate. Null when this only cares about tracking
	 * the expression's arity.
	 */
	public BinaryExpression referencesTable[];

/**
 * Make a new CombinedBinaryExpression. If arity is strictly greater than one,
 * a references table is built and initialized with the reverse relationship of
 * the one defined by {@link BinaryExpression#left}. arity and left must be
 * compatible with each other (that is, there must be at least arity - 1
 * consecutive compatible binary expressions into the leftmost branch of left,
 * the topmost of which being left's immediate left expression).
 * @param left the left branch expression
 * @param right the right branch expression
 * @param operator the operator for this binary expression - only PLUS for now
 * @param arity the number of binary expressions of a compatible operator that
 *        already exist into the leftmost branch of left (including left); must
 *        be strictly greater than 0
 */
public CombinedBinaryExpression(Expression left, Expression right, int operator,
		int arity) {
	super(left, right, operator);
	this.arity = arity;
	if (arity > 1) {
		this.referencesTable = new BinaryExpression[arity];
		this.referencesTable[arity - 1] = (BinaryExpression) left;
		for (int i = arity - 1; i > 0; i--) {
			this.referencesTable[i - 1] =
				(BinaryExpression) this.referencesTable[i].left;
		}
	} else {
		this.arityMax = defaultArityMaxStartingValue;
	}
}

public FlowInfo analyseCode(BlockScope currentScope, FlowContext flowContext,
		FlowInfo flowInfo) {
	// keep implementation in sync with BinaryExpression#analyseCode
	if (this.referencesTable == null) {
		return super.analyseCode(currentScope, flowContext, flowInfo);
	}
	if (this.referencesTable[0] != null && this.referencesTable[0].resolvedType != null) {
		BinaryExpression cursor;
		if ((cursor = this.referencesTable[0]).resolvedType.id !=
				TypeIds.T_JavaLangString) {
			cursor.left.checkNPE(currentScope, flowContext, flowInfo);
		}
		flowInfo = cursor.left.analyseCode(currentScope, flowContext, flowInfo).
			unconditionalInits();
		for (int i = 0, end = this.arity; i < end; i ++) {
			if ((cursor = this.referencesTable[i]).resolvedType.id !=
					TypeIds.T_JavaLangString) {
				cursor.right.checkNPE(currentScope, flowContext, flowInfo);
			}
			flowInfo = cursor.right.
				analyseCode(currentScope, flowContext, flowInfo).
					unconditionalInits();
		}
	}
	if (this.resolvedType.id != TypeIds.T_JavaLangString) {
		this.right.checkNPE(currentScope, flowContext, flowInfo);
	}
	return this.right.analyseCode(currentScope, flowContext, flowInfo).
		unconditionalInits();
}

public StringBuffer printExpressionNoParenthesis(int indent,
		StringBuffer output) {
	// keep implementation in sync with
	// BinaryExpression#printExpressionNoParenthesis and
	// OperatorExpression#printExpression
	if (this.referencesTable == null) {
		return super.printExpressionNoParenthesis(indent, output);
	}
	String operatorString = operatorToString();
	for (int i = this.arity - 1; i >= 0; i--) {
		output.append('(');
	}
	output = this.referencesTable[0].left.
		printExpression(indent, output);
	for (int i = 0, end = this.arity;
				i < end; i++) {
		output.append(' ').append(operatorString).append(' ');
		output = this.referencesTable[i].right.
			printExpression(0, output);
		output.append(')');
	}
	output.append(' ').append(operatorString).append(' ');
	return this.right.printExpression(0, output);
}

public TypeBinding resolveType(BlockScope scope) {
	// keep implementation in sync with BinaryExpression#resolveType
	if (this.referencesTable == null) {
		return super.resolveType(scope);
	}
	BinaryExpression cursor = this.referencesTable[0];
	cursor.left.resolveType(scope);
	for (int i = 0, end = this.arity; i < end; i ++) {
		this.referencesTable[i].nonRecursiveResolveTypeUpwards(scope);
	}
	nonRecursiveResolveTypeUpwards(scope);
	return this.resolvedType;
}

public void traverse(ASTVisitor visitor, BlockScope scope) {
	if (this.referencesTable == null) {
		super.traverse(visitor, scope);
	} else {
		if (visitor.visit(this, scope)) {
			int restart;
			for (restart = this.arity - 1;
					restart >= 0;
					restart--) {
				if (!visitor.visit(
						this.referencesTable[restart], scope)) {
					visitor.endVisit(
						this.referencesTable[restart], scope);
					break;
				}
			}
			restart++;
			// restart now points to the deepest BE for which
			// visit returned true, if any
			if (restart == 0) {
				this.referencesTable[0].left.traverse(visitor, scope);
			}
			for (int i = restart, end = this.arity;
						i < end; i++) {
				this.referencesTable[i].right.traverse(visitor, scope);
				visitor.endVisit(this.referencesTable[i], scope);
			}
			this.right.traverse(visitor, scope);
		}
		visitor.endVisit(this, scope);
	}
}

/**
 * Change {@link #arityMax} if and as needed. The current policy is to double
 * arityMax each time this method is called, until it reaches
 * {@link #ARITY_MAX_MAX}. Other policies may consider incrementing it less
 * agressively. Call only after an appropriate value has been assigned to
 * {@link #left}.
 */
// more sophisticate increment policies would leverage the leftmost expression
// to hold an indication of the number of uses of a given arityMax in a row
public void tuneArityMax() {
	if (this.arityMax < ARITY_MAX_MAX) {
		this.arityMax *= 2;
	}
}
public int getASTType() {
	return IASTNode.COMBINED_BINARY_EXPRESSION;

}
}
