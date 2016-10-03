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
 * Describes a JavaScript search query. A query is described by giving a scope, a
 * scope description, what kind of match to search for (reference, declarations,
 * etc) and either a JavaScript element or a string and what kind of element to search
 * for (type, field, etc). What exactly it means to, for example, to search for
 * "references to type foo" is up to query participants. For example, a
 * participant might consider the "class" attribute of an extension in a
 * plugin.xml file to be a reference to the class mentioned in the attribute.
 * </p>
 * 
 * <p>
 * This class is not intended to be instantiated or subclassed by clients.
 * </p>
 * 
 *
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves. */
public abstract class QuerySpecification {
	private IJavaScriptSearchScope fScope;
	private int fLimitTo;
	private String fScopeDescription;

	QuerySpecification(int limitTo, IJavaScriptSearchScope scope, String scopeDescription) {
		fScope= scope;
		fLimitTo= limitTo;
		fScopeDescription= scopeDescription;
	}

	/**
	 * Returns the search scope to be used in the query.
	 * @return The search scope.
	 */
	public IJavaScriptSearchScope getScope() {
		return fScope;
	}
	
	/**
	 * Returns a human readable description of the search scope.
	 * @return A description of the search scope. 
	 * @see QuerySpecification#getScope()
	 */
	public String getScopeDescription() {
		return fScopeDescription;
	}
	
	/**
	 * Returns what kind of occurrences the query should look for.
	 * @return Whether to search for reference, declaration, etc.
	 * @see org.eclipse.wst.jsdt.core.search.IJavaScriptSearchConstants
	 */
	public int getLimitTo() {
		return fLimitTo;
	}

}
