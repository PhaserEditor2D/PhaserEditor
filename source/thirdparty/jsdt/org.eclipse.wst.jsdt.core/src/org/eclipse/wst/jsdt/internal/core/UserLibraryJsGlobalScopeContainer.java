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
package org.eclipse.wst.jsdt.internal.core;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.wst.jsdt.core.IJsGlobalScopeContainer;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.JavaScriptCore;

/**
 *
 */
public class UserLibraryJsGlobalScopeContainer implements IJsGlobalScopeContainer {

	private String name;

	public UserLibraryJsGlobalScopeContainer(String libName) {
		this.name= libName;
	}

	private UserLibrary getUserLibrary() {
		return UserLibraryManager.getUserLibrary(this.name);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.IJsGlobalScopeContainer#getClasspathEntries()
	 */
	/**
	 * @deprecated Use {@link #getIncludepathEntries()} instead
	 */
	public IIncludePathEntry[] getClasspathEntries() {
		return getIncludepathEntries();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.IJsGlobalScopeContainer#getClasspathEntries()
	 */
	public IIncludePathEntry[] getIncludepathEntries() {
		UserLibrary library= getUserLibrary();
		if (library != null) {
			return library.getEntries();
		}
		return new IIncludePathEntry[0];

	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.IJsGlobalScopeContainer#getDescription()
	 */
	public String getDescription() {
		return this.name;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.IJsGlobalScopeContainer#getKind()
	 */
	public int getKind() {
		UserLibrary library= getUserLibrary();
		if (library != null && library.isSystemLibrary()) {
			return K_SYSTEM;
		}
		return K_APPLICATION;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.IJsGlobalScopeContainer#getPath()
	 */
	public IPath getPath() {
		return new Path(JavaScriptCore.USER_LIBRARY_CONTAINER_ID).append(this.name);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.IJsGlobalScopeContainer#resolvedLibraryImport(java.lang.String)
	 */
	public String[] resolvedLibraryImport(String a) {
		return new String[] {a};
	}
}
