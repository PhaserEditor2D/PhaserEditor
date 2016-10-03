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
package org.eclipse.wst.jsdt.internal.ui.javadocexport;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;

public class JavadocMemberContentProvider implements ITreeContentProvider {

	/*
	 * @see ITreeContentProvider#getChildren(Object)
	 */
	public Object[] getChildren(Object parentElement) {
		return new Object[0];
	}

	/*
	 * @see ITreeContentProvider#getParent(Object)
	 */
	public Object getParent(Object element) {
		//@test
		//System.out.println(element.getClass().toString());
		if (element instanceof IPackageFragment)
			return ((IPackageFragment) element).getParent();
		return null;
	}

	/*
	 * @see ITreeContentProvider#hasChildren(Object)
	 */
	public boolean hasChildren(Object element) {
		try {
			if (element instanceof IPackageFragment) {
				IPackageFragment iPackageFragment= (IPackageFragment) element;
				return (iPackageFragment.getChildren().length > 0);
			}
		} catch (JavaScriptModelException e) {
		}
		return false;
	}

	/*
	 * @see IStructuredContentProvider#getElements(Object)
	 */
	public Object[] getElements(Object inputElement) {
		try {
			if (inputElement instanceof IPackageFragment) {
				IJavaScriptUnit[] cu= ((IPackageFragment) inputElement).getJavaScriptUnits();
				return cu;
			}
		} catch (JavaScriptModelException e) {
			JavaScriptPlugin.log(e);
		}
		return new Object[0];
	}

	/*
	 * @see IContentProvider#dispose()
	 */
	public void dispose() {
	}

	/*
	 * @see IContentProvider#inputChanged(Viewer, Object, Object)
	 */
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}

}
