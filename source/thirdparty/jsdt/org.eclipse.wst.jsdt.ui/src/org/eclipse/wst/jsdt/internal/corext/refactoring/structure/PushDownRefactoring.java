/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.corext.refactoring.structure;

import org.eclipse.core.runtime.Assert;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.ProcessorBasedRefactoring;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.wst.jsdt.internal.corext.refactoring.tagging.IScriptableRefactoring;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;

/**
 * Refactoring to push down members in a type hierarchy.
 */
public final class PushDownRefactoring extends ProcessorBasedRefactoring implements IScriptableRefactoring {

	/** The refactoring processor to use */
	private final PushDownRefactoringProcessor fProcessor;

	/**
	 * Creates a new push down refactoring.
	 * 
	 * @param processor
	 *            the push down refactoring processor to use
	 */
	public PushDownRefactoring(final PushDownRefactoringProcessor processor) {
		super(processor);
		fProcessor= processor;
	}

	/**
	 * {@inheritDoc}
	 */
	public RefactoringProcessor getProcessor() {
		return fProcessor;
	}

	/**
	 * Returns the push down processor.
	 * 
	 * @return the push down processor
	 */
	public PushDownRefactoringProcessor getPushDownProcessor() {
		return (PushDownRefactoringProcessor) getProcessor();
	}

	/**
	 * {@inheritDoc}
	 */
	public final RefactoringStatus initialize(final RefactoringArguments arguments) {
		Assert.isNotNull(arguments);
		final RefactoringProcessor processor= getProcessor();
		if (processor instanceof IScriptableRefactoring) {
			return ((IScriptableRefactoring) processor).initialize(arguments);
		}
		return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.ProcessorBasedRefactoring_error_unsupported_initialization, getProcessor().getIdentifier()));
	}
}
