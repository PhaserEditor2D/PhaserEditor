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
 * Represents a field declared in a type or a var declared at the file scope.
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 *  
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public interface IField extends IMember {
/**
 * Returns the simple name of this field.
 * @return the simple name of this field.
 */
String getElementName();
/**
 * Returns the binding key for this field. A binding key is a key that uniquely
 * identifies this field. It allows access to generic info for parameterized
 * fields.
 *
 * @return the binding key for this field
 * @see org.eclipse.wst.jsdt.core.dom.IBinding#getKey()
 * @see BindingKey
 */
String getKey();
/**
 * Returns the type signature of this field. 
 * <p>
 * The type signature may be either unresolved (for source types)
 * or resolved (for binary types), and either basic (for basic types)
 * or rich (for parameterized types). See {@link Signature} for details.
 * </p>
 *
 * @return the type signature of this field
 * @exception JavaScriptModelException if this element does not exist or if an
 *      exception occurs while accessing its corresponding resource
 * @see Signature
 */
String getTypeSignature() throws JavaScriptModelException;

/**
 * Returns whether this field represents a resolved field.
 * If a field is resoved, its key contains resolved information.
 *
 * @return whether this field represents a resolved field.
 */
boolean isResolved();

}
