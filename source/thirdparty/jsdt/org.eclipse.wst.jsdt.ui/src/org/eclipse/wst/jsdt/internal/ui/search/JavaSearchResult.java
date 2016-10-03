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
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.IEditorMatchAdapter;
import org.eclipse.search.ui.text.IFileMatchAdapter;
import org.eclipse.search.ui.text.Match;
import org.eclipse.search.ui.text.MatchFilter;
import org.eclipse.ui.IEditorPart;
import org.eclipse.wst.jsdt.core.IClassFile;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IParent;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.ui.search.IMatchPresentation;

public class JavaSearchResult extends AbstractTextSearchResult implements IEditorMatchAdapter, IFileMatchAdapter {
	
	private static final Match[] NO_MATCHES= new Match[0];
	
	private final JavaSearchQuery fQuery;
	private final Map fElementsToParticipants;

	public JavaSearchResult(JavaSearchQuery query) {
		fQuery= query;
		fElementsToParticipants= new HashMap();
		setActiveMatchFilters(JavaMatchFilter.getLastUsedFilters());
	}

	public ImageDescriptor getImageDescriptor() {
		return fQuery.getImageDescriptor();
	}

	public String getLabel() {
		return fQuery.getResultLabel(getMatchCount());
	}

	public String getTooltip() {
		return getLabel();
	}
	
	public Match[] computeContainedMatches(AbstractTextSearchResult result, IEditorPart editor) {
		return computeContainedMatches(editor.getEditorInput());
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.text.AbstractTextSearchResult#setMatchFilters(org.eclipse.search.ui.text.MatchFilter[])
	 */
	public void setActiveMatchFilters(MatchFilter[] filters) {
		super.setActiveMatchFilters(filters);
		JavaMatchFilter.setLastUsedFilters(filters);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.text.AbstractTextSearchResult#getAllMatchFilters()
	 */
	public MatchFilter[] getAllMatchFilters() {
		return JavaMatchFilter.allFilters(fQuery);
	}

	public Match[] computeContainedMatches(AbstractTextSearchResult result, IFile file) {
		return computeContainedMatches(file);
	}
	
	private Match[] computeContainedMatches(IAdaptable adaptable) {
		IJavaScriptElement javaElement= (IJavaScriptElement) adaptable.getAdapter(IJavaScriptElement.class);
		Set matches= new HashSet();
		if (javaElement != null) {
			collectMatches(matches, javaElement);
		}
		IFile file= (IFile) adaptable.getAdapter(IFile.class);
		if (file != null) {
			collectMatches(matches, file);
		}
		if (!matches.isEmpty()) {
			return (Match[]) matches.toArray(new Match[matches.size()]);
		}
		return NO_MATCHES;
	}
	
	
	private void collectMatches(Set matches, IFile element) {
		Match[] m= getMatches(element);
		if (m.length != 0) {
			for (int i= 0; i < m.length; i++) {
				matches.add(m[i]);
			}
		}
	}
	
	private void collectMatches(Set matches, IJavaScriptElement element) {
		Match[] m= getMatches(element);
		if (m.length != 0) {
			for (int i= 0; i < m.length; i++) {
				matches.add(m[i]);
			}
		}
		if (element instanceof IParent) {
			IParent parent= (IParent) element;
			try {
				IJavaScriptElement[] children= parent.getChildren();
				for (int i= 0; i < children.length; i++) {
					collectMatches(matches, children[i]);
				}
			} catch (JavaScriptModelException e) {
				// we will not be tracking these results
			}
		}
	}
	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.ISearchResultCategory#getFile(java.lang.Object)
	 */
	public IFile getFile(Object element) {
		if (element instanceof IJavaScriptElement) {
			IJavaScriptElement javaElement= (IJavaScriptElement) element;
			IJavaScriptUnit cu= (IJavaScriptUnit) javaElement.getAncestor(IJavaScriptElement.JAVASCRIPT_UNIT);
			if (cu != null) {
				return (IFile) cu.getResource();
			} else {
				IClassFile cf= (IClassFile) javaElement.getAncestor(IJavaScriptElement.CLASS_FILE);
				if (cf != null)
					return (IFile) cf.getResource();
			}
			return null;
		}
		if (element instanceof IFile)
			return (IFile) element;
		return null;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.search2.ui.text.IStructureProvider#isShownInEditor(org.eclipse.search2.ui.text.Match,
	 *      org.eclipse.ui.IEditorPart)
	 */
	public boolean isShownInEditor(Match match, IEditorPart editor) {
		Object element= match.getElement();
		if (element instanceof IJavaScriptElement) {
			element= ((IJavaScriptElement) element).getOpenable(); // class file or compilation unit 
			return element != null && element.equals(editor.getEditorInput().getAdapter(IJavaScriptElement.class));
		} else if (element instanceof IFile) {
			return element.equals(editor.getEditorInput().getAdapter(IFile.class));
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.ISearchResult#getQuery()
	 */
	public ISearchQuery getQuery() {
		return fQuery;
	}
	
	synchronized IMatchPresentation getSearchParticpant(Object element) {
		return (IMatchPresentation) fElementsToParticipants.get(element);
	}

	boolean addMatch(Match match, IMatchPresentation participant) {
		Object element= match.getElement();
		if (fElementsToParticipants.get(element) != null) {
			// TODO must access the participant id / label to properly report the error.
			JavaScriptPlugin.log(new Status(IStatus.WARNING, JavaScriptPlugin.getPluginId(), 0, "A second search participant was found for an element", null)); //$NON-NLS-1$
			return false;
		}
		fElementsToParticipants.put(element, participant);
		addMatch(match);
		return true;
	}
	
	public void removeAll() {
		synchronized(this) {
			fElementsToParticipants.clear();
		}
		super.removeAll();
	}
	
	public void removeMatch(Match match) {
		synchronized(this) {
			if (getMatchCount(match.getElement()) == 1)
				fElementsToParticipants.remove(match.getElement());
		}
		super.removeMatch(match);
	}
	public IFileMatchAdapter getFileMatchAdapter() {
		return this;
	}
	
	public IEditorMatchAdapter getEditorMatchAdapter() {
		return this;
	}

}
