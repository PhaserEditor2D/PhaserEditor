/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
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
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ui.ide.undo.ResourceDescription;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;


public class UndoDeleteResourceChange extends Change {

	private final ResourceDescription fResourceDescription;

	public UndoDeleteResourceChange(ResourceDescription resourceDescription) {
		fResourceDescription= resourceDescription;
	}
	
	public void initializeValidationData(IProgressMonitor pm) {
		
	}
	
	public Object getModifiedElement() {
		return null;
	}

	public String getName() {
		return Messages.format(RefactoringCoreMessages.UndoDeleteResourceChange_change_name, fResourceDescription.getName()); 
	}

	public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		if (! fResourceDescription.isValid()) {
			return RefactoringStatus.createFatalErrorStatus(
					Messages.format(
							RefactoringCoreMessages.UndoDeleteResourceChange_cannot_restore,
							fResourceDescription.getName()));
		}
		
		if (fResourceDescription.verifyExistence(true)) {
			return RefactoringStatus.createFatalErrorStatus(
					Messages.format(
							RefactoringCoreMessages.UndoDeleteResourceChange_already_exists,
							fResourceDescription.getName()));
		}
		
		return new RefactoringStatus();
	}

	public Change perform(IProgressMonitor pm) throws CoreException {
		IResource created= fResourceDescription.createResource(pm);
		created.refreshLocal(IResource.DEPTH_INFINITE, new SubProgressMonitor(pm, 1));
		if (created instanceof IFile) {
			return new DeleteFileChange((IFile) created, false);
		} else if (created instanceof IFolder) {
			return new DeleteFolderChange((IFolder) created, false);
		} else {
			return null; // should not happen
		}
	}
	
	public String toString() {
		return "Remove " + fResourceDescription.getName(); //$NON-NLS-1$
	}
}
