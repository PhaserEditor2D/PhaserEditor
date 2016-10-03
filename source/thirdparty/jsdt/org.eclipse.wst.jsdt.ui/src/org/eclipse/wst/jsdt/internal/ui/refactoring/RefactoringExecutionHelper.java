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
package org.eclipse.wst.jsdt.internal.ui.refactoring;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IThreadListener;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.PerformChangeOperation;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.ui.refactoring.RefactoringUI;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.actions.WorkbenchRunnableAdapter;

/**
 * A helper class to execute a refactoring. The class takes care of pushing the
 * undo change onto the undo stack and folding editor edits into one editor
 * undo object.
 */
public class RefactoringExecutionHelper {

	private final Refactoring fRefactoring;
	private final Shell fParent;
	private final IRunnableContext fExecContext;
	private final int fStopSeverity;
	private final int fSaveMode;

	private class Operation implements IWorkspaceRunnable {
		public Change fChange;
		public PerformChangeOperation fPerformChangeOperation;
		private final boolean fForked;
		
		public Operation(boolean forked) {
			fForked= forked;
        }
		
		public void run(IProgressMonitor pm) throws CoreException {
			try {
				pm.beginTask("", fForked ? 7 : 11); //$NON-NLS-1$
				pm.subTask(""); //$NON-NLS-1$
				
				final RefactoringStatus status= fRefactoring.checkAllConditions(new SubProgressMonitor(pm, 4, SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK));
				if (status.getSeverity() >= fStopSeverity) {
					final boolean[] canceled= { false };
					if (fForked) {
						fParent.getDisplay().syncExec(new Runnable() {
							public void run() {
								canceled[0]= showStatusDialog(status);
							}
						});
					} else {
						canceled[0]= showStatusDialog(status);
					}
					if (canceled[0]) {
						throw new OperationCanceledException();
					}
				}

				fChange= fRefactoring.createChange(new SubProgressMonitor(pm, 2, SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK));
				fChange.initializeValidationData(new SubProgressMonitor(pm, 1, SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK));
				
				fPerformChangeOperation= RefactoringUI.createUIAwareChangeOperation(fChange);
				fPerformChangeOperation.setUndoManager(RefactoringCore.getUndoManager(), fRefactoring.getName());
				if (fRefactoring instanceof IScheduledRefactoring)
					fPerformChangeOperation.setSchedulingRule(((IScheduledRefactoring)fRefactoring).getSchedulingRule());
				
				if (! fForked)
					fPerformChangeOperation.run(new SubProgressMonitor(pm, 4, SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK));
			} finally {
				pm.done();
			}
		}

		/**
		 * @param status the status to show
		 * @return <code>true</code> iff the operation should be cancelled
		 */
		private boolean showStatusDialog(RefactoringStatus status) {
			Dialog dialog= RefactoringUI.createRefactoringStatusDialog(status, fParent, fRefactoring.getName(), false);
			return dialog.open() == IDialogConstants.CANCEL_ID;
		}
	}
	
	/**
	 * @param refactoring
	 * @param stopSeverity a refactoring status constant from {@link RefactoringStatus}
	 * @param saveMode a save mode from {@link RefactoringSaveHelper}
	 * @param parent
	 * @param context
	 */
	public RefactoringExecutionHelper(Refactoring refactoring, int stopSeverity, int saveMode, Shell parent, IRunnableContext context) {
		super();
		Assert.isNotNull(refactoring);
		Assert.isNotNull(parent);
		Assert.isNotNull(context);
		fRefactoring= refactoring;
		fStopSeverity= stopSeverity;
		fParent= parent;
		fExecContext= context;
		fSaveMode= saveMode;
	}
	
	/**
	 * Must be called in the UI thread.
	 * @param fork if set, the operation will be forked
	 * @param cancelable  if set, the operation will be cancellable
	 * @throws InterruptedException thrown when the operation is cancelled
	 * @throws InvocationTargetException thrown when the operation failed to execute
	 */
	public void perform(boolean fork, boolean cancelable) throws InterruptedException, InvocationTargetException {
		Assert.isTrue(Display.getCurrent() != null);
		final IJobManager manager=  Job.getJobManager();
		final ISchedulingRule rule;
		if (fRefactoring instanceof IScheduledRefactoring) {
			rule= ((IScheduledRefactoring)fRefactoring).getSchedulingRule();
		} else {
			rule= ResourcesPlugin.getWorkspace().getRoot();
		}
		class OperationRunner extends WorkbenchRunnableAdapter implements IThreadListener {
			public OperationRunner(IWorkspaceRunnable runnable, ISchedulingRule schedulingRule) {
				super(runnable, schedulingRule);
			}
			public void threadChange(Thread thread) {
				manager.transferRule(getSchedulingRule(), thread);
			}
		}
		try {
			try {
				Runnable r= new Runnable() {
					public void run() {
						manager.beginRule(rule, null);
					}
				};
				BusyIndicator.showWhile(fParent.getDisplay(), r);
			} catch (OperationCanceledException e) {
				throw new InterruptedException(e.getMessage());
			}
			
			RefactoringSaveHelper saveHelper= new RefactoringSaveHelper(fSaveMode);
			if (!saveHelper.saveEditors(fParent))
				throw new InterruptedException();
			final Operation op= new Operation(fork);
			fRefactoring.setValidationContext(fParent);
			try{
				fExecContext.run(fork, cancelable, new OperationRunner(op, rule));
				if (fork && op.fPerformChangeOperation != null)
					fExecContext.run(false, false, new OperationRunner(op.fPerformChangeOperation, rule));

				if (op.fPerformChangeOperation != null) {
					RefactoringStatus validationStatus= op.fPerformChangeOperation.getValidationStatus();
					if (validationStatus != null && validationStatus.hasFatalError()) {
						MessageDialog.openError(fParent, fRefactoring.getName(), 
								Messages.format(
										RefactoringMessages.RefactoringExecutionHelper_cannot_execute, 
										validationStatus.getMessageMatchingSeverity(RefactoringStatus.FATAL)));
						return;
					}
				}
			} catch (InvocationTargetException e) {
				PerformChangeOperation pco= op.fPerformChangeOperation;
				if (pco != null && pco.changeExecutionFailed()) {
					ChangeExceptionHandler handler= new ChangeExceptionHandler(fParent, fRefactoring);
					Throwable inner= e.getTargetException();
					if (inner instanceof RuntimeException) {
						handler.handle(pco.getChange(), (RuntimeException)inner);
					} else if (inner instanceof CoreException) {
						handler.handle(pco.getChange(), (CoreException)inner);
					} else {
						throw e;
					}
				} else {
					throw e;
				}
			} catch (OperationCanceledException e) {
				throw new InterruptedException(e.getMessage());
			} finally {
				saveHelper.triggerBuild();
			}
		} finally {
			manager.endRule(rule);
			fRefactoring.setValidationContext(null);
		}
	}	
}
