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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.wst.jsdt.core.JsGlobalScopeContainerInitializer;
import org.eclipse.wst.jsdt.core.IJsGlobalScopeContainer;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.UnimplementedException;
import org.eclipse.wst.jsdt.core.compiler.libraries.LibraryLocation;

/**
 *
 */
public class UserLibraryJsGlobalScopeContainerInitializer extends JsGlobalScopeContainerInitializer {

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.JsGlobalScopeContainerInitializer#initialize(org.eclipse.core.runtime.IPath, org.eclipse.wst.jsdt.core.IJavaScriptProject)
	 */
	public void initialize(IPath containerPath, IJavaScriptProject project) throws CoreException {
		if (isUserLibraryContainer(containerPath)) {
			String userLibName= containerPath.segment(1);

			UserLibrary entries= UserLibraryManager.getUserLibrary(userLibName);
			if (entries != null) {
				UserLibraryJsGlobalScopeContainer container= new UserLibraryJsGlobalScopeContainer(userLibName);
				JavaScriptCore.setJsGlobalScopeContainer(containerPath, new IJavaScriptProject[] { project }, 	new IJsGlobalScopeContainer[] { container }, null);
			}
		}
	}

	private boolean isUserLibraryContainer(IPath path) {
		return path != null && path.segmentCount() == 2 && JavaScriptCore.USER_LIBRARY_CONTAINER_ID.equals(path.segment(0));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.JsGlobalScopeContainerInitializer#canUpdateJsGlobalScopeContainer(org.eclipse.core.runtime.IPath, org.eclipse.wst.jsdt.core.IJavaScriptProject)
	 */
	public boolean canUpdateJsGlobalScopeContainer(IPath containerPath, IJavaScriptProject project) {
		return isUserLibraryContainer(containerPath);
	}

	/**
	 * @see org.eclipse.wst.jsdt.core.JsGlobalScopeContainerInitializer#requestJsGlobalScopeContainerUpdate(org.eclipse.core.runtime.IPath, org.eclipse.wst.jsdt.core.IJavaScriptProject, org.eclipse.wst.jsdt.core.IJsGlobalScopeContainer)
	 */
	public void requestJsGlobalScopeContainerUpdate(IPath containerPath, IJavaScriptProject project, IJsGlobalScopeContainer containerSuggestion) throws CoreException {
		if (isUserLibraryContainer(containerPath)) {
			String name= containerPath.segment(1);
			if (containerSuggestion != null) {
				UserLibrary library= new UserLibrary(containerSuggestion.getIncludepathEntries(), containerSuggestion.getKind() == IJsGlobalScopeContainer.K_SYSTEM);
				UserLibraryManager.setUserLibrary(name, library, null); // should use a real progress monitor
			} else {
				UserLibraryManager.setUserLibrary(name, null, null); // should use a real progress monitor
			}
		}
	}

	/**
	 * @see org.eclipse.wst.jsdt.core.JsGlobalScopeContainerInitializer#getDescription(org.eclipse.core.runtime.IPath, org.eclipse.wst.jsdt.core.IJavaScriptProject)
	 */
	public String getDescription(IPath containerPath, IJavaScriptProject project) {
		if (isUserLibraryContainer(containerPath)) {
			return containerPath.segment(1);
		}
		return super.getDescription(containerPath, project);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.JsGlobalScopeContainerInitializer#getComparisonID(org.eclipse.core.runtime.IPath, org.eclipse.wst.jsdt.core.IJavaScriptProject)
	 */
	public Object getComparisonID(IPath containerPath, IJavaScriptProject project) {
		return containerPath;
	}

	public LibraryLocation getLibraryLocation() {
throw new UnimplementedException();
	}
}
