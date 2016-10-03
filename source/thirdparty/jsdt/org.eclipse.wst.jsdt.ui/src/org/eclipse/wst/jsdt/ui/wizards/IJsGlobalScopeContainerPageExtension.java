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
import org.eclipse.wst.jsdt.core.IJavaScriptProject;

/**
 * Classpath container pages that implement <code>IJsGlobalScopeContainerPage</code> can 
 * optionally implement <code>IJsGlobalScopeContainerPageExtension</code> to get additional
 * information about the context when the page is opened. Method <code>initialize()</code>
 * is called before  <code>IJsGlobalScopeContainerPage.setSelection</code>.
 *
 *
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves. */
public interface IJsGlobalScopeContainerPageExtension {
	
	/**
	 * Method <code>initialize()</code> is called before  <code>IJsGlobalScopeContainerPage.setSelection</code>
	 * to give additional information about the context the classpath container entry is configured in. This information
	 * only reflects the underlying dialogs current selection state. The user still can make changes after the
	 * the classpath container pages has been closed or decide to cancel the operation.
	 * @param project The project the new or modified entry is added to. The project does not have to exist. 
	 * Project can be <code>null</code>.
	 * @param currentEntries The class path entries currently selected to be set as the projects classpath. This can also
	 * include the entry to be edited.
	 */
	public void initialize(IJavaScriptProject project, IIncludePathEntry[] currentEntries);

}
