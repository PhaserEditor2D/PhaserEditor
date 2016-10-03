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
 * An interfaces to give access to the type presented in type
 * selection dialogs like the open type dialog.
 * <p>
 * Please note that <code>ITypeInfoRequestor</code> objects <strong>don't
 * </strong> have value semantic. The state of the object might change over 
 * time especially since objects are reused for different call backs. 
 * </p>
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 *
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 */
public interface ITypeInfoRequestor {
	
	/**
	 * Returns the type's modifiers. The modifiers can be 
	 * inspected using the class {@link org.eclipse.wst.jsdt.core.Flags}.
	 * 
	 * @return the type's modifiers
	 */
	public int getModifiers();
	
	/**
	 * Returns the type name.
	 * 
	 * @return the info's type name.
	 */
	public String getTypeName();
	
	/**
	 * Returns the package name.
	 * 
	 * @return the info's package name.
	 */ 
	public String getPackageName();

	/**
	 * Returns a dot separated string of the enclosing types or an 
	 * empty string if the type is a top level type.
	 * 
	 * @return a dot separated string of the enclosing types
	 */
	public String getEnclosingName();
}
