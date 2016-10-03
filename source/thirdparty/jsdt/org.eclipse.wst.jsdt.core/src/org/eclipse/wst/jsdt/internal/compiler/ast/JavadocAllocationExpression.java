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
import org.eclipse.wst.jsdt.core.ast.IJsDocAllocationExpression;
import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.impl.Constant;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Binding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ClassScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ProblemMethodBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ProblemReasons;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Scope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.SourceTypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;

public class JavadocAllocationExpression extends AllocationExpression implements IJsDocAllocationExpression {

	public int tagSourceStart, tagSourceEnd;
	public int tagValue, memberStart;
	public char[][] qualification;

	public JavadocAllocationExpression(int start, int end) {
		this.sourceStart = start;
		this.sourceEnd = end;
		this.bits |= InsideJavadoc;
	}
	public JavadocAllocationExpression(long pos) {
		this((int) (pos >>> 32), (int) pos);
	}

	TypeBinding internalResolveType(Scope scope) {

		// Propagate the type checking to the arguments, and check if the constructor is defined.
		this.constant = Constant.NotAConstant;
		if (this.type == null) {
			this.resolvedType = scope.enclosingSourceType();
		} else if (scope.kind == Scope.CLASS_SCOPE) {
			this.resolvedType = this.type.resolveType((ClassScope)scope);
		} else {
			this.resolvedType = this.type.resolveType((BlockScope)scope, true /* check bounds*/);
		}

		// buffering the arguments' types
		TypeBinding[] argumentTypes = Binding.NO_PARAMETERS;
		boolean hasTypeVarArgs = false;
		if (this.arguments != null) {
			boolean argHasError = false;
			int length = this.arguments.length;
			argumentTypes = new TypeBinding[length];
			for (int i = 0; i < length; i++) {
				Expression argument = this.arguments[i];
				if (scope.kind == Scope.CLASS_SCOPE) {
					argumentTypes[i] = argument.resolveType((ClassScope)scope);
				} else {
					argumentTypes[i] = argument.resolveType((BlockScope)scope);
				}
				if (argumentTypes[i] == null) {
					argHasError = true;
				} else if (!hasTypeVarArgs) {
					hasTypeVarArgs = false;
				}
			}
			if (argHasError) {
				return null;
			}
		}

		// check resolved type
		if (this.resolvedType == null) {
			return null;
		}
		this.resolvedType = this.type.resolvedType;
		SourceTypeBinding enclosingType = scope.enclosingSourceType();
		if (enclosingType == null ? false : enclosingType.isCompatibleWith(this.resolvedType)) {
			this.bits |= ASTNode.SuperAccess;
		}

		ReferenceBinding allocationType = (ReferenceBinding) this.resolvedType;
		this.binding = scope.getConstructor(allocationType, argumentTypes, this);
		if (!this.binding.isValidBinding()) {
			ReferenceBinding enclosingTypeBinding = allocationType;
			MethodBinding contructorBinding = this.binding;
			while (!contructorBinding.isValidBinding() && (enclosingTypeBinding.isMemberType() || enclosingTypeBinding.isLocalType())) {
				enclosingTypeBinding = enclosingTypeBinding.enclosingType();
				contructorBinding = scope.getConstructor(enclosingTypeBinding, argumentTypes, this);
			}
			if (contructorBinding.isValidBinding()) {
				this.binding = contructorBinding;
			}
		}
		if (!this.binding.isValidBinding()) {
			// First try to search a method instead
			MethodBinding methodBinding = scope.getMethod(this.resolvedType, this.resolvedType.sourceName(), argumentTypes, this);
			if (methodBinding.isValidBinding()) {
				this.binding = methodBinding;
			} else {
				if (this.binding.declaringClass == null) {
					this.binding.declaringClass = allocationType;
				}
				scope.problemReporter().javadocInvalidConstructor(this, this.binding, scope.getDeclarationModifiers());
			}
			return this.resolvedType;
		} else if (binding.isVarargs()) {
			int length = argumentTypes.length;
			if (!(binding.parameters.length == length && argumentTypes[length-1].isArrayType())) {
				MethodBinding problem = new ProblemMethodBinding(this.binding, this.binding.selector, argumentTypes, ProblemReasons.NotFound);
				scope.problemReporter().javadocInvalidConstructor(this, problem, scope.getDeclarationModifiers());
			}
		} else if (hasTypeVarArgs) {
			MethodBinding problem = new ProblemMethodBinding(this.binding, this.binding.selector, argumentTypes, ProblemReasons.NotFound);
			scope.problemReporter().javadocInvalidConstructor(this, problem, scope.getDeclarationModifiers());
		}
		if (isMethodUseDeprecated(this.binding, scope, true)) {
			scope.problemReporter().javadocDeprecatedMethod(this.binding, this, scope.getDeclarationModifiers());
		}
		return allocationType;
	}

	public boolean isSuperAccess() {
		return (this.bits & ASTNode.SuperAccess) != 0;
	}

	public TypeBinding resolveType(BlockScope scope) {
		return internalResolveType(scope);
	}

	public TypeBinding resolveType(ClassScope scope) {
		return internalResolveType(scope);
	}
	public void traverse(ASTVisitor visitor, BlockScope scope) {
		if (visitor.visit(this, scope)) {
			if (this.type != null) { // enum constant scenario
				this.type.traverse(visitor, scope);
			}
			if (this.arguments != null) {
				for (int i = 0, argumentsLength = this.arguments.length; i < argumentsLength; i++)
					this.arguments[i].traverse(visitor, scope);
			}
		}
		visitor.endVisit(this, scope);
	}
	public void traverse(ASTVisitor visitor, ClassScope scope) {
		if (visitor.visit(this, scope)) {
			if (this.type != null) { // enum constant scenario
				this.type.traverse(visitor, scope);
			}
			if (this.arguments != null) {
				for (int i = 0, argumentsLength = this.arguments.length; i < argumentsLength; i++)
					this.arguments[i].traverse(visitor, scope);
			}
		}
		visitor.endVisit(this, scope);
	}
	public int getASTType() {
		return IASTNode.JSDOC_ALLOCATION_EXPRESSION;
	
	}
}
