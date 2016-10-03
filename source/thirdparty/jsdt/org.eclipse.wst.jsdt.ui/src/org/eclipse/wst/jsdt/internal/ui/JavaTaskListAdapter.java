/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
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
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.views.tasklist.ITaskListResourceAdapter;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;

public class JavaTaskListAdapter implements ITaskListResourceAdapter {

	/*
	 * @see ITaskListResourceAdapter#getAffectedResource(IAdaptable)
	 */
	public IResource getAffectedResource(IAdaptable element) {
		IResource resource= (IResource) element.getAdapter(IResource.class);
		if (resource != null)
			return resource; 

		IJavaScriptElement java = (IJavaScriptElement) element;
		resource= java.getResource();
		if (resource != null)
			return resource; 
		
		IJavaScriptUnit cu= (IJavaScriptUnit) java.getAncestor(IJavaScriptElement.JAVASCRIPT_UNIT);
		if (cu != null) {
			return cu.getPrimary().getResource();
		}
		return null;
	 }
}
