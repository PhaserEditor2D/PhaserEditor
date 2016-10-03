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
import org.eclipse.wst.jsdt.core.ast.IBinaryExpression;
import org.eclipse.wst.jsdt.core.ast.IExpression;
import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowContext;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowInfo;
import org.eclipse.wst.jsdt.internal.compiler.impl.Constant;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeIds;

public class BinaryExpression extends OperatorExpression implements IBinaryExpression {

/* Tracking helpers
 * The following are used to elaborate realistic statistics about binary
 * expressions. This must be neutralized in the released code.
 * Search the keyword BE_INSTRUMENTATION to reenable.
 * An external device must install a suitable probe so as to monitor the
 * emission of events and publish the results.
	public interface Probe {
		public void ping(int depth);
	}
	public int depthTracker;
	public static Probe probe;
 */

	public Expression left, right;
	public Constant optimizedBooleanConstant;

public BinaryExpression(Expression left, Expression right, int operator) {
	this.left = left;
	this.right = right;
	this.bits |= operator << ASTNode.OperatorSHIFT; // encode operator
	this.sourceStart = left.sourceStart;
	this.sourceEnd = right.sourceEnd;
	// BE_INSTRUMENTATION: neutralized in the released code
//	if (left instanceof BinaryExpression &&
//			((left.bits & OperatorMASK) ^ (this.bits & OperatorMASK)) == 0) {
//		this.depthTracker = ((BinaryExpression)left).depthTracker + 1;
//	} else {
//		this.depthTracker = 1;
//	}
}

public FlowInfo analyseCode(BlockScope currentScope, FlowContext flowContext,
		FlowInfo flowInfo) {
	// keep implementation in sync with CombinedBinaryExpression#analyseCode
	if (this.resolvedType.id == TypeIds.T_JavaLangString) {
		return this.right.analyseCode(
							currentScope, flowContext,
							this.left.analyseCode(currentScope, flowContext, flowInfo).unconditionalInits())
						.unconditionalInits();
	} else {
		this.left.checkNPE(currentScope, flowContext, flowInfo);
		flowInfo = this.left.analyseCode(currentScope, flowContext, flowInfo).unconditionalInits();
		this.right.checkNPE(currentScope, flowContext, flowInfo);
		return this.right.analyseCode(currentScope, flowContext, flowInfo).unconditionalInits();
	}
}

public void computeConstant(BlockScope scope, int leftId, int rightId) {
	//compute the constant when valid

	/* bchilds - Not sure about this change but left side was null (variable) and causing NPE further down */

	if(this.left.constant==null) this.left.constant= Constant.NotAConstant;
	if(this.right.constant==null) this.left.constant= Constant.NotAConstant;

	if ((this.left.constant != Constant.NotAConstant)
		&& (this.right.constant != Constant.NotAConstant)) {
		try {
			this.constant =
				Constant.computeConstantOperation(
					this.left.constant,
					leftId,
					(this.bits & ASTNode.OperatorMASK) >> ASTNode.OperatorSHIFT,
					this.right.constant,
					rightId);
		} catch (Exception e) {
			this.constant = Constant.NotAConstant;
			// 1.2 no longer throws an exception at compile-time
			//scope.problemReporter().compileTimeConstantThrowsArithmeticException(this);
		}
	} else {
		this.constant = Constant.NotAConstant;
		//add some work for the boolean operators & |
		this.optimizedBooleanConstant(
			leftId,
			(this.bits & ASTNode.OperatorMASK) >> ASTNode.OperatorSHIFT,
			rightId);
	}
}

public Constant optimizedBooleanConstant() {
	return this.optimizedBooleanConstant == null ? this.constant : this.optimizedBooleanConstant;
}

public boolean isCompactableOperation() {
	return true;
}

/**
 * Separates into a reusable method the subpart of {@link
 * #resolveType(BlockScope)} that needs to be executed while climbing up the
 * chain of expressions of this' leftmost branch. For use by {@link
 * CombinedBinaryExpression#resolveType(BlockScope)}.
 * @param scope the scope within which the resolution occurs
 */
void nonRecursiveResolveTypeUpwards(BlockScope scope) {
	// keep implementation in sync with BinaryExpression#resolveType
	TypeBinding leftType = this.left.resolvedType;

	TypeBinding rightType = this.right.resolveType(scope);

	// use the id of the type to navigate into the table
	if (leftType == null || rightType == null) {
		this.constant = Constant.NotAConstant;
		return;
	}

	int leftTypeID = leftType.id;
	int rightTypeID = rightType.id;

	// autoboxing support
	boolean use15specifics = scope.compilerOptions().sourceLevel >= ClassFileConstants.JDK1_5;
	if (use15specifics) {
		if (!leftType.isBaseType() && rightTypeID != TypeIds.T_JavaLangString && rightTypeID != TypeIds.T_null) {
			leftTypeID = scope.environment().computeBoxingType(leftType).id;
		}
		if (!rightType.isBaseType() && leftTypeID != TypeIds.T_JavaLangString && leftTypeID != TypeIds.T_null) {
			rightTypeID = scope.environment().computeBoxingType(rightType).id;
		}
	}
	if (leftTypeID > 15
		|| rightTypeID > 15) { // must convert String + Object || Object + String
		if (leftTypeID == TypeIds.T_JavaLangString) {
			rightTypeID = TypeIds.T_JavaLangObject;
		} else if (rightTypeID == TypeIds.T_JavaLangString) {
			leftTypeID = TypeIds.T_JavaLangObject;
		} else {
			this.constant = Constant.NotAConstant;
			scope.problemReporter().invalidOperator(this, leftType, rightType);
			return;
		}
	}

	// the code is an int
	// (cast)  left   Op (cast)  right --> result
	//  0000   0000       0000   0000      0000
	//  <<16   <<12       <<8    <<4       <<0

	// Don't test for result = 0. If it is zero, some more work is done.
	// On the one hand when it is not zero (correct code) we avoid doing the test
	int operator = (this.bits & ASTNode.OperatorMASK) >> ASTNode.OperatorSHIFT;
	int operatorSignature = OperatorExpression.OperatorSignatures[operator][(leftTypeID << 4) + rightTypeID];

	this.bits |= operatorSignature & 0xF;
	switch (operatorSignature & 0xF) { // record the current ReturnTypeID
		// only switch on possible result type.....
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
			this.resolvedType = TypeBinding.INT;
			break;
		case T_long :
			this.resolvedType = TypeBinding.LONG;
			break;
		case T_JavaLangString :
			this.resolvedType = scope.getJavaLangString();
			break;
		default : //error........
			this.constant = Constant.NotAConstant;
			scope.problemReporter().invalidOperator(this, leftType, rightType);
			return;
	}

	// compute the constant when valid
	computeConstant(scope, leftTypeID, rightTypeID);
}

public void optimizedBooleanConstant(int leftId, int operator, int rightId) {
	switch (operator) {
		case AND :
			if ((leftId != TypeIds.T_boolean) || (rightId != TypeIds.T_boolean))
				return;
		case AND_AND :
			Constant cst;
			if ((cst = this.left.optimizedBooleanConstant()) != Constant.NotAConstant) {
				if (cst.booleanValue() == false) { // left is equivalent to false
					this.optimizedBooleanConstant = cst; // constant(false)
					return;
				} else { //left is equivalent to true
					if ((cst = this.right.optimizedBooleanConstant()) != Constant.NotAConstant) {
						this.optimizedBooleanConstant = cst;
						// the conditional result is equivalent to the right conditional value
					}
					return;
				}
			}
			if ((cst = this.right.optimizedBooleanConstant()) != Constant.NotAConstant) {
				if (cst.booleanValue() == false) { // right is equivalent to false
					this.optimizedBooleanConstant = cst; // constant(false)
				}
			}
			return;
		case OR :
			if ((leftId != TypeIds.T_boolean) || (rightId != TypeIds.T_boolean))
				return;
		case OR_OR :
			if ((cst = this.left.optimizedBooleanConstant()) != Constant.NotAConstant) {
				if (cst.booleanValue() == true) { // left is equivalent to true
					this.optimizedBooleanConstant = cst; // constant(true)
					return;
				} else { //left is equivalent to false
					if ((cst = this.right.optimizedBooleanConstant()) != Constant.NotAConstant) {
						this.optimizedBooleanConstant = cst;
					}
					return;
				}
			}
			if ((cst = this.right.optimizedBooleanConstant()) != Constant.NotAConstant) {
				if (cst.booleanValue() == true) { // right is equivalent to true
					this.optimizedBooleanConstant = cst; // constant(true)
				}
			}
	}
}

public StringBuffer printExpressionNoParenthesis(int indent, StringBuffer output) {
	// keep implementation in sync with
	// CombinedBinaryExpression#printExpressionNoParenthesis
	this.left.printExpression(indent, output).append(' ').append(operatorToString()).append(' ');
	return this.right.printExpression(0, output);
}

public TypeBinding resolveType(BlockScope scope) {
	// keep implementation in sync with CombinedBinaryExpression#resolveType
	// and nonRecursiveResolveTypeUpwards
	TypeBinding leftType = this.left.resolveType(scope);
	TypeBinding rightType = this.right.resolveType(scope);

	// use the id of the type to navigate into the table
	if (leftType == null || rightType == null) {
		this.constant = Constant.NotAConstant;
		this.resolvedType=TypeBinding.ANY;
		return null;
	}
	int operator = (this.bits & ASTNode.OperatorMASK) >> ASTNode.OperatorSHIFT;

	int leftTypeID = leftType.id;
	int rightTypeID = rightType.id;

	if(operator==OperatorIds.INSTANCEOF  || operator==OperatorIds.IN  || operator==OperatorIds.OR_OR) {
		if ( rightTypeID>15)
			rightTypeID=  TypeIds.T_JavaLangObject;
		if ( leftTypeID>15)
			leftTypeID=  TypeIds.T_JavaLangObject;
	}

	// autoboxing support
	boolean use15specifics = scope.compilerOptions().sourceLevel >= ClassFileConstants.JDK1_5;
	if (use15specifics) {
		if (!leftType.isBaseType() && rightTypeID != TypeIds.T_JavaLangString && rightTypeID != TypeIds.T_null) {
			leftTypeID = scope.environment().computeBoxingType(leftType).id;
		}
		if (!rightType.isBaseType() && leftTypeID != TypeIds.T_JavaLangString && leftTypeID != TypeIds.T_null) {
			rightTypeID = scope.environment().computeBoxingType(rightType).id;
		}
	}
	if (rightType.isArrayType())
	{
		rightType=rightType.leafComponentType();
		rightTypeID=rightType.id;
	}
	if (leftTypeID > 15
		|| rightTypeID > 15) { // must convert String + Object || Object + String

		if (leftTypeID == TypeIds.T_JavaLangString) {
			rightTypeID = TypeIds.T_JavaLangObject;
		} else if (rightTypeID == TypeIds.T_JavaLangString) {
			leftTypeID = TypeIds.T_JavaLangObject;
		} else {

			this.constant = Constant.NotAConstant;
			scope.problemReporter().invalidOperator(this, leftType, rightType);
			return null;
		}
	}

	// the code is an int
	// (cast)  left   Op (cast)  right --> result
	//  0000   0000       0000   0000      0000
	//  <<16   <<12       <<8    <<4       <<0

	// Don't test for result = 0. If it is zero, some more work is done.
	// On the one hand when it is not zero (correct code) we avoid doing the test
	int operatorSignature = OperatorExpression.OperatorSignatures[operator][(leftTypeID << 4) + rightTypeID];

	this.bits |= operatorSignature & 0xF;
	switch (operatorSignature & 0xF) { // record the current ReturnTypeID
		// only switch on possible result type.....
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
		case T_JavaLangString :
			this.resolvedType = scope.getJavaLangString();
			break;
		case T_any:
			this.resolvedType = TypeBinding.UNKNOWN;
			break;
		case T_function:
			this.resolvedType = scope.getJavaLangFunction();
			break;
		case T_JavaLangObject:
			this.resolvedType = scope.getJavaLangObject();
			break;
		default : //error........
			this.constant = Constant.NotAConstant;
			scope.problemReporter().invalidOperator(this, leftType, rightType);
			return null;
	}
	
	// compute the constant when valid
	computeConstant(scope, leftTypeID, rightTypeID);
	return this.resolvedType;
}

public void traverse(ASTVisitor visitor, BlockScope scope) {
	if (visitor.visit(this, scope)) {
		this.left.traverse(visitor, scope);
		this.right.traverse(visitor, scope);
	}
	visitor.endVisit(this, scope);
}
public int getASTType() {
	return IASTNode.BINARY_EXPRESSION;

}
public IExpression getLeft() {
	return left;
}
public IExpression getRight() {
	return right;
}
}
