/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowContext;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowInfo;
import org.eclipse.wst.jsdt.internal.compiler.impl.Constant;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ArrayBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BaseTypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Binding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ClassScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Scope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeIds;

public abstract class Expression extends Statement implements IExpression {

	public Constant constant;

	public int statementEnd = -1;

	//Some expression may not be used - from a java semantic point
	//of view only - as statements. Other may. In order to avoid the creation
	//of wrappers around expression in order to tune them as expression
	//Expression is a subclass of Statement. See the message isValidJavaStatement()

	public TypeBinding resolvedType = TypeBinding.UNKNOWN;

public static final boolean isConstantValueRepresentable(Constant constant, int constantTypeID, int targetTypeID) {
	//true if there is no loss of precision while casting.
	// constantTypeID == constant.typeID
	if (targetTypeID == constantTypeID || constantTypeID==T_any)
		return true;
	switch (targetTypeID) {
		case T_char :
			switch (constantTypeID) {
				case T_char :
					return true;
				case T_double :
					return constant.doubleValue() == constant.charValue();
				case T_float :
					return constant.floatValue() == constant.charValue();
				case T_int :
					return constant.intValue() == constant.charValue();
				case T_short :
					return constant.shortValue() == constant.charValue();
				case T_long :
					return constant.longValue() == constant.charValue();
				default :
					return false;//boolean
			}

		case T_float :
			switch (constantTypeID) {
				case T_char :
					return constant.charValue() == constant.floatValue();
				case T_double :
					return constant.doubleValue() == constant.floatValue();
				case T_float :
					return true;
				case T_int :
					return constant.intValue() == constant.floatValue();
				case T_short :
					return constant.shortValue() == constant.floatValue();
				case T_long :
					return constant.longValue() == constant.floatValue();
				default :
					return false;//boolean
			}

		case T_double :
			switch (constantTypeID) {
				case T_char :
					return constant.charValue() == constant.doubleValue();
				case T_double :
					return true;
				case T_float :
					return constant.floatValue() == constant.doubleValue();
				case T_int :
					return constant.intValue() == constant.doubleValue();
				case T_short :
					return constant.shortValue() == constant.doubleValue();
				case T_long :
					return constant.longValue() == constant.doubleValue();
				default :
					return false; //boolean
			}

		case T_short :
			switch (constantTypeID) {
				case T_char :
					return constant.charValue() == constant.shortValue();
				case T_double :
					return constant.doubleValue() == constant.shortValue();
				case T_float :
					return constant.floatValue() == constant.shortValue();
				case T_int :
					return constant.intValue() == constant.shortValue();
				case T_short :
					return true;
				case T_long :
					return constant.longValue() == constant.shortValue();
				default :
					return false; //boolean
			}

		case T_int :
			switch (constantTypeID) {
				case T_char :
					return constant.charValue() == constant.intValue();
				case T_double :
					return constant.doubleValue() == constant.intValue();
				case T_float :
					return constant.floatValue() == constant.intValue();
				case T_int :
					return true;
				case T_short :
					return constant.shortValue() == constant.intValue();
				case T_long :
					return constant.longValue() == constant.intValue();
				default :
					return false; //boolean
			}

		case T_long :
			switch (constantTypeID) {
				case T_char :
					return constant.charValue() == constant.longValue();
				case T_double :
					return constant.doubleValue() == constant.longValue();
				case T_float :
					return constant.floatValue() == constant.longValue();
				case T_int :
					return constant.intValue() == constant.longValue();
				case T_short :
					return constant.shortValue() == constant.longValue();
				case T_long :
					return true;
				default :
					return false; //boolean
			}

		default :
			return false; //boolean
	}
}

public Expression() {
	super();
}

public FlowInfo analyseCode(BlockScope currentScope, FlowContext flowContext, FlowInfo flowInfo) {
	return flowInfo;
}

/**
 * More sophisticated for of the flow analysis used for analyzing expressions, and be able to optimize out
 * portions of expressions where no actual value is required.
 *
 * @param currentScope
 * @param flowContext
 * @param flowInfo
 * @param valueRequired
 * @return The state of initialization after the analysis of the current expression
 */
public FlowInfo analyseCode(BlockScope currentScope, FlowContext flowContext, FlowInfo flowInfo, boolean valueRequired) {

	return analyseCode(currentScope, flowContext, flowInfo);
}

/**
 * Returns false if cast is not legal.
 */
public final boolean checkCastTypesCompatibility(Scope scope, TypeBinding castType, TypeBinding expressionType, Expression expression) {

	// see specifications 5.5
	// handle errors and process constant when needed

	// if either one of the type is null ==>
	// some error has been already reported some where ==>
	// we then do not report an obvious-cascade-error.

	if (castType == null || expressionType == null) return true;
	if (castType==expressionType || castType.id==expressionType.id)
		return true;

	// identity conversion cannot be performed upfront, due to side-effects
	// like constant propagation
	boolean use15specifics = scope.compilerOptions().sourceLevel >= ClassFileConstants.JDK1_5;
	if (castType.isBaseType()) {
		if (expressionType.isBaseType()) {
			if (expressionType == castType) {
				if (expression != null) {
					this.constant = expression.constant; //use the same constant
				}
				tagAsUnnecessaryCast(scope, castType);
				return true;
			}
			boolean necessary = false;
			if (expressionType.isCompatibleWith(castType)
					|| (necessary = BaseTypeBinding.isNarrowing(castType.id, expressionType.id))) {
				if (!necessary) tagAsUnnecessaryCast(scope, castType);
				return true;

			}
		} else if (use15specifics
							&& scope.environment().computeBoxingType(expressionType).isCompatibleWith(castType)) { // unboxing - only widening match is allowed
			tagAsUnnecessaryCast(scope, castType);
			return true;
		}
		return false;
	} else if (use15specifics
						&& expressionType.isBaseType()
						&& scope.environment().computeBoxingType(expressionType).isCompatibleWith(castType)) { // boxing - only widening match is allowed
		tagAsUnnecessaryCast(scope, castType);
		return true;
	}

	switch(expressionType.kind()) {
		case Binding.BASE_TYPE :
			//-----------cast to something which is NOT a base type--------------------------
			if (expressionType == TypeBinding.NULL) {
				tagAsUnnecessaryCast(scope, castType);
				return true; //null is compatible with every thing
			}
			return false;

		case Binding.ARRAY_TYPE :
			if (castType == expressionType) {
				tagAsUnnecessaryCast(scope, castType);
				return true; // identity conversion
			}
			switch (castType.kind()) {
				case Binding.ARRAY_TYPE :
					// ( ARRAY ) ARRAY
					TypeBinding castElementType = ((ArrayBinding) castType).elementsType();
					TypeBinding exprElementType = ((ArrayBinding) expressionType).elementsType();
					if (exprElementType.isBaseType() || castElementType.isBaseType()) {
						if (castElementType == exprElementType) {
							tagAsNeedCheckCast();
							return true;
						}
						return false;
					}
					// recurse on array type elements
					return checkCastTypesCompatibility(scope, castElementType, exprElementType, expression);

				default:
					// ( CLASS/INTERFACE ) ARRAY
					switch (castType.id) {
						case T_JavaLangObject :
							tagAsUnnecessaryCast(scope, castType);
							return true;
						default :
							return false;
					}
			}

		default:
			switch (castType.kind()) {
				case Binding.ARRAY_TYPE :
					// ( ARRAY ) CLASS
					if (expressionType.id == TypeIds.T_JavaLangObject) { // potential runtime error
						if (use15specifics) checkUnsafeCast(scope, castType, expressionType, expressionType, true);
						tagAsNeedCheckCast();
						return true;
					}
					return false;

				default :
					// ( CLASS ) CLASS
					TypeBinding match = expressionType.findSuperTypeWithSameErasure(castType);
					if (match != null) {
						if (expression != null && castType.id == TypeIds.T_JavaLangString) this.constant = expression.constant; // (String) cst is still a constant
						return checkUnsafeCast(scope, castType, expressionType, match, false);
					}
					match = castType.findSuperTypeWithSameErasure(expressionType);
					if (match != null) {
						tagAsNeedCheckCast();
						return checkUnsafeCast(scope, castType, expressionType, match, true);
					}
					return false;
			}
			
	}
}

/**
 * Check the local variable of this expression, if any, against potential NPEs
 * given a flow context and an upstream flow info. If so, report the risk to
 * the context. Marks the local as checked, which affects the flow info.
 * @param scope the scope of the analysis
 * @param flowContext the current flow context
 * @param flowInfo the upstream flow info; caveat: may get modified
 */
public void checkNPE(BlockScope scope, FlowContext flowContext,
		FlowInfo flowInfo) {
	LocalVariableBinding local = this.localVariableBinding();
	if (local != null /*&&
			(local.type.tagBits & TagBits.IsBaseType) == 0*/) {
		if ((this.bits & ASTNode.IsNonNull) == 0) {
			flowContext.recordUsingNullReference(scope, local, this,
					FlowContext.MAY_NULL, flowInfo);
		}
		flowInfo.markAsComparedEqualToNonNull(local);
			// from thereon it is set
		if (flowContext.initsOnFinally != null) {
			flowContext.initsOnFinally.markAsComparedEqualToNonNull(local);
		}
	}
}

public boolean checkUnsafeCast(Scope scope, TypeBinding castType, TypeBinding expressionType, TypeBinding match, boolean isNarrowing) {
		if (match == castType) {
			if (!isNarrowing) tagAsUnnecessaryCast(scope, castType);
			return true;
		}
		if (!isNarrowing) tagAsUnnecessaryCast(scope, castType);
		return true;
	}

	public boolean isCompactableOperation() {

		return false;
	}

	//Return true if the conversion is done AUTOMATICALLY by the vm
	//while the javaVM is an int based-machine, thus for example pushing
	//a byte onto the stack , will automatically create an int on the stack
	//(this request some work d be done by the VM on signed numbers)
	public boolean isConstantValueOfTypeAssignableToType(TypeBinding constantType, TypeBinding targetType) {

		if (this.constant == Constant.NotAConstant)
			return false;
		if (constantType == targetType)
			return true;
		if (constantType.id==targetType.id)
			return true;
		if (constantType.isBaseType() && targetType.isBaseType()) {
			//No free assignment conversion from anything but to integral ones.
			if ((constantType == TypeBinding.INT
				|| BaseTypeBinding.isWidening(TypeIds.T_int, constantType.id))
				&& (BaseTypeBinding.isNarrowing(targetType.id, TypeIds.T_int))) {
				//use current explicit conversion in order to get some new value to compare with current one
				return isConstantValueRepresentable(this.constant, constantType.id, targetType.id);
			}
		}
		return false;
	}

	public boolean isTypeReference() {
		return false;
	}

	/**
	 * Returns the local variable referenced by this node. Can be a direct reference (SingleNameReference)
	 * or thru a cast expression etc...
	 */
	public LocalVariableBinding localVariableBinding() {
		return null;
	}

/**
 * Mark this expression as being non null, per a specific tag in the
 * source code.
 */
// this is no more called for now, waiting for inter procedural null reference analysis
public void markAsNonNull() {
	this.bits |= ASTNode.IsNonNull;
}

	public int nullStatus(FlowInfo flowInfo) {

		if (/* (this.bits & IsNonNull) != 0 || */
				this.constant != null && this.constant != Constant.NotAConstant)
			return FlowInfo.NON_NULL; // constant expression cannot be null

		LocalVariableBinding local = localVariableBinding();
		if (local != null) {
			if (flowInfo.isDefinitelyNull(local))
				return FlowInfo.NULL;
			if (flowInfo.isDefinitelyNonNull(local))
				return FlowInfo.NON_NULL;
			return FlowInfo.UNKNOWN;
		}
		return FlowInfo.NON_NULL;
	}

	/**
	 * Constant usable for bytecode pattern optimizations, but cannot be inlined
	 * since it is not strictly equivalent to the definition of constant expressions.
	 * In particular, some side-effects may be required to occur (only the end value
	 * is known).
	 * @return Constant known to be of boolean type
	 */
	public Constant optimizedBooleanConstant() {
		if(this.constant != null)
			return this.constant;
		return Constant.NotAConstant;
	}
	
	public StringBuffer print(int indent, StringBuffer output) {
		printIndent(indent, output);
		return printExpression(indent, output);
	}

	public abstract StringBuffer printExpression(int indent, StringBuffer output);

	public StringBuffer printStatement(int indent, StringBuffer output) {
		return print(indent, output).append(";"); //$NON-NLS-1$
	}

	public void resolve(BlockScope scope) {
		// drops the returning expression's type whatever the type is.

		this.resolveType(scope);
		return;
	}

	/**
	 * Resolve the type of this expression in the context of a blockScope
	 *
	 * @param scope
	 * @return
	 * 	Return the actual type of this expression after resolution
	 */
	public TypeBinding resolveType(BlockScope scope) {
		// by default... subclasses should implement a better TB if required.
		return null;
	}


	public TypeBinding resolveType(BlockScope scope, boolean define, TypeBinding useType) {
		return resolveType(scope);
	}

	/**
	 * Resolve the type of this expression in the context of a classScope
	 *
	 * @param scope
	 * @return
	 * 	Return the actual type of this expression after resolution
	 */
	public TypeBinding resolveType(ClassScope scope) {
		// by default... subclasses should implement a better TB if required.
		return null;
	}


	public TypeBinding resolveTypeExpecting(
		BlockScope scope,
		TypeBinding [] expectedTypes) {

		this.setExpectedType(expectedTypes[0]); // needed in case of generic method invocation
		TypeBinding expressionType = this.resolveType(scope);
		if (expressionType == null) return null;

		for (int i = 0; i < expectedTypes.length; i++) {
			if (expressionType == expectedTypes[i]) return expressionType;

			if (expressionType.isCompatibleWith(expectedTypes[i])) {
//				if (scope.isBoxingCompatibleWith(expressionType, expectedType)) {
//					this.computeConversion(scope, expectedType, expressionType);
//				} else {
//				}
					return expressionType;
			}
		}
		scope.problemReporter().typeMismatchError(expressionType, expectedTypes[0], this);
		return null;
	}


	public TypeBinding resolveTypeExpecting(
		BlockScope scope,
		TypeBinding expectedType) {

		this.setExpectedType(expectedType); // needed in case of generic method invocation
		TypeBinding expressionType = this.resolveType(scope);
		if (expressionType == null) return null;
		if (expressionType == expectedType) return expressionType;

		if (!expressionType.isCompatibleWith(expectedType)) {
			if (scope.isBoxingCompatibleWith(expressionType, expectedType)) {
			} else {
				if (expectedType!=TypeBinding.BOOLEAN || expressionType==TypeBinding.VOID)
				{
					scope.problemReporter().typeMismatchError(expressionType, expectedType, this);
					return null;
				}
			}
		}
		return expressionType;
	}

	/**
	 * Returns an object which can be used to identify identical JSR sequence targets
	 * (see TryStatement subroutine codegen)
	 * or <code>null</null> if not reusable
	 */
	public Object reusableJSRTarget() {
		if (this.constant != Constant.NotAConstant)
			return this.constant;
		return null;
	}

	/**
	 * Record the type expectation before this expression is typechecked.
	 * e.g. String s = foo();, foo() will be tagged as being expected of type String
	 * Used to trigger proper inference of generic method invocations.
	 *
	 * @param expectedType
	 * 	The type denoting an expectation in the context of an assignment conversion
	 */
	public void setExpectedType(TypeBinding expectedType) {
	    // do nothing by default
	}

	public void tagAsNeedCheckCast() {
	    // do nothing by default
	}

	/**
	 * Record the fact a cast expression got detected as being unnecessary.
	 *
	 * @param scope
	 * @param castType
	 */
	public void tagAsUnnecessaryCast(Scope scope, TypeBinding castType) {
	    // do nothing by default
	}

	public Expression toTypeReference() {
		//by default undefined

		//this method is meanly used by the parser in order to transform
		//an expression that is used as a type reference in a cast ....
		//--appreciate the fact that castExpression and ExpressionWithParenthesis
		//--starts with the same pattern.....

		return this;
	}

	/**
	 * Traverse an expression in the context of a blockScope
	 * @param visitor
	 * @param scope
	 */
	public void traverse(ASTVisitor visitor, BlockScope scope) {
		// nothing to do
	}

	/**
	 * Traverse an expression in the context of a classScope
	 * @param visitor
	 * @param scope
	 */
	public void traverse(ASTVisitor visitor, ClassScope scope) {
		// nothing to do
	}

	public void traverse(ASTVisitor visitior, Scope scope)
	{
		if (scope instanceof BlockScope)
			traverse(visitior,(BlockScope)scope);
		else if (scope instanceof ClassScope)
			traverse(visitior,(ClassScope)scope);
		else if (scope instanceof CompilationUnitScope)
			traverse(visitior,(CompilationUnitScope)scope);
	}

	public boolean isPrototype()
	{
		return false;
	}

	// is completion or selection node
	public boolean isSpecialNode()
	{
		return false;
	}
	
	public Binding alternateBinding()
	{ return null;}
	
	public TypeBinding resolveForAllocation(BlockScope scope, ASTNode location)
	{
		switch (getASTType()) {
		case IASTNode.STRING_LITERAL:
		case IASTNode.CHAR_LITERAL:
		case IASTNode.ARRAY_REFERENCE:
		case IASTNode.FUNCTION_CALL:
			
			break;

		default:
			System.out.println("IMPLEMENT resolveForAllocation for "+this.getClass());
			break;
		}
		return this.resolveType(scope);
	}
	
	public int getASTType() {
		return IASTNode.EXPRESSION;
	
	}
}
