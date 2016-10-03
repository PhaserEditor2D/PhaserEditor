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
package org.eclipse.wst.jsdt.internal.ui.text.java;


import org.eclipse.core.runtime.IProgressMonitor;


/**
 * Extension to <code>IProblemRequestor</code>.
 */
public interface IProblemRequestorExtension {

	/**
	 * Sets the progress monitor to this problem requestor.
	 *
	 * @param monitor the progress monitor to be used
	 */
	void setProgressMonitor(IProgressMonitor monitor);

	/**
	 * Sets the active state of this problem requestor.
	 *
	 * @param isActive the state of this problem requestor
	 */
	void setIsActive(boolean isActive);

	/**
	 * Informs the problem requestor that a sequence of reportings is about to start. While
	 * a sequence is active, multiple peering calls of <code>beginReporting</code> and
	 * <code>endReporting</code> can appear.
	 *
	 * 
	 */
	void beginReportingSequence();

	/**
	 * Informs the problem requestor that the sequence of reportings has been finished.
	 *
	 * 
	 */
	void endReportingSequence();

	/**
	 * Tells the problem requestor to handle temporary problems.
	 *
	 * @param enable <code>true</code> if temporary problems are handled
	 * 
	 */
	void setIsHandlingTemporaryProblems(boolean enable);
}
