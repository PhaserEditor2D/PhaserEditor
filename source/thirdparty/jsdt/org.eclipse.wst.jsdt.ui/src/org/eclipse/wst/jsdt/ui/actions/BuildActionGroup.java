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

import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.actions.BuildAction;
import org.eclipse.ui.ide.IDEActionFactory;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.internal.ui.actions.ActionMessages;
import org.eclipse.wst.jsdt.ui.IContextMenuConstants;

/**
 * Contributes all build related actions to the context menu and installs handlers for the 
 * corresponding global menu actions.
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
public class BuildActionGroup extends ActionGroup {

	private IWorkbenchSite fSite;
	
	private BuildAction fBuildAction;
 	private RefreshAction fRefreshAction;

	/**
	 * Creates a new <code>BuildActionGroup</code>. The group requires that
	 * the selection provided by the view part's selection provider is of type
	 * <code>org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param part the view part that owns this action group
	 */
	public BuildActionGroup(IViewPart part) {
		fSite= part.getSite();
		Shell shell= fSite.getShell();
		ISelectionProvider provider= fSite.getSelectionProvider();
		
		fBuildAction= new BuildAction(shell, IncrementalProjectBuilder.INCREMENTAL_BUILD);
		fBuildAction.setText(ActionMessages.BuildAction_label); 
		fBuildAction.setActionDefinitionId("org.eclipse.ui.project.buildProject"); //$NON-NLS-1$
		
		fRefreshAction= new RefreshAction(fSite);
		fRefreshAction.setActionDefinitionId("org.eclipse.ui.file.refresh"); //$NON-NLS-1$
		
		provider.addSelectionChangedListener(fBuildAction);
		provider.addSelectionChangedListener(fRefreshAction);
	}
	
	/**
	 * Returns the refresh action managed by this group.
	 * 
	 * @return the refresh action. If this group doesn't manage a refresh action
	 * 	<code>null</code> is returned
	 */
	public IAction getRefreshAction() {
		return fRefreshAction;
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
		ISelection selection= getContext().getSelection();
		if (!ResourcesPlugin.getWorkspace().isAutoBuilding() && isBuildTarget(selection)) {
			appendToGroup(menu, fBuildAction);
		}
		appendToGroup(menu, fRefreshAction);
		super.fillContextMenu(menu);
	}
	
	/* (non-Javadoc)
	 * Method declared in ActionGroup
	 */
	public void dispose() {
		ISelectionProvider provider= fSite.getSelectionProvider();
		provider.removeSelectionChangedListener(fBuildAction);
		provider.removeSelectionChangedListener(fRefreshAction);
		super.dispose();
	}	
	
	private void setGlobalActionHandlers(IActionBars actionBar) {
		actionBar.setGlobalActionHandler(IDEActionFactory.BUILD_PROJECT.getId(), fBuildAction);
		actionBar.setGlobalActionHandler(ActionFactory.REFRESH.getId(), fRefreshAction);
	}
	
	private void appendToGroup(IMenuManager menu, IAction action) {
		if (action.isEnabled())
			menu.appendToGroup(IContextMenuConstants.GROUP_BUILD, action);
	}
	
	private boolean isBuildTarget(ISelection s) {
		if (!(s instanceof IStructuredSelection))
			return false;
		IStructuredSelection selection= (IStructuredSelection)s;
		if (selection.size() != 1)
			return false;
		return selection.getFirstElement() instanceof IJavaScriptProject;
	}
}
