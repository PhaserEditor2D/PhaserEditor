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
package org.eclipse.wst.jsdt.internal.compiler.ast;

import org.eclipse.wst.jsdt.core.ast.IASTNode;
import org.eclipse.wst.jsdt.core.ast.IPrefixExpression;
import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;

public class PrefixExpression extends CompoundAssignment implements IPrefixExpression {

/**
 * PrefixExpression constructor comment.
 * @param lhs org.eclipse.wst.jsdt.internal.compiler.ast.Expression
 * @param expression org.eclipse.wst.jsdt.internal.compiler.ast.Expression
 * @param operator int
 */
public PrefixExpression(Expression lhs, Expression expression, int operator, int pos) {
	super(lhs, expression, operator, lhs.sourceEnd);
	this.sourceStart = pos;
	this.sourceEnd = lhs.sourceEnd;
}

public String operatorToString() {
	switch (this.operator) {
		case PLUS :
			return "++"; //$NON-NLS-1$
		case MINUS :
			return "--"; //$NON-NLS-1$
	}
	return "unknown operator"; //$NON-NLS-1$
}

public StringBuffer printExpressionNoParenthesis(int indent, StringBuffer output) {

	output.append(operatorToString()).append(' ');
	return this.lhs.printExpression(0, output);
}

public boolean restrainUsageToNumericTypes() {
	return true;
}

public void traverse(ASTVisitor visitor, BlockScope scope) {
	if (visitor.visit(this, scope)) {
		this.lhs.traverse(visitor, scope);
	}
	visitor.endVisit(this, scope);
}
public int getASTType() {
	return IASTNode.PREFIX_EXPRESSION;

}
}
