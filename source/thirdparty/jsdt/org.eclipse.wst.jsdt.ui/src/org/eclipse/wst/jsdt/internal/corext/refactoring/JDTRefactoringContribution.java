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
package org.eclipse.wst.jsdt.internal.corext.refactoring;

import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.wst.jsdt.core.refactoring.descriptors.JavaScriptRefactoringContribution;

/**
 * Partial implementation of a JDT refactoring contribution.
 * 
 * 
 */
public abstract class JDTRefactoringContribution extends JavaScriptRefactoringContribution {

	/**
	 * {@inheritDoc}
	 */
	public final RefactoringDescriptor createDescriptor(final String id, final String project, final String description, final String comment, final Map arguments, final int flags) {
		return new JDTRefactoringDescriptor(id, project, description, comment, arguments, flags);
	}

	/**
	 * Creates the a new refactoring instance.
	 * 
	 * @param descriptor
	 *            the refactoring descriptor
	 * @return the refactoring, or <code>null</code>
	 * @throws CoreException
	 *             if an error occurs while creating the refactoring
	 */
	public abstract Refactoring createRefactoring(RefactoringDescriptor descriptor) throws CoreException;
}
