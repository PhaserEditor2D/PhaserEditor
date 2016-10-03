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
package org.eclipse.wst.jsdt.internal.ui.model;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptorProxy;
import org.eclipse.ltk.core.refactoring.history.RefactoringHistory;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.ui.StandardJavaScriptElementContentProvider;

/**
 * Content provider for Java models.
 * 
 * 
 */
public final class JavaModelContentProvider extends StandardJavaScriptElementContentProvider {

	/** The name of the settings folder */
	private static final String NAME_SETTINGS_FOLDER= ".settings"; //$NON-NLS-1$

	/**
	 * Creates a new java model content provider.
	 */
	public JavaModelContentProvider() {
		super(true);
	}

	/**
	 * {@inheritDoc}
	 */
	public Object[] getChildren(final Object element) {
		if (element instanceof IJavaScriptUnit)
			return NO_CHILDREN;
		else if (element instanceof RefactoringHistory)
			return ((RefactoringHistory) element).getDescriptors();
		else if (element instanceof IJavaScriptProject) {
			final List elements= new ArrayList();
			elements.add(((IJavaScriptProject) element).getProject().getFolder(NAME_SETTINGS_FOLDER));
			final Object[] children= super.getChildren(element);
			for (int index= 0; index < children.length; index++) {
				if (!elements.contains(children[index]))
					elements.add(children[index]);
			}
			return elements.toArray();
		} else if (element instanceof IFolder) {
			final IFolder folder= (IFolder) element;
			try {
				return folder.members();
			} catch (CoreException exception) {
				// Do nothing
			}
		}
		return super.getChildren(element);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean hasChildren(final Object element) {
		if (element instanceof IJavaScriptUnit)
			return false;
		else if (element instanceof RefactoringHistory)
			return true;
		else if (element instanceof RefactoringDescriptorProxy)
			return false;
		else if (element instanceof IFolder)
			return true;
		return super.hasChildren(element);
	}
}
