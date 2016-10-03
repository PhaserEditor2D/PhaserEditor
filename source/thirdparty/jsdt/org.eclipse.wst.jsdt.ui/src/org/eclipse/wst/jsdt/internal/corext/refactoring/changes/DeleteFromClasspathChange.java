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

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.wst.jsdt.internal.corext.refactoring.base.JDTChange;

public class DeleteFromClasspathChange extends JDTChange {

	private final String fProjectHandle;
	private final IPath fPathToDelete;
	
	private IPath fPath;
	private IPath fSourceAttachmentPath;
	private IPath fSourceAttachmentRootPath;
	private int fEntryKind;
	
	public DeleteFromClasspathChange(IPackageFragmentRoot root) {
		this(root.getPath(), root.getJavaScriptProject());
	}
	
	DeleteFromClasspathChange(IPath pathToDelete, IJavaScriptProject project){
		Assert.isNotNull(pathToDelete);
		fPathToDelete= pathToDelete;
		fProjectHandle= project.getHandleIdentifier();
	}
	
	public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException {
		// we have checked the .classpath file in the delete change.
		return super.isValid(pm, READ_ONLY | DIRTY);
	}
	
	public Change perform(IProgressMonitor pm)	throws CoreException {
		pm.beginTask(getName(), 1);
		try{
			IJavaScriptProject project= getJavaProject();
			IIncludePathEntry[] cp= project.getRawIncludepath();
			IIncludePathEntry[] newCp= new IIncludePathEntry[cp.length-1];
			int i= 0; 
			int j= 0;
			while (j < newCp.length) {
				IIncludePathEntry current= JavaScriptCore.getResolvedIncludepathEntry(cp[i]);
				if (current != null && toBeDeleted(current)) {
					i++;
					setDeletedEntryProperties(current);
				} 

				newCp[j]= cp[i];
				i++;
				j++;
			}
			
			IIncludePathEntry last= JavaScriptCore.getResolvedIncludepathEntry(cp[cp.length - 1]);
			if (last != null && toBeDeleted(last))
				setDeletedEntryProperties(last);
				
			project.setRawIncludepath(newCp, pm);
			
			return new AddToClasspathChange(getJavaProject(), fEntryKind, fPath, 
				fSourceAttachmentPath, fSourceAttachmentRootPath);
		} finally {
			pm.done();
		}
	}
	
	private boolean toBeDeleted(IIncludePathEntry entry){
		if (entry == null) //safety net
			return false; 
		return fPathToDelete.equals(entry.getPath());
	}
	
	private void setDeletedEntryProperties(IIncludePathEntry entry){
		fEntryKind= entry.getEntryKind();
		fPath= entry.getPath();
		fSourceAttachmentPath= entry.getSourceAttachmentPath();
		fSourceAttachmentRootPath= entry.getSourceAttachmentRootPath();
	}
	
	private IJavaScriptProject getJavaProject(){
		return (IJavaScriptProject)JavaScriptCore.create(fProjectHandle);
	}
	
	public String getName() {
		return RefactoringCoreMessages.DeleteFromClassPathChange_remove + getJavaProject().getElementName(); 
	}

	public Object getModifiedElement() {
		return getJavaProject();
	}
}
