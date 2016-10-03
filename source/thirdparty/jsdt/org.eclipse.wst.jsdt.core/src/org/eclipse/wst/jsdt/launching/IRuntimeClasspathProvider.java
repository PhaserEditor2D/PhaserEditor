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

/**
 * A includepath provider computes an unresolved includepath for a launch
 * configuration, and resolves includepath entries for a launch configuration.
 * A includepath provider is defined as an extension of type 
 * <code>org.eclipse.wst.jsdt.launching.includepathProvider</code>.
 * <p>
 * A provider is registered with an identifier that can be
 * referenced by a launch configuration. A includepath provider is consulted
 * to compute a includepath or source lookup path when a launch configuration
 * references a provider in one or both of the following attributes:
 * <ul>
 * <li><code>ATTR_CLASSPATH_PROVIDER</code></li>
 * <li><code>ATTR_SOURCE_PATH_PROVIDER</code></li>
 * </ul>
 * </p>
 * A provider extension is defined in <code>plugin.xml</code>.
 * Following is an example definition of a runtime includepath provider
 * extension.
 * <pre>
 * &lt;extension point="org.eclipse.wst.jsdt.launching.includepathProviders"&gt;
 *   &lt;includepathProvider 
 *      id="com.example.ExampleClasspathProvider"
 *      class="com.example.ExampleClasspathProviderImpl"
 *   &lt;/includepathProvider&gt;
 * &lt;/extension&gt;
 * </pre>
 * The attributes are specified as follows:
 * <ul>
 * <li><code>id</code> specifies a unique identifier for this extension. This 
 * 	identifier may be used to reference a provider on one of the launch
 *  configuration attributes mentioned above.</li>
 * <li><code>class</code> specifies the fully qualified name of the JavaScript class
 *   that implements <code>IRuntimeClasspathProvider</code>.</li>
 * </ul>
 * </p>
 * <p>
 * Clients may implement this interface.
 * </p>
 * 
 * 
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public interface IRuntimeClasspathProvider {
	
	/**
	 * Computes and returns an unresolved includepath for the given launch configuration.
	 * Variable and container entries are not resolved.
	 * 
	 * @param configuration launch configuration
	 * @return unresolved path
	 * @exception CoreException if unable to compute a path
	 */
	public IRuntimeClasspathEntry[] computeUnresolvedClasspath(ILaunchConfiguration configuration) throws CoreException;
	
	/**
	 * Returns the resolved path corresponding to the given path, in the context of the
	 * given launch configuration. Variable and container entries are resolved. The returned
	 * (resolved) path need not have the same number of entries as the given (unresolved)
	 * path.
	 * 
	 * @param entries entries to resolve
	 * @param configuration launch configuration context to resolve in
	 * @return resolved path
	 * @exception CoreException if unable to resolve a path
	 */
	public IRuntimeClasspathEntry[] resolveClasspath(IRuntimeClasspathEntry[] entries, ILaunchConfiguration configuration) throws CoreException;

}
