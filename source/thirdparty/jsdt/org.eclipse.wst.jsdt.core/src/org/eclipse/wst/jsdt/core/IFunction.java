/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     IBM Corporation - added J2SE 1.5 support
 *******************************************************************************/
package org.eclipse.wst.jsdt.core;

/**
 * Represents a function or a method (or constructor) declared in a type.
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 *  
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public interface IFunction extends IMember {
/**
 * Returns the simple name of this function or method.
 * For a constructor, this returns the simple name of the declaring type.
 * Note: This holds whether the constructor appears in a source or binary type
 * This is a handle-only method.
 * @return the simple name of this method
 */
String getElementName();
/**
 * Returns the number of parameters of this method.
 * This is a handle-only method.
 *
 * @return the number of parameters of this method
 */
int getNumberOfParameters();
/**
 * Returns the binding key for this method. A binding key is a key that uniquely
 * identifies this method. It allows access to generic info for parameterized
 * methods.
 *
 * @return the binding key for this method
 * @see org.eclipse.wst.jsdt.core.dom.IBinding#getKey()
 * @see BindingKey
 */
String getKey();
/**
 * Returns the names of parameters in this method.
 * Returns an empty array if this method has no parameters.
 *
 * <p>For example, a method declared as <code>function foo( text,  length)</code>
 * would return the array <code>{"text","length"}</code>.
 * </p>
 *
 * @exception JavaScriptModelException if this element does not exist or if an
 *      exception occurs while accessing its corresponding resource.
 * @return the names of parameters in this method, an empty array if this method has no parameters
 */
String[] getParameterNames() throws JavaScriptModelException;
/**
 * Returns the type signatures for the parameters of this method.
 * Returns an empty array if this method has no parameters.
 * This is a handle-only method.
 * <p>
 * The type signatures may be either unresolved (for source types)
 * or resolved (for binary types), and either basic (for basic types)
 * or rich (for parameterized types). See {@link Signature} for details.
 * </p>
 *
 * @return the type signatures for the parameters of this method, an empty array if this method has no parameters
 * @see Signature
 */
String[] getParameterTypes();
/**
 * Returns the names of parameters in this method.
 * Returns an empty array if this method has no parameters.
 *
 * <p>For example, a method declared as <code>function foo( text,  length)</code>
 * would return the array <code>{"text","length"}</code>. For the same method in a
 * binary, this would return <code>{"arg0", "arg1"}</code>.
 * </p>
 *
 * @exception JavaScriptModelException if this element does not exist or if an
 *      exception occurs while accessing its corresponding resource.
 * @return the names of parameters in this method, an empty array if this method has no parameters
 */
String[] getRawParameterNames() throws JavaScriptModelException;
/**
 * Returns the type signature of the return value of this method.
 * For constructors, this returns the signature for void.
 * <p>
 * Until EMCAScript 4 is supported, types are inferred by analying the code, and are not necessarily accurate.
 * </p>
 * <p>
 * For example, a source method declared as <code>function getName(){return "abc"}</code>
 * would return <code>"QString;"</code>.
 * </p>
 * <p>
 * The type signature may be either unresolved (for source types)
 * or resolved (for binary types), and either basic (for basic types)
 * or rich (for parameterized types). See {@link Signature} for details.
 * </p>
 *
 * @exception JavaScriptModelException if this element does not exist or if an
 *      exception occurs while accessing its corresponding resource.
 * @return the type signature of the return value of this method, void  for constructors
 * @see Signature
 */
String getReturnType() throws JavaScriptModelException;
/**
 * Returns the signature of this method. This includes the signatures for the
 * parameter types and return type, but does not include the method name,
 * exception types, or type parameters.
 * <p>
 * For example, a source method declared as <code>public void foo(String text, int length)</code>
 * would return <code>"(QString;I)V"</code>.
 * </p>
 * <p>
 * The type signatures embedded in the method signature may be either unresolved
 * (for source types) or resolved (for binary types), and either basic (for
 * basic types) or rich (for parameterized types). See {@link Signature} for
 * details.
 * </p>
 *
 * @return the signature of this method
 * @exception JavaScriptModelException if this element does not exist or if an
 *      exception occurs while accessing its corresponding resource.
 * @see Signature
 */
String getSignature() throws JavaScriptModelException;
/**
 * Returns whether this method is a constructor.
 *
 * @exception JavaScriptModelException if this element does not exist or if an
 *      exception occurs while accessing its corresponding resource.
 *
 * @return true if this method is a constructor, false otherwise
 */
boolean isConstructor() throws JavaScriptModelException;

/**
 * Returns whether this method represents a resolved method.
 * If a method is resoved, its key contains resolved information.
 *
 * @return whether this method represents a resolved method.
 */
boolean isResolved();
/**
 * Returns whether this method is similar to the given method.
 * Two methods are similar if:
 * <ul>
 * <li>their element names are equal</li>
 * <li>they have the same number of parameters</li>
 * <li>the simple names of their parameter types are equal</li>
 * </ul>
 * This is a handle-only method.
 *
 * @param method the given method
 * @return true if this method is similar to the given method.
 * @see Signature#getSimpleName(char[])
 */
boolean isSimilar(IFunction method);
public IFunction getFunction(String selector, String[] parameterTypeSignatures) ;
}
