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
package org.eclipse.wst.jsdt.internal.corext.refactoring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.search.SearchMatch;
import org.eclipse.wst.jsdt.internal.corext.util.SearchUtils;

public class SearchResultGroup {

	private final IResource fResouce;
	private final List fSearchMatches;
	
	public SearchResultGroup(IResource res, SearchMatch[] matches){
		Assert.isNotNull(matches);
		fResouce= res;
		fSearchMatches= new ArrayList(Arrays.asList(matches));
	}

	public void add(SearchMatch match) {
		Assert.isNotNull(match);
		fSearchMatches.add(match);		
	}
	
	public IResource getResource() {
		return fResouce;
	}
	
	public SearchMatch[] getSearchResults() {
		return (SearchMatch[]) fSearchMatches.toArray(new SearchMatch[fSearchMatches.size()]);
	}
	
	public static IResource[] getResources(SearchResultGroup[] searchResultGroups){
		Set resourceSet= new HashSet(searchResultGroups.length);
		for (int i= 0; i < searchResultGroups.length; i++) {
			resourceSet.add(searchResultGroups[i].getResource());
		}
		return (IResource[]) resourceSet.toArray(new IResource[resourceSet.size()]);
	}
	
	public IJavaScriptUnit getCompilationUnit(){
		if (getSearchResults() == null || getSearchResults().length == 0)
			return null;
		return SearchUtils.getCompilationUnit(getSearchResults()[0]);
	}
	
	public String toString() {
		StringBuffer buf= new StringBuffer(fResouce.getFullPath().toString());
		buf.append('\n');
		for (int i= 0; i < fSearchMatches.size(); i++) {
			SearchMatch match= (SearchMatch) fSearchMatches.get(i);
			buf.append("  ").append(match.getOffset()).append(", ").append(match.getLength()); //$NON-NLS-1$//$NON-NLS-2$
			buf.append(match.getAccuracy() == SearchMatch.A_ACCURATE ? "; acc" : "; inacc"); //$NON-NLS-1$//$NON-NLS-2$
			if (match.isInsideDocComment())
				buf.append("; inDoc"); //$NON-NLS-1$
			if (match.getElement() instanceof IJavaScriptElement)
				buf.append("; in: ").append(((IJavaScriptElement) match.getElement()).getElementName()); //$NON-NLS-1$
			buf.append('\n');
		}
		return buf.toString();
	}
}
