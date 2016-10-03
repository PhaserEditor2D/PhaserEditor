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
package org.eclipse.wst.jsdt.internal.ui.refactoring.sef;

import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.wst.jsdt.internal.corext.refactoring.sef.SelfEncapsulateFieldRefactoring;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.refactoring.RefactoringMessages;

public class SelfEncapsulateFieldWizard extends RefactoringWizard {
	
	/* package */ static final String DIALOG_SETTING_SECTION= "SelfEncapsulateFieldWizard"; //$NON-NLS-1$
	
	public SelfEncapsulateFieldWizard(SelfEncapsulateFieldRefactoring refactoring) {
		super(refactoring, DIALOG_BASED_USER_INTERFACE);
		setDefaultPageTitle(RefactoringMessages.SelfEncapsulateField_sef); 
		setDialogSettings(JavaScriptPlugin.getDefault().getDialogSettings());
	}

	protected void addUserInputPages() {
		addPage(new SelfEncapsulateFieldInputPage());
	}
}
