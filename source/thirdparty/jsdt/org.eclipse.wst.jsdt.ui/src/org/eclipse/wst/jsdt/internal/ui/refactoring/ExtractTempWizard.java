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

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.internal.corext.refactoring.code.ExtractTempRefactoring;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.refactoring.contentassist.ControlContentAssistHelper;
import org.eclipse.wst.jsdt.internal.ui.refactoring.contentassist.VariableNamesProcessor;
import org.eclipse.wst.jsdt.internal.ui.util.RowLayouter;

public class ExtractTempWizard extends RefactoringWizard {

	/* package */ static final String DIALOG_SETTING_SECTION= "ExtractTempWizard"; //$NON-NLS-1$
	
	public ExtractTempWizard(ExtractTempRefactoring ref) {
		super(ref, DIALOG_BASED_USER_INTERFACE | PREVIEW_EXPAND_FIRST_NODE);
		setDefaultPageTitle(RefactoringMessages.ExtractTempWizard_defaultPageTitle); 
	}

	/* non java-doc
	 * @see RefactoringWizard#addUserInputPages
	 */ 
	protected void addUserInputPages(){
		addPage(new ExtractTempInputPage(getExtractTempRefactoring().guessTempNames()));
	}
	
	private ExtractTempRefactoring getExtractTempRefactoring(){
		return (ExtractTempRefactoring)getRefactoring();
	}

	private static class ExtractTempInputPage extends TextInputWizardPage {

		private static final String DECLARE_FINAL= "declareFinal";  //$NON-NLS-1$
		private static final String REPLACE_ALL= "replaceOccurrences";  //$NON-NLS-1$
		
		private final boolean fInitialValid;
		private static final String DESCRIPTION = RefactoringMessages.ExtractTempInputPage_enter_name; 
		private String[] fTempNameProposals;
		private IDialogSettings fSettings;
		
		public ExtractTempInputPage(String[] tempNameProposals) {
			super(DESCRIPTION, true, tempNameProposals.length == 0 ? "" : tempNameProposals[0]); //$NON-NLS-1$
			Assert.isNotNull(tempNameProposals);
			fTempNameProposals= tempNameProposals; 
			fInitialValid= tempNameProposals.length > 0; 
		}
	
		public void createControl(Composite parent) {
			loadSettings();
			Composite result= new Composite(parent, SWT.NONE);
			setControl(result);
			GridLayout layout= new GridLayout();
			layout.numColumns= 2;
			layout.verticalSpacing= 8;
			result.setLayout(layout);
			RowLayouter layouter= new RowLayouter(2);
			
			Label label= new Label(result, SWT.NONE);
			label.setText(RefactoringMessages.ExtractTempInputPage_variable_name); 
			
			Text text= createTextInputField(result);
			text.selectAll();
			text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			ControlContentAssistHelper.createTextContentAssistant(text, new VariableNamesProcessor(fTempNameProposals));
					
			layouter.perform(label, text, 1);
			
			addReplaceAllCheckbox(result, layouter);
			addDeclareFinalCheckbox(result, layouter);
			
			validateTextField(text.getText());
			
			Dialog.applyDialogFont(result);
			PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IJavaHelpContextIds.EXTRACT_TEMP_WIZARD_PAGE);		
		}

		private void loadSettings() {
			fSettings= getDialogSettings().getSection(ExtractTempWizard.DIALOG_SETTING_SECTION);
			if (fSettings == null) {
				fSettings= getDialogSettings().addNewSection(ExtractTempWizard.DIALOG_SETTING_SECTION);
				fSettings.put(DECLARE_FINAL, false);
				fSettings.put(REPLACE_ALL, true);
			}
			getExtractTempRefactoring().setDeclareFinal(fSettings.getBoolean(DECLARE_FINAL));
			getExtractTempRefactoring().setReplaceAllOccurrences(fSettings.getBoolean(REPLACE_ALL));
		}	

		private void addReplaceAllCheckbox(Composite result, RowLayouter layouter) {
			String title= RefactoringMessages.ExtractTempInputPage_replace_all; 
			boolean defaultValue= getExtractTempRefactoring().replaceAllOccurrences();
			final Button checkBox= createCheckbox(result,  title, defaultValue, layouter);
			getExtractTempRefactoring().setReplaceAllOccurrences(checkBox.getSelection());
			checkBox.addSelectionListener(new SelectionAdapter(){
				public void widgetSelected(SelectionEvent e) {
					fSettings.put(REPLACE_ALL, checkBox.getSelection());
					getExtractTempRefactoring().setReplaceAllOccurrences(checkBox.getSelection());
				}
			});		
		}
		
		private void addDeclareFinalCheckbox(Composite result, RowLayouter layouter) {
			String title= RefactoringMessages.ExtractTempInputPage_declare_final; 
			boolean defaultValue= getExtractTempRefactoring().declareFinal();
			final Button checkBox= createCheckbox(result,  title, defaultValue, layouter);
			getExtractTempRefactoring().setDeclareFinal(checkBox.getSelection());
			checkBox.addSelectionListener(new SelectionAdapter(){
				public void widgetSelected(SelectionEvent e) {
					fSettings.put(DECLARE_FINAL, checkBox.getSelection());
					getExtractTempRefactoring().setDeclareFinal(checkBox.getSelection());
				}
			});		
		}
		
		/*
		 * @see org.eclipse.wst.jsdt.internal.ui.refactoring.TextInputWizardPage#textModified(java.lang.String)
		 */
		protected void textModified(String text) {
			getExtractTempRefactoring().setTempName(text);
			super.textModified(text);
		}
		
		
		/*
		 * @see org.eclipse.wst.jsdt.internal.ui.refactoring.TextInputWizardPage#validateTextField(String)
		 */
		protected RefactoringStatus validateTextField(String text) {
			return getExtractTempRefactoring().checkTempName(text);
		}	
		
		private ExtractTempRefactoring getExtractTempRefactoring(){
			return (ExtractTempRefactoring)getRefactoring();
		}
		
		private static Button createCheckbox(Composite parent, String title, boolean value, RowLayouter layouter){
			Button checkBox= new Button(parent, SWT.CHECK);
			checkBox.setText(title);
			checkBox.setSelection(value);
			layouter.perform(checkBox);
			return checkBox;		
		}
	
		/*
		 * @see org.eclipse.wst.jsdt.internal.ui.refactoring.TextInputWizardPage#isInitialInputValid()
		 */
		protected boolean isInitialInputValid() {
			return fInitialValid;
		}
	}
}
