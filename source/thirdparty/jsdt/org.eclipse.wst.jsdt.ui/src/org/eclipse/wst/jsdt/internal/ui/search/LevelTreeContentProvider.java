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

package org.eclipse.wst.jsdt.internal.ui.search;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.wst.jsdt.core.IClassFile;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.ui.StandardJavaScriptElementContentProvider;

public class LevelTreeContentProvider extends JavaSearchContentProvider implements ITreeContentProvider {
	private Map fChildrenMap;
	private StandardJavaScriptElementContentProvider fContentProvider;
	
	public static final int LEVEL_TYPE= 1;
	public static final int LEVEL_FILE= 2;
	public static final int LEVEL_PACKAGE= 3;
	public static final int LEVEL_PROJECT= 4;
	
	private static final int[][] JAVA_ELEMENT_TYPES= {{IJavaScriptElement.TYPE},
			{IJavaScriptElement.CLASS_FILE, IJavaScriptElement.JAVASCRIPT_UNIT},
			{IJavaScriptElement.PACKAGE_FRAGMENT},
			{IJavaScriptElement.JAVASCRIPT_PROJECT, IJavaScriptElement.PACKAGE_FRAGMENT_ROOT},
			{IJavaScriptElement.JAVASCRIPT_MODEL}};
	private static final int[][] RESOURCE_TYPES= {
			{}, 
			{IResource.FILE},
			{IResource.FOLDER}, 
			{IResource.PROJECT}, 
			{IResource.ROOT}};
	
	private static final int MAX_LEVEL= JAVA_ELEMENT_TYPES.length - 1;
	private int fCurrentLevel;
	static class FastJavaElementProvider extends StandardJavaScriptElementContentProvider {
		public Object getParent(Object element) {
			return internalGetParent(element);
		}
	}

	public LevelTreeContentProvider(JavaSearchResultPage page, int level) {
		super(page);
		fCurrentLevel= level;
		fContentProvider= new FastJavaElementProvider();
	}

	public Object getParent(Object child) {
		Object possibleParent= internalGetParent(child);
		if (possibleParent instanceof IJavaScriptElement) {
			IJavaScriptElement javaElement= (IJavaScriptElement) possibleParent;
			for (int j= fCurrentLevel; j < MAX_LEVEL + 1; j++) {
				for (int i= 0; i < JAVA_ELEMENT_TYPES[j].length; i++) {
					if (javaElement.getElementType() == JAVA_ELEMENT_TYPES[j][i]) {
						return null;
					}
				}
			}
		} else if (possibleParent instanceof IResource) {
			IResource resource= (IResource) possibleParent;
			for (int j= fCurrentLevel; j < MAX_LEVEL + 1; j++) {
				for (int i= 0; i < RESOURCE_TYPES[j].length; i++) {
					if (resource.getType() == RESOURCE_TYPES[j][i]) {
						return null;
					}
				}
			}
		}
		if (fCurrentLevel != LEVEL_FILE && child instanceof IType) {
			IType type= (IType) child;
			if (possibleParent instanceof IJavaScriptUnit
					|| possibleParent instanceof IClassFile)
				possibleParent= type.getPackageFragment();
		}
		return possibleParent;
	}

	private Object internalGetParent(Object child) {
		return fContentProvider.getParent(child);
	}

	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	protected synchronized void initialize(JavaSearchResult result) {
		super.initialize(result);
		fChildrenMap= new HashMap();
		if (result != null) {
			Object[] elements= result.getElements();
			for (int i= 0; i < elements.length; i++) {
				if (getPage().getDisplayedMatchCount(elements[i]) > 0) {
					insert(null, null, elements[i]);
				}
			}
		}
	}

	protected void insert(Map toAdd, Set toUpdate, Object child) {
		Object parent= getParent(child);
		while (parent != null) {
			if (insertChild(parent, child)) {
				if (toAdd != null)
					insertInto(parent, child, toAdd);
			} else {
				if (toUpdate != null)
					toUpdate.add(parent);
				return;
			}
			child= parent;
			parent= getParent(child);
		}
		if (insertChild(getSearchResult(), child)) {
			if (toAdd != null)
				insertInto(getSearchResult(), child, toAdd);
		}
	}

	private boolean insertChild(Object parent, Object child) {
		return insertInto(parent, child, fChildrenMap);
	}

	private boolean insertInto(Object parent, Object child, Map map) {
		Set children= (Set) map.get(parent);
		if (children == null) {
			children= new HashSet();
			map.put(parent, children);
		}
		return children.add(child);
	}

	protected void remove(Set toRemove, Set toUpdate, Object element) {
		// precondition here:  fResult.getMatchCount(child) <= 0
	
		if (hasChildren(element)) {
			if (toUpdate != null)
				toUpdate.add(element);
		} else {
			if (getPage().getDisplayedMatchCount(element) == 0) {
				fChildrenMap.remove(element);
				Object parent= getParent(element);
				if (parent != null) {
					if (removeFromSiblings(element, parent)) {
						remove(toRemove, toUpdate, parent);
					}
				} else {
					if (removeFromSiblings(element, getSearchResult())) {
						if (toRemove != null)
							toRemove.add(element);
					}
				}
			} else {
				if (toUpdate != null) {
					toUpdate.add(element);
				}
			}
		}
	}

	/**
	 * @param element
	 * @param parent
	 * @return returns true if it really was a remove (i.e. element was a child of parent).
	 */
	private boolean removeFromSiblings(Object element, Object parent) {
		Set siblings= (Set) fChildrenMap.get(parent);
		if (siblings != null) {
			return siblings.remove(element);
		} else {
			return false;
		}
	}

	public Object[] getChildren(Object parentElement) {
		Set children= (Set) fChildrenMap.get(parentElement);
		if (children == null)
			return EMPTY_ARR;
		int limit= getPage().getElementLimit().intValue();
		if (limit != -1 && limit < children.size()) {
			Object[] limitedArray= new Object[limit];
			Iterator iterator= children.iterator();
			for (int i= 0; i < limit; i++) {
				limitedArray[i]= iterator.next();
			}
			return limitedArray;
		}
		
		return children.toArray();
	}

	public boolean hasChildren(Object element) {
		Set children= (Set) fChildrenMap.get(element);
		return children != null && !children.isEmpty();
	}

	public synchronized void elementsChanged(Object[] updatedElements) {
		if (getSearchResult() == null)
			return;
		
		AbstractTreeViewer viewer= (AbstractTreeViewer) getPage().getViewer();

		Set toRemove= new HashSet();
		Set toUpdate= new HashSet();
		Map toAdd= new HashMap();
		for (int i= 0; i < updatedElements.length; i++) {
			if (getPage().getDisplayedMatchCount(updatedElements[i]) > 0)
				insert(toAdd, toUpdate, updatedElements[i]);
			else
				remove(toRemove, toUpdate, updatedElements[i]);
		}
		
		viewer.remove(toRemove.toArray());
		for (Iterator iter= toAdd.keySet().iterator(); iter.hasNext();) {
			Object parent= iter.next();
			HashSet children= (HashSet) toAdd.get(parent);
			viewer.add(parent, children.toArray());
		}
		for (Iterator elementsToUpdate= toUpdate.iterator(); elementsToUpdate.hasNext();) {
			viewer.refresh(elementsToUpdate.next());
		}
		
	}
	
	public void clear() {
		initialize(getSearchResult());
		getPage().getViewer().refresh();
	}

	public void setLevel(int level) {
		fCurrentLevel= level;
		initialize(getSearchResult());
		getPage().getViewer().refresh();
	}

}
