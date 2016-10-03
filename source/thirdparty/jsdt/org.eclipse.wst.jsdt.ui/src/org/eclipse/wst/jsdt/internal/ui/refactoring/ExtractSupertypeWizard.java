/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.refactoring;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;

/**
 * Refactoring wizard for the extract supertype refactoring.
 * 
 * 
 */
public final class ExtractSupertypeWizard extends RefactoringWizard {

	/** The page name */
	private static final String PAGE_NAME= "ExtractSupertypeMemberPage"; //$NON-NLS-1$

	/**
	 * Creates a new extract supertype wizard.
	 * 
	 * @param refactoring
	 *            the refactoring
	 */
	public ExtractSupertypeWizard(final Refactoring refactoring) {
		super(refactoring, WIZARD_BASED_USER_INTERFACE);
		setDefaultPageTitle(RefactoringMessages.ExtractSupertypeWizard_defaultPageTitle);
		setDefaultPageImageDescriptor(JavaPluginImages.DESC_WIZBAN_REFACTOR_EXTRACT_SUPERTYPE);
	}

	/**
	 * {@inheritDoc}
	 */
	protected void addUserInputPages() {
		final ExtractSupertypeMethodPage page= new ExtractSupertypeMethodPage();
		addPage(new ExtractSupertypeMemberPage(PAGE_NAME, page));
		addPage(page);
	}
}
