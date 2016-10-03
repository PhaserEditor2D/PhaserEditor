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

import org.eclipse.core.runtime.Assert;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CopyProcessor;
import org.eclipse.ltk.core.refactoring.participants.CopyRefactoring;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.wst.jsdt.internal.corext.refactoring.tagging.IScriptableRefactoring;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;

/**
 * A copy refactoring which can be initialized with refactoring arguments.
 * 
 * 
 */
public final class JavaCopyRefactoring extends CopyRefactoring implements IScriptableRefactoring {

	/**
	 * Creates a new java copy refactoring.
	 * 
	 * @param processor
	 *            the copy processor to use
	 */
	public JavaCopyRefactoring(CopyProcessor processor) {
		super(processor);
	}

	/**
	 * {@inheritDoc}
	 */
	public RefactoringStatus initialize(final RefactoringArguments arguments) {
		Assert.isNotNull(arguments);
		final RefactoringProcessor processor= getProcessor();
		if (processor instanceof IScriptableRefactoring) {
			return ((IScriptableRefactoring) processor).initialize(arguments);
		}
		return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.ProcessorBasedRefactoring_error_unsupported_initialization, getProcessor().getIdentifier()));
	}
}
