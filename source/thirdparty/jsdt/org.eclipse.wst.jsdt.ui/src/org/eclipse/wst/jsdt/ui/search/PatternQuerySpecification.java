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
package org.eclipse.wst.jsdt.ui.search;

import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchScope;


/**
 * <p>
 * Describes a search query by giving a textual pattern to search for.
 * </p>
 * <p>
 * This class is not intended to be instantiated or subclassed by clients.
 * </p>
 * 
 * @see org.eclipse.wst.jsdt.ui.search.QuerySpecification
 * 
 *
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves. */
public class PatternQuerySpecification extends QuerySpecification {
	private String fPattern;
	private int fSearchFor;
	private boolean fCaseSensitive;

	/**
	 * @param pattern
	 *            The string that the query should search for.
	 * @param searchFor
	 *            What kind of <code>IJavaScriptElement</code> the query should search for.
	 * @param caseSensitive
	 *            Whether the query should be case sensitive.
	 * @param limitTo
	 *            The kind of occurrence the query should search for.
	 * @param scope
	 *            The scope to search in.
	 * @param scopeDescription
	 *            A human readable description of the search scope.
	 * 
	 * @see org.eclipse.wst.jsdt.core.search.SearchPattern#createPattern(java.lang.String, int, int, int)
	 */
	public PatternQuerySpecification(String pattern, int searchFor, boolean caseSensitive, int limitTo, IJavaScriptSearchScope scope, String scopeDescription) {
		super(limitTo, scope, scopeDescription);
		fPattern= pattern;
		fSearchFor= searchFor;
		fCaseSensitive= caseSensitive;
	}

	/**
	 * Whether the query should be case sensitive.
	 * @return Whether the query should be case sensitive.
	 */
	public boolean isCaseSensitive() {
		return fCaseSensitive;
	}

	/**
	 * Returns the search pattern the query should search for. 
	 * @return the search pattern
	 * @see org.eclipse.wst.jsdt.core.search.SearchPattern#createPattern(java.lang.String, int, int, int)
	 */
	public String getPattern() {
		return fPattern;
	}

	/**
	 * Returns what kind of <code>IJavaScriptElement</code> the query should search for.
	 * 
	 * @return The kind of <code>IJavaScriptElement</code> to search for.
	 * 
	 * @see org.eclipse.wst.jsdt.core.search.IJavaScriptSearchConstants
	 */
	public int getSearchFor() {
		return fSearchFor;
	}
}
