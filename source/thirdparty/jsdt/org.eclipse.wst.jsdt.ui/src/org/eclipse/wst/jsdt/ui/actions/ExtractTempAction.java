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

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringAvailabilityTester;
import org.eclipse.wst.jsdt.internal.corext.refactoring.code.ExtractTempRefactoring;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.actions.ActionUtil;
import org.eclipse.wst.jsdt.internal.ui.actions.SelectionConverter;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.JavaTextSelection;
import org.eclipse.wst.jsdt.internal.ui.refactoring.ExtractTempWizard;
import org.eclipse.wst.jsdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.wst.jsdt.internal.ui.refactoring.RefactoringSaveHelper;
import org.eclipse.wst.jsdt.internal.ui.refactoring.actions.RefactoringStarter;
import org.eclipse.wst.jsdt.internal.ui.util.ExceptionHandler;

/**
 * Extracts an expression into a new local variable and replaces all occurrences of
 * the expression with the local variable.
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
public class ExtractTempAction extends SelectionDispatchAction {

	private final JavaEditor fEditor;
	
	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * @param editor the JavaScript editor
	 */
	public ExtractTempAction(JavaEditor editor) {
		super(editor.getEditorSite());
		setText(RefactoringMessages.ExtractTempAction_label); 
		fEditor= editor;
		setEnabled(SelectionConverter.getInputAsCompilationUnit(fEditor) != null);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.EXTRACT_TEMP_ACTION);
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
		setEnabled(RefactoringAvailabilityTester.isExtractTempAvailable(selection));
	}

	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */		
	public void run(ITextSelection selection) {
		if (!ActionUtil.isEditable(fEditor))
			return;
		try{
			final ExtractTempRefactoring refactoring= new ExtractTempRefactoring(SelectionConverter.getInputAsCompilationUnit(fEditor), selection.getOffset(), selection.getLength());
			new RefactoringStarter().activate(refactoring, new ExtractTempWizard(refactoring), getShell(), RefactoringMessages.ExtractTempAction_extract_temp, RefactoringSaveHelper.SAVE_NOTHING);
		} catch (JavaScriptModelException e){
			ExceptionHandler.handle(e, RefactoringMessages.ExtractTempAction_extract_temp, RefactoringMessages.NewTextRefactoringAction_exception); 
		}	
	}
}
