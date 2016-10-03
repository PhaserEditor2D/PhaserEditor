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
import org.eclipse.wst.jsdt.core.ast.IPostfixExpression;
import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;

public class PostfixExpression extends CompoundAssignment implements IPostfixExpression {

public PostfixExpression(Expression lhs, Expression expression, int operator, int pos) {
	super(lhs, expression, operator, pos);
	this.sourceStart = lhs.sourceStart;
	this.sourceEnd = pos;
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
	return this.lhs.printExpression(indent, output).append(' ').append(operatorToString());
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
	return IASTNode.POSTFIX_EXPRESSION;

}
}
