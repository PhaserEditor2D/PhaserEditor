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

import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchScope;

/**
 * <p>
 * Describes a search query by giving the {@link IJavaScriptElement} to search
 * for.
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
public class ElementQuerySpecification extends QuerySpecification {
	private IJavaScriptElement fElement;

	/**
	 * A constructor.
	 * @param javaElement The JavaScript element the query should search for.
	 * @param limitTo		  The kind of occurrence the query should search for.
	 * @param scope		  The scope to search in.
	 * @param scopeDescription A human readable description of the search scope.
	 */
	public ElementQuerySpecification(IJavaScriptElement javaElement, int limitTo, IJavaScriptSearchScope scope, String scopeDescription) {
		super(limitTo, scope, scopeDescription);
		fElement= javaElement;
	}
	
	/**
	 * Returns the element to search for.
	 * @return The element to search for.
	 */
	public IJavaScriptElement getElement() {
		return fElement;
	}
}
