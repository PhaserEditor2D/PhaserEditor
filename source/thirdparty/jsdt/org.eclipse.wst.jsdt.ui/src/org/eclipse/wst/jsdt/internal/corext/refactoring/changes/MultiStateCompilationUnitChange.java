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
package org.eclipse.wst.jsdt.internal.corext.refactoring.changes;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.ltk.core.refactoring.MultiStateTextFileChange;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;

/**
 * Multi state compilation unit change for composite refactorings.
 * 
 * 
 */
public final class MultiStateCompilationUnitChange extends MultiStateTextFileChange {

	/** The compilation unit */
	private final IJavaScriptUnit fUnit;

	/**
	 * Creates a new multi state compilation unit change.
	 * 
	 * @param name
	 *            the name of the change
	 * @param unit
	 *            the compilation unit
	 */
	public MultiStateCompilationUnitChange(final String name, final IJavaScriptUnit unit) {
		super(name, (IFile) unit.getResource());

		fUnit= unit;

		setTextType("js"); //$NON-NLS-1$
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.Change#getAdapter(java.lang.Class)
	 */
	public final Object getAdapter(final Class adapter) {

		if (IJavaScriptUnit.class.equals(adapter))
			return fUnit;

		return super.getAdapter(adapter);
	}

	/**
	 * Returns the compilation unit.
	 * 
	 * @return the compilation unit
	 */
	public final IJavaScriptUnit getCompilationUnit() {
		return fUnit;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getName() {
		return Messages.format(RefactoringCoreMessages.MultiStateCompilationUnitChange_name_pattern, new String[] { fUnit.getElementName(), getPath(fUnit.getResource()) });
	}

	/**
	 * Returns the path of the resource to display.
	 * 
	 * @param resource
	 *            the resource
	 * @return the path
	 */
	private String getPath(IResource resource) {
		final StringBuffer buffer= new StringBuffer(resource.getProject().getName());
		final String path= resource.getParent().getProjectRelativePath().toString();
		if (path.length() > 0) {
			buffer.append('/');
			buffer.append(path);
		}
		return buffer.toString();
	}
}
