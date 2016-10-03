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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.w3c.dom.Element;

/**
 * Enhancements to <code>IRuntimeClasspathEntry</code> to support
 * extensible runtime includepath entries. Contributed runtime includepath
 * entries have a type of <code>OTHER</code>, and are contributed to
 * the <code>runtimeClasspathEntries</code> extension point.
 * <p>
 * Clients are not intended to implement this interface, as new types
 * of runtime includepath entries are only intended to be contributed
 * by the JavaScript debugger.
 * </p>
 *   
 * @see org.eclipse.wst.jsdt.launching.IRuntimeClasspathEntry
 * 
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public interface IRuntimeClasspathEntry2 extends IRuntimeClasspathEntry {
	
	/**
	 * Initializes this runtime includepath entry from the given memento.
	 * 
	 * @param memento memento created by a includepath entry of the same type
	 * @throws CoreException if unable to initialize from the given memento
	 */
	public void initializeFrom(Element memento) throws CoreException;
	
	/**
	 * Returns the unique identifier of the extension that contributed
	 * this includepath entry type, or <code>null</code> if this includepath
	 * entry type was not contributed.
	 * 
	 * @return the unique identifier of the extension that contributed
	 *  this includepath entry type, or <code>null</code> if this includepath
	 *  entry type was not contributed
	 */
	public String getTypeId();
	
	/**
	 * Returns whether this includepath entry is composed of other entries.
	 * 
	 * @return whether this includepath entry is composed of other entries
	 */
	public boolean isComposite();
	
	/**
	 * Returns the includepath entries this entry is composed of, or an
	 * empty collection if this entry is not a composite entry.
	 * 
	 * @param configuration the context (launch configuration) in which
	 *  this runtime includepath entry is being queried for contained
	 * 	entries, possibly <code>null</code> 
	 * @return the includepath entries this entry is composed of, or an
	 * empty collection if this entry is not a composite entry
	 * @throws CoreException if unable to retrieve contained entries
	 */
	public IRuntimeClasspathEntry[] getRuntimeClasspathEntries(ILaunchConfiguration configuration) throws CoreException;
	
	/**
	 * Returns a human readable name for this includepath entry.
	 * 
	 * @return a human readable name for this includepath entry
	 */
	public String getName();
}
