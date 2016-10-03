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
package org.eclipse.wst.jsdt.core;


/**
 * Represents an entire non-editable JavaScript file.
 * non-editable JavaScript file elements need to be opened before they can be navigated.
 * If a  file cannot be parsed, its structure remains unknown. Use
 * <code>IJavaScriptElement.isStructureKnown</code> to determine whether this is the
 * case.
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 *  
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
*/

public interface IClassFile extends ITypeRoot {

/**
 * Returns the bytes contained in this  file.
 *
 * @return the bytes contained in this  file
 *
 * @exception JavaScriptModelException if this element does not exist or if an
 *      exception occurs while accessing its corresponding resource
 */
byte[] getBytes() throws JavaScriptModelException;

/**
 * Returns the first type contained in this  file.
 * This is a handle-only method. The type may or may not exist.
 *
 * @return the type contained in this file
 *
 */
IType getType();
public IType[] getTypes() throws JavaScriptModelException ;


/* 
 * Returns whether this type is edit. This is not guaranteed to be
 * instantaneous, as it may require parsing the underlying file.
 *
 * @return <code>true</code> if the  file represents a class.
 *
 * @exception JavaScriptModelException if this element does not exist or if an
 *      exception occurs while accessing its corresponding resource
 */
boolean isClass() throws JavaScriptModelException;
}
