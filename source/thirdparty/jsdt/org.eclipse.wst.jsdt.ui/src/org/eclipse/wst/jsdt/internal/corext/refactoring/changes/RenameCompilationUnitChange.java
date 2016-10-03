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
package org.eclipse.wst.jsdt.internal.corext.refactoring.changes;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.corext.refactoring.AbstractJavaElementRenameChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;

public final class RenameCompilationUnitChange extends AbstractJavaElementRenameChange {

	public RenameCompilationUnitChange(IJavaScriptUnit unit, String newName) {
		this(unit.getResource().getFullPath(), unit.getElementName(), newName, IResource.NULL_STAMP);
		Assert.isTrue(!unit.isReadOnly(), "compilation unit must not be read-only"); //$NON-NLS-1$
	}

	private RenameCompilationUnitChange(IPath resourcePath, String oldName, String newName, long stampToRestore) {
		super(resourcePath, oldName, newName, stampToRestore);
	}

	protected IPath createNewPath() {
		final IPath path= getResourcePath();
		if (path.getFileExtension() != null)
			return path.removeFileExtension().removeLastSegments(1).append(getNewName());
		else
			return path.removeLastSegments(1).append(getNewName());
	}

	protected Change createUndoChange(long stampToRestore) throws JavaScriptModelException {
		return new RenameCompilationUnitChange(createNewPath(), getNewName(), getOldName(), stampToRestore);
	}

	protected void doRename(IProgressMonitor pm) throws CoreException {
		IJavaScriptUnit cu= (IJavaScriptUnit) getModifiedElement();
		if (cu != null)
			cu.rename(getNewName(), false, pm);
	}

	public String getName() {
		return Messages.format(RefactoringCoreMessages.RenameCompilationUnitChange_name, new String[] { getOldName(), getNewName()});
	}

	public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException {
		return super.isValid(pm, READ_ONLY | SAVE_IF_DIRTY);
	}
}
