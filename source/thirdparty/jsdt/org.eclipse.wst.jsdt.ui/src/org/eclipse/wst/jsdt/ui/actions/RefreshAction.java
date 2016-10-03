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
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptModel;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.corext.util.Resources;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;
import org.eclipse.wst.jsdt.internal.ui.actions.ActionMessages;
import org.eclipse.wst.jsdt.internal.ui.actions.WorkbenchRunnableAdapter;
import org.eclipse.wst.jsdt.internal.ui.util.ExceptionHandler;

/**
 * Action for refreshing the workspace from the local file system for
 * the selected resources and all of their descendants. This action
 * also considers external Jars managed by the JavaScript Model.
 * <p>
 * Action is applicable to selections containing resources and Java
 * elements down to compilation units.
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
public class RefreshAction extends SelectionDispatchAction {

	/**
	 * Creates a new <code>RefreshAction</code>. The action requires
	 * that the selection provided by the site's selection provider is of type <code>
	 * org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param site the site providing context information for this action
	 */
	public RefreshAction(IWorkbenchSite site) {
		super(site);
		setText(ActionMessages.RefreshAction_label); 
		setToolTipText(ActionMessages.RefreshAction_toolTip); 
		JavaPluginImages.setLocalImageDescriptors(this, "refresh_nav.gif");//$NON-NLS-1$
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.REFRESH_ACTION);
	}
	
	/* (non-Javadoc)
	 * Method declared in SelectionDispatchAction
	 */
	public void selectionChanged(IStructuredSelection selection) {
		setEnabled(checkEnabled(selection));
	}

	private boolean checkEnabled(IStructuredSelection selection) {
		if (selection.isEmpty())
			return true;
		for (Iterator iter= selection.iterator(); iter.hasNext();) {
			Object element= iter.next();
			if (element instanceof IWorkingSet) {
				// don't inspect working sets any deeper.
			} else if (element instanceof IAdaptable) {
				IResource resource= (IResource)((IAdaptable)element).getAdapter(IResource.class);
				if (resource == null)
					return false;
				if (resource.getType() == IResource.PROJECT && !((IProject)resource).isOpen())
					return false;
			} else {
				return false;
			}
		}
		return true;
	}

	/* (non-Javadoc)
	 * Method declared in SelectionDispatchAction
	 */
	public void run(IStructuredSelection selection) {
		final IResource[] resources= getResources(selection);
		IWorkspaceRunnable operation= new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				monitor.beginTask(ActionMessages.RefreshAction_progressMessage, resources.length * 2); 
				monitor.subTask(""); //$NON-NLS-1$
				List javaElements= new ArrayList(5);
				for (int r= 0; r < resources.length; r++) {
					IResource resource= resources[r];
					if (resource.getType() == IResource.PROJECT) {
						checkLocationDeleted((IProject) resource);
					} else if (resource.getType() == IResource.ROOT) {
						IProject[] projects = ((IWorkspaceRoot)resource).getProjects();
						for (int p = 0; p < projects.length; p++) {
							checkLocationDeleted(projects[p]);
						}
					}
					resource.refreshLocal(IResource.DEPTH_INFINITE, new SubProgressMonitor(monitor, 1));
					IJavaScriptElement jElement= JavaScriptCore.create(resource);
					if (jElement != null && jElement.exists())
						javaElements.add(jElement);
				}
				IJavaScriptModel model= JavaScriptCore.create(ResourcesPlugin.getWorkspace().getRoot());
				model.refreshExternalArchives(
					(IJavaScriptElement[]) javaElements.toArray(new IJavaScriptElement[javaElements.size()]),
					new SubProgressMonitor(monitor, resources.length));
			}
		};
		
		try {
			PlatformUI.getWorkbench().getProgressService().run(true, true, new WorkbenchRunnableAdapter(operation));
		} catch (InvocationTargetException e) {
			ExceptionHandler.handle(e, getShell(), 
				ActionMessages.RefreshAction_error_title,  
				ActionMessages.RefreshAction_error_message); 
		} catch (InterruptedException e) {
			// canceled
		}
	}
	
	private IResource[] getResources(IStructuredSelection selection) {
		if (selection.isEmpty()) {
			return new IResource[] {ResourcesPlugin.getWorkspace().getRoot()};
		}
		
		List result= new ArrayList(selection.size());
		getResources(result, selection.toArray());
		
		for (Iterator iter= result.iterator(); iter.hasNext();) {
			IResource resource= (IResource) iter.next();
			if (isDescendent(result, resource))
				iter.remove();			
		}
		
		return (IResource[]) result.toArray(new IResource[result.size()]);
	}
	
	private void getResources(List result, Object[] elements) {
		for (int i= 0; i < elements.length; i++) {
			Object element= elements[i];
			// Must check working set before IAdaptable since WorkingSet
			// implements IAdaptable
			if (element instanceof IWorkingSet) {
				getResources(result, ((IWorkingSet)element).getElements());
			} else if (element instanceof IAdaptable) {
				IResource resource= (IResource)((IAdaptable)element).getAdapter(IResource.class);
				if (resource == null)
					continue;
				if (resource.getType() != IResource.PROJECT  || 
						(resource.getType() == IResource.PROJECT && ((IProject)resource).isOpen())) {
					result.add(resource);
				}
			}
		}
	}
	
	private boolean isDescendent(List candidates, IResource element) {
		IResource parent= element.getParent();
		while (parent != null) {
			if (candidates.contains(parent))
				return true;
			parent= parent.getParent();
		}
		return false;
	}
	
	private void checkLocationDeleted(IProject project) throws CoreException {
		if (!project.exists())
			return;
		URI location= project.getLocationURI();
		if (location == null)
			return;
		IFileStore store= EFS.getStore(location);
		if (!store.fetchInfo().exists()) {
			final String message = Messages.format(
				ActionMessages.RefreshAction_locationDeleted_message, 
				new Object[] { project.getName(), Resources.getLocationString(project) });
			final boolean[] result= new boolean[1];
			// Must prompt user in UI thread (we're in the operation thread here).
			getShell().getDisplay().syncExec(new Runnable() {
				public void run() {
					result[0]= MessageDialog.openQuestion(getShell(), 
						ActionMessages.RefreshAction_locationDeleted_title, 
						message);
				}
			});
			if (result[0]) { 
				project.delete(true, true, null);
			}
		}
	}	
}
