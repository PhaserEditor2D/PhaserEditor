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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;

/**
 * Refactoring arguments which provide the ability to set arguments using
 * key-value pairs of strings.
 * 
 * @see org.eclipse.ltk.core.refactoring.RefactoringContribution
 * @see org.eclipse.ltk.core.refactoring.RefactoringDescriptor
 * 
 * 
 */
public final class JavaRefactoringArguments extends RefactoringArguments {

	/** The attribute map (element type: <code>&lt;String, String&gt;</code>) */
	private final Map fAttributes= new HashMap(2);

	/** The name of the project, or <code>null</code> for the workspace */
	private String fProject;

	/**
	 * Creates a new java refactoring arguments.
	 * 
	 * @param project
	 *            the project, or <code>null</code> for the workspace
	 */
	public JavaRefactoringArguments(final String project) {
		Assert.isTrue(project == null || !"".equals(project)); //$NON-NLS-1$
		fProject= project;
	}

	/**
	 * Returns the attribute with the specified name.
	 * 
	 * @param name
	 *            the name of the attribute
	 * @return the attribute value, or <code>null</code>
	 */
	public String getAttribute(final String name) {
		return (String) fAttributes.get(name);
	}

	/**
	 * Returns the name of the project.
	 * 
	 * @return the name of the project, or <code>null</code> for the workspace
	 */
	public String getProject() {
		return fProject;
	}

	/**
	 * Sets the attribute with the specified name to the indicated value.
	 * 
	 * @param name
	 *            the name of the attribute
	 * @param value
	 *            the value of the attribute
	 */
	public void setAttribute(final String name, final String value) {
		Assert.isNotNull(name);
		Assert.isNotNull(value);
		fAttributes.put(name, value);
	}

	/**
	 * Sets the name of the project.
	 * 
	 * @param project
	 *            the name of the project, or <code>null</code> for the
	 *            workspace
	 */
	public void setProject(final String project) {
		Assert.isTrue(project == null || !"".equals(project)); //$NON-NLS-1$
		fProject= project;
	}

	/**
	 * {@inheritDoc}
	 */
	public String toString() {
		return getClass().getName() + fAttributes.toString();
	}
}
