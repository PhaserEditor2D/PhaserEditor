/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.ui.packageview.JsGlobalScopeContainer;
import org.eclipse.wst.jsdt.internal.ui.util.ExceptionHandler;
import org.eclipse.wst.jsdt.ui.wizards.BuildPathDialogAccess;

/**
 * Action to open a dialog to configure classpath containers. Added as a <code>objectContribution</code>
 * to {@link JsGlobalScopeContainer}.
 */
public class ConfigureContainerAction implements IObjectActionDelegate {

	private ISelection fCurrentSelection;
	private IWorkbenchPart fPart;

	/*
	 * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		fPart= targetPart;
	}

	/*
	 * @see IActionDelegate#run(IAction)
	 */
	public void run(IAction action) {
		if (fCurrentSelection instanceof IStructuredSelection) {
			JsGlobalScopeContainer container= (JsGlobalScopeContainer) ((IStructuredSelection) fCurrentSelection).getFirstElement();
			openWizard(container.getClasspathEntry(), container.getLabel(), container.getJavaProject());
		}
	}
	
	private void openWizard(IIncludePathEntry entry, String label, final IJavaScriptProject project) {
		Shell shell= fPart.getSite().getShell();
		try {
			IIncludePathEntry[] entries= project.getRawIncludepath();
			
			IIncludePathEntry result= BuildPathDialogAccess.configureContainerEntry(shell, entry, project, entries);
			if (result == null || result.equals(entry)) {
				return; // user cancelled or no changes
			}

			int idx= indexInClasspath(entries, entry);
			if (idx == -1) {
				return;
			}
			
			final IIncludePathEntry[] newEntries= new IIncludePathEntry[entries.length];
			System.arraycopy(entries, 0, newEntries, 0, entries.length);
			newEntries[idx]= result;
			
			IRunnableContext context= fPart.getSite().getWorkbenchWindow();
			if (context == null) {
				context= PlatformUI.getWorkbench().getProgressService();
			}
			context.run(true, true, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {			
						project.setRawIncludepath(newEntries, monitor);
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					}
				}
			});
		} catch (JavaScriptModelException e) {
			String title= ActionMessages.ConfigureContainerAction_error_title; 
			String message= ActionMessages.ConfigureContainerAction_error_creationfailed_message; 
			ExceptionHandler.handle(e, shell, title, message);
		} catch (InvocationTargetException e) {
			String title= ActionMessages.ConfigureContainerAction_error_title; 
			String message= ActionMessages.ConfigureContainerAction_error_applyingfailed_message; 
			ExceptionHandler.handle(e, shell, title, message);
		} catch (InterruptedException e) {
			// user cancelled
		}
	}
	
	protected static int indexInClasspath(IIncludePathEntry[] entries, IIncludePathEntry entry) {
		for (int i= 0; i < entries.length; i++) {
			if (entries[i] == entry) {
				return i;
			}
		}
		return -1;
	}

	/*
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		fCurrentSelection= selection;
	}

}
