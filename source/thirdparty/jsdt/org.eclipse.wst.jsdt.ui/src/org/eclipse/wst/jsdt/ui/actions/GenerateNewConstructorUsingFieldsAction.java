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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.text.IRewriteTarget;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.core.Flags;
import org.eclipse.wst.jsdt.core.IField;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.dom.IFunctionBinding;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.IVariableBinding;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.AddCustomConstructorOperation;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.StubUtility2;
import org.eclipse.wst.jsdt.internal.corext.dom.Bindings;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.actions.ActionMessages;
import org.eclipse.wst.jsdt.internal.ui.actions.ActionUtil;
import org.eclipse.wst.jsdt.internal.ui.actions.GenerateConstructorUsingFieldsContentProvider;
import org.eclipse.wst.jsdt.internal.ui.actions.GenerateConstructorUsingFieldsSelectionDialog;
import org.eclipse.wst.jsdt.internal.ui.actions.GenerateConstructorUsingFieldsValidator;
import org.eclipse.wst.jsdt.internal.ui.actions.SelectionConverter;
import org.eclipse.wst.jsdt.internal.ui.actions.WorkbenchRunnableAdapter;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.wst.jsdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.wst.jsdt.internal.ui.util.BusyIndicatorRunnableContext;
import org.eclipse.wst.jsdt.internal.ui.util.ElementValidator;
import org.eclipse.wst.jsdt.internal.ui.util.ExceptionHandler;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.BindingLabelProvider;
import org.eclipse.wst.jsdt.ui.JavaScriptUI;

/**
 * Creates constructors for a type based on existing fields.
 * <p>
 * Will open the parent compilation unit in a JavaScript editor. Opens a dialog with a list
 * fields from which a constructor will be generated. User is able to check or uncheck
 * items before constructors are generated. The result is unsaved, so the user can decide
 * if the changes are acceptable.
 * <p>
 * The action is applicable to structured selections containing elements of type
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
public class GenerateNewConstructorUsingFieldsAction extends SelectionDispatchAction {

	private CompilationUnitEditor fEditor;

	/**
	 * Note: This constructor is for internal use only. Clients should not call this
	 * constructor.
	 * 
	 * @param editor the compilation unit editor
	 */
	public GenerateNewConstructorUsingFieldsAction(CompilationUnitEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(checkEnabledEditor());
	}

	/**
	 * Creates a new <code>GenerateConstructorUsingFieldsAction</code>. The action requires
	 * that the selection provided by the site's selection provider is of type <code>
	 * org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param site the site providing context information for this action
	 */
	public GenerateNewConstructorUsingFieldsAction(IWorkbenchSite site) {
		super(site);
		setText(ActionMessages.GenerateConstructorUsingFieldsAction_label); 
		setDescription(ActionMessages.GenerateConstructorUsingFieldsAction_description); 
		setToolTipText(ActionMessages.GenerateConstructorUsingFieldsAction_tooltip); 

		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.CREATE_NEW_CONSTRUCTOR_ACTION);
	}

	private boolean canEnable(IStructuredSelection selection) throws JavaScriptModelException {
		if (getSelectedFields(selection) != null)
			return true;

		if ((selection.size() == 1) && (selection.getFirstElement() instanceof IType)) {
			IType type= (IType) selection.getFirstElement();
			return type.getJavaScriptUnit() != null;
		}

		if ((selection.size() == 1) && (selection.getFirstElement() instanceof IJavaScriptUnit))
			return true;

		return false;
	}

	private boolean canRunOn(IField[] fields) throws JavaScriptModelException {
		if (fields != null && fields.length > 0) {
			return true;
		}
		return false;
	}

	private boolean checkEnabledEditor() {
		return fEditor != null && SelectionConverter.canOperateOn(fEditor);
	}

	/*
	 * Returns fields in the selection or <code>null</code> if the selection is empty or
	 * not valid.
	 */
	private IField[] getSelectedFields(IStructuredSelection selection) {
		List elements= selection.toList();
		if (elements.size() > 0) {
			IField[] fields= new IField[elements.size()];
			IJavaScriptUnit unit= null;
			for (int index= 0; index < elements.size(); index++) {
				if (elements.get(index) instanceof IField) {
					IField field= (IField) elements.get(index);
					if (index == 0) {
						// remember the CU of the first element
						unit= field.getJavaScriptUnit();
						if (unit == null) {
							return null;
						}
					} else if (!unit.equals(field.getJavaScriptUnit())) {
						// all fields must be in the same CU
						return null;
					}
					
					final IType declaringType= field.getDeclaringType();
					if (declaringType==null)
						return null;
					
					fields[index]= field;
				} else {
					return null;
				}
			}
			return fields;
		}
		return null;
	}

	private IType getSelectedType(IStructuredSelection selection) throws JavaScriptModelException {
		Object[] elements= selection.toArray();
		if (elements.length == 1 && (elements[0] instanceof IType)) {
			IType type= (IType) elements[0];
			if (type.getJavaScriptUnit() != null) {
				return type;
			}
		} else if (elements[0] instanceof IJavaScriptUnit) {
			IJavaScriptUnit unit= (IJavaScriptUnit) elements[0];
			IType type= unit.findPrimaryType();
			if (type != null)
				return type;
		} else if (elements[0] instanceof IField) {
			return ((IField) elements[0]).getJavaScriptUnit().findPrimaryType();
		}
		return null;
	}

	/*
	 * (non-Javadoc) Method declared on SelectionDispatchAction
	 */
	public void run(IStructuredSelection selection) {
		try {
			IType selectionType= getSelectedType(selection);
			if (selectionType == null) {
				MessageDialog.openInformation(getShell(), ActionMessages.GenerateConstructorUsingFieldsAction_error_title, ActionMessages.GenerateConstructorUsingFieldsAction_not_applicable);
				notifyResult(false);
				return;
			}

			IField[] selectedFields= getSelectedFields(selection);

			if (canRunOn(selectedFields)) {
				run(selectedFields[0].getDeclaringType(), selectedFields, false);
				return;
			}
			Object firstElement= selection.getFirstElement();

			if (firstElement instanceof IType) {
				run((IType) firstElement, new IField[0], false);
			} else if (firstElement instanceof IJavaScriptUnit) {
				run(((IJavaScriptUnit) firstElement).findPrimaryType(), new IField[0], false);
			}
		} catch (CoreException exception) {
			ExceptionHandler.handle(exception, getShell(), ActionMessages.GenerateConstructorUsingFieldsAction_error_title, ActionMessages.GenerateConstructorUsingFieldsAction_error_actionfailed); 
		}
	}

	/*
	 * (non-Javadoc) Method declared on SelectionDispatchAction
	 */
	public void run(ITextSelection selection) {
		if (!ActionUtil.isProcessable(fEditor)) {
			notifyResult(false);
			return;
		}
		try {
			IJavaScriptElement[] elements= SelectionConverter.codeResolveForked(fEditor, true);
			if (elements.length == 1 && (elements[0] instanceof IField)) {
				IField field= (IField) elements[0];
				run(field.getDeclaringType(), new IField[] { field}, false);
				return;
			}
			IJavaScriptElement element= SelectionConverter.getElementAtOffset(fEditor);
			if (element != null) {
				IType type= (IType) element.getAncestor(IJavaScriptElement.TYPE);
				if (type != null) {
					if (type.getFields().length > 0) {
						run(type, new IField[0], true);
					} else {
						MessageDialog.openInformation(getShell(), ActionMessages.GenerateConstructorUsingFieldsAction_error_title, ActionMessages.GenerateConstructorUsingFieldsAction_typeContainsNoFields_message);
					}
					return;
				}
			}
			MessageDialog.openInformation(getShell(), ActionMessages.GenerateConstructorUsingFieldsAction_error_title, ActionMessages.GenerateConstructorUsingFieldsAction_not_applicable); 
		} catch (CoreException exception) {
			ExceptionHandler.handle(exception, getShell(), ActionMessages.GenerateConstructorUsingFieldsAction_error_title, ActionMessages.GenerateConstructorUsingFieldsAction_error_actionfailed); 
		} catch (InvocationTargetException exception) {
			ExceptionHandler.handle(exception, getShell(), ActionMessages.GenerateConstructorUsingFieldsAction_error_title, ActionMessages.GenerateConstructorUsingFieldsAction_error_actionfailed); 
		} catch (InterruptedException e) {
			// cancelled
		}
	}

	// ---- Helpers -------------------------------------------------------------------

	void run(IType type, IField[] selected, boolean activated) throws CoreException {
		if (!ElementValidator.check(type, getShell(), ActionMessages.GenerateConstructorUsingFieldsAction_error_title, activated)) {
			notifyResult(false);
			return;
		}
		if (!ActionUtil.isEditable(fEditor, getShell(), type)) {
			notifyResult(false);
			return;
		}
		if (type.getJavaScriptUnit() == null) {
			MessageDialog.openInformation(getShell(), ActionMessages.GenerateConstructorUsingFieldsAction_error_title, ActionMessages.GenerateNewConstructorUsingFieldsAction_error_not_a_source_file);
			notifyResult(false);
			return;
			
		}
		
		IField[] candidates= type.getFields();
		ArrayList fields= new ArrayList();
		for (int index= 0; index < candidates.length; index++) {
			boolean isStatic= Flags.isStatic(candidates[index].getFlags());
			if (!isStatic) {
				fields.add(candidates[index]);
			}
		}
		if (fields.isEmpty()) {
			MessageDialog.openInformation(getShell(), ActionMessages.GenerateConstructorUsingFieldsAction_error_title, ActionMessages.GenerateConstructorUsingFieldsAction_typeContainsNoFields_message);
			notifyResult(false);
			return;
		}
		final GenerateConstructorUsingFieldsContentProvider provider= new GenerateConstructorUsingFieldsContentProvider(type, fields, Arrays.asList(selected));
		IFunctionBinding[] bindings= null;
		final ITypeBinding provided= provider.getType();
		if (provided.isAnonymous()) {
			MessageDialog.openInformation(getShell(), ActionMessages.GenerateConstructorUsingFieldsAction_error_title, ActionMessages.GenerateConstructorUsingFieldsAction_error_anonymous_class);
			notifyResult(false);
			return;
		}
		
		bindings= StubUtility2.getVisibleConstructors(provided, false, true);
		if (bindings.length == 0) {
			MessageDialog.openInformation(getShell(), ActionMessages.GenerateConstructorUsingFieldsAction_error_title, ActionMessages.GenerateConstructorUsingFieldsAction_error_nothing_found);
			notifyResult(false);
			return;
		}

		GenerateConstructorUsingFieldsSelectionDialog dialog= new GenerateConstructorUsingFieldsSelectionDialog(getShell(), new BindingLabelProvider(), provider, fEditor, type, bindings);
		dialog.setCommentString(ActionMessages.SourceActionDialog_createConstructorComment); 
		dialog.setTitle(ActionMessages.GenerateConstructorUsingFieldsAction_dialog_title); 
		dialog.setInitialSelections(provider.getInitiallySelectedElements());
		dialog.setContainerMode(true);
		dialog.setSize(60, 18);
		dialog.setInput(new Object());
		dialog.setMessage(ActionMessages.GenerateConstructorUsingFieldsAction_dialog_label); 
		dialog.setValidator(new GenerateConstructorUsingFieldsValidator(dialog, provided, fields.size()));

		final int dialogResult= dialog.open();
		if (dialogResult == Window.OK) {
			Object[] elements= dialog.getResult();
			if (elements == null) {
				notifyResult(false);
				return;
			}
			ArrayList result= new ArrayList(elements.length);
			for (int index= 0; index < elements.length; index++) {
				if (elements[index] instanceof IVariableBinding)
					result.add(elements[index]);
			}
			IVariableBinding[] variables= new IVariableBinding[result.size()];
			result.toArray(variables);
			IEditorPart editor= JavaScriptUI.openInEditor(type.getJavaScriptUnit());
			CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings(type.getJavaScriptProject());
			settings.createComments= dialog.getGenerateComment();
			IFunctionBinding constructor= dialog.getSuperConstructorChoice();
			IRewriteTarget target= editor != null ? (IRewriteTarget) editor.getAdapter(IRewriteTarget.class) : null;
			if (target != null)
				target.beginCompoundChange();
			try {
				AddCustomConstructorOperation operation= new AddCustomConstructorOperation(type, dialog.getElementPosition(), provider.getCompilationUnit(), variables, constructor, settings, true, false);
				operation.setVisibility(dialog.getVisibilityModifier());
				if (constructor.getParameterTypes().length == 0)
					operation.setOmitSuper(dialog.isOmitSuper());
				IRunnableContext context= JavaScriptPlugin.getActiveWorkbenchWindow();
				if (context == null)
					context= new BusyIndicatorRunnableContext();
				PlatformUI.getWorkbench().getProgressService().runInUI(context, new WorkbenchRunnableAdapter(operation, operation.getSchedulingRule()), operation.getSchedulingRule());
			} catch (InvocationTargetException exception) {
				ExceptionHandler.handle(exception, getShell(), ActionMessages.GenerateConstructorUsingFieldsAction_error_title, ActionMessages.GenerateConstructorUsingFieldsAction_error_actionfailed); 
			} catch (InterruptedException exception) {
				// Do nothing. Operation has been canceled by user.
			} finally {
				if (target != null) {
					target.endCompoundChange();
				}
			}
		}
		notifyResult(dialogResult == Window.OK);
	}

	private IFunctionBinding getObjectConstructor(JavaScriptUnit compilationUnit) {
		final ITypeBinding binding= compilationUnit.getAST().resolveWellKnownType("java.lang.Object"); //$NON-NLS-1$
		return Bindings.findMethodInType(binding, "Object", new ITypeBinding[0]); //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc) Method declared on SelectionDispatchAction
	 */
	public void selectionChanged(IStructuredSelection selection) {
		try {
			setEnabled(canEnable(selection));
		} catch (JavaScriptModelException e) {
			// http://bugs.eclipse.org/bugs/show_bug.cgi?id=19253
			if (JavaModelUtil.isExceptionToBeLogged(e))
				JavaScriptPlugin.log(e);
			setEnabled(false);
		}
	}

	// ---- JavaScript Editor --------------------------------------------------------------

	/*
	 * (non-Javadoc) Method declared on SelectionDispatchAction
	 */
	public void selectionChanged(ITextSelection selection) {
	}
}
