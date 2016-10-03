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
package org.eclipse.wst.jsdt.internal.ui.packageview;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.util.TransferDragSourceListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.internal.corext.util.Resources;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.util.ExceptionHandler;

/**
 * Drag support class to allow dragging of files and folder from
 * the packages view to another application.
 */
class FileTransferDragAdapter extends DragSourceAdapter implements TransferDragSourceListener {
	
	private ISelectionProvider fProvider;
	
	FileTransferDragAdapter(ISelectionProvider provider) {
		fProvider= provider;
		Assert.isNotNull(fProvider);
	}

	public Transfer getTransfer() {
		return FileTransfer.getInstance();
	}
	
	public void dragStart(DragSourceEvent event) {
		event.doit= isDragable(fProvider.getSelection());
	}
	
	private boolean isDragable(ISelection s) {
		if (!(s instanceof IStructuredSelection))
			return false;
		IStructuredSelection selection= (IStructuredSelection)s;
		for (Iterator iter= selection.iterator(); iter.hasNext();) {
			Object element= iter.next();
			if (element instanceof IJavaScriptElement) {
				IJavaScriptElement jElement= (IJavaScriptElement)element;
				int type= jElement.getElementType();
				// valid elements are: roots, units and types. Don't allow dragging
				// projects outside of eclipse
				if (type != IJavaScriptElement.PACKAGE_FRAGMENT_ROOT &&
					type != IJavaScriptElement.JAVASCRIPT_UNIT && type != IJavaScriptElement.TYPE)
					return false;
				IPackageFragmentRoot root= (IPackageFragmentRoot)jElement.getAncestor(IJavaScriptElement.PACKAGE_FRAGMENT_ROOT);
				if (root != null && root.isArchive())
					return false;
			}
		}
		List resources= convertIntoResources(selection);
		return resources.size() == selection.size();
	}
	
	public void dragSetData(DragSourceEvent event){
		List elements= getResources();
		if (elements == null || elements.size() == 0) {
			event.data= null;
			return;
		}
		
		event.data= getResourceLocations(elements);
	}

	private static String[] getResourceLocations(List resources) {
		return Resources.getLocationOSStrings((IResource[]) resources.toArray(new IResource[resources.size()]));
	}
	
	public void dragFinished(DragSourceEvent event) {
		if (!event.doit)
			return;
		
		if (event.detail == DND.DROP_MOVE) {
			// http://bugs.eclipse.org/bugs/show_bug.cgi?id=30543
			// handleDropMove(event);
		} else if (event.detail == DND.DROP_NONE || event.detail == DND.DROP_TARGET_MOVE) {
			handleRefresh(event);
		}
	}
	
	/* package */ void handleDropMove(DragSourceEvent event) {
		final List elements= getResources();
		if (elements == null || elements.size() == 0)
			return;
		
		WorkspaceModifyOperation op= new WorkspaceModifyOperation() {
			public void execute(IProgressMonitor monitor) throws CoreException {
				try {
					monitor.beginTask(PackagesMessages.DragAdapter_deleting, elements.size()); 
					MultiStatus status= createMultiStatus();
					Iterator iter= elements.iterator();
					while(iter.hasNext()) {
						IResource resource= (IResource)iter.next();
						try {
							monitor.subTask(resource.getFullPath().toOSString());
							resource.delete(true, null);
							
						} catch (CoreException e) {
							status.add(e.getStatus());
						} finally {
							monitor.worked(1);
						}
					}
					if (!status.isOK()) {
						throw new CoreException(status);
					}
				} finally {
					monitor.done();
				}
			}
		};
		runOperation(op, true, false);
	}
	
	private  void handleRefresh(DragSourceEvent event) {
		final Set roots= collectRoots(getResources());
		
		WorkspaceModifyOperation op= new WorkspaceModifyOperation() {
			public void execute(IProgressMonitor monitor) throws CoreException {
				try {
					monitor.beginTask(PackagesMessages.DragAdapter_refreshing, roots.size()); 
					MultiStatus status= createMultiStatus();
					Iterator iter= roots.iterator();
					while (iter.hasNext()) {
						IResource r= (IResource)iter.next();
						try {
							r.refreshLocal(IResource.DEPTH_ONE, new SubProgressMonitor(monitor, 1));
						} catch (CoreException e) {
							status.add(e.getStatus());
						}	
					}
					if (!status.isOK()) {
						throw new CoreException(status);
					}
				} finally {
					monitor.done();
				}
			}
		};
		
		runOperation(op, true, false);
	}

	protected Set collectRoots(final List elements) {
		final Set roots= new HashSet(10);
		
		Iterator iter= elements.iterator();
		while (iter.hasNext()) {
			IResource resource= (IResource)iter.next();
			IResource parent= resource.getParent();
			if (parent == null) {
				roots.add(resource);
			} else {
				roots.add(parent);
			}
		}
		return roots;
	}
	
	private List getResources() {
		ISelection s= fProvider.getSelection();
		if (!(s instanceof IStructuredSelection)) 
			return null;
		
		return convertIntoResources((IStructuredSelection)s);
	}

	private List convertIntoResources(IStructuredSelection selection) {
		List result= new ArrayList(selection.size());
		for (Iterator iter= selection.iterator(); iter.hasNext();) {
			Object o= iter.next();
			IResource r= null;
			if (o instanceof IResource) {
				r= (IResource)o;
			} else if (o instanceof IAdaptable) {
				r= (IResource)((IAdaptable)o).getAdapter(IResource.class);
			}
			// Only add resource for which we have a location
			// in the local file system.
			if (r != null && r.getLocation() != null) {
				result.add(r);
			}
		}
		return result;
	}
	
	private MultiStatus createMultiStatus() {
		return new MultiStatus(JavaScriptPlugin.getPluginId(), 
			IStatus.OK, PackagesMessages.DragAdapter_problem, null); 
	}
	
	private void runOperation(IRunnableWithProgress op, boolean fork, boolean cancelable) {
		try {
			Shell parent= JavaScriptPlugin.getActiveWorkbenchShell();
			new ProgressMonitorDialog(parent).run(fork, cancelable, op);
		} catch (InvocationTargetException e) {
			String message= PackagesMessages.DragAdapter_problem; 
			String title= PackagesMessages.DragAdapter_problemTitle; 
			ExceptionHandler.handle(e, title, message);
		} catch (InterruptedException e) {
			// Do nothing. Operation has been canceled by user.
		}
	}
}
