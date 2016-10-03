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


import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.ISourceRange;
import org.eclipse.wst.jsdt.core.IType;
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
 * Action to convert an anonymous inner class to a nested class.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 */
public class ConvertAnonymousToNestedAction extends SelectionDispatchAction {

	private final JavaEditor fEditor;
	
	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * @param editor the JavaScript editor
	 */
	public ConvertAnonymousToNestedAction(JavaEditor editor) {
		super(editor.getEditorSite());
		setText(RefactoringMessages.ConvertAnonymousToNestedAction_Convert_Anonymous); 
		fEditor= editor;
		setEnabled(SelectionConverter.getInputAsCompilationUnit(fEditor) != null);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.CONVERT_ANONYMOUS_TO_NESTED_ACTION);
	}

	/**
	 * Creates a new <code>ConvertAnonymousToNestedAction</code>. The action requires 
	 * that the selection provided by the site's selection provider is of type
	 * <code>org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param site the site providing context information for this action
	 */
	public ConvertAnonymousToNestedAction(IWorkbenchSite site) {
		super(site);
		fEditor= null;
		setText(RefactoringMessages.ConvertAnonymousToNestedAction_Convert_Anonymous); 
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.CONVERT_ANONYMOUS_TO_NESTED_ACTION);
	}
	
	//---- Structured selection -----------------------------------------------------
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */
	public void selectionChanged(IStructuredSelection selection) {
		try {
			setEnabled(RefactoringAvailabilityTester.isConvertAnonymousAvailable(selection));
		} catch (JavaScriptModelException e) {
			if (JavaModelUtil.isExceptionToBeLogged(e))
				JavaScriptPlugin.log(e);
			setEnabled(false);
		}
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */
	public void run(IStructuredSelection selection) {
		IType type= getElement(selection);
		if (type == null)
			return;
		ISourceRange range;
		try {
			range= type.getNameRange();
			run(type.getJavaScriptUnit(), range.getOffset(), range.getLength());
		} catch (JavaScriptModelException e) {
			ExceptionHandler.handle(e, RefactoringMessages.ConvertAnonymousToNestedAction_dialog_title, RefactoringMessages.NewTextRefactoringAction_exception); 
		}
	}

	private IType getElement(IStructuredSelection selection) {
		if (selection.size() != 1)
			return null;
		Object element= selection.getFirstElement();
		if (!(element instanceof IType))
			return null;
		IType type= (IType)element;
		try {
			if (type.isAnonymous())
				return type;
		} catch (JavaScriptModelException e) {
			// fall through
		}
		return null;
	}
	
	//---- Text selection -----------------------------------------------------------
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */		
	public void run(ITextSelection selection) {
		try{
			run(SelectionConverter.getInputAsCompilationUnit(fEditor), selection.getOffset(), selection.getLength());
		} catch (JavaScriptModelException e){
			ExceptionHandler.handle(e, RefactoringMessages.ConvertAnonymousToNestedAction_dialog_title, RefactoringMessages.NewTextRefactoringAction_exception); 
		}	
	}

	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */		
	public void selectionChanged(ITextSelection selection) {
		setEnabled(fEditor != null && SelectionConverter.getInputAsCompilationUnit(fEditor) != null);
	}

	/**
	 * Note: This method is for internal use only. Clients should not call this method.
	 */
	public void selectionChanged(JavaTextSelection selection) {
		try {
			setEnabled(RefactoringAvailabilityTester.isConvertAnonymousAvailable(selection));
		} catch (JavaScriptModelException e) {
			setEnabled(false);
		}
	}

	//---- helpers -------------------------------------------------------------------
	
	private void run(IJavaScriptUnit unit, int offset, int length) throws JavaScriptModelException {
		if (!ActionUtil.isEditable(fEditor, getShell(), unit))
			return;
		RefactoringExecutionStarter.startConvertAnonymousRefactoring(unit, offset, length, getShell());
	}
}
