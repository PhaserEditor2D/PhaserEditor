/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    mkaufman@bea.com - initial API as IvalidationParticipant
 *    IBM - changed from interface IvalidationParticipant to abstract class validationParticipant
 *    IBM - rewrote spec
 *
 *******************************************************************************/

package org.eclipse.wst.jsdt.core.compiler;

import org.eclipse.wst.jsdt.core.IJavaScriptProject;

/**
 * A validation participant is notified of events occuring during the validation process.
 * The notified events are the result of a build action, a clean action, a reconcile operation
 * (for a working copy), etc.
 * <p>
 * Clients wishing to participate in the validation process must subclass this class, and implement
 * {@link #isActive(IJavaScriptProject)}, {@link #aboutToBuild(IJavaScriptProject)},
 * {@link #reconcile(ReconcileContext)}, etc.
* </p><p>
 * This class is intended to be subclassed by clients.
 * </p>
 *  
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public abstract class ValidationParticipant {

public static int READY_FOR_BUILD = 1;
public static int NEEDS_FULL_BUILD = 2;

/**
 * Notifies this participant that a validation is about to start and provides it the opportunity to
 * create missing source folders for generated source files. Additional source folders
 * should be marked as optional so the project can be built when the folders do not exist.
 * Only sent to participants interested in the project.
 * <p>
 * Default is to return <code>READY_FOR_BUILD</code>.
 * </p>
 * @param project the project about to build
 * @return READY_FOR_BUILD or NEEDS_FULL_BUILD
 */
public int aboutToBuild(IJavaScriptProject project) {
	return READY_FOR_BUILD;
}

/**
 * Notifies this participant that a validation operation is about to start and provides it the opportunity to
 * generate source files based on the source files about to be validated.
 * When isBatchBuild is true, then files contains all source files in the project.
 * Only sent to participants interested in the current build project.
 *
 * @param files is an array of BuildContext
 * @param isBatch identifies when the build is a batch build
  */
public void buildStarting(BuildContext[] files, boolean isBatch) {
	// do nothing by default
}

/**
 * Notifies this participant that a clean is about to start and provides it the opportunity to
 * delete generated source files.
 * Only sent to participants interested in the project.
 * @param project the project about to be cleaned
 */
public void cleanStarting(IJavaScriptProject project) {
	// do nothing by default
}

/**
 * Returns whether this participant is active for a given project.
 * <p>
 * Default is to return <code>false</code>.
 * </p><p>
 * For efficiency, participants that are not interested in the
 * given project should return <code>false</code> for that project.
 * </p>
 * @param project the project to participate in
 * @return whether this participant is active for a given project
 */
public boolean isActive(IJavaScriptProject project) {
	return false;
}

/**
 * Notifies this participant that a reconcile operation is happening. The participant can act on this reconcile
 * operation by using the given context. Other participant can then see the result of this participation
 * on this context.
 * <p>
 * Note that a participant should not modify the buffer of the working copy that is being reconciled.
 * </p><p>
 * Default is to do nothing.
 * </p>
 * @param context the reconcile context to act on
  */
public void reconcile(ReconcileContext context) {
	// do nothing by default
}

}
