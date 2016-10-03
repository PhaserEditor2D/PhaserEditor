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
package org.eclipse.wst.jsdt.internal.ui.workingsets;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.IWorkingSetPage;

/**
 * 
 */
public abstract class AbstractWorkingSetWizardPage extends WizardPage implements IWorkingSetPage {

	private boolean fIsFirstValidation;
	private Text fWorkingSetName;
	private IWorkingSet fWorkingSet;

	/**
	 * Default constructor.
	 */
	public AbstractWorkingSetWizardPage(String pageid, String title, ImageDescriptor image) {
		super(pageid, title, image);
	}

	/*
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);
		
		Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		setControl(composite);

		Label label= new Label(composite, SWT.WRAP);
		label.setText(WorkingSetMessages.AbstractWorkingSetPage_workingSet_name); 
		GridData gd= new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_CENTER);
		label.setLayoutData(gd);

		fWorkingSetName= new Text(composite, SWT.SINGLE | SWT.BORDER);
		fWorkingSetName.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
		fWorkingSetName.addModifyListener(
			new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					validateInput();
				}
			}
		);
		fWorkingSetName.setFocus();
		
		Dialog.applyDialogFont(composite);
	}

	/*
	 * Implements method from IWorkingSetPage
	 */
	public IWorkingSet getSelection() {
		return fWorkingSet;
	}

	/*
	 * Implements method from IWorkingSetPage
	 */
	public void setSelection(IWorkingSet workingSet) {
		Assert.isNotNull(workingSet, "Working set must not be null"); //$NON-NLS-1$
		fWorkingSet= workingSet;
		if (getContainer() != null && getShell() != null && fWorkingSetName != null) {
			fWorkingSetName.setText(fWorkingSet.getName());
			validateInput();
		}
	}

	/*
	 * Implements method from IWorkingSetPage
	 */
	public void finish() {
		String workingSetName= fWorkingSetName.getText();
		if (fWorkingSet == null) {
			fWorkingSet= createWorkingSet(workingSetName);
		}
		fWorkingSet.setName(workingSetName);
	}
	
	protected abstract IWorkingSet createWorkingSet(String workingSetName);

	private void validateInput() {
		String errorMessage= null; 
		String newText= fWorkingSetName.getText();

		if (newText.equals(newText.trim()) == false)
			errorMessage = WorkingSetMessages.AbstractWorkingSetPage_warning_nameWhitespace; 
		if (newText.equals("")) { //$NON-NLS-1$
			if (fIsFirstValidation) {
				setPageComplete(false);
				fIsFirstValidation= false;
				return;
			} else {
				errorMessage= WorkingSetMessages.AbstractWorkingSetPage_warning_nameMustNotBeEmpty; 
			}
		}

		fIsFirstValidation= false;

		if (errorMessage == null && (fWorkingSet == null || newText.equals(fWorkingSet.getName()) == false)) {
			IWorkingSet[] workingSets= PlatformUI.getWorkbench().getWorkingSetManager().getWorkingSets();
			for (int i= 0; i < workingSets.length; i++) {
				if (newText.equals(workingSets[i].getName())) {
					errorMessage= WorkingSetMessages.AbstractWorkingSetPage_warning_workingSetExists; 
				}
			}
		}
		setErrorMessage(errorMessage);
		setPageComplete(errorMessage == null);
	}	
}
