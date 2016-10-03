/*******************************************************************************
 * Copyright (c) 2006, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.compare;

import org.eclipse.core.resources.IFile;
import org.eclipse.team.ui.history.ElementLocalHistoryPageSource;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IMember;

public class JavaElementHistoryPageSource extends ElementLocalHistoryPageSource {
	
	private static JavaElementHistoryPageSource instance;

	public static JavaElementHistoryPageSource getInstance() {
		if (instance == null)
			instance = new JavaElementHistoryPageSource();
		return instance;
	}
	
	/**
	 * Returns true if the given IJavaScriptElement maps to a JavaNode.
	 * The JavaHistoryAction uses this function to determine whether
	 * a selected Java element can be replaced by some piece of
	 * code from the local history.
	 */
	public static boolean hasEdition(IJavaScriptElement je) {

		if (je instanceof IMember && ((IMember)je).isBinary())
			return false;
			
		switch (je.getElementType()) {
		case IJavaScriptElement.JAVASCRIPT_UNIT:
		case IJavaScriptElement.TYPE:
		case IJavaScriptElement.FIELD:
		case IJavaScriptElement.METHOD:
		case IJavaScriptElement.INITIALIZER:
		case IJavaScriptElement.IMPORT_CONTAINER:
		case IJavaScriptElement.IMPORT_DECLARATION:
			return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.history.ElementLocalHistoryPageSource#getFile(java.lang.Object)
	 */
	public IFile getFile(Object input) {
		// extract CU from input
		IJavaScriptUnit cu= null;
		if (input instanceof IJavaScriptUnit)
			cu= (IJavaScriptUnit) input;
		else if (input instanceof IMember)
			cu= ((IMember)input).getJavaScriptUnit();
			
		if (cu == null || !cu.exists())
			return null;
			
		// get to original CU
		cu= cu.getPrimary();
			
		// find underlying file
		IFile file= (IFile) cu.getResource();
		if (file != null && file.exists())
			return file;
		return null;
	}
}
