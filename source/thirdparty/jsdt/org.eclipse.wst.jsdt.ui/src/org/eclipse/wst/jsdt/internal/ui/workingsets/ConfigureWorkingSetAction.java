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
package org.eclipse.wst.jsdt.internal.ui.workingsets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkingSet;

public class ConfigureWorkingSetAction extends Action {

	private final IWorkbenchPartSite fSite;
	private WorkingSetModel fWorkingSetModel;

	public ConfigureWorkingSetAction(IWorkbenchPartSite site) {
		super(WorkingSetMessages.ConfigureWorkingSetAction_label); 
		fSite= site;
	}
	
	public void setWorkingSetModel(WorkingSetModel model) {
		fWorkingSetModel= model;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void run() {
		List workingSets= new ArrayList(Arrays.asList(fWorkingSetModel.getAllWorkingSets()));
		IWorkingSet[] activeWorkingSets= fWorkingSetModel.getActiveWorkingSets();
		WorkingSetConfigurationDialog dialog= new WorkingSetConfigurationDialog(
			fSite.getShell(), 
			(IWorkingSet[])workingSets.toArray(new IWorkingSet[workingSets.size()]),
			activeWorkingSets); 
		dialog.setSelection(activeWorkingSets);
		if (dialog.open() == IDialogConstants.OK_ID) {
			IWorkingSet[] selection= dialog.getSelection();
			fWorkingSetModel.setActiveWorkingSets(selection);
		}
	}
}
