/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.packageview;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;

public class LibraryContainer extends PackageFragmentRootContainer {

	public LibraryContainer(IJavaScriptProject project) {
		super(project);
	}

	public boolean equals(Object obj) {
		if (obj instanceof LibraryContainer) {
			LibraryContainer other = (LibraryContainer)obj;
			return getJavaProject().equals(other.getJavaProject());
		}
		return false;
	}

	public int hashCode() {
		return getJavaProject().hashCode();
	}

	public IAdaptable[] getChildren() {
		return getPackageFragmentRoots();
	}	


	public ImageDescriptor getImageDescriptor() {
		return JavaPluginImages.DESC_OBJS_LIBRARY;
	}

	public String getLabel() {
		return PackagesMessages.LibraryContainer_name;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.ui.packageview.PackageFragmentRootContainer#getPackageFragmentRoots()
	 */
	public IPackageFragmentRoot[] getPackageFragmentRoots() {
		List list= new ArrayList();
		try {
			IPackageFragmentRoot[] roots= getJavaProject().getPackageFragmentRoots();
			for (int i= 0; i < roots.length; i++) {
				IPackageFragmentRoot root= roots[i];
				int classpathEntryKind= root.getRawIncludepathEntry().getEntryKind();
				if (classpathEntryKind == IIncludePathEntry.CPE_LIBRARY || classpathEntryKind == IIncludePathEntry.CPE_VARIABLE) {
					list.add(root);
				}
			}
		} catch (JavaScriptModelException e) {
			// fall through
		}
		return (IPackageFragmentRoot[]) list.toArray(new IPackageFragmentRoot[list.size()]);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return getJavaProject().getDisplayName();
	}
}
