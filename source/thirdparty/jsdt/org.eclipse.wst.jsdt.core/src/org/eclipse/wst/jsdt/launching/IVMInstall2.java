/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.wst.jsdt.launching;


/**
 * Optional extensions that may be implemented by an
 * {@link org.eclipse.wst.jsdt.launching.IVMInstall}.
 * <p>
 * When an <code>IVMInstall</code> implements this interface,
 * clients must call <code>getVMArgs()</code> in place of 
 * <code>getVMArguments()</code> and <code>setVMArgs(String)</code> in place of
 * <code>setVMArguments(String[])</code>. This avoids the problem noted
 * in bug 73493.
 * </p>
 * <p>
 * Additionally, this interface optionally provides the JavaScript version
 * associated with a VM install. 
 * </p>
 * <p>
 * Clients that implement {@link org.eclipse.wst.jsdt.launching.IVMInstall} may additionally
 * implement this interface. However, it is strongly recommended that clients subclass
 * {@link org.eclipse.wst.jsdt.launching.AbstractVMInstall} instead, which already implements
 * this interface, and will insulate clients from additional API additions in the future.
 * </p>
 * 
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public interface IVMInstall2 {
	
	/**
	 * Returns VM arguments to be used with this vm install whenever this
	 * VM is launched as a raw string, or <code>null</code> if none.
	 * 
	 * @return VM arguments to be used with this vm install whenever this
	 * VM is launched as a raw string, or <code>null</code> if none
	 */	
	public String getVMArgs();
	
	/**
	 * Sets VM arguments to be used with this vm install whenever this
	 * VM is launched as a raw string, possibly <code>null</code>.
	 * 
	 * @param vmArgs VM arguments to be used with this vm install whenever this
	 * VM is launched as a raw string, possibly <code>null</code>
	 */
	public void setVMArgs(String vmArgs);
    
    /**
     * Returns a string representing the <code>java.version</code> system property
     * of this VM install, or <code>null</code> if unknown.
     * 
     * @return a string representing the <code>java.version</code> system property
     * of this VM install, or <code>null</code> if unknown.
     */
    public String getJavaVersion();
}
