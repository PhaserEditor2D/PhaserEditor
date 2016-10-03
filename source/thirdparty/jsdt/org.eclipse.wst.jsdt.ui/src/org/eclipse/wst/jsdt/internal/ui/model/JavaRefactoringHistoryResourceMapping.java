/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.model;

import org.eclipse.ltk.core.refactoring.history.RefactoringHistory;
import org.eclipse.ltk.core.refactoring.model.AbstractRefactoringHistoryResourceMapping;

/**
 * Refactoring history resource mapping for the Java model provider.
 * 
 * 
 */
public final class JavaRefactoringHistoryResourceMapping extends AbstractRefactoringHistoryResourceMapping {

	/**
	 * Creates a new refactoring history resource mapping.
	 * 
	 * @param history
	 *            the refactoring history
	 */
	public JavaRefactoringHistoryResourceMapping(final RefactoringHistory history) {
		super(history);
	}

	/**
	 * {@inheritDoc}
	 */
	public String getModelProviderId() {
		return JavaModelProvider.JAVA_MODEL_PROVIDER_ID;
	}
}
