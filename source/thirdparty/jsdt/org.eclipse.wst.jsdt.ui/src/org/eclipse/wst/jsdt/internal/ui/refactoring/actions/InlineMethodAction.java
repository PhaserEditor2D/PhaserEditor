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
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.ISourceRange;
import org.eclipse.wst.jsdt.core.ITypeRoot;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringAvailabilityTester;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringExecutionStarter;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.RefactoringASTParser;
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

/**
 * Inlines a method.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 */
public class InlineMethodAction extends SelectionDispatchAction {

	private JavaEditor fEditor;
	
	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * @param editor the java editor
	 */
	public InlineMethodAction(JavaEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(SelectionConverter.canOperateOn(fEditor));
	}

	public InlineMethodAction(IWorkbenchSite site) {
		super(site);
		setText(RefactoringMessages.InlineMethodAction_inline_Method); 
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.INLINE_ACTION);
	}

	//---- structured selection ----------------------------------------------
	
	/*
	 * @see SelectionDispatchAction#selectionChanged(IStructuredSelection)
	 */
	public void selectionChanged(IStructuredSelection selection) {
		try {
			setEnabled(RefactoringAvailabilityTester.isInlineMethodAvailable(selection));
		} catch (JavaScriptModelException e) {
			if (JavaModelUtil.isExceptionToBeLogged(e))
				JavaScriptPlugin.log(e);
		}
	}

	/*
	 * @see SelectionDispatchAction#run(IStructuredSelection)
	 */
	public void run(IStructuredSelection selection) {
		try {
			Assert.isTrue(RefactoringAvailabilityTester.isInlineMethodAvailable(selection));
			IFunction method= (IFunction) selection.getFirstElement();
			ISourceRange nameRange= method.getNameRange();
			run(nameRange.getOffset(), nameRange.getLength(), method.getTypeRoot());
		} catch (JavaScriptModelException e) {
			ExceptionHandler.handle(e, getShell(), RefactoringMessages.InlineMethodAction_dialog_title, RefactoringMessages.InlineMethodAction_unexpected_exception); 
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
	 * @param selection 
	 */
	public void selectionChanged(JavaTextSelection selection) {
		try {
			setEnabled(RefactoringAvailabilityTester.isInlineMethodAvailable(selection));
		} catch (JavaScriptModelException e) {
			setEnabled(false);
		}
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */		
	public void run(ITextSelection selection) {
		ITypeRoot typeRoot= SelectionConverter.getInputAsTypeRoot(fEditor);
		if (typeRoot == null)
			return;
		if (! JavaElementUtil.isSourceAvailable(typeRoot))
			return;
		run(selection.getOffset(), selection.getLength(), typeRoot);
	}

	private void run(int offset, int length, ITypeRoot typeRoot) {
		if (!ActionUtil.isEditable(fEditor, getShell(), typeRoot))
			return;
		try {
			JavaScriptUnit compilationUnit= RefactoringASTParser.parseWithASTProvider(typeRoot, true, null);
			if (! RefactoringExecutionStarter.startInlineMethodRefactoring(typeRoot, compilationUnit, offset, length, getShell())) {
				MessageDialog.openInformation(getShell(), RefactoringMessages.InlineMethodAction_dialog_title, RefactoringMessages.InlineMethodAction_no_method_invocation_or_declaration_selected);
			}
		} catch (JavaScriptModelException e) {
			ExceptionHandler.handle(e, getShell(), RefactoringMessages.InlineMethodAction_dialog_title, RefactoringMessages.InlineMethodAction_unexpected_exception); 
		}
	}

	public boolean tryInlineMethod(ITypeRoot typeRoot, JavaScriptUnit node, ITextSelection selection, Shell shell) {
		try {
			if (RefactoringExecutionStarter.startInlineMethodRefactoring(typeRoot, node, selection.getOffset(), selection.getLength(), shell)) {
				return true;
			}
		} catch (JavaScriptModelException exception) {
			JavaScriptPlugin.log(exception);
		}
		return false;
	}
}
