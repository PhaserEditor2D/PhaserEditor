/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.filters;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.wst.jsdt.ui.ProjectLibraryRoot;


/**
 * The LibraryFilter is a filter used to determine whether
 * the JavaScript Resources node is shown. This node contains libraries.
 */
public class LibraryFilter extends ViewerFilter {
	
	/* (non-Javadoc)
	 * Method declared on ViewerFilter.
	 */
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if (element instanceof ProjectLibraryRoot) {
			return false;
		} //else if (element instanceof JsGlobalScopeContainer.RequiredProjectWrapper) {
//			return false;
//		}
		return true;
	}
}
