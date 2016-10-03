/*******************************************************************************
 * Copyright (c) 2011, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.core.search;

/**
 * <p>Requester to use when requesting function matches.</p>
 */
public interface IFunctionRequester {

	/**
	 * <p>Accept a function defined with all of the given information.</p>
	 * 
	 * @param signature
	 * @param parameterFullyQualifiedTypeNames
	 * @param parameterNames
	 * @param returnQualification
	 * @param returnSimpleName
	 * @param declaringQualification
	 * @param declaringSimpleName
	 * @param modifiers
	 * @param path
	 */
	public void acceptFunction(
				char[] signature,
				char[][] parameterFullyQualifiedTypeNames,
				char[][] parameterNames,
				char[] returnQualification,
				char[] returnSimpleName,
				char[] declaringQualification,
				char[] declaringSimpleName,
				int modifiers,
				String path);
}
