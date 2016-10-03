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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.search.ui.IQueryListener;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.search.ui.text.Match;
import org.eclipse.wst.jsdt.core.ElementChangedEvent;
import org.eclipse.wst.jsdt.core.IElementChangedListener;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptElementDelta;
import org.eclipse.wst.jsdt.core.JavaScriptCore;

public class SearchResultUpdater implements IElementChangedListener, IQueryListener {

	JavaSearchResult fResult;
	private static final int REMOVED_FLAGS= IJavaScriptElementDelta.F_MOVED_TO | 
									IJavaScriptElementDelta.F_REMOVED_FROM_CLASSPATH |
									IJavaScriptElementDelta.F_CLOSED |
									IJavaScriptElementDelta.F_CONTENT;
	
	public SearchResultUpdater(JavaSearchResult result) {
		fResult= result;
		NewSearchUI.addQueryListener(this);
		JavaScriptCore.addElementChangedListener(this);
		// TODO make this work with resources
	}

	public void elementChanged(ElementChangedEvent event) {
		//long t0= System.currentTimeMillis();
		IJavaScriptElementDelta delta= event.getDelta();
		Set removedElements= new HashSet();
		Set potentiallyRemovedElements= new HashSet();
		collectRemoved(potentiallyRemovedElements, removedElements, delta);
		if (removedElements.size() > 0)
			handleRemoved(removedElements);
		if (potentiallyRemovedElements.size() > 0)
			handleRemoved(potentiallyRemovedElements);
		//System.out.println(this+"handled delta in: "+(System.currentTimeMillis()-t0));
	}

	private void handleRemoved(Set removedElements) {
		Object[] elements= fResult.getElements();
		for (int i= 0; i < elements.length; i++) {
			if (isContainedInRemoved(removedElements, elements[i])) {
				if (elements[i] instanceof IJavaScriptElement) {
					IJavaScriptElement je= (IJavaScriptElement)elements[i];
					if (!je.exists()) {
						//System.out.println("removing: "+je+" in "+fResult.getUserData());
						Match[] matches= fResult.getMatches(elements[i]);
						for (int j= 0; j < matches.length; j++) {
							fResult.removeMatch(matches[j]);
						}
					}
				} else if (elements[i] instanceof IResource) {
					IResource resource= (IResource)elements[i];
					if (!resource.exists()) {
						//System.out.println("removing: "+resource+" in "+fResult.getUserData());
						Match[] matches= fResult.getMatches(elements[i]);
						for (int j= 0; j < matches.length; j++) {
							fResult.removeMatch(matches[j]);
						}
					}
					
				}
			}
		}
	}

	private boolean isContainedInRemoved(Set removedElements, Object object) {
		for (Iterator elements= removedElements.iterator(); elements.hasNext();) {
			if (isParentOf(elements.next(), object))
				return true;
		}
		return false;
	}

	private boolean isParentOf(Object ancestor, Object descendant) {
		while (descendant != null && !ancestor.equals(descendant))
			descendant= getParent(descendant);
		return descendant != null;
	}

	private Object getParent(Object object) {
		if (object instanceof IJavaScriptElement)
			return ((IJavaScriptElement)object).getParent();
		else if (object instanceof IResource)
			return ((IResource)object).getParent();
		return null;
	}

	private void collectRemoved(Set potentiallyRemovedSet, Set removedElements, IJavaScriptElementDelta delta) {
		if (delta.getKind() == IJavaScriptElementDelta.REMOVED) 
			removedElements.add(delta.getElement());
		else if (delta.getKind() == IJavaScriptElementDelta.CHANGED) {
			int flags= delta.getFlags();
			if ((flags & REMOVED_FLAGS) != 0) {
				potentiallyRemovedSet.add(delta.getElement());
			} else {
				IJavaScriptElementDelta[] childDeltas= delta.getAffectedChildren();
				for (int i= 0; i < childDeltas.length; i++) {
					collectRemoved(potentiallyRemovedSet, removedElements, childDeltas[i]);
				}
			}
		}
		IResourceDelta[] resourceDeltas= delta.getResourceDeltas();
		if (resourceDeltas != null) {
			for (int i= 0; i < resourceDeltas.length; i++) {
				collectRemovals(removedElements, resourceDeltas[i]);
			}
		}
	}

	public void queryAdded(ISearchQuery query) {
		// don't care
	}

	public void queryRemoved(ISearchQuery query) {
		if (fResult.equals(query.getSearchResult())) {
			JavaScriptCore.removeElementChangedListener(this);
			NewSearchUI.removeQueryListener(this);
		}		
	}

	private void collectRemovals(Set removals, IResourceDelta delta) {
		if (delta.getKind() == IResourceDelta.REMOVED)
			removals.add(delta.getResource());
		else {
			IResourceDelta[] children= delta.getAffectedChildren();
			for (int i= 0; i < children.length; i++) {
				collectRemovals(removals, children[i]);
			}
		}
	}

	public void queryStarting(ISearchQuery query) {
		// not interested
	}

	public void queryFinished(ISearchQuery query) {
		// not interested
	}

}
