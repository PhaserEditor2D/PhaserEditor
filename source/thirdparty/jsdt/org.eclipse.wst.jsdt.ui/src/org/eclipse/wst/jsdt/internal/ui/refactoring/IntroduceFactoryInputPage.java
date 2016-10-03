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
package org.eclipse.wst.jsdt.internal.ui.refactoring;

import org.eclipse.jface.window.Window;
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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchConstants;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchScope;
import org.eclipse.wst.jsdt.core.search.SearchEngine;
import org.eclipse.wst.jsdt.internal.corext.refactoring.code.IntroduceFactoryRefactoring;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.dialogs.FilteredTypesSelectionDialog;
import org.eclipse.wst.jsdt.internal.ui.dialogs.TextFieldNavigationHandler;
import org.eclipse.wst.jsdt.internal.ui.util.SWTUtil;

/**
 * @author rfuhrer
 */
public class IntroduceFactoryInputPage extends UserInputWizardPage {
	/**
	 * The name of the factory method to be created.
	 */
	private Text fMethodName;
	
	private RefactoringStatus fMethodNameStatus;
	private RefactoringStatus fDestinationStatus;

	/**
	 * Constructor for IntroduceFactoryInputPage.
	 * @param name the name of the page
	 */
	public IntroduceFactoryInputPage(String name) {
		super(name);
	}

	private Text createTextInputField(Composite result) {
		final Text textField = new Text(result, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
		textField.selectAll();
		TextFieldNavigationHandler.install(textField);
		return textField;
	}

	/**
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite result = new Composite(parent, SWT.NONE);

		setControl(result);

		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		result.setLayout(layout);

		Label methNameLabel= new Label(result, SWT.NONE);
		methNameLabel.setText(RefactoringMessages.IntroduceFactoryInputPage_method_name); 
		
		fMethodName= createTextInputField(result);
		GridData gd= new GridData(GridData.FILL_HORIZONTAL);
		fMethodName.setLayoutData(gd);
		fMethodName.setText(getUseFactoryRefactoring().getNewMethodName());

		final Label	factoryTypeLabel= new Label(result, SWT.NONE);
		factoryTypeLabel.setText(RefactoringMessages.IntroduceFactoryInputPage_factoryClassLabel); 
		
		Composite inner= new Composite(result, SWT.NONE);
		GridLayout innerLayout= new GridLayout();
		innerLayout.marginHeight= 0; innerLayout.marginWidth= 0;
		innerLayout.numColumns= 2;
		inner.setLayout(innerLayout);
		inner.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		final Text factoryTypeName= createTextInputField(inner);
		factoryTypeName.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		final Button browseTypes= new Button(inner, SWT.PUSH);
		browseTypes.setText(RefactoringMessages.IntroduceFactoryInputPage_browseLabel); 
		gd= new GridData();
		gd.horizontalAlignment= GridData.END;
		gd.widthHint = SWTUtil.getButtonWidthHint(browseTypes);		
		browseTypes.setLayoutData(gd);

		final Button protectCtorCB= new Button(result, SWT.CHECK);
		protectCtorCB.setText(RefactoringMessages.IntroduceFactoryInputPage_protectConstructorLabel); 
		gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan= 2;
		protectCtorCB.setLayoutData(gd);

		fMethodName.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				fMethodNameStatus = getUseFactoryRefactoring().setNewMethodName(fMethodName.getText());
				validateInput(true);
				/*
				boolean				nameOk= status.isOK();

				if (status.hasFatalError()) {
					IntroduceFactoryInputPage.this.setPageComplete(false);
					
				}
				IntroduceFactoryInputPage.this.setPageComplete(!status.hasFatalError());
				IntroduceFactoryInputPage.this.setErrorMessage(nameOk ?
					"" : //$NON-NLS-1$
					status.getMessageMatchingSeverity(RefactoringStatus.ERROR));
					*/
			}
		});
		protectCtorCB.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				boolean	isChecked = protectCtorCB.getSelection();

				getUseFactoryRefactoring().setProtectConstructor(isChecked);
			}
		});

		factoryTypeName.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				fDestinationStatus= getUseFactoryRefactoring().setFactoryClass(factoryTypeName.getText());
				validateInput(false);
				/*
				boolean	nameOk= status.isOK();

				IntroduceFactoryInputPage.this.setPageComplete(nameOk);
				IntroduceFactoryInputPage.this.setErrorMessage(nameOk ? "" : //$NON-NLS-1$
															   status.getMessageMatchingSeverity(RefactoringStatus.ERROR));
															   */
			}
		});
		browseTypes.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				IType factoryType= chooseFactoryClass();

				if (factoryType == null)
					return;

				RefactoringStatus status= getUseFactoryRefactoring().setFactoryClass(factoryType.getFullyQualifiedName());
				boolean nameOk= status.isOK();

				factoryTypeName.setText(factoryType.getFullyQualifiedName());
				IntroduceFactoryInputPage.this.setPageComplete(nameOk);
				IntroduceFactoryInputPage.this.setErrorMessage(nameOk ? "" : //$NON-NLS-1$
															   status.getMessageMatchingSeverity(RefactoringStatus.ERROR));
			}
		});

		// Set up the initial state of the various dialog options.
		if (getUseFactoryRefactoring().canProtectConstructor())
			protectCtorCB.setSelection(true);
		else {
			protectCtorCB.setSelection(false);
			protectCtorCB.setEnabled(false);
			getUseFactoryRefactoring().setProtectConstructor(false);
		}
		factoryTypeName.setText(getUseFactoryRefactoring().getFactoryClassName());

		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IJavaHelpContextIds.INTRODUCE_FACTORY_WIZARD_PAGE);		
	}

	private IType chooseFactoryClass() {
		IJavaScriptProject	proj= getUseFactoryRefactoring().getProject();

		if (proj == null)
			return null;

		IJavaScriptElement[] elements= new IJavaScriptElement[] { proj };
		IJavaScriptSearchScope scope= SearchEngine.createJavaSearchScope(elements);

		FilteredTypesSelectionDialog dialog= new FilteredTypesSelectionDialog(
			getShell(), false, getWizard().getContainer(), scope, IJavaScriptSearchConstants.CLASS);

		dialog.setTitle(RefactoringMessages.IntroduceFactoryInputPage_chooseFactoryClass_title); 
		dialog.setMessage(RefactoringMessages.IntroduceFactoryInputPage_chooseFactoryClass_message); 

		if (dialog.open() == Window.OK) {
			return (IType) dialog.getFirstResult();
		}
		return null;
	}

	private IntroduceFactoryRefactoring getUseFactoryRefactoring() {
		return (IntroduceFactoryRefactoring) getRefactoring();
	}
	
	private void validateInput(boolean methodName) {
		RefactoringStatus merged= new RefactoringStatus();
		if (fMethodNameStatus != null && (methodName || fMethodNameStatus.hasError()))
			merged.merge(fMethodNameStatus);
		if (fDestinationStatus != null && (!methodName || fDestinationStatus.hasError()))
			merged.merge(fDestinationStatus);
		
		setPageComplete(!merged.hasError());
		int severity= merged.getSeverity();
		String message= merged.getMessageMatchingSeverity(severity);
		if (severity >= RefactoringStatus.INFO) {
			setMessage(message, severity);
		} else {
			setMessage("", NONE); //$NON-NLS-1$
		}
	}
 }
