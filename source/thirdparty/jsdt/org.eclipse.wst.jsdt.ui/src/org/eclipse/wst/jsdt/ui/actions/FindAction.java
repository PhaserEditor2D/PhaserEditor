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

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.progress.IProgressService;
import org.eclipse.wst.jsdt.core.IClassFile;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.ILocalVariable;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchScope;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.actions.ActionUtil;
import org.eclipse.wst.jsdt.internal.ui.actions.SelectionConverter;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.wst.jsdt.internal.ui.search.JavaSearchQuery;
import org.eclipse.wst.jsdt.internal.ui.search.JavaSearchScopeFactory;
import org.eclipse.wst.jsdt.internal.ui.search.SearchMessages;
import org.eclipse.wst.jsdt.internal.ui.search.SearchUtil;
import org.eclipse.wst.jsdt.internal.ui.util.ExceptionHandler;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabelProvider;
import org.eclipse.wst.jsdt.ui.search.ElementQuerySpecification;
import org.eclipse.wst.jsdt.ui.search.QuerySpecification;

/**
 * Abstract class for JavaScript search actions.
 * <p>
 * Note: This class is for internal use only. Clients should not use this class.
 * </p>
 * 
 *
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 */
public abstract class FindAction extends SelectionDispatchAction {

	// A dummy which can't be selected in the UI
	private static final IJavaScriptElement RETURN_WITHOUT_BEEP= JavaScriptCore.create(JavaScriptPlugin.getWorkspace().getRoot());
		
	private Class[] fValidTypes;
	private JavaEditor fEditor;	


	FindAction(IWorkbenchSite site) {
		super(site);
		fValidTypes= getValidTypes();
		init();
	}

	FindAction(JavaEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(SelectionConverter.canOperateOn(fEditor));
	}
	
	/**
	 * Called once by the constructors to initialize label, tooltip, image and help support of the action.
	 * To be overridden by implementors of this action.
	 */
	abstract void init();

	/**
	 * Called once by the constructors to get the list of the valid input types of the action.
	 * To be overridden by implementors of this action.
	 * @return the valid input types of the action
	 */
	abstract Class[] getValidTypes();
	
	private boolean canOperateOn(IStructuredSelection sel) {
		return sel != null && !sel.isEmpty() && canOperateOn(getJavaElement(sel, true));
	}
		
	boolean canOperateOn(IJavaScriptElement element) {
		if (element == null || fValidTypes == null || fValidTypes.length == 0 || !ActionUtil.isOnBuildPath(element))
			return false;

		for (int i= 0; i < fValidTypes.length; i++) {
			if (fValidTypes[i].isInstance(element)) {
				if (element.getElementType() == IJavaScriptElement.PACKAGE_FRAGMENT)
					return hasChildren((IPackageFragment)element);
				else
					return true;
			}
		}
		return false;
	}
	
	private boolean hasChildren(IPackageFragment packageFragment) {
		try {
			return packageFragment.hasChildren();
		} catch (JavaScriptModelException ex) {
			return false;
		}
	}

	private IJavaScriptElement getTypeIfPossible(IJavaScriptElement o, boolean silent) {
		switch (o.getElementType()) {
			case IJavaScriptElement.JAVASCRIPT_UNIT:
				if (silent)
					return o;
				else
					return findType((IJavaScriptUnit)o, silent);
			case IJavaScriptElement.CLASS_FILE:
				return ((IClassFile)o).getType();
			default:
				return o;				
		}
	}

	IJavaScriptElement getJavaElement(IStructuredSelection selection, boolean silent) {
		if (selection.size() == 1) {
			Object firstElement= selection.getFirstElement();
			IJavaScriptElement elem= null;
			if (firstElement instanceof IJavaScriptElement) 
				elem= (IJavaScriptElement) firstElement;
			else if (firstElement instanceof IAdaptable) 
				elem= (IJavaScriptElement) ((IAdaptable) firstElement).getAdapter(IJavaScriptElement.class);
			if (elem != null) {
				return getTypeIfPossible(elem, silent);
			}
			
		}
		return null;
	}

	private void showOperationUnavailableDialog() {
		MessageDialog.openInformation(getShell(), SearchMessages.JavaElementAction_operationUnavailable_title, getOperationUnavailableMessage()); 
	}	

	String getOperationUnavailableMessage() {
		return SearchMessages.JavaElementAction_operationUnavailable_generic; 
	}

	private IJavaScriptElement findType(IJavaScriptUnit cu, boolean silent) {
		IType[] types= null;
		try {					
			types= cu.getAllTypes();
		} catch (JavaScriptModelException ex) {
			if (JavaModelUtil.isExceptionToBeLogged(ex))
				ExceptionHandler.log(ex, SearchMessages.JavaElementAction_error_open_message); 
			if (silent)
				return RETURN_WITHOUT_BEEP;
			else
				return null;
		}
		if (types.length == 1 || (silent && types.length > 0))
			return types[0];
		if (silent)
			return RETURN_WITHOUT_BEEP;
		if (types.length == 0)
			return null;
		String title= SearchMessages.JavaElementAction_typeSelectionDialog_title; 
		String message = SearchMessages.JavaElementAction_typeSelectionDialog_message; 
		int flags= (JavaScriptElementLabelProvider.SHOW_DEFAULT);						

		ElementListSelectionDialog dialog= new ElementListSelectionDialog(getShell(), new JavaScriptElementLabelProvider(flags));
		dialog.setTitle(title);
		dialog.setMessage(message);
		dialog.setElements(types);
		
		if (dialog.open() == Window.OK)
			return (IType)dialog.getFirstResult();
		else
			return RETURN_WITHOUT_BEEP;
	}

	/* 
	 * Method declared on SelectionChangedAction.
	 */
	public void run(IStructuredSelection selection) {
		IJavaScriptElement element= getJavaElement(selection, false);
		if (element == null || !element.exists()) {
			showOperationUnavailableDialog();
			return;
		} 
		else if (element == RETURN_WITHOUT_BEEP)
			return;
		
		run(element);
	}

	/* 
	 * Method declared on SelectionChangedAction.
	 */
	public void run(ITextSelection selection) {
		if (!ActionUtil.isProcessable(fEditor))
			return;
		try {
			String title= SearchMessages.SearchElementSelectionDialog_title; 
			String message= SearchMessages.SearchElementSelectionDialog_message; 
			
			IJavaScriptElement[] elements= SelectionConverter.codeResolveForked(fEditor, true);
			if (elements.length > 0 && canOperateOn(elements[0])) {
				IJavaScriptElement element= elements[0];
				if (elements.length > 1)
					element= SelectionConverter.selectJavaElement(elements, getShell(), title, message);
				if (element != null)
					run(element);
			}
			else
				showOperationUnavailableDialog();
		} catch (InvocationTargetException ex) {
			String title= SearchMessages.Search_Error_search_title; 
			String message= SearchMessages.Search_Error_codeResolve; 
			ExceptionHandler.handle(ex, getShell(), title, message);
		} catch (InterruptedException e) {
			// ignore
		}
	}

	/* 
	 * Method declared on SelectionChangedAction.
	 */
	public void selectionChanged(IStructuredSelection selection) {
		setEnabled(canOperateOn(selection));
	}

	/* 
	 * Method declared on SelectionChangedAction.
	 */
	public void selectionChanged(ITextSelection selection) {
	}

	/**
	 * Executes this action for the given JavaScript element.
	 * @param element The JavaScript element to be found.
	 */
	public void run(IJavaScriptElement element) {
		
		if (!ActionUtil.isProcessable(getShell(), element))
			return;
		
		// will return true except for debugging purposes.
		try {
			performNewSearch(element);
		} catch (JavaScriptModelException ex) {
			ExceptionHandler.handle(ex, getShell(), SearchMessages.Search_Error_search_notsuccessful_title, SearchMessages.Search_Error_search_notsuccessful_message); 
		} catch (InterruptedException e) {
			// cancelled
		}
	}

	private void performNewSearch(IJavaScriptElement element) throws JavaScriptModelException, InterruptedException {
		JavaSearchQuery query= new JavaSearchQuery(createQuery(element));
		if (query.canRunInBackground()) {
			/*
			 * This indirection with Object as parameter is needed to prevent the loading
			 * of the Search plug-in: the VM verifies the method call and hence loads the
			 * types used in the method signature, eventually triggering the loading of
			 * a plug-in (in this case ISearchQuery results in Search plug-in being loaded).
			 */
			SearchUtil.runQueryInBackground(query);
		} else {
			IProgressService progressService= PlatformUI.getWorkbench().getProgressService();
			/*
			 * This indirection with Object as parameter is needed to prevent the loading
			 * of the Search plug-in: the VM verifies the method call and hence loads the
			 * types used in the method signature, eventually triggering the loading of
			 * a plug-in (in this case it would be ISearchQuery).
			 */
			IStatus status= SearchUtil.runQueryInForeground(progressService, query);
			if (status.matches(IStatus.ERROR | IStatus.INFO | IStatus.WARNING)) {
				ErrorDialog.openError(getShell(), SearchMessages.Search_Error_search_title, SearchMessages.Search_Error_search_message, status); 
			}
		}
	}
	
	QuerySpecification createQuery(IJavaScriptElement element) throws JavaScriptModelException, InterruptedException {
		JavaSearchScopeFactory factory= JavaSearchScopeFactory.getInstance();
		IJavaScriptSearchScope scope= factory.createWorkspaceScope(true);
		String description= factory.getWorkspaceScopeDescription(true);
		return new ElementQuerySpecification(element, getLimitTo(), scope, description);
	}

	abstract int getLimitTo();

	IType getType(IJavaScriptElement element) {
		if (element == null)
			return null;
		
		IType type= null;
		if (element.getElementType() == IJavaScriptElement.TYPE)
			type= (IType)element;
		else if (element instanceof IMember)
			type= ((IMember)element).getDeclaringType();
		else if (element instanceof ILocalVariable) {
			type= (IType)element.getAncestor(IJavaScriptElement.TYPE);
		}
		return type;
	}
	
	JavaEditor getEditor() {
		return fEditor;
	}
		
}
