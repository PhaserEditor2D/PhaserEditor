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
package org.eclipse.wst.jsdt.internal.ui.refactoring.actions;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.wst.jsdt.core.IField;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.ILocalVariable;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringAvailabilityTester;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringExecutionStarter;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.actions.ActionUtil;
import org.eclipse.wst.jsdt.internal.ui.actions.SelectionConverter;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.JavaTextSelection;
import org.eclipse.wst.jsdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.wst.jsdt.internal.ui.refactoring.reorg.RenameLinkedMode;
import org.eclipse.wst.jsdt.internal.ui.util.ExceptionHandler;
import org.eclipse.wst.jsdt.ui.PreferenceConstants;
import org.eclipse.wst.jsdt.ui.actions.SelectionDispatchAction;

public class RenameJavaElementAction extends SelectionDispatchAction {

	private JavaEditor fEditor;
	
	public RenameJavaElementAction(IWorkbenchSite site) {
		super(site);
	}
	
	public RenameJavaElementAction(JavaEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(SelectionConverter.canOperateOn(fEditor));
	}

	//---- Structured selection ------------------------------------------------

	public void selectionChanged(IStructuredSelection selection) {
		try {
			if (selection.size() == 1) {
				setEnabled(canEnable(selection));
				return;
			}
		} catch (JavaScriptModelException e) {
			// http://bugs.eclipse.org/bugs/show_bug.cgi?id=19253
			if (JavaModelUtil.isExceptionToBeLogged(e))
				JavaScriptPlugin.log(e);
		} catch (CoreException e) {
			JavaScriptPlugin.log(e);
		}
		setEnabled(false);
	}
	
	private static boolean canEnable(IStructuredSelection selection) throws CoreException {
		IJavaScriptElement element= getJavaElement(selection);
		if (element == null)
			return false;
		return isRenameAvailable(element);
	} 

	private static IJavaScriptElement getJavaElement(IStructuredSelection selection) {
		if (selection.size() != 1)
			return null;
		Object first= selection.getFirstElement();
		if (! (first instanceof IJavaScriptElement))
			return null;
		return (IJavaScriptElement)first;
	}
	
	public void run(IStructuredSelection selection) {
		IJavaScriptElement element= getJavaElement(selection);
		if (element == null)
			return;
		try {
			run(element, false);	
		} catch (CoreException e){
			ExceptionHandler.handle(e, RefactoringMessages.RenameJavaElementAction_name, RefactoringMessages.RenameJavaElementAction_exception);  
		}	
	}
	
	//---- text selection ------------------------------------------------------------

	public void selectionChanged(ITextSelection selection) {
		if (selection instanceof JavaTextSelection) {
			try {
				IJavaScriptElement[] elements= ((JavaTextSelection)selection).resolveElementAtOffset();
				if (elements.length == 1) {
					setEnabled(isRenameAvailable(elements[0]));
				} else {
					setEnabled(false);
				}
			} catch (CoreException e) {
				setEnabled(false);
			}
		} else {
			setEnabled(true);
		}
	}

	public void run(ITextSelection selection) {
		RenameLinkedMode activeLinkedMode= RenameLinkedMode.getActiveLinkedMode();
		if (activeLinkedMode != null) {
			if (activeLinkedMode.isCaretInLinkedPosition()) {
				activeLinkedMode.startFullDialog();
				return;
			} else {
				activeLinkedMode.cancel();
			}
		}
		
		try {
			IJavaScriptElement element= getJavaElementFromEditor();
			if (element != null && isRenameAvailable(element)) {
				IPreferenceStore store= JavaScriptPlugin.getDefault().getPreferenceStore();
				run(element, store.getBoolean(PreferenceConstants.REFACTOR_LIGHTWEIGHT));
				return;
			}
		} catch (CoreException e) {
			ExceptionHandler.handle(e, RefactoringMessages.RenameJavaElementAction_name, RefactoringMessages.RenameJavaElementAction_exception);
		}
		MessageDialog.openInformation(getShell(), RefactoringMessages.RenameJavaElementAction_name, RefactoringMessages.RenameJavaElementAction_not_available);
	}
	
	public boolean canRunInEditor() {
		if (RenameLinkedMode.getActiveLinkedMode() != null)
			return true;
		
		try {
			IJavaScriptElement element= getJavaElementFromEditor();
			if (element == null)
				return false;

			return isRenameAvailable(element);
		} catch (JavaScriptModelException e) {
			if (JavaModelUtil.isExceptionToBeLogged(e))
				JavaScriptPlugin.log(e);
		} catch (CoreException e) {
			JavaScriptPlugin.log(e);
		}
		return false;
	}
	
	private IJavaScriptElement getJavaElementFromEditor() throws JavaScriptModelException {
		IJavaScriptElement[] elements= SelectionConverter.codeResolve(fEditor); 
		if (elements == null || elements.length != 1)
			return null;
		return elements[0];
	}
	
	//---- helper methods -------------------------------------------------------------------

	private void run(IJavaScriptElement element, boolean lightweight) throws CoreException {
		// Work around for http://dev.eclipse.org/bugs/show_bug.cgi?id=19104		
		if (! ActionUtil.isEditable(fEditor, getShell(), element))
			return;		
		//XXX workaround bug 31998
		if (ActionUtil.mustDisableJavaModelAction(getShell(), element))
			return;
		
		if (lightweight && fEditor instanceof CompilationUnitEditor && ! (element instanceof IPackageFragment)) {
			new RenameLinkedMode(element, (CompilationUnitEditor) fEditor).start();
		} else {
			RefactoringExecutionStarter.startRenameRefactoring(element, getShell());
		}
	}

	private static boolean isRenameAvailable(IJavaScriptElement element) throws CoreException {
		switch (element.getElementType()) {
			case IJavaScriptElement.JAVASCRIPT_PROJECT:
				return RefactoringAvailabilityTester.isRenameAvailable((IJavaScriptProject) element);
			case IJavaScriptElement.PACKAGE_FRAGMENT_ROOT:
				return RefactoringAvailabilityTester.isRenameAvailable((IPackageFragmentRoot) element);
			case IJavaScriptElement.PACKAGE_FRAGMENT:
				return RefactoringAvailabilityTester.isRenameAvailable((IPackageFragment) element);
			case IJavaScriptElement.JAVASCRIPT_UNIT:
				return RefactoringAvailabilityTester.isRenameAvailable((IJavaScriptUnit) element);
			case IJavaScriptElement.TYPE:
				return RefactoringAvailabilityTester.isRenameAvailable((IType) element);
			case IJavaScriptElement.METHOD:
				final IFunction method= (IFunction) element;
				if (method.isConstructor())
					return RefactoringAvailabilityTester.isRenameAvailable(method.getDeclaringType());
				else
					return RefactoringAvailabilityTester.isRenameAvailable(method);
			case IJavaScriptElement.FIELD:
				final IField field= (IField) element;
				return RefactoringAvailabilityTester.isRenameFieldAvailable(field);
			case IJavaScriptElement.LOCAL_VARIABLE:
				return RefactoringAvailabilityTester.isRenameAvailable((ILocalVariable) element);
		}
		return false;
	}
}
