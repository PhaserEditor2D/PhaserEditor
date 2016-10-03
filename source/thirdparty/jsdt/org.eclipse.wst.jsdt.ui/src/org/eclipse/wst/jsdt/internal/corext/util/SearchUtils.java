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
package org.eclipse.wst.jsdt.internal.corext.util;

import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.search.SearchEngine;
import org.eclipse.wst.jsdt.core.search.SearchMatch;
import org.eclipse.wst.jsdt.core.search.SearchParticipant;
import org.eclipse.wst.jsdt.core.search.SearchPattern;

public class SearchUtils {

	/**
	 * @param match
	 * @return the enclosing {@link IJavaScriptElement}, or null iff none
	 */
	public static IJavaScriptElement getEnclosingJavaElement(SearchMatch match) {
		Object element = match.getElement();
		if (element instanceof IJavaScriptElement)
			return (IJavaScriptElement) element;
		else
			return null;
	}
	
	/**
	 * @param match
	 * @return the enclosing {@link IJavaScriptUnit} of the given match, or null iff none
	 */
	public static IJavaScriptUnit getCompilationUnit(SearchMatch match) {
		IJavaScriptElement enclosingElement = getEnclosingJavaElement(match);
		if (enclosingElement != null){
			if (enclosingElement instanceof IJavaScriptUnit)
				return (IJavaScriptUnit) enclosingElement;
			IJavaScriptUnit cu= (IJavaScriptUnit) enclosingElement.getAncestor(IJavaScriptElement.JAVASCRIPT_UNIT);
			if (cu != null)
				return cu;
		}
		
		IJavaScriptElement jElement= JavaScriptCore.create(match.getResource());
		if (jElement != null && jElement.exists() && jElement.getElementType() == IJavaScriptElement.JAVASCRIPT_UNIT)
			return (IJavaScriptUnit) jElement;
		return null;
	}
	
	public static SearchParticipant[] getDefaultSearchParticipants() {
		return new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() };
	}
	
    /**
     * Constant for use as matchRule in {@link SearchPattern#createPattern(IJavaScriptElement, int, int)}
     * to get search behavior as of 3.1M3 (all generic instantiations are found).
     */
    public final static int GENERICS_AGNOSTIC_MATCH_RULE= SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE | SearchPattern.R_ERASURE_MATCH;

    /**
     * Returns whether the given pattern is a camel case pattern or not.
     * 
     * @param pattern the pattern to inspect
     * @return whether it is a camel case pattern or not
     */
	public static boolean isCamelCasePattern(String pattern) {
		return SearchPattern.validateMatchRule(
			pattern, 
			SearchPattern.R_CAMELCASE_MATCH) == SearchPattern.R_CAMELCASE_MATCH;
	}
}
