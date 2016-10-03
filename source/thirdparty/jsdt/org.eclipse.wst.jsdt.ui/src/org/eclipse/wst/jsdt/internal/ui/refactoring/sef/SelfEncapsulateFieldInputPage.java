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

package org.eclipse.wst.jsdt.internal.ui.refactoring.sef;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.core.Flags;
import org.eclipse.wst.jsdt.core.IField;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.corext.refactoring.sef.SelfEncapsulateFieldRefactoring;
import org.eclipse.wst.jsdt.internal.corext.util.JdtFlags;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;
import org.eclipse.wst.jsdt.internal.ui.dialogs.TextFieldNavigationHandler;
import org.eclipse.wst.jsdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.wst.jsdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabels;

public class SelfEncapsulateFieldInputPage extends UserInputWizardPage {

	private SelfEncapsulateFieldRefactoring fRefactoring;
	private IDialogSettings fSettings;
	private List fEnablements;
	
	private static final String GENERATE_JAVADOC= "GenerateJavadoc";  //$NON-NLS-1$
	

	public SelfEncapsulateFieldInputPage() {
		super("InputPage"); //$NON-NLS-1$
		setDescription(RefactoringMessages.SelfEncapsulateFieldInputPage_description); 
		setImageDescriptor(JavaPluginImages.DESC_WIZBAN_REFACTOR_CU);
	}
	
	public void createControl(Composite parent) {
		fRefactoring= (SelfEncapsulateFieldRefactoring)getRefactoring();
		fEnablements= new ArrayList();
		loadSettings();
		
		Composite result= new Composite(parent, SWT.NONE);
		setControl(result);
		initializeDialogUnits(result);
		
		GridLayout layout= new GridLayout();
		layout.numColumns= 3;
		layout.verticalSpacing= 8;
		result.setLayout(layout);
		GridData gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint= convertWidthInCharsToPixels(25);
		
		Label label= new Label(result, SWT.LEAD);
		label.setText(RefactoringMessages.SelfEncapsulateFieldInputPage_getter_name); 
		
		Text getter= new Text(result, SWT.BORDER);
		getter.setText(fRefactoring.getGetterName());
		getter.setLayoutData(gd);
		TextFieldNavigationHandler.install(getter);
		
		final Label reUseGetter= new Label(result,SWT.LEAD);
		GridData getterGD= new GridData();
		getterGD.widthHint=convertWidthInCharsToPixels(23);
		reUseGetter.setLayoutData(getterGD);
		updateUseGetter(reUseGetter);
		getter.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				String getterName= ((Text)e.widget).getText();
				fRefactoring.setGetterName(getterName);
				updateUseGetter(reUseGetter);
				processValidation();
			}
		});
		
		if (needsSetter()) {
			label= new Label(result, SWT.LEAD);
			label.setText(RefactoringMessages.SelfEncapsulateFieldInputPage_setter_name); 
			
			Text setter= new Text(result, SWT.BORDER);
			setter.setText(fRefactoring.getSetterName());
			setter.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			final Label reUseSetter= new Label(result, SWT.LEAD);
			GridData setterGD= new GridData();
			setterGD.widthHint=convertWidthInCharsToPixels(23);
			reUseSetter.setLayoutData(setterGD);
			updateUseSetter(reUseSetter);
			setter.addModifyListener(new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					String setterName= ((Text)e.widget).getText();
					fRefactoring.setSetterName(setterName);
					updateUseSetter(reUseSetter);
					processValidation();
				}

			});
			TextFieldNavigationHandler.install(setter);
		}			
		
		// createSeparator(result, layouter);
		createFieldAccessBlock(result);
		
		label= new Label(result, SWT.LEFT);
		label.setText(RefactoringMessages.SelfEncapsulateFieldInputPage_insert_after);
		fEnablements.add(label);
		final Combo combo= new Combo(result, SWT.READ_ONLY);
		fillWithPossibleInsertPositions(combo, fRefactoring.getField());
		combo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				fRefactoring.setInsertionIndex(combo.getSelectionIndex() - 1);
			}
		});
		GridData gridData= new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan=2;
		combo.setLayoutData(gridData);
		fEnablements.add(combo);
		
		
		createAccessModifier(result);
			
		Button checkBox= new Button(result, SWT.CHECK);
		checkBox.setText(RefactoringMessages.SelfEncapsulateFieldInputPage_generateJavadocComment); 
		checkBox.setSelection(fRefactoring.getGenerateJavadoc());
		checkBox.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				setGenerateJavadoc(((Button)e.widget).getSelection());
			}
		});
		GridData checkGD= new GridData(GridData.FILL_HORIZONTAL);
		checkGD.horizontalSpan=3;
		checkBox.setLayoutData(checkGD);
		fEnablements.add(checkBox);
		
		updateEnablements();
		
		processValidation();
		
		getter.setFocus();
		
		Dialog.applyDialogFont(result);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IJavaHelpContextIds.SEF_WIZARD_PAGE);		
	}

	private void updateUseSetter(Label reUseSetter) {
		if (fRefactoring.isUsingLocalSetter())
			reUseSetter.setText(RefactoringMessages.SelfEncapsulateFieldInputPage_useexistingsetter_label);
		else
			reUseSetter.setText(RefactoringMessages.SelfEncapsulateFieldInputPage_usenewgetter_label); 
		updateEnablements();
	}

	private void updateEnablements() {
		boolean enable=!(fRefactoring.isUsingLocalSetter()&&fRefactoring.isUsingLocalGetter());
		for (Iterator iter= fEnablements.iterator(); iter.hasNext();) {
			Control control= (Control) iter.next();
			control.setEnabled(enable);
		}
	}

	private void updateUseGetter(Label reUseGetter) {
		if (fRefactoring.isUsingLocalGetter())
			reUseGetter.setText(RefactoringMessages.SelfEncapsulateFieldInputPage_useexistinggetter_label);
		else
			reUseGetter.setText(RefactoringMessages.SelfEncapsulateFieldInputPage_usenewsetter_label);
		updateEnablements();
	}

	private void loadSettings() {
		fSettings= getDialogSettings().getSection(SelfEncapsulateFieldWizard.DIALOG_SETTING_SECTION);
		if (fSettings == null) {
			fSettings= getDialogSettings().addNewSection(SelfEncapsulateFieldWizard.DIALOG_SETTING_SECTION);
			fSettings.put(GENERATE_JAVADOC, JavaPreferencesSettings.getCodeGenerationSettings(fRefactoring.getField().getJavaScriptProject()).createComments);
		}
		fRefactoring.setGenerateJavadoc(fSettings.getBoolean(GENERATE_JAVADOC));
	}	

	private void createAccessModifier(Composite result) {
		int visibility= fRefactoring.getVisibility();
		if (Flags.isPublic(visibility))
			return;
		GridLayout layout;
		Label label;
		label= new Label(result, SWT.NONE);
		label.setText(RefactoringMessages.SelfEncapsulateFieldInputPage_access_Modifiers); 
		fEnablements.add(label);
		Composite group= new Composite(result, SWT.NONE);
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		layout= new GridLayout();
		layout.numColumns= 4; layout.marginWidth= 0; layout.marginHeight= 0;
		group.setLayout(layout);
		GridData gridData= new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan=2;
		group.setLayoutData(gridData);
		
		Object[] info= createData(visibility);
		String[] labels= (String[])info[0];
		Integer[] data= (Integer[])info[1];
		for (int i= 0; i < labels.length; i++) {
			Button radio= new Button(group, SWT.RADIO);
			radio.setText(labels[i]);
			radio.setData(data[i]);
			int iData= data[i].intValue();
			if (iData == visibility)
				radio.setSelection(true);
			radio.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent event) {
					fRefactoring.setVisibility(((Integer)event.widget.getData()).intValue());
				}
			});
			fEnablements.add(radio);
		}
	}
	
	private void createFieldAccessBlock(Composite result) {
		Label label= new Label(result, SWT.LEFT);
		label.setText(RefactoringMessages.SelfEncapsulateField_field_access); 
		
		Composite group= new Composite(result, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.marginWidth= 0; layout.marginHeight= 0; layout.numColumns= 2;
		group.setLayout(layout);
		GridData gridData= new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan=2;
		group.setLayoutData(gridData);
		Button radio= new Button(group, SWT.RADIO);
		radio.setText(RefactoringMessages.SelfEncapsulateField_use_setter_getter); 
		radio.setSelection(true);
		radio.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fRefactoring.setEncapsulateDeclaringClass(true);
			}
		});
		radio.setLayoutData(new GridData());
		
		radio= new Button(group, SWT.RADIO);
		radio.setText(RefactoringMessages.SelfEncapsulateField_keep_references); 
		radio.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fRefactoring.setEncapsulateDeclaringClass(false);
			}
		});
		radio.setLayoutData(new GridData());
	}

	private Object[] createData(int visibility) {
		String pub= RefactoringMessages.SelfEncapsulateFieldInputPage_public; 
		String def= RefactoringMessages.SelfEncapsulateFieldInputPage_default; 
		String priv= RefactoringMessages.SelfEncapsulateFieldInputPage_private; 
		
		String[] labels= null;
		Integer[] data= null;
		if (Flags.isPrivate(visibility)) {
			labels= new String[] { pub, def, priv };
			data= new Integer[] {Integer.valueOf(Flags.AccPublic), Integer.valueOf(0), Integer.valueOf(Flags.AccPrivate) };
		} else {
			labels= new String[] { pub, def };
			data= new Integer[] {Integer.valueOf(Flags.AccPublic), Integer.valueOf(0)};
		}
		return new Object[] {labels, data};
	}
	
	private void fillWithPossibleInsertPositions(Combo combo, IField field) {
		int select= 0;
		combo.add(RefactoringMessages.SelfEncapsulateFieldInputPage_first_method); 
		try {
			IFunction[] methods= field.getDeclaringType().getFunctions();
			for (int i= 0; i < methods.length; i++) {
				combo.add(JavaScriptElementLabels.getElementLabel(methods[i], JavaScriptElementLabels.M_PARAMETER_TYPES));
			}
			if (methods.length > 0)
				select= methods.length;
		} catch (JavaScriptModelException e) {
			// Fall through
		}
		combo.select(select);
		fRefactoring.setInsertionIndex(select - 1);
	}
	
	private void setGenerateJavadoc(boolean value) {
		fSettings.put(GENERATE_JAVADOC, value);
		fRefactoring.setGenerateJavadoc(value);
	}
	
	private void processValidation() {
		RefactoringStatus status= fRefactoring.checkMethodNames();
		String message= null;
		boolean valid= true;
		if (status.hasFatalError()) {
			message= status.getMessageMatchingSeverity(RefactoringStatus.FATAL);
			valid= false;
		}
		setErrorMessage(message);
		setPageComplete(valid);
	}
	
	private boolean needsSetter() {
		try {
			return !JdtFlags.isFinal(fRefactoring.getField());
		} catch(JavaScriptModelException e) {
			return true;
		}
	}
}
