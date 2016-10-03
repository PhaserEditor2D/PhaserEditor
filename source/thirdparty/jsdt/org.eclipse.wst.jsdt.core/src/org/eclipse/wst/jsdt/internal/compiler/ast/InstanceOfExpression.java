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
import org.eclipse.wst.jsdt.core.ast.IInstanceOfExpression;
import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowContext;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowInfo;
import org.eclipse.wst.jsdt.internal.compiler.impl.Constant;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TagBits;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;

public class InstanceOfExpression extends OperatorExpression implements IInstanceOfExpression {

	public Expression expression;
	public TypeReference type;

	public InstanceOfExpression(Expression expression, TypeReference type) {

		this.expression = expression;
		this.type = type;
		type.bits |= IgnoreRawTypeCheck; // no need to worry about raw type usage
		this.bits |= INSTANCEOF << OperatorSHIFT;
		this.sourceStart = expression.sourceStart;
		this.sourceEnd = type.sourceEnd;
	}

public FlowInfo analyseCode(
		BlockScope currentScope,
		FlowContext flowContext,
		FlowInfo flowInfo) {
	LocalVariableBinding local = this.expression.localVariableBinding();
	if (local != null && (local.type.tagBits & TagBits.IsBaseType) == 0) {
		flowContext.recordUsingNullReference(currentScope, local,
			this.expression, FlowContext.CAN_ONLY_NULL, flowInfo);
		flowInfo = expression.analyseCode(currentScope, flowContext, flowInfo).
			unconditionalInits();
		FlowInfo initsWhenTrue = flowInfo.copy();
		initsWhenTrue.markAsComparedEqualToNonNull(local);
		// no impact upon enclosing try context
		return FlowInfo.conditional(initsWhenTrue, flowInfo.copy());
	}
	return expression.analyseCode(currentScope, flowContext, flowInfo).
			unconditionalInits();
}

	public StringBuffer printExpressionNoParenthesis(int indent, StringBuffer output) {

		expression.printExpression(indent, output).append(" instanceof "); //$NON-NLS-1$
		return type.print(0, output);
	}

	public TypeBinding resolveType(BlockScope scope) {

		constant = Constant.NotAConstant;
		TypeBinding expressionType = expression.resolveType(scope);
		TypeBinding checkedType = type.resolveType(scope, true /* check bounds*/);
		if (expressionType == null || checkedType == null)
			return null;

		if ((expressionType != TypeBinding.NULL && expressionType.isBaseType()) // disallow autoboxing
				|| !checkCastTypesCompatibility(scope, checkedType, expressionType, null)) {
			scope.problemReporter().notCompatibleTypesError(this, expressionType, checkedType);
		}
		return this.resolvedType = TypeBinding.BOOLEAN;
	}
	public void traverse(ASTVisitor visitor, BlockScope scope) {

		if (visitor.visit(this, scope)) {
			expression.traverse(visitor, scope);
			type.traverse(visitor, scope);
		}
		visitor.endVisit(this, scope);
	}
	public int getASTType() {
		return IASTNode.INSTANCEOF_EXPRESSION;
	
	}
}
