/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.core;

import org.eclipse.core.runtime.IPath;

/**
 * Interface of a includepath container.
 * A includepath container provides a way to indirectly reference a set of includepath entries through
 * a includepath entry of kind <code>CPE_CONTAINER</code>. Typically, a includepath container can
 * be used to describe a complex library composed of filess or projects, considering also
 * that containers can map to different set of entries on each project, in other words, several
 * projects can reference the same generic container path, but have each of them actually bound
 * to a different container object.
 * <p>
 * The set of entries associated with a includepath container may contain any of the following:
 * <ul>
 * <li> library entries (<code>CPE_LIBRARY</code>) </li>
 * <li> project entries (<code>CPE_PROJECT</code>) </li>
 * </ul>
 * In particular, a includepath container can neither reference further includepath containers or includepath variables.
 * <p>
 * Classpath container values are persisted locally to the workspace, but are not preserved from a
 * session to another. It is thus highly recommended to register a <code>JsGlobalScopeContainerInitializer</code>
 * for each referenced container (through the extension point "org.eclipse.wst.jsdt.core.JsGlobalScopeContainerInitializer").
 * <p>
 * @see IIncludePathEntry
 *  
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */

public interface IJsGlobalScopeContainer {

	/**
	 * Kind for a container mapping to an application library
	 */
	int K_APPLICATION = 1;

	/**
	 * Kind for a container mapping to a system library
	 */
	int K_SYSTEM = 2;

	/**
	 * Kind for a container mapping to a default system library, implicitly contributed by the runtime
	 */
	int K_DEFAULT_SYSTEM = 3;

	/**
	 * Answers the set of includepath entries this container is mapping to.
	 * <p>
	 * The set of entries associated with a includepath container may contain any of the following:
	 * <ul>
	 * <li> library entries (<code>CPE_LIBRARY</code>) </li>
	 * <li> project entries (<code>CPE_PROJECT</code>) </li>
	 * </ul>
	 * A includepath container can neither reference further includepath containers
	 * or includepath variables.
	 * </p>
	 * <p>
	 * This method is called by the JavaScript model when it needs to resolve this
	 * includepath container entry into a list of library and project entries.
	 * The method is typically called exactly once for a given JavaScript project,
	 * and the resulting list of entries cached internally by the JavaScript model.
	 * This method must not be called by other clients.
	 * <p>
	 * There are a wide variety of conditions under which this method may be
	 * invoked. To ensure that the implementation does not interfere with
	 * correct functioning of the JavaScript model, the implementation should use
	 * only the following JavaScript model APIs:
	 * <ul>
	 * <li>{@link JavaScriptCore#newLibraryEntry(IPath, IPath, IPath, boolean)} and variants</li>
	 * <li>{@link JavaScriptCore#newProjectEntry(IPath, boolean)} and variants</li>
	 * <li>{@link JavaScriptCore#create(org.eclipse.core.resources.IWorkspaceRoot)}</li>
	 * <li>{@link JavaScriptCore#create(org.eclipse.core.resources.IProject)}</li>
	 * <li>{@link IJavaScriptModel#getJavaScriptProjects()}</li>
	 * <li>{@link IJavaScriptProject#getRawIncludepath()}</li>
	 * <li>{@link IJavaScriptProject#readRawIncludepath()}</li>
	 * <li>{@link IJavaScriptProject#getOutputLocation()}</li>
	 * <li>{@link IJavaScriptProject#readOutputLocation()}</li>
	 * <li>JavaScript element operations marked as "handle-only"</li>
	 * </ul>
	 * The effects of using other JavaScript model APIs are unspecified.
	 * </p>
	 *
	 * @return IIncludePathEntry[] - the includepath entries this container represents
	 * @see IIncludePathEntry
	 */
    IIncludePathEntry[] getIncludepathEntries();

	/**
	 * Answers a readable description of this container
	 *
	 * @return String - a string description of the container
	 */
    String getDescription();

	/**
	 * Answers the kind of this container. Can be either:
	 * <ul>
	 * <li><code>K_APPLICATION</code> if this container maps to an application library</li>
	 * <li><code>K_SYSTEM</code> if this container maps to a system library</li>
	 * <li><code>K_DEFAULT_SYSTEM</code> if this container maps to a default system library (library
	 * 	implicitly contributed by the runtime).</li>
	 * </ul>
	 * Typically, system containers should be placed first on a build path.
	 * @return the kind of this container
	 */
    int getKind();

	/**
	 * Answers the container path identifying this container.
	 * A container path is formed by a first ID segment followed with extra segments, which
	 * can be used as additional hints for resolving to this container.
	 * <p>
	 * The container ID is also used to identify a<code>JsGlobalScopeContainerInitializer</code>
	 * registered on the extension point "org.eclipse.wst.jsdt.core.JsGlobalScopeContainerInitializer", which can
	 * be invoked if needing to resolve the container before it is explicitly set.
	 * <p>
	 * @return IPath - the container path that is associated with this container
	 */
    IPath getPath();

    /* allows mapping between HTML imports and a toolkits actual page imports.  Implementers
     * should ensure the validity of the imports before returning a value.
     */
    String[] resolvedLibraryImport(String a);
}

