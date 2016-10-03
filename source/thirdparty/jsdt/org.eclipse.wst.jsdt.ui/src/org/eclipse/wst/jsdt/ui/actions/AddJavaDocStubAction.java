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
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IField;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.AddJavaDocStubOperation;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.actions.ActionMessages;
import org.eclipse.wst.jsdt.internal.ui.actions.ActionUtil;
import org.eclipse.wst.jsdt.internal.ui.actions.SelectionConverter;
import org.eclipse.wst.jsdt.internal.ui.actions.WorkbenchRunnableAdapter;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.wst.jsdt.internal.ui.util.ElementValidator;
import org.eclipse.wst.jsdt.internal.ui.util.ExceptionHandler;
import org.eclipse.wst.jsdt.ui.JavaScriptUI;

/**
 * Create Javadoc comment stubs for the selected members.
 * <p>
 * Will open the parent compilation unit in a JavaScript editor. The result is 
 * unsaved, so the user can decide if the changes are acceptable.
 * <p>
 * The action is applicable to structured selections containing elements
 * of type <code>IMember</code>.
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
public class AddJavaDocStubAction extends SelectionDispatchAction {

	private CompilationUnitEditor fEditor;

	/**
	 * Creates a new <code>AddJavaDocStubAction</code>. The action requires
	 * that the selection provided by the site's selection provider is of type <code>
	 * org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param site the site providing context information for this action
	 */
	public AddJavaDocStubAction(IWorkbenchSite site) {
		super(site);
		setText(ActionMessages.AddJavaDocStubAction_label); 
		setDescription(ActionMessages.AddJavaDocStubAction_description); 
		setToolTipText(ActionMessages.AddJavaDocStubAction_tooltip); 
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.ADD_JAVADOC_STUB_ACTION);
	}

	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * @param editor the compilation unit editor
	 */
	public AddJavaDocStubAction(CompilationUnitEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(checkEnabledEditor());
	}

	//---- Structured Viewer -----------------------------------------------------------
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */
	public void selectionChanged(IStructuredSelection selection) {
		IMember[] members= getSelectedMembers(selection);
		setEnabled(members != null && members.length > 0);
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */		
	public void run(IStructuredSelection selection) {
		IMember[] members= getSelectedMembers(selection);
		if (members == null || members.length == 0) {
			return;
		}
		
		try {
			IJavaScriptUnit cu= members[0].getJavaScriptUnit();
			if (!ActionUtil.isEditable(getShell(), cu)) {
				return;
			}
			
			// open the editor, forces the creation of a working copy
			IEditorPart editor= JavaScriptUI.openInEditor(cu);
			
			if (ElementValidator.check(members, getShell(), getDialogTitle(), false))
				run(cu, members);
			JavaModelUtil.reconcile(cu);
			EditorUtility.revealInEditor(editor, members[0]);
			
		} catch (CoreException e) {
			ExceptionHandler.handle(e, getShell(), getDialogTitle(), ActionMessages.AddJavaDocStubsAction_error_actionFailed); 
		}
	}
	
	//---- JavaScript Editor --------------------------------------------------------------
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */		
	public void selectionChanged(ITextSelection selection) {
	}

	private boolean checkEnabledEditor() {
		return fEditor != null && SelectionConverter.canOperateOn(fEditor);
	}	
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */		
	public void run(ITextSelection selection) {
		try {
			IJavaScriptElement element= SelectionConverter.getElementAtOffset(fEditor);
			if (!ActionUtil.isEditable(fEditor, getShell(), element))
				return;
			int type= element != null ? element.getElementType() : -1;
			if (type != IJavaScriptElement.METHOD && type != IJavaScriptElement.TYPE && type != IJavaScriptElement.FIELD) {
		 		element= SelectionConverter.getTypeAtOffset(fEditor);
		 		if (element == null) {
					MessageDialog.openInformation(getShell(), getDialogTitle(), 
						ActionMessages.AddJavaDocStubsAction_not_applicable); 
					return;
		 		}
			}
			IMember[] members= new IMember[] { (IMember)element };
			if (ElementValidator.checkValidateEdit(members, getShell(), getDialogTitle()))
				run(members[0].getJavaScriptUnit(), members);
		} catch (CoreException e) {
			ExceptionHandler.handle(e, getShell(), getDialogTitle(), ActionMessages.AddJavaDocStubsAction_error_actionFailed); 
		}
	}

	//---- Helpers -------------------------------------------------------------------
	
	/**
	 * Note this method is for internal use only. 
	 * 
	 * @param cu the compilation unit
	 * @param members an array of members
	 */
	public void run(IJavaScriptUnit cu, IMember[] members) {
		try {
			AddJavaDocStubOperation op= new AddJavaDocStubOperation(members);
			PlatformUI.getWorkbench().getProgressService().runInUI(
				PlatformUI.getWorkbench().getProgressService(),
				new WorkbenchRunnableAdapter(op, op.getScheduleRule()),
				op.getScheduleRule());
		} catch (InvocationTargetException e) {
			ExceptionHandler.handle(e, getShell(), getDialogTitle(), ActionMessages.AddJavaDocStubsAction_error_actionFailed); 
		} catch (InterruptedException e) {
			// operation canceled
		}
	}
	
	private IMember[] getSelectedMembers(IStructuredSelection selection) {
		List elements= selection.toList();
		int nElements= elements.size();
		if (nElements > 0) {
			IMember[] res= new IMember[nElements];
			IJavaScriptUnit cu= null;
			for (int i= 0; i < nElements; i++) {
				Object curr= elements.get(i);
				if (curr instanceof IFunction || curr instanceof IType || curr instanceof IField) {
					IMember member= (IMember)curr; // limit to methods, types & fields
					if (! member.exists()) {
						return null;
					}
					if (i == 0) {
						cu= member.getJavaScriptUnit();
						if (cu == null) {
							return null;
						}
					} else if (!cu.equals(member.getJavaScriptUnit())) {
						return null;
					}
					if (member instanceof IType && member.getElementName().length() == 0) {
						return null; // anonymous type
					}
					res[i]= member;
				} else {
					return null;
				}
			}
			return res;
		}
		return null;
	}
	
	private String getDialogTitle() {
		return ActionMessages.AddJavaDocStubsAction_error_dialogTitle; 
	}	
}
