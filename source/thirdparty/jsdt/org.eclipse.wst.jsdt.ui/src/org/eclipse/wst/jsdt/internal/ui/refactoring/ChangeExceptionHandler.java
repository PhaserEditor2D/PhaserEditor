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
package org.eclipse.wst.jsdt.internal.ui.refactoring;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ProgressMonitorWrapper;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.actions.WorkbenchRunnableAdapter;
import org.eclipse.wst.jsdt.internal.ui.util.ExceptionHandler;

/**
 * Copy of org.eclipse.ltk.internal.ui.refactoring.ChangeExceptionHandler
 *
 */
public class ChangeExceptionHandler {
	
	public static class NotCancelableProgressMonitor extends ProgressMonitorWrapper {
		public NotCancelableProgressMonitor(IProgressMonitor monitor) {
			super(monitor);
		}
		public void setCanceled(boolean b) {
			// ignore set cancel
		}
		public boolean isCanceled() {
			return false;
		}
	}
	
	private Shell fParent;
	private String fName;
	
	private static class RefactorErrorDialog extends ErrorDialog {
		public RefactorErrorDialog(Shell parentShell, String dialogTitle, String dialogMessage, IStatus status, int displayMask) {
			super(parentShell, dialogTitle, dialogMessage, status, displayMask);
		}
		protected void createButtonsForButtonBar(Composite parent) {
			super.createButtonsForButtonBar(parent);
			Button ok= getButton(IDialogConstants.OK_ID);
			ok.setText( RefactoringMessages.ChangeExceptionHandler_undo_button); 
			Button abort= createButton(parent, IDialogConstants.CANCEL_ID, RefactoringMessages.ChangeExceptionHandler_abort_button, true); 
			abort.moveBelow(ok);
			abort.setFocus();
		}
		protected Control createMessageArea (Composite parent) {
			Control result= super.createMessageArea(parent);
			new Label(parent, SWT.NONE); // filler
			Label label= new Label(parent, SWT.NONE);
			label.setText(RefactoringMessages.ChangeExceptionHandler_message); 
			label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			applyDialogFont(result);
			return result;
		}
	}
	
	public ChangeExceptionHandler(Shell parent, Refactoring refactoring) {
		fParent= parent;
		fName= refactoring.getName();
	}
	
	public void handle(Change change, RuntimeException exception) {
		JavaScriptPlugin.log(exception);
		IStatus status= null;
		if (exception.getMessage() == null) {
			status= new Status(IStatus.ERROR, JavaScriptPlugin.getPluginId(), IStatus.ERROR, 
				RefactoringMessages.ChangeExceptionHandler_status_without_detail, exception); 
		} else {
			status= new Status(IStatus.ERROR, JavaScriptPlugin.getPluginId(), IStatus.ERROR, 
				exception.getMessage(), exception);
		}
		handle(change, status);
	}
	
	public void handle(Change change, CoreException exception) {
		JavaScriptPlugin.log(exception);
		handle(change, exception.getStatus());
	}
	
	private void handle(Change change, IStatus status) {
		if (change instanceof CompositeChange) {
			Change undo= ((CompositeChange)change).getUndoUntilException();
			if (undo != null) {
				JavaScriptPlugin.log(status);
				final ErrorDialog dialog= new RefactorErrorDialog(fParent,
					RefactoringMessages.ChangeExceptionHandler_dialog_title, 
					Messages.format(RefactoringMessages.ChangeExceptionHandler_dialog_message, fName), 
					status, IStatus.OK | IStatus.INFO | IStatus.WARNING | IStatus.ERROR); 
				int result= dialog.open();
				if (result == IDialogConstants.OK_ID) {
					performUndo(undo);
				}
				return;
			}
		}
		ErrorDialog dialog= new ErrorDialog(fParent,
			RefactoringMessages.ChangeExceptionHandler_dialog_title, 
			Messages.format(RefactoringMessages.ChangeExceptionHandler_dialog_message, fName), 
			status, IStatus.OK | IStatus.INFO | IStatus.WARNING | IStatus.ERROR); 
		dialog.open();
	}
	
	private void performUndo(final Change undo) {
		IWorkspaceRunnable runnable= new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				monitor.beginTask("", 11);  //$NON-NLS-1$
				try {
					undo.initializeValidationData(new NotCancelableProgressMonitor(new SubProgressMonitor(monitor, 1)));
					if (undo.isValid(new SubProgressMonitor(monitor,1)).hasFatalError()) {
						monitor.done();
						return;
					}
					undo.perform(new SubProgressMonitor(monitor, 9));
				} finally {
					undo.dispose();
				}
			}
		};
		WorkbenchRunnableAdapter adapter= new WorkbenchRunnableAdapter(runnable,
			ResourcesPlugin.getWorkspace().getRoot());
		ProgressMonitorDialog dialog= new ProgressMonitorDialog(fParent);
		try {
			dialog.run(false, false, adapter);
		} catch (InvocationTargetException e) {
			ExceptionHandler.handle(e, fParent, 
				RefactoringMessages.ChangeExceptionHandler_undo_dialog_title,  
				RefactoringMessages.ChangeExceptionHandler_undo_dialog_message + fName); 
		} catch (InterruptedException e) {
			// can't happen
		}
	}
}
