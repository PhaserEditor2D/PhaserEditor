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
package org.eclipse.wst.jsdt.internal.ui.refactoring.code;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.corext.refactoring.code.InlineMethodRefactoring;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;
import org.eclipse.wst.jsdt.internal.ui.refactoring.RefactoringMessages;

public class InlineMethodInputPage extends UserInputWizardPage {

	public static final String PAGE_NAME= "InlineMethodInputPage";//$NON-NLS-1$
	private static final String DESCRIPTION = RefactoringMessages.InlineMethodInputPage_description; 

	private InlineMethodRefactoring fRefactoring;
	private Group fInlineMode;
	private Button fRemove;
	
	public InlineMethodInputPage() {
		super(PAGE_NAME);
		setImageDescriptor(JavaPluginImages.DESC_WIZBAN_REFACTOR_CU);
		setDescription(DESCRIPTION);
	}

	public void createControl(Composite parent) {
		initializeDialogUnits(parent);
		fRefactoring= (InlineMethodRefactoring)getRefactoring();
		
		Composite result= new Composite(parent, SWT.NONE);
		setControl(result);
		GridLayout layout= new GridLayout();
		result.setLayout(layout);
		GridData gd= null;

		boolean all= fRefactoring.getInitialMode() == InlineMethodRefactoring.Mode.INLINE_ALL;
		fInlineMode= new Group(result, SWT.NONE);
		fInlineMode.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fInlineMode.setLayout(new GridLayout());
		fInlineMode.setText(RefactoringMessages.InlineMethodInputPage_inline); 
		
		Button radio= new Button(fInlineMode, SWT.RADIO);
		radio.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		radio.setText(RefactoringMessages.InlineMethodInputPage_all_invocations); 
		radio.setSelection(all);
		radio.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				fRemove.setEnabled(fRefactoring.canEnableDeleteSource());
				if (((Button)event.widget).getSelection())
					changeRefactoring(InlineMethodRefactoring.Mode.INLINE_ALL);
			}
		});

		fRemove= new Button(fInlineMode, SWT.CHECK);
		gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalIndent= convertWidthInCharsToPixels(3);
		fRemove.setLayoutData(gd);
		fRemove.setText(RefactoringMessages.InlineMethodInputPage_delete_declaration); 
		fRemove.setEnabled(all && fRefactoring.canEnableDeleteSource());
		fRemove.setSelection(fRefactoring.canEnableDeleteSource());
		fRefactoring.setDeleteSource(fRefactoring.canEnableDeleteSource());
		fRemove.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fRefactoring.setDeleteSource(((Button)e.widget).getSelection());
			}
		});

		
		radio= new Button(fInlineMode, SWT.RADIO);
		radio.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		radio.setText(RefactoringMessages.InlineMethodInputPage_only_selected); 
		radio.setSelection(!all);
		if (all) {
			radio.setEnabled(false);
		}
		radio.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				fRemove.setEnabled(false);
				if (((Button)event.widget).getSelection())
					changeRefactoring(InlineMethodRefactoring.Mode.INLINE_SINGLE);
			}
		});		
		Dialog.applyDialogFont(result);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IJavaHelpContextIds.INLINE_METHOD_WIZARD_PAGE);
	}
	
	private void changeRefactoring(InlineMethodRefactoring.Mode mode) {
		RefactoringStatus status;
		try {
			status= fRefactoring.setCurrentMode(mode);
		} catch (JavaScriptModelException e) {
			status= RefactoringStatus.createFatalErrorStatus(e.getMessage());
		}
		setPageComplete(status);
	}	
}
