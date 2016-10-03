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
package org.eclipse.wst.jsdt.core;

import org.eclipse.wst.jsdt.core.compiler.IProblem;

/**
 * A callback interface for receiving javaScript problem as they are discovered
 * by some JavaScript operation.
 *
 * @see IProblem
 *  
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public interface IProblemRequestor {

	/**
	 * Notification of a JavaScript problem.
	 *
	 * @param problem IProblem - The discovered JavaScript problem.
	 */
	void acceptProblem(IProblem problem);

	/**
	 * Notification sent before starting the problem detection process.
	 * Typically, this would tell a problem collector to clear previously recorded problems.
	 */
	void beginReporting();

	/**
	 * Notification sent after having completed problem detection process.
	 * Typically, this would tell a problem collector that no more problems should be expected in this
	 * iteration.
	 */
	void endReporting();

	/**
	 * Predicate allowing the problem requestor to signal whether or not it is currently
	 * interested by problem reports. When answering <code>false</code>, problem will
	 * not be discovered any more until the next iteration.
	 *
	 * This  predicate will be invoked once prior to each problem detection iteration.
	 *
	 * @return boolean - indicates whether the requestor is currently interested by problems.
	 */
	boolean isActive();
}
