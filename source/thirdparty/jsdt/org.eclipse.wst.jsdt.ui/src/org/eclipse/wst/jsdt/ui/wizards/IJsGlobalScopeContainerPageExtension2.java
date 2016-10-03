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
package org.eclipse.wst.jsdt.ui.wizards;

import org.eclipse.wst.jsdt.core.IIncludePathEntry;

/**
 * Classpath container pages that implement {@link IJsGlobalScopeContainerPage} can 
 * optionally implement {@link IJsGlobalScopeContainerPageExtension2} to return more
 * than one element when creating new containers. If implemented, the method {@link #getNewContainers()}
 * is used instead of the method {@link IJsGlobalScopeContainerPage#getSelection() } to get the
 * newly selected containers. {@link IJsGlobalScopeContainerPage#getSelection() } is still used
 * for edited elements.
 *
 *
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves. */
public interface IJsGlobalScopeContainerPageExtension2 {
	
	/**
	 * Method {@link #getNewContainers()} is called instead of {@link IJsGlobalScopeContainerPage#getSelection() }
	 * to get the newly added containers. {@link IJsGlobalScopeContainerPage#getSelection() } is still used
	 * to get the edited elements.
	 * @return the classpath entries created on the page. All returned entries must be on kind
	 * {@link IIncludePathEntry#CPE_CONTAINER}
	 */
	public IIncludePathEntry[] getNewContainers();

}
