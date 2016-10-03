/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sebastian Davids - Fixed bug 25898
 *******************************************************************************/
package org.eclipse.wst.jsdt.ui.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.PerformChangeOperation;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;
import org.eclipse.ltk.ui.refactoring.RefactoringUI;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.ISourceRange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.wst.jsdt.internal.corext.refactoring.surround.ISurroundWithTryCatchQuery;
import org.eclipse.wst.jsdt.internal.corext.refactoring.surround.SurroundWithTryCatchRefactoring;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.actions.ActionUtil;
import org.eclipse.wst.jsdt.internal.ui.actions.SelectionConverter;
import org.eclipse.wst.jsdt.internal.ui.actions.WorkbenchRunnableAdapter;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.wst.jsdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.wst.jsdt.internal.ui.util.BusyIndicatorRunnableContext;
import org.eclipse.wst.jsdt.internal.ui.util.ElementValidator;
import org.eclipse.wst.jsdt.internal.ui.util.ExceptionHandler;

/**
 * Action to surround a set of statements with a try/catch block.
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
public class SurroundWithTryCatchAction extends SelectionDispatchAction {

	private CompilationUnitEditor fEditor;

	private static class Query implements ISurroundWithTryCatchQuery {
		private Shell fParent;
		public Query(Shell shell) {
			fParent= shell;
		}
		public boolean catchRuntimeException() {
			MessageDialog dialog = new MessageDialog(
				fParent, getDialogTitle(),  null,	// accept the default window icon
				RefactoringMessages.SurroundWithTryCatchAction_no_exceptions, 
				MessageDialog.QUESTION, 
				new String[] {IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL}, 
				1) {
					// Work around for http://dev.eclipse.org/bugs/show_bug.cgi?id=18303
					protected void createButtonsForButtonBar(Composite parent) {
						super.createButtonsForButtonBar(parent);
						Button button= getButton(1);
						if (button != null)
							button.setFocus();
					}
			};
			return dialog.open() == 0; // yes selected
		}
	}

	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * @param editor the compilation unit editor
	 */
	public SurroundWithTryCatchAction(CompilationUnitEditor editor) {
		super(editor.getEditorSite());
		setText(RefactoringMessages.SurroundWithTryCatchAction_label); 
		fEditor= editor;
		setEnabled((fEditor != null && SelectionConverter.getInputAsCompilationUnit(fEditor) != null));
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.SURROUND_WITH_TRY_CATCH_ACTION);
	}

	public void run(ITextSelection selection) {
		if (!ActionUtil.isEditable(fEditor))
			return;
		IJavaScriptUnit cu= SelectionConverter.getInputAsCompilationUnit(fEditor);
		if (cu == null || !ElementValidator.checkValidateEdit(cu, getShell(), getDialogTitle()))
			return;
		SurroundWithTryCatchRefactoring refactoring= SurroundWithTryCatchRefactoring.create(cu, selection, new Query(getShell()));
		
		if (refactoring == null)
			return;
		try {
			RefactoringStatus status= refactoring.checkInitialConditions(new NullProgressMonitor());
			if (status.hasFatalError()) {
				RefactoringStatusEntry entry= status.getEntryMatchingSeverity(RefactoringStatus.FATAL);
				MessageDialog.openInformation(getShell(), getDialogTitle(), entry.getMessage());
				if (entry.getContext() instanceof JavaStatusContext && fEditor != null) {
					JavaStatusContext context= (JavaStatusContext)entry.getContext();
					ISourceRange range= context.getSourceRange();
					fEditor.setHighlightRange(range.getOffset(), range.getLength(), true);
				}
				return;
			}
			if (refactoring.stopExecution())
				return;
			Change change= refactoring.createChange(new NullProgressMonitor());
			change.initializeValidationData(new NullProgressMonitor());
			PerformChangeOperation op= RefactoringUI.createUIAwareChangeOperation(change);
			// must be fork == false since file buffers can't be manipulated in a different thread.
			WorkbenchRunnableAdapter adapter= new WorkbenchRunnableAdapter(op);
			PlatformUI.getWorkbench().getProgressService().runInUI(
				new BusyIndicatorRunnableContext(), adapter, adapter.getSchedulingRule());
		} catch (CoreException e) {
			ExceptionHandler.handle(e, getDialogTitle(), RefactoringMessages.SurroundWithTryCatchAction_exception); 
		} catch (InvocationTargetException e) {
			ExceptionHandler.handle(e, getDialogTitle(), RefactoringMessages.SurroundWithTryCatchAction_exception); 
		} catch (InterruptedException e) {
			// not cancelable
		}
	}

	public void selectionChanged(ITextSelection selection) {
		setEnabled(selection.getLength() > 0 && (fEditor != null && SelectionConverter.getInputAsCompilationUnit(fEditor) != null));
	}

	private static String getDialogTitle() {
		return RefactoringMessages.SurroundWithTryCatchAction_dialog_title; 
	}
}
