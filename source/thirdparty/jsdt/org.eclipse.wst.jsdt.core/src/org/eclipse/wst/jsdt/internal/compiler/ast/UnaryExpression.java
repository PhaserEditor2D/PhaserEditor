/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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
import org.eclipse.wst.jsdt.core.ast.IExpression;
import org.eclipse.wst.jsdt.core.ast.IUnaryExpression;
import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowContext;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowInfo;
import org.eclipse.wst.jsdt.internal.compiler.impl.BooleanConstant;
import org.eclipse.wst.jsdt.internal.compiler.impl.Constant;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;

public class UnaryExpression extends OperatorExpression implements IUnaryExpression {

	public Expression expression;
	public Constant optimizedBooleanConstant;

	public UnaryExpression(Expression expression, int operator) {
		this.expression = expression;
		this.bits |= operator << OperatorSHIFT; // encode operator
	}

	public FlowInfo analyseCode( BlockScope currentScope, FlowContext flowContext,
		FlowInfo flowInfo) {
		this.expression.checkNPE(currentScope, flowContext, flowInfo);
		if (((bits & OperatorMASK) >> OperatorSHIFT) == NOT) {
			return this.expression.analyseCode(currentScope, flowContext, flowInfo).
				asNegatedCondition();
		} else {
			return this.expression.analyseCode(currentScope, flowContext, flowInfo);
		}
	}

	public Constant optimizedBooleanConstant() {
		return this.optimizedBooleanConstant == null
				? this.constant == null ? Constant.NotAConstant : this.constant
				: this.optimizedBooleanConstant;
	}

	public StringBuffer printExpressionNoParenthesis(int indent, StringBuffer output) {

		output.append(operatorToString()).append(' ');
		return this.expression.printExpression(0, output);
	}

	public final int getOperator() {
		return (bits & OperatorMASK) >> OperatorSHIFT;
	}

	public TypeBinding resolveType(BlockScope scope) {
		TypeBinding expressionType = null;
		if (getOperator()==TYPEOF && (this.expression instanceof SingleNameReference))
			expressionType=TypeBinding.UNKNOWN;
		else
			expressionType = this.expression.resolveType(scope);
		if (expressionType == null) {
			this.constant = Constant.NotAConstant;
			return null;
		}
		int expressionTypeID = expressionType.id;
	
		if (expressionTypeID > 15) {
			expressionTypeID=T_JavaLangObject;
//			this.constant = Constant.NotAConstant;
//			scope.problemReporter().invalidOperator(this, expressionType);
//			return null;
		}

		int tableId=-1;
		int operator = (bits & OperatorMASK) >> OperatorSHIFT;
		switch (operator) {
			case NOT :
				this.resolvedType=  TypeBinding.BOOLEAN;
				break;
			case TWIDDLE :
				tableId = LEFT_SHIFT;
				break;
			case TYPEOF :
				this.resolvedType=  scope.getJavaLangString();
				break;
			case OperatorIds.VOID :
				this.resolvedType= TypeBinding.VOID;
				break;
			default :
				tableId = MINUS;
		} //+ and - cases

		// the code is an int
		// (cast)  left   Op (cast)  rigth --> result
		//  0000   0000       0000   0000      0000
		//  <<16   <<12       <<8    <<4       <<0
		if (tableId>-1)	// not already determined
		{
		int operatorSignature = OperatorSignatures[tableId][(expressionTypeID << 4) + expressionTypeID];
		this.bits |= operatorSignature & 0xF;
		switch (operatorSignature & 0xF) { // only switch on possible result type.....
			case T_boolean :
				this.resolvedType = TypeBinding.BOOLEAN;
				break;
			case T_char :
				this.resolvedType = TypeBinding.CHAR;
				break;
			case T_double :
				this.resolvedType = TypeBinding.DOUBLE;
				break;
			case T_float :
				this.resolvedType = TypeBinding.FLOAT;
				break;
			case T_int :
				this.resolvedType = scope.getJavaLangNumber();
				break;
			case T_long :
				this.resolvedType = TypeBinding.LONG;
				break;
			case T_any :
				this.resolvedType = TypeBinding.UNKNOWN;
				break;
			default : //error........
				this.constant = Constant.NotAConstant;
				if (expressionTypeID != T_undefined)
					scope.problemReporter().invalidOperator(this, expressionType);
				return null;
		}
		}
		// compute the constant when valid
		if (this.expression.constant != Constant.NotAConstant) {
			this.constant =
				Constant.computeConstantOperation(
					this.expression.constant,
					expressionTypeID,
					operator);
		} else {
			this.constant = Constant.NotAConstant;
			if (operator == NOT) {
				Constant cst = expression.optimizedBooleanConstant();
				if (cst != Constant.NotAConstant)
					this.optimizedBooleanConstant = BooleanConstant.fromValue(!cst.booleanValue());
			}
		}
		return this.resolvedType;
	}

	public void traverse(
    		ASTVisitor visitor,
    		BlockScope blockScope) {

		if (visitor.visit(this, blockScope)) {
			this.expression.traverse(visitor, blockScope);
		}
		visitor.endVisit(this, blockScope);
	}
	
	public int getASTType() {
		return IASTNode.UNARY_EXPRESSION;
	}
	
	public IExpression getExpression() {
		return expression;
	}
}
