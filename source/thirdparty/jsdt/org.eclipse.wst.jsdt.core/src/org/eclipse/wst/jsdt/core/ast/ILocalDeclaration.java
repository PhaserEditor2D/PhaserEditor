/*******************************************************************************
 * Copyright (c) 2005, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.wst.jsdt.core.ast;

import org.eclipse.wst.jsdt.internal.compiler.lookup.InvocationSite;
import org.eclipse.wst.jsdt.internal.compiler.lookup.LocalVariableBinding;

/**
 *  
 *  Representation of a local var declaration.
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */

public interface ILocalDeclaration extends InvocationSite, IAbstractVariableDeclaration {
	public void setBinding(LocalVariableBinding binding);
	public LocalVariableBinding getBinding();
	/**
	 * Get the initialization expression of the var as an assignment
	 * @return initialization assignment expression or null
	 */
	public IAssignment getAssignment();
	public void setIsLocal(boolean isLocal);
	public boolean isLocal();
}