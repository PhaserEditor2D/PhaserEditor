/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
import org.eclipse.wst.jsdt.core.ast.IArrayReference;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowContext;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowInfo;
import org.eclipse.wst.jsdt.internal.compiler.impl.Constant;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ArrayBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.SourceTypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;

public class ArrayReference extends Reference implements IArrayReference {

	public Expression receiver;
	public Expression position;

	public ArrayReference(Expression rec, Expression pos) {
		this.receiver = rec;
		this.position = pos;
		sourceStart = rec.sourceStart;
	}

public FlowInfo analyseAssignment(
		BlockScope currentScope,
		FlowContext flowContext,
		FlowInfo flowInfo,
		Assignment assignment,
		boolean compoundAssignment) {
	// TODO (maxime) optimization: unconditionalInits is applied to all existing calls
	if (assignment.expression == null) {
		return analyseCode(currentScope, flowContext, flowInfo);
	}
	return assignment
		.expression
		.analyseCode(
			currentScope,
			flowContext,
			analyseCode(currentScope, flowContext, flowInfo).unconditionalInits());
}

public FlowInfo analyseCode(
		BlockScope currentScope,
		FlowContext flowContext,
		FlowInfo flowInfo) {
	receiver.checkNPE(currentScope, flowContext, flowInfo);
	flowInfo = receiver.analyseCode(currentScope, flowContext, flowInfo);
	return position.analyseCode(currentScope, flowContext, flowInfo);
}

	public int nullStatus(FlowInfo flowInfo) {
	return FlowInfo.UNKNOWN;
}

	public StringBuffer printExpression(int indent, StringBuffer output) {

		receiver.printExpression(0, output).append('[');
		return position.printExpression(0, output).append(']');
	}

	public TypeBinding resolveType(BlockScope scope) {

		constant = Constant.NotAConstant;
//		if (receiver instanceof CastExpression	// no cast check for ((type[])null)[0]
//				&& ((CastExpression)receiver).innermostCastedExpression() instanceof NullLiteral) {
//			this.receiver.bits |= DisableUnnecessaryCastCheck; // will check later on
//		}
		TypeBinding arrayType = receiver.resolveType(scope);
		if (arrayType != null) {
			if (arrayType.isArrayType()) {
				TypeBinding elementType = ((ArrayBinding) arrayType).elementsType();
				this.resolvedType = elementType;
			} else if (arrayType instanceof SourceTypeBinding) {
				this.resolvedType = TypeBinding.UNKNOWN;				
				if (position instanceof StringLiteral) {
					FieldBinding[] fields = ((SourceTypeBinding) arrayType).fields();
					char[] positionSource = ((StringLiteral) position).source;
					for (int idx = 0; idx < fields.length; idx++) {
						if (CharOperation.equals(positionSource, fields[idx].name)) {
							this.resolvedType = fields[idx].type;
							break;
						}
					}
				}
			} else {
//				scope.problemReporter().referenceMustBeArrayTypeAt(arrayType, this);
				this.resolvedType=TypeBinding.UNKNOWN;
			}
		}
		else 
			this.resolvedType=TypeBinding.UNKNOWN;
		  position.resolveTypeExpecting(scope, new TypeBinding[] {scope.getJavaLangNumber(),scope.getJavaLangString(),TypeBinding.ANY});
//		if (positionType != null) {
//			position.computeConversion(scope, TypeBinding.INT, positionType);
//		}
		return this.resolvedType;
	}

	public void traverse(ASTVisitor visitor, BlockScope scope) {

		if (visitor.visit(this, scope)) {
			receiver.traverse(visitor, scope);
			position.traverse(visitor, scope);
		}
		visitor.endVisit(this, scope);
	}
	public int getASTType() {
		return IASTNode.ARRAY_REFERENCE;
	
	}
}
