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
package org.eclipse.wst.jsdt.internal.ui;

import org.eclipse.core.resources.IResource;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;

/**
 * This class locates different resources
 * which are related to an object
 */
public class ResourceLocator implements IResourceLocator {
	
	public IResource getUnderlyingResource(Object element) throws JavaScriptModelException {
		if (element instanceof IJavaScriptElement)
			return ((IJavaScriptElement) element).getUnderlyingResource();
		else
			return null;
	}

	public IResource getCorrespondingResource(Object element) throws JavaScriptModelException {
		if (element instanceof IJavaScriptElement)
			return ((IJavaScriptElement) element).getCorrespondingResource();
		else
			return null;
	}

	public IResource getContainingResource(Object element) throws JavaScriptModelException {
		IResource resource= null;
		if (element instanceof IResource)
			resource= (IResource) element;
		if (element instanceof IJavaScriptElement) {
			resource= ((IJavaScriptElement) element).getResource();
			if (resource == null)
				resource= ((IJavaScriptElement) element).getJavaScriptProject().getProject();
		}
		return resource;
	}
}
