/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
 * Represents an import declaration in JavaScript unit.
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 *  
 * <b>This Interface only applies to ECMAScript 4 which is not yet supported</b>
 *  
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public interface IImportDeclaration extends IJavaScriptElement, ISourceReference, ISourceManipulation {
/**
 * Returns the name that has been imported.
 *
 * @return the name that has been imported
 */
String getElementName();
/**
 * Returns the modifier flags for this import. The flags can be examined using class
 * <code>Flags</code>. Only the static flag is meaningful for import declarations.
 *
 * @return the modifier flags for this import
 * @exception JavaScriptModelException if this element does not exist or if an
 *      exception occurs while accessing its corresponding resource.
 * @see Flags
 */
int getFlags() throws JavaScriptModelException;

/*
 * Returns whether the import is on-demand. An import is on-demand if it ends
 * with <code>".*"</code>.
 * @return true if the import is on-demand, false otherwise
 */
boolean isOnDemand();
}
