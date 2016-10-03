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

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;
import org.eclipse.wst.jsdt.internal.ui.refactoring.RefactoringMessages;

public class RenameMethodWizard extends RenameRefactoringWizard {
	
	public RenameMethodWizard(Refactoring refactoring) {
		super(refactoring,
			RefactoringMessages.RenameMethodWizard_defaultPageTitle, 
			RefactoringMessages.RenameMethodWizard_inputPage_description, 
			JavaPluginImages.DESC_WIZBAN_REFACTOR_METHOD,
			IJavaHelpContextIds.RENAME_METHOD_WIZARD_PAGE);
	}
}
