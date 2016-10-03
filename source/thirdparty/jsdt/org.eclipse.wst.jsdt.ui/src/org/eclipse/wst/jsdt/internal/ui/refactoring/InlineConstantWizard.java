/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.refactoring;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.internal.corext.refactoring.code.InlineConstantRefactoring;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;

public class InlineConstantWizard extends RefactoringWizard {

	private static final String MESSAGE = RefactoringMessages.InlineConstantWizard_message; 

	public InlineConstantWizard(InlineConstantRefactoring ref) {
		super(ref, DIALOG_BASED_USER_INTERFACE | PREVIEW_EXPAND_FIRST_NODE); 
		setDefaultPageTitle(RefactoringMessages.InlineConstantWizard_Inline_Constant); 
	}

	/* non java-doc
	 * @see RefactoringWizard#addUserInputPages
	 */
	protected void addUserInputPages() {

		String message= null;
		int messageType= IMessageProvider.NONE;			
		if(!getInlineConstantRefactoring().isInitializerAllStaticFinal()) {
			message= RefactoringMessages.InlineConstantWizard_initializer_refers_to_fields; 
			messageType= IMessageProvider.INFORMATION;
		} else {	
			message= MESSAGE;
			messageType= IMessageProvider.NONE;
		}
		
		addPage(new InlineConstantInputPage(message, messageType));
	}

	private InlineConstantRefactoring getInlineConstantRefactoring(){
		return (InlineConstantRefactoring)getRefactoring();
	}
	
	private static class InlineConstantInputPage extends UserInputWizardPage {

		public static final String PAGE_NAME= "InlineConstantInputPage";//$NON-NLS-1$

		private InlineConstantRefactoring fRefactoring;
		private Group fInlineMode;
		private Button fRemove;

		private final int fOriginalMessageType;
		private final String fOriginalMessage;
	
		public InlineConstantInputPage(String description, int messageType) {
			super(PAGE_NAME);
			fOriginalMessage= description;
			fOriginalMessageType= messageType;
			setDescription(description);
		}

		public void createControl(Composite parent) {
			initializeDialogUnits(parent);
			fRefactoring= (InlineConstantRefactoring)getRefactoring();
			fRefactoring.setReplaceAllReferences(fRefactoring.isDeclarationSelected());
			fRefactoring.setRemoveDeclaration(true);
			
			Composite result= new Composite(parent, SWT.NONE);
			setControl(result);
			GridLayout layout= new GridLayout();
			result.setLayout(layout);
			GridData gd= null;

			fInlineMode= new Group(result, SWT.NONE);
			fInlineMode.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			fInlineMode.setLayout(new GridLayout());
			fInlineMode.setText(RefactoringMessages.InlineConstantInputPage_Inline); 
		
			final Button all= new Button(fInlineMode, SWT.RADIO);
			all.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			all.setText(RefactoringMessages.InlineConstantInputPage_All_references); 
			all.setSelection(fRefactoring.getReplaceAllReferences());
			all.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent event) {
					fRefactoring.setReplaceAllReferences(true);
					fRemove.setEnabled(true);
				}
			});

			fRemove= new Button(fInlineMode, SWT.CHECK);
			gd= new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalIndent= convertWidthInCharsToPixels(3);
			fRemove.setLayoutData(gd);
			fRemove.setText(RefactoringMessages.InlineConstantInputPage_Delete_constant); 
			fRemove.setEnabled(all.getSelection());
			fRemove.setSelection(fRefactoring.getRemoveDeclaration());
			fRemove.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					fRefactoring.setRemoveDeclaration(fRemove.getSelection());
				}
			});

		
			final Button onlySelected= new Button(fInlineMode, SWT.RADIO);
			onlySelected.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			onlySelected.setText(RefactoringMessages.InlineConstantInputPage_Only_selected); 
			onlySelected.setSelection(!fRefactoring.getReplaceAllReferences());
			onlySelected.setEnabled(!fRefactoring.isDeclarationSelected());
			onlySelected.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent event) {
					fRefactoring.setReplaceAllReferences(false);
					fRemove.setEnabled(false);
				}
			});		
			Dialog.applyDialogFont(result);
			
			PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IJavaHelpContextIds.INLINE_CONSTANT_WIZARD_PAGE);
		}

		/*
		 * @see org.eclipse.wst.jsdt.internal.ui.refactoring.TextInputWizardPage#restoreMessage()
		 */
		protected void restoreMessage() {
			setMessage(fOriginalMessage, fOriginalMessageType);
		}
	}
}
