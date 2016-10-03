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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.IWorkingSetUpdater;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.core.ElementChangedEvent;
import org.eclipse.wst.jsdt.core.IElementChangedListener;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptElementDelta;
import org.eclipse.wst.jsdt.core.IJavaScriptModel;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;

public class OthersWorkingSetUpdater implements IWorkingSetUpdater {
	
	public static final String ID= "org.eclipse.wst.jsdt.internal.ui.OthersWorkingSet";  //$NON-NLS-1$
	
	private IWorkingSet fWorkingSet;
	private WorkingSetModel fWorkingSetModel;
	
	private class ResourceChangeListener implements IResourceChangeListener {
		public void resourceChanged(IResourceChangeEvent event) {
			if (fWorkingSet == null)
				return;		// not yet initialized
			IResourceDelta delta= event.getDelta();
			IResourceDelta[] affectedChildren= delta.getAffectedChildren(IResourceDelta.ADDED | IResourceDelta.REMOVED, IResource.PROJECT);
			if (affectedChildren.length > 0) {
				updateElements();
			} else {
				affectedChildren= delta.getAffectedChildren(IResourceDelta.CHANGED, IResource.PROJECT);
				for (int i= 0; i < affectedChildren.length; i++) {
					IResourceDelta projectDelta= affectedChildren[i];
					if ((projectDelta.getFlags() & IResourceDelta.DESCRIPTION) != 0) {
						updateElements();
						// one is enough
						return;
					}
				}
			}
		}
	}
	private IResourceChangeListener fResourceChangeListener;
	
	private class WorkingSetListener implements IPropertyChangeListener {
		public void propertyChange(PropertyChangeEvent event) {
			if (IWorkingSetManager.CHANGE_WORKING_SET_CONTENT_CHANGE.equals(event.getProperty())) {
				IWorkingSet changedWorkingSet= (IWorkingSet) event.getNewValue();
				if (changedWorkingSet != fWorkingSet && fWorkingSetModel.isActiveWorkingSet(changedWorkingSet)) {
					updateElements();
				}
			}
		}
	}
	private IPropertyChangeListener fWorkingSetListener;
	
	private class JavaElementChangeListener implements IElementChangedListener {
		public void elementChanged(ElementChangedEvent event) {
			if (fWorkingSet == null)
				return; // not yet initialized
			
			processJavaDelta(new ArrayList(Arrays.asList(fWorkingSet.getElements())), event.getDelta());
		}
		private void processJavaDelta(List elements, IJavaScriptElementDelta delta) {
			IJavaScriptElement jElement= delta.getElement();
			int type= jElement.getElementType();
			if (type == IJavaScriptElement.JAVASCRIPT_PROJECT) {
				int index= elements.indexOf(jElement);
				int kind= delta.getKind();
				int flags= delta.getFlags();
				if (kind == IJavaScriptElementDelta.CHANGED) {
					if (index != -1 && (flags & IJavaScriptElementDelta.F_CLOSED) != 0) {
						elements.set(index, ((IJavaScriptProject)jElement).getProject());
						fWorkingSet.setElements((IAdaptable[])elements.toArray(new IAdaptable[elements.size()]));
					} else if ((flags & IJavaScriptElementDelta.F_OPENED) != 0) {
						index= elements.indexOf(((IJavaScriptProject)jElement).getProject());
						if (index != -1) {
							elements.set(index, jElement);
							fWorkingSet.setElements((IAdaptable[])elements.toArray(new IAdaptable[elements.size()]));
						}
					}
				}
				// don't visit below projects
				return;
			}
			IJavaScriptElementDelta[] children= delta.getAffectedChildren();
			for (int i= 0; i < children.length; i++) {
				processJavaDelta(elements, children[i]);
			}
		}
	}
	private IElementChangedListener fJavaElementChangeListener;
	
	/**
	 * {@inheritDoc}
	 */
	public void add(IWorkingSet workingSet) {
		Assert.isTrue(fWorkingSet == null && fWorkingSetModel != null);
		fWorkingSet= workingSet;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean remove(IWorkingSet workingSet) {
		Assert.isTrue(fWorkingSet == workingSet);
		fWorkingSet= null;
		return true;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean contains(IWorkingSet workingSet) {
		return fWorkingSet == workingSet;
	}
	
	public void init(WorkingSetModel model) {
		fWorkingSetModel= model;
		fResourceChangeListener= new ResourceChangeListener();
		ResourcesPlugin.getWorkspace().addResourceChangeListener(fResourceChangeListener, IResourceChangeEvent.POST_CHANGE);
		fWorkingSetListener= new WorkingSetListener();
		PlatformUI.getWorkbench().getWorkingSetManager().addPropertyChangeListener(fWorkingSetListener);
		fJavaElementChangeListener= new JavaElementChangeListener();
		JavaScriptCore.addElementChangedListener(fJavaElementChangeListener, ElementChangedEvent.POST_CHANGE);
	}
	
	public void dispose() {
		if (fResourceChangeListener != null) {
			ResourcesPlugin.getWorkspace().removeResourceChangeListener(fResourceChangeListener);
			fResourceChangeListener= null;
		}
		if (fWorkingSetListener != null) {
			PlatformUI.getWorkbench().getWorkingSetManager().removePropertyChangeListener(fWorkingSetListener);
			fWorkingSetListener= null;
		}
		if (fJavaElementChangeListener != null) {
			JavaScriptCore.removeElementChangedListener(fJavaElementChangeListener);
		}
	}
	
	public void updateElements() {
		Assert.isTrue(fWorkingSet != null && fWorkingSetModel != null); // init and addWorkingSet have happend
		
		IWorkingSet[] activeWorkingSets= fWorkingSetModel.getActiveWorkingSets();

		List result= new ArrayList();
		Set projects= new HashSet();
		for (int i= 0; i < activeWorkingSets.length; i++) {
			if (activeWorkingSets[i] == fWorkingSet) continue;
			IAdaptable[] elements= activeWorkingSets[i].getElements();
			for (int j= 0; j < elements.length; j++) {
				IAdaptable element= elements[j];
				IResource resource= (IResource)element.getAdapter(IResource.class);
				if (resource != null && resource.getType() == IResource.PROJECT) {
					projects.add(resource);
				}
			}
		}
		IJavaScriptModel model= JavaScriptCore.create(ResourcesPlugin.getWorkspace().getRoot());
		try {
			IJavaScriptProject[] jProjects= model.getJavaScriptProjects();
			for (int i= 0; i < jProjects.length; i++) {
				if (!projects.contains(jProjects[i].getProject()))
					result.add(jProjects[i]);
			}
			Object[] rProjects= model.getNonJavaScriptResources();
			for (int i= 0; i < rProjects.length; i++) {
				if (!projects.contains(rProjects[i]))
					result.add(rProjects[i]);
			}
		} catch (JavaScriptModelException e) {
			JavaScriptPlugin.log(e);
		}
		fWorkingSet.setElements((IAdaptable[])result.toArray(new IAdaptable[result.size()]));
	}
}
