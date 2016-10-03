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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.OpenResourceAction;
import org.eclipse.ui.dialogs.ListSelectionDialog;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.actions.ActionMessages;
import org.eclipse.wst.jsdt.internal.ui.actions.WorkbenchRunnableAdapter;
import org.eclipse.wst.jsdt.internal.ui.util.ExceptionHandler;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabelProvider;

/**
 * Action to open a closed project. Action either opens the closed projects
 * provided by the structured selection or presents a dialog from which the
 * user can select the projects to be opened.
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
public class OpenProjectAction extends SelectionDispatchAction implements IResourceChangeListener {
	
	private int CLOSED_PROJECTS_SELECTED= 1;
	private int OTHER_ELEMENTS_SELECTED= 2;
	
	private OpenResourceAction fWorkbenchAction;

	/**
	 * Creates a new <code>OpenProjectAction</code>. The action requires
	 * that the selection provided by the site's selection provider is of type <code>
	 * org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param site the site providing context information for this action
	 */
	public OpenProjectAction(IWorkbenchSite site) {
		super(site);
		fWorkbenchAction= new OpenResourceAction(site.getShell());
		setText(fWorkbenchAction.getText());
		setToolTipText(fWorkbenchAction.getToolTipText());
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.OPEN_PROJECT_ACTION);
		setEnabled(hasClosedProjectsInWorkspace());
	}
	
	/*
	 * @see IResourceChangeListener#resourceChanged(IResourceChangeEvent)
	 */
	public void resourceChanged(IResourceChangeEvent event) {
		IResourceDelta delta = event.getDelta();
		if (delta != null) {
			IResourceDelta[] projDeltas = delta.getAffectedChildren(IResourceDelta.CHANGED);
			for (int i = 0; i < projDeltas.length; ++i) {
				IResourceDelta projDelta = projDeltas[i];
				if ((projDelta.getFlags() & IResourceDelta.OPEN) != 0) {
					setEnabled(hasClosedProjectsInWorkspace());
					return;
				}
			}
		}
	}
	
	//---- normal selection -------------------------------------
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.ui.actions.SelectionDispatchAction#selectionChanged(org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(ISelection selection) {
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.ui.actions.SelectionDispatchAction#run(org.eclipse.jface.viewers.ISelection)
	 */
	public void run(ISelection selection) {
		internalRun(null);
	}
	
	private int evaluateSelection(IStructuredSelection selection, List allClosedProjects) {
		Object[] array= selection.toArray();
		int selectionStatus = 0;
		for (int i= 0; i < array.length; i++) {
			Object curr= array[i];
			if (isClosedProject(curr)) {
				if (allClosedProjects != null)
					allClosedProjects.add(curr);
				selectionStatus |= CLOSED_PROJECTS_SELECTED;
			} else {
				if (curr instanceof IWorkingSet) {
					IAdaptable[] elements= ((IWorkingSet) curr).getElements();
					for (int k= 0; k < elements.length; k++) {
						Object elem= elements[k];
						if (isClosedProject(elem)) {
							if (allClosedProjects != null)
								allClosedProjects.add(elem);
							selectionStatus |= CLOSED_PROJECTS_SELECTED;
						}
					}
				}
				selectionStatus |= OTHER_ELEMENTS_SELECTED;
			}

		}
		return selectionStatus;
	}
	
	private static boolean isClosedProject(Object element) {
		// assume all closed project are rendered as IProject
		return element instanceof IProject && !((IProject) element).isOpen();
	}
	
	
	//---- structured selection ---------------------------------------
		
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.ui.actions.SelectionDispatchAction#run(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void run(IStructuredSelection selection) {
		ArrayList allClosedProjects= new ArrayList();
		int selectionStatus= evaluateSelection(selection, allClosedProjects);
		if (selectionStatus == CLOSED_PROJECTS_SELECTED) { // only closed projects selected
			fWorkbenchAction.selectionChanged(new StructuredSelection(allClosedProjects));
			fWorkbenchAction.run();
		} else {
			internalRun(allClosedProjects);
		}
	}
	
	private void internalRun(List initialSelection) {
		ListSelectionDialog dialog= new ListSelectionDialog(getShell(), getClosedProjectsInWorkspace(), new ArrayContentProvider(), new JavaScriptElementLabelProvider(), ActionMessages.OpenProjectAction_dialog_message);
		dialog.setTitle(ActionMessages.OpenProjectAction_dialog_title); 
		if (initialSelection != null && !initialSelection.isEmpty()) {
			dialog.setInitialElementSelections(initialSelection);
		}
		int result= dialog.open();
		if (result != Window.OK)
			return;
		final Object[] projects= dialog.getResult();
		IWorkspaceRunnable runnable= createRunnable(projects);
		try {
			PlatformUI.getWorkbench().getProgressService().run(true, true, new WorkbenchRunnableAdapter(runnable));
		} catch (InvocationTargetException e) {
			ExceptionHandler.handle(e, getShell(), ActionMessages.OpenProjectAction_dialog_title, ActionMessages.OpenProjectAction_error_message); 
		} catch (InterruptedException e) {
			// user cancelled
		}
	}
	
	private IWorkspaceRunnable createRunnable(final Object[] projects) {
		return new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				monitor.beginTask("", projects.length); //$NON-NLS-1$
				MultiStatus errorStatus= null;
				for (int i = 0; i < projects.length; i++) {
					IProject project= (IProject)projects[i];
					try {
						project.open(new SubProgressMonitor(monitor, 1));
					} catch (CoreException e) {
						if (errorStatus == null)
							errorStatus = new MultiStatus(JavaScriptPlugin.getPluginId(), IStatus.ERROR, ActionMessages.OpenProjectAction_error_message, null); 
						errorStatus.add(e.getStatus());
					}
				}
				monitor.done();
				if (errorStatus != null)
					throw new CoreException(errorStatus);
			}
		};
	}
	
	private Object[] getClosedProjectsInWorkspace() {
		IProject[] projects= ResourcesPlugin.getWorkspace().getRoot().getProjects();
		List result= new ArrayList(5);
		for (int i = 0; i < projects.length; i++) {
			IProject project= projects[i];
			if (!project.isOpen())
				result.add(project);
		}
		return result.toArray();
	}
	
	private boolean hasClosedProjectsInWorkspace() {
		IProject[] projects= ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (int i = 0; i < projects.length; i++) {
			if (!projects[i].isOpen())
				return true;
		}
		return false;
	}
}
