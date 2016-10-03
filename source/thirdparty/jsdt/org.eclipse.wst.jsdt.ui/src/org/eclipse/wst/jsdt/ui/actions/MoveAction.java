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
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringAvailabilityTester;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.actions.ActionUtil;
import org.eclipse.wst.jsdt.internal.ui.actions.SelectionConverter;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.wst.jsdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.wst.jsdt.internal.ui.refactoring.actions.MoveInstanceMethodAction;
import org.eclipse.wst.jsdt.internal.ui.refactoring.actions.MoveStaticMembersAction;
import org.eclipse.wst.jsdt.internal.ui.refactoring.reorg.ReorgMoveAction;
import org.eclipse.wst.jsdt.internal.ui.util.ExceptionHandler;

/**
 * This action moves JavaScript elements to a new location. The action prompts
 * the user for the new location.
 * <p>
 * The action is applicable to a homogeneous selection containing either
 * projects, package fragment roots, package fragments, compilation units,
 * or static methods.
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
public class MoveAction extends SelectionDispatchAction{
//TODO: remove duplicate availability checks. Look at
//- f...Action.selectionChanged
//- f...Action.isEnabled
//- ...Refactoring.isAvailable
//- try...
//... and remove duplicated code for text/structured selections.
//We have to clean this up, once we have a long term solution to
//bug 35748 (no JavaElements for local types). 
	
	private JavaEditor fEditor;
	private MoveInstanceMethodAction fMoveInstanceMethodAction;
	private MoveStaticMembersAction fMoveStaticMembersAction;
	private ReorgMoveAction fReorgMoveAction;
	
	/**
	 * Creates a new <code>MoveAction</code>. The action requires
	 * that the selection provided by the site's selection provider is of type <code>
	 * org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param site the site providing context information for this action
	 */
	public MoveAction(IWorkbenchSite site) {
		super(site);
		setText(RefactoringMessages.MoveAction_text); 
		fMoveStaticMembersAction= new MoveStaticMembersAction(site);
		fMoveInstanceMethodAction= new MoveInstanceMethodAction(site);
		fReorgMoveAction= new ReorgMoveAction(site);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.MOVE_ACTION);
	}
	
	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * @param editor the JavaScript editor
	 */
	public MoveAction(JavaEditor editor) {
		super(editor.getEditorSite());
		fEditor= editor;
		setText(RefactoringMessages.MoveAction_text); 
		fMoveStaticMembersAction= new MoveStaticMembersAction(editor);
		fMoveInstanceMethodAction= new MoveInstanceMethodAction(editor);
		fReorgMoveAction= new ReorgMoveAction(editor.getEditorSite());
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.MOVE_ACTION);
	}	

	/*
	 * @see ISelectionChangedListener#selectionChanged(SelectionChangedEvent)
	 */
	public void selectionChanged(SelectionChangedEvent event) {
		fMoveStaticMembersAction.selectionChanged(event);
		fMoveInstanceMethodAction.selectionChanged(event);
		fReorgMoveAction.selectionChanged(event);
		setEnabled(computeEnableState());	
	}

	/*
	 * @see org.eclipse.wst.jsdt.ui.actions.SelectionDispatchAction#run(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void run(IStructuredSelection selection) {
		try {
			if (fMoveInstanceMethodAction.isEnabled() && tryMoveInstanceMethod(selection)) 
				return;
	
			if (fMoveStaticMembersAction.isEnabled() && tryMoveStaticMembers(selection)) 
				return;
	
			if (fReorgMoveAction.isEnabled())
				fReorgMoveAction.run();
		
		} catch (JavaScriptModelException e) {
			ExceptionHandler.handle(e, RefactoringMessages.OpenRefactoringWizardAction_refactoring, RefactoringMessages.OpenRefactoringWizardAction_exception); 
		}

	}

	/*
	 * @see org.eclipse.wst.jsdt.ui.actions.SelectionDispatchAction#run(org.eclipse.jface.text.ITextSelection)
	 */
	public void run(ITextSelection selection) {
		try {
			if (!ActionUtil.isEditable(fEditor))
				return;
			if (fMoveStaticMembersAction.isEnabled() && tryMoveStaticMembers(selection))
				return;
		
			if (fMoveInstanceMethodAction.isEnabled() && tryMoveInstanceMethod(selection))
				return;
	
			if (tryReorgMove(selection))
				return;
			
			MessageDialog.openInformation(getShell(), RefactoringMessages.MoveAction_Move, RefactoringMessages.MoveAction_select); 
		} catch (JavaScriptModelException e) {
			ExceptionHandler.handle(e, RefactoringMessages.OpenRefactoringWizardAction_refactoring, RefactoringMessages.OpenRefactoringWizardAction_exception); 
		}
	}

	private boolean tryMoveStaticMembers(ITextSelection selection) throws JavaScriptModelException {
		IJavaScriptElement element= SelectionConverter.getElementAtOffset(fEditor);
		if (element == null || !(element instanceof IMember))
			return false;
		IMember[] array= new IMember[] { (IMember) element};
		if (!RefactoringAvailabilityTester.isMoveStaticMembersAvailable(array))
			return false;
		fMoveStaticMembersAction.run(selection);
		return true;
	}

	private static IMember[] getSelectedMembers(IStructuredSelection selection){
		if (selection.isEmpty())
			return null;
		
		for  (Iterator iter= selection.iterator(); iter.hasNext(); ) {
			if (! (iter.next() instanceof IMember))
				return null;
		}
		return convertToMemberArray(selection.toArray());
	}

	private static IMember[] convertToMemberArray(Object[] obj) {
		if (obj == null)
			return null;
		Set memberSet= new HashSet();
		memberSet.addAll(Arrays.asList(obj));
		return (IMember[]) memberSet.toArray(new IMember[memberSet.size()]);
	}

	private boolean tryMoveStaticMembers(IStructuredSelection selection) throws JavaScriptModelException {
		IMember[] array= getSelectedMembers(selection);
		if (!RefactoringAvailabilityTester.isMoveStaticMembersAvailable(array))
			return false;
		fMoveStaticMembersAction.run(selection);
		return true;
	}

	private boolean tryMoveInstanceMethod(ITextSelection selection) throws JavaScriptModelException {
		IJavaScriptElement element= SelectionConverter.getElementAtOffset(fEditor);
		if (element == null || !(element instanceof IFunction))
			return false;

		IFunction method= (IFunction) element;
		if (!RefactoringAvailabilityTester.isMoveMethodAvailable(method))
			return false;
		fMoveInstanceMethodAction.run(selection);
		return true;
	}

	private boolean tryMoveInstanceMethod(IStructuredSelection selection) throws JavaScriptModelException {
		IFunction method= getSingleSelectedMethod(selection);
		if (method == null)
			return false;
		if (!RefactoringAvailabilityTester.isMoveMethodAvailable(method))
			return false;
		fMoveInstanceMethodAction.run(selection);
		return true;
	}

	private static IFunction getSingleSelectedMethod(IStructuredSelection selection) {
		if (selection.isEmpty() || selection.size() != 1) 
			return null;
		
		Object first= selection.getFirstElement();
		if (! (first instanceof IFunction))
			return null;
		return (IFunction) first;
	}
	

	private boolean tryReorgMove(ITextSelection selection) throws JavaScriptModelException{
		IJavaScriptElement element= SelectionConverter.getElementAtOffset(fEditor);
		if (element == null)
			return false;
		StructuredSelection mockStructuredSelection= new StructuredSelection(element);
		fReorgMoveAction.selectionChanged(mockStructuredSelection);
		if (!fReorgMoveAction.isEnabled())
			return false;
			
		fReorgMoveAction.run(mockStructuredSelection);
		return true;			
	}


	/*
	 * @see SelectionDispatchAction#update(ISelection)
	 */
	public void update(ISelection selection) {
		fMoveStaticMembersAction.update(selection);
		fMoveInstanceMethodAction.update(selection);
		fReorgMoveAction.update(selection);
		setEnabled(computeEnableState());
	}
	
	private boolean computeEnableState(){
		return fMoveStaticMembersAction.isEnabled()
				|| fMoveInstanceMethodAction.isEnabled()
				|| fReorgMoveAction.isEnabled();
	}
}
