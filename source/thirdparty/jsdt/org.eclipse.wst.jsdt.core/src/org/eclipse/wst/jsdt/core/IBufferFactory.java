/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
 * A factory that creates <code>IBuffer</code>s for openables.
 * <p>
 * This interface may be implemented by clients.
 * </p>
 * @deprecated Use {@link WorkingCopyOwner} instead
 *  
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public interface IBufferFactory {

	/**
	 * Creates a buffer for the given owner.
	 * The new buffer will be initialized with the contents of the owner
	 * if and only if it was not already initialized by the factory (a buffer is uninitialized if
	 * its content is <code>null</code>).
	 *
	 * @param owner the owner of the buffer
	 * @return the newly created buffer
	 * @see IBuffer
	 */
	IBuffer createBuffer(IOpenable owner);
}

