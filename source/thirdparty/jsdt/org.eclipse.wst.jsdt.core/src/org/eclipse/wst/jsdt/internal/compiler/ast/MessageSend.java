/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Nick Teryaev - fix for bug (https://bugs.eclipse.org/bugs/show_bug.cgi?id=40752)
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.compiler.ast;

import org.eclipse.wst.jsdt.core.ast.IASTNode;
import org.eclipse.wst.jsdt.core.ast.IExpression;
import org.eclipse.wst.jsdt.core.ast.IFunctionCall;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowContext;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowInfo;
import org.eclipse.wst.jsdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.wst.jsdt.internal.compiler.impl.Constant;
import org.eclipse.wst.jsdt.internal.compiler.impl.ReferenceContext;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Binding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ExtraCompilerModifiers;
import org.eclipse.wst.jsdt.internal.compiler.lookup.IndirectMethodBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.InvocationSite;
import org.eclipse.wst.jsdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ProblemMethodBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ProblemReasons;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.problem.ProblemSeverities;

public class MessageSend extends Expression implements InvocationSite, IFunctionCall {

	public Expression receiver;
	public char[] selector;
	public Expression[] arguments;
	
	/**
	 * exact binding resulting from lookup
	 */
	public MethodBinding binding; 
	
	/**
	 * for generic method invocation (return type inference)
	 */
	public TypeBinding expectedType; 

	/**
	 * (start<<32)+end
	 */
	public long nameSourcePosition;

	public TypeBinding actualReceiverType;


	public char[] getSelector() {
		return this.selector;
	}

	public IExpression[] getArguments() {
		return this.arguments;
	}


	public FlowInfo analyseCode(BlockScope currentScope, FlowContext flowContext, FlowInfo flowInfo) {

		boolean nonStatic = binding == null || !binding.isStatic();
		if (receiver != null) {
			flowInfo = receiver.analyseCode(currentScope, flowContext, flowInfo, nonStatic).unconditionalInits();
			if (nonStatic) {
				receiver.checkNPE(currentScope, flowContext, flowInfo);
			}
		}
		if (selector != null) {
			Binding existingVariable = currentScope.getLocalBinding(selector, Binding.VARIABLE, this, false);
			if (existingVariable != null && existingVariable instanceof LocalVariableBinding) {
				LocalVariableBinding localBinding = (LocalVariableBinding) existingVariable;

				if ((flowInfo.tagBits & FlowInfo.UNREACHABLE) == 0) {
					localBinding.useFlag = LocalVariableBinding.USED;
				}
				else if (localBinding.useFlag == LocalVariableBinding.UNUSED) {
					localBinding.useFlag = LocalVariableBinding.FAKE_USED;
				}
			}
		}

		if (arguments != null) {
			int length = arguments.length;
			for (int i = 0; i < length; i++) {
				flowInfo = arguments[i].analyseCode(currentScope, flowContext, flowInfo).unconditionalInits();
			}
		}
		return flowInfo;
	}

	public boolean isSuperAccess() {
		return receiver != null && receiver.isSuper();
	}

	public boolean isTypeAccess() {
		return receiver != null && receiver.isTypeReference();
	}

	public int nullStatus(FlowInfo flowInfo) {
		return FlowInfo.UNKNOWN;
	}

	public StringBuffer printExpression(int indent, StringBuffer output) {
		if (receiver != null && !receiver.isImplicitThis()) {
			receiver.printExpression(0, output);
			if (selector != null) {
				output.append('.');
			}
		}
		if (selector != null) {
			output.append(selector);
		}
		
		output.append('(');
		if (arguments != null) {
			for (int i = 0; i < arguments.length; i++) {
				if (i > 0) {
					output.append(", "); //$NON-NLS-1$
				}
				arguments[i].printExpression(0, output);
			}
		}
		return output.append(')');
	}

	public TypeBinding resolveType(BlockScope scope) {
		// Answer the signature return type Base type promotion
		constant = Constant.NotAConstant;

		if (receiver instanceof FunctionExpression) {
			FunctionExpression expr = (FunctionExpression) receiver;
			if (expr.methodDeclaration != null) {
				if (arguments != null && expr.methodDeclaration.arguments != null) {
					for (int i = 0; i < Math.min(arguments.length, expr.methodDeclaration.arguments.length); i++) {
						Expression msgSndArgument = arguments[i];
						Argument funcExprArgument = expr.methodDeclaration.arguments[i];

						if (msgSndArgument != null) {
							msgSndArgument.resolve(scope);
							if (msgSndArgument.resolvedType != null) {
								funcExprArgument.type = new SingleTypeReference(msgSndArgument.resolvedType.readableName(), 0);
								funcExprArgument.type.resolvedType = arguments[i].resolvedType;
							}
						}
					}
				}
			}
		}

		this.actualReceiverType = (receiver != null) ? receiver.resolveType(scope) : null;
		boolean receiverIsType = (receiver instanceof NameReference || receiver instanceof FieldReference || receiver instanceof ThisReference) && (receiver.bits & Binding.TYPE) != 0;

		// will check for null after args are resolved
		TypeBinding[] argumentTypes = Binding.NO_PARAMETERS;
		if (arguments != null) {
			int length = arguments.length;
			argumentTypes = new TypeBinding[length];
			for (int i = 0; i < length; i++) {
				Expression argument = arguments[i];
				if ((argumentTypes[i] = argument.resolveType(scope)) == null) {
					argumentTypes[i] = TypeBinding.UNKNOWN;
				}
			}
		}

		if (selector == null) {
			this.binding = new IndirectMethodBinding(0, this.actualReceiverType, argumentTypes, scope.compilationUnitScope().referenceContext.compilationUnitBinding);
		} else {
			if (receiver == null) {
				this.binding = scope.getImplicitMethod(selector, argumentTypes, this);
			} else {
				this.binding = scope.getMethod(this.actualReceiverType, selector, argumentTypes, this);
				// if receiver type was function, try using binding from receiver
				if (!binding.isValidBinding() && (this.actualReceiverType != null && this.actualReceiverType.isFunctionType())) {
					Binding alternateBinding = receiver.alternateBinding();
					if (alternateBinding instanceof TypeBinding) {
						this.actualReceiverType = (TypeBinding) alternateBinding;
						this.binding = scope.getMethod(this.actualReceiverType, selector, argumentTypes, this);
						receiverIsType = true;
					}
				}
				else if (!binding.isValidBinding() && receiverIsType) {
					/* we are a type, check the alternate binding which will
					 * be a Function object */
					Binding alternateBinding = scope.getJavaLangFunction();
					MethodBinding tempBinding = scope.getMethod((TypeBinding) alternateBinding, selector, argumentTypes, this);
					if (tempBinding.isValidBinding()) {
						this.actualReceiverType = (TypeBinding) alternateBinding;
						this.binding = tempBinding;
						receiverIsType = false;
					}
				}

			}
			if (argumentTypes.length != this.binding.parameters.length)
				scope.problemReporter().wrongNumberOfArguments(this, this.binding);
		}

		if (!binding.isValidBinding() && !(this.actualReceiverType == TypeBinding.ANY || this.actualReceiverType == TypeBinding.UNKNOWN)) {
			if (binding.declaringClass == null) {
				if (this.actualReceiverType == null || this.actualReceiverType instanceof ReferenceBinding) {
					binding.declaringClass = (ReferenceBinding) this.actualReceiverType;
				}
				else {
					return null;
				}
			}
			scope.problemReporter().invalidMethod(this, binding);
			MethodBinding closestMatch = ((ProblemMethodBinding) binding).closestMatch;
			switch (this.binding.problemId()) {
				case ProblemReasons.Ambiguous :
					break; // no resilience on ambiguous
				case ProblemReasons.NotVisible :
				case ProblemReasons.NonStaticReferenceInConstructorInvocation :
				case ProblemReasons.NonStaticReferenceInStaticContext :
				case ProblemReasons.ReceiverTypeNotVisible :
					// only steal returnType in cases listed above
					if (closestMatch != null)
						this.resolvedType = closestMatch.returnType;
			}
			/* record the closest match, for clients who may still need hint
			 * about possible method match */
			if (closestMatch != null) {
				this.binding = closestMatch;
				MethodBinding closestMatchOriginal = closestMatch.original();
				if ((closestMatchOriginal.isPrivate() || closestMatchOriginal.declaringClass.isLocalType()) && !scope.isDefinedInMethod(closestMatchOriginal)) {
					// ignore cases where method is used from within inside itself (e.g. direct recursions)
					closestMatchOriginal.original().modifiers |= ExtraCompilerModifiers.AccLocallyUsed;
				}
			}
			
			return this.resolvedType;
		}
		final CompilerOptions compilerOptions = scope.compilerOptions();
		if (!binding.isStatic()) {
			/* the "receiver" must not be a type, in other words, a
			 * NameReference that the TC has bound to a Type */
			if (receiverIsType && binding.isValidBinding()) {
				scope.problemReporter().mustUseAStaticMethod(this, binding);
			}
		}
		else {
			if (receiver != null) {
				// static message invoked through receiver? legal but unoptimal (optional warning).
				if (!(receiver.isImplicitThis() || receiver.isSuper() || receiverIsType)) {
					scope.problemReporter().nonStaticAccessToStaticMethod(this, binding);
				}
				if (!receiver.isImplicitThis() && binding.declaringClass != actualReceiverType) {
					// scope.problemReporter().indirectAccessToStaticMethod(this, binding);
				}
			}
		}

		if (isMethodUseDeprecated(binding, scope, true)) {
			scope.problemReporter().deprecatedMethod(binding, this);
		}


		TypeBinding returnType = this.binding.returnType;
		if (returnType == null) {
			returnType = TypeBinding.UNKNOWN;
		}
		
		this.resolvedType = returnType;

		if (receiver != null && receiver.isSuper() && compilerOptions.getSeverity(CompilerOptions.OverridingMethodWithoutSuperInvocation) != ProblemSeverities.Ignore) {
			final ReferenceContext referenceContext = scope.methodScope().referenceContext;
			if (referenceContext instanceof AbstractMethodDeclaration) {
				final AbstractMethodDeclaration abstractMethodDeclaration = (AbstractMethodDeclaration) referenceContext;
				MethodBinding enclosingMethodBinding = abstractMethodDeclaration.binding;
				if (enclosingMethodBinding.isOverriding() && CharOperation.equals(this.binding.selector, enclosingMethodBinding.selector) && this.binding.areParametersEqual(enclosingMethodBinding)) {
					abstractMethodDeclaration.bits |= ASTNode.OverridingMethodWithSupercall;
				}
			}
		}
		return this.resolvedType;
	}

	public void setActualReceiverType(ReferenceBinding receiverType) {
		// error scenario only
		if (receiverType == null) {
			return; 
		}
		
		this.actualReceiverType = receiverType;
	}

	public void setDepth(int depth) {
		bits &= ~DepthMASK; // flush previous depth if any
		if (depth > 0) {
			bits |= (depth & 0xFF) << DepthSHIFT; // encoded on 8 bits
		}
	}

	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.ast.Expression#setExpectedType(org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding)
	 */
	public void setExpectedType(TypeBinding expectedType) {
		this.expectedType = expectedType;
	}

	public void setFieldIndex(int depth) {
		// ignore for here
	}

	public void traverse(ASTVisitor visitor, BlockScope blockScope) {
		if (visitor.visit(this, blockScope)) {
			if (receiver != null) {
				receiver.traverse(visitor, blockScope);
			}
			
			if (arguments != null) {
				int argumentsLength = arguments.length;
				for (int i = 0; i < argumentsLength; i++) {
					arguments[i].traverse(visitor, blockScope);
				}
			}
		}
		visitor.endVisit(this, blockScope);
	}

	public int getASTType() {
		return IASTNode.FUNCTION_CALL;

	}

	public IExpression getReceiver() {
		return this.receiver;
	}
}
