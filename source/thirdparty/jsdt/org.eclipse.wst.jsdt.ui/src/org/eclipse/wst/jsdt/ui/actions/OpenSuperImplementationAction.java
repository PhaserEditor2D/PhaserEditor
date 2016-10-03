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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.core.Flags;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.corext.util.MethodOverrideTester;
import org.eclipse.wst.jsdt.internal.corext.util.SuperTypeHierarchyCache;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.actions.ActionMessages;
import org.eclipse.wst.jsdt.internal.ui.actions.ActionUtil;
import org.eclipse.wst.jsdt.internal.ui.actions.SelectionConverter;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.wst.jsdt.internal.ui.util.ExceptionHandler;
import org.eclipse.wst.jsdt.ui.JavaScriptUI;

/**
 * The action opens a JavaScript editor on the selected method's super implementation.
 * <p>
 * The action is applicable to selections containing elements of type <code>
 * IFunction</code>.
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
public class OpenSuperImplementationAction extends SelectionDispatchAction {

	private JavaEditor fEditor;

	/**
	 * Creates a new <code>OpenSuperImplementationAction</code>. The action requires
	 * that the selection provided by the site's selection provider is of type <code>
	 * org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param site the site providing context information for this action
	 */
	public OpenSuperImplementationAction(IWorkbenchSite site) {
		super(site);
		setText(ActionMessages.OpenSuperImplementationAction_label); 
		setDescription(ActionMessages.OpenSuperImplementationAction_description); 
		setToolTipText(ActionMessages.OpenSuperImplementationAction_tooltip); 
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.OPEN_SUPER_IMPLEMENTATION_ACTION);
	}
	
    

	
	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * @param editor the JavaScript editor
	 */
	public OpenSuperImplementationAction(JavaEditor editor) {
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
		IFunction method= getMethod(selection);
		
		setEnabled(method != null && checkMethod(method));
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	public void run(ITextSelection selection) {
		if (!ActionUtil.isProcessable(fEditor))
			return;
		IJavaScriptElement element= elementAtOffset();
		if (element == null || !(element instanceof IFunction)) {
			MessageDialog.openInformation(getShell(), getDialogTitle(), ActionMessages.OpenSuperImplementationAction_not_applicable); 
			return;
		}
		run((IFunction) element);
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	public void run(IStructuredSelection selection) {
		run(getMethod(selection));
	}
	
	/*
	 * No Javadoc since the method isn't meant to be public but is
	 * since the beginning
	 */
	public void run(IFunction method) {
		if (method == null)
			return;		
		if (!ActionUtil.isProcessable(getShell(), method))
			return;
		
		if (!checkMethod(method)) {
			MessageDialog.openInformation(getShell(), getDialogTitle(), 
				Messages.format(ActionMessages.OpenSuperImplementationAction_no_super_implementation, method.getElementName())); 
			return;
		}		

		try {
			IFunction impl= findSuperImplementation(method);
			if (impl != null) {
				JavaScriptUI.openInEditor(impl, true, true);
			}
		} catch (CoreException e) {
			ExceptionHandler.handle(e, getDialogTitle(), ActionMessages.OpenSuperImplementationAction_error_message);
		}
	}
	
	private IFunction findSuperImplementation(IFunction method) throws JavaScriptModelException {
		MethodOverrideTester tester= SuperTypeHierarchyCache.getMethodOverrideTester(method.getDeclaringType());
		return tester.findOverriddenMethod(method, false);
	}
	
	
	private IFunction getMethod(IStructuredSelection selection) {
		if (selection.size() != 1)
			return null;
		Object element= selection.getFirstElement();
		if (element instanceof IFunction) {
			return (IFunction) element;
		}
		return null;
	}
	
	private boolean checkMethod(IFunction method) {
		try {
			int flags= method.getFlags();
			if (!Flags.isStatic(flags) && !Flags.isPrivate(flags)) {
				IType declaringType= method.getDeclaringType();
				if (declaringType==null)
					return false;
				if (SuperTypeHierarchyCache.hasInCache(declaringType)) {
					if (findSuperImplementation(method) == null) {
						return false;
					}
				}
				return true;
			}
		} catch (JavaScriptModelException e) {
			if (!e.isDoesNotExist()) {
				JavaScriptPlugin.log(e);
			}
		}
		return false;
	}
	
	private IJavaScriptElement elementAtOffset() {
		try {
			return SelectionConverter.getElementAtOffset(fEditor);
		} catch(JavaScriptModelException e) {
		}
		return null;
	}
	
	private static String getDialogTitle() {
		return ActionMessages.OpenSuperImplementationAction_error_title; 
	}		
}
