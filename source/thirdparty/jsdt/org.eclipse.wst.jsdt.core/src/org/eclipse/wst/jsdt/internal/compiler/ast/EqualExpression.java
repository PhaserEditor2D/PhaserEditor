/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
import org.eclipse.wst.jsdt.core.ast.IEqualExpression;
import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowContext;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowInfo;
import org.eclipse.wst.jsdt.internal.compiler.flow.UnconditionalFlowInfo;
import org.eclipse.wst.jsdt.internal.compiler.impl.BooleanConstant;
import org.eclipse.wst.jsdt.internal.compiler.impl.Constant;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TagBits;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;

public class EqualExpression extends BinaryExpression implements IEqualExpression {

	public EqualExpression(Expression left, Expression right,int operator) {
		super(left,right,operator);
	}
	private void checkNullComparison(BlockScope scope, FlowContext flowContext, FlowInfo flowInfo, FlowInfo initsWhenTrue, FlowInfo initsWhenFalse) {

		LocalVariableBinding local = this.left.localVariableBinding();
		if (local != null && (local.type.tagBits & TagBits.IsBaseType) == 0) {
			checkVariableComparison(scope, flowContext, flowInfo, initsWhenTrue, initsWhenFalse, local, right.nullStatus(flowInfo), this.left);
		}
		local = this.right.localVariableBinding();
		if (local != null && (local.type.tagBits & TagBits.IsBaseType) == 0) {
			checkVariableComparison(scope, flowContext, flowInfo, initsWhenTrue, initsWhenFalse, local, left.nullStatus(flowInfo), this.right);
		}
	}
	private void checkVariableComparison(BlockScope scope, FlowContext flowContext, FlowInfo flowInfo, FlowInfo initsWhenTrue, FlowInfo initsWhenFalse, LocalVariableBinding local, int nullStatus, Expression reference) {
		switch (nullStatus) {
		case FlowInfo.NULL :
			if (((this.bits & OperatorMASK) >> OperatorSHIFT) == EQUAL_EQUAL) {
				flowContext.recordUsingNullReference(scope, local, reference,
						FlowContext.CAN_ONLY_NULL_NON_NULL | FlowContext.IN_COMPARISON_NULL, flowInfo);
				initsWhenTrue.markAsComparedEqualToNull(local); // from thereon it is set
				initsWhenFalse.markAsComparedEqualToNonNull(local); // from thereon it is set
			} else {
				flowContext.recordUsingNullReference(scope, local, reference,
						FlowContext.CAN_ONLY_NULL_NON_NULL | FlowContext.IN_COMPARISON_NON_NULL, flowInfo);
				initsWhenTrue.markAsComparedEqualToNonNull(local); // from thereon it is set
				initsWhenFalse.markAsComparedEqualToNull(local); // from thereon it is set
			}
			break;
		case FlowInfo.NON_NULL :
			if (((this.bits & OperatorMASK) >> OperatorSHIFT) == EQUAL_EQUAL) {
				flowContext.recordUsingNullReference(scope, local, reference,
						FlowContext.CAN_ONLY_NULL | FlowContext.IN_COMPARISON_NON_NULL, flowInfo);
				initsWhenTrue.markAsComparedEqualToNonNull(local); // from thereon it is set
			} else {
				flowContext.recordUsingNullReference(scope, local, reference,
						FlowContext.CAN_ONLY_NULL | FlowContext.IN_COMPARISON_NULL, flowInfo);
			}
			break;
	}
	// we do not impact enclosing try context because this kind of protection
	// does not preclude the variable from being null in an enclosing scope
	}

	public FlowInfo analyseCode(BlockScope currentScope, FlowContext flowContext, FlowInfo flowInfo) {
		FlowInfo result;
		if (((bits & OperatorMASK) >> OperatorSHIFT) == EQUAL_EQUAL) {
			if ((left.constant != Constant.NotAConstant) && (left.constant.typeID() == T_boolean)) {
				if (left.constant.booleanValue()) { //  true == anything
					//  this is equivalent to the right argument inits
					result = right.analyseCode(currentScope, flowContext, flowInfo);
				} else { // false == anything
					//  this is equivalent to the right argument inits negated
					result = right.analyseCode(currentScope, flowContext, flowInfo).asNegatedCondition();
				}
			}
			else if (right.constant != null && (right.constant != Constant.NotAConstant) && (right.constant.typeID() == T_boolean)) {
				if (right.constant.booleanValue()) { //  anything == true
					//  this is equivalent to the left argument inits
					result = left.analyseCode(currentScope, flowContext, flowInfo);
				} else { // anything == false
					//  this is equivalent to the right argument inits negated
					result = left.analyseCode(currentScope, flowContext, flowInfo).asNegatedCondition();
				}
			}
			else {
				result = right.analyseCode(
					currentScope, flowContext,
					left.analyseCode(currentScope, flowContext, flowInfo).unconditionalInits()).unconditionalInits();
			}
		} else { //NOT_EQUAL :
			if ((left.constant != Constant.NotAConstant) && (left.constant.typeID() == T_boolean)) {
				if (!left.constant.booleanValue()) { //  false != anything
					//  this is equivalent to the right argument inits
					result = right.analyseCode(currentScope, flowContext, flowInfo);
				} else { // true != anything
					//  this is equivalent to the right argument inits negated
					result = right.analyseCode(currentScope, flowContext, flowInfo).asNegatedCondition();
				}
			}
			else if ((right.constant != Constant.NotAConstant) && (right.constant.typeID() == T_boolean)) {
				if (!right.constant.booleanValue()) { //  anything != false
					//  this is equivalent to the right argument inits
					result = left.analyseCode(currentScope, flowContext, flowInfo);
				} else { // anything != true
					//  this is equivalent to the right argument inits negated
					result = left.analyseCode(currentScope, flowContext, flowInfo).asNegatedCondition();
				}
			}
			else {
				result = right.analyseCode(
					currentScope, flowContext,
					left.analyseCode(currentScope, flowContext, flowInfo).unconditionalInits()).
					/* unneeded since we flatten it: asNegatedCondition(). */
					unconditionalInits();
			}
		}
		if (result instanceof UnconditionalFlowInfo &&
				(result.tagBits & FlowInfo.UNREACHABLE) == 0) { // the flow info is flat
			result = FlowInfo.conditional(result.copy(), result.copy());
			// TODO (maxime) check, reintroduced copy
		}
	  checkNullComparison(currentScope, flowContext, result, result.initsWhenTrue(), result.initsWhenFalse());
	  return result;
	}

	public final void computeConstant(TypeBinding leftType, TypeBinding rightType) {
		if ((this.left.constant != Constant.NotAConstant) && (this.right.constant != Constant.NotAConstant)) {
			this.constant =
				Constant.computeConstantOperationEQUAL_EQUAL(
					left.constant,
					leftType.id,
					right.constant,
					rightType.id);
			if (((this.bits & OperatorMASK) >> OperatorSHIFT) == NOT_EQUAL)
				constant = BooleanConstant.fromValue(!constant.booleanValue());
		} else {
			this.constant = Constant.NotAConstant;
			// no optimization for null == null
		}
	}
	public boolean isCompactableOperation() {
		return false;
	}
	public TypeBinding resolveType(BlockScope scope) {

		constant = Constant.NotAConstant;
		TypeBinding originalLeftType = left.resolveType(scope);
		TypeBinding originalRightType = right.resolveType(scope);

		// always return BooleanBinding
		if (originalLeftType == null || originalRightType == null){
			constant = Constant.NotAConstant;
			return TypeBinding.BOOLEAN;
		}

		// autoboxing support
		boolean use15specifics = scope.compilerOptions().sourceLevel >= ClassFileConstants.JDK1_5;
		TypeBinding leftType = originalLeftType, rightType = originalRightType;
		if (use15specifics) {
			if (leftType != TypeBinding.NULL && leftType.isBaseType()) {
				if (!rightType.isBaseType()) {
					rightType = scope.environment().computeBoxingType(rightType);
				}
			} else {
				if (rightType != TypeBinding.NULL && rightType.isBaseType()) {
					leftType = scope.environment().computeBoxingType(leftType);
				}
			}
		}
		// both base type
		if (leftType.isAnyType() || rightType.isAnyType())
		{
		  return  TypeBinding.BOOLEAN;
		}
		if ( leftType.isBasicType() && rightType.isBasicType()) {
			int leftTypeID = leftType.id;
			int rightTypeID = rightType.id;

			// the code is an int
			// (cast)  left   == (cast)  right --> result
			//  0000   0000       0000   0000      0000
			//  <<16   <<12       <<8    <<4       <<0
			int operatorSignature = OperatorSignatures[EQUAL_EQUAL][ (leftTypeID << 4) + rightTypeID];
			bits |= operatorSignature & 0xF;
			// fix for https://bugs.eclipse.org/bugs/show_bug.cgi?id=283663
			if ((operatorSignature & 0x0000F) == T_undefined) {
				constant = Constant.NotAConstant;
				//scope.problemReporter().invalidOperator(this, leftType, rightType);
				return TypeBinding.BOOLEAN;
			}

			computeConstant(leftType, rightType);
			return this.resolvedType = TypeBinding.BOOLEAN;
		}

		// Object references
		// spec 15.20.3
		if ((!leftType.isBaseType() || leftType == TypeBinding.NULL) // cannot compare: Object == (int)0
				&& (!rightType.isBaseType() || rightType == TypeBinding.NULL)
				&& (this.checkCastTypesCompatibility(scope, leftType, rightType, null)
						|| this.checkCastTypesCompatibility(scope, rightType, leftType, null))) {

			// (special case for String)
			if ((rightType.id == T_JavaLangString) && (leftType.id == T_JavaLangString)) {
				computeConstant(leftType, rightType);
			} else {
				constant = Constant.NotAConstant;
			}
			return this.resolvedType = TypeBinding.BOOLEAN;
		}
		constant = Constant.NotAConstant;
		return null;
	}
	public void traverse(ASTVisitor visitor, BlockScope scope) {
		if (visitor.visit(this, scope)) {
			left.traverse(visitor, scope);
			right.traverse(visitor, scope);
		}
		visitor.endVisit(this, scope);
	}
	public int getASTType() {
		return IASTNode.EQUAL_EXPRESSION;
	
	}
}
