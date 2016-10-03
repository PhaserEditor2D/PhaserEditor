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

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.text.IRewriteTarget;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.IFunctionBinding;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.AddUnimplementedMethodsOperation;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.actions.ActionMessages;
import org.eclipse.wst.jsdt.internal.ui.actions.ActionUtil;
import org.eclipse.wst.jsdt.internal.ui.actions.SelectionConverter;
import org.eclipse.wst.jsdt.internal.ui.actions.WorkbenchRunnableAdapter;
import org.eclipse.wst.jsdt.internal.ui.dialogs.OverrideMethodDialog;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.wst.jsdt.internal.ui.util.BusyIndicatorRunnableContext;
import org.eclipse.wst.jsdt.internal.ui.util.ElementValidator;
import org.eclipse.wst.jsdt.internal.ui.util.ExceptionHandler;
import org.eclipse.wst.jsdt.ui.JavaScriptUI;

/**
 * Adds unimplemented methods of a type. The action opens a dialog from which the user can
 * choose the methods to be added.
 * <p>
 * Will open the parent compilation unit in a JavaScript editor. The result is unsaved, so the
 * user can decide if the changes are acceptable.
 * <p>
 * The action is applicable to structured selections containing elements of type
 * {@link org.eclipse.wst.jsdt.core.IType}.
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
public class OverrideMethodsAction extends SelectionDispatchAction {

	/** The dialog title */
	private static final String DIALOG_TITLE= ActionMessages.OverrideMethodsAction_error_title; 

	/** The compilation unit editor */
	private CompilationUnitEditor fEditor;

	/**
	 * Note: This constructor is for internal use only. Clients should not call this
	 * constructor.
	 * @param editor the compilation unit editor
	 */
	public OverrideMethodsAction(final CompilationUnitEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(checkEnabledEditor());
	}

	/**
	 * Creates a new override method action.
	 * <p>
	 * The action requires that the selection provided by the site's selection provider is
	 * of type {@link org.eclipse.jface.viewers.IStructuredSelection}.
	 * 
	 * @param site the workbench site providing context information for this action
	 */
	public OverrideMethodsAction(final IWorkbenchSite site) {
		super(site);
		setText(ActionMessages.OverrideMethodsAction_label); 
		setDescription(ActionMessages.OverrideMethodsAction_description); 
		setToolTipText(ActionMessages.OverrideMethodsAction_tooltip); 
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.ADD_UNIMPLEMENTED_METHODS_ACTION);
	}

	private boolean canEnable(IStructuredSelection selection) throws JavaScriptModelException {
		if ((selection.size() == 1) && (selection.getFirstElement() instanceof IType)) {
			final IType type= (IType) selection.getFirstElement();
			return type.getJavaScriptUnit() != null;
		}
		if ((selection.size() == 1) && (selection.getFirstElement() instanceof IJavaScriptUnit))
			return true;
		return false;
	}

	private boolean checkEnabledEditor() {
		return fEditor != null && SelectionConverter.canOperateOn(fEditor);
	}

	private String getDialogTitle() {
		return DIALOG_TITLE;
	}

	private IType getSelectedType(IStructuredSelection selection) throws JavaScriptModelException {
		final Object[] elements= selection.toArray();
		if (elements.length == 1 && (elements[0] instanceof IType)) {
			final IType type= (IType) elements[0];
			if (type.getJavaScriptUnit() != null) {
				return type;
			}
		} else if (elements[0] instanceof IJavaScriptUnit) {
			final IType type= ((IJavaScriptUnit) elements[0]).findPrimaryType();
			if (type != null)
				return type;
		}
		return null;
	}

	/*
	 * @see org.eclipse.wst.jsdt.ui.actions.SelectionDispatchAction#run(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void run(IStructuredSelection selection) {
		try {
			final IType type= getSelectedType(selection);
			if (type == null) {
				MessageDialog.openInformation(getShell(), getDialogTitle(), ActionMessages.OverrideMethodsAction_not_applicable);
				notifyResult(false);
				return;
			}
			if (!ElementValidator.check(type, getShell(), getDialogTitle(), false) || !ActionUtil.isEditable(getShell(), type)) {
				notifyResult(false);
				return;
			}
			run(getShell(), type);
		} catch (CoreException exception) {
			ExceptionHandler.handle(exception, getShell(), getDialogTitle(), ActionMessages.OverrideMethodsAction_error_actionfailed); 
		}
	}

	/*
	 * @see org.eclipse.wst.jsdt.ui.actions.SelectionDispatchAction#run(org.eclipse.jface.text.ITextSelection)
	 */
	public void run(ITextSelection selection) {
		try {
			final IType type= SelectionConverter.getTypeAtOffset(fEditor);
			if (type != null) {
				if (!ElementValidator.check(type, getShell(), getDialogTitle(), false) || !ActionUtil.isEditable(fEditor, getShell(), type)) {
					notifyResult(false);
					return;
				}
				run(getShell(), type);
			} else {
				MessageDialog.openInformation(getShell(), getDialogTitle(), ActionMessages.OverrideMethodsAction_not_applicable); 
			}
		} catch (JavaScriptModelException e) {
			ExceptionHandler.handle(e, getShell(), getDialogTitle(), null);
		} catch (CoreException e) {
			ExceptionHandler.handle(e, getShell(), getDialogTitle(), ActionMessages.OverrideMethodsAction_error_actionfailed); 
		}
	}

	private void run(Shell shell, IType type) throws CoreException {
		final OverrideMethodDialog dialog= new OverrideMethodDialog(shell, fEditor, type,false);
		if (!dialog.hasMethodsToOverride()) {
			MessageDialog.openInformation(shell, getDialogTitle(), ActionMessages.OverrideMethodsAction_error_nothing_found);
			notifyResult(false);
			return;
		}
		if (dialog.open() != Window.OK) {
			notifyResult(false);
			return;
		}
			
		final Object[] selected= dialog.getResult();
		if (selected == null) {
			notifyResult(false);
			return;
		}
		
		ArrayList methods= new ArrayList();
		for (int i= 0; i < selected.length; i++) {
			Object elem= selected[i];
			if (elem instanceof IFunctionBinding) {
				methods.add(elem);
			}
		}
		IFunctionBinding[] methodToOverride= (IFunctionBinding[]) methods.toArray(new IFunctionBinding[methods.size()]);

		
		final IEditorPart editor= JavaScriptUI.openInEditor(type.getJavaScriptUnit());
		final IRewriteTarget target= editor != null ? (IRewriteTarget) editor.getAdapter(IRewriteTarget.class) : null;
		if (target != null)
			target.beginCompoundChange();
		try {
			JavaScriptUnit astRoot= dialog.getCompilationUnit();
			final ITypeBinding typeBinding= ASTNodes.getTypeBinding(astRoot, type);
			int insertPos= dialog.getInsertOffset();
			
			AddUnimplementedMethodsOperation operation= (AddUnimplementedMethodsOperation) createRunnable(astRoot, typeBinding, methodToOverride, insertPos, dialog.getGenerateComment());
			IRunnableContext context= JavaScriptPlugin.getActiveWorkbenchWindow();
			if (context == null)
				context= new BusyIndicatorRunnableContext();
			PlatformUI.getWorkbench().getProgressService().runInUI(context, new WorkbenchRunnableAdapter(operation, operation.getSchedulingRule()), operation.getSchedulingRule());
			final String[] created= operation.getCreatedMethods();
			if (created == null || created.length == 0)
				MessageDialog.openInformation(shell, getDialogTitle(), ActionMessages.OverrideMethodsAction_error_nothing_found); 
		} catch (InvocationTargetException exception) {
			ExceptionHandler.handle(exception, shell, getDialogTitle(), null);
		} catch (InterruptedException exception) {
			// Do nothing. Operation has been canceled by user.
		} finally {
			if (target != null)
				target.endCompoundChange();
		}
		notifyResult(true);
	}

	/**
	 * Returns a runnable that creates the method stubs for overridden methods.
	 * 
	 * @param astRoot the AST of the compilation unit to work on. The AST must have been created from a {@link IJavaScriptUnit}, that
	 * means {@link org.eclipse.wst.jsdt.core.dom.ASTParser#setSource(IJavaScriptUnit)} was used.
	 * @param type the binding of the type to add the new methods to. The type binding must correspond to a type declaration in the AST.
	 * @param methodToOverride the bindings of methods to override or <code>null</code> to implement all unimplemented, abstract methods from super types.
	 * @param insertPos a hint for a location in the source where to insert the new methods or <code>-1</code> to use the default behavior.
	 * @param createComments if set, comments will be added to the new methods.
	 * @return returns a runnable that creates the methods stubs.
	 * @throws IllegalArgumentException a {@link IllegalArgumentException} is thrown if the AST passed has not been created from a {@link IJavaScriptUnit}.
	 * 
	 * 
	 */
	public static IWorkspaceRunnable createRunnable(JavaScriptUnit astRoot, ITypeBinding type, IFunctionBinding[] methodToOverride, int insertPos, boolean createComments) {
		AddUnimplementedMethodsOperation operation= new AddUnimplementedMethodsOperation(astRoot, type, methodToOverride, insertPos, true, true, false);
		operation.setCreateComments(createComments);
		return operation;
	}

	/*
	 * @see org.eclipse.wst.jsdt.ui.actions.SelectionDispatchAction#selectionChanged(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void selectionChanged(IStructuredSelection selection) {
		try {
			setEnabled(canEnable(selection));
		} catch (JavaScriptModelException exception) {
			if (JavaModelUtil.isExceptionToBeLogged(exception))
				JavaScriptPlugin.log(exception);
			setEnabled(false);
		}
	}

	/*
	 * @see org.eclipse.wst.jsdt.ui.actions.SelectionDispatchAction#run(org.eclipse.jface.text.ITextSelection)
	 */
	public void selectionChanged(ITextSelection selection) {
		// Do nothing
	}
}
