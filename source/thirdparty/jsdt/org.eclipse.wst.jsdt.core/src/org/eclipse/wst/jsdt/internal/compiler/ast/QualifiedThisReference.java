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
import org.eclipse.wst.jsdt.core.ast.IQualifiedThisReference;
import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowContext;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowInfo;
import org.eclipse.wst.jsdt.internal.compiler.impl.Constant;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ClassScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;

public class QualifiedThisReference extends ThisReference implements IQualifiedThisReference {

	public TypeReference qualification;
	ReferenceBinding currentCompatibleType;

	public QualifiedThisReference(TypeReference name, int sourceStart, int sourceEnd) {
		super(sourceStart, sourceEnd);
		qualification = name;
		name.bits |= IgnoreRawTypeCheck; // no need to worry about raw type usage
		this.sourceStart = name.sourceStart;
	}

	public FlowInfo analyseCode(
		BlockScope currentScope,
		FlowContext flowContext,
		FlowInfo flowInfo) {

		return flowInfo;
	}

	public FlowInfo analyseCode(
		BlockScope currentScope,
		FlowContext flowContext,
		FlowInfo flowInfo,
		boolean valueRequired) {

		return flowInfo;
	}

	public TypeBinding resolveType(BlockScope scope) {

		constant = Constant.NotAConstant;
		// X.this is not a param/raw type as denoting enclosing instance
		TypeBinding type = this.qualification.resolveType(scope, true /* check bounds*/);
		if (type == null) return null;

		// resolvedType needs to be converted to parameterized
		if (type instanceof ReferenceBinding) {
			this.resolvedType = type;
		} else {
			// error case
			this.resolvedType = type;
		}

		// the qualification MUST exactly match some enclosing type name
		// It is possible to qualify 'this' by the name of the current class
		int depth = 0;
		this.currentCompatibleType = scope.referenceType().binding;
		while (this.currentCompatibleType != null && this.currentCompatibleType != type) {
			depth++;
			this.currentCompatibleType = this.currentCompatibleType.isStatic() ? null : this.currentCompatibleType.enclosingType();
		}
		bits &= ~DepthMASK; // flush previous depth if any
		bits |= (depth & 0xFF) << DepthSHIFT; // encoded depth into 8 bits

		if (this.currentCompatibleType == null) {
			return this.resolvedType;
		}

		// Ensure one cannot write code like: B() { super(B.this); }
		if (depth == 0) {
			checkAccess(scope.methodScope());
		} // if depth>0, path emulation will diagnose bad scenarii

		return  this.resolvedType;
	}

	public StringBuffer printExpression(int indent, StringBuffer output) {

		return qualification.print(0, output).append(".this"); //$NON-NLS-1$
	}

	public void traverse(
		ASTVisitor visitor,
		BlockScope blockScope) {

		if (visitor.visit(this, blockScope)) {
			qualification.traverse(visitor, blockScope);
		}
		visitor.endVisit(this, blockScope);
	}

	public void traverse(
			ASTVisitor visitor,
			ClassScope blockScope) {

		if (visitor.visit(this, blockScope)) {
			qualification.traverse(visitor, blockScope);
		}
		visitor.endVisit(this, blockScope);
	}
	public int getASTType() {
		return IASTNode.QUALIFIED_THIS_REFERENCE;
	
	}
}
