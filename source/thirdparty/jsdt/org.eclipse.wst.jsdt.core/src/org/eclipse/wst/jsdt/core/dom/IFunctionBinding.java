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

package org.eclipse.wst.jsdt.core.dom;

/**
 * A method binding represents a method or constructor of a class or interface.
 * Method bindings usually correspond directly to method or
 * constructor declarations found in the source code.
 * However, in certain cases of references to a generic method,
 * the method binding may correspond to a copy of a generic method
 * declaration with substitutions for the method's type parameters
 * (for these, <code>getTypeArguments</code> returns a non-empty
 * list, and either <code>isParameterizedMethod</code> or
 * <code>isRawMethod</code> returns <code>true</code>).
 * And in certain cases of references to a method declared in a
 * generic type, the method binding may correspond to a copy of a
 * method declaration with substitutions for the type's type
 * parameters (for these, <code>getTypeArguments</code> returns
 * an empty list, and both <code>isParameterizedMethod</code> and
 * <code>isRawMethod</code> return <code>false</code>).
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 *
 * @see ITypeBinding#getDeclaredMethods()
 * 
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public interface IFunctionBinding extends IBinding {

	/**
	 * Returns whether this binding is for a constructor or a method.
	 *
	 * @return <code>true</code> if this is the binding for a constructor,
	 *    and <code>false</code> if this is the binding for a method
	 */
	public boolean isConstructor();

	/**
	 * Returns whether this binding is known to be a compiler-generated
	 * default constructor.
	 * <p>
	 * This method returns <code>false</code> for:
	 * <ul>
	 * <li>methods</li>
	 * <li>constructors with more than one parameter</li>
	 * <li>0-argument constructors where the binding information was obtained
	 * from a JavaScript source file containing an explicit 0-argument constructor
	 * declaration</li>
	 * <li>0-argument constructors where the binding information was obtained
	 * from a JavaScript class file (it is not possible to determine from a
	 * class file whether a 0-argument constructor was present in the source
	 * code versus generated automatically by a JavaScript compiler)</li>
	 * </ul>
	 *
	 * @return <code>true</code> if this is known to be the binding for a
	 * compiler-generated default constructor, and <code>false</code>
	 * otherwise
	 *  
	 */
	public boolean isDefaultConstructor();

	/**
	 * Returns the name of the method declared in this binding. The method name
	 * is always a simple identifier. The name of a constructor is always the
	 * same as the declared name of its declaring class.
	 *
	 * @return the name of this method, or the declared name of this
	 *   constructor's declaring class
	 */
	public String getName();

	/**
	 * Returns the type binding representing the class or interface
	 * that declares this method or constructor.
	 *
	 * @return the binding of the class or interface that declares this method
	 *    or constructor
	 */
	public ITypeBinding getDeclaringClass();

	/**
	 * Returns the resolved default value of an annotation type member,
	 * or <code>null</code> if the member has no default value, or if this
	 * is not the binding for an annotation type member.
	 * <p>
	 * Resolved values are represented as follows (same as for
	 * {@link IMemberValuePairBinding#getValue()}):
	 * <ul>
	 * <li>Primitive type - the equivalent boxed object</li>
	 * <li>java.lang.Class - the <code>ITypeBinding</code> for the class object</li>
	 * <li>java.lang.String - the string value itself</li>
	 * <li>enum type - the <code>IVariableBinding</code> for the enum constant</li>
	 * <li>annotation type - an <code>IAnnotationBinding</code></li>
	 * <li>array type - an <code>Object[]</code> whose elements are as per above
	 * (the language only allows single dimensional arrays in annotations)</li>
	 * </ul>
	 *
	 * @return the default value of this annotation type member, or <code>null</code>
	 * if none or not applicable
	 *  
	 */
	public Object getDefaultValue();


	/**
	 * Returns a list of type bindings representing the formal parameter types,
	 * in declaration order, of this method or constructor. Returns an array of
	 * length 0 if this method or constructor does not takes any parameters.
	 * <p>
	 * Note that the binding for the last parameter type of a vararg method
	 * declaration like <code>void fun(Foo... args)</code> is always for
	 * an array type (i.e., <code>Foo[]</code>) reflecting the the way varargs
	 * get compiled. However, the type binding obtained directly from
	 * the <code>SingleVariableDeclaration</code> for the vararg parameter
	 * is always for the type as written; i.e., the type binding for
	 * <code>Foo</code>.
	 * </p>
	 * <p>
	 * Note: The result does not include synthetic parameters introduced by
	 * inner class emulation.
	 * </p>
	 *
	 * @return a (possibly empty) list of type bindings for the formal
	 *   parameters of this method or constructor
	 */
	public ITypeBinding[] getParameterTypes();

	/**
	 * Returns the binding for the return type of this method. Returns the
	 * special primitive <code>void</code> return type for constructors.
	 *
	 * @return the binding for the return type of this method, or the
	 *    <code>void</code> return type for constructors
	 */
	public ITypeBinding getReturnType();

	/**
	 * Returns the binding for the method declaration corresponding to this
	 * method binding. For parameterized methods ({@link #isParameterizedMethod()})
	 * and raw methods ({@link #isRawMethod()}), this method returns the binding
	 * for the corresponding generic method. For other method bindings, this
	 * returns the same binding.
	 *
	 * <p>Note: The one notable exception is the method <code>Object.getClass()</code>,
	 * which is declared to return <code>Class&lt;? extends Object&gt;</code>, but
	 * when invoked its return type becomes <code>Class&lt;? extends
	 * </code><em>R</em><code>&gt;</code>, where <em>R</em> is the compile type of
	 * the receiver of the method invocation.</p>
	 *
	 * @return the method binding
	 *  
	 */
	public IFunctionBinding getMethodDeclaration();

	/**
	 * Returns whether this method's signature is a subsignature of the given method.
	 *
	 * @return <code>true</code> if this method's signature is a subsignature of the given method
	 *  
	 */
	public boolean isSubsignature(IFunctionBinding otherMethod);

	/**
	 * Returns whether this is a variable arity method.
	 * <p>
	 * Note: Variable arity ("varargs") methods were added in JLS3.
	 * </p>
	 *
	 * @return <code>true</code> if this is a variable arity method,
	 *    and <code>false</code> otherwise
	 *  
	 */
	public boolean isVarargs();

	/**
	 * Returns whether this method overrides the given method.
	 *
	 * @param method the method that is possibly overridden
	 * @return <code>true</code> if this method overrides the given method,
	 * and <code>false</code> otherwise
	 *  
	 */
	public boolean overrides(IFunctionBinding method);
}
