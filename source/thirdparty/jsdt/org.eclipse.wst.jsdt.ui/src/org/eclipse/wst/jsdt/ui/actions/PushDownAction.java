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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IMember;
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
 * Action to push down methods and fields into subclasses.
 * <p>
 * Action is applicable to selections containing elements of type
 * <code>IField</code> and <code>IFunction</code>.
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
public class PushDownAction extends SelectionDispatchAction {

	private static IMember[] getSelectedMembers(IStructuredSelection selection) {
		if (selection.isEmpty())
			return null;
		if (selection.size() == 1) {
			try {
				final IType type= RefactoringAvailabilityTester.getSingleSelectedType(selection);
				if (type != null)
					return new IType[] { type};
			} catch (JavaScriptModelException exception) {
				JavaScriptPlugin.log(exception);
			}
		}
		for (Iterator iter= selection.iterator(); iter.hasNext();) {
			if (!(iter.next() instanceof IMember))
				return null;
		}
		Set memberSet= new HashSet();
		memberSet.addAll(Arrays.asList(selection.toArray()));
		return (IMember[]) memberSet.toArray(new IMember[memberSet.size()]);
	}

	private JavaEditor fEditor;

	/**
	 * Note: This constructor is for internal use only. Clients should not call
	 * this constructor.
	 * 
	 * @param editor the JavaScript editor
	 */
	public PushDownAction(JavaEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(SelectionConverter.canOperateOn(fEditor));
	}

	/**
	 * Creates a new <code>PushDownAction</code>. The action requires that
	 * the selection provided by the site's selection provider is of type <code>
	 * org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param site
	 *            the site providing context information for this action
	 */
	public PushDownAction(IWorkbenchSite site) {
		super(site);
		setText(RefactoringMessages.PushDownAction_Push_Down);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.PUSH_DOWN_ACTION);
	}

	private IMember getSelectedMemberFromEditor() throws JavaScriptModelException {
		IJavaScriptElement element= SelectionConverter.resolveEnclosingElement(fEditor, (ITextSelection) fEditor.getSelectionProvider().getSelection());
		if (element == null || !(element instanceof IMember))
			return null;
		return (IMember) element;
	}

	/**
	 * {@inheritDoc}
	 */
	public void run(IStructuredSelection selection) {
		try {
			IMember[] members= getSelectedMembers(selection);
			if (RefactoringAvailabilityTester.isPushDownAvailable(members) && ActionUtil.isEditable(getShell(), members[0]))
				RefactoringExecutionStarter.startPushDownRefactoring(members, getShell());
		} catch (JavaScriptModelException e) {
			ExceptionHandler.handle(e, RefactoringMessages.OpenRefactoringWizardAction_refactoring, RefactoringMessages.OpenRefactoringWizardAction_exception);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void run(ITextSelection selection) {
		try {
			if (!ActionUtil.isEditable(fEditor))
				return;
			IMember member= getSelectedMemberFromEditor();
			IMember[] array= new IMember[] { member};
			if (member != null && RefactoringAvailabilityTester.isPushDownAvailable(array)) {
				RefactoringExecutionStarter.startPushDownRefactoring(array, getShell());
			} else {
				MessageDialog.openInformation(getShell(), RefactoringMessages.OpenRefactoringWizardAction_unavailable, RefactoringMessages.PushDownAction_To_activate);
			}
		} catch (JavaScriptModelException e) {
			ExceptionHandler.handle(e, RefactoringMessages.OpenRefactoringWizardAction_refactoring, RefactoringMessages.OpenRefactoringWizardAction_exception);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void selectionChanged(IStructuredSelection selection) {
		try {
			setEnabled(RefactoringAvailabilityTester.isPushDownAvailable(selection));
		} catch (JavaScriptModelException e) {
			// http://bugs.eclipse.org/bugs/show_bug.cgi?id=19253
			if (JavaModelUtil.isExceptionToBeLogged(e))
				JavaScriptPlugin.log(e);
			setEnabled(false);// no UI
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void selectionChanged(ITextSelection selection) {
		setEnabled(true);
	}

	/**
	 * Note: This method is for internal use only. Clients should not call this
	 * method.
	 * 
	 * @param selection
	 */
	public void selectionChanged(JavaTextSelection selection) {
		try {
			setEnabled(RefactoringAvailabilityTester.isPushDownAvailable(selection));
		} catch (JavaScriptModelException e) {
			setEnabled(false);
		}
	}
}
