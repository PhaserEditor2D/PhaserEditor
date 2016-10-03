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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.wst.jsdt.internal.corext.refactoring.code.ExtractMethodRefactoring;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.refactoring.RefactoringMessages;

public class ExtractMethodWizard extends RefactoringWizard {
	
	/* package */ static final String DIALOG_SETTING_SECTION= "ExtractMethodWizard"; //$NON-NLS-1$
	
	public ExtractMethodWizard(ExtractMethodRefactoring ref){
		super(ref, DIALOG_BASED_USER_INTERFACE | PREVIEW_EXPAND_FIRST_NODE);
		setDefaultPageTitle(RefactoringMessages.ExtractMethodWizard_extract_method); 
		setDialogSettings(JavaScriptPlugin.getDefault().getDialogSettings());
	}

	public Change createChange(){
		// creating the change is cheap. So we don't need to show progress.
		try {
			return getRefactoring().createChange(new NullProgressMonitor());
		} catch (CoreException e) {
			JavaScriptPlugin.log(e);
			return null;
		}	
	}
		
	protected void addUserInputPages(){
		addPage(new ExtractMethodInputPage());
	}
}
