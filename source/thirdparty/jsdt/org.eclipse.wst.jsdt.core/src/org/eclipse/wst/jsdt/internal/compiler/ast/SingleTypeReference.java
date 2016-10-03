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
import org.eclipse.wst.jsdt.core.ast.ISingleTypeReference;
import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ClassScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Scope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;

public class SingleTypeReference extends TypeReference implements ISingleTypeReference {

	public char[] token;

	public SingleTypeReference(char[] source, long pos) {

			token = source;
			sourceStart = (int) (pos>>>32)  ;
			sourceEnd = (int) (pos & 0x00000000FFFFFFFFL) ;

	}

	public TypeReference copyDims(int dim){
		//return a type reference copy of me with some dimensions
		//warning : the new type ref has a null binding

		return new ArrayTypeReference(token, dim,(((long)sourceStart)<<32)+sourceEnd);
	}

	public char[] getLastToken() {
		return this.token;
	}
	protected TypeBinding getTypeBinding(Scope scope) {
		if (this.resolvedType != null)
			return this.resolvedType;

		this.resolvedType = scope.getType(token);

		if (scope.kind == Scope.CLASS_SCOPE && this.resolvedType.isValidBinding())
			if (((ClassScope) scope).detectHierarchyCycle(this.resolvedType, this))
				return null;
		return this.resolvedType;
	}

	public char [][] getTypeName() {
		return new char[][] { token };
	}

	public StringBuffer printExpression(int indent, StringBuffer output){

		return output.append(token);
	}

	public TypeBinding resolveTypeEnclosing(BlockScope scope, ReferenceBinding enclosingType) {

		TypeBinding memberType = scope.getMemberType(token, enclosingType);
		if (!memberType.isValidBinding()) {
			this.resolvedType = memberType;
			return null;
		}
		if (isTypeUseDeprecated(memberType, scope))
			scope.problemReporter().deprecatedType(memberType, this);
		return this.resolvedType = memberType;
	}

	public void traverse(ASTVisitor visitor, BlockScope scope) {
		visitor.visit(this, scope);
		visitor.endVisit(this, scope);
	}

	public void traverse(ASTVisitor visitor, ClassScope scope) {
		visitor.visit(this, scope);
		visitor.endVisit(this, scope);
	}
	public int getASTType() {
		return IASTNode.SINGLE_TYPE_REFERENCE;
	
	}
}
