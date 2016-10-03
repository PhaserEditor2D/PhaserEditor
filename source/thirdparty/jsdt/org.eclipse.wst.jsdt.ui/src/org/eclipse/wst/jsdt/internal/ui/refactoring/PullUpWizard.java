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
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.PullUpRefactoring;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;

/**
 * Refactoring wizard for the pull up refactoring.
 */
public final class PullUpWizard extends RefactoringWizard {

	/** The page name */
	private static final String PAGE_NAME= "PullUpMemberPage"; //$NON-NLS-1$

	/**
	 * Creates a new pull up wizard.
	 * 
	 * @param refactoring
	 *            the pull up refactoring
	 */
	public PullUpWizard(final PullUpRefactoring refactoring) {
		super(refactoring, WIZARD_BASED_USER_INTERFACE);
		setDefaultPageTitle(RefactoringMessages.PullUpWizard_defaultPageTitle);
		setDefaultPageImageDescriptor(JavaPluginImages.DESC_WIZBAN_REFACTOR_PULL_UP);
	}

	/**
	 * {@inheritDoc}
	 */
	protected void addUserInputPages() {
		final PullUpMethodPage page= new PullUpMethodPage();
		addPage(new PullUpMemberPage(PullUpWizard.PAGE_NAME, page));
		addPage(page);
	}
}
