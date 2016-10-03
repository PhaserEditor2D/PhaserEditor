/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.core;



/**
 * Represents a container of methods and fields/vars (either an <code>IJavaScriptUnit</code>
 * or an <code>IType</code>).
 *
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 *
 * <p>
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 * </p>

 */
public interface IFunctionContainer  {


/**
 * Returns the field with the specified name
 * in this type (for example, <code>"bar"</code>).
 * This is a handle-only method.  The field may or may not exist.
 *
 * @param name the given name
 * @return the field with the specified name in this type
 */
IField getField(String name);
/**
 * Returns the fields declared by this type or javascript file.
 *
 * @exception JavaScriptModelException if this element does not exist or if an
 *		exception occurs while accessing its corresponding resource.
 * @return the fields declared by this type or file
 */
IField[] getFields() throws JavaScriptModelException;

/**
 * Returns the method with the specified name and parameter types
 * in this type (for example, <code>"foo", {"I", "QString;"}</code>).
 * To get the handle for a constructor, the name specified must be the
 * simple name of the enclosing type.
 * This is a handle-only method.  The method may or may not be present.
 * <p>
 * The type signatures may be either unresolved (for source types)
 * or resolved (for binary types), and either basic (for basic types)
 * or rich (for parameterized types). See {@link Signature} for details.
 * </p>
 *
 * @param name the given name
 * @param parameterTypeSignatures the given parameter types
 * @return the method with the specified name and parameter types in this type
 */
IFunction getFunction(String name, String[] parameterTypeSignatures);

/**
 * Returns the methods and constructors declared by this type or file.
 *
 * @exception JavaScriptModelException if this element does not exist or if an
 *		exception occurs while accessing its corresponding resource.
 * @return the methods and constructors declared by this type
 */
IFunction[] getFunctions() throws JavaScriptModelException;

/**
 * Returns the type with the specified name
 *
 * @exception JavaScriptModelException if this element does not exist or if an
 *		exception occurs while accessing its corresponding resource.
 * @return the type with the specified name in this file
 */
IType getType(String name);

}
