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
package org.eclipse.wst.jsdt.ui.actions;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.core.IField;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.ISourceRange;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.Signature;
import org.eclipse.wst.jsdt.core.dom.PrimitiveType;
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
 * Action to generalize the type of a local or field declaration or the
 * return type of a method declaration.
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
public class ChangeTypeAction extends SelectionDispatchAction {
	
	private JavaEditor fEditor;

	/**
	 * Note: This constructor is for internal use only. Clients should not call
	 * this constructor.
	 * @param editor the JavaScript editor
	 */
	public ChangeTypeAction(JavaEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(SelectionConverter.getInputAsCompilationUnit(fEditor) != null);
	}

	/**
	 * Creates a new <code>ChangeTypeAction</code>. The action requires that
	 * the selection provided by the site's selection provider is of type
	 * <code>org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param site the site providing context information for this action
	 */
	public ChangeTypeAction(IWorkbenchSite site) {
		super(site);
		setText(RefactoringMessages.ChangeTypeAction_label); 
		setToolTipText(RefactoringMessages.ChangeTypeAction_tooltipText); 
		setDescription(RefactoringMessages.ChangeTypeAction_description); 
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.CHANGE_TYPE_ACTION);
	}
	
	//---- structured selection ---------------------------------------------

	public void selectionChanged(IStructuredSelection selection) {
		try {
			setEnabled(RefactoringAvailabilityTester.isGeneralizeTypeAvailable(selection));
		} catch (JavaScriptModelException e) {
			if (JavaModelUtil.isExceptionToBeLogged(e))
				JavaScriptPlugin.log(e);
			setEnabled(false);
		}
	}

	public void run(IStructuredSelection selection) {
		try {
			IMember member= getMember(selection);
			if (member == null || !ActionUtil.isEditable(getShell(), member))
				return;
			ISourceRange range= member.getNameRange();
			RefactoringExecutionStarter.startChangeTypeRefactoring(member.getJavaScriptUnit(), getShell(), range.getOffset(), range.getLength());
		} catch (CoreException e) {
			ExceptionHandler.handle(e, RefactoringMessages.ChangeTypeAction_dialog_title, RefactoringMessages.ChangeTypeAction_exception); 
		}
	}

	private static IMember getMember(IStructuredSelection selection) throws JavaScriptModelException {
		if (selection.size() != 1)
			return null;
		
		Object element= selection.getFirstElement();
		if (!(element instanceof IMember))
			return null;
		
		if (element instanceof IFunction) {
			IFunction method= (IFunction)element;
			String returnType= method.getReturnType();
			if (PrimitiveType.toCode(Signature.toString(returnType)) != null)
				return null;
			return method;
		} else if (element instanceof IField) {
			return (IField)element;
		}
		return null;
	}

	//---- text selection ------------------------------------------------------------
	
	/*
	 * (non-Javadoc) Method declared on SelectionDispatchAction
	 */
	public void selectionChanged(ITextSelection selection) {
		setEnabled(true);
	}

	/**
	 * Note: This method is for internal use only. Clients should not call this method.
	 * @param selection the JavaScript text selection
	 */
	public void selectionChanged(JavaTextSelection selection) {
		try {
			setEnabled(RefactoringAvailabilityTester.isGeneralizeTypeAvailable(selection));
		} catch (JavaScriptModelException e) {
			setEnabled(false);
		}
	}

	/*
	 * (non-Javadoc) Method declared on SelectionDispatchAction
	 */
	public void run(ITextSelection selection) {
		if (!ActionUtil.isEditable(fEditor))
			return;
		try {
			RefactoringExecutionStarter.startChangeTypeRefactoring(SelectionConverter.getInputAsCompilationUnit(fEditor), getShell(), selection.getOffset(), selection.getLength());
		} catch (CoreException e) {
			ExceptionHandler.handle(e, RefactoringMessages.ChangeTypeAction_dialog_title, RefactoringMessages.ChangeTypeAction_exception); 
		}
	}
}
