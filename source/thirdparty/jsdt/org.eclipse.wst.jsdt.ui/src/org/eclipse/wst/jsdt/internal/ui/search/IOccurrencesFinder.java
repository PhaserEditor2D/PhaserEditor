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

import java.util.Collection;
import java.util.List;

import org.eclipse.jface.text.IDocument;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;

public interface IOccurrencesFinder {
	
	public String initialize(JavaScriptUnit root, int offset, int length);
	
	public List perform();
	
	public String getJobLabel();

	/**
	 * Returns the plural label for this finder with 3 placeholders:
	 * <ul>
	 * <li>{0} for the {@link #getElementName() element name}</li>
	 * <li>{1} for the number of results found</li>
	 * <li>{2} for the scope (name of the compilation unit)</li>
	 *  </ul>
	 * @return the unformatted label
	 */
	public String getUnformattedPluralLabel();
	
	/**
	 * Returns the singular label for this finder with 2 placeholders:
	 * <ul>
	 * <li>{0} for the {@link #getElementName() element name}</li>
	 * <li>{1} for the scope (name of the compilation unit)</li>
	 *  </ul>
	 * @return the unformatted label
	 */
	public String getUnformattedSingularLabel();
	
	/**
	 * Returns the name of the lement to look for or <code>null</code> if the finder hasn't
	 * been initialized yet.
	 * @return the name of the element
	 */
	public String getElementName();
	
	public void collectOccurrenceMatches(IJavaScriptElement element, IDocument document, Collection resultingMatches);
}
