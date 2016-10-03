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

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ISetSelectionTarget;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.actions.ActionMessages;
import org.eclipse.wst.jsdt.internal.ui.actions.ActionUtil;
import org.eclipse.wst.jsdt.internal.ui.actions.SelectionConverter;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.wst.jsdt.internal.ui.util.ExceptionHandler;

/**
 * Reveals the selected element in the resource navigator view. 
 * <p>
 * Action is applicable to structured selections containing JavaScript element 
 * or resources.
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
public class ShowInNavigatorViewAction extends SelectionDispatchAction {

	private JavaEditor fEditor;
	
	/**
	 * Creates a new <code>ShowInNavigatorViewAction</code>. The action requires 
	 * that the selection provided by the site's selection provider is of type 
	 * <code>org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param site the site providing context information for this action
	 */
	public ShowInNavigatorViewAction(IWorkbenchSite site) {
		super(site);
		setText(ActionMessages.ShowInNavigatorView_label); 
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.SHOW_IN_NAVIGATOR_VIEW_ACTION);
	}

	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * @param editor the JavaScript editor
	 */
	public ShowInNavigatorViewAction(JavaEditor editor) {
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
		setEnabled(getResource(selection) != null); 
	}

	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	public void run(ITextSelection selection) {
		IJavaScriptElement input= SelectionConverter.getInput(fEditor);
		if (!ActionUtil.isProcessable(getShell(), input))
			return;		
		
		
		try {
			IJavaScriptElement[] elements= SelectionConverter.codeResolveOrInputForked(fEditor);
			if (elements == null || elements.length == 0)
				return;
			
			IJavaScriptElement candidate= elements[0];
			if (elements.length > 1) {
				candidate= SelectionConverter.selectJavaElement(elements, getShell(), getDialogTitle(), ActionMessages.ShowInNavigatorView_dialog_message);
			}
			if (candidate != null) {
				run(getResource(candidate));
			}
		} catch (InvocationTargetException e) {
			ExceptionHandler.handle(e, getDialogTitle(), ActionMessages.SelectionConverter_codeResolve_failed);
		} catch (InterruptedException e) {
			// cancelled
		}
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	public void run(IStructuredSelection selection) {
		run(getResource(selection));
	}
	
	/*
	 * No Javadoc. The method should be internal but can't be changed since
	 * we shipped it with a public visibility 
	 */
	public void run(IResource resource) {
		if (resource == null)
			return;
		try {
			IWorkbenchPage page= getSite().getWorkbenchWindow().getActivePage();	
			IViewPart view= page.showView(IPageLayout.ID_RES_NAV);
			if (view instanceof ISetSelectionTarget) {
				ISelection selection= new StructuredSelection(resource);
				((ISetSelectionTarget)view).selectReveal(selection);
			}
		} catch(PartInitException e) {
			ExceptionHandler.handle(e, getShell(), getDialogTitle(), ActionMessages.ShowInNavigatorView_error_activation_failed); 
		}
	}
	
	private IResource getResource(IStructuredSelection selection) {
		if (selection.size() != 1)
			return null;
		Object element= selection.getFirstElement();
		if (element instanceof IResource)
			return (IResource)element;
		if (element instanceof IJavaScriptElement)
			return getResource((IJavaScriptElement)element);
		return null;
	}
	
	private IResource getResource(IJavaScriptElement element) {
		if (element == null)
			return null;
		
		element= (IJavaScriptElement) element.getOpenable();
		if (element instanceof IJavaScriptUnit) {
			element= ((IJavaScriptUnit) element).getPrimary();
		}
		return element.getResource();
	}
	
	private static String getDialogTitle() {
		return ActionMessages.ShowInNavigatorView_dialog_title; 
	}	
}
