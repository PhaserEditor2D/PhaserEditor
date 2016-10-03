/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Martin Moebius (m.moebius@gmx.de) - initial API and implementation
 *             (report 28793)
 *   IBM Corporation - updates
 *******************************************************************************/

package org.eclipse.wst.jsdt.ui.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.text.IRewriteTarget;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.wst.jsdt.core.IField;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.Signature;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.IBinding;
import org.eclipse.wst.jsdt.core.dom.IFunctionBinding;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.IVariableBinding;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.AddDelegateMethodsOperation;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.StubUtility2;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.corext.dom.Bindings;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.JavaElementUtil;
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
import org.eclipse.wst.jsdt.internal.ui.viewsupport.BindingLabelProvider;
import org.eclipse.wst.jsdt.ui.JavaScriptUI;

import com.ibm.icu.text.Collator;

/**
 * Creates delegate methods for a type's fields. Opens a dialog with a list of fields for
 * which delegate methods can be generated. User is able to check or uncheck items before
 * methods are generated.
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
public class AddDelegateMethodsAction extends SelectionDispatchAction {

	// ---- Helpers -------------------------------------------------------------------

	private static class AddDelegateMethodsActionStatusValidator implements ISelectionStatusValidator {

		private static int fEntries;

		AddDelegateMethodsActionStatusValidator(int entries) {
			fEntries= entries;
		}

		public IStatus validate(Object[] selection) {
			StatusInfo info= new StatusInfo();
			if (selection != null && selection.length > 0) {
				int count= 0;
				List bindings= new ArrayList(selection.length);
				IFunctionBinding binding= null;
				for (int index= 0; index < selection.length; index++) {
					if (selection[index] instanceof IBinding[]) {
						count++;
						binding= (IFunctionBinding) ((IBinding[]) selection[index])[1];
						IFunctionBinding existing= null;
						for (int offset= 0; offset < bindings.size(); offset++) {
							existing= (IFunctionBinding) bindings.get(offset);
							if (Bindings.isEqualMethod(binding, existing.getName(), existing.getParameterTypes())) {
								return new StatusInfo(IStatus.ERROR, ActionMessages.AddDelegateMethodsAction_duplicate_methods); 
							}
						}
						bindings.add(binding);
						info= new StatusInfo(IStatus.INFO, Messages.format(ActionMessages.AddDelegateMethodsAction_selectioninfo_more, new Object[] { String.valueOf(count), String.valueOf(fEntries)})); 
					}
				}
			}
			return info;
		}
	}

	private static class AddDelegateMethodsContentProvider implements ITreeContentProvider {

		private IBinding[][] fBindings= new IBinding[0][0];

		private int fCount= 0;

		private IVariableBinding[] fExpanded= new IVariableBinding[0];

		private final JavaScriptUnit fUnit;

		AddDelegateMethodsContentProvider(IType type, IField[] fields) throws JavaScriptModelException {
			RefactoringASTParser parser= new RefactoringASTParser(AST.JLS3);
			fUnit= parser.parse(type.getJavaScriptUnit(), true);
			final ITypeBinding binding= ASTNodes.getTypeBinding(fUnit, type);
			if (binding != null) {
				IBinding[][] bindings= StubUtility2.getDelegatableMethods(fUnit.getAST(), binding);
				if (bindings != null) {
					fBindings= bindings;
					fCount= bindings.length;
				}
				List expanded= new ArrayList();
				for (int index= 0; index < fields.length; index++) {
					VariableDeclarationFragment fragment= ASTNodeSearchUtil.getFieldDeclarationFragmentNode(fields[index], fUnit);
					if (fragment != null) {
						IVariableBinding variableBinding= fragment.resolveBinding();
						if (variableBinding != null)
							expanded.add(variableBinding);
					}
				}
				IVariableBinding[] result= new IVariableBinding[expanded.size()];
				expanded.toArray(result);
				fExpanded= result;
			}
		}

		public JavaScriptUnit getCompilationUnit() {
			return fUnit;
		}

		public void dispose() {
		}

		public Object[] getChildren(Object element) {
			if (element instanceof IVariableBinding) {
				IVariableBinding binding= (IVariableBinding) element;
				List result= new ArrayList();
				final String key= binding.getKey();
				for (int index= 0; index < fBindings.length; index++)
					if (fBindings[index][0].getKey().equals(key))
						result.add(fBindings[index]);
				return result.toArray();
			}
			return null;
		}

		public int getCount() {
			return fCount;
		}

		public Object[] getElements(Object inputElement) {
			Set keys= new HashSet();
			List result= new ArrayList();
			for (int index= 0; index < fBindings.length; index++) {
				IBinding[] bindings= fBindings[index];
				final String key= bindings[0].getKey();
				if (!keys.contains(key)) {
					keys.add(key);
					result.add(bindings[0]);
				}
			}
			return result.toArray();
		}

		public IVariableBinding[] getExpandedElements() {
			return fExpanded;
		}

		public IBinding[][] getInitiallySelectedElements() {
			List result= new ArrayList();
			for (int index= 0; index < fBindings.length; index++)
				for (int offset= 0; offset < fExpanded.length; offset++)
					if (fExpanded[offset].getKey().equals(fBindings[index][0].getKey()))
						result.add(fBindings[index]);
			return (IBinding[][]) result.toArray(new IBinding[result.size()][2]);
		}

		public Object getParent(Object element) {
			if (element instanceof IBinding[])
				return ((IBinding[]) element)[0];
			return null;
		}

		public boolean hasChildren(Object element) {
			return element instanceof IVariableBinding;
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
	}

	private static class AddDelegateMethodsDialog extends SourceActionDialog {

		public AddDelegateMethodsDialog(Shell parent, ILabelProvider labelProvider, ITreeContentProvider contentProvider, CompilationUnitEditor editor, IType type, boolean isConstructor) throws JavaScriptModelException {
			super(parent, labelProvider, contentProvider, editor, type, isConstructor);
		}

		protected void configureShell(Shell shell) {
			super.configureShell(shell);
			PlatformUI.getWorkbench().getHelpSystem().setHelp(shell, IJavaHelpContextIds.ADD_DELEGATE_METHODS_SELECTION_DIALOG);
		}
		
		/*
		 * @see org.eclipse.wst.jsdt.internal.ui.dialogs.SourceActionDialog#createLinkControl(org.eclipse.swt.widgets.Composite)
		 */
		protected Control createLinkControl(Composite composite) {
			Link link= new Link(composite, SWT.WRAP);
			link.setText(ActionMessages.AddDelegateMethodsAction_template_link_message); 
			link.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					openCodeTempatePage(CodeTemplateContextType.OVERRIDECOMMENT_ID);
				}
			});
			link.setToolTipText(ActionMessages.AddDelegateMethodsAction_template_link_tooltip); 
			
			GridData gridData= new GridData(SWT.FILL, SWT.BEGINNING, true, false);
			gridData.widthHint= convertWidthInCharsToPixels(40); // only expand further if anyone else requires it
			link.setLayoutData(gridData);
			return link;
		}
	}

	private static class AddDelegateMethodsLabelProvider extends BindingLabelProvider {

		public Image getImage(Object element) {
			if (element instanceof IBinding[]) {
				return super.getImage((((IBinding[]) element)[1]));
			} else if (element instanceof IVariableBinding) {
				return super.getImage(element);
			}
			return null;
		}

		public String getText(Object element) {
			if (element instanceof IBinding[]) {
				return super.getText((((IBinding[]) element)[1]));
			} else if (element instanceof IVariableBinding) {
				return super.getText(element);
			}
			return null;
		}
	}

	private static class AddDelegateMethodsViewerComparator extends ViewerComparator {

		private final BindingLabelProvider fProvider= new BindingLabelProvider();
		private final Collator fCollator= Collator.getInstance();

		public int category(Object element) {
			if (element instanceof IBinding[])
				return 0;
			return 1;
		}

		public int compare(Viewer viewer, Object object1, Object object2) {
			String first= ""; //$NON-NLS-1$
			String second= ""; //$NON-NLS-1$
			if (object1 instanceof IBinding[])
				first= fProvider.getText(((IBinding[]) object1)[1]);
			else if (object1 instanceof IVariableBinding)
				first= ((IBinding) object1).getName();
			if (object2 instanceof IBinding[])
				second= fProvider.getText(((IBinding[]) object2)[1]);
			else if (object2 instanceof IVariableBinding)
				second= ((IBinding) object2).getName();
			return fCollator.compare(first, second);
		}
	}

	private static final String DIALOG_TITLE= ActionMessages.AddDelegateMethodsAction_error_title; 

	private static boolean hasPrimitiveType(IField field) throws JavaScriptModelException {
		String signature= field.getTypeSignature();
		char first= Signature.getElementType(signature).charAt(0);
		return (first != Signature.C_RESOLVED && first != Signature.C_UNRESOLVED);
	}

	private static boolean isArray(IField field) throws JavaScriptModelException {
		return Signature.getArrayCount(field.getTypeSignature()) > 0;
	}

	private CompilationUnitEditor fEditor;

	/**
	 * Note: This constructor is for internal use only. Clients should not call this
	 * constructor.
	 * 
	 * @param editor the compilation unit editor
	 */
	public AddDelegateMethodsAction(CompilationUnitEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(SelectionConverter.getInputAsCompilationUnit(editor) != null);
	}

	/**
	 * Creates a new <code>AddDelegateMethodsAction</code>. The action requires that
	 * the selection provided by the site's selection provider is of type <code>
	 * org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param site the site providing context information for this action
	 */
	public AddDelegateMethodsAction(IWorkbenchSite site) {
		super(site);
		setText(ActionMessages.AddDelegateMethodsAction_label); 
		setDescription(ActionMessages.AddDelegateMethodsAction_description); 
		setToolTipText(ActionMessages.AddDelegateMethodsAction_tooltip); 

		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.ADD_DELEGATE_METHODS_ACTION);
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
		if (fields == null || fields.length == 0)
			return false;
		int count= 0;
		for (int index= 0; index < fields.length; index++) {
			if (!hasPrimitiveType(fields[index]) || isArray(fields[index]))
				count++;
		}
		if (count == 0)
			MessageDialog.openInformation(getShell(), DIALOG_TITLE, ActionMessages.AddDelegateMethodsAction_not_applicable); 
		return (count > 0);
	}

	private boolean canRunOn(IType type) throws JavaScriptModelException {
		if (type == null || type.getJavaScriptUnit() == null) {
			MessageDialog.openInformation(getShell(), DIALOG_TITLE, ActionMessages.AddDelegateMethodsAction_not_in_source_file); 
			return false;
		}
		return canRunOn(type.getFields());
	}

	private IField[] getSelectedFields(IStructuredSelection selection) {
		List elements= selection.toList();
		if (elements.size() > 0) {
			IField[] result= new IField[elements.size()];
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

					result[index]= field;
				} else {
					return null;
				}
			}
			return result;
		}
		return null;
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
			else if (firstElement instanceof IJavaScriptUnit)
				run(JavaElementUtil.getMainType((IJavaScriptUnit) firstElement), new IField[0], false);
			else if (!(firstElement instanceof IField))
				MessageDialog.openInformation(getShell(), DIALOG_TITLE, ActionMessages.AddDelegateMethodsAction_not_applicable); 
		} catch (CoreException e) {
			ExceptionHandler.handle(e, getShell(), DIALOG_TITLE, ActionMessages.AddDelegateMethodsAction_error_actionfailed); 
		}

	}

	/*
	 * (non-Javadoc) Method declared on SelectionDispatchAction
	 */
	public void run(ITextSelection selection) {
		try {
			if (!ActionUtil.isProcessable(fEditor))
				return;

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
			MessageDialog.openInformation(getShell(), DIALOG_TITLE, ActionMessages.AddDelegateMethodsAction_not_applicable); 
		} catch (CoreException e) {
			ExceptionHandler.handle(e, getShell(), DIALOG_TITLE, ActionMessages.AddDelegateMethodsAction_error_actionfailed); 
		} catch (InvocationTargetException e) {
			ExceptionHandler.handle(e, getShell(), DIALOG_TITLE, ActionMessages.AddDelegateMethodsAction_error_actionfailed); 
		} catch (InterruptedException e) {
			// cancelled
		}
	}

	private void run(IType type, IField[] preselected, boolean editor) throws CoreException {
		if (!ElementValidator.check(type, getShell(), DIALOG_TITLE, editor))
			return;
		if (!ActionUtil.isEditable(fEditor, getShell(), type))
			return;
		if (!canRunOn(type))
			return;
		showUI(type, preselected);
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

	// ---- JavaScript Editor --------------------------------------------------------------

	/*
	 * (non-Javadoc) Method declared on SelectionDispatchAction
	 */
	public void selectionChanged(ITextSelection selection) {
	}

	private void showUI(IType type, IField[] fields) {
		try {
			AddDelegateMethodsContentProvider provider= new AddDelegateMethodsContentProvider(type, fields);
			SourceActionDialog dialog= new AddDelegateMethodsDialog(getShell(), new AddDelegateMethodsLabelProvider(), provider, fEditor, type, false);
			dialog.setValidator(new AddDelegateMethodsActionStatusValidator(provider.getCount()));
			AddDelegateMethodsViewerComparator comparator= new AddDelegateMethodsViewerComparator();
			dialog.setComparator(comparator);
			dialog.setInput(new Object());
			dialog.setContainerMode(true);
			dialog.setMessage(ActionMessages.AddDelegateMethodsAction_message); 
			dialog.setTitle(ActionMessages.AddDelegateMethodsAction_title); 
			IVariableBinding[] expanded= provider.getExpandedElements();
			if (expanded.length > 0) {
				dialog.setExpandedElements(expanded);
			} else {
				Object[] elements= provider.getElements(null);
				if (elements.length > 0) {
					comparator.sort(null, elements);
					Object[] expand= { elements[0]};
					dialog.setExpandedElements(expand);
				}
			}
			dialog.setInitialSelections(provider.getInitiallySelectedElements());
			dialog.setSize(60, 18);
			int result= dialog.open();
			if (result == Window.OK) {
				Object[] object= dialog.getResult();
				if (object == null) {
					notifyResult(false);
					return;
				}
				List tuples= new ArrayList(object.length);
				for (int index= 0; index < object.length; index++) {
					if (object[index] instanceof IBinding[])
						tuples.add(object[index]);
				}
				IEditorPart part= JavaScriptUI.openInEditor(type);
				IRewriteTarget target= (IRewriteTarget) part.getAdapter(IRewriteTarget.class);
				try {
					if (target != null)
						target.beginCompoundChange();
					CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings(type.getJavaScriptProject());
					settings.createComments= dialog.getGenerateComment();
					final int size= tuples.size();
					String[] methodKeys= new String[size];
					String[] variableKeys= new String[size];
					for (int index= 0; index < size; index++) {
						final IBinding[] tuple= (IBinding[]) tuples.get(index);
						variableKeys[index]= tuple[0].getKey();
						methodKeys[index]= tuple[1].getKey();
					}
					AddDelegateMethodsOperation operation= new AddDelegateMethodsOperation(type, dialog.getElementPosition(), provider.getCompilationUnit(), variableKeys, methodKeys, settings, true, false);
					IRunnableContext context= JavaScriptPlugin.getActiveWorkbenchWindow();
					if (context == null)
						context= new BusyIndicatorRunnableContext();
					try {
						PlatformUI.getWorkbench().getProgressService().runInUI(context, new WorkbenchRunnableAdapter(operation, operation.getSchedulingRule()), operation.getSchedulingRule());
					} catch (InterruptedException exception) {
						// User interruption
					}
				} finally {
					if (target != null)
						target.endCompoundChange();
				}
			}
			notifyResult(result == Window.OK);
		} catch (CoreException exception) {
			ExceptionHandler.handle(exception, DIALOG_TITLE, ActionMessages.AddDelegateMethodsAction_error_actionfailed); 
		} catch (InvocationTargetException e) {
			ExceptionHandler.handle(e, DIALOG_TITLE, ActionMessages.AddDelegateMethodsAction_error_actionfailed); 
		}
	}
}
