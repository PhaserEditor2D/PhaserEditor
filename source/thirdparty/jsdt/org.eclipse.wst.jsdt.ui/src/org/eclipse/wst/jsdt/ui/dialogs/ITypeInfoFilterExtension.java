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
package org.eclipse.wst.jsdt.ui.dialogs;

/**
 * A filter to select {@link ITypeInfoRequestor} objects.
 * <p>
 * The interface should be implemented by clients wishing to provide special
 * filtering to the type selection dialog.
 * </p>
 * 
 *
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 */
public interface ITypeInfoFilterExtension {
	
	/**
	 * Returns whether the given type makes it into the list or
	 * not.
	 * 
	 * @param typeInfoRequestor the <code>ITypeInfoRequestor</code> to 
	 *  used to access data for the type under inspection
	 * 
	 * @return whether the type is selected or not
	 */
	public boolean select(ITypeInfoRequestor typeInfoRequestor);
	
}
