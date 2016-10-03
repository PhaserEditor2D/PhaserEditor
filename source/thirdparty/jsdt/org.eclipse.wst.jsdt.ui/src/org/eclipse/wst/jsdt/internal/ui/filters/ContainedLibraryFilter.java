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
package org.eclipse.wst.jsdt.internal.ui.filters;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;


/**
 * The LibraryFilter is a filter used to determine whether
 * a Java internal library is shown
 */
public class ContainedLibraryFilter extends ViewerFilter {
	
	/* (non-Javadoc)
	 * Method declared on ViewerFilter.
	 */
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if (element instanceof IPackageFragmentRoot) {
			IPackageFragmentRoot root= (IPackageFragmentRoot)element;
			if (root.isArchive()) {
				// don't filter out JARs contained in the project itself
				IResource resource= root.getResource();
				if (resource != null) {
					IProject jarProject= resource.getProject();
					IProject container= root.getJavaScriptProject().getProject();
					return !container.equals(jarProject);
				}
			}
		}
		return true;
	}
}
