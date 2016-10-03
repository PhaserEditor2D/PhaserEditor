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
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IContainmentAdapter;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptModel;
import org.eclipse.wst.jsdt.core.JavaScriptCore;

public class JavaElementContainmentAdapter implements IContainmentAdapter {
	
	private IJavaScriptModel fJavaModel= JavaScriptCore.create(ResourcesPlugin.getWorkspace().getRoot());

	public boolean contains(Object workingSetElement, Object element, int flags) {
		if (!(workingSetElement instanceof IJavaScriptElement) || element == null)
			return false;
						
		IJavaScriptElement workingSetJavaElement= (IJavaScriptElement)workingSetElement;
		IResource resource= null;		
		IJavaScriptElement jElement= null;
		if (element instanceof IJavaScriptElement) {
			jElement= (IJavaScriptElement)element;	
			resource= jElement.getResource(); 
		} else {
			if (element instanceof IAdaptable) {
				resource= (IResource)((IAdaptable)element).getAdapter(IResource.class);
				if (resource != null) {
					if (fJavaModel.contains(resource)) {
						jElement= JavaScriptCore.create(resource);
						if (jElement != null && !jElement.exists())
							jElement= null;		
					}
				}
			}
		}
		
		if (jElement != null) {
			if (contains(workingSetJavaElement, jElement, flags))
				return true;
			if (workingSetJavaElement.getElementType() == IJavaScriptElement.PACKAGE_FRAGMENT && 
				resource.getType() == IResource.FOLDER && checkIfDescendant(flags))
				return isChild(workingSetJavaElement, resource);
		} else if (resource != null) {
			return contains(workingSetJavaElement, resource, flags);
		}
		return false;
	}
	
	private boolean contains(IJavaScriptElement workingSetElement, IJavaScriptElement element, int flags) {
		if (checkContext(flags) && workingSetElement.equals(element)) {
			return true;
		}
		if (checkIfChild(flags) && workingSetElement.equals(element.getParent())) {
			return true;
		}
		if (checkIfDescendant(flags) && check(workingSetElement, element)) {
			return true;
		}
		if (checkIfAncestor(flags) && check(element, workingSetElement)) {
			return true;
		}
		return false;
	}
	
	private boolean check(IJavaScriptElement ancestor, IJavaScriptElement descendent) {
		descendent= descendent.getParent();
		while (descendent != null) {
			if (ancestor.equals(descendent))
				return true;
			descendent= descendent.getParent();
		}
		return false;
	}
	
	private boolean isChild(IJavaScriptElement workingSetElement, IResource element) {
		IResource resource= workingSetElement.getResource();
		if (resource == null)
			return false;
		return check(element, resource);
	}
	
	private boolean contains(IJavaScriptElement workingSetElement, IResource element, int flags) {
		IResource workingSetResource= workingSetElement.getResource();
		if (workingSetResource == null)
			return false;
		if (checkContext(flags) && workingSetResource.equals(element)) {
			return true;
		}
		if (checkIfChild(flags) && workingSetResource.equals(element.getParent())) {
			return true;
		}
		if (checkIfDescendant(flags) && check(workingSetResource, element)) {
			return true;
		}
		if (checkIfAncestor(flags) && check(element, workingSetResource)) {
			return true;
		}
		return false;
	}
	
	private boolean check(IResource ancestor, IResource descendent) {
		descendent= descendent.getParent();
		while(descendent != null) {
			if (ancestor.equals(descendent))
				return true;
			descendent= descendent.getParent();
		}
		return false;
	}
	
	private boolean checkIfDescendant(int flags) {
		return (flags & CHECK_IF_DESCENDANT) != 0;
	}
	
	private boolean checkIfAncestor(int flags) {
		return (flags & CHECK_IF_ANCESTOR) != 0;
	}
	
	private boolean checkIfChild(int flags) {
		return (flags & CHECK_IF_CHILD) != 0;
	}
	
	private boolean checkContext(int flags) {
		return (flags & CHECK_CONTEXT) != 0;
	}
}
