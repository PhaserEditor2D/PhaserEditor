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
package org.eclipse.wst.jsdt.internal.ui.viewsupport;


import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.wst.jsdt.core.IJarEntryResource;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.JavaUIMessages;
import org.eclipse.wst.jsdt.internal.ui.packageview.PackageFragmentRootContainer;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabels;

/**
 * Add the <code>StatusBarUpdater</code> to your ViewPart to have the statusbar
 * describing the selected elements.
 */
public class StatusBarUpdater implements ISelectionChangedListener {
	
	private final long LABEL_FLAGS= JavaScriptElementLabels.DEFAULT_QUALIFIED | JavaScriptElementLabels.ROOT_POST_QUALIFIED | JavaScriptElementLabels.APPEND_ROOT_PATH |
			JavaScriptElementLabels.M_PARAMETER_TYPES | JavaScriptElementLabels.M_PARAMETER_NAMES | JavaScriptElementLabels.M_APP_RETURNTYPE | JavaScriptElementLabels.M_EXCEPTIONS | 
		 	JavaScriptElementLabels.F_APP_TYPE_SIGNATURE | JavaScriptElementLabels.T_TYPE_PARAMETERS;
		 	
	private IStatusLineManager fStatusLineManager;
	
	public StatusBarUpdater(IStatusLineManager statusLineManager) {
		fStatusLineManager= statusLineManager;
	}
		
	/*
	 * @see ISelectionChangedListener#selectionChanged
	 */
	public void selectionChanged(SelectionChangedEvent event) {
		String statusBarMessage= formatMessage(event.getSelection());
		fStatusLineManager.setMessage(statusBarMessage);
	}
	
	
	protected String formatMessage(ISelection sel) {
		if (sel instanceof IStructuredSelection && !sel.isEmpty()) {
			IStructuredSelection selection= (IStructuredSelection) sel;
			
			int nElements= selection.size();
			if (nElements > 1) {
				return Messages.format(JavaUIMessages.StatusBarUpdater_num_elements_selected, String.valueOf(nElements)); 
			} else { 
				Object elem= selection.getFirstElement();
				if (elem instanceof IJavaScriptElement) {
					return formatJavaElementMessage((IJavaScriptElement) elem);
				} else if (elem instanceof IResource) {
					return formatResourceMessage((IResource) elem);
				} else if (elem instanceof PackageFragmentRootContainer) {
					PackageFragmentRootContainer container= (PackageFragmentRootContainer) elem;
					return container.getLabel() + JavaScriptElementLabels.CONCAT_STRING + container.getJavaProject().getElementName();
				} else if (elem instanceof IJarEntryResource) {
					IJarEntryResource jarEntryResource= (IJarEntryResource) elem;
					StringBuffer buf= new StringBuffer(jarEntryResource.getName());
					buf.append(JavaScriptElementLabels.CONCAT_STRING);
					IPath fullPath= jarEntryResource.getFullPath();
					if (fullPath.segmentCount() > 1) {
						buf.append(fullPath.removeLastSegments(1).makeRelative());
						buf.append(JavaScriptElementLabels.CONCAT_STRING);
					}
					JavaScriptElementLabels.getPackageFragmentRootLabel(jarEntryResource.getPackageFragmentRoot(), JavaScriptElementLabels.ROOT_POST_QUALIFIED, buf);
					return buf.toString();
				} else if (elem instanceof IAdaptable) {
					IWorkbenchAdapter wbadapter= (IWorkbenchAdapter) ((IAdaptable)elem).getAdapter(IWorkbenchAdapter.class);
					if (wbadapter != null) {
						return wbadapter.getLabel(elem);
					}
				}
			}
		}
		return "";  //$NON-NLS-1$
	}
		
	private String formatJavaElementMessage(IJavaScriptElement element) {
		return JavaScriptElementLabels.getElementLabel(element, LABEL_FLAGS);
	}
		
	private String formatResourceMessage(IResource element) {
		IContainer parent= element.getParent();
		if (parent != null && parent.getType() != IResource.ROOT)
			return element.getName() + JavaScriptElementLabels.CONCAT_STRING + parent.getFullPath().makeRelative().toString();
		else
			return element.getName();
	}	

}
