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

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;
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

/**
 * Action to start the modify parameters refactoring. The refactoring supports 
 * swapping and renaming of arguments.
 * <p>
 * This action is applicable to selections containing a method with one or
 * more arguments.
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
public class ModifyParametersAction extends SelectionDispatchAction {
	
	private JavaEditor fEditor;
	
	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * @param editor the JavaScript editor
	 */
	public ModifyParametersAction(JavaEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(SelectionConverter.canOperateOn(fEditor));
	}

	/**
	 * Creates a new <code>ModifyParametersAction</code>. The action requires
	 * that the selection provided by the site's selection provider is of type <code>
	 * org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param site the site providing context information for this action
	 */
	public ModifyParametersAction(IWorkbenchSite site) {
		super(site);
		setText(RefactoringMessages.RefactoringGroup_modify_Parameters_label);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.MODIFY_PARAMETERS_ACTION);
	}
	
	/*
	 * @see SelectionDispatchAction#selectionChanged(IStructuredSelection)
	 */
	public void selectionChanged(IStructuredSelection selection) {
		try {
			setEnabled(RefactoringAvailabilityTester.isChangeSignatureAvailable(selection));
		} catch (JavaScriptModelException e) {
			// http://bugs.eclipse.org/bugs/show_bug.cgi?id=19253
			if (JavaModelUtil.isExceptionToBeLogged(e))
				JavaScriptPlugin.log(e);
			setEnabled(false);//no UI here - happens on selection changes
		}
	}

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
			setEnabled(RefactoringAvailabilityTester.isChangeSignatureAvailable(selection));
		} catch (JavaScriptModelException e) {
			setEnabled(false);
		}
	}

	/*
	 * @see SelectionDispatchAction#run(IStructuredSelection)
	 */
	public void run(IStructuredSelection selection) {
		try {
			// we have to call this here - no selection changed event is sent after a refactoring but it may still invalidate enablement
			if (RefactoringAvailabilityTester.isChangeSignatureAvailable(selection)) {
				IFunction method= getSingleSelectedMethod(selection);
				if (! ActionUtil.isEditable(getShell(), method))
					return;
				RefactoringExecutionStarter.startChangeSignatureRefactoring(method, this, getShell());
			}
		} catch (JavaScriptModelException e) {
			ExceptionHandler.handle(e, RefactoringMessages.OpenRefactoringWizardAction_refactoring, RefactoringMessages.OpenRefactoringWizardAction_exception); 
		}
	}

    /*
     * @see SelectionDispatchAction#run(ITextSelection)
     */
	public void run(ITextSelection selection) {
		try {
			if (! ActionUtil.isEditable(fEditor))
				return;
			IFunction method= getSingleSelectedMethod(selection);
			if (RefactoringAvailabilityTester.isChangeSignatureAvailable(method)){
				RefactoringExecutionStarter.startChangeSignatureRefactoring(method, this, getShell());
			} else {
				MessageDialog.openInformation(getShell(), RefactoringMessages.OpenRefactoringWizardAction_unavailable, RefactoringMessages.ModifyParametersAction_unavailable); 
			}
		} catch (JavaScriptModelException e) {
			ExceptionHandler.handle(e, RefactoringMessages.OpenRefactoringWizardAction_refactoring, RefactoringMessages.OpenRefactoringWizardAction_exception); 
		}
	}

	private static IFunction getSingleSelectedMethod(IStructuredSelection selection){
		if (selection.isEmpty() || selection.size() != 1) 
			return null;
		if (selection.getFirstElement() instanceof IFunction)
			return (IFunction)selection.getFirstElement();
		return null;
	}

	private IFunction getSingleSelectedMethod(ITextSelection selection) throws JavaScriptModelException{
		//- when caret/selection on method name (call or declaration) -> that method
		//- otherwise: caret position's enclosing method declaration
		//  - when caret inside argument list of method declaration -> enclosing method declaration
		//  - when caret inside argument list of method call -> enclosing method declaration (and NOT method call)
		IJavaScriptElement[] elements= SelectionConverter.codeResolve(fEditor); 
		if (elements.length > 1)
			return null;
		if (elements.length == 1 && elements[0] instanceof IFunction)
			return (IFunction)elements[0];
		IJavaScriptElement elementAt= SelectionConverter.getInputAsCompilationUnit(fEditor).getElementAt(selection.getOffset());
		if (elementAt instanceof IFunction)
			return (IFunction)elementAt;
		return null;
	}
}
