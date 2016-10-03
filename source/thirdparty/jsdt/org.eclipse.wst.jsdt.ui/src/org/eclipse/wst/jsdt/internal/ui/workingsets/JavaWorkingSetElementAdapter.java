/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.wst.jsdt.internal.ui.workingsets;

import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetElementAdapter;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.JavaScriptCore;

public class JavaWorkingSetElementAdapter implements IWorkingSetElementAdapter {

	public IAdaptable[] adaptElements(IWorkingSet ws, IAdaptable[] elements) {
		ArrayList result= new ArrayList(elements.length);
		
		for (int i= 0; i < elements.length; i++) {
			IAdaptable curr= elements[i];
			if (curr instanceof IJavaScriptElement) {
				result.add(curr);
			} else if (curr instanceof IResource) {
				result.add(adaptFromResource((IResource) curr));
			} else {
				Object elem= curr.getAdapter(IJavaScriptElement.class);
				if (elem == null) {
					elem= curr.getAdapter(IResource.class);
					if (elem != null) {
						elem= adaptFromResource((IResource) elem);
					}
				}
				if (elem != null) {
					result.add(elem);
				} // ignore all others
			}
		}
		return (IAdaptable[]) result.toArray(new IAdaptable[result.size()]);
	}
	
	private Object adaptFromResource(IResource resource) {
		IProject project= resource.getProject();
		if (project != null && project.isAccessible()) {
			try {
				if (project.hasNature(JavaScriptCore.NATURE_ID)) {
					IJavaScriptElement elem= JavaScriptCore.create(resource);
					if (elem != null) {
						return elem;
					}
				}
			} catch (CoreException e) {
				// ignore
			}
		}
		return resource;
	}
	

	public void dispose() {
	}

}
