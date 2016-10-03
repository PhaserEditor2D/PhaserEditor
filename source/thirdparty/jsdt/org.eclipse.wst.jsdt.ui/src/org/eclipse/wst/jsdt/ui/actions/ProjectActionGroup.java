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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.actions.CloseResourceAction;
import org.eclipse.ui.actions.CloseUnrelatedProjectsAction;
import org.eclipse.ui.ide.IDEActionFactory;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.ui.IContextMenuConstants;

/**
 * Adds actions to open and close a project to the global menu bar.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 *
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 */
public class ProjectActionGroup extends ActionGroup {

	private IWorkbenchSite fSite;

	private OpenProjectAction fOpenAction;
	private CloseResourceAction fCloseAction;
	private CloseResourceAction fCloseUnrelatedAction;
	
	private ISelectionChangedListener fSelectionChangedListener;

	/**
	 * Creates a new <code>ProjectActionGroup</code>. The group requires
	 * that the selection provided by the site's selection provider is of type <code>
	 * org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param part the view part that owns this action group
	 */
	public ProjectActionGroup(IViewPart part) {
		fSite = part.getSite();
		Shell shell= fSite.getShell();
		ISelectionProvider provider= fSite.getSelectionProvider();
		ISelection selection= provider.getSelection();
		
		fCloseAction= new CloseResourceAction(shell);
		fCloseAction.setActionDefinitionId("org.eclipse.ui.project.closeProject"); //$NON-NLS-1$
		
		fCloseUnrelatedAction= new CloseUnrelatedProjectsAction(shell);
		fCloseUnrelatedAction.setActionDefinitionId("org.eclipse.ui.project.closeUnrelatedProjects"); //$NON-NLS-1$
		
		fOpenAction= new OpenProjectAction(fSite);
		fOpenAction.setActionDefinitionId("org.eclipse.ui.project.openProject"); //$NON-NLS-1$
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection s= (IStructuredSelection)selection;
			fOpenAction.selectionChanged(s);
			fCloseAction.selectionChanged(s);
			fCloseUnrelatedAction.selectionChanged(s);
		}
		
		fSelectionChangedListener= new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				ISelection s= event.getSelection();
				if (s instanceof IStructuredSelection) {
					performSelectionChanged((IStructuredSelection) s);
				}
			}
		};
		provider.addSelectionChangedListener(fSelectionChangedListener);

		IWorkspace workspace= ResourcesPlugin.getWorkspace();
		workspace.addResourceChangeListener(fOpenAction);
		workspace.addResourceChangeListener(fCloseAction);
		workspace.addResourceChangeListener(fCloseUnrelatedAction);
	}
	
	protected void performSelectionChanged(IStructuredSelection structuredSelection) {
		Object[] array= structuredSelection.toArray();
		ArrayList openProjects= new ArrayList();
		int selectionStatus= evaluateSelection(array, openProjects);
		StructuredSelection sel= new StructuredSelection(openProjects);

		fOpenAction.setEnabled(selectionStatus == CLOSED_PROJECTS_SELECTED || (selectionStatus == 0 && hasClosedProjectsInWorkspace()));
		fCloseAction.selectionChanged(sel);
		fCloseUnrelatedAction.selectionChanged(sel);
	}
	
	private int CLOSED_PROJECTS_SELECTED= 1;
	private int NON_PROJECT_SELECTED= 2;

	private int evaluateSelection(Object[] array, List allOpenProjects) {
		int status= 0;
		for (int i= 0; i < array.length; i++) {
			Object curr= array[i];
			if (curr instanceof IJavaScriptProject) {
				curr= ((IJavaScriptProject) curr).getProject();
			}
			if (curr instanceof IProject) {
				IProject project= (IProject) curr;
				if (project.isOpen()) {
					allOpenProjects.add(project);
				} else {
					status |= CLOSED_PROJECTS_SELECTED;
				}
			} else {
				if (curr instanceof IWorkingSet) {
					int res= evaluateSelection(((IWorkingSet) curr).getElements(), allOpenProjects);
					status |= res;
				} else {
					status |= NON_PROJECT_SELECTED;
				}
			}
		}
		return status;
	}
		
	private boolean hasClosedProjectsInWorkspace() {
		IProject[] projects= ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (int i = 0; i < projects.length; i++) {
			if (!projects[i].isOpen())
				return true;
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * Method declared in ActionGroup
	 */
	public void fillActionBars(IActionBars actionBars) {
		super.fillActionBars(actionBars);
		actionBars.setGlobalActionHandler(IDEActionFactory.CLOSE_PROJECT.getId(), fCloseAction);
		actionBars.setGlobalActionHandler(IDEActionFactory.CLOSE_UNRELATED_PROJECTS.getId(), fCloseUnrelatedAction);
		actionBars.setGlobalActionHandler(IDEActionFactory.OPEN_PROJECT.getId(), fOpenAction);
	}
	
	/* (non-Javadoc)
	 * Method declared in ActionGroup
	 */
	public void fillContextMenu(IMenuManager menu) {
		super.fillContextMenu(menu);
		if (fOpenAction.isEnabled())
			menu.appendToGroup(IContextMenuConstants.GROUP_BUILD, fOpenAction);
		if (fCloseAction.isEnabled())
			menu.appendToGroup(IContextMenuConstants.GROUP_BUILD, fCloseAction);
		if (fCloseUnrelatedAction.isEnabled() && areOnlyProjectsSelected(fCloseUnrelatedAction.getStructuredSelection()))
			menu.appendToGroup(IContextMenuConstants.GROUP_BUILD, fCloseUnrelatedAction);
	}
	
	/**
	 * Returns the open project action contained in this project action group.
	 * 
	 * @return returns the open project action
	 * 
	 * 
	 */
	public OpenProjectAction getOpenProjectAction() {
		return fOpenAction;
	}

	private boolean areOnlyProjectsSelected(IStructuredSelection selection) {
		if (selection.isEmpty())
			return false;
		
		Iterator iter= selection.iterator();
		while (iter.hasNext()) {
			Object obj= iter.next();
			if (obj instanceof IAdaptable) {
				if (((IAdaptable)obj).getAdapter(IProject.class) == null)
					return false;
			}
		}
		return true;
	}

	/*
	 * @see ActionGroup#dispose()
	 */
	public void dispose() {
		ISelectionProvider provider= fSite.getSelectionProvider();
		provider.removeSelectionChangedListener(fSelectionChangedListener);
		
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		workspace.removeResourceChangeListener(fOpenAction);
		workspace.removeResourceChangeListener(fCloseAction);
		workspace.removeResourceChangeListener(fCloseUnrelatedAction);
		super.dispose();
	}
}
