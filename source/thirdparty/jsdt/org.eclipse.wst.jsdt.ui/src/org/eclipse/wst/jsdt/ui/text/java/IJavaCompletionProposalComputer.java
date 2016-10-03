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
package org.eclipse.wst.jsdt.ui.text.java;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Computes completions and context information displayed by the JavaScript editor content assistant.
 * Contributions to the <tt>org.eclipse.wst.jsdt.ui.javaCompletionProposalComputer</tt> extension point
 * must implement this interface.
 * 
 *
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves. */
public interface IJavaCompletionProposalComputer {
	/**
	 * Informs the computer that a content assist session has started. This call will always be
	 * followed by a {@link #sessionEnded()} call, but not necessarily by calls to
	 * {@linkplain #computeCompletionProposals(ContentAssistInvocationContext, IProgressMonitor) computeCompletionProposals}
	 * or
	 * {@linkplain #computeContextInformation(ContentAssistInvocationContext, IProgressMonitor) computeContextInformation}.
	 */
	void sessionStarted();

	/**
	 * Returns a list of completion proposals valid at the given invocation context.
	 * 
	 * @param context the context of the content assist invocation
	 * @param monitor a progress monitor to report progress. The monitor is private to this
	 *        invocation, i.e. there is no need for the receiver to spawn a sub monitor.
	 * @return a list of completion proposals (element type: {@link org.eclipse.jface.text.contentassist.ICompletionProposal})
	 */
	List computeCompletionProposals(ContentAssistInvocationContext context, IProgressMonitor monitor);

	/**
	 * Returns context information objects valid at the given invocation context.
	 * 
	 * @param context the context of the content assist invocation
	 * @param monitor a progress monitor to report progress. The monitor is private to this
	 *        invocation, i.e. there is no need for the receiver to spawn a sub monitor.
	 * @return a list of context information objects (element type: {@link org.eclipse.jface.text.contentassist.IContextInformation})
	 */
	List computeContextInformation(ContentAssistInvocationContext context, IProgressMonitor monitor);

	/**
	 * Returns the reason why this computer was unable to produce any completion proposals or
	 * context information.
	 * 
	 * @return an error message or <code>null</code> if no error occurred
	 */
	String getErrorMessage();

	/**
	 * Informs the computer that a content assist session has ended. This call will always be after
	 * any calls to
	 * {@linkplain #computeCompletionProposals(ContentAssistInvocationContext, IProgressMonitor) computeCompletionProposals}
	 * and
	 * {@linkplain #computeContextInformation(ContentAssistInvocationContext, IProgressMonitor) computeContextInformation}.
	 */
	void sessionEnded();
}
