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
package org.eclipse.wst.jsdt.ui.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.actions.ActionMessages;
import org.eclipse.wst.jsdt.internal.ui.actions.WorkbenchRunnableAdapter;
import org.eclipse.wst.jsdt.internal.ui.util.ExceptionHandler;
import org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths.ArchiveFileFilter;

/**
 * Action to add a JAR to the classpath of its parent project.
 * Action is applicable to selections containing archives (JAR or zip) 
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 */
public class AddToClasspathAction extends SelectionDispatchAction {

	/**
	 * Creates a new <code>AddToClasspathAction</code>. The action requires that
	 * the selection provided by the site's selection provider is of type <code>
	 * org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param site the site providing context information for this action
	 */
	public AddToClasspathAction(IWorkbenchSite site) {
		super(site);
		setText(ActionMessages.AddToClasspathAction_label); 
		setToolTipText(ActionMessages.AddToClasspathAction_toolTip); 
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.ADD_TO_CLASSPATH_ACTION);
	}
	
	/* (non-Javadoc)
	 * Method declared in SelectionDispatchAction
	 */
	public void selectionChanged(IStructuredSelection selection) {
		try {
			setEnabled(checkEnabled(selection));
		} catch (JavaScriptModelException e) {
			// http://bugs.eclipse.org/bugs/show_bug.cgi?id=19253
			if (JavaModelUtil.isExceptionToBeLogged(e))
				JavaScriptPlugin.log(e);
			setEnabled(false);
		}
	}
	
	private static boolean checkEnabled(IStructuredSelection selection) throws JavaScriptModelException {
		if (selection.isEmpty())
			return false;
		for (Iterator iter= selection.iterator(); iter.hasNext();) {
			if (! canBeAddedToBuildPath(iter.next()))
				return false;
		}
		return true;
	}

	private static boolean canBeAddedToBuildPath(Object element) throws JavaScriptModelException{
		return (element instanceof IAdaptable) && getCandidate((IAdaptable) element) != null;
	}

	private static IFile getCandidate(IAdaptable element) throws JavaScriptModelException {
		IResource resource= (IResource)element.getAdapter(IResource.class);
		if (! (resource instanceof IFile) || ! ArchiveFileFilter.isArchivePath(resource.getFullPath()))
			return null;
		
		IJavaScriptProject project= JavaScriptCore.create(resource.getProject());
		if (project != null && project.exists() && (project.findPackageFragmentRoot(resource.getFullPath()) == null))
			return (IFile) resource;
		return null;
	}
			
	/* (non-Javadoc)
	 * Method declared in SelectionDispatchAction
	 */
	public void run(IStructuredSelection selection) {
		try {
			final IFile[] files= getJARFiles(selection);	
			
			IWorkspaceRunnable operation= new IWorkspaceRunnable() {
				public void run(IProgressMonitor monitor) throws CoreException {
					monitor.beginTask(ActionMessages.AddToClasspathAction_progressMessage, files.length); 
					for (int i= 0; i < files.length; i++) {
						monitor.subTask(files[i].getFullPath().toString());
						IJavaScriptProject project= JavaScriptCore.create(files[i].getProject());
						addToClassPath(project, files[i].getFullPath(), new SubProgressMonitor(monitor, 1));
					}
				}
				
				private void addToClassPath(IJavaScriptProject project, IPath jarPath, IProgressMonitor monitor) throws JavaScriptModelException {
					if (monitor.isCanceled())
						throw new OperationCanceledException();
					IIncludePathEntry[] entries= project.getRawIncludepath();
					IIncludePathEntry[] newEntries= new IIncludePathEntry[entries.length + 1];
					System.arraycopy(entries, 0, newEntries, 0, entries.length);
					newEntries[entries.length]= JavaScriptCore.newLibraryEntry(jarPath, null, null, false);
					project.setRawIncludepath(newEntries, monitor);
				}
			};	
			
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().run(true, true, new WorkbenchRunnableAdapter(operation));
		} catch (InvocationTargetException e) {
			ExceptionHandler.handle(e, getShell(), 
				ActionMessages.AddToClasspathAction_error_title,  
				ActionMessages.AddToClasspathAction_error_message); 
		} catch (JavaScriptModelException e) {
			ExceptionHandler.handle(e, getShell(), 
					ActionMessages.AddToClasspathAction_error_title,  
					ActionMessages.AddToClasspathAction_error_message); 
		} catch (InterruptedException e) {
			// canceled
		}

	}
	
	private static IFile[] getJARFiles(IStructuredSelection selection) throws JavaScriptModelException {
		ArrayList list= new ArrayList();
		for (Iterator iter= selection.iterator(); iter.hasNext();) {
			Object element= iter.next();
			if (element instanceof IAdaptable) {
				IFile file= getCandidate((IAdaptable) element);
				if (file != null) {
					list.add(file);
				}
			}
		}
		return (IFile[]) list.toArray(new IFile[list.size()]);
	}
}

