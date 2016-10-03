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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.internal.corext.refactoring.code.ConvertAnonymousToNestedRefactoring;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.dialogs.TextFieldNavigationHandler;

public class ConvertAnonymousToNestedWizard extends RefactoringWizard {

	public ConvertAnonymousToNestedWizard(ConvertAnonymousToNestedRefactoring ref) {
		super(ref, PREVIEW_EXPAND_FIRST_NODE | DIALOG_BASED_USER_INTERFACE); 
		setDefaultPageTitle(RefactoringMessages.ConvertAnonymousToNestedAction_wizard_title); 
	}

	/* non java-doc
	 * @see RefactoringWizard#addUserInputPages
	 */ 
	protected void addUserInputPages(){
		addPage(new ConvertAnonymousToNestedInputPage());
	}
	
	private static class ConvertAnonymousToNestedInputPage extends UserInputWizardPage {

		private static final String DESCRIPTION = RefactoringMessages.ConvertAnonymousToNestedInputPage_description; 
		public static final String PAGE_NAME= "ConvertAnonymousToNestedInputPage";//$NON-NLS-1$
    
		public ConvertAnonymousToNestedInputPage() {
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
		
			addVisibilityControl(result);
			Text textField= addFieldNameField(result);
			addDeclareFinalCheckbox(result);
			addDeclareAsStaticCheckbox(result);
		
			textField.setFocus();
			setPageComplete(false);
			Dialog.applyDialogFont(result);
			PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IJavaHelpContextIds.CONVERT_ANONYMOUS_TO_NESTED_WIZARD_PAGE);		
		}

		private Text addFieldNameField(Composite result) {
			Label nameLabel= new Label(result, SWT.NONE);
			nameLabel.setText(RefactoringMessages.ConvertAnonymousToNestedInputPage_class_name); 
			nameLabel.setLayoutData(new GridData());
        
			final Text classNameField= new Text(result, SWT.BORDER | SWT.SINGLE);
			classNameField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			classNameField.addModifyListener(new ModifyListener(){
				public void modifyText(ModifyEvent e) {
					ConvertAnonymousToNestedInputPage.this.getConvertRefactoring().setClassName(classNameField.getText());
					ConvertAnonymousToNestedInputPage.this.updateStatus();
				}
			});
			TextFieldNavigationHandler.install(classNameField);
			return classNameField;
		}
	
		private void updateStatus() {
			setPageComplete(getConvertRefactoring().validateInput());
		}
	
		private void addVisibilityControl(Composite result) {
			int[] availableVisibilities= getConvertRefactoring().getAvailableVisibilities();
			int currectVisibility= getConvertRefactoring().getVisibility();
			IVisibilityChangeListener visibilityChangeListener= new IVisibilityChangeListener(){
				public void visibilityChanged(int newVisibility) {
					getConvertRefactoring().setVisibility(newVisibility);
				}

				public void modifierChanged(int modifier, boolean isChecked) {
				}
			};
			Composite visibilityComposite= VisibilityControlUtil.createVisibilityControl(result, visibilityChangeListener, availableVisibilities, currectVisibility);
			if(visibilityComposite != null) {
			    GridData gd= new GridData(GridData.FILL_HORIZONTAL);
			    gd.horizontalSpan= 2;
			    visibilityComposite.setLayoutData(gd);
			}
		}
	
		public void addDeclareFinalCheckbox(Composite result) {
			GridData gd;
			final Button declareFinalCheckbox= new Button(result, SWT.CHECK);
			declareFinalCheckbox.setEnabled(getConvertRefactoring().canEnableSettingFinal());
			declareFinalCheckbox.setSelection(getConvertRefactoring().getDeclareFinal());
			declareFinalCheckbox.setText(RefactoringMessages.ConvertAnonymousToNestedInputPage_declare_final); 
			gd= new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalSpan= 2;
			declareFinalCheckbox.setLayoutData(gd);
			declareFinalCheckbox.addSelectionListener(new SelectionAdapter(){
				public void widgetSelected(SelectionEvent e) {
					getConvertRefactoring().setDeclareFinal(declareFinalCheckbox.getSelection());
				}
			});
		}
	
		public void addDeclareAsStaticCheckbox(Composite result) {
			GridData gd;
			final Button declareAsStaticCheckbox= new Button(result, SWT.CHECK);
			ConvertAnonymousToNestedRefactoring r= getConvertRefactoring();
			declareAsStaticCheckbox.setEnabled((!r.mustInnerClassBeStatic() && !r.isLocalInnerType()));
			declareAsStaticCheckbox.setSelection(getConvertRefactoring().getDeclareStatic());
			declareAsStaticCheckbox.setText(RefactoringMessages.ConvertAnonymousToNestedInputPage_declare_static); 
			gd= new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalSpan= 2;
			declareAsStaticCheckbox.setLayoutData(gd);
			declareAsStaticCheckbox.addSelectionListener(new SelectionAdapter() {
			    public void widgetSelected(SelectionEvent e) {
			        getConvertRefactoring().setDeclareStatic(declareAsStaticCheckbox.getSelection());
			    }
			});
		}
		 
		private ConvertAnonymousToNestedRefactoring getConvertRefactoring(){
			return (ConvertAnonymousToNestedRefactoring)getRefactoring();
		}
	}
}
