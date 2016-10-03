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
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.ui.ILocalWorkingSetManager;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.IWorkingSetUpdater;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;

public class WorkingSetModel {

	public static final String CHANGE_WORKING_SET_MODEL_CONTENT= "workingSetModelChanged"; //$NON-NLS-1$

	public static final IElementComparer COMPARER= new WorkingSetComparar();

	private static final String TAG_LOCAL_WORKING_SET_MANAGER= "localWorkingSetManager"; //$NON-NLS-1$
	private static final String TAG_ACTIVE_WORKING_SET= "activeWorkingSet"; //$NON-NLS-1$
	private static final String TAG_WORKING_SET_NAME= "workingSetName"; //$NON-NLS-1$
	private static final String TAG_CONFIGURED= "configured"; //$NON-NLS-1$

	private ILocalWorkingSetManager fLocalWorkingSetManager;
	private List fActiveWorkingSets;
	private ListenerList fListeners;
	private IPropertyChangeListener fWorkingSetManagerListener;
	private OthersWorkingSetUpdater fOthersWorkingSetUpdater;

	private ElementMapper fElementMapper= new ElementMapper();

	private boolean fConfigured;

	private static class WorkingSetComparar implements IElementComparer {
		public boolean equals(Object o1, Object o2) {
			IWorkingSet w1= o1 instanceof IWorkingSet ? (IWorkingSet)o1 : null;
			IWorkingSet w2= o2 instanceof IWorkingSet ? (IWorkingSet)o2 : null;
			if (w1 == null || w2 == null)
				return o1.equals(o2);
			return w1 == w2;
		}
		public int hashCode(Object element) {
			if (element instanceof IWorkingSet)
				return System.identityHashCode(element);
			return element.hashCode();
		}
	}

	private static class ElementMapper {
		private Map fElementToWorkingSet= new HashMap();
		private Map fWorkingSetToElement= new IdentityHashMap();

		private Map fResourceToWorkingSet= new HashMap();
		private List fNonProjectTopLevelElements= new ArrayList();

		public void clear() {
			fElementToWorkingSet.clear();
			fWorkingSetToElement.clear();
			fResourceToWorkingSet.clear();
			fNonProjectTopLevelElements.clear();
		}
		public void rebuild(IWorkingSet[] workingSets) {
			clear();
			for (int i= 0; i < workingSets.length; i++) {
				put(workingSets[i]);
			}
		}
		public IAdaptable[] remove(IWorkingSet ws) {
			IAdaptable[] elements= (IAdaptable[])fWorkingSetToElement.remove(ws);
			if (elements != null) {
				for (int i= 0; i < elements.length; i++) {
					removeElement(elements[i], ws);
				}
			}
			return elements;
		}
		public IAdaptable[] refresh(IWorkingSet ws) {
			IAdaptable[] oldElements= (IAdaptable[])fWorkingSetToElement.get(ws);
			if (oldElements == null)
				return null;
			IAdaptable[] newElements= ws.getElements();
			List toRemove= new ArrayList(Arrays.asList(oldElements));
			List toAdd= new ArrayList(Arrays.asList(newElements));
			computeDelta(toRemove, toAdd, oldElements, newElements);
			for (Iterator iter= toAdd.iterator(); iter.hasNext();) {
				addElement((IAdaptable)iter.next(), ws);
			}
			for (Iterator iter= toRemove.iterator(); iter.hasNext();) {
				removeElement((IAdaptable)iter.next(), ws);
			}
			if (toRemove.size() > 0 || toAdd.size() > 0)
				fWorkingSetToElement.put(ws, newElements);
			return oldElements;
		}
		private void computeDelta(List toRemove, List toAdd, IAdaptable[] oldElements, IAdaptable[] newElements) {
			for (int i= 0; i < oldElements.length; i++) {
				toAdd.remove(oldElements[i]);
			}
			for (int i= 0; i < newElements.length; i++) {
				toRemove.remove(newElements[i]);
			}

		}
		public IWorkingSet getFirstWorkingSet(Object element) {
			return (IWorkingSet)getFirstElement(fElementToWorkingSet, element);
		}
		public List getAllWorkingSets(Object element) {
			 List allElements= getAllElements(fElementToWorkingSet, element);
			 if (allElements.isEmpty() && element instanceof IJavaScriptElement) {
				 // try a second time in case the working set was manually updated (bug 168032)
				 allElements= getAllElements(fElementToWorkingSet, ((IJavaScriptElement) element).getResource());
			 }
			 return allElements;
		}
		public IWorkingSet getFirstWorkingSetForResource(IResource resource) {
			return (IWorkingSet)getFirstElement(fResourceToWorkingSet, resource);
		}
		public List getAllWorkingSetsForResource(IResource resource) {
			return getAllElements(fResourceToWorkingSet, resource);
		}
		public List getNonProjectTopLevelElements() {
			return fNonProjectTopLevelElements;
		}
		private void put(IWorkingSet ws) {
			if (fWorkingSetToElement.containsKey(ws))
				return;
			IAdaptable[] elements= ws.getElements();
			fWorkingSetToElement.put(ws, elements);
			for (int i= 0; i < elements.length; i++) {
				IAdaptable element= elements[i];
				addElement(element, ws);
				if (!(element instanceof IProject) && !(element instanceof IJavaScriptProject)) {
					fNonProjectTopLevelElements.add(element);
				}
			}
		}
		private void addElement(IAdaptable element, IWorkingSet ws) {
			addToMap(fElementToWorkingSet, element, ws);
			IResource resource= (IResource)element.getAdapter(IResource.class);
			if (resource != null) {
				addToMap(fResourceToWorkingSet, resource, ws);
			}
		}
		private void removeElement(IAdaptable element, IWorkingSet ws) {
			removeFromMap(fElementToWorkingSet, element, ws);
			IResource resource= (IResource)element.getAdapter(IResource.class);
			if (resource != null) {
				removeFromMap(fResourceToWorkingSet, resource, ws);
			}
		}
		private void addToMap(Map map, IAdaptable key, IWorkingSet value) {
			Object obj= map.get(key);
			if (obj == null) {
				map.put(key, value);
			} else if (obj instanceof IWorkingSet) {
				List l= new ArrayList(2);
				l.add(obj);
				l.add(value);
				map.put(key, l);
			} else if (obj instanceof List) {
				((List)obj).add(value);
			}
		}
		private void removeFromMap(Map map, IAdaptable key, IWorkingSet value) {
			Object current= map.get(key);
			if (current == null) {
				return;
			} else if (current instanceof List) {
				List list= (List)current;
				list.remove(value);
				switch (list.size()) {
					case 0:
						map.remove(key);
						break;
					case 1:
						map.put(key, list.get(0));
						break;
				}
			} else if (current == value) {
				map.remove(key);
			}
		}
		private Object getFirstElement(Map map, Object key) {
			Object obj= map.get(key);
			if (obj instanceof List) 
				return ((List)obj).get(0);
			return obj;
		}
		private List getAllElements(Map map, Object key) {
			Object obj= map.get(key);
			if (obj instanceof List)
				return (List)obj;
			if (obj == null)
				return Collections.EMPTY_LIST;
			List result= new ArrayList(1);
			result.add(obj);
			return result;
		}
	}

	/**
	 * @param memento a memento, or <code>null</code>
	 */
	public WorkingSetModel(IMemento memento) {
		fLocalWorkingSetManager= PlatformUI.getWorkbench().createLocalWorkingSetManager();
		addListenersToWorkingSetManagers();
		fActiveWorkingSets= new ArrayList(2);
		
		if (memento == null || ! restoreState(memento)) {
			IWorkingSet others= fLocalWorkingSetManager.createWorkingSet(WorkingSetMessages.WorkingSetModel_others_name, new IAdaptable[0]); 
			others.setId(OthersWorkingSetUpdater.ID);
			fLocalWorkingSetManager.addWorkingSet(others);
			fActiveWorkingSets.add(others);
		}
		Assert.isNotNull(fOthersWorkingSetUpdater);
		
		fElementMapper.rebuild(getActiveWorkingSets());
		fOthersWorkingSetUpdater.updateElements();
	}

	private void addListenersToWorkingSetManagers() {
		fListeners= new ListenerList(ListenerList.IDENTITY);
		fWorkingSetManagerListener= new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				workingSetManagerChanged(event);
			}
		};
		PlatformUI.getWorkbench().getWorkingSetManager().addPropertyChangeListener(fWorkingSetManagerListener);
		fLocalWorkingSetManager.addPropertyChangeListener(fWorkingSetManagerListener);
	}

	public void dispose() {
		if (fWorkingSetManagerListener != null) {
			PlatformUI.getWorkbench().getWorkingSetManager().removePropertyChangeListener(fWorkingSetManagerListener);
			fLocalWorkingSetManager.removePropertyChangeListener(fWorkingSetManagerListener);
			fLocalWorkingSetManager.dispose();
			fWorkingSetManagerListener= null;
		}
	}

	//---- model relationships ---------------------------------------

	public IAdaptable[] getChildren(IWorkingSet workingSet) {
		return workingSet.getElements();
	}

	public Object getParent(Object element) {
		if (element instanceof IWorkingSet && fActiveWorkingSets.contains(element))
			return this;
		return fElementMapper.getFirstWorkingSet(element);
	}

	public Object[] getAllParents(Object element) {
		if (element instanceof IWorkingSet && fActiveWorkingSets.contains(element))
			return new Object[] {this};
		return fElementMapper.getAllWorkingSets(element).toArray();
	}

	public Object[] addWorkingSets(Object[] elements) {
		List result= null;
		for (int i= 0; i < elements.length; i++) {
			Object element= elements[i];
			List sets= null;
			if (element instanceof IResource) {
				sets= fElementMapper.getAllWorkingSetsForResource((IResource)element);
			} else {
				sets= fElementMapper.getAllWorkingSets(element);
			}
			if (sets != null && sets.size() > 0) {
				if (result == null)
					result= new ArrayList(Arrays.asList(elements));
				result.addAll(sets);
			}
		}
		if (result == null)
			return elements;
		return result.toArray();
	}

	public boolean needsConfiguration() {
		return !fConfigured && fActiveWorkingSets.size() == 1 &&
		OthersWorkingSetUpdater.ID.equals(((IWorkingSet)fActiveWorkingSets.get(0)).getId());
	}

	public void configured() {
		fConfigured= true;
	}

	//---- working set management -----------------------------------

	/**
	 * Adds a property change listener.
	 * 
	 * @param listener the property change listener to add
	 */
	public void addPropertyChangeListener(IPropertyChangeListener listener) {
		fListeners.add(listener);
	}

	/**
	 * Removes the property change listener.
	 * 
	 * @param listener the property change listener to remove
	 */
	public void removePropertyChangeListener(IPropertyChangeListener listener) {
		fListeners.remove(listener);
	}

	public IWorkingSet[] getActiveWorkingSets() {
		return (IWorkingSet[])fActiveWorkingSets.toArray(new IWorkingSet[fActiveWorkingSets.size()]);
	}

	public IWorkingSet[] getAllWorkingSets() {
		List result= new ArrayList();
		result.addAll(fActiveWorkingSets);
		IWorkingSet[] locals= fLocalWorkingSetManager.getWorkingSets();
		for (int i= 0; i < locals.length; i++) {
			if (!result.contains(locals[i]))
				result.add(locals[i]);
		}
		IWorkingSet[] globals= PlatformUI.getWorkbench().getWorkingSetManager().getWorkingSets();
		for (int i= 0; i < globals.length; i++) {
			if (!result.contains(globals[i]))
				result.add(globals[i]);
		}
		return (IWorkingSet[])result.toArray(new IWorkingSet[result.size()]);
	}

	public void setActiveWorkingSets(IWorkingSet[] workingSets) {
		fActiveWorkingSets= new ArrayList(Arrays.asList(workingSets));
		fElementMapper.rebuild(getActiveWorkingSets());
		fOthersWorkingSetUpdater.updateElements();
		fireEvent(new PropertyChangeEvent(this, CHANGE_WORKING_SET_MODEL_CONTENT, null, null));
	}

	public void saveState(IMemento memento) {
		memento.putString(TAG_CONFIGURED, Boolean.toString(fConfigured));
		fLocalWorkingSetManager.saveState(memento.createChild(TAG_LOCAL_WORKING_SET_MANAGER));
		for (Iterator iter= fActiveWorkingSets.iterator(); iter.hasNext();) {
			IMemento active= memento.createChild(TAG_ACTIVE_WORKING_SET);
			IWorkingSet workingSet= (IWorkingSet)iter.next();
			active.putString(TAG_WORKING_SET_NAME, workingSet.getName());
		}
	}
	
	public List getNonProjectTopLevelElements() {
		return fElementMapper.getNonProjectTopLevelElements();
	}

	/**
	 * Restore localWorkingSetManager and active working sets
	 * @param memento
	 * @return whether the restore was successful
	 */
	private boolean restoreState(IMemento memento) {
		String configured= memento.getString(TAG_CONFIGURED);
		if (configured == null)
			return false;
		
		fConfigured= Boolean.valueOf(configured).booleanValue();
		fLocalWorkingSetManager.restoreState(memento.getChild(TAG_LOCAL_WORKING_SET_MANAGER));

		IMemento[] actives= memento.getChildren(TAG_ACTIVE_WORKING_SET);
		for (int i= 0; i < actives.length; i++) {
			String name= actives[i].getString(TAG_WORKING_SET_NAME);
			if (name != null) {
				IWorkingSet ws= fLocalWorkingSetManager.getWorkingSet(name);
				if (ws == null) {
					ws= PlatformUI.getWorkbench().getWorkingSetManager().getWorkingSet(name);
				}
				if (ws != null) {
					fActiveWorkingSets.add(ws);
				}
			}
		}
		return true;
	}
	private void workingSetManagerChanged(PropertyChangeEvent event) {
		String property= event.getProperty();
		if (IWorkingSetManager.CHANGE_WORKING_SET_UPDATER_INSTALLED.equals(property) && event.getSource() == fLocalWorkingSetManager) {
			IWorkingSetUpdater updater= (IWorkingSetUpdater)event.getNewValue();
			if (updater instanceof OthersWorkingSetUpdater) {
				fOthersWorkingSetUpdater= (OthersWorkingSetUpdater)updater;
				fOthersWorkingSetUpdater.init(this);
			}
			return;
		}
		// don't handle working sets not managed by the model
		if (!isAffected(event))
			return;

		if (IWorkingSetManager.CHANGE_WORKING_SET_CONTENT_CHANGE.equals(property)) {
			IWorkingSet workingSet= (IWorkingSet)event.getNewValue();
			IAdaptable[] elements= fElementMapper.refresh(workingSet);
			if (elements != null) {
				fireEvent(event);
			}
		} else if (IWorkingSetManager.CHANGE_WORKING_SET_REMOVE.equals(property)) {
			IWorkingSet workingSet= (IWorkingSet)event.getOldValue();
			List elements= new ArrayList(fActiveWorkingSets);
			elements.remove(workingSet);
			setActiveWorkingSets((IWorkingSet[])elements.toArray(new IWorkingSet[elements.size()]));
		} else if (IWorkingSetManager.CHANGE_WORKING_SET_NAME_CHANGE.equals(property)) {
			fireEvent(event);
		}
	}

	private void fireEvent(PropertyChangeEvent event) {
		Object[] listeners= fListeners.getListeners();
		for (int i= 0; i < listeners.length; i++) {
			((IPropertyChangeListener)listeners[i]).propertyChange(event);
		}
	}

	private boolean isAffected(PropertyChangeEvent event) {
		if (fActiveWorkingSets == null)
			return false;
		Object oldValue= event.getOldValue();
		Object newValue= event.getNewValue();
		if ((oldValue != null && fActiveWorkingSets.contains(oldValue)) 
				|| (newValue != null && fActiveWorkingSets.contains(newValue))) {
			return true;
		}
		return false;
	}

	public boolean isActiveWorkingSet(IWorkingSet changedWorkingSet) {
		return fActiveWorkingSets.contains(changedWorkingSet);
	}
}
