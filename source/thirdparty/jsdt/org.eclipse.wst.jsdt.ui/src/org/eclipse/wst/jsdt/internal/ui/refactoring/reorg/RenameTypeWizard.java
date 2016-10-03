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

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
import org.eclipse.ltk.core.refactoring.participants.RenameRefactoring;
import org.eclipse.wst.jsdt.internal.corext.refactoring.rename.RenameCompilationUnitProcessor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.rename.RenameTypeProcessor;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;
import org.eclipse.wst.jsdt.internal.ui.refactoring.RefactoringMessages;

/**
 * The type renaming wizard.
 */
public class RenameTypeWizard extends RenameRefactoringWizard {

	public RenameTypeWizard(Refactoring refactoring) {
		this(refactoring, RefactoringMessages.RenameTypeWizard_defaultPageTitle, RefactoringMessages.RenameTypeWizardInputPage_description, JavaPluginImages.DESC_WIZBAN_REFACTOR_TYPE,
				IJavaHelpContextIds.RENAME_TYPE_WIZARD_PAGE);
	}

	public RenameTypeWizard(Refactoring refactoring, String defaultPageTitle, String inputPageDescription, ImageDescriptor inputPageImageDescriptor, String pageContextHelpId) {
		super(refactoring, defaultPageTitle, inputPageDescription, inputPageImageDescriptor, pageContextHelpId);
	}

	/*
	 * non java-doc
	 * 
	 * @see RefactoringWizard#addUserInputPages
	 */
	protected void addUserInputPages() {
		super.addUserInputPages();
		if (isRenameType())
			addPage(new RenameTypeWizardSimilarElementsPage());

	}

	public RenameTypeProcessor getRenameTypeProcessor() {
		RefactoringProcessor proc= ((RenameRefactoring) getRefactoring()).getProcessor();
		if (proc instanceof RenameTypeProcessor)
			return (RenameTypeProcessor) proc;
		else if (proc instanceof RenameCompilationUnitProcessor) {
			RenameCompilationUnitProcessor rcu= (RenameCompilationUnitProcessor) proc;
			return rcu.getRenameTypeProcessor();
		}
		Assert.isTrue(false); // Should never get here
		return null;
	}

	protected boolean isRenameType() {
		return true;
	}

	protected RenameInputWizardPage createInputPage(String message, String initialSetting) {
		return new RenameTypeWizardInputPage(message, IJavaHelpContextIds.RENAME_TYPE_WIZARD_PAGE, true, initialSetting) {

			protected RefactoringStatus validateTextField(String text) {
				return validateNewName(text);
			}
		};
	}
}
