/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.core.dom;

/**
 * Common interface for AST nodes that represent modifiers or
 * annotations.
 * <pre>
 * ExtendedModifier:
 *   Modifier
 *   Annotation
 * </pre>
 * 
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public interface IExtendedModifier {

	/**
	 * Returns whether this extended modifier is a standard modifier.
	 *
	 * @return <code>true</code> if this is a standard modifier
	 * (instance of {@link Modifier}), and <code>false</code> otherwise
	 */
	public boolean isModifier();
}

