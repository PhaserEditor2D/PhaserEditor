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

import java.io.File;
import java.net.URL;

/**
 * Represents a particular installation of a VM. A VM instance holds all parameters
 * specific to a VM installation. Unlike VM types, VM instances can be created and
 * configured dynamically at run-time. This is typically done by the user 
 * interactively in the UI.
 * <p>
 * A VM install is responsible for creating VM runners to launch a JavaScript program
 * in a specific mode.
 * </p>
 * <p>
 * This interface is intended to be implemented by clients that contribute
 * to the <code>"org.eclipse.wst.jsdt.launching.vmInstallTypes"</code> extension point.
 * Rather than implementing this interface directly, it is strongly recommended that
 * clients subclass {@link org.eclipse.wst.jsdt.launching.AbstractVMInstall} to be insulated
 * from potential API additions. In 3.1, a new optional interface has been added for
 * implementors of this interface - {@link org.eclipse.wst.jsdt.launching.IVMInstall2}.
 * The new interface is implemented by {@link org.eclipse.wst.jsdt.launching.AbstractVMInstall}.
 * </p>
 * @see org.eclipse.wst.jsdt.launching.IVMInstall2
 * 
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public interface IVMInstall {
	/**
	 * Returns a VM runner that runs this installed VM in the given mode.
	 * 
	 * @param mode the mode the VM should be launched in; one of the constants
	 *   declared in <code>org.eclipse.debug.core.ILaunchManager</code>
	 * @return 	a VMRunner for a given mode May return <code>null</code> if the given mode
	 * 			is not supported by this VM.
	 * @see org.eclipse.debug.core.ILaunchManager
	 */
	IVMRunner getVMRunner(String mode);
	/**
	 * Returns the id for this VM. VM IDs are unique within the VMs 
	 * of a given VM type. The VM id is not intended to be presented to users.
	 * 
	 * @return the VM identifier. Must not return <code>null</code>.
	 */
	String getId();
	/**
	 * Returns the display name of this VM.
	 * The VM name is intended to be presented to users.
	 * 
	 * @return the display name of this VM. May return <code>null</code>.
	 */
	String getName();
	/**
	 * Sets the display name of this VM.
	 * The VM name is intended to be presented to users.
	 * 
	 * @param name the display name of this VM
	 */
	void setName(String name);
	/**
	 * Returns the root directory of the install location of this VM.
	 * 
	 * @return the root directory of this VM installation. May
	 * 			return <code>null</code>.
	 */
	File getInstallLocation();
	/**
	 * Sets the root directory of the install location of this VM.
	 * 
	 * @param installLocation the root directory of this VM installation
	 */
	void setInstallLocation(File installLocation);
		
	/**
	 * Returns the VM type of this VM.
	 * 
	 * @return the VM type that created this IVMInstall instance
	 */
	IVMInstallType getVMInstallType();
	
	/**
	 * Returns the library locations of this IVMInstall. Generally,
	 * clients should use <code>JavaRuntime.getLibraryLocations(IVMInstall)</code>
	 * to determine the libraries associated with this VM install.
	 * 
	 * @see IVMInstall#setLibraryLocations(LibraryLocation[])
	 * @return 	The library locations of this IVMInstall.
	 * 			Returns <code>null</code> to indicate that this VM install uses
	 * 			the default library locations associated with this VM's install type.
	 *  
	 */
	LibraryLocation[] getLibraryLocations();	
	
	/**
	 * Sets the library locations of this IVMInstall.
	 * @param	locations The <code>LibraryLocation</code>s to associate
	 * 			with this IVMInstall.
	 * 			May be <code>null</code> to indicate that this VM install uses
	 * 			the default library locations associated with this VM's install type.
	 *  
	 */
	void setLibraryLocations(LibraryLocation[] locations);	
	
	/**
	 * Sets the jsdoc location associated with this VM install.
	 * 
	 * @param url a url pointing to the jsdoc location associated with
	 * 	this VM install
	 *  
	 */
	public void setJavadocLocation(URL url);
	
	/**
	 * Returns the jsdoc location associated with this VM install.
	 * 
	 * @return a url pointing to the jsdoc location associated with
	 * 	this VM install, or <code>null</code> if none
	 *  
	 */
	public URL getJavadocLocation();
	
	/**
	 * Returns VM arguments to be used with this vm install whenever this
	 * VM is launched as they should be passed to the command line, or
	 * <code>null</code> if none.
	 * 
	 * @return VM arguments to be used with this vm install whenever this
	 * VM is launched as they should be passed to the command line, or
	 * <code>null</code> if none
	 *  
	 */
	public String[] getVMArguments();
	
	/**
	 * Sets VM arguments to be used with this vm install whenever this
	 * VM is launched, possibly <code>null</code>. This is equivalent
	 * to <code>setVMArgs(String)</code> with whitespace character delimited
	 * arguments.  
	 * 
	 * @param vmArgs VM arguments to be used with this vm install whenever this
	 * VM is launched, possibly <code>null</code>
	 *  
	 * @deprecated if possible, clients should use setVMArgs(String) on
	 *  {@link IVMInstall2} when possible
	 */
	public void setVMArguments(String[] vmArgs);
	    
}
