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
import org.eclipse.wst.jsdt.core.ast.ISuperReference;
import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.impl.Constant;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;

public class SuperReference extends ThisReference implements ISuperReference {

	public SuperReference(int sourceStart, int sourceEnd) {

		super(sourceStart, sourceEnd);
	}

	public static ExplicitConstructorCall implicitSuperConstructorCall() {

		return new ExplicitConstructorCall(ExplicitConstructorCall.ImplicitSuper);
	}

	public boolean isImplicitThis() {

		return false;
	}

	public boolean isSuper() {

		return true;
	}

	public boolean isThis() {

		return false ;
	}

	public StringBuffer printExpression(int indent, StringBuffer output){

		return output.append("super"); //$NON-NLS-1$

	}

	public TypeBinding resolveType(BlockScope scope) {

		constant = Constant.NotAConstant;
		if (!checkAccess(scope.methodScope()))
			return null;
		ReferenceBinding enclosingReceiverType = scope.enclosingReceiverType();
		if (enclosingReceiverType.id == T_JavaLangObject) {
			return null;
		}
		return this.resolvedType = enclosingReceiverType.getSuperBinding();
	}

	public void traverse(ASTVisitor visitor, BlockScope blockScope) {
		visitor.visit(this, blockScope);
		visitor.endVisit(this, blockScope);
	}
	public int getASTType() {
		return IASTNode.SUPER_REFERENCE;
	
	}
}
