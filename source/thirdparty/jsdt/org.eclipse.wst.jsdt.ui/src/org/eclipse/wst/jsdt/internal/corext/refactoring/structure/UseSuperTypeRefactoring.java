/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
import org.eclipse.wst.jsdt.core.refactoring.IJavaScriptRefactorings;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.wst.jsdt.internal.corext.refactoring.tagging.IScriptableRefactoring;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;

/**
 * Refactoring to replace type occurrences by a super type where possible.
 * 
 * 
 */
public final class UseSuperTypeRefactoring extends ProcessorBasedRefactoring implements IScriptableRefactoring {

	/** The processor to use */
	private final UseSuperTypeProcessor fProcessor;

	/**
	 * Creates a new use super type refactoring.
	 * 
	 * @param processor
	 *            the processor to use
	 */
	public UseSuperTypeRefactoring(final UseSuperTypeProcessor processor) {
		super(processor);

		fProcessor= processor;
	}
	/*
	 * @see org.eclipse.ltk.core.refactoring.participants.ProcessorBasedRefactoring#getProcessor()
	 */
	public final RefactoringProcessor getProcessor() {
		return fProcessor;
	}

	/**
	 * Returns the use super type processor.
	 * 
	 * @return the refactoring processor
	 */
	public final UseSuperTypeProcessor getUseSuperTypeProcessor() {
		return (UseSuperTypeProcessor) getProcessor();
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
		return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.ProcessorBasedRefactoring_error_unsupported_initialization, IJavaScriptRefactorings.USE_SUPER_TYPE));
	}
}
