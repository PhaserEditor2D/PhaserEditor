/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.compiler.lookup;

import org.eclipse.wst.jsdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.wst.jsdt.internal.oaametadata.Method;

public class MethodBinding extends Binding {

	public int modifiers;
	public char[] selector;
	public TypeBinding returnType;
	public TypeBinding[] parameters;
	public ReferenceBinding declaringClass;
	char[] signature;
	public long tagBits;
	public FunctionTypeBinding functionTypeBinding;
	public ReferenceBinding allocationType;
	public Method oaaMethod;

	/**
	 * <p>
	 * {@link MethodBinding} used to create this {@link MethodBinding}.
	 * </p>
	 */
	private MethodBinding fOriginal;
	
	protected MethodBinding() {
		// for creating problem or synthetic method
	}

	public MethodBinding(int modifiers, char[] selector, TypeBinding returnType, TypeBinding[] parameters, ReferenceBinding declaringClass) {
		this.modifiers = modifiers;
		this.selector = selector;
		this.returnType = returnType;
		this.setParameters((parameters == null || parameters.length == 0) ? Binding.NO_PARAMETERS : parameters);
		this.declaringClass = declaringClass;

		// propagate the strictfp & deprecated modifiers
		if (this.declaringClass != null) {
			if (this.declaringClass.isStrictfp()) {
				if (!(isAbstract())) {
					this.modifiers |= ClassFileConstants.AccStrictfp;
				}
			}
		}
	}

	public MethodBinding(int modifiers, TypeBinding[] parameters, ReferenceBinding declaringClass) {
		this(modifiers, TypeConstants.INIT, TypeBinding.UNKNOWN, parameters, declaringClass);
	}

	/**
	 * special API used to change method declaring class for runtime visibility check
	 *
	 * @param initialMethodBinding
	 * @param declaringClass
	 */
	public MethodBinding(MethodBinding initialMethodBinding, ReferenceBinding declaringClass) {
		this.modifiers = initialMethodBinding.modifiers;
		this.selector = initialMethodBinding.selector;
		this.returnType = initialMethodBinding.returnType;
		this.parameters = initialMethodBinding.parameters;
		this.declaringClass = declaringClass;
		this.fOriginal = initialMethodBinding;
	}

	/**
	 * @return Answer true if the argument types & the receiver's parameters are equal
	 */
	public final boolean areParametersEqual(MethodBinding method) {
		TypeBinding[] args = method.parameters;
		if (parameters == args)
			return true;

		int length = parameters.length;
		if (length != args.length)
			return false;

		for (int i = 0; i < length; i++)
			if (parameters[i] != args[i])
				return false;
		return true;
	}

	/**
	 * @return true if given parameters are compatible with this method
	 *         parameters. Callers to this method should first check that the
	 *         number of TypeBindings passed as argument matches this
	 *         FunctionBinding number of parameters
	 */
	public final boolean areParametersCompatibleWith(TypeBinding[] arguments) {
		int paramLength = this.parameters.length;
		int argLength = arguments.length;
		int lastIndex = argLength;
		if (isVarargs()) {
			lastIndex = paramLength - 1;
			// accept X[] but not X or X[][]
			if (paramLength == argLength) {
				// is an ArrayBinding by definition
				TypeBinding varArgType = parameters[lastIndex]; 
				TypeBinding lastArgument = arguments[lastIndex];
				if (varArgType != lastArgument && !lastArgument.isCompatibleWith(varArgType))
					return false;
			}
			// all remainig argument types must be compatible with the elementsType of varArgType
			else if (paramLength < argLength) {
				TypeBinding varArgType = ((ArrayBinding) parameters[lastIndex]).elementsType();
				for (int i = lastIndex; i < argLength; i++)
					if (varArgType != arguments[i] && !arguments[i].isCompatibleWith(varArgType))
						return false;
			}
			// can call foo(int i, X ... x) with foo(1) but NOT foo();
			else if (lastIndex != argLength) {
				return false;
			}
			// now compare standard arguments from 0 to lastIndex
		}
		for (int i = 0; i < lastIndex; i++) {
			if (parameters[i] != arguments[i] && !arguments[i].isCompatibleWith(parameters[i])) {
				return false;
			}
		}
		
		return true;
	}

	/**
	 * API Answer the receiver's binding type from Binding.BindingID.
	 */
	public final int kind() {
		return Binding.METHOD;
	}

	/**
	 * @return true if the receiver is visible to the invocationPackage.
	 */
	public final boolean canBeSeenBy(PackageBinding invocationPackage) {
		if (isPublic())
			return true;
		if (isPrivate())
			return false;

		// isProtected() or isDefault()
		return invocationPackage == declaringClass.getPackage();
	}

	/**
	 * NOTE: This method should ONLY be sent if the receiver is a constructor.
	 * 
	 * NOTE: Cannot invoke this method with a compilation unit scope.
	 * 
	 * @return true if the receiver is visible to the type provided by the
	 *         scope. InvocationSite implements isSuperAccess() to provide
	 *         additional information if the receiver is protected.
	 */
	public final boolean canBeSeenBy(InvocationSite invocationSite, Scope scope) {
		if (isPublic())
			return true;

		SourceTypeBinding invocationType = scope.enclosingSourceType();
		if (invocationType == declaringClass)
			return true;

		if (isProtected()) {
			// answer true if the receiver is in the same package as the invocationType
			if (invocationType.fPackage == declaringClass.fPackage)
				return true;
			return invocationSite.isSuperAccess();
		}

		if (isPrivate()) {
			/* answer true if the invocationType and the declaringClass
			 * have a common enclosingType
			 * 
			 * already know they are not the identical type */
			ReferenceBinding outerInvocationType = invocationType;
			ReferenceBinding temp = outerInvocationType.enclosingType();
			while (temp != null) {
				outerInvocationType = temp;
				temp = temp.enclosingType();
			}

			ReferenceBinding outerDeclaringClass = declaringClass;
			temp = outerDeclaringClass.enclosingType();
			while (temp != null) {
				outerDeclaringClass = temp;
				temp = temp.enclosingType();
			}
			return outerInvocationType == outerDeclaringClass;
		}

		return invocationType.fPackage == declaringClass.fPackage;
	}

	/**
	 * NOTE: Cannot invoke this method with a compilation unit scope.
	 * 
	 * @return true if the receiver is visible to the type provided by the
	 *         scope. InvocationSite implements isSuperAccess() to provide
	 *         additional information if the receiver is protected.
	 */
	public final boolean canBeSeenBy(TypeBinding receiverType, InvocationSite invocationSite, Scope scope) {
		if (isPublic()) {
			return true;
		}

		SourceTypeBinding invocationType = scope.enclosingSourceType();
		if (invocationType == declaringClass && invocationType == receiverType) {
			return true;
		}

		 // static import call
		if (invocationType == null) {
			return !isPrivate() && scope.getCurrentPackage() == declaringClass.fPackage;
		}

		if (isProtected()) {
			/* answer true if the invocationType is the declaringClass or they are in the same package
			 * OR the invocationType is a subclass of the declaringClass
			 * AND the receiverType is the invocationType or its subclass
			 * OR the method is a static method accessed directly through a type
			 * OR previous assertions are true for one of the enclosing type */
			if (invocationType == declaringClass) {
				return true;
			}
			
			if (invocationType.fPackage == declaringClass.fPackage) {
				return true;
			}

			ReferenceBinding currentType = invocationType;
			TypeBinding receiverErasure = receiverType;
			ReferenceBinding declaringErasure = declaringClass;
			int depth = 0;
			do {
				if (currentType.findSuperTypeWithSameErasure(declaringErasure) != null) {
					if (invocationSite.isSuperAccess())
						return true;
					// receiverType can be an array binding in one case... see if you can change it
					if (receiverType instanceof ArrayBinding) {
						return false;
					}
					
					if (isStatic()) {
						if (depth > 0) {
							invocationSite.setDepth(depth);
						}
						
						return true;
					}
					if (currentType == receiverErasure || receiverErasure.findSuperTypeWithSameErasure(currentType) != null) {
						if (depth > 0) {
							invocationSite.setDepth(depth);
						}
						
						return true;
					}
				}
				depth++;
				currentType = currentType.enclosingType();
			} while (currentType != null);
			
			return false;
		}

		if (isPrivate()) {
			/* answer true if the receiverType is the declaringClass
			 * AND the invocationType and the declaringClass have a common  enclosingType */
			if (receiverType != declaringClass) {
				return false;
			}

			if (invocationType != declaringClass) {
				ReferenceBinding outerInvocationType = invocationType;
				ReferenceBinding temp = outerInvocationType.enclosingType();
				while (temp != null) {
					outerInvocationType = temp;
					temp = temp.enclosingType();
				}

				ReferenceBinding outerDeclaringClass = declaringClass;
				temp = outerDeclaringClass.enclosingType();
				while (temp != null) {
					outerDeclaringClass = temp;
					temp = temp.enclosingType();
				}
				
				if (outerInvocationType != outerDeclaringClass) {
					return false;
				}
			}
			return true;
		}

	PackageBinding declaringPackage = null;
	if(declaringClass != null) {
		declaringPackage = declaringClass.fPackage;
		if (invocationType.fPackage != declaringPackage) {
			return false;
		}
	}

		// receiverType can be an array binding in one case... see if you can change it
		if (receiverType instanceof ArrayBinding) {
			return false;
		}
		
		ReferenceBinding currentType = (ReferenceBinding) receiverType;
		do {
			if (declaringClass == currentType) {
				return true;
			}
			
			if (currentType == null) {
				return true;
			}
			
			PackageBinding currentPackage = currentType.fPackage;
			// package could be null for wildcards/intersection types, ignore and recurse in superclass
			if (currentPackage != null && currentPackage != declaringPackage) {
				return false;
			}
		} while ((currentType = currentType.getSuperBinding()) != null);
		
		return false;
	}

	/**
	 * declaringUniqueKey dot selector genericSignature p.X { <T> void bar(X<T> t)
	 * } --> Lp/X;.bar<T:Ljava/lang/Object;>(LX<TT;>;)V
	 */
	public char[] computeUniqueKey(boolean isLeaf) {
		// declaring class
		char[] declaringKey = this.declaringClass.computeUniqueKey(false);
		int declaringLength = declaringKey.length;

		// selector
		int selectorLength = (this.selector == TypeConstants.INIT || this.selector == null) ? 0 : this.selector.length;

		// generic signature
		char[] sig = signature();
		int signatureLength = sig.length;

		char[] uniqueKey = new char[declaringLength + 1 + selectorLength + signatureLength];
		int index = 0;
		System.arraycopy(declaringKey, 0, uniqueKey, index, declaringLength);
		index = declaringLength;
		uniqueKey[index++] = '.';
		if (this.selector != null) {
			System.arraycopy(this.selector, 0, uniqueKey, index, selectorLength);
		}
		
		index += selectorLength;
		System.arraycopy(sig, 0, uniqueKey, index, signatureLength);

		return uniqueKey;
	}

	/**
	 * @return the declaring class to use in the constant pool may not be a
	 *         reference binding (see subtypes)
	 */
	public TypeBinding constantPoolDeclaringClass() {
		return this.declaringClass;
	}

	/**
	 * <init> for constructors <clinit> for clinit methods or the source name
	 * of the method
	 * 
	 * @return the receiver's constant pool name.
	 */
	public final char[] constantPoolName() {
		return selector;
	}

	public final int getAccessFlags() {
		return modifiers & ExtraCompilerModifiers.AccJustFlag;
	}

	/**
	 * @return the default value for this annotation method or
	 *         <code>null</code> if there is no default value
	 */
	public Object getDefaultValue() {
		MethodBinding originalMethod = this.original();
		if ((originalMethod.tagBits & TagBits.DefaultValueResolved) == 0) {
			originalMethod.tagBits |= TagBits.DefaultValueResolved;
		}
		return null;
	}

	/**
	 * @return true if the receiver is an abstract method
	 */
	public final boolean isAbstract() {
		return (modifiers & ClassFileConstants.AccAbstract) != 0;
	}

	/**
	 * @return true if the receiver is a bridge method
	 */
	public final boolean isBridge() {
		return (modifiers & ClassFileConstants.AccBridge) != 0;
	}

	/**
	 * @return true if the receiver is a constructor
	 */
	public final boolean isConstructor() {
		return (selector == TypeConstants.INIT || (this.tagBits & TagBits.IsConstructor) != 0);
	}

	/**
	 * @return true if the receiver has default visibility
	 */
	public final boolean isDefault() {
		return !isPublic() && !isProtected() && !isPrivate();
	}

	/**
	 * @return true if the receiver is a system generated default abstract method
	 */
	public final boolean isDefaultAbstract() {
		return (modifiers & ExtraCompilerModifiers.AccDefaultAbstract) != 0;
	}

	/**
	 * @return true if the receiver is a deprecated method
	 */
	public final boolean isDeprecated() {
		return (modifiers & ClassFileConstants.AccDeprecated) != 0;
	}

	/**
	 * @return true if the receiver is final and cannot be overridden
	 */
	public final boolean isFinal() {
		return (modifiers & ClassFileConstants.AccFinal) != 0;
	}

	/**
	 * @return true if the receiver is implementing another method in other
	 *         words, it is overriding and concrete, and overriden method is
	 *         abstract Only set for source methods
	 */
	public final boolean isImplementing() {
		return (modifiers & ExtraCompilerModifiers.AccImplementing) != 0;
	}

	/**
	 * @return true if the receiver is overriding another method Only set for
	 *         source methods
	 */
	public final boolean isOverriding() {
		return (modifiers & ExtraCompilerModifiers.AccOverriding) != 0;
	}

	/**
	 * @return true if the receiver has private visibility
	 */
	public final boolean isPrivate() {
		return (modifiers & ClassFileConstants.AccPrivate) != 0;
	}

	/**
	 * @return true if the receiver has private visibility and is used locally
	 */
	public final boolean isUsed() {
		return (modifiers & ExtraCompilerModifiers.AccLocallyUsed) != 0;
	}

	/**
	 * @return true if the receiver has protected visibility
	 */
	public final boolean isProtected() {
		return (modifiers & ClassFileConstants.AccProtected) != 0;
	}

	/**
	 * @return true if the receiver has public visibility
	 */
	public final boolean isPublic() {
		return (modifiers & ClassFileConstants.AccPublic) != 0;
	}

	/**
	 * @return true if the receiver is a static method
	 */
	public final boolean isStatic() {
		return (modifiers & ClassFileConstants.AccStatic) != 0;
	}

	/**
	 * @return true if all float operations must adher to IEEE 754
	 *         float/double rules
	 */
	public final boolean isStrictfp() {
		return (modifiers & ClassFileConstants.AccStrictfp) != 0;
	}

	/**
	 * @return true if the receiver method has varargs
	 */
	public final boolean isVarargs() {
		return (modifiers & ClassFileConstants.AccVarargs) != 0;
	}

	/**
	 * @return true if the receiver's declaring type is deprecated (or any of
	 *         its enclosing types)
	 */
	public final boolean isViewedAsDeprecated() {
		return (modifiers & (ClassFileConstants.AccDeprecated | ExtraCompilerModifiers.AccDeprecatedImplicitly)) != 0;
	}

	/**
	 * @return {@link MethodBinding} this {@link MethodBinding} was created
	 *         with or <code>this</code> if this {@link MethodBinding} was
	 *         created from scratch.
	 */
	public MethodBinding original() {
		return this.fOriginal != null ? this.fOriginal : this;
	}

	/**
	 * foo(int, Thread)
	 * 
	 * @see org.eclipse.wst.jsdt.internal.compiler.lookup.Binding#readableName()
	 */
	public char[] readableName() {
		StringBuffer buffer = new StringBuffer(parameters.length + 1 * 20);
		if (isConstructor())
			buffer.append(declaringClass.sourceName());
		else
			buffer.append(selector);
		buffer.append('(');
		if (parameters != Binding.NO_PARAMETERS) {
			for (int i = 0, length = parameters.length; i < length; i++) {
				if (i > 0)
					buffer.append(", "); //$NON-NLS-1$
				buffer.append(parameters[i].sourceName());
			}
		}
		buffer.append(')');
		return buffer.toString().toCharArray();
	}

	public void setDefaultValue(Object defaultValue) {
		MethodBinding originalMethod = this.original();
		originalMethod.tagBits |= TagBits.DefaultValueResolved;
	}

	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.lookup.Binding#shortReadableName()
	 */
	public char[] shortReadableName() {
		StringBuffer buffer = new StringBuffer(parameters.length + 1 * 20);
		if (isConstructor())
			buffer.append(declaringClass.shortReadableName());
		else
			buffer.append(selector);
		buffer.append('(');
		if (parameters != Binding.NO_PARAMETERS) {
			for (int i = 0, length = parameters.length; i < length; i++) {
				if (i > 0)
					buffer.append(", "); //$NON-NLS-1$
				buffer.append(parameters[i].shortReadableName());
			}
		}
		buffer.append(')');
		int nameLength = buffer.length();
		char[] shortReadableName = new char[nameLength];
		buffer.getChars(0, nameLength, shortReadableName, 0);
		return shortReadableName;
	}

	protected final void setSelector(char[] selector) {
		this.selector = selector;
		this.signature = null;
	}

	/**
	 * NOTE: This method should only be used during/after code gen. The
	 * signature is cached so if the signature of the return type or any
	 * parameter type changes, the cached state is invalid.
	 * 
	 * (ILjava/lang/Thread;)Ljava/lang/Object;
	 * 
	 * @return the receiver's signature.
	 */
	public final char[] signature() {
		if (signature != null)
			return signature;

		StringBuffer buffer = new StringBuffer(parameters.length + 1 * 20);
		buffer.append('(');

		TypeBinding[] targetParameters = this.parameters;
		boolean isConstructor = isConstructor();

		boolean needSynthetics = isConstructor && declaringClass.isNestedType();

		if (targetParameters != Binding.NO_PARAMETERS) {
			for (int i = 0; i < targetParameters.length; i++) {
				buffer.append(targetParameters[i].signature());
			}
		}
		if (needSynthetics) {
			// move the extra padding arguments of the synthetic constructor invocation to the end
			for (int i = targetParameters.length, extraLength = parameters.length; i < extraLength; i++) {
				buffer.append(parameters[i].signature());
			}
		}
		buffer.append(')');
		if (this.returnType != null) {
			buffer.append(this.returnType.signature());
		}
		
		int nameLength = buffer.length();
		signature = new char[nameLength];
		buffer.getChars(0, nameLength, signature, 0);

		return signature;
	}

	public final int sourceEnd() {
		AbstractMethodDeclaration method = sourceMethod();
		if (method == null) {
			if (this.declaringClass instanceof SourceTypeBinding) {
				return ((SourceTypeBinding) this.declaringClass).sourceEnd();
			}
			
			return 0;
		}
		return method.sourceEnd;
	}

	public AbstractMethodDeclaration sourceMethod() {
		ReferenceBinding declClass = this.original().declaringClass;
		if (declClass instanceof SourceTypeBinding) {
			return ((SourceTypeBinding)declClass).sourceMethod(this.original());
		}
		return null;
	}

	public final int sourceStart() {
		AbstractMethodDeclaration method = sourceMethod();
		if (method == null) {
			if (this.declaringClass instanceof SourceTypeBinding) {
				return ((SourceTypeBinding) this.declaringClass).sourceStart();
			}
			
			return 0;
		}
		
		return method.sourceStart;
	}

	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append( (returnType != null) ? returnType.debugName() : "NULL TYPE"); //$NON-NLS-1$
		s.append( (selector != null) ? new String(selector) : "UNNAMED METHOD"); //$NON-NLS-1$

		s.append('(');
		if (parameters != null) {
			if (parameters != Binding.NO_PARAMETERS) {
				for (int i = 0, length = parameters.length; i < length; i++) {
					if (i > 0) {
						s.append(',').append(' ');
					}
					
					s.append( (parameters[i] != null) ? parameters[i].debugName() : "NULL TYPE"); //$NON-NLS-1$
				}
			}
		}
		else {
			s.append("NULL PARAMETERS"); //$NON-NLS-1$
		}
		s.append(") "); //$NON-NLS-1$

		return s.toString();
	}

	/**
	 * @return the method to use during tiebreak (usually the method itself).
	 * For generic method invocations, tiebreak needs to use generic method
	 * with erasure substitutes.
	 */
	public MethodBinding tiebreakMethod() {
		return this;
	}

	public void createFunctionTypeBinding(Scope scope) {
		functionTypeBinding = new FunctionTypeBinding(this, scope);
	}

	public MethodBinding createNamedMethodBinding(char[] name) {
		MethodBinding newBinding = new MethodBinding(this.modifiers, name, this.returnType, this.parameters, this.declaringClass);
		newBinding.functionTypeBinding = this.functionTypeBinding;
		newBinding.tagBits = this.tagBits;
		newBinding.signature = this.signature;
		return newBinding;
	}

	public void updateFrom(MethodBinding functionBinding) {
		this.returnType = functionBinding.returnType;
		this.parameters = functionBinding.parameters;
	}

	public void cleanup() {
		if (this.functionTypeBinding != null) {
			this.functionTypeBinding.cleanup();
		}
	}
	
	/**
	 * <p>
	 * Sets the parameters on this binding as well as the original binding if
	 * this binding was created from some other binding.
	 * </p>
	 * 
	 * @param params
	 *            {@link TypeBinding}s to set as the parameters of this
	 *            binding
	 */
	protected void setParameters(TypeBinding[] params) {
		this.parameters = params;
		
		if(this.fOriginal != null) {
			this.fOriginal.parameters = params;
		}
	}
	
	/**
	 * @return the parameters sfor this binding, or if not set and this
	 *         binding has an original binding then the parameters from the
	 *         original binding, else {@link Binding#NO_PARAMETERS}
	 */
	public TypeBinding[] getParameters() {
		TypeBinding[] params = Binding.NO_PARAMETERS;
		
		if(this.parameters != null && this.parameters.length > 0) {
			params = this.parameters;
		} else if(this.fOriginal.parameters != null && this.fOriginal.parameters.length > 0) {
			params = this.fOriginal.parameters;
		}
		
		return params;
	}

	void ensureBindingsAreComplete() {
		if (this.declaringClass instanceof SourceTypeBinding) {
			SourceTypeBinding parentBinding = (SourceTypeBinding) this.declaringClass;
			if ((parentBinding.tagBits & TagBits.AreMethodsComplete) == 0) {
				parentBinding.methods(); // finish resolving method bindings
			}
		}
	}
}