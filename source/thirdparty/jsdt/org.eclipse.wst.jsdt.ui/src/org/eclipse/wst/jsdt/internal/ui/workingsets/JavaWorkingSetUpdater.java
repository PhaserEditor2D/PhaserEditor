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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetUpdater;
import org.eclipse.wst.jsdt.core.ElementChangedEvent;
import org.eclipse.wst.jsdt.core.IElementChangedListener;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptElementDelta;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.JavaScriptCore;


public class JavaWorkingSetUpdater implements IWorkingSetUpdater, IElementChangedListener {

	public static final String ID= "org.eclipse.wst.jsdt.ui.JavaWorkingSetPage"; //$NON-NLS-1$
	
	private List fWorkingSets;
	
	private static class WorkingSetDelta {
		private IWorkingSet fWorkingSet;
		private List fElements;
		private boolean fChanged;
		public WorkingSetDelta(IWorkingSet workingSet) {
			fWorkingSet= workingSet;
			fElements= new ArrayList(Arrays.asList(workingSet.getElements()));
		}
		public int indexOf(Object element) {
			return fElements.indexOf(element);
		}
		public void set(int index, Object element) {
			fElements.set(index, element);
			fChanged= true;
		}
		public void remove(int index) {
			if (fElements.remove(index) != null) {
				fChanged= true;
			}
		}
		public void process() {
			if (fChanged) {
				fWorkingSet.setElements((IAdaptable[])fElements.toArray(new IAdaptable[fElements.size()]));
			}
		}
	}
	
	public JavaWorkingSetUpdater() {
		fWorkingSets= new ArrayList();
		JavaScriptCore.addElementChangedListener(this);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void add(IWorkingSet workingSet) {
		checkElementExistence(workingSet);
		synchronized (fWorkingSets) {
			fWorkingSets.add(workingSet);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean remove(IWorkingSet workingSet) {
		boolean result;
		synchronized(fWorkingSets) {
			result= fWorkingSets.remove(workingSet);
		}
		return result;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean contains(IWorkingSet workingSet) {
		synchronized(fWorkingSets) {
			return fWorkingSets.contains(workingSet);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void dispose() {
		synchronized(fWorkingSets) {
			fWorkingSets.clear();
		}
		JavaScriptCore.removeElementChangedListener(this);
	}

	/**
	 * {@inheritDoc}
	 */
	public void elementChanged(ElementChangedEvent event) {
		IWorkingSet[] workingSets;
		synchronized(fWorkingSets) {
			workingSets= (IWorkingSet[])fWorkingSets.toArray(new IWorkingSet[fWorkingSets.size()]);
		}
		for (int w= 0; w < workingSets.length; w++) {
			WorkingSetDelta workingSetDelta= new WorkingSetDelta(workingSets[w]);
			processJavaDelta(workingSetDelta, event.getDelta());
			IResourceDelta[] resourceDeltas= event.getDelta().getResourceDeltas();
			if (resourceDeltas != null) {
				for (int r= 0; r < resourceDeltas.length; r++) {
					processResourceDelta(workingSetDelta, resourceDeltas[r]);
				}
			}
			workingSetDelta.process();
		}
	}

	private void processJavaDelta(WorkingSetDelta result, IJavaScriptElementDelta delta) {
		IJavaScriptElement jElement= delta.getElement();
		int index= result.indexOf(jElement);
		int type= jElement.getElementType();
		int kind= delta.getKind();
		int flags= delta.getFlags();
		if (type == IJavaScriptElement.JAVASCRIPT_PROJECT && kind == IJavaScriptElementDelta.CHANGED) {
			if (index != -1 && (flags & IJavaScriptElementDelta.F_CLOSED) != 0) {
				result.set(index, ((IJavaScriptProject)jElement).getProject());
			} else if ((flags & IJavaScriptElementDelta.F_OPENED) != 0) {
				index= result.indexOf(((IJavaScriptProject)jElement).getProject());
				if (index != -1)
					result.set(index, jElement);
			}
		}
		if (index != -1) {
			if (kind == IJavaScriptElementDelta.REMOVED) {
				if ((flags & IJavaScriptElementDelta.F_MOVED_TO) != 0) {
					result.set(index, delta.getMovedToElement());
				} else {
					result.remove(index);
				}
			}
		}
		IResourceDelta[] resourceDeltas= delta.getResourceDeltas();
		if (resourceDeltas != null) {
			for (int i= 0; i < resourceDeltas.length; i++) {
				processResourceDelta(result, resourceDeltas[i]);
			}
		}
		IJavaScriptElementDelta[] children= delta.getAffectedChildren();
		for (int i= 0; i < children.length; i++) {
			processJavaDelta(result, children[i]);
		}
	}
	
	private void processResourceDelta(WorkingSetDelta result, IResourceDelta delta) {
		IResource resource= delta.getResource();
		int type= resource.getType();
		int index= result.indexOf(resource);
		int kind= delta.getKind();
		int flags= delta.getFlags();
		if (kind == IResourceDelta.CHANGED && type == IResource.PROJECT && index != -1) {
			if ((flags & IResourceDelta.OPEN) != 0) {
				result.set(index, resource);
			}
		}
		if (index != -1 && kind == IResourceDelta.REMOVED) {
			if ((flags & IResourceDelta.MOVED_TO) != 0) {
				result.set(index, 
					ResourcesPlugin.getWorkspace().getRoot().findMember(delta.getMovedToPath()));
			} else {
				result.remove(index);
			}
		}
		
		// Don't dive into closed or opened projects
		if (projectGotClosedOrOpened(resource, kind, flags))
			return;
		
		IResourceDelta[] children= delta.getAffectedChildren();
		for (int i= 0; i < children.length; i++) {
			processResourceDelta(result, children[i]);
		}
	}

	private boolean projectGotClosedOrOpened(IResource resource, int kind, int flags) {
		return resource.getType() == IResource.PROJECT 
			&& kind == IResourceDelta.CHANGED 
			&& (flags & IResourceDelta.OPEN) != 0;
	}
	
	private void checkElementExistence(IWorkingSet workingSet) {
		List elements= new ArrayList(Arrays.asList(workingSet.getElements()));
		boolean changed= false;
		for (Iterator iter= elements.iterator(); iter.hasNext();) {
			IAdaptable element= (IAdaptable)iter.next();
			boolean remove= false;
			if (element instanceof IJavaScriptElement) {
				IJavaScriptElement jElement= (IJavaScriptElement)element;
				// If we have directly a project then remove it when it
				// doesn't exist anymore. However if we have a sub element
				// under a project only remove the element if the parent
				// project is open. Otherwise we would remove all elements
				// in closed projects.
				if (jElement instanceof IJavaScriptProject) {
					remove= !jElement.exists();
				} else {
					IProject project= jElement.getJavaScriptProject().getProject();
					remove= project.isOpen() && !jElement.exists();
				}
			} else if (element instanceof IResource) {
				IResource resource= (IResource)element;
				// See comments above
				if (resource instanceof IProject) {
					remove= !resource.exists();
				} else {
					IProject project= resource.getProject();
					remove= (project != null ? project.isOpen() : true) && !resource.exists();
				}
			}
			if (remove) {
				iter.remove();
				changed= true;
			}
		}
		if (changed) {
			workingSet.setElements((IAdaptable[])elements.toArray(new IAdaptable[elements.size()]));
		}
	}
}
