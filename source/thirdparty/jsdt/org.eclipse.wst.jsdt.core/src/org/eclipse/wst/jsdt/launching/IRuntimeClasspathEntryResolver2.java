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
package org.eclipse.wst.jsdt.launching;


import org.eclipse.wst.jsdt.core.IIncludePathEntry;

/**
 * Optional enhancements to {@link IRuntimeClasspathEntryResolver}.
 * <p>
 * Clients may implement this interface.
 * </p>
 * 
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public interface IRuntimeClasspathEntryResolver2 extends IRuntimeClasspathEntryResolver {
	
	/**
	 * Returns whether the given includepath entry references a VM install.
	 * 
	 * @param entry includepath entry
	 * @return whether the given includepath entry references a VM install
	 */
	public boolean isVMInstallReference(IIncludePathEntry entry);
}
