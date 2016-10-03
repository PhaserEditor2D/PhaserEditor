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

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.part.Page;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.wst.jsdt.internal.ui.packageview.PackageExplorerPart;
import org.eclipse.wst.jsdt.ui.IContextMenuConstants;

/**
 * Action group that adds the show actions to a context menu and
 * the action bar's navigate menu.
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
public class ShowActionGroup extends ActionGroup {

	private boolean fIsPackageExplorer;

	private IWorkbenchSite fSite;
	private ShowInPackageViewAction fShowInPackagesViewAction;

	/**
	 * Creates a new <code>ShowActionGroup</code>. The action requires 
	 * that the selection provided by the page's selection provider is of type 
	 * <code>org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param page the page that owns this action group
	 */
	public ShowActionGroup(Page page) {
		this(page.getSite());
	}
	
	/**
	 * Creates a new <code>ShowActionGroup</code>. The action requires 
	 * that the selection provided by the part's selection provider is of type 
	 * <code>org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param part the view part that owns this action group
	 */
	public ShowActionGroup(IViewPart part) {
		this(part.getSite());
		fIsPackageExplorer= part instanceof PackageExplorerPart;
	}
	
	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * @param part the JavaScript editor
	 */
	public ShowActionGroup(JavaEditor part) {
		fShowInPackagesViewAction= new ShowInPackageViewAction(part);
		fShowInPackagesViewAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.SHOW_IN_PACKAGE_VIEW);
		part.setAction("ShowInPackageView", fShowInPackagesViewAction); //$NON-NLS-1$

		initialize(part.getSite(), true);
	}

	private ShowActionGroup(IWorkbenchSite site) {
		fShowInPackagesViewAction= new ShowInPackageViewAction(site);
		fShowInPackagesViewAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.SHOW_IN_PACKAGE_VIEW);
		
		initialize(site , false);		
	}

	private void initialize(IWorkbenchSite site, boolean isJavaEditor) {
		fSite= site;
		ISelectionProvider provider= fSite.getSelectionProvider();
		ISelection selection= provider.getSelection();
		fShowInPackagesViewAction.update(selection);
		if (!isJavaEditor) {
			provider.addSelectionChangedListener(fShowInPackagesViewAction);
		}
	}

	/* (non-Javadoc)
	 * Method declared in ActionGroup
	 */
	public void fillActionBars(IActionBars actionBar) {
		super.fillActionBars(actionBar);
		setGlobalActionHandlers(actionBar);
	}
	
	/* (non-Javadoc)
	 * Method declared in ActionGroup
	 */
	public void fillContextMenu(IMenuManager menu) {
		super.fillContextMenu(menu);
		if (!fIsPackageExplorer) {
			appendToGroup(menu, fShowInPackagesViewAction);
		}
	}
	
	/*
	 * @see ActionGroup#dispose()
	 */
	public void dispose() {
		ISelectionProvider provider= fSite.getSelectionProvider();
		provider.removeSelectionChangedListener(fShowInPackagesViewAction);
		super.dispose();
	}
	
	private void setGlobalActionHandlers(IActionBars actionBar) {
		if (!fIsPackageExplorer)
			actionBar.setGlobalActionHandler(JdtActionConstants.SHOW_IN_PACKAGE_VIEW, fShowInPackagesViewAction);
	}
	
	private void appendToGroup(IMenuManager menu, IAction action) {
		if (action.isEnabled())
			menu.appendToGroup(IContextMenuConstants.GROUP_SHOW, action);
	}		
}
