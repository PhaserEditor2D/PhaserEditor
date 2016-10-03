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
import org.eclipse.wst.jsdt.core.ast.IJsDocArgumentExpression;
import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.wst.jsdt.internal.compiler.impl.Constant;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ClassScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Scope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;

public class JavadocArgumentExpression extends Expression implements IJsDocArgumentExpression {
	public char[] token;
	public Argument argument;

	public JavadocArgumentExpression(char[] name, int startPos, int endPos, TypeReference typeRef) {
		this.token = name;
		this.sourceStart = startPos;
		this.sourceEnd = endPos;
		long pos = (((long) startPos) << 32) + endPos;
		this.argument = new Argument(name, pos, typeRef, ClassFileConstants.AccDefault);
		this.bits |= InsideJavadoc;
	}

	/*
	 * Resolves type on a Block or Class scope.
	 */
	private TypeBinding internalResolveType(Scope scope) {
		this.constant = Constant.NotAConstant;
		if (this.resolvedType != null) // is a shared type reference which was already resolved
			return this.resolvedType.isValidBinding() ? this.resolvedType : null; // already reported error

		if (this.argument != null) {
			TypeReference typeRef = this.argument.type;
			if (typeRef != null) {
				this.resolvedType = typeRef.getTypeBinding(scope);
				typeRef.resolvedType = this.resolvedType;
				if (!this.resolvedType.isValidBinding()) {
					scope.problemReporter().javadocInvalidType(typeRef, this.resolvedType, scope.getDeclarationModifiers());
					return null;
				}
				if (isTypeUseDeprecated(this.resolvedType, scope)) {
					scope.problemReporter().javadocDeprecatedType(this.resolvedType, typeRef, scope.getDeclarationModifiers());
				}
				return this.resolvedType;
			}
		}
		return null;
	}

	public StringBuffer printExpression(int indent, StringBuffer output) {
		if (this.argument == null) {
			if (this.token != null) {
				output.append(this.token);
			}
		}
		else {
			this.argument.print(indent, output);
		}
		return output;
	}

	public void resolve(BlockScope scope) {
		if (this.argument != null) {
			this.argument.resolve(scope);
		}
	}

	public TypeBinding resolveType(BlockScope scope) {
		return internalResolveType(scope);
	}

	public TypeBinding resolveType(ClassScope scope) {
		return internalResolveType(scope);
	}

	/* (non-Javadoc)
	 * Redefine to capture javadoc specific signatures
	 * @see org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode#traverse(org.eclipse.wst.jsdt.internal.compiler.ASTVisitor, org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
	 */
	public void traverse(ASTVisitor visitor, BlockScope blockScope) {
		if (visitor.visit(this, blockScope)) {
			if (this.argument != null) {
				this.argument.traverse(visitor, blockScope);
			}
		}
		visitor.endVisit(this, blockScope);
	}
	public void traverse(ASTVisitor visitor, ClassScope blockScope) {
		if (visitor.visit(this, blockScope)) {
			if (this.argument != null) {
				this.argument.traverse(visitor, blockScope);
			}
		}
		visitor.endVisit(this, blockScope);
	}
	public int getASTType() {
		return IASTNode.JSDOC_ARGUMENTEXPRESSION;
	
	}
}
