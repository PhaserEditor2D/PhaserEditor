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


import java.io.CharConversionException;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringAvailabilityTester;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringExecutionStarter;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.actions.ActionUtil;
import org.eclipse.wst.jsdt.internal.ui.actions.SelectionConverter;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.JavaTextSelection;
import org.eclipse.wst.jsdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.wst.jsdt.internal.ui.util.ExceptionHandler;

/**
 * Action to convert a nested class to a top level class.
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
public class ConvertNestedToTopAction extends SelectionDispatchAction {

	private JavaEditor fEditor;

	/**
	 * Note: This constructor is for internal use only. Clients should not call
	 * this constructor.
	 * @param editor the JavaScript editor
	 */
	public ConvertNestedToTopAction(JavaEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(SelectionConverter.canOperateOn(fEditor));
	}

	/**
	 * Creates a new <code>MoveInnerToTopAction</code>. The action requires
	 * that the selection provided by the site's selection provider is of type
	 * <code>
	 * org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param site
	 *            the site providing context information for this action
	 */
	public ConvertNestedToTopAction(IWorkbenchSite site) {
		super(site);
		setText(RefactoringMessages.ConvertNestedToTopAction_Convert); 
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.MOVE_INNER_TO_TOP_ACTION);
	}

	//---- Structured selection ------------------------------------------------

	/*
	 * @see SelectionDispatchAction#selectionChanged(IStructuredSelection)
	 */
	public void selectionChanged(IStructuredSelection selection) {
		try {
			setEnabled(RefactoringAvailabilityTester.isMoveInnerAvailable(selection));
		} catch (JavaScriptModelException e) {
			// http://bugs.eclipse.org/bugs/show_bug.cgi?id=19253
			if (!(e.getException() instanceof CharConversionException) && JavaModelUtil.isExceptionToBeLogged(e))
				JavaScriptPlugin.log(e);
			setEnabled(false);//no UI
		}
	}

	/*
	 * @see SelectionDispatchAction#run(IStructuredSelection)
	 */
	public void run(IStructuredSelection selection) {
		try {
			//we have to call this here - no selection changed event is sent
			// after a refactoring but it may still invalidate enablement
			if (RefactoringAvailabilityTester.isMoveInnerAvailable(selection)) {
				IType singleSelectedType= getSingleSelectedType(selection);
				if (! ActionUtil.isEditable(getShell(), singleSelectedType))
					return;
				RefactoringExecutionStarter.startMoveInnerRefactoring(singleSelectedType, getShell());
			}
		} catch (JavaScriptModelException e) {
			ExceptionHandler.handle(e, 
				RefactoringMessages.OpenRefactoringWizardAction_refactoring, 
				RefactoringMessages.OpenRefactoringWizardAction_exception); 
		}
	}

	private static IType getSingleSelectedType(IStructuredSelection selection) throws JavaScriptModelException {
		if (selection.isEmpty() || selection.size() != 1)
			return null;

		Object first= selection.getFirstElement();
		if (first instanceof IType)
			return (IType)first;
		if (first instanceof IJavaScriptUnit)
			return JavaElementUtil.getMainType((IJavaScriptUnit)first);
		return null;
	}

	//---- Text Selection -------------------------------------------------------

	/*
	 * @see SelectionDispatchAction#selectionChanged(ITextSelection)
	 */
	public void selectionChanged(ITextSelection selection) {
		setEnabled(true);
	}
	
	/**
	 * Note: This method is for internal use only. Clients should not call this method.
	 */
	public void selectionChanged(JavaTextSelection selection) {
		try {
			setEnabled(RefactoringAvailabilityTester.isMoveInnerAvailable(selection));
		} catch (JavaScriptModelException e) {
			setEnabled(false);
		}
	}
	
	/*
	 * @see SelectionDispatchAction#run(ITextSelection)
	 */
	public void run(ITextSelection selection) {
		try {
			IType type= RefactoringAvailabilityTester.getDeclaringType(SelectionConverter.resolveEnclosingElement(fEditor, selection));
			if (type != null && RefactoringAvailabilityTester.isMoveInnerAvailable(type)) {
				if (! ActionUtil.isEditable(fEditor, getShell(), type))
					return;
				RefactoringExecutionStarter.startMoveInnerRefactoring(type, getShell());
			} else {
				MessageDialog.openInformation(getShell(), RefactoringMessages.OpenRefactoringWizardAction_unavailable, RefactoringMessages.ConvertNestedToTopAction_To_activate); 
			}
		} catch (JavaScriptModelException e) {
			ExceptionHandler.handle(e, 
				RefactoringMessages.OpenRefactoringWizardAction_refactoring,
				RefactoringMessages.OpenRefactoringWizardAction_exception); 
		}
	}
}
