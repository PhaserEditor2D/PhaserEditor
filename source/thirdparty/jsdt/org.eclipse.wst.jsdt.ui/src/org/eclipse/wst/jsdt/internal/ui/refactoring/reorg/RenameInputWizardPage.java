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
package org.eclipse.wst.jsdt.internal.ui.refactoring.reorg;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.ltk.core.refactoring.Refactoring;
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
import org.eclipse.wst.jsdt.internal.corext.refactoring.tagging.IDelegateUpdating;
import org.eclipse.wst.jsdt.internal.corext.refactoring.tagging.INameUpdating;
import org.eclipse.wst.jsdt.internal.corext.refactoring.tagging.IQualifiedNameUpdating;
import org.eclipse.wst.jsdt.internal.corext.refactoring.tagging.IReferenceUpdating;
import org.eclipse.wst.jsdt.internal.corext.refactoring.tagging.ITextUpdating;
import org.eclipse.wst.jsdt.internal.ui.refactoring.DelegateUIHelper;
import org.eclipse.wst.jsdt.internal.ui.refactoring.QualifiedNameComponent;
import org.eclipse.wst.jsdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.wst.jsdt.internal.ui.refactoring.TextInputWizardPage;
import org.eclipse.wst.jsdt.internal.ui.util.RowLayouter;

abstract class RenameInputWizardPage extends TextInputWizardPage {

	private String fHelpContextID;
	private Button fUpdateReferences;
	private Button fUpdateTextualMatches;
	private Button fUpdateQualifiedNames;
	private Button fLeaveDelegateCheckBox;
	private Button fDeprecateDelegateCheckBox;
	private QualifiedNameComponent fQualifiedNameComponent;
	
	/**
	 * Creates a new text input page.
	 * @param isLastUserPage <code>true</code> if this page is the wizard's last
	 *  user input page. Otherwise <code>false</code>.
	 * @param initialValue the initial value
	 */
	public RenameInputWizardPage(String description, String contextHelpId, boolean isLastUserPage, String initialValue) {
		super(description, isLastUserPage, initialValue);
		fHelpContextID= contextHelpId;
	}

	public void createControl(Composite parent) {
		Composite superComposite= new Composite(parent, SWT.NONE);
		setControl(superComposite);
		initializeDialogUnits(superComposite);
		superComposite.setLayout(new GridLayout());
		Composite composite= new Composite(superComposite, SWT.NONE);
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));	
		
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		layout.marginHeight= 0;
		layout.marginWidth= 0;

		composite.setLayout(layout);
		RowLayouter layouter= new RowLayouter(2);
		
		Label label= new Label(composite, SWT.NONE);
		label.setText(getLabelText());
		
		Text text= createTextInputField(composite);
		text.selectAll();
		GridData gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint= convertWidthInCharsToPixels(25);
		text.setLayoutData(gd);

		layouter.perform(label, text, 1);

		Label separator= new Label(composite, SWT.NONE);
		GridData gridData= new GridData(SWT.FILL, SWT.FILL, false, false);
		gridData.heightHint= 2;
		separator.setLayoutData(gridData);
		
		
		int indent= convertWidthInCharsToPixels(2);
		
		addOptionalUpdateReferencesCheckbox(composite, layouter);
		addAdditionalOptions(composite, layouter);
		addOptionalUpdateTextualMatches(composite, layouter);
		addOptionalUpdateQualifiedNameComponent(composite, layouter, indent);
		addOptionalLeaveDelegateCheckbox(composite, layouter);
		addOptionalDeprecateDelegateCheckbox(composite, layouter, indent);
		updateForcePreview();
		
		Dialog.applyDialogFont(superComposite);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), fHelpContextID);
	}
	
	/**
	 * Clients can override this method to provide more UI elements. By default, does nothing
	 * 
	 * @param composite the parent composite
	 * @param layouter the row layouter to use
	 */
	protected void addAdditionalOptions(Composite composite, RowLayouter layouter) {
		// none by default
	}

	public void setVisible(boolean visible) {
		if (visible) {
			INameUpdating nameUpdating= (INameUpdating)getRefactoring().getAdapter(INameUpdating.class);
			if (nameUpdating != null) {
				String newName= getNewName(nameUpdating);
				if (newName != null && newName.length() > 0 && !newName.equals(getInitialValue())) {
					Text textField= getTextField();
					textField.setText(newName);
					textField.setSelection(0, newName.length());
				}
			}
		}
		super.setVisible(visible);
	}

	/**
	 * Returns the new name for the Java element or <code>null</code>
	 * if no new name is provided
	 * 
	 * @return the new name or <code>null</code>
	 */
	protected String getNewName(INameUpdating nameUpdating) {
		return nameUpdating.getNewElementName();
	}
	
	protected boolean saveSettings() {
		// always save
//		if (getContainer() instanceof Dialog)
//			return ((Dialog)getContainer()).getReturnCode() == IDialogConstants.OK_ID;
		return true;
	}
	
	public void dispose() {
		if (saveSettings()) {
			saveBooleanSetting(RenameRefactoringWizard.UPDATE_TEXTUAL_MATCHES, fUpdateTextualMatches);
			saveBooleanSetting(RenameRefactoringWizard.UPDATE_QUALIFIED_NAMES, fUpdateQualifiedNames);
			if (fQualifiedNameComponent != null)
				fQualifiedNameComponent.savePatterns(getRefactoringSettings());
			DelegateUIHelper.saveLeaveDelegateSetting(fLeaveDelegateCheckBox);
			DelegateUIHelper.saveDeprecateDelegateSetting(fDeprecateDelegateCheckBox);
		}
		super.dispose();
	}
	
	private void addOptionalUpdateReferencesCheckbox(Composite result, RowLayouter layouter) {
		final IReferenceUpdating ref= (IReferenceUpdating)getRefactoring().getAdapter(IReferenceUpdating.class);
		if (ref == null || !ref.canEnableUpdateReferences())	
			return;
		String title= RefactoringMessages.RenameInputWizardPage_update_references; 
		boolean defaultValue= true; //bug 77901
		fUpdateReferences= createCheckbox(result, title, defaultValue, layouter);
		ref.setUpdateReferences(fUpdateReferences.getSelection());
		fUpdateReferences.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				ref.setUpdateReferences(fUpdateReferences.getSelection());
			}
		});		
	}
		
	private void addOptionalUpdateTextualMatches(Composite result, RowLayouter layouter) {
		final ITextUpdating refactoring= (ITextUpdating) getRefactoring().getAdapter(ITextUpdating.class);
		if (refactoring == null || !refactoring.canEnableTextUpdating())
			return;
		String title= RefactoringMessages.RenameInputWizardPage_update_textual_matches; 
		boolean defaultValue= getBooleanSetting(RenameRefactoringWizard.UPDATE_TEXTUAL_MATCHES, refactoring.getUpdateTextualMatches());
		fUpdateTextualMatches= createCheckbox(result, title, defaultValue, layouter);
		refactoring.setUpdateTextualMatches(fUpdateTextualMatches.getSelection());
		fUpdateTextualMatches.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				refactoring.setUpdateTextualMatches(fUpdateTextualMatches.getSelection());
				updateForcePreview();
			}
		});		
	}

	private void addOptionalUpdateQualifiedNameComponent(Composite parent, RowLayouter layouter, int marginWidth) {
		final IQualifiedNameUpdating ref= (IQualifiedNameUpdating)getRefactoring().getAdapter(IQualifiedNameUpdating.class);
		if (ref == null || !ref.canEnableQualifiedNameUpdating())
			return;
		fUpdateQualifiedNames= new Button(parent, SWT.CHECK);
		int indent= marginWidth + fUpdateQualifiedNames.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
		fUpdateQualifiedNames.setText(RefactoringMessages.RenameInputWizardPage_update_qualified_names); 
		layouter.perform(fUpdateQualifiedNames);
		
		fQualifiedNameComponent= new QualifiedNameComponent(parent, SWT.NONE, ref, getRefactoringSettings());
		layouter.perform(fQualifiedNameComponent);
		GridData gd= (GridData)fQualifiedNameComponent.getLayoutData();
		gd.horizontalAlignment= GridData.FILL;
		gd.horizontalIndent= indent;
		
		boolean defaultSelection= getBooleanSetting(RenameRefactoringWizard.UPDATE_QUALIFIED_NAMES, ref.getUpdateQualifiedNames());
		fUpdateQualifiedNames.setSelection(defaultSelection);
		updateQulifiedNameUpdating(ref, defaultSelection);

		fUpdateQualifiedNames.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				boolean enabled= ((Button)e.widget).getSelection();
				updateQulifiedNameUpdating(ref, enabled);
			}
		});
	}
	
	private void updateQulifiedNameUpdating(final IQualifiedNameUpdating ref, boolean enabled) {
		fQualifiedNameComponent.setEnabled(enabled);
		ref.setUpdateQualifiedNames(enabled);
		updateForcePreview();
	}
	
	private void addOptionalLeaveDelegateCheckbox(Composite result, RowLayouter layouter) {
		final IDelegateUpdating refactoring= (IDelegateUpdating) getRefactoring().getAdapter(IDelegateUpdating.class);
		if (refactoring == null || !refactoring.canEnableDelegateUpdating())
			return;
		fLeaveDelegateCheckBox= createCheckbox(result, refactoring.getDelegateUpdatingTitle(false), DelegateUIHelper.loadLeaveDelegateSetting(refactoring), layouter);
		refactoring.setDelegateUpdating(fLeaveDelegateCheckBox.getSelection());
		fLeaveDelegateCheckBox.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				refactoring.setDelegateUpdating(fLeaveDelegateCheckBox.getSelection());
			}
		});
	}

	private void addOptionalDeprecateDelegateCheckbox(Composite result, RowLayouter layouter, int marginWidth) {
		final IDelegateUpdating refactoring= (IDelegateUpdating) getRefactoring().getAdapter(IDelegateUpdating.class);
		if (refactoring == null || !refactoring.canEnableDelegateUpdating())
			return;
		fDeprecateDelegateCheckBox= new Button(result, SWT.CHECK);
		GridData data= new GridData();
		data.horizontalAlignment= GridData.FILL;
		data.horizontalIndent= (marginWidth + fDeprecateDelegateCheckBox.computeSize(SWT.DEFAULT, SWT.DEFAULT).x);
		fDeprecateDelegateCheckBox.setLayoutData(data);
		fDeprecateDelegateCheckBox.setText(DelegateUIHelper.getDeprecateDelegateCheckBoxTitle());
		fDeprecateDelegateCheckBox.setSelection(DelegateUIHelper.loadDeprecateDelegateSetting(refactoring));
		layouter.perform(fDeprecateDelegateCheckBox);
		refactoring.setDeprecateDelegates(fDeprecateDelegateCheckBox.getSelection());
		fDeprecateDelegateCheckBox.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				refactoring.setDeprecateDelegates(fDeprecateDelegateCheckBox.getSelection());
			}
		});
		if (fLeaveDelegateCheckBox != null) {
			fDeprecateDelegateCheckBox.setEnabled(fLeaveDelegateCheckBox.getSelection());
			fLeaveDelegateCheckBox.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					fDeprecateDelegateCheckBox.setEnabled(fLeaveDelegateCheckBox.getSelection());
				}
			});
		}
	}

	protected void updateLeaveDelegateCheckbox(int delegateCount) {
		if (fLeaveDelegateCheckBox == null)
			return;
		final IDelegateUpdating refactoring= (IDelegateUpdating) getRefactoring().getAdapter(IDelegateUpdating.class);
		fLeaveDelegateCheckBox.setEnabled(delegateCount > 0);
		fLeaveDelegateCheckBox.setText(refactoring.getDelegateUpdatingTitle(delegateCount > 1));
		if (delegateCount == 0) {
			fLeaveDelegateCheckBox.setSelection(false);
			refactoring.setDelegateUpdating(false);
		}
	}
	
	protected String getLabelText() {
		return RefactoringMessages.RenameInputWizardPage_new_name; 
	}

	protected boolean getBooleanSetting(String key, boolean defaultValue) {
		String update= getRefactoringSettings().get(key);
		if (update != null)
			return Boolean.valueOf(update).booleanValue();
		else
			return defaultValue;
	}
	
	protected void saveBooleanSetting(String key, Button checkBox) {
		if (checkBox != null)
			getRefactoringSettings().put(key, checkBox.getSelection());
	}

	private static Button createCheckbox(Composite parent, String title, boolean value, RowLayouter layouter) {
		Button checkBox= new Button(parent, SWT.CHECK);
		checkBox.setText(title);
		checkBox.setSelection(value);
		layouter.perform(checkBox);
		return checkBox;		
	}
	
	private void updateForcePreview() {
		boolean forcePreview= false;
		Refactoring refactoring= getRefactoring();
		ITextUpdating tu= (ITextUpdating) refactoring.getAdapter(ITextUpdating.class);
		IQualifiedNameUpdating qu= (IQualifiedNameUpdating)refactoring.getAdapter(IQualifiedNameUpdating.class);
		if (tu != null) {
			forcePreview= tu.getUpdateTextualMatches();
		}
		if (qu != null) {
			forcePreview |= qu.getUpdateQualifiedNames();
		}
		getRefactoringWizard().setForcePreviewReview(forcePreview);
	}
}
