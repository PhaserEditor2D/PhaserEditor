/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.corext.refactoring.code;

import org.eclipse.wst.jsdt.core.dom.Assignment;
import org.eclipse.wst.jsdt.core.dom.ConditionalExpression;
import org.eclipse.wst.jsdt.core.dom.Expression;
import org.eclipse.wst.jsdt.core.dom.InfixExpression;
import org.eclipse.wst.jsdt.core.dom.InstanceofExpression;
import org.eclipse.wst.jsdt.core.dom.PostfixExpression;
import org.eclipse.wst.jsdt.core.dom.PrefixExpression;
import org.eclipse.wst.jsdt.core.dom.InfixExpression.Operator;

public class OperatorPrecedence {

	private static final int ASSIGNMENT= 			0;
	private static final int CONDITIONAL=			1;
	private static final int CONDITIONAL_OR= 		2;
	private static final int CONDITIONAL_AND= 		3;
	private static final int BITWISE_INCLUSIVE_OR=	4;
	private static final int BITWISE_EXCLUSIVE_OR=	5;
	private static final int BITWISE_AND=			6;
	private static final int EQUALITY=				7;
	private static final int RATIONAL=				8;
	private static final int SHIFT=					9;
	private static final int ADDITIVE=				10;
	private static final int MULTIPLICATIVE=		11;
	private static final int PREFIX=				12;
	private static final int POSTFIX=				13;
	
	public static int getValue(Expression expression) {
		if (expression instanceof InfixExpression) {
			return getNormalizedValue((InfixExpression)expression); 
		} else if (expression instanceof PostfixExpression) {
			return getNormalizedValue((PostfixExpression)expression);
		} else if (expression instanceof PrefixExpression) {
			return getNormalizedValue((PrefixExpression)expression);
		} else if (expression instanceof Assignment) {
			return getNormalizedValue((Assignment)expression);
		} else if (expression instanceof ConditionalExpression) {
			return getNormalizedValue((ConditionalExpression)expression);
		} else if (expression instanceof InstanceofExpression) {
			return getNormalizedValue((InstanceofExpression)expression);
		}
		return -1;
	}
	
	private static int getNormalizedValue(Assignment ass) {
		return ASSIGNMENT;
	}
	
	private static int getNormalizedValue(ConditionalExpression exp) {
		return CONDITIONAL;
	}
	
	private static int getNormalizedValue(InfixExpression exp) {
		Operator operator= exp.getOperator();
		if (operator == Operator.CONDITIONAL_OR) {
			return CONDITIONAL_OR;
		} else if (operator == Operator.CONDITIONAL_AND) {
			return CONDITIONAL_AND;
		} else if (operator == Operator.OR) {
			return BITWISE_INCLUSIVE_OR;
		} else if (operator == Operator.XOR) {
			return BITWISE_EXCLUSIVE_OR;
		} else if (operator == Operator.AND) {
			return BITWISE_AND;
		} else if (operator == Operator.EQUALS || operator == Operator.NOT_EQUALS) {
			return EQUALITY;
		} else if (operator == Operator.LESS || operator == Operator.LESS_EQUALS || operator == Operator.GREATER || operator == Operator.GREATER_EQUALS) {
			return RATIONAL;
		} else if (operator == Operator.LEFT_SHIFT || operator == Operator.RIGHT_SHIFT_SIGNED || operator == Operator.RIGHT_SHIFT_UNSIGNED) {
			return SHIFT;
		} else if (operator == Operator.PLUS || operator == Operator.MINUS) {
			return ADDITIVE;
		} else if (operator == Operator.REMAINDER || operator == Operator.DIVIDE || operator == Operator.TIMES) {
			return MULTIPLICATIVE;
		}
		return -1;
	}
	
	private static int getNormalizedValue(InstanceofExpression exp) {
		return RATIONAL;
	}

	private static int getNormalizedValue(PrefixExpression exp) {
		return PREFIX;
	}

	private static int getNormalizedValue(PostfixExpression exp) {
		return POSTFIX;
	}
}
