/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.packageview;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptModel;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.internal.ui.workingsets.WorkingSetModel;

public class WorkingSetAwareContentProvider extends ScriptExplorerContentProvider implements IMultiElementTreeContentProvider {

	private WorkingSetModel fWorkingSetModel;
	private IPropertyChangeListener fListener;
	
	public WorkingSetAwareContentProvider(boolean provideMembers, WorkingSetModel model) {
		super(provideMembers);
		fWorkingSetModel= model;
		fListener= new IPropertyChangeListener() {
					public void propertyChange(PropertyChangeEvent event) {
						workingSetModelChanged(event);
					}
				};
		fWorkingSetModel.addPropertyChangeListener(fListener);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void dispose() {
		fWorkingSetModel.removePropertyChangeListener(fListener);
		super.dispose();
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean hasChildren(Object element) {
		if (element instanceof IWorkingSet)
			return true;
		return super.hasChildren(element);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Object[] getChildren(Object element) {
		Object[] children;
		if (element instanceof WorkingSetModel) {
			Assert.isTrue(fWorkingSetModel == element);
			return fWorkingSetModel.getActiveWorkingSets();
		} else if (element instanceof IWorkingSet) {
			children= getWorkingSetChildren((IWorkingSet)element);
		} else {
			children= super.getChildren(element);
		}
		return children;
	}

	private Object[] getWorkingSetChildren(IWorkingSet set) {
		IAdaptable[] elements= fWorkingSetModel.getChildren(set);
		Set result= new HashSet(elements.length);
		for (int i= 0; i < elements.length; i++) {
			IAdaptable element= elements[i];
			if (element instanceof IProject) {
				processResource((IProject) element, result); // also add closed projects
				result.add(element);
			} else if (element instanceof IResource) {
				IProject project= ((IResource) element).getProject();
				if (project.isOpen()) {
//					processResource((IResource) element, result);
					result.add(element);
				}
			} else if (element instanceof IJavaScriptProject) {
//				result.add(((IJavaScriptProject) element)); // also add closed projects
				result.add(((IJavaScriptProject) element).getProject()); // also add closed projects
			} else if (element instanceof IJavaScriptElement) {
				IJavaScriptElement elem= (IJavaScriptElement) element;
				IProject project= getProject(elem);
				if (project != null && project.isOpen()) {
					result.add(elem);
				}
			} else {
				IProject project= (IProject) element.getAdapter(IProject.class);
				if (project != null) {
					processResource(project, result);
				}
			}
		}
		return result.toArray();
	}
	
	private void processResource(IResource resource, Collection result) {
		IJavaScriptElement elem= JavaScriptCore.create(resource);
		if (elem != null && elem.exists()) {
			result.add(elem);
		} else {
			result.add(resource);
		}
	}
	
	private IProject getProject(IJavaScriptElement element) {
		IJavaScriptProject project= element.getJavaScriptProject();
		if (project == null)
			return null;
		return project.getProject();
	}
 
	/**
	 * {@inheritDoc}
	 */
	public TreePath[] getTreePaths(Object element) {
		if (element instanceof IWorkingSet) {
			TreePath path= new TreePath(new Object[] {element});
			return new TreePath[] {path};
		}
		List modelParents= getModelPath(element);
		List result= new ArrayList();
		for (int i= 0; i < modelParents.size(); i++) {
			result.addAll(getTreePaths(modelParents, i));
		}
		return (TreePath[])result.toArray(new TreePath[result.size()]);
	}
	
	private List getModelPath(Object element) {
		List result= new ArrayList();
		result.add(element);
		Object parent= super.getParent(element);
		Object input= getViewerInput();
		// stop at input or on JavaModel. We never visualize it anyway.
		while (parent != null && !parent.equals(input) && !(parent instanceof IJavaScriptModel)) {
			result.add(parent);
			parent= super.getParent(parent);
		}
		Collections.reverse(result);
		return result;
	}
	
	private List/*<TreePath>*/ getTreePaths(List modelParents, int index) {
		List result= new ArrayList();
		Object input= getViewerInput();
		Object element= modelParents.get(index);
		Object[] parents= fWorkingSetModel.getAllParents(element);
		for (int i= 0; i < parents.length; i++) {
			List chain= new ArrayList();
			if (!parents[i].equals(input))
				chain.add(parents[i]);
			for (int m= index; m < modelParents.size(); m++) {
				chain.add(modelParents.get(m));
			}
			result.add(new TreePath(chain.toArray()));
		}
		return result;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Object getParent(Object child) {
		Object[] parents= fWorkingSetModel.getAllParents(child);
		if(parents.length == 0)
			return super.getParent(child);
		Object first= parents[0];
		return first;
	}
	
	protected void augmentElementToRefresh(List toRefresh, int relation, Object affectedElement) {
		// we are refreshing the JavaModel and are in working set mode.
		if (JavaScriptCore.create(ResourcesPlugin.getWorkspace().getRoot()).equals(affectedElement)) {
			toRefresh.remove(affectedElement);
			toRefresh.add(fWorkingSetModel);
		} else if (relation == GRANT_PARENT) {
			Object parent= internalGetParent(affectedElement);
			if (parent != null) {
				toRefresh.addAll(Arrays.asList(fWorkingSetModel.getAllParents(parent)));
			}
		}
		List nonProjetTopLevelElemens= fWorkingSetModel.getNonProjectTopLevelElements();
		if (nonProjetTopLevelElemens.isEmpty())
			return;
		List toAdd= new ArrayList();
		for (Iterator iter= nonProjetTopLevelElemens.iterator(); iter.hasNext();) {
			Object element= iter.next();
			if (isChildOf(element, toRefresh))
				toAdd.add(element);
		}
		toRefresh.addAll(toAdd);
	}
	
	private void workingSetModelChanged(PropertyChangeEvent event) {
		String property= event.getProperty();
		Object newValue= event.getNewValue();
		List toRefresh= new ArrayList(1);
		if (WorkingSetModel.CHANGE_WORKING_SET_MODEL_CONTENT.equals(property)) {
			toRefresh.add(fWorkingSetModel);
		} else if (IWorkingSetManager.CHANGE_WORKING_SET_CONTENT_CHANGE.equals(property)) {
			toRefresh.add(newValue);
		} else if (IWorkingSetManager.CHANGE_WORKING_SET_NAME_CHANGE.equals(property)) {
			toRefresh.add(newValue);
		}
		ArrayList runnables= new ArrayList();
		postRefresh(toRefresh, true, runnables);
		executeRunnables(runnables);
	}
	
	private boolean isChildOf(Object element, List potentialParents) {
		// Calling super get parent to bypass working set mapping
		Object parent= super.getParent(element);
		if (parent == null)
			return false;
		for (Iterator iter= potentialParents.iterator(); iter.hasNext();) {
			Object potentialParent= iter.next();
			while(parent != null) {
				if (parent.equals(potentialParent))
					return true;
				parent= super.getParent(parent);
			}
			
		}
		return false;
	}
}
