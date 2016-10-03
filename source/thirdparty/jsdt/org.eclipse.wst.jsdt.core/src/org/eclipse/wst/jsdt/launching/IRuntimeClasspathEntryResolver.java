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
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;

/**
 * Resolves variable and/or container runtime includepath entries in
 * the context of a launch configuration or JavaScript project. A resolver can be declared
 * as an extension (<code>org.eclipse.wst.jsdt.launching.runtimeClasspathEntryResolver</code>),
 * or be registered with the <code>JavaRuntime</code> programmatically.
 * <p>
 * A resolver is registered for a specific includepath
 * <code>VARIABLE</code> and/or <code>CONTAINER</code>. A resolver is
 * consulted when a runtime includepath entry is needs to be resolved.
 * </p>
 * A resolver extension is defined in <code>plugin.xml</code>.
 * Following is an example definition of a runtime includepath entry
 * resolver extension.
 * <pre>
 * &lt;extension point="org.eclipse.wst.jsdt.launching.runtimeClasspathEntryResolvers"&gt;
 *   &lt;runtimeClasspathEntryResolver 
 *      id="com.example.ExampleResolver"
 *      class="com.example.ExampleResolverImpl"
 *      variable="VAR_NAME"
 *      container="CONTAINER_ID"
 *   &lt;/runtimeClasspathEntryResolver&gt;
 * &lt;/extension&gt;
 * </pre>
 * The attributes are specified as follows:
 * <ul>
 * <li><code>id</code> specifies a unique identifier for this extension.</li>
 * <li><code>class</code> specifies the fully qualified name of the JavaScript class
 *   that implements <code>IRuntimeClasspathEntryResolver</code>.</li>
 * <li><code>variable</code> name of the includepath variable this resolver
 * 	is registered for.</li>
 * <li><code>container</code> identifier of the includepath container this
 * 	resolver is registered for.</li>
 * </ul>
 * At least one of <code>variable</code> or <code>container</code> must be
 * specified.
 * </p>
 * <p>
 * Clients may implement this interface.
 * </p>
 * 
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public interface IRuntimeClasspathEntryResolver {
	
	/**
	 * Returns resolved runtime includepath entries for the given runtime includepath entry,
	 * in the context of the given launch configuration.
	 * 
	 * @param entry runtime includepath entry to resolve, of type
	 * 	<code>VARIABLE</code> or <code>CONTAINTER</code>
	 * @param configuration the context in which the runtime includepath entry
	 * 	needs to be resolved
	 * @return resolved entries (zero or more)
	 * @exception CoreException if unable to resolve the entry  
	 */
	public IRuntimeClasspathEntry[] resolveRuntimeClasspathEntry(IRuntimeClasspathEntry entry, ILaunchConfiguration configuration) throws CoreException;
	
	/**
	 * Returns resolved runtime includepath entries for the given runtime includepath entry,
	 * in the context of the given JavaScript project.
	 * 
	 * @param entry runtime includepath entry to resolve, of type
	 * 	<code>VARIABLE</code> or <code>CONTAINTER</code>
	 * @param project context in which the runtime includepath entry
	 * 	needs to be resolved
	 * @return resolved entries (zero or more)
	 * @exception CoreException if unable to resolve the entry  
	 */
	public IRuntimeClasspathEntry[] resolveRuntimeClasspathEntry(IRuntimeClasspathEntry entry, IJavaScriptProject project) throws CoreException;	
	
	/**
	 * Returns a VM install associated with the given includepath entry,
	 * or <code>null</code> if none.
	 * 
	 * @param entry includepath entry
	 * @return vm install associated with entry or <code>null</code> if none
	 * @exception CoreException if unable to resolve a VM
	 */
	public IVMInstall resolveVMInstall(IIncludePathEntry entry) throws CoreException;
}
