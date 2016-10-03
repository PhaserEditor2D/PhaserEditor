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
package org.eclipse.wst.jsdt.internal.ui.workingsets;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.jsdt.core.IJavaScriptModel;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.ui.StandardJavaScriptElementContentProvider;

class JavaWorkingSetPageContentProvider extends StandardJavaScriptElementContentProvider {
	
	public boolean hasChildren(Object element) {

		if (element instanceof IProject && !((IProject)element).isAccessible())
			return false;

		if (element instanceof IPackageFragment) {
			IPackageFragment pkg= (IPackageFragment)element;
			try {
				if (pkg.getKind() == IPackageFragmentRoot.K_BINARY)
					return pkg.getChildren().length > 0;
			} catch (JavaScriptModelException ex) {
				// use super behavior
			}
		}
		return super.hasChildren(element);
	}

	public Object[] getChildren(Object parentElement) {
		try {
			if (parentElement instanceof IJavaScriptModel) 
				return concatenate(super.getChildren(parentElement), getNonJavaProjects((IJavaScriptModel)parentElement));
			
			if (parentElement instanceof IProject) 
				return ((IProject)parentElement).members();

			return super.getChildren(parentElement);
		} catch (CoreException e) {
			return NO_CHILDREN;
		}
	}

	private Object[] getNonJavaProjects(IJavaScriptModel model) throws JavaScriptModelException {
		return model.getNonJavaScriptResources();
	}
}
