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

import org.eclipse.wst.jsdt.core.JavaScriptCore;

 
 
/**
 * A VM install changed listener is notified when
 * the workspace default VM install changes, or when an attribute of
 * a specific VM install changes.
 * Listeners register with <code>JavaRuntime</code>.
 * <p>
 * Clients may implement this interface.
 * </p>
 * 
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public interface IVMInstallChangedListener {
	
	/**
	 * Property constant indicating the library locations associated
	 * with a VM install have changed.
	 */
	public static final String PROPERTY_LIBRARY_LOCATIONS = JavaScriptCore.PLUGIN_ID + ".PROPERTY_LIBRARY_LOCATIONS"; //$NON-NLS-1$

	/**
	 * Property constant indicating the name associated
	 * with a VM install has changed.
	 */
	public static final String PROPERTY_NAME = JavaScriptCore.PLUGIN_ID + ".PROPERTY_NAME"; //$NON-NLS-1$
	
	/**
	 * Property constant indicating the install location of
	 * a VM install has changed.
	 */
	public static final String PROPERTY_INSTALL_LOCATION =JavaScriptCore.PLUGIN_ID + ".PROPERTY_INSTALL_LOCATION";	 //$NON-NLS-1$
			
	/**
	 * Property constant indicating the jsdoc location associated
	 * with a VM install has changed.
	 */
	public static final String PROPERTY_JAVADOC_LOCATION = JavaScriptCore.PLUGIN_ID+ ".PROPERTY_JAVADOC_LOCATION"; //$NON-NLS-1$

	/**
	 * Property constant indicating the VM arguments associated
	 * with a VM install has changed.
     * 
     *  
	 */
	public static final String PROPERTY_VM_ARGUMENTS = JavaScriptCore.PLUGIN_ID + ".PROPERTY_VM_ARGUMENTS"; //$NON-NLS-1$

	/**
	 * Notification that the workspace default VM install
	 * has changed.
	 * 
	 * @param previous the VM install that was previously assigned
	 * 	to the workspace, possibly <code>null</code>
	 * @param current the VM install that is currently assigned to the
	 * 	workspace, possibly <code>null</code>
	 */
	public void defaultVMInstallChanged(IVMInstall previous, IVMInstall current);
	
	/**
	 * Notification that a property of a VM install has changed.
	 * 
	 * @param event event describing the change. The VM that has changed
	 * 	is the source object associated with the event.
	 */
	public void vmChanged(PropertyChangeEvent event);	
	
	/**
	 * Notification that a VM has been created.
	 * 
	 * @param vm the vm that has been created
	 */
	public void vmAdded(IVMInstall vm);		
	
	/**
	 * Notification that a VM has been disposed.
	 * 
	 * @param vm the vm that has been disposed
	 */
	public void vmRemoved(IVMInstall vm);			
	
}
