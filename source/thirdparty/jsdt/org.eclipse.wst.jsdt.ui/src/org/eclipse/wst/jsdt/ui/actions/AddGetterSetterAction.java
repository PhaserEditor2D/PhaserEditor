/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Philippe Ombredanne - bug 149382
 *******************************************************************************/
package org.eclipse.wst.jsdt.ui.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.IRewriteTarget;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.wst.jsdt.core.Flags;
import org.eclipse.wst.jsdt.core.IField;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.Signature;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.AddGetterSetterOperation;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.GetterSetterUtil;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.IRequestQuery;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.wst.jsdt.internal.corext.template.java.CodeTemplateContextType;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.actions.ActionMessages;
import org.eclipse.wst.jsdt.internal.ui.actions.ActionUtil;
import org.eclipse.wst.jsdt.internal.ui.actions.SelectionConverter;
import org.eclipse.wst.jsdt.internal.ui.actions.WorkbenchRunnableAdapter;
import org.eclipse.wst.jsdt.internal.ui.dialogs.SourceActionDialog;
import org.eclipse.wst.jsdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.wst.jsdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.wst.jsdt.internal.ui.util.BusyIndicatorRunnableContext;
import org.eclipse.wst.jsdt.internal.ui.util.ElementValidator;
import org.eclipse.wst.jsdt.internal.ui.util.ExceptionHandler;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.JavaElementImageProvider;
import org.eclipse.wst.jsdt.ui.JavaScriptElementComparator;
import org.eclipse.wst.jsdt.ui.JavaScriptElementImageDescriptor;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabelProvider;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabels;
import org.eclipse.wst.jsdt.ui.JavaScriptUI;

/**
 * Creates getter and setter methods for a type's fields. Opens a dialog with a list of
 * fields for which a setter or getter can be generated. User is able to check or uncheck
 * items before setters or getters are generated.
 * <p>
 * Will open the parent compilation unit in a JavaScript editor. The result is unsaved, so the
 * user can decide if the changes are acceptable.
 * <p>
 * The action is applicable to structured selections containing elements of type
 * <code>IField</code> or <code>IType</code>.
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
public class AddGetterSetterAction extends SelectionDispatchAction {

	private boolean fSort;

	//private boolean fFinal;

	private int fVisibility;

	private boolean fGenerateComment;

	private int fNumEntries;

	private CompilationUnitEditor fEditor;

	private static final String DIALOG_TITLE= ActionMessages.AddGetterSetterAction_error_title; 

	/**
	 * Creates a new <code>AddGetterSetterAction</code>. The action requires that the
	 * selection provided by the site's selection provider is of type <code>
	 * org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param site the site providing context information for this action
	 */
	public AddGetterSetterAction(IWorkbenchSite site) {
		super(site);
		setText(ActionMessages.AddGetterSetterAction_label); 
		setDescription(ActionMessages.AddGetterSetterAction_description); 
		setToolTipText(ActionMessages.AddGetterSetterAction_tooltip); 

		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.GETTERSETTER_ACTION);
	}

	/**
	 * Note: This constructor is for internal use only. Clients should not call this
	 * constructor.
	 * 
	 * @param editor the compilation unit editor
	 */
	public AddGetterSetterAction(CompilationUnitEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(SelectionConverter.getInputAsCompilationUnit(editor) != null);
		fEditor.getEditorSite();
	}

	// ---- Structured Viewer -----------------------------------------------------------

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

	/*
	 * (non-Javadoc) Method declared on SelectionDispatchAction
	 */
	public void run(IStructuredSelection selection) {
		try {
			IField[] selectedFields= getSelectedFields(selection);
			if (canRunOn(selectedFields)) {
				run(selectedFields[0].getDeclaringType(), selectedFields, false);
				return;
			}
			Object firstElement= selection.getFirstElement();

			if (firstElement instanceof IType)
				run((IType) firstElement, new IField[0], false);
			else if (firstElement instanceof IJavaScriptUnit) {
				// http://bugs.eclipse.org/bugs/show_bug.cgi?id=38500
				IType type= ((IJavaScriptUnit) firstElement).findPrimaryType();
				// type can be null if file has a bad encoding
				if (type == null) {
					MessageDialog.openError(getShell(), 
						ActionMessages.AddGetterSetterAction_no_primary_type_title, 
						ActionMessages.AddGetterSetterAction_no_primary_type_message);
					notifyResult(false);
					return;
				}
				
				run(((IJavaScriptUnit) firstElement).findPrimaryType(), new IField[0], false);
			}
		} catch (CoreException e) {
			ExceptionHandler.handle(e, getShell(), DIALOG_TITLE, ActionMessages.AddGetterSetterAction_error_actionfailed); 
		}

	}

	private boolean canEnable(IStructuredSelection selection) throws JavaScriptModelException {
		if (getSelectedFields(selection) != null)
			return true;

		if ((selection.size() == 1) && (selection.getFirstElement() instanceof IType)) {
			IType type= (IType) selection.getFirstElement();
			return type.getJavaScriptUnit() != null && !type.isLocal();
		}

		if ((selection.size() == 1) && (selection.getFirstElement() instanceof IJavaScriptUnit))
			return true;

		return false;
	}

	private boolean canRunOn(IField[] fields) throws JavaScriptModelException {
		if (fields == null || fields.length == 0)
			return false;
		int count= 0;
		for (int index= 0; index < fields.length; index++) {
			count++;
		}
		if (count == 0)
			MessageDialog.openInformation(getShell(), DIALOG_TITLE, ActionMessages.AddGetterSetterAction_not_applicable); 
		return (count > 0);
	}

	private void resetNumEntries() {
		fNumEntries= 0;
	}

	private void incNumEntries() {
		fNumEntries++;
	}

	private void run(IType type, IField[] preselected, boolean editor) throws CoreException {
		if (type.getJavaScriptUnit() == null) {
			MessageDialog.openInformation(getShell(), DIALOG_TITLE, ActionMessages.AddGetterSetterAction_error_not_in_source_file);
			notifyResult(false);
			return;
		}
		if (!ElementValidator.check(type, getShell(), DIALOG_TITLE, editor)) {
			notifyResult(false);
			return;
		}
		if (!ActionUtil.isEditable(getShell(), type)) {
			notifyResult(false);
			return;
		}

		ILabelProvider lp= new AddGetterSetterLabelProvider();
		resetNumEntries();
		Map entries= createGetterSetterMapping(type);
		if (entries.isEmpty()) {
			MessageDialog.openInformation(getShell(), DIALOG_TITLE, ActionMessages.AddGettSetterAction_typeContainsNoFields_message);
			notifyResult(false);
			return;
		}
		AddGetterSetterContentProvider cp= new AddGetterSetterContentProvider(entries);
		GetterSetterTreeSelectionDialog dialog= new GetterSetterTreeSelectionDialog(getShell(), lp, cp, fEditor, type);
		dialog.setComparator(new JavaScriptElementComparator());
		dialog.setTitle(DIALOG_TITLE);
		String message= ActionMessages.AddGetterSetterAction_dialog_label;
		dialog.setMessage(message);
		dialog.setValidator(createValidator(fNumEntries));
		dialog.setContainerMode(true);
		dialog.setSize(60, 18);
		dialog.setInput(type);

		if (preselected.length > 0) {
			dialog.setInitialSelections(preselected);
			dialog.setExpandedElements(preselected);
		}
		final Set keySet= new LinkedHashSet(entries.keySet());
		int dialogResult= dialog.open();
		if (dialogResult == Window.OK) {
			Object[] result= dialog.getResult();
			if (result == null) {
				notifyResult(false);
				return;
			}
			fSort= dialog.getSortOrder();
			//fFinal= dialog.getFinal();
			fVisibility= dialog.getVisibilityModifier();
			fGenerateComment= dialog.getGenerateComment();
			IField[] getterFields, setterFields, getterSetterFields;
			if (fSort) {
				getterFields= getGetterFields(result, keySet);
				setterFields= getSetterFields(result, keySet);
				getterSetterFields= new IField[0];
			} else {
				getterFields= getGetterOnlyFields(result, keySet);
				setterFields= getSetterOnlyFields(result, keySet);
				getterSetterFields= getGetterSetterFields(result, keySet);
			}
			generate(type, getterFields, setterFields, getterSetterFields, new RefactoringASTParser(AST.JLS3).parse(type.getJavaScriptUnit(), true), dialog.getElementPosition());
		}
		notifyResult(dialogResult == Window.OK);
	}

	private static class AddGetterSetterSelectionStatusValidator implements ISelectionStatusValidator {

		private static int fEntries;

		AddGetterSetterSelectionStatusValidator(int entries) {
			fEntries= entries;
		}

		public IStatus validate(Object[] selection) {
			// https://bugs.eclipse.org/bugs/show_bug.cgi?id=38478
			HashSet map= null;
			if ((selection != null) && (selection.length > 1)) {
				map= new HashSet(selection.length);
			}

			int count= 0;
			for (int i= 0; i < selection.length; i++) {
				try {
					if (selection[i] instanceof GetterSetterEntry) {
						Object key= selection[i];
						IField getsetField= ((GetterSetterEntry) selection[i]).field;
						if (((GetterSetterEntry) selection[i]).isGetter) {
							if (!map.add(GetterSetterUtil.getGetterName(getsetField, null)))
								return new StatusInfo(IStatus.WARNING, ActionMessages.AddGetterSetterAction_error_duplicate_methods); 
						} else {
							key= createSignatureKey(GetterSetterUtil.getSetterName(getsetField, null), getsetField);
							if (!map.add(key))
								return new StatusInfo(IStatus.WARNING, ActionMessages.AddGetterSetterAction_error_duplicate_methods); 
						}
						count++;
					}
				} catch (JavaScriptModelException e) {
				}
			}

			if (count == 0)
				return new StatusInfo(IStatus.ERROR, ""); //$NON-NLS-1$
			String message= Messages.format(ActionMessages.AddGetterSetterAction_methods_selected, 
					new Object[] { String.valueOf(count), String.valueOf(fEntries)});
			return new StatusInfo(IStatus.INFO, message);
		}
	}

	/**
	 * Creates a key used in hash maps for a method signature
	 * (gettersettername+arguments(fqn)).
	 */
	private static String createSignatureKey(String methodName, IField field) throws JavaScriptModelException {
		StringBuffer buffer= new StringBuffer();
		buffer.append(methodName);
		String fieldType= field.getTypeSignature();
		String signature= Signature.getSimpleName(Signature.toString(fieldType));
		buffer.append("#"); //$NON-NLS-1$
		buffer.append(signature);

		return buffer.toString();
	}

	private static ISelectionStatusValidator createValidator(int entries) {
		AddGetterSetterSelectionStatusValidator validator= new AddGetterSetterSelectionStatusValidator(entries);
		return validator;
	}

	// returns a list of fields with setter entries checked
	private static IField[] getSetterFields(Object[] result, Set set) {
		List list= new ArrayList(0);
		Object each= null;
		GetterSetterEntry entry= null;
		for (int i= 0; i < result.length; i++) {
			each= result[i];
			if ((each instanceof GetterSetterEntry)) {
				entry= (GetterSetterEntry) each;
				if (!entry.isGetter) {
					list.add(entry.field);
				}
			}
		}
		list= reorderFields(list, set);
		return (IField[]) list.toArray(new IField[list.size()]);
	}

	// returns a list of fields with getter entries checked
	private static IField[] getGetterFields(Object[] result, Set set) {
		List list= new ArrayList(0);
		Object each= null;
		GetterSetterEntry entry= null;
		for (int i= 0; i < result.length; i++) {
			each= result[i];
			if ((each instanceof GetterSetterEntry)) {
				entry= (GetterSetterEntry) each;
				if (entry.isGetter) {
					list.add(entry.field);
				}
			}
		}
		list= reorderFields(list, set);
		return (IField[]) list.toArray(new IField[list.size()]);
	}

	// returns a list of fields with only getter entries checked
	private static IField[] getGetterOnlyFields(Object[] result, Set set) {
		List list= new ArrayList(0);
		Object each= null;
		GetterSetterEntry entry= null;
		boolean getterSet= false;
		for (int i= 0; i < result.length; i++) {
			each= result[i];
			if ((each instanceof GetterSetterEntry)) {
				entry= (GetterSetterEntry) each;
				if (entry.isGetter) {
					list.add(entry.field);
					getterSet= true;
				}
				if ((!entry.isGetter) && (getterSet == true)) {
					list.remove(entry.field);
					getterSet= false;
				}
			} else
				getterSet= false;
		}
		list= reorderFields(list, set);
		return (IField[]) list.toArray(new IField[list.size()]);
	}

	// returns a list of fields with only setter entries checked
	private static IField[] getSetterOnlyFields(Object[] result, Set set) {
		List list= new ArrayList(0);
		Object each= null;
		GetterSetterEntry entry= null;
		boolean getterSet= false;
		for (int i= 0; i < result.length; i++) {
			each= result[i];
			if ((each instanceof GetterSetterEntry)) {
				entry= (GetterSetterEntry) each;
				if (entry.isGetter) {
					getterSet= true;
				}
				if ((!entry.isGetter) && (getterSet != true)) {
					list.add(entry.field);
					getterSet= false;
				}
			} else
				getterSet= false;
		}
		list= reorderFields(list, set);
		return (IField[]) list.toArray(new IField[list.size()]);
	}

	// returns a list of fields with both entries checked
	private static IField[] getGetterSetterFields(Object[] result, Set set) {
		List list= new ArrayList(0);
		Object each= null;
		GetterSetterEntry entry= null;
		boolean getterSet= false;
		for (int i= 0; i < result.length; i++) {
			each= result[i];
			if ((each instanceof GetterSetterEntry)) {
				entry= (GetterSetterEntry) each;
				if (entry.isGetter) {
					getterSet= true;
				}
				if ((!entry.isGetter) && (getterSet == true)) {
					list.add(entry.field);
					getterSet= false;
				}
			} else
				getterSet= false;
		}
		list= reorderFields(list, set);
		return (IField[]) list.toArray(new IField[list.size()]);
	}

    private static List reorderFields(List collection, Set set) {
    	final List list= new ArrayList(collection.size());
    	for (final Iterator iterator= set.iterator(); iterator.hasNext();) {
	        final IField field= (IField) iterator.next();
	        if (collection.contains(field))
	        	list.add(field);
        }
    	return list;
    }

	private void generate(IType type, IField[] getterFields, IField[] setterFields, IField[] getterSetterFields, JavaScriptUnit unit, IJavaScriptElement elementPosition) throws CoreException {
		if (getterFields.length == 0 && setterFields.length == 0 && getterSetterFields.length == 0)
			return;

		IJavaScriptUnit cu= null;
		if (getterFields.length != 0)
			cu= getterFields[0].getJavaScriptUnit();
		else if (setterFields.length != 0)
			cu= setterFields[0].getJavaScriptUnit();
		else
			cu= getterSetterFields[0].getJavaScriptUnit();
		// open the editor, forces the creation of a working copy
		run(cu, type, getterFields, setterFields, getterSetterFields, JavaScriptUI.openInEditor(cu), unit, elementPosition);
	}

	// ---- JavaScript Editor --------------------------------------------------------------

	/*
	 * (non-Javadoc) Method declared on SelectionDispatchAction
	 */
	public void selectionChanged(ITextSelection selection) {
	}

	/*
	 * (non-Javadoc) Method declared on SelectionDispatchAction
	 */
	public void run(ITextSelection selection) {
		try {
			if (!ActionUtil.isProcessable(fEditor)) {
				notifyResult(false);
				return;
			}

			IJavaScriptElement[] elements= SelectionConverter.codeResolveForked(fEditor, true);
			if (elements.length == 1 && (elements[0] instanceof IField)) {
				IField field= (IField) elements[0];
				run(field.getDeclaringType(), new IField[] { field}, true);
				return;
			}
			IJavaScriptElement element= SelectionConverter.getElementAtOffset(fEditor);

			if (element != null) {
				IType type= (IType) element.getAncestor(IJavaScriptElement.TYPE);
				if (type != null) {
					if (type.getFields().length > 0) {
						run(type, new IField[0], true);
						return;
					}
				}
			}
			MessageDialog.openInformation(getShell(), DIALOG_TITLE, ActionMessages.AddGetterSetterAction_not_applicable); 
		} catch (CoreException e) {
			ExceptionHandler.handle(e, getShell(), DIALOG_TITLE, ActionMessages.AddGetterSetterAction_error_actionfailed); 
		} catch (InvocationTargetException e) {
			ExceptionHandler.handle(e, getShell(), DIALOG_TITLE, ActionMessages.AddGetterSetterAction_error_actionfailed); 
		} catch (InterruptedException e) {
			// cancelled
		}
	}

	// ---- Helpers -------------------------------------------------------------------

	private void run(IJavaScriptUnit cu, IType type, IField[] getterFields, IField[] setterFields, IField[] getterSetterFields, IEditorPart editor, JavaScriptUnit unit, IJavaScriptElement elementPosition) {
		IRewriteTarget target= (IRewriteTarget) editor.getAdapter(IRewriteTarget.class);
		if (target != null) {
			target.beginCompoundChange();
		}
		try {
			CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings(cu.getJavaScriptProject());
			settings.createComments= fGenerateComment;

			AddGetterSetterOperation op= new AddGetterSetterOperation(type, getterFields, setterFields, getterSetterFields, unit, skipReplaceQuery(), elementPosition, settings, true, false);
			setOperationStatusFields(op);

			IRunnableContext context= JavaScriptPlugin.getActiveWorkbenchWindow();
			if (context == null) {
				context= new BusyIndicatorRunnableContext();
			}

			PlatformUI.getWorkbench().getProgressService().runInUI(context, new WorkbenchRunnableAdapter(op, op.getSchedulingRule()), op.getSchedulingRule());

		} catch (InvocationTargetException e) {
			String message= ActionMessages.AddGetterSetterAction_error_actionfailed; 
			ExceptionHandler.handle(e, getShell(), DIALOG_TITLE, message);
		} catch (InterruptedException e) {
			// operation canceled
		} finally {
			if (target != null) {
				target.endCompoundChange();
			}
		}
	}

	private void setOperationStatusFields(AddGetterSetterOperation op) {
		// Set the status fields corresponding to the visibility and modifiers set
		int flags= fVisibility;

		op.setSort(fSort);
		op.setVisibility(flags);
	}

	private IRequestQuery skipReplaceQuery() {
		return new IRequestQuery() {

			public int doQuery(IMember method) {
				int[] returnCodes= { IRequestQuery.YES, IRequestQuery.NO, IRequestQuery.YES_ALL, IRequestQuery.CANCEL};
				String skipLabel= ActionMessages.AddGetterSetterAction_SkipExistingDialog_skip_label; 
				String replaceLabel= ActionMessages.AddGetterSetterAction_SkipExistingDialog_replace_label; 
				String skipAllLabel= ActionMessages.AddGetterSetterAction_SkipExistingDialog_skipAll_label; 
				String[] options= { skipLabel, replaceLabel, skipAllLabel, IDialogConstants.CANCEL_LABEL};
				String methodName= JavaScriptElementLabels.getElementLabel(method, JavaScriptElementLabels.M_PARAMETER_TYPES);
				String formattedMessage= Messages.format(ActionMessages.AddGetterSetterAction_SkipExistingDialog_message, methodName); 
				return showQueryDialog(formattedMessage, options, returnCodes);
			}
		};
	}

	private int showQueryDialog(final String message, final String[] buttonLabels, int[] returnCodes) {
		final Shell shell= getShell();
		if (shell == null) {
			JavaScriptPlugin.logErrorMessage("AddGetterSetterAction.showQueryDialog: No active shell found"); //$NON-NLS-1$
			return IRequestQuery.CANCEL;
		}
		final int[] result= { Window.CANCEL};
		shell.getDisplay().syncExec(new Runnable() {

			public void run() {
				String title= ActionMessages.AddGetterSetterAction_QueryDialog_title; 
				MessageDialog dialog= new MessageDialog(shell, title, null, message, MessageDialog.QUESTION, buttonLabels, 0);
				result[0]= dialog.open();
			}
		});
		int returnVal= result[0];
		return returnVal < 0 ? IRequestQuery.CANCEL : returnCodes[returnVal];
	}

	/*
	 * Returns fields in the selection or <code>null</code> if the selection is empty or
	 * not valid.
	 */
	private IField[] getSelectedFields(IStructuredSelection selection) {
		List elements= selection.toList();
		int nElements= elements.size();
		if (nElements > 0) {
			IField[] res= new IField[nElements];
			IJavaScriptUnit cu= null;
			for (int i= 0; i < nElements; i++) {
				Object curr= elements.get(i);
				if (curr instanceof IField) {
					IField fld= (IField) curr;

					if (i == 0) {
						// remember the cu of the first element
						cu= fld.getJavaScriptUnit();
						if (cu == null) {
							return null;
						}
					} else if (!cu.equals(fld.getJavaScriptUnit())) {
						// all fields must be in the same CU
						return null;
					}
					
					final IType declaringType= fld.getDeclaringType();
					if (declaringType==null)
						return null;
					
					res[i]= fld;
				} else {
					return null;
				}
			}
			return res;
		}
		return null;
	}

	private static class AddGetterSetterLabelProvider extends JavaScriptElementLabelProvider {

		AddGetterSetterLabelProvider() {
		}

		/*
		 * @see ILabelProvider#getText(Object)
		 */
		public String getText(Object element) {
			if (element instanceof GetterSetterEntry) {
				GetterSetterEntry entry= (GetterSetterEntry) element;
				try {
					if (entry.isGetter) {
						return GetterSetterUtil.getGetterName(entry.field, null) + "()"; //$NON-NLS-1$ 
					} else {
						return GetterSetterUtil.getSetterName(entry.field, null) + '(' + Signature.getSimpleName(Signature.toString(entry.field.getTypeSignature())) + ')';
					}
				} catch (JavaScriptModelException e) {
					return ""; //$NON-NLS-1$
				}
			}
			return super.getText(element);
		}

		/*
		 * @see ILabelProvider#getImage(Object)
		 */
		public Image getImage(Object element) {
			if (element instanceof GetterSetterEntry) {
				int flags= 0;
				try {
					flags= ((GetterSetterEntry) element).field.getFlags();
				} catch (JavaScriptModelException e) {
					JavaScriptPlugin.log(e);
				}
				ImageDescriptor desc= JavaElementImageProvider.getFieldImageDescriptor(false, Flags.AccPublic);
				int adornmentFlags= Flags.isStatic(flags) ? JavaScriptElementImageDescriptor.STATIC : 0;
				desc= new JavaScriptElementImageDescriptor(desc, adornmentFlags, JavaElementImageProvider.BIG_SIZE);
				return JavaScriptPlugin.getImageDescriptorRegistry().get(desc);
			}
			return super.getImage(element);
		}
	}

	/**
	 * @return map IField -> GetterSetterEntry[]
	 */
	private Map createGetterSetterMapping(IType type) throws JavaScriptModelException {
		IField[] fields= type.getFields();
		Map result= new LinkedHashMap();
		for (int i= 0; i < fields.length; i++) {
			IField field= fields[i];
			
			List l= new ArrayList(2);
			if (GetterSetterUtil.getGetter(field) == null) {
				l.add(new GetterSetterEntry(field, true, false));
				incNumEntries();
			}

			if (GetterSetterUtil.getSetter(field) == null) {
				l.add(new GetterSetterEntry(field, false, false));
				incNumEntries();
			}

			if (!l.isEmpty())
				result.put(field, l.toArray(new GetterSetterEntry[l.size()]));
		
		}
		return result;
	}

	private static class AddGetterSetterContentProvider implements ITreeContentProvider {

		private static final Object[] EMPTY= new Object[0];

		//private Viewer fViewer;

		private Map fGetterSetterEntries;

		public AddGetterSetterContentProvider(Map entries) {
			fGetterSetterEntries= entries;
		}

		/*
		 * @see IContentProvider#inputChanged(Viewer, Object, Object)
		 */
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			//fViewer= viewer;
		}

//		public Viewer getViewer() {
//			return fViewer;
//		}
			
		/*
		 * @see ITreeContentProvider#getChildren(Object)
		 */
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof IField)
				return (Object[]) fGetterSetterEntries.get(parentElement);
			return EMPTY;
		}

		/*
		 * @see ITreeContentProvider#getParent(Object)
		 */
		public Object getParent(Object element) {
			if (element instanceof IMember)
			{ IMember member=(IMember) element;
				return  member.getDeclaringType()!=null ? 
						member.getDeclaringType() :(Object)member.getJavaScriptUnit();
			}
			if (element instanceof GetterSetterEntry)
				return ((GetterSetterEntry) element).field;
			return null;
		}

		/*
		 * @see ITreeContentProvider#hasChildren(Object)
		 */
		public boolean hasChildren(Object element) {
			return getChildren(element).length > 0;
		}

		/*
		 * @see IStructuredContentProvider#getElements(Object)
		 */
		public Object[] getElements(Object inputElement) {
			return fGetterSetterEntries.keySet().toArray();
		}

		/*
		 * @see IContentProvider#dispose()
		 */
		public void dispose() {
			fGetterSetterEntries.clear();
			fGetterSetterEntries= null;
		}
	}
	
	private static class SettersForFinalFieldsFilter extends ViewerFilter {
		
		private final AddGetterSetterContentProvider fContentProvider;

		public SettersForFinalFieldsFilter(AddGetterSetterContentProvider contentProvider) {
			fContentProvider= contentProvider;
		}

		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (element instanceof GetterSetterEntry) {
				GetterSetterEntry getterSetterEntry= (GetterSetterEntry) element;
				return getterSetterEntry.isGetter || !getterSetterEntry.isFinal;
			} else if (element instanceof IField) {
				Object[] children= fContentProvider.getChildren(element);
				for (int i= 0; i < children.length; i++) {
					GetterSetterEntry curr= (GetterSetterEntry) children[i];
					if (curr.isGetter || !curr.isFinal) {
						return true;
					}
				}
				return false;
			}
			return true;
		}
	}
	

	private static class GetterSetterTreeSelectionDialog extends SourceActionDialog {

		private AddGetterSetterContentProvider fContentProvider;

		private static final int SELECT_GETTERS_ID= IDialogConstants.CLIENT_ID + 1;
		private static final int SELECT_SETTERS_ID= IDialogConstants.CLIENT_ID + 2;
		private final String SETTINGS_SECTION= "AddGetterSetterDialog"; //$NON-NLS-1$
		private final String SORT_ORDER= "SortOrdering"; //$NON-NLS-1$
		private final String ALLOW_SETTERS_FOR_FINALS= "RemoveFinal"; //$NON-NLS-1$
		
		private IDialogSettings fSettings;
		private SettersForFinalFieldsFilter fSettersForFinalFieldsFilter;

		private boolean fSortOrder;
		private boolean fAllowSettersForFinals;
		
		private ArrayList fPreviousSelectedFinals;


		public GetterSetterTreeSelectionDialog(Shell parent, ILabelProvider labelProvider, AddGetterSetterContentProvider contentProvider, CompilationUnitEditor editor, IType type) throws JavaScriptModelException {
			super(parent, labelProvider, contentProvider, editor, type, false);
			fContentProvider= contentProvider;
			fPreviousSelectedFinals= new ArrayList();

			// http://bugs.eclipse.org/bugs/show_bug.cgi?id=19253
			IDialogSettings dialogSettings= JavaScriptPlugin.getDefault().getDialogSettings();
			fSettings= dialogSettings.getSection(SETTINGS_SECTION);
			if (fSettings == null) {
				fSettings= dialogSettings.addNewSection(SETTINGS_SECTION);
				fSettings.put(SORT_ORDER, false); 
				fSettings.put(ALLOW_SETTERS_FOR_FINALS, false); 
			}

			fSortOrder= fSettings.getBoolean(SORT_ORDER);
			fAllowSettersForFinals= fSettings.getBoolean(ALLOW_SETTERS_FOR_FINALS);
			
			fSettersForFinalFieldsFilter= new SettersForFinalFieldsFilter(contentProvider);
		}

		public boolean getSortOrder() {
			return fSortOrder;
		}

		public void setSortOrder(boolean sort) {
			if (fSortOrder != sort) {
				fSortOrder= sort;
				fSettings.put(SORT_ORDER, sort);
				if (getTreeViewer() != null) {
					getTreeViewer().refresh();
				}
			}
		}
		
		private boolean allowSettersForFinals() {
			return fAllowSettersForFinals;
		}
		
		public void allowSettersForFinals(boolean allowSettersForFinals) {
			if (fAllowSettersForFinals != allowSettersForFinals) {
				fAllowSettersForFinals= allowSettersForFinals;
				fSettings.put(ALLOW_SETTERS_FOR_FINALS, allowSettersForFinals);
				CheckboxTreeViewer treeViewer= getTreeViewer();
				if (treeViewer != null) {
					ArrayList newChecked= new ArrayList();
					if (allowSettersForFinals) {
						newChecked.addAll(fPreviousSelectedFinals);
					}
					fPreviousSelectedFinals.clear();
					Object[] checkedElements= treeViewer.getCheckedElements();
					for (int i= 0; i < checkedElements.length; i++) {
						if (checkedElements[i] instanceof GetterSetterEntry) {
							GetterSetterEntry entry= (GetterSetterEntry) checkedElements[i];
							if (allowSettersForFinals || entry.isGetter || !entry.isFinal) {
								newChecked.add(entry);
							} else {
								fPreviousSelectedFinals.add(entry);
							}
						}
					}
					if (allowSettersForFinals) {
						treeViewer.removeFilter(fSettersForFinalFieldsFilter);
					} else {
						treeViewer.addFilter(fSettersForFinalFieldsFilter);
					}
					treeViewer.setCheckedElements(newChecked.toArray());
				}
				updateOKStatus();
			}
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.ui.dialogs.CheckedTreeSelectionDialog#createTreeViewer(org.eclipse.swt.widgets.Composite)
		 */
		protected CheckboxTreeViewer createTreeViewer(Composite parent) {
			 CheckboxTreeViewer treeViewer= super.createTreeViewer(parent);
			 if (!fAllowSettersForFinals) {
				 treeViewer.addFilter(fSettersForFinalFieldsFilter);
			 }
			return treeViewer;
		}

		protected void configureShell(Shell shell) {
			super.configureShell(shell);
			PlatformUI.getWorkbench().getHelpSystem().setHelp(shell, IJavaHelpContextIds.ADD_GETTER_SETTER_SELECTION_DIALOG);
		}
		
		private void createGetterSetterButtons(Composite buttonComposite) {
			createButton(buttonComposite, SELECT_GETTERS_ID, ActionMessages.GetterSetterTreeSelectionDialog_select_getters, false); 
			createButton(buttonComposite, SELECT_SETTERS_ID, ActionMessages.GetterSetterTreeSelectionDialog_select_setters, false); 
		}

		protected void buttonPressed(int buttonId) {
			super.buttonPressed(buttonId);
			switch (buttonId) {
				case SELECT_GETTERS_ID: {
					getTreeViewer().setCheckedElements(getGetterSetterElements(true));
					updateOKStatus();
					break;
				}
				case SELECT_SETTERS_ID: {
					getTreeViewer().setCheckedElements(getGetterSetterElements(false));
					updateOKStatus();
					break;
				}
			}
		}

		protected Composite createInsertPositionCombo(Composite composite) {
			Button addRemoveFinalCheckbox= addAllowSettersForFinalslCheckbox(composite);
			addRemoveFinalCheckbox.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			
			Composite entryComposite= super.createInsertPositionCombo(composite);
			addSortOrder(entryComposite);
			addVisibilityAndModifiersChoices(entryComposite);
			return entryComposite;
		}

		private Button addAllowSettersForFinalslCheckbox(Composite entryComposite) {
			Button allowSettersForFinalsButton= new Button(entryComposite, SWT.CHECK);
			allowSettersForFinalsButton.setText(ActionMessages.AddGetterSetterAction_allow_setters_for_finals_description); 

			allowSettersForFinalsButton.addSelectionListener(new SelectionListener() {
				public void widgetSelected(SelectionEvent e) {
					boolean isSelected= (((Button) e.widget).getSelection());
					allowSettersForFinals(isSelected);
				}

				public void widgetDefaultSelected(SelectionEvent e) {
					widgetSelected(e);
				}
			});
			allowSettersForFinalsButton.setSelection(allowSettersForFinals());
			return allowSettersForFinalsButton;
		}

		private Composite addSortOrder(Composite composite) {
			Label label= new Label(composite, SWT.NONE);
			label.setText(ActionMessages.GetterSetterTreeSelectionDialog_sort_label); 
			GridData gd= new GridData(GridData.FILL_BOTH);
			label.setLayoutData(gd);

			final Combo combo= new Combo(composite, SWT.READ_ONLY);
			combo.setItems(new String[] { ActionMessages.GetterSetterTreeSelectionDialog_alpha_pair_sort, 
					ActionMessages.GetterSetterTreeSelectionDialog_alpha_method_sort}); 
			final int methodIndex= 1; // Hard-coded. Change this if the
														// list gets more complicated.
			// http://bugs.eclipse.org/bugs/show_bug.cgi?id=38400
			int sort= getSortOrder() ? 1 : 0;
			combo.setText(combo.getItem(sort));
			gd= new GridData(GridData.FILL_BOTH);
			combo.setLayoutData(gd);
			combo.addSelectionListener(new SelectionAdapter() {

				public void widgetSelected(SelectionEvent e) {
					setSortOrder(combo.getSelectionIndex() == methodIndex);
				}
			});
			return composite;
		}

		private Object[] getGetterSetterElements(boolean isGetter) {
			Object[] allFields= fContentProvider.getElements(null);
			Set result= new HashSet();
			for (int i= 0; i < allFields.length; i++) {
				IField field= (IField) allFields[i];
				GetterSetterEntry[] entries= getEntries(field);
				for (int j= 0; j < entries.length; j++) {
					AddGetterSetterAction.GetterSetterEntry entry= entries[j];
					if (entry.isGetter == isGetter)
						result.add(entry);
				}
			}
			return result.toArray();
		}

		private GetterSetterEntry[] getEntries(IField field) {
			List result= Arrays.asList(fContentProvider.getChildren(field));
			return (GetterSetterEntry[]) result.toArray(new GetterSetterEntry[result.size()]);
		}

		protected Composite createSelectionButtons(Composite composite) {
			Composite buttonComposite= super.createSelectionButtons(composite);

			GridLayout layout= new GridLayout();
			buttonComposite.setLayout(layout);

			createGetterSetterButtons(buttonComposite);

			layout.marginHeight= 0;
			layout.marginWidth= 0;
			layout.numColumns= 1;

			return buttonComposite;
		}

		/*
		 * @see org.eclipse.wst.jsdt.internal.ui.dialogs.SourceActionDialog#createLinkControl(org.eclipse.swt.widgets.Composite)
		 */
		protected Control createLinkControl(Composite composite) {
			Link link= new Link(composite, SWT.WRAP);
			link.setText(ActionMessages.AddGetterSetterAction_template_link_description); 
			link.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					openCodeTempatePage(CodeTemplateContextType.GETTERCOMMENT_ID);
				}
			});
			link.setToolTipText(ActionMessages.AddGetterSetterAction_template_link_tooltip); 
			
			GridData gridData= new GridData(SWT.FILL, SWT.BEGINNING, true, false);
			gridData.widthHint= convertWidthInCharsToPixels(40); // only expand further if anyone else requires it
			link.setLayoutData(gridData);
			return link;
		}
	}

	private static class GetterSetterEntry {
		public final IField field;
		public final boolean isGetter;
		public final boolean isFinal;

		GetterSetterEntry(IField field, boolean isGetterEntry, boolean isFinal) {
			this.field= field;
			this.isGetter= isGetterEntry;
			this.isFinal= isFinal;
		}
	}
}
