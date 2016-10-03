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
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.MoveInnerToTopRefactoring;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;

public class MoveInnerToTopWizard extends RefactoringWizard {

	private class MoveInnerToToplnputPage extends TextInputWizardPage {

		private Text fFieldNameEntryText;

		private Label fFieldNameLabel;

		private Button fFinalCheckBox;

		private final boolean fInitialInputValid;

		public MoveInnerToToplnputPage(String initialValue) {
			super(RefactoringMessages.MoveInnerToToplnputPage_description, true, initialValue); 
			final MoveInnerToTopRefactoring refactoring= getMoveRefactoring();
			final boolean mandatory= refactoring.isCreatingInstanceFieldMandatory();
			fInitialInputValid= (!initialValue.equals("")) || !mandatory; //$NON-NLS-1$
			if (!mandatory)
				refactoring.setCreateInstanceField(false);
		}

		private void addFieldNameEntry(Composite newControl) {
			fFieldNameLabel= new Label(newControl, SWT.NONE);
			if (getMoveRefactoring().isCreatingInstanceFieldMandatory())
				fFieldNameLabel.setText(RefactoringMessages.MoveInnerToToplnputPage_enter_name_mandatory); 
			else
				fFieldNameLabel.setText(RefactoringMessages.MoveInnerToToplnputPage_enter_name); 
			fFieldNameLabel.setLayoutData(new GridData());

			fFieldNameEntryText= createTextInputField(newControl);
			fFieldNameEntryText.selectAll();
			fFieldNameEntryText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		}

		private void addFinalCheckBox(Composite newControl) {
			fFinalCheckBox= new Button(newControl, SWT.CHECK);
			fFinalCheckBox.setText(RefactoringMessages.MoveInnerToToplnputPage_instance_final); 
			GridData data= new GridData(GridData.FILL_HORIZONTAL);
			data.horizontalSpan= 2;
			fFinalCheckBox.setLayoutData(data);
			fFinalCheckBox.addSelectionListener(new SelectionAdapter() {

				public void widgetSelected(SelectionEvent event) {
					getMoveRefactoring().setMarkInstanceFieldAsFinal(fFinalCheckBox.getSelection());
				}
			});
			fFieldNameEntryText.addModifyListener(new ModifyListener() {

				public final void modifyText(ModifyEvent event) {
					final String text= fFieldNameEntryText.getText();
					final MoveInnerToTopRefactoring refactoring= getMoveRefactoring();
					if (refactoring.isCreatingInstanceFieldMandatory())
						setPageComplete(validateTextField(text));
					final boolean empty= text.length() == 0;
					if (refactoring.isCreatingInstanceFieldMandatory()) {
						// Do nothing
					} else if (refactoring.isCreatingInstanceFieldPossible()) {
						fFinalCheckBox.setEnabled(!empty);
					}
					if (!refactoring.isCreatingInstanceFieldMandatory())
						refactoring.setCreateInstanceField(!empty);
				}
			});
		}

		public void createControl(Composite parent) {
			initializeDialogUnits(parent);
			Composite newControl= new Composite(parent, SWT.NONE);
			setControl(newControl);
			PlatformUI.getWorkbench().getHelpSystem().setHelp(newControl, IJavaHelpContextIds.MOVE_INNER_TO_TOP_WIZARD_PAGE);
			newControl.setLayout(new GridLayout());
			Dialog.applyDialogFont(newControl);

			GridLayout layout= new GridLayout();
			layout.numColumns= 2;
			layout.verticalSpacing= 8;
			newControl.setLayout(layout);

			addFieldNameEntry(newControl);
			addFinalCheckBox(newControl);

			if (getMoveRefactoring().isCreatingInstanceFieldPossible()) {
				fFinalCheckBox.setSelection(getMoveRefactoring().isInstanceFieldMarkedFinal());
				fFinalCheckBox.setEnabled(true);
			} else {
				fFinalCheckBox.setSelection(false);
				fFinalCheckBox.setEnabled(false);
			}
		}

		/*
		 * @see org.eclipse.wst.jsdt.internal.ui.refactoring.TextInputWizardPage#isEmptyInputValid()
		 */
		protected boolean isEmptyInputValid() {
			return !getMoveRefactoring().isCreatingInstanceFieldMandatory();
		}

		/*
		 * @see org.eclipse.wst.jsdt.internal.ui.refactoring.TextInputWizardPage#isInitialInputValid()
		 */
		protected boolean isInitialInputValid() {
			return fInitialInputValid;
		}

		/*
		 * @see org.eclipse.jface.dialogs.IDialogPage#setVisible(boolean)
		 */
		public void setVisible(boolean visible) {
			super.setVisible(visible);
			if (visible) {
				String message= getMoveRefactoring().isCreatingInstanceFieldMandatory() ? RefactoringMessages.MoveInnerToToplnputPage_mandatory_info : RefactoringMessages.MoveInnerToToplnputPage_optional_info;
				setPageComplete(RefactoringStatus.createInfoStatus(message)); 
			} else {
				setPageComplete(new RefactoringStatus());
				getContainer().updateMessage();
			}
		}

		/*
		 * @see org.eclipse.wst.jsdt.internal.ui.refactoring.TextInputWizardPage#validateTextField(String)
		 */
		protected RefactoringStatus validateTextField(String text) {
			final MoveInnerToTopRefactoring refactoring= getMoveRefactoring();
			refactoring.setEnclosingInstanceName(text);
			if (refactoring.isCreatingInstanceFieldMandatory())
				return refactoring.checkEnclosingInstanceName(text);
			else if (!text.equals("")) //$NON-NLS-1$
				return refactoring.checkEnclosingInstanceName(text);
			else
				return new RefactoringStatus();
		}
	}

	public MoveInnerToTopWizard(Refactoring refactoring) {
		super(refactoring, DIALOG_BASED_USER_INTERFACE);
		setDefaultPageTitle(RefactoringMessages.MoveInnerToTopWizard_Move_Inner); 
	}

	/*
	 * @see RefactoringWizard#addUserInputPages
	 */
	protected void addUserInputPages() {
		final MoveInnerToTopRefactoring refactoring= getMoveRefactoring();
		if (refactoring.isCreatingInstanceFieldPossible())
			addPage(new MoveInnerToToplnputPage(refactoring.isCreatingInstanceFieldMandatory() ? refactoring.getEnclosingInstanceName() : "")); //$NON-NLS-1$
		else
			setChangeCreationCancelable(false);
	}

	private MoveInnerToTopRefactoring getMoveRefactoring() {
		return (MoveInnerToTopRefactoring) getRefactoring();
	}
}
