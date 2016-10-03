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
package org.eclipse.wst.jsdt.internal.corext.refactoring.structure;

import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;

/**
 * Refactoring to move an instance method to another class.
 */
public final class MoveInstanceMethodRefactoring extends JavaMoveRefactoring {

	/**
	 * Creates a new move instance method refactoring.
	 * 
	 * @param processor
	 *            the move instance method processor to use
	 */
	public MoveInstanceMethodRefactoring(final MoveInstanceMethodProcessor processor) {
		super(processor);
	}

	/**
	 * Returns the move instance method processor
	 * 
	 * @return the move processor
	 */
	public final MoveInstanceMethodProcessor getMoveMethodProcessor() {
		return (MoveInstanceMethodProcessor) getMoveProcessor();
	}

	/**
	 * {@inheritDoc}
	 */
	public final String getName() {
		return RefactoringCoreMessages.MoveInstanceMethodRefactoring_name;
	}
}
