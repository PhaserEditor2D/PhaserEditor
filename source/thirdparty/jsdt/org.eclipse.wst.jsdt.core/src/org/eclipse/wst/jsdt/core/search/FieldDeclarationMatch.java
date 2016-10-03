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
 * A JavaScript search match that represents a field declaration.
 * The element is an <code>IField</code>.
 * <p>
 * This class is intended to be instantiated and subclassed by clients.
 * </p>
 * 
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public class FieldDeclarationMatch extends SearchMatch {

	/**
	 * Creates a new field declaration match.
	 *
	 * @param element the field declaration
	 * @param accuracy one of A_ACCURATE or A_INACCURATE
	 * @param offset the offset the match starts at, or -1 if unknown
	 * @param length the length of the match, or -1 if unknown
	 * @param participant the search participant that created the match
	 * @param resource the resource of the element
	 */
	public FieldDeclarationMatch(IJavaScriptElement element, int accuracy, int offset, int length, SearchParticipant participant, IResource resource) {
		super(element, accuracy, offset, length, participant, resource);
	}
}
