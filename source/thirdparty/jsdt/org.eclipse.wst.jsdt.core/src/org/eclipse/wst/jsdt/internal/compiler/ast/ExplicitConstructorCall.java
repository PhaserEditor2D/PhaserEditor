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
import org.eclipse.wst.jsdt.core.ast.IExplicitConstructorCall;
import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowContext;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowInfo;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Binding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ExtraCompilerModifiers;
import org.eclipse.wst.jsdt.internal.compiler.lookup.InvocationSite;
import org.eclipse.wst.jsdt.internal.compiler.lookup.LocalTypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.MethodScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ProblemMethodBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TagBits;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeConstants;
import org.eclipse.wst.jsdt.internal.compiler.lookup.VariableBinding;

public class ExplicitConstructorCall extends Statement implements InvocationSite, IExplicitConstructorCall {

	public Expression[] arguments;
	public Expression qualification;
	public MethodBinding binding;							// exact binding resulting from lookup
	protected MethodBinding codegenBinding;		// actual binding used for code generation (if no synthetic accessor)
	public int accessMode;
	public TypeReference[] typeArguments;
	public TypeBinding[] genericTypeArguments;

	public final static int ImplicitSuper = 1;
	public final static int This = 3;

	public VariableBinding[][] implicitArguments;

	// TODO Remove once DOMParser is activated
	public int typeArgumentsSourceStart;

	public ExplicitConstructorCall(int accessMode) {
		this.accessMode = accessMode;
	}

	public FlowInfo analyseCode(
		BlockScope currentScope,
		FlowContext flowContext,
		FlowInfo flowInfo) {

		// must verify that exceptions potentially thrown by this expression are caught in the method.

		try {
			((MethodScope) currentScope).isConstructorCall = true;

			// process enclosing instance
			if (qualification != null) {
				flowInfo =
					qualification
						.analyseCode(currentScope, flowContext, flowInfo)
						.unconditionalInits();
			}
			// process arguments
			if (arguments != null) {
				for (int i = 0, max = arguments.length; i < max; i++) {
					flowInfo =
						arguments[i]
							.analyseCode(currentScope, flowContext, flowInfo)
							.unconditionalInits();
				}
			}
			manageEnclosingInstanceAccessIfNecessary(currentScope, flowInfo);
			manageSyntheticAccessIfNecessary(currentScope, flowInfo);
			return flowInfo;
		} finally {
			((MethodScope) currentScope).isConstructorCall = false;
		}
	}

	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.lookup.InvocationSite#genericTypeArguments()
	 */
	public TypeBinding[] genericTypeArguments() {
		return this.genericTypeArguments;
	}
	public boolean isImplicitSuper() {
		//return true if I'm of these compiler added statement super();

		return (accessMode == ImplicitSuper);
	}

	public boolean isSuperAccess() {

		return accessMode != This;
	}

	public boolean isTypeAccess() {

		return true;
	}

	/* Inner emulation consists in either recording a dependency
	 * link only, or performing one level of propagation.
	 *
	 * Dependency mechanism is used whenever dealing with source target
	 * types, since by the time we reach them, we might not yet know their
	 * exact need.
	 */
	void manageEnclosingInstanceAccessIfNecessary(BlockScope currentScope, FlowInfo flowInfo) {
		ReferenceBinding superTypeErasure = binding.declaringClass;

		if ((flowInfo.tagBits & FlowInfo.UNREACHABLE) == 0)	{
		// perform some emulation work in case there is some and we are inside a local type only
		if (superTypeErasure.isNestedType()
			&& currentScope.enclosingSourceType().isLocalType()) {

			if (superTypeErasure.isLocalType()) {
				((LocalTypeBinding) superTypeErasure).addInnerEmulationDependent(currentScope, qualification != null);
			}
		}
		}
	}

	public void manageSyntheticAccessIfNecessary(BlockScope currentScope, FlowInfo flowInfo) {

		if ((flowInfo.tagBits & FlowInfo.UNREACHABLE) == 0)	{
		// if constructor from parameterized type got found, use the original constructor at codegen time
		this.codegenBinding = this.binding.original();

		// perform some emulation work in case there is some and we are inside a local type only
		if (binding.isPrivate() && accessMode != This) {
			ReferenceBinding declaringClass = this.codegenBinding.declaringClass;
			// from 1.4 on, local type constructor can lose their private flag to ease emulation
			if ((declaringClass.tagBits & TagBits.IsLocalType) != 0 	&& currentScope.compilerOptions().complianceLevel >= ClassFileConstants.JDK1_4) {
				// constructor will not be dumped as private, no emulation required thus
				this.codegenBinding.tagBits |= TagBits.ClearPrivateModifier;
			}
		}
		}
	}

	public StringBuffer printStatement(int indent, StringBuffer output) {

		printIndent(indent, output);
		if (qualification != null) qualification.printExpression(0, output).append('.');
		if (typeArguments != null) {
			output.append('<');
			int max = typeArguments.length - 1;
			for (int j = 0; j < max; j++) {
				typeArguments[j].print(0, output);
				output.append(", ");//$NON-NLS-1$
			}
			typeArguments[max].print(0, output);
			output.append('>');
		}
		if (accessMode == This) {
			output.append("this("); //$NON-NLS-1$
		} else {
			output.append("super("); //$NON-NLS-1$
		}
		if (arguments != null) {
			for (int i = 0; i < arguments.length; i++) {
				if (i > 0) output.append(", "); //$NON-NLS-1$
				arguments[i].printExpression(0, output);
			}
		}
		return output.append(");"); //$NON-NLS-1$
	}

	public void resolve(BlockScope scope) {
		// the return type should be void for a constructor.
		// the test is made into getConstructor

		// mark the fact that we are in a constructor call.....
		// unmark at all returns
		MethodScope methodScope = scope.methodScope();
		try {
			AbstractMethodDeclaration methodDeclaration = methodScope.referenceMethod();
			if (methodDeclaration == null
					|| !methodDeclaration.isConstructor()
					|| ((ConstructorDeclaration) methodDeclaration).constructorCall != this) {
				// fault-tolerance
				if (this.qualification != null) {
					this.qualification.resolveType(scope);
				}
				if (this.typeArguments != null) {
					for (int i = 0, max = this.typeArguments.length; i < max; i++) {
						this.typeArguments[i].resolveType(scope, true /* check bounds*/);
					}
				}
				if (this.arguments != null) {
					for (int i = 0, max = this.arguments.length; i < max; i++) {
						this.arguments[i].resolveType(scope);
					}
				}
				return;
			}
			methodScope.isConstructorCall = true;
			ReferenceBinding receiverType = scope.enclosingReceiverType();
			if (accessMode != This)
				receiverType = receiverType.getSuperBinding();

			if (receiverType == null) {
				return;
			}
			// qualification should be from the type of the enclosingType
			if (qualification != null) {
				
				ReferenceBinding enclosingType = receiverType.enclosingType();
				if (enclosingType == null) {
					this.bits |= ASTNode.DiscardEnclosingInstance;
				} else {
					qualification.resolveTypeExpecting(scope, enclosingType);
				}
			}
			// resolve type arguments (for generic constructor call)
			if (this.typeArguments != null) {
				int length = this.typeArguments.length;
				boolean argHasError = false; // typeChecks all arguments
				this.genericTypeArguments = new TypeBinding[length];
				for (int i = 0; i < length; i++) {
					TypeReference typeReference = this.typeArguments[i];
					if ((this.genericTypeArguments[i] = typeReference.resolveType(scope, true /* check bounds*/)) == null) {
						argHasError = true;
					}
				}
				if (argHasError) {
					return;
				}
			}

			// arguments buffering for the method lookup
			TypeBinding[] argumentTypes = Binding.NO_PARAMETERS;
			boolean argsContainCast = false;
			if (arguments != null) {
				boolean argHasError = false; // typeChecks all arguments
				int length = arguments.length;
				argumentTypes = new TypeBinding[length];
				for (int i = 0; i < length; i++) {
					Expression argument = this.arguments[i];
					if ((argumentTypes[i] = argument.resolveType(scope)) == null) {
						argHasError = true;
					}
				}
				if (argHasError) {
					// record a best guess, for clients who need hint about possible contructor match
					TypeBinding[] pseudoArgs = new TypeBinding[length];
					for (int i = length; --i >= 0;) {
						pseudoArgs[i] = argumentTypes[i] == null ? TypeBinding.NULL : argumentTypes[i]; // replace args with errors with null type
					}
					this.binding = scope.findMethod(receiverType, TypeConstants.INIT, pseudoArgs, this);
					if (this.binding != null && !this.binding.isValidBinding()) {
						MethodBinding closestMatch = ((ProblemMethodBinding)this.binding).closestMatch;
						// record the closest match, for clients who may still need hint about possible method match
						if (closestMatch != null) {
							this.binding = closestMatch;
							MethodBinding closestMatchOriginal = closestMatch.original();
							if ((closestMatchOriginal.isPrivate() || closestMatchOriginal.declaringClass.isLocalType()) && !scope.isDefinedInMethod(closestMatchOriginal)) {
								// ignore cases where method is used from within inside itself (e.g. direct recursions)
								closestMatchOriginal.modifiers |= ExtraCompilerModifiers.AccLocallyUsed;
							}
						}
					}
					return;
				}
			}
			if ((binding = scope.getConstructor(receiverType, argumentTypes, this)).isValidBinding()) {
				if (isMethodUseDeprecated(this.binding, scope, this.accessMode != ImplicitSuper))
					scope.problemReporter().deprecatedMethod(binding, this);
				checkInvocationArguments(scope, null, receiverType, binding, this.arguments, argumentTypes, argsContainCast, this);
				if (binding.isPrivate() || receiverType.isLocalType()) {
					binding.original().modifiers |= ExtraCompilerModifiers.AccLocallyUsed;
				}
			} else {
				if (binding.declaringClass == null)
					binding.declaringClass = receiverType;
				scope.problemReporter().invalidConstructor(this, binding);
			}
		} finally {
			methodScope.isConstructorCall = false;
		}
	}

	public void setActualReceiverType(ReferenceBinding receiverType) {
		// ignored
	}

	public void setDepth(int depth) {
		// ignore for here
	}

	public void setFieldIndex(int depth) {
		// ignore for here
	}

	public void traverse(ASTVisitor visitor, BlockScope scope) {

		if (visitor.visit(this, scope)) {
			if (this.qualification != null) {
				this.qualification.traverse(visitor, scope);
			}
			if (this.typeArguments != null) {
				for (int i = 0, typeArgumentsLength = this.typeArguments.length; i < typeArgumentsLength; i++) {
					this.typeArguments[i].traverse(visitor, scope);
				}
			}
			if (this.arguments != null) {
				for (int i = 0, argumentLength = this.arguments.length; i < argumentLength; i++)
					this.arguments[i].traverse(visitor, scope);
			}
		}
		visitor.endVisit(this, scope);
	}
	public int getASTType() {
		return IASTNode.EXPLICIT_CONSTRUCTOR_CALL;
	
	}
}
