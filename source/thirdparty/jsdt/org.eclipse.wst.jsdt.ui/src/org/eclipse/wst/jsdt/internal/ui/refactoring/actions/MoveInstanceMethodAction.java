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
package org.eclipse.wst.jsdt.internal.ui.refactoring.actions;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringAvailabilityTester;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringExecutionStarter;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.actions.ActionUtil;
import org.eclipse.wst.jsdt.internal.ui.actions.SelectionConverter;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.JavaTextSelection;
import org.eclipse.wst.jsdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.wst.jsdt.internal.ui.util.ExceptionHandler;
import org.eclipse.wst.jsdt.ui.actions.SelectionDispatchAction;

public final class MoveInstanceMethodAction extends SelectionDispatchAction {

	private JavaEditor fEditor;
	
	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * @param editor the java editor
	 */
	public MoveInstanceMethodAction(JavaEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(SelectionConverter.canOperateOn(fEditor));
	}

	public MoveInstanceMethodAction(IWorkbenchSite site) {
		super(site);
		setText(RefactoringMessages.MoveInstanceMethodAction_Move_Method); 
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.MOVE_ACTION);		
	}

	/*
	 * @see SelectionDispatchAction#selectionChanged(IStructuredSelection)
	 */
	public void selectionChanged(IStructuredSelection selection) {
		try {
			setEnabled(RefactoringAvailabilityTester.isMoveMethodAvailable(selection));
		} catch (JavaScriptModelException e) {
			// http://bugs.eclipse.org/bugs/show_bug.cgi?id=19253
			if (JavaModelUtil.isExceptionToBeLogged(e))
				JavaScriptPlugin.log(e);
			setEnabled(false);//no ui
		}
	}

	public void selectionChanged(ITextSelection selection) {
		setEnabled(true);
    }

	/**
	 * Note: This method is for internal use only. Clients should not call this method.
	 */
	public void selectionChanged(JavaTextSelection selection) {
		try {
			setEnabled(RefactoringAvailabilityTester.isMoveMethodAvailable(selection));
		} catch (CoreException e) {
			setEnabled(false);
		}
	}
	
	private static IFunction getSingleSelectedMethod(IStructuredSelection selection) {
		if (selection.isEmpty() || selection.size() != 1) 
			return null;
		
		Object first= selection.getFirstElement();
		if (! (first instanceof IFunction))
			return null;
		return (IFunction) first;
	}
	/*
	 * @see SelectionDispatchAction#run(IStructuredSelection)
	 */
	public void run(IStructuredSelection selection) {		
		try {
			Assert.isTrue(RefactoringAvailabilityTester.isMoveMethodAvailable(selection));
			IFunction method= getSingleSelectedMethod(selection);
			Assert.isNotNull(method);
			if (!ActionUtil.isEditable(fEditor, getShell(), method))
				return;
			RefactoringExecutionStarter.startMoveMethodRefactoring(method, getShell());
		} catch (JavaScriptModelException e) {
			ExceptionHandler.handle(e, getShell(), RefactoringMessages.MoveInstanceMethodAction_dialog_title, RefactoringMessages.MoveInstanceMethodAction_unexpected_exception);	 
		}
 	}	
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */		
	public void run(ITextSelection selection) {
		try {
			run(selection, SelectionConverter.getInputAsCompilationUnit(fEditor));
		} catch (JavaScriptModelException e) {
			ExceptionHandler.handle(e, getShell(), RefactoringMessages.MoveInstanceMethodAction_dialog_title, RefactoringMessages.MoveInstanceMethodAction_unexpected_exception);	 
		}
	}

	private void run(ITextSelection selection, IJavaScriptUnit cu) throws JavaScriptModelException {
		Assert.isNotNull(cu);
		Assert.isTrue(selection.getOffset() >= 0);
		Assert.isTrue(selection.getLength() >= 0);

		if (!ActionUtil.isEditable(fEditor, getShell(), cu))
			return;

		IFunction method= getMethod(cu, selection);
		if (method != null) {
			RefactoringExecutionStarter.startMoveMethodRefactoring(method, getShell());
		} else {
			MessageDialog.openInformation(getShell(), RefactoringMessages.MoveInstanceMethodAction_dialog_title, RefactoringMessages.MoveInstanceMethodAction_No_reference_or_declaration); 
		}
	}

	private static IFunction getMethod(IJavaScriptUnit cu, ITextSelection selection) throws JavaScriptModelException {
		IJavaScriptElement element= SelectionConverter.getElementAtOffset(cu, selection);
		if (element instanceof IFunction)
			return (IFunction) element;
		return null;
	}
}
