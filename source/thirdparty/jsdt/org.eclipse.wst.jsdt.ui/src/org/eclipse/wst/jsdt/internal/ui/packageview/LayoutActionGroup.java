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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;

/**
 * Adds view menus to switch between flat and hierarchical layout.
 * 
 * 
 */
class LayoutActionGroup extends ActionGroup {

	public static final String VIEWMENU_LAYOUT_GROUP= "layout"; //$NON-NLS-1$
	
	private IAction fFlatLayoutAction;
	private IAction fHierarchicalLayoutAction;
	private IAction fShowLibrariesNode;
	
	LayoutActionGroup(PackageExplorerPart packageExplorer) {
		fFlatLayoutAction= new LayoutAction(packageExplorer, true);
		fHierarchicalLayoutAction= new LayoutAction(packageExplorer, false);
		fShowLibrariesNode= new ShowLibrariesNodeAction(packageExplorer);
	}

	/* (non-Javadoc)
	 * @see ActionGroup#fillActionBars(IActionBars)
	 */
	public void fillActionBars(IActionBars actionBars) {
		super.fillActionBars(actionBars);
		contributeToViewMenu(actionBars.getMenuManager());
	}
	
	private void contributeToViewMenu(IMenuManager viewMenu) {
		viewMenu.add(new Separator(VIEWMENU_LAYOUT_GROUP));

		// Create layout sub menu
		
		IMenuManager layoutSubMenu= new MenuManager(PackagesMessages.LayoutActionGroup_label);
		layoutSubMenu.add(fFlatLayoutAction);
		layoutSubMenu.add(fHierarchicalLayoutAction);
		
		viewMenu.add(layoutSubMenu);
		viewMenu.add(fShowLibrariesNode);
	}
}

class LayoutAction extends Action implements IAction {

	private boolean fIsFlatLayout;
	private PackageExplorerPart fPackageExplorer;

	public LayoutAction(PackageExplorerPart packageExplorer, boolean flat) {
		super("", AS_RADIO_BUTTON); //$NON-NLS-1$

		fIsFlatLayout= flat;
		fPackageExplorer= packageExplorer;
		if (fIsFlatLayout) {
			setText(PackagesMessages.LayoutActionGroup_flatLayoutAction_label); 
			JavaPluginImages.setLocalImageDescriptors(this, "flatLayout.gif"); //$NON-NLS-1$
			PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.LAYOUT_FLAT_ACTION);
		} else {
			setText(PackagesMessages.LayoutActionGroup_hierarchicalLayoutAction_label);	  
			JavaPluginImages.setLocalImageDescriptors(this, "hierarchicalLayout.gif"); //$NON-NLS-1$
			PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.LAYOUT_HIERARCHICAL_ACTION);
		}
		setChecked(packageExplorer.isFlatLayout() == fIsFlatLayout);
	}

	/*
	 * @see org.eclipse.jface.action.IAction#run()
	 */
	public void run() {
		if (fPackageExplorer.isFlatLayout() != fIsFlatLayout)
			fPackageExplorer.setFlatLayout(fIsFlatLayout);
	}
}

class ShowLibrariesNodeAction extends Action implements IAction {

	private PackageExplorerPart fPackageExplorer;

	public ShowLibrariesNodeAction(PackageExplorerPart packageExplorer) {
		super(PackagesMessages.LayoutActionGroup_show_libraries_in_group, AS_CHECK_BOX);
		fPackageExplorer= packageExplorer;
		setChecked(packageExplorer.isLibrariesNodeShown());
	}

	/*
	 * @see org.eclipse.jface.action.IAction#run()
	 */
	public void run() {
		fPackageExplorer.setShowLibrariesNode(isChecked());
	}
}
