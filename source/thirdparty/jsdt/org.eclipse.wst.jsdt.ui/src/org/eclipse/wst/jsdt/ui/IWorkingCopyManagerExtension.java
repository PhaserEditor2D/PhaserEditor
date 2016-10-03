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

package org.eclipse.wst.jsdt.ui;

import org.eclipse.ui.IEditorInput;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;

/**
 * Extension interface for {@link IWorkingCopyManager}.
 * <p>
 * Introduces API to set and remove the working copy for a given editor input.<p>
 * </p>
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 */
public interface IWorkingCopyManagerExtension {
	
	/**
	 * Sets the given working copy for the given editor input. If the given editor input
	 * is not connected to this working copy manager, this call has no effect. <p>
	 * This working copy manager does not assume the ownership of this working copy, i.e.,
	 * the given working copy is not automatically be freed when this manager is shut down.
	 * 
	 * @param input the editor input
	 * @param workingCopy the working copy
	 */
	void setWorkingCopy(IEditorInput input, IJavaScriptUnit workingCopy);
	
	/**
	 * Removes the working copy set for the given editor input. If there is no
	 * working copy set for this input or this input is not connected to this
	 * working copy manager, this call has no effect.
	 * 
	 * @param input the editor input
	 */
	void removeWorkingCopy(IEditorInput input);
}
