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
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.internal.corext.refactoring.code.PromoteTempToFieldRefactoring;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.dialogs.TextFieldNavigationHandler;
import org.eclipse.wst.jsdt.internal.ui.refactoring.contentassist.ControlContentAssistHelper;
import org.eclipse.wst.jsdt.internal.ui.refactoring.contentassist.FieldNameProcessor;

public class PromoteTempWizard extends RefactoringWizard {

	public PromoteTempWizard(PromoteTempToFieldRefactoring ref) {
		super(ref, DIALOG_BASED_USER_INTERFACE | PREVIEW_EXPAND_FIRST_NODE);
		setDefaultPageTitle(RefactoringMessages.ConvertLocalToField_title); 
	}

	/* non java-doc
	 * @see RefactoringWizard#addUserInputPages
	 */ 
	protected void addUserInputPages(){
		addPage(new PromoteTempInputPage());
	}
	
	private static class PromoteTempInputPage extends UserInputWizardPage {

		private static final String DESCRIPTION = RefactoringMessages.PromoteTempInputPage_description;
		public static final String PAGE_NAME= "PromoteTempInputPage";//$NON-NLS-1$
		private static final String[] RADIO_BUTTON_LABELS= {
							RefactoringMessages.PromoteTempInputPage_Field_declaration, 
							RefactoringMessages.PromoteTempInputPage_Current_method, 
							RefactoringMessages.PromoteTempInputPage_constructors}; 
		private static final Integer[] RADIO_BUTTON_DATA= {
							Integer.valueOf(PromoteTempToFieldRefactoring.INITIALIZE_IN_FIELD),
							Integer.valueOf(PromoteTempToFieldRefactoring.INITIALIZE_IN_METHOD),
							Integer.valueOf(PromoteTempToFieldRefactoring.INITIALIZE_IN_CONSTRUCTOR)};
		private Button fDeclareStaticCheckbox;
		private Button fDeclareFinalCheckbox;
		private Button[] fInitializeInRadioButtons;
		private Text fNameField;
	
		public PromoteTempInputPage() {
			super(PAGE_NAME);
			setDescription(DESCRIPTION);
		}

		public void createControl(Composite parent) {
			Composite result= new Composite(parent, SWT.NONE);
			setControl(result);
			GridLayout layout= new GridLayout();
			layout.numColumns= 2;
			layout.verticalSpacing= 8;
			result.setLayout(layout);
		
			addFieldNameField(result);
			addVisibilityControl(result);
			addInitizeInRadioButtonGroup(result);
			addDeclareStaticCheckbox(result);
			addDeclareFinalCheckbox(result);
				
			Dialog.applyDialogFont(result);
			PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IJavaHelpContextIds.PROMOTE_TEMP_TO_FIELD_WIZARD_PAGE);		
		}

		private void addFieldNameField(Composite result) {
			Label nameLabel= new Label(result, SWT.NONE);
			nameLabel.setText(RefactoringMessages.PromoteTempInputPage_Field_name); 
			nameLabel.setLayoutData(new GridData());
			
			String[] guessedFieldNames= getPromoteTempRefactoring().guessFieldNames();
        
			fNameField = new Text(result, SWT.BORDER | SWT.SINGLE);
			fNameField.setText(guessedFieldNames[0]);
			fNameField.selectAll();
			fNameField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			fNameField.addModifyListener(new ModifyListener(){
				public void modifyText(ModifyEvent e) {
					PromoteTempInputPage.this.getPromoteTempRefactoring().setFieldName(fNameField.getText());
					PromoteTempInputPage.this.updateStatus();
				}
			});
			IContentAssistProcessor processor= new FieldNameProcessor(guessedFieldNames, getPromoteTempRefactoring());
			ControlContentAssistHelper.createTextContentAssistant(fNameField, processor);
			TextFieldNavigationHandler.install(fNameField);
		}

		private void updateStatus() {
			setPageComplete(getPromoteTempRefactoring().validateInput());
		}

		private void addInitizeInRadioButtonGroup(Composite result) {
			GridData gd;		
			Group initializeIn= new Group(result, SWT.NONE);
			initializeIn.setText(RefactoringMessages.PromoteTempInputPage_Initialize); 
			initializeIn.setLayout(new GridLayout());
			gd= new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalSpan= 2;
			initializeIn.setLayoutData(gd);
        
			Assert.isTrue(RADIO_BUTTON_LABELS.length == RADIO_BUTTON_DATA.length);
			fInitializeInRadioButtons= new Button[RADIO_BUTTON_LABELS.length];
			for (int i= 0; i < RADIO_BUTTON_LABELS.length; i++) {
				Integer dataItem= RADIO_BUTTON_DATA[i];
				fInitializeInRadioButtons[i]= new Button(initializeIn, SWT.RADIO);
				fInitializeInRadioButtons[i].setEnabled(canEnable(dataItem.intValue()));
				fInitializeInRadioButtons[i].setText(RADIO_BUTTON_LABELS[i]);
				fInitializeInRadioButtons[i].setSelection(dataItem.intValue() == getPromoteTempRefactoring().getInitializeIn());
				fInitializeInRadioButtons[i].setLayoutData(new GridData());
				fInitializeInRadioButtons[i].setData(dataItem);
				final int j= i;
				fInitializeInRadioButtons[i].addSelectionListener(new SelectionAdapter(){
					public void widgetSelected(SelectionEvent e) {
						getPromoteTempRefactoring().setInitializeIn(getDataAsInt(fInitializeInRadioButtons[j]));
						updateButtonsEnablement();
					}
				});
			}				
		}

		private void updateButtonsEnablement() {
			fDeclareFinalCheckbox.setEnabled(getPromoteTempRefactoring().canEnableSettingFinal());
			fDeclareStaticCheckbox.setEnabled(getPromoteTempRefactoring().canEnableSettingStatic());
			for (int i= 0; i < fInitializeInRadioButtons.length; i++) {
				fInitializeInRadioButtons[i].setEnabled(canEnable(getDataAsInt(fInitializeInRadioButtons[i])));
			}
		}
    
		private static int getDataAsInt(Button button){
			return ((Integer)button.getData()).intValue();
		}
    
		private boolean canEnable(int initializeIn){
			switch(initializeIn){
				case PromoteTempToFieldRefactoring.INITIALIZE_IN_CONSTRUCTOR:
					return getPromoteTempRefactoring().canEnableSettingDeclareInConstructors();
				case PromoteTempToFieldRefactoring.INITIALIZE_IN_FIELD:
					return getPromoteTempRefactoring().canEnableSettingDeclareInFieldDeclaration();
				case PromoteTempToFieldRefactoring.INITIALIZE_IN_METHOD:
					return getPromoteTempRefactoring().canEnableSettingDeclareInMethod();
				default: Assert.isTrue(false); return false;		
			}
		}

		public void addDeclareStaticCheckbox(Composite result) {
			GridData gd;
			fDeclareStaticCheckbox= new Button(result, SWT.CHECK);
			fDeclareStaticCheckbox.setEnabled(getPromoteTempRefactoring().canEnableSettingStatic());
			fDeclareStaticCheckbox.setSelection(getPromoteTempRefactoring().getDeclareStatic());
			fDeclareStaticCheckbox.setText(RefactoringMessages.PromoteTempInputPage_declare_static); 
			gd= new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalSpan= 2;
			fDeclareStaticCheckbox.setLayoutData(gd);
			fDeclareStaticCheckbox.addSelectionListener(new SelectionAdapter(){
				public void widgetSelected(SelectionEvent e) {
					getPromoteTempRefactoring().setDeclareStatic(fDeclareStaticCheckbox.getSelection());
					updateButtonsEnablement();
				}
			});
		}

		private void addDeclareFinalCheckbox(Composite result) {
			GridData gd;
			fDeclareFinalCheckbox= new Button(result, SWT.CHECK);
			fDeclareFinalCheckbox.setEnabled(getPromoteTempRefactoring().canEnableSettingFinal());
			fDeclareFinalCheckbox.setSelection(getPromoteTempRefactoring().getDeclareFinal());
			fDeclareFinalCheckbox.setText(RefactoringMessages.PromoteTempInputPage_declare_final); 
			gd= new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalSpan= 2;
			fDeclareFinalCheckbox.setLayoutData(gd);
			fDeclareFinalCheckbox.addSelectionListener(new SelectionAdapter(){
				public void widgetSelected(SelectionEvent e) {
					getPromoteTempRefactoring().setDeclareFinal(fDeclareFinalCheckbox.getSelection());
					updateButtonsEnablement();
				}
			});
		}

		private void addVisibilityControl(Composite result) {
			int[] availableVisibilities= getPromoteTempRefactoring().getAvailableVisibilities();
			int currectVisibility= getPromoteTempRefactoring().getVisibility();
			IVisibilityChangeListener visibilityChangeListener= new IVisibilityChangeListener(){
				public void visibilityChanged(int newVisibility) {
					getPromoteTempRefactoring().setVisibility(newVisibility);
				}

				public void modifierChanged(int modifier, boolean isChecked) {
				}
			};
			Composite visibilityComposite= VisibilityControlUtil.createVisibilityControl(result, visibilityChangeListener, availableVisibilities, currectVisibility);
			GridData gd= new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalSpan= 2;
			visibilityComposite.setLayoutData(gd);
		}

		private PromoteTempToFieldRefactoring getPromoteTempRefactoring(){
			return (PromoteTempToFieldRefactoring)getRefactoring();
		}
		/* (non-Javadoc)
		 * @see org.eclipse.jface.dialogs.IDialogPage#setVisible(boolean)
		 */
		public void setVisible(boolean visible) {
			super.setVisible(visible);
			if (visible && fNameField != null)
				fNameField.setFocus();
		}

	}
}
