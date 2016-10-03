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
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.wst.jsdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.wst.jsdt.internal.ui.refactoring.actions.RenameJavaElementAction;
import org.eclipse.wst.jsdt.internal.ui.refactoring.actions.RenameResourceAction;

/**
 * Renames a JavaScript element or workbench resource.
 * <p>
 * Action is applicable to selections containing elements of type
 * <code>IJavaScriptElement</code> or <code>IResource</code>.
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
public class RenameAction extends SelectionDispatchAction {

	private RenameJavaElementAction fRenameJavaElement;
	private RenameResourceAction fRenameResource;

	/**
	 * Creates a new <code>RenameAction</code>. The action requires
	 * that the selection provided by the site's selection provider is of type <code>
	 * org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param site the site providing context information for this action
	 */
	public RenameAction(IWorkbenchSite site) {
		super(site);
		setText(RefactoringMessages.RenameAction_text); 
		fRenameJavaElement= new RenameJavaElementAction(site);
		fRenameJavaElement.setText(getText());
		fRenameResource= new RenameResourceAction(site);
		fRenameResource.setText(getText());
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.RENAME_ACTION);
	}
	
	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * @param editor the JavaScript editor
	 */
	public RenameAction(JavaEditor editor) {
		this(editor.getEditorSite());
		fRenameJavaElement= new RenameJavaElementAction(editor);
	}
	
	/*
	 * @see ISelectionChangedListener#selectionChanged(SelectionChangedEvent)
	 */
	public void selectionChanged(SelectionChangedEvent event) {
		fRenameJavaElement.selectionChanged(event);
		if (fRenameResource != null)
			fRenameResource.selectionChanged(event);
		setEnabled(computeEnabledState());		
	}

	/*
	 * @see SelectionDispatchAction#update(ISelection)
	 */
	public void update(ISelection selection) {
		fRenameJavaElement.update(selection);
		
		if (fRenameResource != null)
			fRenameResource.update(selection);
	
		setEnabled(computeEnabledState());		
	}
	
	private boolean computeEnabledState(){
		if (fRenameResource != null) {
			return fRenameJavaElement.isEnabled() || fRenameResource.isEnabled();
		} else {
			return fRenameJavaElement.isEnabled();
		}
	}
	
	public void run(IStructuredSelection selection) {
		if (fRenameJavaElement.isEnabled())
			fRenameJavaElement.run(selection);
		if (fRenameResource != null && fRenameResource.isEnabled())
			fRenameResource.run(selection);
	}

	public void run(ITextSelection selection) {
		if (fRenameJavaElement.canRunInEditor())
			fRenameJavaElement.run(selection);
		else
			MessageDialog.openInformation(getShell(), RefactoringMessages.RenameAction_rename, RefactoringMessages.RenameAction_unavailable);  
	}
}
