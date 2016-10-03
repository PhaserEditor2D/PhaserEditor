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

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.participants.ReorgExecutionLog;
import org.eclipse.wst.jsdt.internal.corext.refactoring.base.JDTChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.reorg.INewNameQuery;
import org.eclipse.wst.jsdt.internal.corext.refactoring.reorg.ReorgUtils;

abstract class ResourceReorgChange extends JDTChange {
	
	private final IPath fResourcePath;
	private final boolean fIsFile;
	private final IPath fDestinationPath;
	private final boolean fIsDestinationProject;
	private final INewNameQuery fNewNameQuery;

	ResourceReorgChange(IResource res, IContainer dest, INewNameQuery nameQuery){
		Assert.isTrue(res instanceof IFile || res instanceof IFolder);
		fIsFile= (res instanceof IFile);
		fResourcePath= Utils.getResourcePath(res);
	
		Assert.isTrue(dest instanceof IProject || dest instanceof IFolder);
		fIsDestinationProject= (dest instanceof IProject);
		fDestinationPath= Utils.getResourcePath(dest);
		fNewNameQuery= nameQuery;
	}
	
	protected abstract Change doPerformReorg(IPath path, IProgressMonitor pm) throws CoreException;
	
	/* non java-doc
	 * @see IChange#perform(ChangeContext, IProgressMonitor)
	 */
	public final Change perform(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		try{
			pm.beginTask(getName(), 2);
			
			String newName= getNewResourceName();
			IResource resource= getResource();
			boolean performReorg= deleteIfAlreadyExists(new SubProgressMonitor(pm, 1), newName);
			if (!performReorg)
				return null;
			final Change result= doPerformReorg(getDestinationPath(newName), new SubProgressMonitor(pm, 1));
			markAsExecuted(resource);
			return result;
		} finally {
			pm.done();
		}
	}

	protected IPath getDestinationPath(String newName) {
		return getDestination().getFullPath().append(newName);
	}

	/**
	 * returns false if source and destination are the same (in workspace or on disk)
	 * in such case, no action should be performed
	 */
	private boolean deleteIfAlreadyExists(IProgressMonitor pm, String newName) throws CoreException {
		pm.beginTask("", 1); //$NON-NLS-1$
		IResource current= getDestination().findMember(newName);
		if (current == null)
			return true;
		if (! current.exists())
			return true;

		IResource resource= getResource();
		Assert.isNotNull(resource);
			
		if (ReorgUtils.areEqualInWorkspaceOrOnDisk(resource, current))
			return false;
		
		if (current instanceof IFile)
			((IFile)current).delete(false, true, new SubProgressMonitor(pm, 1));
		else if (current instanceof IFolder)
			((IFolder)current).delete(false, true, new SubProgressMonitor(pm, 1));
		else 
			Assert.isTrue(false);
			
		return true;	
	}
	

	private String getNewResourceName() throws OperationCanceledException {
		if (fNewNameQuery == null)
			return getResource().getName();
		String name= fNewNameQuery.getNewName();
		if (name == null)
			return getResource().getName();
		return name;
	}
	
	/* non java-doc
	 * @see IChange#getModifiedLanguageElement()
	 */
	public Object getModifiedElement() {
		return getResource();
	}

	private IFile getFile(){
		return Utils.getFile(fResourcePath);
	}
	
	private IFolder getFolder(){
		return Utils.getFolder(fResourcePath);
	}
	
	protected IResource getResource(){
		if (fIsFile)
			return getFile();
		else
			return getFolder();
	}
	
	IContainer getDestination(){
		if (fIsDestinationProject)
			return Utils.getProject(fDestinationPath);
		else
			return Utils.getFolder(fDestinationPath);	
	}

	protected int getReorgFlags() {
		return IResource.KEEP_HISTORY | IResource.SHALLOW;
	}
	
	private void markAsExecuted(IResource resource) {
		ReorgExecutionLog log= (ReorgExecutionLog)getAdapter(ReorgExecutionLog.class);
		if (log != null) {
			log.markAsProcessed(resource);
		}
	}
}

