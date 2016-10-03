/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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
 * A source range defines an element's source coordinates relative to
 * its source buffer.
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 *  
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public interface ISourceRange {

/**
 * Returns the number of characters of the source code for this element,
 * relative to the source buffer in which this element is contained.
 *
 * @return the number of characters of the source code for this element,
 * relative to the source buffer in which this element is contained
 */
int getLength();
/**
 * Returns the 0-based index of the first character of the source code for this element,
 * relative to the source buffer in which this element is contained.
 *
 * @return the 0-based index of the first character of the source code for this element,
 * relative to the source buffer in which this element is contained
 */
int getOffset();
}
