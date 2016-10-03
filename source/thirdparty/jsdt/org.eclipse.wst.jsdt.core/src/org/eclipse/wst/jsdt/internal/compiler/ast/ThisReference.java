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
import org.eclipse.wst.jsdt.core.ast.IThisReference;
import org.eclipse.wst.jsdt.core.infer.InferredType;
import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowContext;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowInfo;
import org.eclipse.wst.jsdt.internal.compiler.impl.Constant;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Binding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ClassScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.MethodScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;

public class ThisReference extends Reference implements IThisReference {

	/**
	 * <p>
	 * {@link InferredType} referred to by "this"
	 * </p>
	 */
	private InferredType fInferredType;
	
	public static ThisReference implicitThis(){

		ThisReference implicitThis = new ThisReference(0, 0);
		implicitThis.bits |= IsImplicitThis;
		return implicitThis;
	}

	public ThisReference(int sourceStart, int sourceEnd) {

		this.sourceStart = sourceStart;
		this.sourceEnd = sourceEnd;
	}

	/*
	 * @see Reference#analyseAssignment(...)
	 */
	public FlowInfo analyseAssignment(BlockScope currentScope, FlowContext flowContext, FlowInfo flowInfo, Assignment assignment, boolean isCompound) {

		return flowInfo; // this cannot be assigned
	}

	public boolean checkAccess(MethodScope methodScope) {

		// this/super cannot be used in constructor call
//		if (methodScope!=null && methodScope.isConstructorCall) {
//			methodScope.problemReporter().fieldsOrThisBeforeConstructorInvocation(this);
//			return false;
//		}

		// static may not refer to this/super
//		if (methodScope!=null && methodScope.isStatic) {
//			methodScope.problemReporter().errorThisSuperInStatic(this);
//			return false;
//		}
		return true;
	}

	public boolean isImplicitThis() {

		return (this.bits & IsImplicitThis) != 0;
	}

	public boolean isThis() {

		return true ;
	}

	public int nullStatus(FlowInfo flowInfo) {
		return FlowInfo.NON_NULL;
	}

	public StringBuffer printExpression(int indent, StringBuffer output){

		if (this.isImplicitThis()) return output;
		return output.append("this"); //$NON-NLS-1$
	}

	public TypeBinding resolveType(BlockScope scope) {

		constant = Constant.NotAConstant;
		if (!this.isImplicitThis() &&!checkAccess(scope.methodScope())) {
			return null;
		}
		MethodScope methodScope = scope.methodScope();
		if (methodScope!=null && methodScope.isStatic)
			bits |= Binding.TYPE;
		return this.resolvedType = scope.enclosingReceiverType();
	}

	public void traverse(ASTVisitor visitor, BlockScope blockScope) {

		visitor.visit(this, blockScope);
		visitor.endVisit(this, blockScope);
	}
	public void traverse(ASTVisitor visitor, ClassScope blockScope) {

		visitor.visit(this, blockScope);
		visitor.endVisit(this, blockScope);
	}
	public int getASTType() {
		return IASTNode.THIS_REFERENCE;
	
	}
	
	/**
	 * @param type {@link InferredType} referred to by "this"
	 */
	public void setInferredType(InferredType type) {
		this.fInferredType = type;
	}
	
	/**
	 * @return {@link InferredType} referred to by "this", or
	 * <code>null</code> if none is set
	 */
	public InferredType getInferredType() {
		return this.fInferredType;
	}
}
