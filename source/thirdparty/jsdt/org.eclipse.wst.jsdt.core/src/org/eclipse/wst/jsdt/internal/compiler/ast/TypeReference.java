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
import org.eclipse.wst.jsdt.core.ast.ITypeReference;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowContext;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowInfo;
import org.eclipse.wst.jsdt.internal.compiler.impl.Constant;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ClassScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Scope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeIds;

public abstract class TypeReference extends Expression implements ITypeReference {

public FlowInfo analyseCode(BlockScope currentScope, FlowContext flowContext, FlowInfo flowInfo) {
	return flowInfo;
}

// allows us to trap completion & selection nodes
public void aboutToResolve(Scope scope) {
	// default implementation: do nothing
}
/*
 * Answer a base type reference (can be an array of base type).
 */
public static final TypeReference baseTypeReference(int baseType, int dim) {

	if (dim == 0) {
		switch (baseType) {
			case (TypeIds.T_void) :
				return new SingleTypeReference(TypeBinding.VOID.simpleName, 0);
			case (TypeIds.T_boolean) :
				return new SingleTypeReference(TypeBinding.BOOLEAN.simpleName, 0);
			case (TypeIds.T_char) :
				return new SingleTypeReference(TypeBinding.CHAR.simpleName, 0);
			case (TypeIds.T_float) :
				return new SingleTypeReference(TypeBinding.FLOAT.simpleName, 0);
			case (TypeIds.T_double) :
				return new SingleTypeReference(TypeBinding.DOUBLE.simpleName, 0);
			case (TypeIds.T_short) :
				return new SingleTypeReference(TypeBinding.SHORT.simpleName, 0);
			case (TypeIds.T_int) :
				return new SingleTypeReference(TypeBinding.INT.simpleName, 0);
			default : //T_long
				return new SingleTypeReference(TypeBinding.LONG.simpleName, 0);
		}
	}
	switch (baseType) {
		case (TypeIds.T_void) :
			return new ArrayTypeReference(TypeBinding.VOID.simpleName, dim, 0);
		case (TypeIds.T_boolean) :
			return new ArrayTypeReference(TypeBinding.BOOLEAN.simpleName, dim, 0);
		case (TypeIds.T_char) :
			return new ArrayTypeReference(TypeBinding.CHAR.simpleName, dim, 0);
		case (TypeIds.T_float) :
			return new ArrayTypeReference(TypeBinding.FLOAT.simpleName, dim, 0);
		case (TypeIds.T_double) :
			return new ArrayTypeReference(TypeBinding.DOUBLE.simpleName, dim, 0);
		case (TypeIds.T_short) :
			return new ArrayTypeReference(TypeBinding.SHORT.simpleName, dim, 0);
		case (TypeIds.T_int) :
			return new ArrayTypeReference(TypeBinding.INT.simpleName, dim, 0);
		default : //T_long
			return new ArrayTypeReference(TypeBinding.LONG.simpleName, dim, 0);
	}
}

public abstract TypeReference copyDims(int dim);
public int dimensions() {
	return 0;
}

public abstract char[] getLastToken();

protected abstract TypeBinding getTypeBinding(Scope scope);
/**
 * @return char[][]
 */
public abstract char [][] getTypeName() ;

public char[] getSimpleTypeName()
{
	char[][] typeName = getTypeName();
	return typeName[typeName.length-1];
}

public char[] getFullTypeName() {
	char[][] typeName = getTypeName();
	return CharOperation.concatWith(typeName, '.');
}

public boolean isTypeReference() {
	return true;
}
public TypeBinding resolveSuperType(ClassScope scope) {
	// assumes the implementation of resolveType(ClassScope) will call back to detect cycles
	if (resolveType(scope) == null) return null;

	return this.resolvedType;
}

public final TypeBinding resolveType(BlockScope blockScope) {
	return resolveType(blockScope, true /* checkbounds if any */);
}

public TypeBinding resolveType(BlockScope scope, boolean checkBounds) {
	// handle the error here
	this.constant = Constant.NotAConstant;
	if (this.resolvedType != null) // is a shared type reference which was already resolved
		return this.resolvedType.isValidBinding() ? this.resolvedType : null; // already reported error

	TypeBinding type = this.resolvedType = getTypeBinding(scope);
	if (type == null)
		return null; // detected cycle while resolving hierarchy
	if (!type.isValidBinding()) {
		reportInvalidType(scope);
		return null;
	}

	if (isTypeUseDeprecated(type, scope))
		reportDeprecatedType(type, scope);

	return this.resolvedType = type;
}
public TypeBinding resolveType(ClassScope scope) {
	// handle the error here
	this.constant = Constant.NotAConstant;
	if (this.resolvedType != null) // is a shared type reference which was already resolved
		return this.resolvedType.isValidBinding() ? this.resolvedType : null; // already reported error

	TypeBinding type = this.resolvedType = getTypeBinding(scope);
	if (type == null)
		return null; // detected cycle while resolving hierarchy
	if (!type.isValidBinding()) {
		reportInvalidType(scope);
		return null;
	}
	if (isTypeUseDeprecated(type, scope))
		reportDeprecatedType(type, scope);

	return this.resolvedType = type;
}

public TypeBinding resolveTypeArgument(BlockScope blockScope, ReferenceBinding genericType, int rank) {
    return resolveType(blockScope, true /* check bounds*/);
}

public TypeBinding resolveTypeArgument(ClassScope classScope, ReferenceBinding genericType, int rank) {
    return resolveType(classScope);
}

protected void reportInvalidType(Scope scope) {
	scope.problemReporter().invalidType(this, this.resolvedType);
}
protected void reportDeprecatedType(TypeBinding type, Scope scope) {
	scope.problemReporter().deprecatedType(type, this);
}
public abstract void traverse(ASTVisitor visitor, BlockScope scope);
public abstract void traverse(ASTVisitor visitor, ClassScope scope);

public int getASTType() {
	return IASTNode.TYPE_REFERENCE;

}
}
