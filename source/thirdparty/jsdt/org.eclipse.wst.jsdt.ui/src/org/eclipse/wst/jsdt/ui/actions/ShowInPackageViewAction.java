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

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IOpenable;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.actions.ActionMessages;
import org.eclipse.wst.jsdt.internal.ui.actions.SelectionConverter;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.wst.jsdt.internal.ui.packageview.PackageExplorerPart;
/**
 * This action reveals the currently selected JavaScript element in the 
 * package explorer. 
 * <p>
 * The action is applicable to selections containing elements of type
 * <code>IJavaScriptElement</code>.
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
public class ShowInPackageViewAction extends SelectionDispatchAction {
	
	private JavaEditor fEditor;
	
	/**
	 * Creates a new <code>ShowInPackageViewAction</code>. The action requires 
	 * that the selection provided by the site's selection provider is of type 
	 * <code>org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param site the site providing context information for this action
	 */
	public ShowInPackageViewAction(IWorkbenchSite site) {
		super(site);
		setText(ActionMessages.ShowInPackageViewAction_label); 
		setDescription(ActionMessages.ShowInPackageViewAction_description); 
		setToolTipText(ActionMessages.ShowInPackageViewAction_tooltip); 
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.SHOW_IN_PACKAGEVIEW_ACTION);	
	}
	
	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * @param editor the JavaScript editor
	 */
	public ShowInPackageViewAction(JavaEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(SelectionConverter.canOperateOn(fEditor));
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	public void selectionChanged(ITextSelection selection) {
	}

	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	public void selectionChanged(IStructuredSelection selection) {
		setEnabled(checkEnabled(selection));
	}
	
	private boolean checkEnabled(IStructuredSelection selection) {
		if (selection.size() != 1)
			return false;
		return selection.getFirstElement() instanceof IJavaScriptElement;
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	public void run(ITextSelection selection) {
		try {
			IJavaScriptElement element= SelectionConverter.getElementAtOffset(fEditor);
			if (element != null)
				run(element);
		} catch (JavaScriptModelException e) {
			JavaScriptPlugin.log(e);
			String message= ActionMessages.ShowInPackageViewAction_error_message; 
			ErrorDialog.openError(getShell(), getDialogTitle(), message, e.getStatus());
		}	
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	public void run(IStructuredSelection selection) {
		if (!checkEnabled(selection))
			return;
		run((IJavaScriptElement)selection.getFirstElement());
	}
	
	/*
	 * No Javadoc. The method should be internal but can't be changed since
	 * we shipped it with a public visibility 
	 */
	public void run(IJavaScriptElement element) {
		if (element == null)
			return;
			
		// reveal the top most element only
		IOpenable openable= element.getOpenable();
		if (openable instanceof IJavaScriptElement)
			element= (IJavaScriptElement)openable;

		PackageExplorerPart view= PackageExplorerPart.openInActivePerspective();
		view.tryToReveal(element);
	}

	private static String getDialogTitle() {
		return ActionMessages.ShowInPackageViewAction_dialog_title; 
	}
}
