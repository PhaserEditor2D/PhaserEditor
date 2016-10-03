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
package org.eclipse.wst.jsdt.internal.corext.refactoring;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.internal.corext.refactoring.base.JDTChange;

public abstract class AbstractJavaElementRenameChange extends JDTChange {

	private final String fNewName;

	private final String fOldName;

	private final IPath fResourcePath;

	private final long fStampToRestore;

	protected AbstractJavaElementRenameChange(IPath resourcePath, String oldName, String newName) {
		this(resourcePath, oldName, newName, IResource.NULL_STAMP);
	}

	protected AbstractJavaElementRenameChange(IPath resourcePath, String oldName, String newName, long stampToRestore) {
		Assert.isNotNull(newName, "new name"); //$NON-NLS-1$
		Assert.isNotNull(oldName, "old name"); //$NON-NLS-1$
		fResourcePath= resourcePath;
		fOldName= oldName;
		fNewName= newName;
		fStampToRestore= stampToRestore;
	}

	protected abstract IPath createNewPath();

	protected abstract Change createUndoChange(long stampToRestore) throws CoreException;

	protected abstract void doRename(IProgressMonitor pm) throws CoreException;

	public Object getModifiedElement() {
		return JavaScriptCore.create(getResource());
	}

	public String getNewName() {
		return fNewName;
	}

	public String getOldName() {
		return fOldName;
	}

	protected final IResource getResource() {
		return ResourcesPlugin.getWorkspace().getRoot().findMember(fResourcePath);
	}

	protected IPath getResourcePath() {
		return fResourcePath;
	}

	public final Change perform(IProgressMonitor pm) throws CoreException {
		try {
			pm.beginTask(RefactoringCoreMessages.AbstractRenameChange_Renaming, 1);
			IResource resource= getResource();
			IPath newPath= createNewPath();
			Change result= createUndoChange(resource.getModificationStamp());
			doRename(new SubProgressMonitor(pm, 1));
			if (fStampToRestore != IResource.NULL_STAMP) {
				IResource newResource= ResourcesPlugin.getWorkspace().getRoot().findMember(newPath);
				newResource.revertModificationStamp(fStampToRestore);
			}
			return result;
		} finally {
			pm.done();
		}
	}
}
