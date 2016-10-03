/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.wst.jsdt.libraries;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.wst.jsdt.core.IAccessRule;
import org.eclipse.wst.jsdt.core.IIncludePathAttribute;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJsGlobalScopeContainer;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.compiler.libraries.LibraryLocation;

/**
 * <p>
 * Container for generic client-side scripting libraries. This container must
 * support all offered versions to avoid duplicates being added into a
 * project's include path.
 * </p>
 * <p>
 * Path format:
 * <code>org.eclipse.wst.jsdt.launching.baseBrowserLibrary/StandardBrowser/(html5|html4)?</code>
 * </p>
 */
public class BasicBrowserLibraryContainer implements IJsGlobalScopeContainer {
	private static final Object HTML5 = "html5"; //$NON-NLS-1$

	IPath fPath = Path.ROOT;

	/**
	 * @param containerPath
	 */
	BasicBrowserLibraryContainer(IPath containerPath) {
		super();
		fPath = containerPath;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.wst.jsdt.core.IJsGlobalScopeContainer#getDescription()
	 */
	public String getDescription() {
		if (isHTML5())
			return Messages.BasicBrowserLibraryJsGlobalScopeContainerInitializer_HTML5Browser;
		return Messages.BasicBrowserLibraryJsGlobalScopeContainerInitializer_ECMA3Browser;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.wst.jsdt.core.IJsGlobalScopeContainer#getIncludepathEntries
	 * ()
	 */
	public IIncludePathEntry[] getIncludepathEntries() {
		LibraryLocation baseLocation = BasicBrowserLibraryJsGlobalScopeContainerInitializer.BasicLibLocation.getInstance();
		char[][] filesInLibs = null;
		if (isHTML5()) {
			filesInLibs = BasicBrowserLibraryJsGlobalScopeContainerInitializer.LIBRARY_FILE_NAME5;
		}
		else {
			filesInLibs = BasicBrowserLibraryJsGlobalScopeContainerInitializer.LIBRARY_FILE_NAME4;
		}
		IIncludePathEntry[] entries = new IIncludePathEntry[filesInLibs.length];
		for (int i = 0; i < entries.length; i++) {
			IPath libraryLocation = new Path(baseLocation.getLibraryPath(filesInLibs[i]));
			entries[i] = JavaScriptCore.newLibraryEntry(libraryLocation.makeAbsolute(), null, null, new IAccessRule[0], new IIncludePathAttribute[0], false);
		}
		return entries;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.wst.jsdt.core.IJsGlobalScopeContainer#getKind()
	 */
	public int getKind() {
		return IJsGlobalScopeContainer.K_SYSTEM;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.wst.jsdt.core.IJsGlobalScopeContainer#getPath()
	 */
	public IPath getPath() {
		return fPath;
	}

	private boolean isHTML5() {
		return !fPath.isEmpty() && fPath.segmentCount() > 1 && HTML5.equals(fPath.lastSegment());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.wst.jsdt.core.IJsGlobalScopeContainer#resolvedLibraryImport
	 * (java.lang.String)
	 */
	public String[] resolvedLibraryImport(String a) {
		return new String[0];
	}

}
