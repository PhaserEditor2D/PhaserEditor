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
package org.eclipse.wst.jsdt.internal.corext.refactoring.reorg;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JDTRefactoringDescriptor;

/**
 * Descriptor object of a JDT move refactoring.
 * 
 * 
 */
public final class JDTMoveRefactoringDescriptor extends JDTRefactoringDescriptor {

	/** The create target execution log */
	private final CreateTargetExecutionLog fLog;

	/**
	 * Creates a new JDT move refactoring descriptor.
	 * 
	 * @param log
	 *            the create target execution log
	 * @param id
	 *            the unique id of the refactoring
	 * @param project
	 *            the project name, or <code>null</code>
	 * @param description
	 *            the description
	 * @param comment
	 *            the comment, or <code>null</code>
	 * @param arguments
	 *            the argument map
	 * @param flags
	 *            the flags
	 */
	public JDTMoveRefactoringDescriptor(final CreateTargetExecutionLog log, final String id, final String project, final String description, final String comment, final Map arguments, final int flags) {
		super(id, project, description, comment, arguments, flags);
		Assert.isNotNull(log);
		fLog= log;
	}

	/**
	 * {@inheritDoc}
	 */
	public Map getArguments() {
		final Map arguments= new HashMap(super.getArguments());
		ReorgPolicyFactory.storeCreateTargetExecutionLog(getProject(), arguments, fLog);
		return arguments;
	}
}
