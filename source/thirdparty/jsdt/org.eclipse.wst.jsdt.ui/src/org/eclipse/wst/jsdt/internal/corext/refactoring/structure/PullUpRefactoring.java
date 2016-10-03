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
 * Refactoring to pull up members in a type hierarchy.
 */
public class PullUpRefactoring extends ProcessorBasedRefactoring implements IScriptableRefactoring {

	/** The refactoring processor to use */
	private final HierarchyProcessor fProcessor;

	/**
	 * Creates a new pull up refactoring.
	 * 
	 * @param processor
	 *            the pull up refactoring processor to use
	 */
	public PullUpRefactoring(final HierarchyProcessor processor) {
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
	 * Returns the pull up processor.
	 * 
	 * @return the pull up processor
	 */
	public PullUpRefactoringProcessor getPullUpProcessor() {
		return (PullUpRefactoringProcessor) getProcessor();
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
