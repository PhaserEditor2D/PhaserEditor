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

public class MoveStaticMembersAction extends SelectionDispatchAction{
	
	private JavaEditor fEditor;

	public MoveStaticMembersAction(IWorkbenchSite site) {
		super(site);
		setText(RefactoringMessages.RefactoringGroup_move_label);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.MOVE_ACTION);		
	}

	public MoveStaticMembersAction(JavaEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(SelectionConverter.canOperateOn(fEditor));
	}
		
	public void selectionChanged(IStructuredSelection selection) {
		try {
			setEnabled(RefactoringAvailabilityTester.isMoveStaticMembersAvailable(getSelectedMembers(selection)));
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
			setEnabled(RefactoringAvailabilityTester.isMoveStaticAvailable(selection));
		} catch (JavaScriptModelException e) {
			setEnabled(false);
		}
	}

	public void run(IStructuredSelection selection) {
		try {
			IMember[] members= getSelectedMembers(selection);
			for (int index= 0; index < members.length; index++) {
				if (!ActionUtil.isEditable(getShell(), members[index]))
					return;
			}
			if (RefactoringAvailabilityTester.isMoveStaticMembersAvailable(members))
				RefactoringExecutionStarter.startMoveStaticMembersRefactoring(members, getShell());
		} catch (JavaScriptModelException e) {
			ExceptionHandler.handle(e, RefactoringMessages.OpenRefactoringWizardAction_refactoring, RefactoringMessages.OpenRefactoringWizardAction_exception); 
		}
	}

	public void run(ITextSelection selection) {
		try {
			IMember member= getSelectedMemberFromEditor();
			if (!ActionUtil.isEditable(fEditor, getShell(), member))
				return;
			IMember[] array= new IMember[]{member};
			if (member != null && RefactoringAvailabilityTester.isMoveStaticMembersAvailable(array)){
				RefactoringExecutionStarter.startMoveStaticMembersRefactoring(array, getShell());	
			} else {
				MessageDialog.openInformation(getShell(), RefactoringMessages.OpenRefactoringWizardAction_unavailable, RefactoringMessages.MoveMembersAction_unavailable); 
			}
		} catch (JavaScriptModelException e) {
			ExceptionHandler.handle(e, RefactoringMessages.OpenRefactoringWizardAction_refactoring, RefactoringMessages.OpenRefactoringWizardAction_exception); 
		}
	}
	
	private static IMember[] getSelectedMembers(IStructuredSelection selection){
		if (selection.isEmpty())
			return null;
		
		for  (final Iterator iterator= selection.iterator(); iterator.hasNext(); ) {
			if (! (iterator.next() instanceof IMember))
				return null;
		}
		Set memberSet= new HashSet();
		memberSet.addAll(Arrays.asList(selection.toArray()));
		return (IMember[]) memberSet.toArray(new IMember[memberSet.size()]);
	}

	private IMember getSelectedMemberFromEditor() throws JavaScriptModelException{
		IJavaScriptElement element= SelectionConverter.getElementAtOffset(fEditor);
		if (element == null || ! (element instanceof IMember))
			return null;
		return (IMember)element;
	}
}
