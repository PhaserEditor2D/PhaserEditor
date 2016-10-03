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
package org.eclipse.wst.jsdt.internal.ui.wizards;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.templates.persistence.TemplateStore;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.internal.ui.IUIConstants;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.actions.WorkbenchRunnableAdapter;
import org.eclipse.wst.jsdt.internal.ui.dialogs.OptionalMessageDialog;
import org.eclipse.wst.jsdt.internal.ui.util.ExceptionHandler;

public abstract class NewElementWizard extends Wizard implements INewWizard {

	private IWorkbench fWorkbench;
	private IStructuredSelection fSelection;

	public NewElementWizard() {
		setNeedsProgressMonitor(true);
	}
			
	protected void openResource(final IFile resource) {
		final IWorkbenchPage activePage= JavaScriptPlugin.getActivePage();
		if (activePage != null) {
			final Display display= getShell().getDisplay();
			if (display != null) {
				display.asyncExec(new Runnable() {
					public void run() {
						try {
							IDE.openEditor(activePage, resource, true);
						} catch (PartInitException e) {
							JavaScriptPlugin.log(e);
						}
					}
				});
			}
		}
	}
	
	/**
	 * Subclasses should override to perform the actions of the wizard.
	 * This method is run in the wizard container's context as a workspace runnable.
	 * @param monitor
	 * @throws InterruptedException
	 * @throws CoreException
	 */
	protected abstract void finishPage(IProgressMonitor monitor) throws InterruptedException, CoreException;
	
	/**
	 * Returns the scheduling rule for creating the element.
	 */
	protected ISchedulingRule getSchedulingRule() {
		return ResourcesPlugin.getWorkspace().getRoot(); // look all by default
	}
	
	
	protected boolean canRunForked() {
		return true;
	}
	
	public abstract IJavaScriptElement getCreatedElement();
	
	protected void handleFinishException(Shell shell, InvocationTargetException e) {
		String title= NewWizardMessages.NewElementWizard_op_error_title; 
		String message= NewWizardMessages.NewElementWizard_op_error_message; 
		ExceptionHandler.handle(e, shell, title, message);
	}
	
	/*
	 * @see Wizard#performFinish
	 */		
	public boolean performFinish() {
		IWorkspaceRunnable op= new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException, OperationCanceledException {
				try {
					finishPage(monitor);
				} catch (InterruptedException e) {
					throw new OperationCanceledException(e.getMessage());
				}
			}
		};
		try {
			ISchedulingRule rule= null;
			Job job= Job.getJobManager().currentJob();
			if (job != null)
				rule= job.getRule();
			IRunnableWithProgress runnable= null;
			if (rule != null)
				runnable= new WorkbenchRunnableAdapter(op, rule, true);
			else
				runnable= new WorkbenchRunnableAdapter(op, getSchedulingRule());
			getContainer().run(canRunForked(), true, runnable);
		} catch (InvocationTargetException e) {
			handleFinishException(getShell(), e);
			return false;
		} catch  (InterruptedException e) {
			return false;
		}
		return true;
	}
	
	protected void warnAboutTypeCommentDeprecation() {
		String key= IUIConstants.DIALOGSTORE_TYPECOMMENT_DEPRECATED;
		if (OptionalMessageDialog.isDialogEnabled(key)) {
			TemplateStore templates= JavaScriptPlugin.getDefault().getTemplateStore();
			boolean isOldWorkspace= templates.findTemplate("filecomment") != null && templates.findTemplate("typecomment") != null; //$NON-NLS-1$ //$NON-NLS-2$
			if (!isOldWorkspace) {
				OptionalMessageDialog.setDialogEnabled(key, false);
			}
			String title= NewWizardMessages.NewElementWizard_typecomment_deprecated_title; 
			String message= NewWizardMessages.NewElementWizard_typecomment_deprecated_message; 
			OptionalMessageDialog.open(key, getShell(), title, null, message, MessageDialog.INFORMATION, new String[] { IDialogConstants.OK_LABEL }, 0);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchWizard#init(org.eclipse.ui.IWorkbench, org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void init(IWorkbench workbench, IStructuredSelection currentSelection) {
		fWorkbench= workbench;
		fSelection= currentSelection;
	}
	
	public IStructuredSelection getSelection() {
		return fSelection;
	}

	public IWorkbench getWorkbench() {
		return fWorkbench;
	}

	protected void selectAndReveal(IResource newResource) {
		BasicNewResourceWizard.selectAndReveal(newResource, fWorkbench.getActiveWorkbenchWindow());
	}

}
