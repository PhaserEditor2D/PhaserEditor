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
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.actions.ActionGroup;

public class WorkingSetShowActionGroup extends ActionGroup implements IWorkingSetActionGroup {
	
	private List fContributions= new ArrayList();
	private ConfigureWorkingSetAction fConfigureWorkingSetAction;	
	private WorkingSetModel fWorkingSetModel;
	private final IWorkbenchPartSite fSite; 
	
	public WorkingSetShowActionGroup(IWorkbenchPartSite site) {
		Assert.isNotNull(site);
		fSite= site;
	}
	
	public void setWorkingSetMode(WorkingSetModel model) {
		Assert.isNotNull(model);
		fWorkingSetModel= model;
		if (fConfigureWorkingSetAction != null)
			fConfigureWorkingSetAction.setWorkingSetModel(fWorkingSetModel);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void fillActionBars(IActionBars actionBars) {
		super.fillActionBars(actionBars);
		IMenuManager menuManager= actionBars.getMenuManager();
		fillViewMenu(menuManager);
	}
	
	public void fillViewMenu(IMenuManager menuManager) {
		fConfigureWorkingSetAction=  new ConfigureWorkingSetAction(fSite);
		if (fWorkingSetModel != null)
			fConfigureWorkingSetAction.setWorkingSetModel(fWorkingSetModel);
		addAction(menuManager, fConfigureWorkingSetAction);
	}
	
	public void cleanViewMenu(IMenuManager menuManager) {
		for (Iterator iter= fContributions.iterator(); iter.hasNext();) {
			IContributionItem removed= menuManager.remove((IContributionItem) iter.next());
			if (removed != null) {
				removed.dispose();
			}
		}
		fContributions.clear();
	}

	private void addAction(IMenuManager menuManager, Action action) {
		IContributionItem item= new ActionContributionItem(action);
		menuManager.appendToGroup(ACTION_GROUP, item);
		fContributions.add(item);
	}
}
