/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
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
 * {@link org.eclipse.wst.jsdt.launching.IVMInstall}, providing access to
 * a JRE's system properties.
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
public interface IVMInstall3 {

	/**
	 * Evaluates the specified system properties in this VM, returning the result
	 * as a map of property names to property values.
	 * 
	 * @param properties the property names to evaluate, for example <code>{"user.home"}</code>
	 * @param monitor progress monitor or <code>null</code>
	 * @return map of system property names to associated property values
	 * @throws CoreException if an exception occurs evaluating the properties
	 *  
	 */
// ASDT never called
	//	public Map evaluateSystemProperties(String[] properties, IProgressMonitor monitor) throws CoreException;
}
