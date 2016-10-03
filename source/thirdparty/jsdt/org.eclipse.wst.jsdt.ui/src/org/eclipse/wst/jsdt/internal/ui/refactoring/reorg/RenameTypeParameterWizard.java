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

/**
 * Wizard for the rename type parameter refactoring.
 */
public final class RenameTypeParameterWizard extends RenameRefactoringWizard {

	/**
	 * Creates a new rename type parameter wizard.
	 * 
	 * @param refactoring
	 *        the refactoring to create the wizard for
	 */
	public RenameTypeParameterWizard(Refactoring refactoring) {
		super(refactoring, RefactoringMessages.RenameTypeParameterWizard_defaultPageTitle, RefactoringMessages.RenameTypeParameterWizard_inputPage_description, JavaPluginImages.DESC_WIZBAN_REFACTOR, IJavaHelpContextIds.RENAME_TYPE_PARAMETER_WIZARD_PAGE); 
	}
}
