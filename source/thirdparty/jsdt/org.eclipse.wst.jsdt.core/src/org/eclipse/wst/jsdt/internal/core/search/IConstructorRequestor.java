/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.core.search;

import org.eclipse.wst.jsdt.internal.compiler.env.AccessRestriction;

/**
 * A <code>IConstructorRequestor</code> collects search results from a <code>searchAllConstructorDeclarations</code>
 * query to a <code>SearchBasicEngine</code> providing restricted access information of declaring type when a constructor is accepted.
 */
public interface IConstructorRequestor {

	/**
	 * <p>Accepts a constructor found during an index search.</p>
	 * 
	 * @param modifiers Modifiers to the constructor such as public/private
	 * @param typeName Name of the type the constructor is for
	 * @param parameterCount Number of parameters for the constructor, or -1 for a default constructor
	 * @param parameterTypes Type names of the parameters, should be same length as <code>parameterCount</code>
	 * @param parameterNames Names of the parameters, should be same length as <code>parameterCount</code>
	 * @param path to the document containing the constructor match
	 * @param access Accessibility of the constructor
	 */
	public void acceptConstructor(
			int modifiers,
			char[] typeName,
			int parameterCount,
			char[][] parameterTypes,
			char[][] parameterNames,
			String path,
			AccessRestriction access);
}
