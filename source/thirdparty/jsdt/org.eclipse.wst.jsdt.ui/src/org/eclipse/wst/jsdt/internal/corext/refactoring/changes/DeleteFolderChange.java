/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ui.ide.undo.ResourceDescription;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;

public class DeleteFolderChange extends AbstractDeleteChange {
	
	private final IPath fPath;
	private final boolean fIsExecuteChange;
	
	public DeleteFolderChange(IFolder folder, boolean isExecuteChange) {
		this(getFolderPath(folder), isExecuteChange);
	}
	
	public DeleteFolderChange(IPath path, boolean isExecuteChange) {
		fPath= path;
		fIsExecuteChange= isExecuteChange;
	}
	
	public static IPath getFolderPath(IFolder folder){
		return folder.getFullPath().removeFirstSegments(ResourcesPlugin.getWorkspace().getRoot().getFullPath().segmentCount());
	}
	
	public static IFolder getFolder(IPath path){
		return ResourcesPlugin.getWorkspace().getRoot().getFolder(path);
	}

	public String getName() {
		return Messages.format(RefactoringCoreMessages.DeleteFolderChange_0, fPath.lastSegment()); 
	}
	
	public Object getModifiedElement() {
		return getFolder(fPath);
	}

	public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException {
		if (fIsExecuteChange) {
			// no need to do additional checking since the dialog
			// already prompts the user if there are dirty
			// or read only files in the folder. The change is
			// currently not used as a undo/redo change
			return super.isValid(pm, NONE);
		} else {
			return super.isValid(pm, READ_ONLY | DIRTY);
		}
	}

	protected Change doDelete(IProgressMonitor pm) throws CoreException{
		IFolder folder= getFolder(fPath);
		Assert.isTrue(folder.exists());
		pm.beginTask("", 3); //$NON-NLS-1$
		folder.accept(new IResourceVisitor() {
			public boolean visit(IResource resource) throws CoreException {
				if (resource instanceof IFile) {
					// progress is covered outside.
					saveFileIfNeeded((IFile)resource, new NullProgressMonitor());
				}
				return true;
			}
		}, IResource.DEPTH_INFINITE, false);
		pm.worked(1);
		
		ResourceDescription resourceDescription = ResourceDescription.fromResource(folder);
		folder.delete(false, true, new SubProgressMonitor(pm, 1));
		resourceDescription.recordStateFromHistory(folder, new SubProgressMonitor(pm, 1));
		pm.done();
		
		return new UndoDeleteResourceChange(resourceDescription);
	}
}

