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
import org.eclipse.wst.jsdt.core.ast.IJsDocFieldReference;
import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.impl.Constant;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Binding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ClassScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ProblemFieldBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ProblemReasons;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Scope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;

public class JavadocFieldReference extends FieldReference implements IJsDocFieldReference {

	public int tagSourceStart, tagSourceEnd;
	public int tagValue;
	public MethodBinding methodBinding;

	public JavadocFieldReference(char[] source, long pos) {
		super(source, pos);
		this.bits |= InsideJavadoc;
	}

	/*
	public Binding getBinding() {
		if (this.methodBinding != null) {
			return this.methodBinding;
		}
		return this.binding;
	}
	*/

	/*
	 * Resolves type on a Block or Class scope.
	 */
	protected TypeBinding internalResolveType(Scope scope) {

		this.constant = Constant.NotAConstant;
		if (this.receiver == null) {
			this.receiverType = scope.enclosingSourceType();
		} else if (scope.kind == Scope.CLASS_SCOPE) {
			this.receiverType = this.receiver.resolveType((ClassScope) scope);
		} else {
			this.receiverType = this.receiver.resolveType((BlockScope)scope);
		}
		if (this.receiverType == null) {
			return null;
		}
		
		//temp fix for 267053 - I don't think these javadoc classes have ever been updated to work well with javascript - cmj
		if(this.receiver != null && this.receiver.isThis() && scope.classScope() == null) {
			return null;
		}

		Binding fieldBinding = (this.receiver != null && this.receiver.isThis())
			? scope.classScope().getBinding(this.token, this.bits & RestrictiveFlagMASK, this, true /*resolve*/)
			: scope.getField(this.receiverType, this.token, this);
		if (!fieldBinding.isValidBinding()) {
			// implicit lookup may discover issues due to static/constructor contexts. javadoc must be resilient
			switch (fieldBinding.problemId()) {
				case ProblemReasons.NonStaticReferenceInConstructorInvocation:
				case ProblemReasons.NonStaticReferenceInStaticContext:
				case ProblemReasons.InheritedNameHidesEnclosingName :
					FieldBinding closestMatch = ((ProblemFieldBinding)fieldBinding).closestMatch;
					if (closestMatch != null) {
						fieldBinding = closestMatch; // ignore problem if can reach target field through it
					}
			}
		}
		// When there's no valid field binding, try to resolve possible method reference without parenthesis
		if (!fieldBinding.isValidBinding() || !(fieldBinding instanceof FieldBinding)) {
			if (this.receiverType instanceof ReferenceBinding) {
				ReferenceBinding refBinding = (ReferenceBinding) this.receiverType;
				MethodBinding[] methodBindings = refBinding.getMethods(this.token);
				if (methodBindings == null) {
					scope.problemReporter().javadocInvalidField(this.sourceStart, this.sourceEnd, fieldBinding, this.receiverType, scope.getDeclarationModifiers());
				} else {
					switch (methodBindings.length) {
						case 0:
							// no method was found: report problem
							scope.problemReporter().javadocInvalidField(this.sourceStart, this.sourceEnd, fieldBinding, this.receiverType, scope.getDeclarationModifiers());
							break;
						case 1:
							// one method binding was found: store binding in specific field
							this.methodBinding = methodBindings[0];
							break;
					}
				}
			}
			return null;
		}
		this.binding = (FieldBinding) fieldBinding;

		if (isFieldUseDeprecated(this.binding, scope, (this.bits & IsStrictlyAssigned) != 0)) {
			scope.problemReporter().javadocDeprecatedField(this.binding, this, scope.getDeclarationModifiers());
		}
		return this.resolvedType = this.binding.type;
	}

	public boolean isSuperAccess() {
		return (this.bits & ASTNode.SuperAccess) != 0;
	}

	public StringBuffer printExpression(int indent, StringBuffer output) {

		if (this.receiver != null) {
			this.receiver.printExpression(0, output);
		}
		output.append('#').append(this.token);
		return output;
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
	public void traverse(ASTVisitor visitor, BlockScope scope) {

		if (visitor.visit(this, scope)) {
			if (this.receiver != null) {
				this.receiver.traverse(visitor, scope);
			}
		}
		visitor.endVisit(this, scope);
	}
	public void traverse(ASTVisitor visitor, ClassScope scope) {

		if (visitor.visit(this, scope)) {
			if (this.receiver != null) {
				this.receiver.traverse(visitor, scope);
			}
		}
		visitor.endVisit(this, scope);
	}
	public int getASTType() {
		return IASTNode.JSDOC_FIELD_REFERENCE;
	
	}
}
