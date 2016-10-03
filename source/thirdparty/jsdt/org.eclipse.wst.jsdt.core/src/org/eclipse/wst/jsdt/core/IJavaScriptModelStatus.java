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

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;

/**
 * Represents the outcome of an JavaScript model operation. Status objects are
 * used inside <code>JavaScriptModelException</code> objects to indicate what went
 * wrong.
 * <p>
 * JavaScript model status object are distinguished by their plug-in id:
 * <code>getPlugin</code> returns <code>"org.eclipse.wst.jsdt.core"</code>.
 * <code>getCode</code> returns one of the status codes declared in
 * <code>IJavaScriptModelStatusConstants</code>.
 * </p>
 * <p>
 * A JavaScript model status may also carry additional information (that is, in
 * addition to the information defined in <code>IStatus</code>):
 * <ul>
 *   <li>elements - optional handles to JavaScript elements associated with the failure</li>
 *   <li>string - optional string associated with the failure</li>
 * </ul>
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 *
 * @see org.eclipse.core.runtime.IStatus
 * @see IJavaScriptModelStatusConstants
 *  
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public interface IJavaScriptModelStatus extends IStatus {
/**
 * Returns any JavaScript elements associated with the failure (see specification
 * of the status code), or an empty array if no elements are related to this
 * particular status code.
 *
 * @return the list of JavaScript element culprits
 * @see IJavaScriptModelStatusConstants
 */
IJavaScriptElement[] getElements();
/**
 * Returns the path associated with the failure (see specification
 * of the status code), or <code>null</code> if the failure is not
 * one of <code>DEVICE_PATH</code>, <code>INVALID_PATH</code>,
 * <code>PATH_OUTSIDE_PROJECT</code>, or <code>RELATIVE_PATH</code>.
 *
 * @return the path that caused the failure, or <code>null</code> if none
 * @see IJavaScriptModelStatusConstants#DEVICE_PATH
 * @see IJavaScriptModelStatusConstants#INVALID_PATH
 * @see IJavaScriptModelStatusConstants#PATH_OUTSIDE_PROJECT
 * @see IJavaScriptModelStatusConstants#RELATIVE_PATH
 */
IPath getPath();
/**
 * Returns whether this status indicates that a JavaScript model element does not exist.
 * This convenience method is equivalent to
 * <code>getCode() == IJavaScriptModelStatusConstants.ELEMENT_DOES_NOT_EXIST</code>.
 *
 * @return <code>true</code> if the status code indicates that a JavaScript model
 *   element does not exist
 * @see IJavaScriptModelStatusConstants#ELEMENT_DOES_NOT_EXIST
 */
boolean isDoesNotExist();
}
