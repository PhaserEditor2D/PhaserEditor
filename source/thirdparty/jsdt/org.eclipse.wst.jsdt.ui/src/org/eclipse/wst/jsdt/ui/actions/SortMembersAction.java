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

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IParent;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.SortMembersOperation;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.actions.ActionMessages;
import org.eclipse.wst.jsdt.internal.ui.actions.ActionUtil;
import org.eclipse.wst.jsdt.internal.ui.actions.SelectionConverter;
import org.eclipse.wst.jsdt.internal.ui.actions.WorkbenchRunnableAdapter;
import org.eclipse.wst.jsdt.internal.ui.dialogs.OptionalMessageDialog;
import org.eclipse.wst.jsdt.internal.ui.dialogs.SortMembersMessageDialog;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.IJavaAnnotation;
import org.eclipse.wst.jsdt.internal.ui.util.BusyIndicatorRunnableContext;
import org.eclipse.wst.jsdt.internal.ui.util.ElementValidator;
import org.eclipse.wst.jsdt.internal.ui.util.ExceptionHandler;
import org.eclipse.wst.jsdt.ui.JavaScriptUI;

/**
 * Sorts the members of a compilation unit with the sort order as specified in
 * the Sort Order preference page.
 * <p>
 * The action will open the parent compilation unit in a JavaScript editor. The result
 * is unsaved, so the user can decide if the changes are acceptable.
 * <p>
 * The action is applicable to structured selections containing a single
 * <code>IJavaScriptUnit</code> or top level <code>IType</code> in a
 * compilation unit.
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
public class SortMembersAction extends SelectionDispatchAction {

	private CompilationUnitEditor fEditor;
	private final static String ID_OPTIONAL_DIALOG= "org.eclipse.wst.jsdt.ui.actions.SortMembersAction"; //$NON-NLS-1$

	/**
	 * Creates a new <code>SortMembersAction</code>. The action requires
	 * that the selection provided by the site's selection provider is of type <code>
	 * org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param site the site providing context information for this action
	 */
	public SortMembersAction(IWorkbenchSite site) {
		super(site);
		setText(ActionMessages.SortMembersAction_label); 
		setDescription(ActionMessages.SortMembersAction_description); 
		setToolTipText(ActionMessages.SortMembersAction_tooltip); 
		
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.SORT_MEMBERS_ACTION);
	}

	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * @param editor the compilation unit editor
	 */
	public SortMembersAction(CompilationUnitEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(checkEnabledEditor());
	}
	
	private boolean checkEnabledEditor() {
		return fEditor != null && SelectionConverter.canOperateOn(fEditor);
	}	
	
	//---- Structured Viewer -----------------------------------------------------------
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */
	public void selectionChanged(IStructuredSelection selection) {
		boolean enabled= false;
		enabled= getSelectedCompilationUnit(selection) != null;
		setEnabled(enabled);
	}	
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */
	public void run(IStructuredSelection selection) {
		Shell shell= getShell();
		try {
			IJavaScriptUnit cu= getSelectedCompilationUnit(selection);
			if (cu == null) {
				return;
			}
			IJavaScriptElement[] types= cu.getChildren();
			if (!hasMembersToSort(types)) {
				return;
			}
			if (!ActionUtil.isEditable(getShell(), cu)) {
				return;
			}
			
			SortMembersMessageDialog dialog= new SortMembersMessageDialog(getShell());
			if (dialog.open() != Window.OK) {
				return;
			}
			
			if (!ElementValidator.check(cu, getShell(), getDialogTitle(), false)) {
				return;
			}
			
			// open an editor and work on a working copy
			IEditorPart editor= JavaScriptUI.openInEditor(cu);
			if (editor != null) {
				run(shell, cu, editor, dialog.isNotSortingFieldsEnabled());
			}
		} catch (CoreException e) {
			ExceptionHandler.handle(e, shell, getDialogTitle(), null); 
		}			
	}
	
	private boolean hasMembersToSort(IJavaScriptElement[] members) throws JavaScriptModelException {
		if (members.length > 1) {
			return true;
		}
		if (members.length == 1) {
			IJavaScriptElement elem= members[0];
			if (elem instanceof IParent) {
				return hasMembersToSort(((IParent) elem).getChildren());
			}
		}
		return false;
	}

	//---- JavaScript Editor --------------------------------------------------------------
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */
	public void selectionChanged(ITextSelection selection) {
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */
	public void run(ITextSelection selection) {
		Shell shell= getShell();
		IJavaScriptElement input= SelectionConverter.getInput(fEditor);
		if (input instanceof IJavaScriptUnit) {
			if (!ActionUtil.isEditable(fEditor)) {
				return;
			}
			SortMembersMessageDialog dialog= new SortMembersMessageDialog(getShell());
			if (dialog.open() != Window.OK) {
				return;
			}
			if (!ElementValidator.check(input, getShell(), getDialogTitle(), true)) {
				return;
			}
			run(shell, (IJavaScriptUnit) input, fEditor, dialog.isNotSortingFieldsEnabled());
		} else {
			MessageDialog.openInformation(shell, getDialogTitle(), ActionMessages.SortMembersAction_not_applicable); 
		}
	}

	//---- Helpers -------------------------------------------------------------------
	
	private boolean containsRelevantMarkers(IEditorPart editor) {
		IAnnotationModel model= JavaScriptUI.getDocumentProvider().getAnnotationModel(editor.getEditorInput());
		Iterator iterator= model.getAnnotationIterator();
		while (iterator.hasNext()) {
			Object element= iterator.next();
			if (element instanceof IJavaAnnotation) {
				IJavaAnnotation annot= (IJavaAnnotation) element;
				if (!annot.isMarkedDeleted() && annot.isPersistent() && !annot.isProblem())
					return true;
			}
		}		
		return false;
	}
	
	private void run(Shell shell, IJavaScriptUnit cu, IEditorPart editor, boolean isNotSortFields) {
		if (containsRelevantMarkers(editor)) {
			int returnCode= OptionalMessageDialog.open(ID_OPTIONAL_DIALOG, 
					getShell(), 
					getDialogTitle(),
					null,
					ActionMessages.SortMembersAction_containsmarkers,  
					MessageDialog.WARNING, 		
					new String[] {IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL}, 
					0);
			if (returnCode != OptionalMessageDialog.NOT_SHOWN && 
					returnCode != Window.OK ) return;	
		}

		SortMembersOperation op= new SortMembersOperation(cu, null, isNotSortFields);
		try {
			BusyIndicatorRunnableContext context= new BusyIndicatorRunnableContext();
			PlatformUI.getWorkbench().getProgressService().runInUI(context,
				new WorkbenchRunnableAdapter(op, op.getScheduleRule()),
				op.getScheduleRule());
		} catch (InvocationTargetException e) {
			ExceptionHandler.handle(e, shell, getDialogTitle(), null); 
		} catch (InterruptedException e) {
			// Do nothing. Operation has been canceled by user.
		}
	}

		
	private IJavaScriptUnit getSelectedCompilationUnit(IStructuredSelection selection) {
		if (selection.size() == 1) {
			Object element= selection.getFirstElement();
			if (element instanceof IJavaScriptUnit) {
				return (IJavaScriptUnit) element;
			} else if (element instanceof IType) {
				IType type= (IType) element;
				if (type.getParent() instanceof IJavaScriptUnit) { // only top level types
					return type.getJavaScriptUnit();
				}
			}
		}
		return null;
	}
	
	private String getDialogTitle() {
		return ActionMessages.SortMembersAction_dialog_title; 
	}	
}
