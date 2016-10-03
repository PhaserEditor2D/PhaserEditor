/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.core.IClassFile;
import org.eclipse.wst.jsdt.core.IImportDeclaration;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.Signature;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.IJavaStatusConstants;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.actions.ActionMessages;
import org.eclipse.wst.jsdt.internal.ui.actions.ActionUtil;
import org.eclipse.wst.jsdt.internal.ui.actions.SelectionConverter;
import org.eclipse.wst.jsdt.internal.ui.browsing.LogicalPackage;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.wst.jsdt.internal.ui.util.ExceptionHandler;
import org.eclipse.wst.jsdt.internal.ui.util.OpenTypeHierarchyUtil;

/**
 * This action opens a type hierarchy on the selected type.
 * <p>
 * The action is applicable to selections containing elements of type
 * <code>IType</code>.
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
public class OpenTypeHierarchyAction extends SelectionDispatchAction {
	
	private JavaEditor fEditor;
	
	/**
	 * Creates a new <code>OpenTypeHierarchyAction</code>. The action requires
	 * that the selection provided by the site's selection provider is of type <code>
	 * org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param site the site providing context information for this action
	 */
	public OpenTypeHierarchyAction(IWorkbenchSite site) {
		super(site);
		setText(ActionMessages.OpenTypeHierarchyAction_label); 
		setToolTipText(ActionMessages.OpenTypeHierarchyAction_tooltip); 
		setDescription(ActionMessages.OpenTypeHierarchyAction_description); 
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.OPEN_TYPE_HIERARCHY_ACTION);
	}
	
	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * @param editor the JavaScript editor
	 */
	public OpenTypeHierarchyAction(JavaEditor editor) {
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
		setEnabled(isEnabled(selection));
	}
	
	private boolean isEnabled(IStructuredSelection selection) {
		if (selection.size() != 1)
			return false;
		Object input= selection.getFirstElement();
		
		
		if (input instanceof LogicalPackage)
			return true;
		
		if (!(input instanceof IJavaScriptElement))
			return false;
		switch (((IJavaScriptElement)input).getElementType()) {
			case IJavaScriptElement.INITIALIZER:
			case IJavaScriptElement.METHOD:
			case IJavaScriptElement.FIELD:
			case IJavaScriptElement.TYPE:
				return true;
			case IJavaScriptElement.PACKAGE_FRAGMENT_ROOT:
			case IJavaScriptElement.JAVASCRIPT_PROJECT:
			case IJavaScriptElement.PACKAGE_FRAGMENT:
			case IJavaScriptElement.IMPORT_DECLARATION:	
			case IJavaScriptElement.CLASS_FILE:
			case IJavaScriptElement.JAVASCRIPT_UNIT:
				return true;
			case IJavaScriptElement.LOCAL_VARIABLE:
			default:
				return false;
		}
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
			if (elements == null)
				return;
			List candidates= new ArrayList(elements.length);
			for (int i= 0; i < elements.length; i++) {
				IJavaScriptElement[] resolvedElements= OpenTypeHierarchyUtil.getCandidates(elements[i]);
				if (resolvedElements != null)	
					candidates.addAll(Arrays.asList(resolvedElements));
			}
			run((IJavaScriptElement[])candidates.toArray(new IJavaScriptElement[candidates.size()]));
		} catch (InvocationTargetException e) {
			ExceptionHandler.handle(e, getShell(), getDialogTitle(), ActionMessages.SelectionConverter_codeResolve_failed);
		} catch (InterruptedException e) {
			// cancelled
		}
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	public void run(IStructuredSelection selection) {
		if (selection.size() != 1)
			return;
		Object input= selection.getFirstElement();
		
		if (input instanceof LogicalPackage) {
			IPackageFragment[] fragments= ((LogicalPackage)input).getFragments();
			if (fragments.length == 0)
				return;
			input= fragments[0];
		}

		if (!(input instanceof IJavaScriptElement)) {
			IStatus status= createStatus(ActionMessages.OpenTypeHierarchyAction_messages_no_java_element); 
			ErrorDialog.openError(getShell(), getDialogTitle(), ActionMessages.OpenTypeHierarchyAction_messages_title, status); 
			return;
		}
		IJavaScriptElement element= (IJavaScriptElement) input;
		if (!ActionUtil.isProcessable(getShell(), element))
			return;

		List result= new ArrayList(1);
		IStatus status= compileCandidates(result, element);
		if (status.isOK()) {
			run((IJavaScriptElement[]) result.toArray(new IJavaScriptElement[result.size()]));
		} else {
			ErrorDialog.openError(getShell(), getDialogTitle(), ActionMessages.OpenTypeHierarchyAction_messages_title, status); 
		}
	}

	/*
	 * No Javadoc since the method isn't meant to be public but is
	 * since the beginning
	 */
	public void run(IJavaScriptElement[] elements) {
		if (elements.length == 0) {
			getShell().getDisplay().beep();
			return;
		}
		OpenTypeHierarchyUtil.open(elements, getSite().getWorkbenchWindow());
	}
	
	private static String getDialogTitle() {
		return ActionMessages.OpenTypeHierarchyAction_dialog_title; 
	}
	
	private static IStatus compileCandidates(List result, IJavaScriptElement elem) {
		IStatus ok= new Status(IStatus.OK, JavaScriptPlugin.getPluginId(), 0, "", null); //$NON-NLS-1$		
		try {
			switch (elem.getElementType()) {
				case IJavaScriptElement.INITIALIZER:
				case IJavaScriptElement.METHOD:
				case IJavaScriptElement.FIELD:
				case IJavaScriptElement.TYPE:
				case IJavaScriptElement.PACKAGE_FRAGMENT_ROOT:
				case IJavaScriptElement.JAVASCRIPT_PROJECT:
					result.add(elem);
					return ok;
				case IJavaScriptElement.PACKAGE_FRAGMENT:
					if (((IPackageFragment)elem).containsJavaResources()) {
						result.add(elem);
						return ok;
					}
					return createStatus(ActionMessages.OpenTypeHierarchyAction_messages_no_java_resources); 
				case IJavaScriptElement.IMPORT_DECLARATION:	
					IImportDeclaration decl= (IImportDeclaration) elem;
					if (decl.isOnDemand()) {
						elem= JavaModelUtil.findTypeContainer(elem.getJavaScriptProject(), Signature.getQualifier(elem.getElementName()));
					} else {
						elem= elem.getJavaScriptProject().findType(elem.getElementName());
					}
					if (elem != null) {
						result.add(elem);
						return ok;
					}
					return createStatus(ActionMessages.OpenTypeHierarchyAction_messages_unknown_import_decl);
				case IJavaScriptElement.CLASS_FILE:
					result.add(((IClassFile)elem).getType());
					return ok;				
				case IJavaScriptElement.JAVASCRIPT_UNIT:
					IJavaScriptUnit cu= (IJavaScriptUnit)elem;
					IType[] types= cu.getTypes();
					if (types.length > 0) {
						result.addAll(Arrays.asList(types));
						return ok;
					}
					return createStatus(ActionMessages.OpenTypeHierarchyAction_messages_no_types); 
			}
		} catch (JavaScriptModelException e) {
			return e.getStatus();
		}
		return createStatus(ActionMessages.OpenTypeHierarchyAction_messages_no_valid_java_element); 
	}
	
	private static IStatus createStatus(String message) {
		return new Status(IStatus.INFO, JavaScriptPlugin.getPluginId(), IJavaStatusConstants.INTERNAL_ERROR, message, null);
	}			
}
