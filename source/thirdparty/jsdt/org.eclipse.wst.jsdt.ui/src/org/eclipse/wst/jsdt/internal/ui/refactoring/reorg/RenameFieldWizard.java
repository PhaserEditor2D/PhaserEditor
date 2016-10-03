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
package org.eclipse.wst.jsdt.internal.ui.refactoring.reorg;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.RenameRefactoring;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.internal.corext.refactoring.rename.RenameFieldProcessor;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;
import org.eclipse.wst.jsdt.internal.ui.refactoring.RefactoringMessages;

public class RenameFieldWizard extends RenameRefactoringWizard {

	public RenameFieldWizard(Refactoring refactoring) {
		super(refactoring, RefactoringMessages.RenameFieldWizard_defaultPageTitle,  
			RefactoringMessages.RenameFieldWizard_inputPage_description,   
			JavaPluginImages.DESC_WIZBAN_REFACTOR_FIELD,
			IJavaHelpContextIds.RENAME_FIELD_WIZARD_PAGE);
	}

	protected RenameInputWizardPage createInputPage(String message, String initialSetting) {
		return new RenameFieldInputWizardPage(message, IJavaHelpContextIds.RENAME_FIELD_WIZARD_PAGE, initialSetting) {
			protected RefactoringStatus validateTextField(String text) {
				RefactoringStatus result= validateNewName(text);
				updateGetterSetterLabels();
				return result;
			}	
		};
	}

	private static class RenameFieldInputWizardPage extends RenameInputWizardPage {

		private Button fRenameGetter;
		private Button fRenameSetter;
		private String fGetterRenamingErrorMessage;
		private String fSetterRenamingErrorMessage;
		
		public RenameFieldInputWizardPage(String message, String contextHelpId, String initialValue) {
			super(message, contextHelpId, true, initialValue);
		}

		public void createControl(Composite parent) {
			super.createControl(parent);
			Composite parentComposite= (Composite)getControl();
				
			Composite composite= new Composite(parentComposite, SWT.NONE);
			final GridLayout layout= new GridLayout();
			layout.marginHeight= 0;
			layout.marginWidth= 0;
			composite.setLayout(layout);
			composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		
			Label separator= new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
			separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			getGetterSetterRenamingEnablement();
				
			fRenameGetter= new Button(composite, SWT.CHECK);
			boolean getterEnablement= fGetterRenamingErrorMessage == null;
			fRenameGetter.setEnabled(getterEnablement);
			boolean getterSelection= getterEnablement && getBooleanSetting(RenameRefactoringWizard.FIELD_RENAME_GETTER, getRenameFieldProcessor().getRenameGetter());
			fRenameGetter.setSelection(getterSelection);
			getRenameFieldProcessor().setRenameGetter(getterSelection);
			fRenameGetter.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			fRenameGetter.addSelectionListener(new SelectionAdapter(){
				public void widgetSelected(SelectionEvent e) {
					getRenameFieldProcessor().setRenameGetter(fRenameGetter.getSelection());
					updateLeaveDelegateCheckbox(getRenameFieldProcessor().getDelegateCount());
				}
			});
		
			fRenameSetter= new Button(composite, SWT.CHECK);
			boolean setterEnablement= fSetterRenamingErrorMessage == null;
			fRenameSetter.setEnabled(setterEnablement);
			boolean setterSelection= setterEnablement && getBooleanSetting(RenameRefactoringWizard.FIELD_RENAME_SETTER, getRenameFieldProcessor().getRenameSetter());
			fRenameSetter.setSelection(setterSelection);
			getRenameFieldProcessor().setRenameSetter(setterSelection);
			fRenameSetter.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			fRenameSetter.addSelectionListener(new SelectionAdapter(){
				public void widgetSelected(SelectionEvent e) {
					getRenameFieldProcessor().setRenameSetter(fRenameSetter.getSelection());
					updateLeaveDelegateCheckbox(getRenameFieldProcessor().getDelegateCount());
				}
			});
		
			updateGetterSetterLabels();
			updateLeaveDelegateCheckbox(getRenameFieldProcessor().getDelegateCount());
			Dialog.applyDialogFont(composite);
		}
		
		public void dispose() {
			if (saveSettings()) {
				if (fRenameGetter.isEnabled())
					saveBooleanSetting(RenameRefactoringWizard.FIELD_RENAME_GETTER, fRenameGetter);
				if (fRenameSetter.isEnabled())
					saveBooleanSetting(RenameRefactoringWizard.FIELD_RENAME_SETTER, fRenameSetter);
			}
			super.dispose();
		}
		
		private void getGetterSetterRenamingEnablement() {
			BusyIndicator.showWhile(getShell().getDisplay(), new Runnable(){
				public void run() {
					checkGetterRenamingEnablement();
					checkSetterRenamingEnablement();
				}
			});
		}
	
		protected void updateGetterSetterLabels(){
			fRenameGetter.setText(getRenameGetterLabel());
			fRenameSetter.setText(getRenameSetterLabel());
		}
	
		private String getRenameGetterLabel(){
			String defaultLabel= RefactoringMessages.RenameFieldInputWizardPage_rename_getter; 
			if (fGetterRenamingErrorMessage != null)
				return constructDisabledGetterRenamingLabel(defaultLabel);
			try {
				IFunction	getter= getRenameFieldProcessor().getGetter();
				if (getter == null || ! getter.exists())
					return defaultLabel;
				String oldGetterName= getter.getElementName();
				String newGetterName= createNewGetterName();
				return Messages.format(RefactoringMessages.RenameFieldInputWizardPage_rename_getter_to, new String[]{oldGetterName, newGetterName}); 
			} catch(CoreException e) {
				JavaScriptPlugin.log(e)	;
				return defaultLabel;			
			}
		}
		
		private String getRenameSetterLabel(){
			String defaultLabel= RefactoringMessages.RenameFieldInputWizardPage_rename_setter; 
			if (fSetterRenamingErrorMessage != null)
				return constructDisabledSetterRenamingLabel(defaultLabel);
			try {
				IFunction	setter= getRenameFieldProcessor().getSetter();
				if (setter == null || ! setter.exists())
					return defaultLabel;
				String oldSetterName= setter.getElementName();
				String newSetterName= createNewSetterName();
				return Messages.format(RefactoringMessages.RenameFieldInputWizardPage_rename_setter_to, new String[]{oldSetterName, newSetterName});
			} catch(CoreException e) {
				JavaScriptPlugin.log(e);
				return defaultLabel;			
			}
		}
		private String constructDisabledSetterRenamingLabel(String defaultLabel) {
			if (fSetterRenamingErrorMessage.equals("")) //$NON-NLS-1$
				return defaultLabel;
			String[] keys= {defaultLabel, fSetterRenamingErrorMessage};
			return Messages.format(RefactoringMessages.RenameFieldInputWizardPage_setter_label, keys); 
		}
	
		private String constructDisabledGetterRenamingLabel(String defaultLabel) {
			if (fGetterRenamingErrorMessage.equals("")) //$NON-NLS-1$
				return defaultLabel;
			String[] keys= {defaultLabel, fGetterRenamingErrorMessage};
			return Messages.format(RefactoringMessages.RenameFieldInputWizardPage_getter_label, keys);			 
		}
	
		private String createNewGetterName() throws CoreException {
			return getRenameFieldProcessor().getNewGetterName();
		}
	
		private String createNewSetterName() throws CoreException {
			return getRenameFieldProcessor().getNewSetterName();
		}
	
		private String checkGetterRenamingEnablement() {
			if (fGetterRenamingErrorMessage != null)
				return  fGetterRenamingErrorMessage;
			try {
				fGetterRenamingErrorMessage= getRenameFieldProcessor().canEnableGetterRenaming();
				return fGetterRenamingErrorMessage;
			} catch (CoreException e) {
				JavaScriptPlugin.log(e);
				return ""; //$NON-NLS-1$
			} 
		}

		private String checkSetterRenamingEnablement() {
			if (fSetterRenamingErrorMessage != null)
				return  fSetterRenamingErrorMessage;
			try {
				fSetterRenamingErrorMessage= getRenameFieldProcessor().canEnableSetterRenaming();
				return fSetterRenamingErrorMessage;
			} catch (CoreException e) {
				JavaScriptPlugin.log(e);
				return ""; //$NON-NLS-1$
			} 
		}
	
		private RenameFieldProcessor getRenameFieldProcessor() {
			return (RenameFieldProcessor)((RenameRefactoring)getRefactoring()).getProcessor();
		}
	}
}
