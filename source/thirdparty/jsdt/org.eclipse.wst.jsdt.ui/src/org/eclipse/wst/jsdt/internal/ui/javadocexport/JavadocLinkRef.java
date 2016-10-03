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

package org.eclipse.wst.jsdt.internal.ui.javadocexport;

import java.net.URL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths.BuildPathSupport;
import org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths.CPListElement;
import org.eclipse.wst.jsdt.ui.JavaScriptUI;


public class JavadocLinkRef {
	private final IJavaScriptProject fProject;
	private final IPath fContainerPath;
	private IIncludePathEntry fClasspathEntry;
	
	public JavadocLinkRef(IPath containerPath, IIncludePathEntry classpathEntry, IJavaScriptProject project) {
		fContainerPath= containerPath;
		fProject= project;
		fClasspathEntry= classpathEntry;
	}
	
	public JavadocLinkRef(IJavaScriptProject project) {
		this(null, null, project);
	}
	
	public boolean isProjectRef() {
		return fClasspathEntry == null;
	}
	
	public IPath getFullPath() {
		return isProjectRef() ? fProject.getPath() : fClasspathEntry.getPath();
	}
	
	public URL getURL() {
		if (isProjectRef()) {
			return JavaScriptUI.getProjectJSdocLocation(fProject);
		} else {
			return JavaScriptUI.getLibraryJSdocLocation(fClasspathEntry);
		}
	}
	
	public void setURL(URL url, IProgressMonitor monitor) throws CoreException {
		if (isProjectRef()) {
			JavaScriptUI.setProjectJSdocLocation(fProject, url);
		} else {
			CPListElement element= CPListElement.createFromExisting(fClasspathEntry, fProject);
			String location= url != null ? url.toExternalForm() : null;
			element.setAttribute(CPListElement.JAVADOC, location);
			String[] changedAttributes= { CPListElement.JAVADOC };
			BuildPathSupport.modifyClasspathEntry(null, element.getClasspathEntry(), changedAttributes, fProject, fContainerPath, monitor);
			fClasspathEntry= element.getClasspathEntry();
		}
	}
	
	public boolean equals(Object obj) {
		if (obj != null && obj.getClass().equals(getClass())) {
			JavadocLinkRef other= (JavadocLinkRef) obj;
			if (!fProject.equals(other.fProject) || isProjectRef() != other.isProjectRef()) {
				return false;
			}
			if (!isProjectRef()) {
				return !fClasspathEntry.equals(other.fClasspathEntry);
			}
		}
		return false;
	}
	
	public int hashCode() {
		if (isProjectRef()) {
			return fProject.hashCode();
		} else {
			return fProject.hashCode() + fClasspathEntry.hashCode();
		}

	}
}
