/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.filters;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.wst.jsdt.core.IClassFile;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;


/**
 * Filters out all compilation units and class files elements.
 */
public class JavaFileFilter  extends ViewerFilter {
	
	/**
	 * Returns the result of this filter, when applied to the
	 * given inputs.
	 *
	 * @return Returns true if element should be included in filtered set
	 */
	public boolean select(Viewer viewer, Object parent, Object element) {
		if (element instanceof IJavaScriptUnit)
			return false;
		if (element instanceof IClassFile)
			return false;
			
		if (element instanceof IPackageFragment) {
			try {
				return ((IPackageFragment)element).getNonJavaScriptResources().length > 0;
			} catch (JavaScriptModelException ex) {
				return true;
			}
		}
		return !(element instanceof IFile && JavaScriptCore.isJavaScriptLikeFileName(((IFile) element).getName()));
	}
}
