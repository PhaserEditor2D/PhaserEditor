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
package org.eclipse.wst.jsdt.internal.corext.refactoring.changes;

import org.eclipse.core.runtime.Assert;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.ChangeDescriptor;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringChangeDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;

/**
 * Composite change with an associated refactoring descriptor.
 * 
 * 
 */
public final class RefactoringDescriptorChange extends CompositeChange {

	/** The refactoring descriptor */
	private RefactoringDescriptor fDescriptor;

	/**
	 * Creates a new refactoring descriptor change.
	 * 
	 * @param descriptor
	 *            the refactoring descriptor
	 * @param name
	 *            the name
	 */
	public RefactoringDescriptorChange(final RefactoringDescriptor descriptor, final String name) {
		super(name);
		Assert.isNotNull(descriptor);
		fDescriptor= descriptor;
	}

	/**
	 * Creates a new refactoring descriptor change.
	 * 
	 * @param descriptor
	 *            the refactoring descriptor
	 * @param name
	 *            the name
	 * @param changes
	 *            the changes
	 */
	public RefactoringDescriptorChange(final RefactoringDescriptor descriptor, final String name, final Change[] changes) {
		super(name, changes);
		Assert.isNotNull(descriptor);
		fDescriptor= descriptor;
	}

	/**
	 * {@inheritDoc}
	 */
	public ChangeDescriptor getDescriptor() {
		return new RefactoringChangeDescriptor(fDescriptor);
	}
}
