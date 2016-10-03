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
import org.eclipse.wst.jsdt.core.ast.IJsDocSingleTypeReference;
import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.impl.Constant;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Binding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ClassScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.PackageBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ProblemReasons;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ProblemReferenceBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Scope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;


public class JavadocSingleTypeReference extends SingleTypeReference implements IJsDocSingleTypeReference {

	public int tagSourceStart, tagSourceEnd;
	public PackageBinding packageBinding;

	public JavadocSingleTypeReference(char[] source, long pos, int tagStart, int tagEnd) {
		super(source, pos);
		this.tagSourceStart = tagStart;
		this.tagSourceEnd = tagEnd;
		this.bits |= ASTNode.InsideJavadoc;
	}

	protected void reportInvalidType(Scope scope) {
		scope.problemReporter().javadocInvalidType(this, this.resolvedType, scope.getDeclarationModifiers());
	}
	protected void reportDeprecatedType(TypeBinding type, Scope scope) {
		scope.problemReporter().javadocDeprecatedType(type, this, scope.getDeclarationModifiers());
	}

	/* (non-Javadoc)
	 * Redefine to capture javadoc specific signatures
	 * @see org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode#traverse(org.eclipse.wst.jsdt.internal.compiler.ASTVisitor, org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
	 */
	public void traverse(ASTVisitor visitor, BlockScope scope) {
		visitor.visit(this, scope);
		visitor.endVisit(this, scope);
	}

	public void traverse(ASTVisitor visitor, ClassScope scope) {
		visitor.visit(this, scope);
		visitor.endVisit(this, scope);
	}

	/*
	 * We need to modify resolving behavior to handle package references
	 */
	TypeBinding internalResolveType(Scope scope) {
		// handle the error here
		this.constant = Constant.NotAConstant;
		if (this.resolvedType != null)// is a shared type reference which was already resolved
			return this.resolvedType.isValidBinding() ? this.resolvedType : null; // already reported error

		this.resolvedType = getTypeBinding(scope);
		if (!this.resolvedType.isValidBinding()) {
			char[][] tokens = { this.token };
			Binding binding = scope.getTypeOrPackage(tokens);
			if (binding instanceof PackageBinding) {
				this.packageBinding = (PackageBinding) binding;
			} else {
				if (this.resolvedType.problemId() == ProblemReasons.NonStaticReferenceInStaticContext) {
					ReferenceBinding closestMatch = ((ProblemReferenceBinding)this.resolvedType).closestMatch();
				}
				reportInvalidType(scope);
			}
			return null;
		}
		if (isTypeUseDeprecated(this.resolvedType, scope))
			reportDeprecatedType(this.resolvedType, scope);
		return this.resolvedType;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.compiler.ast.Expression#resolveType(org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
	 * We need to override to handle package references
	 */
	public TypeBinding resolveType(BlockScope blockScope, boolean checkBounds) {
		return internalResolveType(blockScope);
	}

	public TypeBinding resolveType(ClassScope classScope) {
		return internalResolveType(classScope);
	}
	public int getASTType() {
		return IASTNode.JSDOC_SINGLE_TYPE_REFERENCE;
	
	}
}
