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

/**
 * <p>Requester to use when requesting variable matches.</p>
 */

public interface IVariableRequester {
	
	/**
	 * <p>Accept a variable defined with all of the given information.</p>
	 * 
	 * @param signature
	 * @param typeQualification
	 * @param typeSimpleName
	 * @param declaringQualification
	 * @param declaringSimpleName
	 * @param modifiers
	 * @param path
	 */
	public void acceptVariable(
				char[] signature,
				char[] typeQualification,
				char[] typeSimpleName,
				char[] declaringQualification,
				char[] declaringSimpleName,
				int modifiers,
				String path);
}
