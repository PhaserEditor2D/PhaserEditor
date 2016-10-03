/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *
 *******************************************************************************/
package org.eclipse.wst.jsdt.ui.text.java;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;


/**
 * Interface to be implemented by contributors to the extension point
 * <code>org.eclipse.wst.jsdt.ui.quickFixProcessors</code>.
 * <p>Since 3.2, each extension specifies the marker types it can handle, and {@link #hasCorrections(IJavaScriptUnit, int)} and
 * {@link #getCorrections(IInvocationContext, IProblemLocation[])} are called if (and only if) quick fix is
 * required for a problem of these types.</p>
 * <p>Note, if a extension does not specify marker types it will be only called for problem of type
 * <code>org.eclipse.wst.jsdt.core.problem</code>, <code>org.eclipse.wst.jsdt.core.buildpath_problem</code>
 * and <code>org.eclipse.wst.jsdt.core.task</code>; compatible with the behavior prior to 3.2</p>
 * 
 *
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves. */
public interface IQuickFixProcessor {

	/**
	 * Returns <code>true</code> if the processor has proposals for the given problem. This test should be an
	 * optimistic guess and be very cheap.
	 *
	 * @param unit the compilation unit
	 * @param problemId the problem Id. The id is of a problem of the problem type(s) this processor specified in
	 * the extension point.
	 * @return <code>true</code> if the processor has proposals for the given problem
	 */
	boolean hasCorrections(IJavaScriptUnit unit, int problemId);

	/**
	 * Collects corrections or code manipulations for the given context.
	 *
	 * @param context Defines current compilation unit, position and a shared AST
	 * @param locations Problems are the current location.
	 * @return the corrections applicable at the location or <code>null</code> if no proposals
	 * 			can be offered
	 * @throws CoreException CoreException can be thrown if the operation fails
	 */
	IJavaCompletionProposal[] getCorrections(IInvocationContext context, IProblemLocation[] locations) throws CoreException;

}
