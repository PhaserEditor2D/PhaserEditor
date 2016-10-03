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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.corext.refactoring.AbstractJavaElementRenameChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;

public final class RenameJavaProjectChange extends AbstractJavaElementRenameChange {

	private boolean fUpdateReferences;

	public RenameJavaProjectChange(IJavaScriptProject project, String newName, boolean updateReferences) {
		this(project.getPath(), project.getElementName(), newName, IResource.NULL_STAMP, updateReferences);
		Assert.isTrue(!project.isReadOnly(), "should not be read only"); //$NON-NLS-1$
	}

	private RenameJavaProjectChange(IPath resourcePath, String oldName, String newName, long stampToRestore, boolean updateReferences) {
		super(resourcePath, oldName, newName);
		fUpdateReferences= updateReferences;
	}

	private IIncludePathEntry createModifiedEntry(IIncludePathEntry oldEntry) {
		return JavaScriptCore.newProjectEntry(createNewPath(), oldEntry.getAccessRules(), oldEntry.combineAccessRules(), oldEntry.getExtraAttributes(), oldEntry.isExported());
	}

	protected IPath createNewPath() {
		return getResourcePath().removeLastSegments(1).append(getNewName());
	}

	protected Change createUndoChange(long stampToRestore) throws JavaScriptModelException {
		return new RenameJavaProjectChange(createNewPath(), getNewName(), getOldName(), stampToRestore, fUpdateReferences);
	}

	protected void doRename(IProgressMonitor pm) throws CoreException {
		try {
			pm.beginTask(getName(), 2);
			if (fUpdateReferences)
				modifyClassPaths(new SubProgressMonitor(pm, 1));
			IProject project= getProject();
			if (project != null) {
				IProjectDescription description= project.getDescription();
				description.setName(getNewName());
				project.move(description, IResource.FORCE | IResource.SHALLOW, new SubProgressMonitor(pm, 1));
			}
		} finally {
			pm.done();
		}
	}

	private IJavaScriptProject getJavaProject() {
		return (IJavaScriptProject) getModifiedElement();
	}

	public String getName() {
		return Messages.format(RefactoringCoreMessages.RenameJavaProjectChange_rename, new String[] { getOldName(), getNewName()});
	}

	private IProject getProject() {
		IJavaScriptProject jp= getJavaProject();
		if (jp == null)
			return null;
		return jp.getProject();
	}

	private boolean isOurEntry(IIncludePathEntry cpe) {
		if (cpe.getEntryKind() != IIncludePathEntry.CPE_PROJECT)
			return false;
		if (!cpe.getPath().equals(getResourcePath()))
			return false;
		return true;
	}

	public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException {
		return isValid(pm, DIRTY);
	}

	private void modifyClassPath(IJavaScriptProject referencingProject, IProgressMonitor pm) throws JavaScriptModelException {
		pm.beginTask("", 1); //$NON-NLS-1$
		IIncludePathEntry[] oldEntries= referencingProject.getRawIncludepath();
		IIncludePathEntry[] newEntries= new IIncludePathEntry[oldEntries.length];
		for (int i= 0; i < newEntries.length; i++) {
			if (isOurEntry(oldEntries[i]))
				newEntries[i]= createModifiedEntry(oldEntries[i]);
			else
				newEntries[i]= oldEntries[i];
		}
		referencingProject.setRawIncludepath(newEntries, pm);
		pm.done();
	}

	private void modifyClassPaths(IProgressMonitor pm) throws JavaScriptModelException {
		IProject[] referencing= getProject().getReferencingProjects();
		pm.beginTask(RefactoringCoreMessages.RenameJavaProjectChange_update, referencing.length);
		for (int i= 0; i < referencing.length; i++) {
			IJavaScriptProject jp= JavaScriptCore.create(referencing[i]);
			if (jp != null && jp.exists()) {
				modifyClassPath(jp, new SubProgressMonitor(pm, 1));
			} else {
				pm.worked(1);
			}
		}
		pm.done();
	}
}
