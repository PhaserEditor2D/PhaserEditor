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
package org.eclipse.wst.jsdt.internal.ui.actions;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.tasklist.TaskPropertiesDialog;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.ui.actions.SelectionDispatchAction;

public class AddTaskAction extends SelectionDispatchAction {

	public AddTaskAction(IWorkbenchSite site) {
		super(site);
		setEnabled(false);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.ADD_TASK_ACTION);
	}

	public void selectionChanged(IStructuredSelection selection) {
		setEnabled(getElement(selection) != null);
	}

	public void run(IStructuredSelection selection) {
		IResource resource= getElement(selection);
		if (resource == null)
			return;

		TaskPropertiesDialog dialog= new TaskPropertiesDialog(getShell());
		dialog.setResource(resource);
		dialog.open();
	}
	
	private IResource getElement(IStructuredSelection selection) {
		if (selection.size() != 1)
			return null;

		Object element= selection.getFirstElement();
		if (!(element instanceof IAdaptable))
			return null;
		return (IResource)((IAdaptable)element).getAdapter(IResource.class);
	}	
}
