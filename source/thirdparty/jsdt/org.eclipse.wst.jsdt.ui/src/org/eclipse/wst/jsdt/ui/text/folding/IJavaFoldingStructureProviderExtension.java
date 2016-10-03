/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.ui.text.folding;

import org.eclipse.wst.jsdt.core.IJavaScriptElement;

/**
 * Extends {@link IJavaFoldingStructureProvider} with the following
 * functions:
 * <ul>
 * <li>collapsing of comments and members</li>
 * <li>expanding and collapsing of certain JavaScript elements</li>
 * </ul>
 * 
 *
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves. */
public interface IJavaFoldingStructureProviderExtension {
	/**
	 * Collapses all members except for top level types.
	 */
	void collapseMembers();

	/**
	 * Collapses all comments.
	 */
	void collapseComments();

	/**
	 * Collapses the given elements.
	 * 
	 * @param elements the JavaScript elements to collapse (the array and its elements must not be
	 *        modified)
	 */
	void collapseElements(IJavaScriptElement[] elements);

	/**
	 * Expands the given elements.
	 * 
	 * @param elements the JavaScript elements to expand (the array and its elements must not be modified)
	 */
	void expandElements(IJavaScriptElement[] elements);
}
