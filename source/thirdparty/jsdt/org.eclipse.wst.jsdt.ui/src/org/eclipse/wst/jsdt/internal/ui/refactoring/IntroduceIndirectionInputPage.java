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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizardPage;
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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchConstants;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchScope;
import org.eclipse.wst.jsdt.core.search.SearchEngine;
import org.eclipse.wst.jsdt.internal.corext.refactoring.code.IntroduceIndirectionRefactoring;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.dialogs.FilteredTypesSelectionDialog;
import org.eclipse.wst.jsdt.internal.ui.dialogs.TextFieldNavigationHandler;
import org.eclipse.wst.jsdt.internal.ui.refactoring.contentassist.ControlContentAssistHelper;
import org.eclipse.wst.jsdt.internal.ui.refactoring.contentassist.JavaTypeCompletionProcessor;
import org.eclipse.wst.jsdt.internal.ui.util.SWTUtil;

/**
 * 
 */
public class IntroduceIndirectionInputPage extends UserInputWizardPage {

	/**
	 * The name of the intermediary method to be created.
	 */
	private Text fIntermediaryMethodName;
	
	private Combo fIntermediaryTypeName;
	private static final int INTERMEDIARY_TYPE_COUNT= 10;
	private static List fgIntermediaryTypes= new ArrayList(INTERMEDIARY_TYPE_COUNT);

	/**
	 * Constructor for IntroduceIndirectionInputPage.
	 * @param name the name of the page
	 */
	public IntroduceIndirectionInputPage(String name) {
		super(name);
	}

	private Text createIntermediaryNameCombo(Composite result) {
		final Text textField= new Text(result, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
		textField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		TextFieldNavigationHandler.install(textField);
		return textField;
	}
	
	private Combo createIntermediaryTypeCombo(Composite composite) {
		final Combo textCombo= new Combo(composite, SWT.SINGLE | SWT.BORDER);
		textCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		textCombo.setItems((String[]) fgIntermediaryTypes.toArray(new String[fgIntermediaryTypes.size()]));
		textCombo.setVisibleItemCount(INTERMEDIARY_TYPE_COUNT);
		
		JavaTypeCompletionProcessor processor= new JavaTypeCompletionProcessor(false, false, true);
		processor.setPackageFragment(getIntroduceIndirectionRefactoring().getInvocationPackage()); 
		ControlContentAssistHelper.createComboContentAssistant(textCombo, processor);
		TextFieldNavigationHandler.install(textCombo);
		return textCombo;
	}

	/**
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite result= new Composite(parent, SWT.NONE);

		setControl(result);

		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		result.setLayout(layout);

		Label methNameLabel= new Label(result, SWT.NONE);
		methNameLabel.setText(RefactoringMessages.IntroduceIndirectionInputPage_new_method_name);

		fIntermediaryMethodName= createIntermediaryNameCombo(result);

		final Label intermediaryTypeLabel= new Label(result, SWT.NONE);
		intermediaryTypeLabel.setText(RefactoringMessages.IntroduceIndirectionInputPage_declaring_class);

		Composite inner= new Composite(result, SWT.NONE);
		GridLayout innerLayout= new GridLayout();
		innerLayout.marginHeight= 0;
		innerLayout.marginWidth= 0;
		innerLayout.numColumns= 2;
		inner.setLayout(innerLayout);
		inner.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		fIntermediaryTypeName= createIntermediaryTypeCombo(inner);
		fIntermediaryTypeName.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		final Button browseTypes= new Button(inner, SWT.PUSH);
		browseTypes.setText(RefactoringMessages.IntroduceIndirectionInputPage_browse);
		GridData gd= new GridData();
		gd.horizontalAlignment= GridData.END;
		gd.widthHint= SWTUtil.getButtonWidthHint(browseTypes);
		browseTypes.setLayoutData(gd);

		final Button enableReferencesCheckBox= new Button(result, SWT.CHECK);
		enableReferencesCheckBox.setText(RefactoringMessages.IntroduceIndirectionInputPage_update_references);
		gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan= 2;
		gd.verticalIndent= 2;
		enableReferencesCheckBox.setLayoutData(gd);
		
		fIntermediaryMethodName.setText(getIntroduceIndirectionRefactoring().getIntermediaryMethodName());
		fIntermediaryTypeName.setText(getIntroduceIndirectionRefactoring().getIntermediaryClassName());

		fIntermediaryMethodName.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				validateInput();
			}
		});
		
		enableReferencesCheckBox.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				getIntroduceIndirectionRefactoring().setEnableUpdateReferences(enableReferencesCheckBox.getSelection());
			}
		});

		fIntermediaryTypeName.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				validateInput();
			}
		});
		
		browseTypes.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				IType intermediaryType= chooseIntermediaryClass();

				if (intermediaryType == null)
					return;

				fIntermediaryTypeName.setText(intermediaryType.getFullyQualifiedName());
			}
		});

		if (getIntroduceIndirectionRefactoring().canEnableUpdateReferences())
			enableReferencesCheckBox.setSelection(true);
		else {
			enableReferencesCheckBox.setSelection(false);
			enableReferencesCheckBox.setEnabled(false);
			getIntroduceIndirectionRefactoring().setEnableUpdateReferences(false);
		}
		
		fIntermediaryMethodName.setFocus();
		fIntermediaryMethodName.selectAll();
		validateInput();

		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IJavaHelpContextIds.INTRODUCE_INDIRECTION_WIZARD_PAGE);
	}

	private IType chooseIntermediaryClass() {
		IJavaScriptProject proj= getIntroduceIndirectionRefactoring().getProject();

		if (proj == null)
			return null;

		IJavaScriptElement[] elements= new IJavaScriptElement[] { proj };
		IJavaScriptSearchScope scope= SearchEngine.createJavaSearchScope(elements);

		FilteredTypesSelectionDialog dialog= new FilteredTypesSelectionDialog(getShell(), false, getWizard().getContainer(), scope, IJavaScriptSearchConstants.CLASS);

		dialog.setTitle(RefactoringMessages.IntroduceIndirectionInputPage_dialog_choose_declaring_class);
		dialog.setMessage(RefactoringMessages.IntroduceIndirectionInputPage_dialog_choose_declaring_class_long);

		if (dialog.open() == Window.OK) {
			return (IType) dialog.getFirstResult();
		}
		return null;
	}

	private IntroduceIndirectionRefactoring getIntroduceIndirectionRefactoring() {
		return (IntroduceIndirectionRefactoring) getRefactoring();
	}

	private void validateInput() {
		RefactoringStatus merged= new RefactoringStatus();
		merged.merge(getIntroduceIndirectionRefactoring().setIntermediaryClassName(fIntermediaryTypeName.getText()));
		merged.merge(getIntroduceIndirectionRefactoring().setIntermediaryMethodName(fIntermediaryMethodName.getText()));

		setPageComplete(!merged.hasError());
		int severity= merged.getSeverity();
		String message= merged.getMessageMatchingSeverity(severity);
		if (severity >= RefactoringStatus.INFO) {
			setMessage(message, severity);
		} else {
			setMessage("", NONE); //$NON-NLS-1$
		}
	}
	
	protected boolean performFinish() {
		storeIntermediaryTypeName();
		return super.performFinish();
	}

	public IWizardPage getNextPage() {
		storeIntermediaryTypeName();
		return super.getNextPage();
	}

	private void storeIntermediaryTypeName() {
		String destination= fIntermediaryTypeName.getText();
		if (!fgIntermediaryTypes.remove(destination) && fgIntermediaryTypes.size() >= INTERMEDIARY_TYPE_COUNT)
			fgIntermediaryTypes.remove(fgIntermediaryTypes.size() - 1);
		fgIntermediaryTypes.add(0, destination);
	}
}
