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

import org.eclipse.wst.jsdt.core.IJavaScriptElement;

public class OccurrencesGroupKey extends JavaElementLine {
	private boolean fIsWriteAccess;
	private boolean fIsVariable;
	
	/**
	 * Create a new occurrences group key.
	 * 
	 * @param element either an IJavaScriptUnit or an IClassFile
	 * @param line the line number
	 * @param lineContents the line contents
	 * @param isWriteAccess <code>true</code> if it groups writable occurrences
	 * @param isVariable <code>true</code> if it groups variable occurrences
	 */
	public OccurrencesGroupKey(IJavaScriptElement element, int line, String lineContents, boolean isWriteAccess, boolean isVariable) {
		super(element, line, lineContents);
		fIsWriteAccess= isWriteAccess;
		fIsVariable= isVariable;
	}

	public boolean isVariable() {
		return fIsVariable;
	}

	public boolean isWriteAccess() {
		return fIsWriteAccess;
	}

	public void setWriteAccess(boolean isWriteAccess) {
		fIsWriteAccess= isWriteAccess;
	}
}
