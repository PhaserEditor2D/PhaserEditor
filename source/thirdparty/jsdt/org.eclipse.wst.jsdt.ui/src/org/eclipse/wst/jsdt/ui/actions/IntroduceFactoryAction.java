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
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.ISourceRange;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringAvailabilityTester;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringExecutionStarter;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.actions.ActionUtil;
import org.eclipse.wst.jsdt.internal.ui.actions.SelectionConverter;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.JavaTextSelection;
import org.eclipse.wst.jsdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.wst.jsdt.internal.ui.util.ExceptionHandler;

/**
 * Action that encapsulates the a constructor call with a factory
 * method.
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
public class IntroduceFactoryAction extends SelectionDispatchAction {
	
	private JavaEditor fEditor;

	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * @param editor the JavaScript editor
	 */
	public IntroduceFactoryAction(JavaEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(SelectionConverter.getInputAsCompilationUnit(fEditor) != null);
	}

	/**
	 * Creates a new <code>IntroduceFactoryAction</code>. The action requires
	 * that the selection provided by the site's selection provider is of type <code>
	 * org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param site the site providing context information for this action
	 */
	public IntroduceFactoryAction(IWorkbenchSite site) {
		super(site);
		setText(RefactoringMessages.IntroduceFactoryAction_label); 
		setToolTipText(RefactoringMessages.IntroduceFactoryAction_tooltipText); 
		setDescription(RefactoringMessages.IntroduceFactoryAction_description); 
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.INTRODUCE_FACTORY_ACTION);
	}
	
	//---- structured selection --------------------------------------------------
	
	/*
	 * @see SelectionDispatchAction#selectionChanged(IStructuredSelection)
	 */
	public void selectionChanged(IStructuredSelection selection) {
		try {
			setEnabled(RefactoringAvailabilityTester.isIntroduceFactoryAvailable(selection));
		} catch (JavaScriptModelException e) {
			if (JavaModelUtil.isExceptionToBeLogged(e))
				JavaScriptPlugin.log(e);
			setEnabled(false);//no UI here - happens on selection changes
		}
	}

	/*
	 * @see SelectionDispatchAction#run(IStructuredSelection)
	 */
	public void run(IStructuredSelection selection) {
		try {
			// we have to call this here - no selection changed event is sent after a refactoring but it may still invalidate enablement
			if (RefactoringAvailabilityTester.isIntroduceFactoryAvailable(selection)) {
				IFunction method= (IFunction) selection.getFirstElement();
				if (!ActionUtil.isEditable(getShell(), method))
					return;
				ISourceRange range= method.getNameRange();
				RefactoringExecutionStarter.startIntroduceFactoryRefactoring(method.getJavaScriptUnit(), new TextSelection(range.getOffset(), range.getLength()), getShell());
			}
		} catch (CoreException e) {
			ExceptionHandler.handle(e, RefactoringMessages.IntroduceFactoryAction_dialog_title, RefactoringMessages.IntroduceFactoryAction_exception); 
		}
	}

	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */		
	public void selectionChanged(ITextSelection selection) {
		setEnabled(fEditor != null && SelectionConverter.getInputAsCompilationUnit(fEditor) != null);
	}

	/**
	 * Note: This method is for internal use only. Clients should not call this method.
	 */
	public void selectionChanged(JavaTextSelection selection) {
		try {
			setEnabled(RefactoringAvailabilityTester.isIntroduceFactoryAvailable(selection));
		} catch (JavaScriptModelException e) {
			setEnabled(false);
		}
	}

	public void run(ITextSelection selection) {
		if (!ActionUtil.isEditable(fEditor))
			return;
		try {
			RefactoringExecutionStarter.startIntroduceFactoryRefactoring(SelectionConverter.getInputAsCompilationUnit(fEditor), selection, getShell());
		} catch (CoreException e) {
			ExceptionHandler.handle(e, RefactoringMessages.IntroduceFactoryAction_dialog_title, RefactoringMessages.IntroduceFactoryAction_exception); 
		}
	}
}
