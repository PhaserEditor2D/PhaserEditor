/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
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
import org.eclipse.wst.jsdt.core.ast.IReturnStatement;
import org.eclipse.wst.jsdt.core.infer.InferredType;
import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowContext;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowInfo;
import org.eclipse.wst.jsdt.internal.compiler.flow.InitializationFlowContext;
import org.eclipse.wst.jsdt.internal.compiler.flow.InsideSubRoutineFlowContext;
import org.eclipse.wst.jsdt.internal.compiler.impl.Constant;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.MethodScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;

public class ReturnStatement extends Statement implements IReturnStatement {

	public Expression expression;
	public SubRoutineStatement[] subroutines;
	public LocalVariableBinding saveValueVariable;
	public int initStateIndex = -1;
	
	/**
	 * <p>
	 * {@link InferredType} returned by this return statement.
	 * </p>
	 */
	private InferredType fInferredType;
	
	/**
	 * <p>
	 * <code>true</code> if this return statement is actually returning a
	 * type, rather then the instance of a type. <code>false</code> if this
	 * return statement is returning an instance of a type rather then the
	 * type itself.
	 * </p>
	 */
	private boolean fIsType;

	public ReturnStatement(Expression expression, int sourceStart, int sourceEnd) {
		this.sourceStart = sourceStart;
		this.sourceEnd = sourceEnd;
		this.expression = expression ;
		this.fInferredType = null;
		this.fIsType = false;
	}
	
	public FlowInfo analyseCode(BlockScope currentScope, FlowContext flowContext, FlowInfo flowInfo) {
		if (this.expression != null) {
			flowInfo = this.expression.analyseCode(currentScope, flowContext, flowInfo);
		}
	
		// compute the return sequence (running the finally blocks)
		FlowContext traversedContext = flowContext;
		int subCount = 0;
		boolean saveValueNeeded = false;
		boolean hasValueToSave = this.expression != null
							&& this.expression.constant == Constant.NotAConstant
							&& !(this.expression instanceof NullLiteral);
		do {
			SubRoutineStatement sub;
			if ((sub = traversedContext.subroutine()) != null) {
				if (this.subroutines == null){
					this.subroutines = new SubRoutineStatement[5];
				}
				if (subCount == this.subroutines.length) {
					System.arraycopy(this.subroutines, 0, (this.subroutines = new SubRoutineStatement[subCount*2]), 0, subCount); // grow
				}
				this.subroutines[subCount++] = sub;
				if (sub.isSubRoutineEscaping()) {
					saveValueNeeded = false;
					this.bits |= ASTNode.IsAnySubRoutineEscaping;
					break;
				}
			}
			traversedContext.recordReturnFrom(flowInfo.unconditionalInits());
	
			if (traversedContext instanceof InsideSubRoutineFlowContext) {
				ASTNode node = traversedContext.associatedNode;
				if (node instanceof TryStatement) {
					TryStatement tryStatement = (TryStatement) node;
					flowInfo.addInitializationsFrom(tryStatement.subRoutineInits); // collect inits
					if (hasValueToSave) {
						if (this.saveValueVariable == null){ // closest subroutine secret variable is used
							prepareSaveValueLocation(tryStatement);
						}
						saveValueNeeded = true;
					}
				}
			} else if (traversedContext instanceof InitializationFlowContext) {
					currentScope.problemReporter().cannotReturnOutsideFunction(this);
					return FlowInfo.DEAD_END;
			}
		} while ((traversedContext = traversedContext.parent) != null);
	
		// resize subroutines
		if ((this.subroutines != null) && (subCount != this.subroutines.length)) {
			System.arraycopy(this.subroutines, 0, (this.subroutines = new SubRoutineStatement[subCount]), 0, subCount);
		}
	
		// secret local variable for return value (note that this can only occur in a real method)
		if (saveValueNeeded) {
			if (this.saveValueVariable != null) {
				this.saveValueVariable.useFlag = LocalVariableBinding.USED;
			}
		} else {
			this.saveValueVariable = null;
			if ( this.expression != null && this.expression.resolvedType == TypeBinding.BOOLEAN) {
				this.expression.bits |= ASTNode.IsReturnedValue;
			}
		}
		return FlowInfo.DEAD_END;
	}
	
	public boolean needValue() {
		return this.saveValueVariable != null
		|| ((this.bits & ASTNode.IsAnySubRoutineEscaping) == 0);
	}
	
	public void prepareSaveValueLocation(TryStatement targetTryStatement){
		this.saveValueVariable = targetTryStatement.secretReturnValue;
	}
	
	public StringBuffer printStatement(int tab, StringBuffer output){
		printIndent(tab, output).append("return "); //$NON-NLS-1$
		if (this.expression != null )
			this.expression.printExpression(0, output) ;
		return output.append(';');
	}
	
	public void resolve(BlockScope scope) {
		MethodScope methodScope = scope.methodScope();
	
		if(methodScope==null) {
			/* return statement outside of a method */
			scope.problemReporter().cannotReturnOutsideFunction(this);
			return;
		}
	
		MethodBinding methodBinding = null;
		TypeBinding methodType =
			(methodScope.referenceContext instanceof AbstractMethodDeclaration)
				? ((methodBinding = ((AbstractMethodDeclaration) methodScope.referenceContext).binding) == null
					? null
					: methodBinding.returnType)
				: TypeBinding.ANY;
		TypeBinding expressionType;
		if (this.expression == null) {
			if (methodType != null && !methodType.isAnyType()) scope.problemReporter().shouldReturn(methodType, this);
			return;
		}
		this.expression.setExpectedType(methodType); // needed in case of generic method invocation
		if ((expressionType = this.expression.resolveType(scope)) == null) return;
		if (methodType == null)
			return;
	
		if (methodType != expressionType) // must call before computeConversion() and typeMismatchError()
			scope.compilationUnitScope().recordTypeConversion(methodType, expressionType);
		if (this.expression.isConstantValueOfTypeAssignableToType(expressionType, methodType)
				|| expressionType.isCompatibleWith(methodType)) {
	
			return;
		}
		if(methodBinding != null && !methodBinding.isConstructor())
			scope.problemReporter().typeMismatchError(expressionType, methodType, this.expression);
	}
	
	public void traverse(ASTVisitor visitor, BlockScope scope) {
		if (visitor.visit(this, scope)) {
			if (this.expression != null)
				this.expression.traverse(visitor, scope);
		}
		visitor.endVisit(this, scope);
	}
	
	public int getASTType() {
		return IASTNode.RETURN_STATEMENT;
	
	}
	
	public IExpression getExpression() {
		return this.expression;
	}

	/**
	 * @see org.eclipse.wst.jsdt.core.ast.IReturnStatement#setInferredType(org.eclipse.wst.jsdt.core.infer.InferredType)
	 */
	public void setInferredType(InferredType type) {
		this.fInferredType = type;
	}

	/**
	 * @see org.eclipse.wst.jsdt.core.ast.IReturnStatement#getInferredType()
	 */
	public InferredType getInferredType() {
		return this.fInferredType;
	}

	/**
	 * @see org.eclipse.wst.jsdt.core.ast.IReturnStatement#setIsType(boolean)
	 */
	public void setIsType(boolean isType) {
		this.fIsType = isType;
	}

	/**
	 * @see org.eclipse.wst.jsdt.core.ast.IReturnStatement#isType()
	 */
	public boolean isType() {
		return this.fIsType;
	}
}