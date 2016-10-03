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

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.NewProjectAction;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.util.ExceptionHandler;
import org.eclipse.wst.jsdt.internal.ui.util.PixelConverter;
import org.eclipse.wst.jsdt.internal.ui.wizards.NewElementWizard;
import org.eclipse.wst.jsdt.internal.ui.wizards.NewWizardMessages;

/**
 * <p>Abstract base classed used for the open wizard actions.</p>
 * 
 * <p>
 * Note: This class is for internal use only. Clients should not use this class.
 * </p>
  * Provisional API: This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 */
public abstract class AbstractOpenWizardAction extends Action {
	
	private Shell fShell;
	private IStructuredSelection fSelection;
	private IJavaScriptElement fCreatedElement;
	
	/**
	 * Creates the action.
	 */
	protected AbstractOpenWizardAction() {
		fShell= null;
		fSelection= null;
		fCreatedElement= null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#run()
	 */
	public void run() {
		Shell shell= getShell();
		if (!doCreateProjectFirstOnEmptyWorkspace(shell)) {
			return;
		}
		try {
			INewWizard wizard= createWizard();
			wizard.init(PlatformUI.getWorkbench(), getSelection());
			
			WizardDialog dialog= new WizardDialog(shell, wizard);
			PixelConverter converter= new PixelConverter(JFaceResources.getDialogFont());
			dialog.setMinimumPageSize(converter.convertWidthInCharsToPixels(70), converter.convertHeightInCharsToPixels(20));
			dialog.create();
			int res= dialog.open();
			if (res == Window.OK && wizard instanceof NewElementWizard) {
				fCreatedElement= ((NewElementWizard)wizard).getCreatedElement();
			}
			
			notifyResult(res == Window.OK);
		} catch (CoreException e) {
			String title= NewWizardMessages.AbstractOpenWizardAction_createerror_title; 
			String message= NewWizardMessages.AbstractOpenWizardAction_createerror_message; 
			ExceptionHandler.handle(e, shell, title, message);
		}
	}
	
	/**
	 * Creates and configures the wizard. This method should only be called once.
	 * @return returns the created wizard.
	 * @throws CoreException exception is thrown when the creation was not successful.
	 */
	abstract protected INewWizard createWizard() throws CoreException;
	
	/**
	 * Returns the configured selection. If no selection has been configured using {@link #setSelection(IStructuredSelection)},
	 * the currently selected element of the active workbench is returned.
	 * @return the configured selection
	 */
	protected IStructuredSelection getSelection() {
		if (fSelection == null) {
			return evaluateCurrentSelection();
		}
		return fSelection;
	}
			
	private IStructuredSelection evaluateCurrentSelection() {
		IWorkbenchWindow window= JavaScriptPlugin.getActiveWorkbenchWindow();
		if (window != null) {
			ISelection selection= window.getSelectionService().getSelection();
			if (selection instanceof IStructuredSelection) {
				return (IStructuredSelection) selection;
			}
		}
		return StructuredSelection.EMPTY;
	}
	
	/**
	 * Configures the selection to be used as initial selection of the wizard. 
	 * @param selection the selection to be set or <code>null</code> to use the selection of the active workbench window
	 */
	public void setSelection(IStructuredSelection selection) {
		fSelection= selection;
	}
	
	/**
	 * Returns the configured shell. If no shell has been configured using {@link #setShell(Shell)},
	 * 	the shell of the currently active workbench is returned.
	 * @return the configured shell
	 */
	protected Shell getShell() {
		if (fShell == null) {
			return JavaScriptPlugin.getActiveWorkbenchShell();
		}
		return fShell;
	}
	
	/**
	 * Configures the shell to be used as parent shell by the wizard.
	 * @param shell the shell to be set or <code>null</code> to use the shell of the active workbench window
	 */
	public void setShell(Shell shell) {
		fShell= shell;
	}
	
	/**
	 * Opens the new project dialog if the workspace is empty. This method is called on {@link #run()}.
	 * @param shell the shell to use
	 * @return returns <code>true</code> when a project has been created, or <code>false</code> when the
	 * new project has been canceled.
	 */
	protected boolean doCreateProjectFirstOnEmptyWorkspace(Shell shell) {
		IWorkspaceRoot workspaceRoot= ResourcesPlugin.getWorkspace().getRoot();
		if (workspaceRoot.getProjects().length == 0) {
			String title= NewWizardMessages.AbstractOpenWizardAction_noproject_title; 
			String message= NewWizardMessages.AbstractOpenWizardAction_noproject_message; 
			if (MessageDialog.openQuestion(shell, title, message)) {
				new NewProjectAction().run();
				return workspaceRoot.getProjects().length != 0;
			}
			return false;
		}
		return true;
	}
	
	/**
	 * Returns the created element or <code>null</code> if the wizard has not run or was canceled.
	 * @return the created element or <code>null</code>
	 */
	public IJavaScriptElement getCreatedElement() {
		return fCreatedElement;
	}
	
	
}
