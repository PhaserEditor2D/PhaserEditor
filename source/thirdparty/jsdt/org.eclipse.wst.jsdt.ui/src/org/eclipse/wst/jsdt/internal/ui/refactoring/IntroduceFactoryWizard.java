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

import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.wst.jsdt.internal.corext.refactoring.code.IntroduceFactoryRefactoring;

/**
 * @author rfuhrer@watson.ibm.com
 */
public class IntroduceFactoryWizard extends RefactoringWizard {
	/**
	 * Constructor for IntroduceFactoryWizard.
	 * @param ref the refactoring
	 * @param pageTitle the page title
	 */
	public IntroduceFactoryWizard(IntroduceFactoryRefactoring ref, String pageTitle) {
		super(ref, DIALOG_BASED_USER_INTERFACE | PREVIEW_EXPAND_FIRST_NODE); 
		setDefaultPageTitle(pageTitle);
	}

	/**
	 * @see RefactoringWizard#addUserInputPages
	 */
	protected void addUserInputPages() {
		String message= RefactoringMessages.IntroduceFactoryInputPage_name_factory; 

		IntroduceFactoryInputPage	page= new IntroduceFactoryInputPage(message);

		addPage(page);
	}

	public IntroduceFactoryRefactoring getIntroduceFactoryRefactoring() {
		return (IntroduceFactoryRefactoring) getRefactoring();
	}
}
