/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.corext.refactoring.changes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.NullChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.JavaScriptConventions;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.wst.jsdt.internal.corext.refactoring.base.JDTChange;

public class AddToClasspathChange extends JDTChange {
	
	private IJavaScriptProject fProjectHandle;
	private IIncludePathEntry fEntryToAdd;
	
	public AddToClasspathChange(IJavaScriptProject project, IIncludePathEntry entryToAdd) {
		fProjectHandle= project;
		fEntryToAdd= entryToAdd;
	}
	
	public AddToClasspathChange(IJavaScriptProject project, String sourceFolderName){
		this(project, JavaScriptCore.newSourceEntry(project.getPath().append(sourceFolderName)));
	}
	
	/**
	 * Adds a new project class path entry to the project.
	 * @param project
	 * @param newProjectEntry (must be absolute <code>IPath</code>)
	 */
	public AddToClasspathChange(IJavaScriptProject project, IPath newProjectEntry){
		this(project, JavaScriptCore.newProjectEntry(newProjectEntry));
	}
	
	public AddToClasspathChange(IJavaScriptProject project, int entryKind, IPath path, IPath sourceAttachmentPath, IPath sourceAttachmentRootPath){
		this(project, createNewClasspathEntry(entryKind, path, sourceAttachmentPath, sourceAttachmentRootPath));
	}

	public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException {
		// .classpath file will be handled by JDT/Core.
		return super.isValid(pm, READ_ONLY | DIRTY);
	}
	
	public Change perform(IProgressMonitor pm) throws CoreException {
		pm.beginTask(getName(), 1);
		try {
			if (validateClasspath()) {
				getJavaProject().setRawIncludepath(getNewClasspathEntries(), new SubProgressMonitor(pm, 1));
				IPath classpathEntryPath= JavaScriptCore.getResolvedIncludepathEntry(fEntryToAdd).getPath();
				return new DeleteFromClasspathChange(classpathEntryPath, getJavaProject());
			} else {
				return new NullChange();
			}
		} finally {
			pm.done();
		}		
	}
	
	public boolean validateClasspath() throws JavaScriptModelException {
		IJavaScriptProject javaProject= getJavaProject();
		IIncludePathEntry[] newClasspathEntries= getNewClasspathEntries();
		return JavaScriptConventions.validateClasspath(javaProject, newClasspathEntries).isOK();
	}
	
	private IIncludePathEntry[] getNewClasspathEntries() throws JavaScriptModelException{
		IIncludePathEntry[] entries= getJavaProject().getRawIncludepath();
		List cp= new ArrayList(entries.length + 1);
		cp.addAll(Arrays.asList(entries));
		cp.add(fEntryToAdd);
		return (IIncludePathEntry[])cp.toArray(new IIncludePathEntry[cp.size()]);
	}
	
	private static IIncludePathEntry createNewClasspathEntry(int kind, IPath path, IPath sourceAttach, IPath sourceAttachRoot){
		switch(kind){
			case IIncludePathEntry.CPE_LIBRARY:
				return JavaScriptCore.newLibraryEntry(path, sourceAttach, sourceAttachRoot);
			case IIncludePathEntry.CPE_PROJECT:
				return JavaScriptCore.newProjectEntry(path);
			case IIncludePathEntry.CPE_SOURCE:
				return JavaScriptCore.newSourceEntry(path);
			case IIncludePathEntry.CPE_VARIABLE:
				return JavaScriptCore.newVariableEntry(path, sourceAttach, sourceAttachRoot);	
			case IIncludePathEntry.CPE_CONTAINER:
				return JavaScriptCore.newContainerEntry(path);	
			default:
				Assert.isTrue(false);
				return null;	
		}
	}
	
	private IJavaScriptProject getJavaProject(){
		return fProjectHandle;
	}

	public String getName() {
		return RefactoringCoreMessages.AddToClasspathChange_add + getJavaProject().getElementName(); 
 
	}

	public Object getModifiedElement() {
		return getJavaProject();
	}
	
	public IIncludePathEntry getClasspathEntry() {
		return fEntryToAdd;
	}
}
