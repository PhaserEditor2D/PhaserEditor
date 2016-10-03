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
package org.eclipse.wst.jsdt.internal.ui.browsing;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;

class ProjectAndSourceFolderContentProvider extends JavaBrowsingContentProvider {

	ProjectAndSourceFolderContentProvider(JavaBrowsingPart browsingPart) {
		super(false, browsingPart);
	}

	/* (non-Javadoc)
	 * Method declared on ITreeContentProvider.
	 */
	public Object[] getChildren(Object element) {
		if (!exists(element))
			return NO_CHILDREN;

		try {
			startReadInDisplayThread();
			if (element instanceof IStructuredSelection) {
				Assert.isLegal(false);
				Object[] result= new Object[0];
				Class clazz= null;
				Iterator iter= ((IStructuredSelection)element).iterator();
				while (iter.hasNext()) {
					Object item=  iter.next();
					if (clazz == null)
						clazz= item.getClass();
					if (clazz == item.getClass())
						result= concatenate(result, getChildren(item));
					else
						return NO_CHILDREN;
				}
				return result;
			}
			if (element instanceof IStructuredSelection) {
				Assert.isLegal(false);
				Object[] result= new Object[0];
				Iterator iter= ((IStructuredSelection)element).iterator();
				while (iter.hasNext())
					result= concatenate(result, getChildren(iter.next()));
				return result;
			}
			if (element instanceof IJavaScriptProject)
				return getPackageFragmentRoots((IJavaScriptProject)element);
			if (element instanceof IPackageFragmentRoot)
				return NO_CHILDREN;

			return super.getChildren(element);

		} catch (JavaScriptModelException e) {
			return NO_CHILDREN;
		} finally {
			finishedReadInDisplayThread();
		}
	}

	protected Object[] getPackageFragmentRoots(IJavaScriptProject project) throws JavaScriptModelException {
		if (!project.getProject().isOpen())
			return NO_CHILDREN;

		IPackageFragmentRoot[] roots= project.getPackageFragmentRoots();
		List list= new ArrayList(roots.length);
		// filter out package fragments that correspond to projects and
		// replace them with the package fragments directly
		for (int i= 0; i < roots.length; i++) {
			IPackageFragmentRoot root= roots[i];
			if (!isProjectPackageFragmentRoot(root))
				list.add(root);
		}
		return list.toArray();
	}

	/*
	 *
	 * @see ITreeContentProvider
	 */
	public boolean hasChildren(Object element) {
		return element instanceof IJavaScriptProject && super.hasChildren(element);
	}
}
