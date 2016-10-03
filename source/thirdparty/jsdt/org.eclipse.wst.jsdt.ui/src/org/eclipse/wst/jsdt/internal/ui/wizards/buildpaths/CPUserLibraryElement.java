/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IJsGlobalScopeContainer;
import org.eclipse.wst.jsdt.core.JavaScriptCore;

public class CPUserLibraryElement {
	
	private  class UpdatedJsGlobalScopeContainer implements IJsGlobalScopeContainer {
				
		/* (non-Javadoc)
		 * @see org.eclipse.wst.jsdt.core.IJsGlobalScopeContainer#getClasspathEntries()
		 */
		/**
		 * @deprecated Use {@link #getIncludepathEntries()} instead
		 */
		public IIncludePathEntry[] getClasspathEntries() {
			return getIncludepathEntries();
		}

		/* (non-Javadoc)
		 * @see org.eclipse.wst.jsdt.core.IJsGlobalScopeContainer#getClasspathEntries()
		 */
		public IIncludePathEntry[] getIncludepathEntries() {
			CPListElement[] children= getChildren();
			IIncludePathEntry[] entries= new IIncludePathEntry[children.length];
			for (int i= 0; i < entries.length; i++) {
				entries[i]= children[i].getClasspathEntry();
			}
			return entries;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.wst.jsdt.core.IJsGlobalScopeContainer#getDescription()
		 */
		public String getDescription() {
			return getName();
		}

		/* (non-Javadoc)
		 * @see org.eclipse.wst.jsdt.core.IJsGlobalScopeContainer#getKind()
		 */
		public int getKind() {
			return K_APPLICATION;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.wst.jsdt.core.IJsGlobalScopeContainer#getPath()
		 */
		public IPath getPath() {
			return CPUserLibraryElement.this.getPath();
		}

		/* (non-Javadoc)
		 * @see org.eclipse.wst.jsdt.core.IJsGlobalScopeContainer#resolvedLibraryImport(java.lang.String)
		 */
		public String[] resolvedLibraryImport(String a) {
			// TODO Auto-generated method stub
			return null;
		}
	}
	
	
	private String fName;
	private List fChildren;

	public CPUserLibraryElement(String name, IJsGlobalScopeContainer container, IJavaScriptProject project) {
		fName= name;
		fChildren= new ArrayList();
		if (container != null) {
			IIncludePathEntry[] entries= container.getIncludepathEntries();
			CPListElement[] res= new CPListElement[entries.length];
			for (int i= 0; i < res.length; i++) {
				IIncludePathEntry curr= entries[i];
				CPListElement elem= CPListElement.createFromExisting(this, curr, project);
				fChildren.add(elem);
			}
		}
	}
	
	public CPUserLibraryElement(String name, CPListElement[] children) {
		fName= name;
		fChildren= new ArrayList();
		if (children != null) {
			for (int i= 0; i < children.length; i++) {
				fChildren.add(children[i]);
			}
		}
	}
	
	public CPListElement[] getChildren() {
		return (CPListElement[]) fChildren.toArray(new CPListElement[fChildren.size()]);
	}

	public String getName() {
		return fName;
	}
	
	public IPath getPath() {
		return new Path(JavaScriptCore.USER_LIBRARY_CONTAINER_ID).append(fName);
	}
	
	public void add(CPListElement element) {
		if (!fChildren.contains(element)) {
			fChildren.add(element);
		}
	}
	
	public void remove(CPListElement element) {
		fChildren.remove(element);
	}
	
	public void replace(CPListElement existingElement, CPListElement element) {
		if (fChildren.contains(element)) {
			fChildren.remove(existingElement);
		} else {
			int index= fChildren.indexOf(existingElement);
			if (index != -1) {
				fChildren.set(index, element);
			} else {
				fChildren.add(element);
			}
			element.setAttributesFromExisting(existingElement);
		}
	}
	
	public IJsGlobalScopeContainer getUpdatedContainer() {
		return new UpdatedJsGlobalScopeContainer();
	}
		
	public boolean hasChanges(IJsGlobalScopeContainer oldContainer) {
		if (oldContainer == null) {
			return true;
		}
		IIncludePathEntry[] oldEntries= oldContainer.getIncludepathEntries();
		if (fChildren.size() != oldEntries.length) {
			return true;
		}
		for (int i= 0; i < oldEntries.length; i++) {
			CPListElement child= (CPListElement) fChildren.get(i);
			if (!child.getClasspathEntry().equals(oldEntries[i])) {
				return true;
			}
		}
		return false;
	}
	
	
}
