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
package org.eclipse.wst.jsdt.internal.ui.refactoring;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.resource.ImageDescriptor;
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
import org.eclipse.wst.jsdt.core.Flags;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.corext.refactoring.code.ExtractConstantRefactoring;
import org.eclipse.wst.jsdt.internal.corext.util.JdtFlags;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.refactoring.contentassist.ControlContentAssistHelper;
import org.eclipse.wst.jsdt.internal.ui.refactoring.contentassist.VariableNamesProcessor;
import org.eclipse.wst.jsdt.internal.ui.util.ExceptionHandler;
import org.eclipse.wst.jsdt.internal.ui.util.RowLayouter;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.JavaElementImageProvider;
import org.eclipse.wst.jsdt.ui.JavaScriptElementImageDescriptor;

public class ExtractConstantWizard extends RefactoringWizard {

	private static final String MESSAGE = RefactoringMessages.ExtractConstantInputPage_enter_name; 

	public ExtractConstantWizard(ExtractConstantRefactoring ref) {
		super(ref, DIALOG_BASED_USER_INTERFACE | PREVIEW_EXPAND_FIRST_NODE); 
		setDefaultPageTitle(RefactoringMessages.ExtractConstantWizard_defaultPageTitle); 
	}

	/* non java-doc
	 * @see RefactoringWizard#addUserInputPages
	 */
	protected void addUserInputPages() {

		String message= null;
		int messageType= IMessageProvider.NONE;			
		if(!getExtractConstantRefactoring().selectionAllStaticFinal()) {
			message= RefactoringMessages.ExtractConstantInputPage_selection_refers_to_nonfinal_fields;  
			messageType= IMessageProvider.INFORMATION;
		} else {	
			message= MESSAGE;
			messageType= IMessageProvider.NONE;
		}
		
		String[] guessedNames= getExtractConstantRefactoring().guessConstantNames();
		String initialValue= guessedNames.length == 0 ? "" : guessedNames[0]; //$NON-NLS-1$
		addPage(new ExtractConstantInputPage(message, messageType, initialValue, guessedNames));
	}

	private ExtractConstantRefactoring getExtractConstantRefactoring(){
		return (ExtractConstantRefactoring) getRefactoring();
	}

	private static class ExtractConstantInputPage extends TextInputWizardPage {
		private static final String QUALIFY_REFERENCES= "qualifyReferences"; //$NON-NLS-1$

		private Label fLabel;
		private final boolean fInitialValid;
		private final int fOriginalMessageType;
		private final String fOriginalMessage;
		
		private Button fQualifyReferences;
		private String[] fConstNameProposals;

		private VariableNamesProcessor fContentAssistProcessor;
		private String fAccessModifier;
	
		public ExtractConstantInputPage(String description, int messageType, String initialValue, String[] guessedNames) {
			super(description, true, initialValue);
			fOriginalMessage= description;
			fOriginalMessageType= messageType;
			fInitialValid= ! ("".equals(initialValue)); //$NON-NLS-1$
			fConstNameProposals= guessedNames;
		}

		public void createControl(Composite parent) {
			Composite result= new Composite(parent, SWT.NONE);
			setControl(result);
			GridLayout layout= new GridLayout();
			layout.numColumns= 2;
			layout.verticalSpacing= 8;
			result.setLayout(layout);
			RowLayouter layouter= new RowLayouter(2);
		
			Label label= new Label(result, SWT.NONE);
			label.setText(RefactoringMessages.ExtractConstantInputPage_constant_name); 
		
			Text text= createTextInputField(result);
			text.selectAll();
			text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			if (fConstNameProposals.length > 0) {
				fContentAssistProcessor= new VariableNamesProcessor(fConstNameProposals);
				ControlContentAssistHelper.createTextContentAssistant(text, fContentAssistProcessor);
			}
				
			layouter.perform(label, text, 1);
		
			addAccessModifierGroup(result, layouter);
			addReplaceAllCheckbox(result, layouter);
			addQualifyReferencesCheckbox(result, layouter);
			addSeparator(result, layouter);
			addLabel(result, layouter);
		
			validateTextField(text.getText());
		
			Dialog.applyDialogFont(result);
			PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IJavaHelpContextIds.EXTRACT_CONSTANT_WIZARD_PAGE);		
		}
	
		private void addAccessModifierGroup(Composite result, RowLayouter layouter) {
			fAccessModifier= getExtractConstantRefactoring().getVisibility();
			if (getExtractConstantRefactoring().getTargetIsInterface())
				return;
			
			Label label= new Label(result, SWT.NONE);
			label.setText(RefactoringMessages.ExtractConstantInputPage_access_modifiers); 
		
			Composite group= new Composite(result, SWT.NONE);
			group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			GridLayout layout= new GridLayout();
			layout.numColumns= 4; layout.marginWidth= 0;
			group.setLayout(layout);
		
			String[] labels= new String[] {
				RefactoringMessages.ExtractMethodInputPage_public,  
				RefactoringMessages.ExtractMethodInputPage_protected, 
				RefactoringMessages.ExtractMethodInputPage_default, 
				RefactoringMessages.ExtractMethodInputPage_private
			};
			String[] data= new String[] { JdtFlags.VISIBILITY_STRING_PUBLIC,
										  JdtFlags.VISIBILITY_STRING_PROTECTED,
										  JdtFlags.VISIBILITY_STRING_PACKAGE,
										  JdtFlags.VISIBILITY_STRING_PRIVATE }; //   

			updateContentAssistImage();
			for (int i= 0; i < labels.length; i++) {
				Button radio= new Button(group, SWT.RADIO);
				radio.setText(labels[i]);
				radio.setData(data[i]);
				if (data[i] == fAccessModifier)
					radio.setSelection(true);
				radio.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent event) {
						setAccessModifier((String)event.widget.getData());
					}
				});
			}
			layouter.perform(label, group, 1);	
		}
	
		private void updateContentAssistImage() {
			if (fContentAssistProcessor == null)
				return;
			
			int flags;
			if (fAccessModifier == JdtFlags.VISIBILITY_STRING_PRIVATE) {
				flags= Flags.AccPrivate;
			} else if (fAccessModifier == JdtFlags.VISIBILITY_STRING_PUBLIC) {
				flags= Flags.AccPublic;
			} else {
				flags= Flags.AccDefault;
			}
			ImageDescriptor imageDesc= JavaElementImageProvider.getFieldImageDescriptor(false, flags);
			imageDesc= new JavaScriptElementImageDescriptor(imageDesc, JavaScriptElementImageDescriptor.STATIC | JavaScriptElementImageDescriptor.FINAL, JavaElementImageProvider.BIG_SIZE);
			fContentAssistProcessor.setProposalImageDescriptor(imageDesc);
		}

		private void addReplaceAllCheckbox(Composite result, RowLayouter layouter) {
			String title= RefactoringMessages.ExtractConstantInputPage_replace_all; 
			boolean defaultValue= getExtractConstantRefactoring().replaceAllOccurrences();
			final Button checkBox= createCheckbox(result,  title, defaultValue, layouter);
			getExtractConstantRefactoring().setReplaceAllOccurrences(checkBox.getSelection());
			checkBox.addSelectionListener(new SelectionAdapter(){
				public void widgetSelected(SelectionEvent e) {
					getExtractConstantRefactoring().setReplaceAllOccurrences(checkBox.getSelection());
				}
			});		
		}

		private void addQualifyReferencesCheckbox(Composite result, RowLayouter layouter) {
			String title= RefactoringMessages.ExtractConstantInputPage_qualify_constant_references_with_class_name; 
			boolean defaultValue= getBooleanSetting(QUALIFY_REFERENCES, getExtractConstantRefactoring().qualifyReferencesWithDeclaringClassName());
			fQualifyReferences= createCheckbox(result,  title, defaultValue, layouter);
			getExtractConstantRefactoring().setQualifyReferencesWithDeclaringClassName(fQualifyReferences.getSelection());
			fQualifyReferences.addSelectionListener(new SelectionAdapter(){
				public void widgetSelected(SelectionEvent e) {
					getExtractConstantRefactoring().setQualifyReferencesWithDeclaringClassName(fQualifyReferences.getSelection());
				}
			});	
		}

		private void addLabel(Composite result, RowLayouter layouter) {
			fLabel= new Label(result, SWT.WRAP);
			GridData gd= new GridData(GridData.FILL_BOTH);
			gd.widthHint= convertWidthInCharsToPixels(50);
			fLabel.setLayoutData(gd);
			updatePreviewLabel();
			layouter.perform(fLabel);
		}

		private void addSeparator(Composite result, RowLayouter layouter) {
			Label separator= new Label(result, SWT.SEPARATOR | SWT.HORIZONTAL);
			separator.setLayoutData((new GridData(GridData.FILL_HORIZONTAL)));
			layouter.perform(separator);
		}
	
		private void updatePreviewLabel(){
			try {
				if (fLabel != null)
					fLabel.setText(RefactoringMessages.ExtractConstantInputPage_signature_preview + getExtractConstantRefactoring().getConstantSignaturePreview()); 
			} catch(JavaScriptModelException e) {
				ExceptionHandler.handle(e, RefactoringMessages.ExtractTempInputPage_extract_local, RefactoringMessages.ExtractConstantInputPage_exception); 
			}
		}
	
		/*
		 * @see org.eclipse.wst.jsdt.internal.ui.refactoring.TextInputWizardPage#validateTextField(String)
		 */
		protected RefactoringStatus validateTextField(String text) {
			try {
				getExtractConstantRefactoring().setConstantName(text);
				updatePreviewLabel();
				RefactoringStatus result= getExtractConstantRefactoring().checkConstantNameOnChange();
				if (fOriginalMessageType == IMessageProvider.INFORMATION && result.getSeverity() == RefactoringStatus.OK)
					return RefactoringStatus.createInfoStatus(fOriginalMessage);
				else 
					return result;
			} catch (JavaScriptModelException e) {
				JavaScriptPlugin.log(e);
				return RefactoringStatus.createFatalErrorStatus(RefactoringMessages.ExtractConstantInputPage_Internal_Error); 
			}
		}

		private void setAccessModifier(String accessModifier) {
			getExtractConstantRefactoring().setVisibility(accessModifier);
			fAccessModifier= accessModifier;
			updateContentAssistImage();
			updatePreviewLabel();
		}
	
		private ExtractConstantRefactoring getExtractConstantRefactoring(){
			return (ExtractConstantRefactoring)getRefactoring();
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

		/*
		 * @see org.eclipse.wst.jsdt.internal.ui.refactoring.TextInputWizardPage#restoreMessage()
		 */
		protected void restoreMessage() {
			setMessage(fOriginalMessage, fOriginalMessageType);
		}
		
		private boolean getBooleanSetting(String key, boolean defaultValue) {
			String update= getRefactoringSettings().get(key);
			if (update != null)
				return Boolean.valueOf(update).booleanValue();
			else
				return defaultValue;
		}
		
		private void saveBooleanSetting(String key, Button checkBox) {
			if (checkBox != null)
				getRefactoringSettings().put(key, checkBox.getSelection());
		}
		
		private boolean saveSettings() {
			if (getContainer() instanceof Dialog)
				return ((Dialog)getContainer()).getReturnCode() == IDialogConstants.OK_ID;
			return true;
		}
		
		public void dispose() {
			if (saveSettings()) {
				saveBooleanSetting(QUALIFY_REFERENCES, fQualifyReferences);
			}
			super.dispose();
		}

	}	
}
