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
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;

/**
 * Filters non-shared projects and Java projects. Non-shared projects are
 * projects that are not controlled by a team provider.
 * 
 * 
 */
public class NonSharedProjectFilter extends ViewerFilter {

	/*
	 * @see ViewerFilter
	 */
	public boolean select(Viewer viewer, Object parent, Object element) {
		if (element instanceof IProject)
			return isSharedProject((IProject)element);
		
		if (element instanceof IJavaScriptProject)
			return isSharedProject(((IJavaScriptProject)element).getProject());

		return true;
	}
	
	private boolean isSharedProject(IProject project) {
		return !project.isAccessible() || RepositoryProvider.isShared(project);
	}
}
