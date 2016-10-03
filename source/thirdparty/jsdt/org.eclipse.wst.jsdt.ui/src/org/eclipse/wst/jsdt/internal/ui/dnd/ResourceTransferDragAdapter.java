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
package org.eclipse.wst.jsdt.internal.ui.dnd;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.util.TransferDragSourceListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.part.ResourceTransfer;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.internal.ui.IJavaStatusConstants;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.JavaUIMessages;
import org.eclipse.wst.jsdt.internal.ui.util.SWTUtil;

/**
 * A drag adapter that transfers the current selection as </code>
 * IResource</code>. Only those elements in the selection are part 
 * of the transfer which can be converted into an <code>IResource
 * </code>.
 */
public class ResourceTransferDragAdapter extends DragSourceAdapter implements TransferDragSourceListener {

	private ISelectionProvider fProvider;

	private static final List EMPTY_LIST= new ArrayList(0);

	/**
	 * Creates a new ResourceTransferDragAdapter for the given selection
	 * provider.
	 * 
	 * @param provider the selection provider to access the viewer's selection
	 */
	public ResourceTransferDragAdapter(ISelectionProvider provider) {
		fProvider= provider;
		Assert.isNotNull(fProvider);
	}
	
	public Transfer getTransfer() {
		return ResourceTransfer.getInstance();
	}
	
	public void dragStart(DragSourceEvent event) {
		event.doit= convertSelection().size() > 0;
	}
	
	public void dragSetData(DragSourceEvent event) {
		List resources= convertSelection();
		event.data= resources.toArray(new IResource[resources.size()]);
	}
	
	public void dragFinished(DragSourceEvent event) {
		if (!event.doit)
			return;

		if (event.detail == DND.DROP_MOVE) {
			handleFinishedDropMove(event);
		}	
	}
	
	private List convertSelection() {
		ISelection s= fProvider.getSelection();
		if (!(s instanceof IStructuredSelection))
			return EMPTY_LIST;
		IStructuredSelection selection= (IStructuredSelection)s;
		List result= new ArrayList(selection.size());
		for (Iterator iter= selection.iterator(); iter.hasNext();) {
			Object element= iter.next();
			IResource resource= null;
			if (element instanceof IJavaScriptElement) {
				// don't use IAdaptable as for members only the top level type adapts
				resource= ((IJavaScriptElement) element).getResource();
			} else if (element instanceof IAdaptable) {
				resource= (IResource) ((IAdaptable) element).getAdapter(IResource.class);
			}
			if (resource != null)
				result.add(resource);
		}
		return result;
	}
	
	private void handleFinishedDropMove(DragSourceEvent event) {
		MultiStatus status= new MultiStatus(
			JavaScriptPlugin.getPluginId(), 
			IJavaStatusConstants.INTERNAL_ERROR, 
			JavaUIMessages.ResourceTransferDragAdapter_cannot_delete_resource,  
			null);
		List resources= convertSelection();
		for (Iterator iter= resources.iterator(); iter.hasNext();) {
			IResource resource= (IResource) iter.next();
			try {
				resource.delete(true, null);
			} catch (CoreException e) {
				status.add(e.getStatus());
			}
		}
		if (status.getChildren().length > 0) {
			Shell parent= SWTUtil.getShell(event.widget);
			ErrorDialog error= new ErrorDialog(parent, 
				JavaUIMessages.ResourceTransferDragAdapter_moving_resource,  
				JavaUIMessages.ResourceTransferDragAdapter_cannot_delete_files,  
				status, IStatus.ERROR);
			error.open();
		}
	}
}

