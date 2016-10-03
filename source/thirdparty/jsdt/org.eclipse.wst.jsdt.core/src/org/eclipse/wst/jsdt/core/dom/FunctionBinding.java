/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.wst.jsdt.core.dom;

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.ITypeRoot;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.Signature;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ExtraCompilerModifiers;
import org.eclipse.wst.jsdt.internal.compiler.lookup.LookupEnvironment;
import org.eclipse.wst.jsdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.MethodVerifier;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.problem.AbortCompilation;
import org.eclipse.wst.jsdt.internal.core.JavaElement;
import org.eclipse.wst.jsdt.internal.core.Member;
import org.eclipse.wst.jsdt.internal.core.util.Util;

/**
 * Internal implementation of method bindings.
 */
class FunctionBinding implements IFunctionBinding { 

	private static final int VALID_MODIFIERS = Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE |
		Modifier.ABSTRACT | Modifier.STATIC | Modifier.FINAL | Modifier.SYNCHRONIZED | Modifier.NATIVE |
		Modifier.STRICTFP;
	private static final ITypeBinding[] NO_TYPE_BINDINGS = new ITypeBinding[0];
	private org.eclipse.wst.jsdt.internal.compiler.lookup.MethodBinding binding;
	private BindingResolver resolver;
	private ITypeBinding[] parameterTypes;
	private String name;
	private ITypeBinding declaringClass;
	private ITypeBinding returnType;
	private String key;

	FunctionBinding(BindingResolver resolver, org.eclipse.wst.jsdt.internal.compiler.lookup.MethodBinding binding) {
		this.resolver = resolver;
		this.binding = binding;
	}

	/**
	 * @see IFunctionBinding#isConstructor()
	 */
	public boolean isConstructor() {
		return this.binding.isConstructor();
	}

	/**
	 * @see IFunctionBinding#isDefaultConstructor()
	 *  
	 */
	public boolean isDefaultConstructor() {
		final ReferenceBinding declaringClassBinding = this.binding.declaringClass;
		if (declaringClassBinding.isBinaryBinding()) {
			return false;
		}
		return (this.binding.modifiers & ExtraCompilerModifiers.AccIsDefaultConstructor) != 0;
	}

	/**
	 * @see IBinding#getName()
	 */
	public String getName() {
		if (name == null) {
			if (this.binding.isConstructor()) {
				name = this.getDeclaringClass().getName();
			} else {
				name = (this.binding.selector!=null) ? new String(this.binding.selector) : "";
			}
		}
		return name;
	}

	/**
	 * @see IFunctionBinding#getDeclaringClass()
	 */
	public ITypeBinding getDeclaringClass() {
		if (this.declaringClass == null) {
			this.declaringClass = this.resolver.getTypeBinding(this.binding.declaringClass);
		}
		return declaringClass;
	}

	/**
	 * @see IFunctionBinding#getParameterTypes()
	 */
	public ITypeBinding[] getParameterTypes() {
		if (this.parameterTypes != null) {
			return parameterTypes;
		}
		org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding[] parameters = this.binding.parameters;
		int length = parameters == null ? 0 : parameters.length;
		if (length == 0) {
			return this.parameterTypes = NO_TYPE_BINDINGS;
		} else {
			ITypeBinding[] paramTypes = new ITypeBinding[length];
			for (int i = 0; i < length; i++) {
				final TypeBinding parameterBinding = parameters[i];
				if (parameterBinding != null) {
					ITypeBinding typeBinding = this.resolver.getTypeBinding(parameterBinding);
					if (typeBinding == null) {
						return this.parameterTypes = NO_TYPE_BINDINGS;
					}
					paramTypes[i] = typeBinding;
				} else {
					// log error
					StringBuffer message = new StringBuffer("Report method binding where a parameter is null:\n");  //$NON-NLS-1$
					message.append(this.toString());
					Util.log(new IllegalArgumentException(), message.toString());
					// report no binding since one or more parameter has no binding
					return this.parameterTypes = NO_TYPE_BINDINGS;
				}
			}
			return this.parameterTypes = paramTypes;
		}
	}

	/**
	 * @see IFunctionBinding#getReturnType()
	 */
	public ITypeBinding getReturnType() {
		if (this.returnType == null) {
			this.returnType = this.resolver.getTypeBinding(this.binding.returnType);
		}
		return this.returnType;
	}

	public Object getDefaultValue() {
		return null;
	}
	
	public IJavaScriptElement getJavaElement() {
		JavaElement element = getUnresolvedJavaElement();
		if (element == null)
			return null;
		return element.resolved(this.binding);
	}

	private JavaElement getUnresolvedJavaElement() {
		IJavaScriptElement declaringElement = getDeclaringClass().getJavaElement();
		if (declaringElement == null) return null;
		if (!(this.resolver instanceof DefaultBindingResolver)) return null;
		ASTNode node = (ASTNode) ((DefaultBindingResolver) this.resolver).bindingsToAstNodes.get(this);
		ITypeRoot typeRoot=null;
		IType  declaringType=null;
		if (declaringElement instanceof ITypeRoot)
		{
			typeRoot=(ITypeRoot)declaringElement;

		}
		else if (declaringElement instanceof IType )
			declaringType=(IType)declaringElement;
//		IType declaringType=(IType)declaringElement;
		if (node != null && declaringElement.getParent().getElementType() != IJavaScriptElement.CLASS_FILE) {
			if (node instanceof FunctionDeclaration) {
				FunctionDeclaration methodDeclaration = (FunctionDeclaration) node;
				ArrayList parameterSignatures = new ArrayList();
				Iterator iterator = methodDeclaration.parameters().iterator();
				while (iterator.hasNext()) {
					SingleVariableDeclaration parameter = (SingleVariableDeclaration) iterator.next();
					Type type = parameter.getType();
					String typeSig = Util.getSignature(type);
					int arrayDim = parameter.getExtraDimensions();
					if (parameter.getAST().apiLevel() >= AST.JLS3 && parameter.isVarargs()) {
						arrayDim++;
					}
					if (arrayDim > 0) {
						typeSig = Signature.createArraySignature(typeSig, arrayDim);
					}
					parameterSignatures.add(typeSig);
				}
				int parameterCount = parameterSignatures.size();
				String[] parameters = new String[parameterCount];
				parameterSignatures.toArray(parameters);
				if (typeRoot!=null)
					return (JavaElement) typeRoot.getFunction(getName(), parameters);
				else
					return (JavaElement) declaringType.getFunction(getName(), parameters);
			}
		}
		else {
			// case of method not in the created AST, or a binary method
			MethodBinding original = this.binding.original();
			String selector = original.isConstructor() ? declaringType.getElementName() : (original.selector != null ? new String(original.selector) : null);
			if (selector != null) {
				boolean isBinary = declaringType!= null && declaringType.isBinary();
				ReferenceBinding enclosingType = original.declaringClass.enclosingType();
				boolean isInnerBinaryTypeConstructor = isBinary && original.isConstructor() && enclosingType != null;
				TypeBinding[] parameters = original.parameters;
				int length = parameters == null ? 0 : parameters.length;
				int declaringIndex = isInnerBinaryTypeConstructor ? 1 : 0;
				String[] parameterSignatures = new String[declaringIndex + length];
				if (isInnerBinaryTypeConstructor)
					parameterSignatures[0] = new String(enclosingType.signature()).replace('/', '.');
				for (int i = 0;  i < length; i++) {
					parameterSignatures[declaringIndex + i] = new String(parameters[i].signature()).replace('/', '.');
				}
				IFunction result = null;
				if (declaringType != null)
					result = declaringType.getFunction(selector, parameterSignatures);
				else if (typeRoot != null)
					result = typeRoot.getFunction(selector, parameterSignatures);
				if (isBinary)
					return (JavaElement) result;
				IFunction[] methods = null;
				try {
					if (declaringType != null)
						methods = declaringType.getFunctions();
					else if (typeRoot != null)
						methods = typeRoot.getFunctions();
				}
				catch (JavaScriptModelException e) {
					// declaring type doesn't exist
					return null;
				}
				IFunction[] candidates = Member.findMethods(result, methods);
				if (candidates == null || candidates.length == 0)
					return null;
				return (JavaElement) candidates[0];
			}
		}
		
		return null;
	}
	/**
	 * @see IBinding#getKind()
	 */
	public int getKind() {
		return IBinding.METHOD;
	}

	/**
	 * @see IBinding#getModifiers()
	 */
	public int getModifiers() {
		return this.binding.getAccessFlags() & VALID_MODIFIERS;
	}

	/**
	 * @see IBinding#isDeprecated()
	 */
	public boolean isDeprecated() {
		return this.binding.isDeprecated();
	}

	/**
	 * @see IBinding#isRecovered()
	 */
	public boolean isRecovered() {
		return false;
	}

	/**
	 * @see org.eclipse.wst.jsdt.core.dom.IFunctionBinding#isVarargs()
	 *  
	 */
	public boolean isVarargs() {
		return this.binding.isVarargs();
	}

	/**
	 * @see IBinding#getKey()
	 */
	public String getKey() {
		if (this.key == null) {
			this.key = new String(this.binding.computeUniqueKey());
		}
		return this.key;
	}

	/**
	 * @see IBinding#isEqualTo(IBinding)
	 *  
	 */
	public boolean isEqualTo(IBinding other) {
		if (other == this) {
			// identical binding - equal (key or no key)
			return true;
		}
		if (other == null) {
			// other binding missing
			return false;
		}
		if (!(other instanceof FunctionBinding)) {
			return false;
		}
		org.eclipse.wst.jsdt.internal.compiler.lookup.MethodBinding otherBinding = ((FunctionBinding) other).binding;
		return BindingComparator.isEqual(this.binding, otherBinding);
	}

	public boolean isSubsignature(IFunctionBinding otherMethod) {
		try {
			org.eclipse.wst.jsdt.internal.compiler.lookup.MethodBinding other = ((FunctionBinding) otherMethod).binding;
			if (!CharOperation.equals(this.binding.selector, other.selector))
				return false;
			return this.binding.areParametersEqual(other);
		} catch (AbortCompilation e) {
			// don't surface internal exception to clients
			// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=143013
			return false;
		}
	}

	/**
	 * @see org.eclipse.wst.jsdt.core.dom.IFunctionBinding#getMethodDeclaration()
	 */
	public IFunctionBinding getMethodDeclaration() {
		return this.resolver.getMethodBinding(this.binding.original());
	}

	/**
	 * @see IFunctionBinding#overrides(IFunctionBinding)
	 */
	public boolean overrides(IFunctionBinding overridenMethod) {
		try {
			org.eclipse.wst.jsdt.internal.compiler.lookup.MethodBinding overridenCompilerBinding = ((FunctionBinding) overridenMethod).binding;
			if (this.binding == overridenCompilerBinding)
				return false;
			char[] selector = this.binding.selector;
			if (!CharOperation.equals(selector, overridenCompilerBinding.selector))
				return false;
			TypeBinding match = this.binding.declaringClass.findSuperTypeWithSameErasure(overridenCompilerBinding.declaringClass);
			if (!(match instanceof ReferenceBinding)) return false;

			org.eclipse.wst.jsdt.internal.compiler.lookup.MethodBinding[] superMethods = ((ReferenceBinding)match).getMethods(selector);
			for (int i = 0, length = superMethods.length; i < length; i++) {
				if (superMethods[i].original() == overridenCompilerBinding) {
					LookupEnvironment lookupEnvironment = this.resolver.lookupEnvironment();
					if (lookupEnvironment == null) return false;
					MethodVerifier methodVerifier = lookupEnvironment.methodVerifier();
					org.eclipse.wst.jsdt.internal.compiler.lookup.MethodBinding superMethod = superMethods[i];
					return !superMethod.isPrivate()
						&& !(superMethod.isDefault() && (superMethod.declaringClass.getPackage()) != this.binding.declaringClass.getPackage())
						&& methodVerifier.doesMethodOverride(this.binding, superMethod);
				}
			}
			return false;
		} catch (AbortCompilation e) {
			// don't surface internal exception to clients
			// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=143013
			return false;
		}
	}

	/**
	 * For debugging purpose only.
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return this.binding.toString();
	}
}
