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
package org.eclipse.wst.jsdt.core.search;

import org.eclipse.core.resources.IResource;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;

/**
 * A JavaScript search match that represents a package reference.
 * The element is the inner-most enclosing member that references this package.
 * <p>
 * This class is intended to be instantiated and subclassed by clients.
 * </p>
 * 
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public class PackageReferenceMatch extends SearchMatch {

	/**
	 * Creates a new package reference match.
	 *
	 * @param enclosingElement the inner-most enclosing member that references this package
	 * @param accuracy one of {@link #A_ACCURATE} or {@link #A_INACCURATE}
	 * @param offset the offset the match starts at, or -1 if unknown
	 * @param length the length of the match, or -1 if unknown
	 * @param insideDocComment <code>true</code> if this search match is inside a doc
	 * comment, and <code>false</code> otherwise
	 * @param participant the search participant that created the match
	 * @param resource the resource of the element
	 */
	public PackageReferenceMatch(IJavaScriptElement enclosingElement, int accuracy, int offset, int length, boolean insideDocComment, SearchParticipant participant, IResource resource) {
		super(enclosingElement, accuracy, offset, length, participant, resource);
		setInsideDocComment(insideDocComment);
	}
}
