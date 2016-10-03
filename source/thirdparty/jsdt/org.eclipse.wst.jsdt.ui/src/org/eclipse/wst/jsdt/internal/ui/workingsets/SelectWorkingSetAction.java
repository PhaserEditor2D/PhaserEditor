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
package org.eclipse.wst.jsdt.internal.ui.workingsets;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.IWorkingSetSelectionDialog;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;

/**
 * Displays an IWorkingSetSelectionDialog and sets the selected 
 * working set in the action group's view.
 * 
 * 
 */
public class SelectWorkingSetAction extends Action {
	private IWorkbenchPartSite fSite;
	private Shell fShell;
	private WorkingSetFilterActionGroup fActionGroup;

	public SelectWorkingSetAction(WorkingSetFilterActionGroup actionGroup, IWorkbenchPartSite site) {
		this(actionGroup); 
		fSite= site;
	}
	
	public SelectWorkingSetAction(WorkingSetFilterActionGroup actionGroup, Shell shell) {
		this(actionGroup); 
		fShell= shell;
	}
	
	private SelectWorkingSetAction(WorkingSetFilterActionGroup actionGroup) {
		super(WorkingSetMessages.SelectWorkingSetAction_text); 
		Assert.isNotNull(actionGroup);
		setToolTipText(WorkingSetMessages.SelectWorkingSetAction_toolTip); 
		fActionGroup= actionGroup;
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.SELECT_WORKING_SET_ACTION);
	}
	
	/*
	 * Overrides method from Action
	 */
	public void run() {
		Shell shell= getShell();
		IWorkingSetManager manager= PlatformUI.getWorkbench().getWorkingSetManager();
		IWorkingSetSelectionDialog dialog= manager.createWorkingSetSelectionDialog(shell, false);
		IWorkingSet workingSet= fActionGroup.getWorkingSet();
		if (workingSet != null)
			dialog.setSelection(new IWorkingSet[]{workingSet});

		if (dialog.open() == Window.OK) {
			IWorkingSet[] result= dialog.getSelection();
			if (result != null && result.length > 0) {
				fActionGroup.setWorkingSet(result[0], true);
				manager.addRecentWorkingSet(result[0]);
			}
			else
				fActionGroup.setWorkingSet(null, true);
		}
	}
	
	private Shell getShell() {
		if (fSite != null) {
			return fSite.getShell();
		} else if (fShell != null) {
			return fShell;
		} else {
			return JavaScriptPlugin.getActiveWorkbenchShell();
		}
	}
}
