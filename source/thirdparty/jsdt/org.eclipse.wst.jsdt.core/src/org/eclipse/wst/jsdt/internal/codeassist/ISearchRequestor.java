/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.codeassist;

import org.eclipse.wst.jsdt.core.Flags;
import org.eclipse.wst.jsdt.core.IAccessRule;
import org.eclipse.wst.jsdt.internal.compiler.env.AccessRestriction;

/**
 * This is the internal requestor passed to the searchable name environment
 * so as to process the multiple search results as they are discovered.
 *
 * It is used to allow the code assist engine to add some more information
 * to the raw name environment results before answering them to the UI.
 */
public interface ISearchRequestor {
	/**
	 * One result of the search consists of a new type.
	 *
	 * NOTE - All package and type names are presented in their readable form:
	 *    Package names are in the form "a.b.c".
	 *    Nested type names are in the qualified form "A.I".
	 *    The default package is represented by an empty array.
	 */
	public void acceptType(char[] packageName, char [] fileName, char[] typeName, char[][] enclosingTypeNames, int modifiers, AccessRestriction accessRestriction);
	public void acceptBinding(char[] packageName, char [] fileName, char[] bindingName, int bindingType, int modifiers, AccessRestriction accessRestriction);

	/**
	 * One result of the search consists of a new package.
	 *
	 * NOTE - All package names are presented in their readable form:
	 *    Package names are in the form "a.b.c".
	 *    The default package is represented by an empty array.
	 */
	public void acceptPackage(char[] packageName);
	
	/**
	 * <p>Accepts a constructor found during an index search.</p>
	 * 
	 * @param modifiers Modifiers to the constructor such as public/private
	 * @param typeName Name of the type the constructor is for
	 * @param parameterTypes Type names of the parameters, should be same length as <code>parameterCount</code>
	 * @param parameterNames Names of the parameters, should be same length as <code>parameterCount</code>
	 * @param path to the document containing the constructor match
	 * @param access Accessibility of the constructor
	 * 
	 * @see Flags
	 * @see IAccessRule
	 */
	public void acceptConstructor(
			int modifiers,
			char[] typeName,
			char[][] parameterTypes,
			char[][] parameterNames,
			String path,
			AccessRestriction access);
	
	/**
	 * <p>Accept a function defined with all of the given information</p>
	 * 
	 * @param signature
	 * @param parameterFullyQualifedTypeNames
	 * @param parameterNames
	 * @param returnQualification
	 * @param returnSimpleName
	 * @param declaringQualification
	 * @param declaringSimpleName
	 * @param modifiers
	 * @param path
	 */
	public void acceptFunction(char[] signature,
			char[][] parameterFullyQualifedTypeNames,
			char[][] parameterNames,
			char[] returnQualification,
			char[] returnSimpleName,
			char[] declaringQualification,
			char[] declaringSimpleName,
			int modifiers,
			String path);
	
	/**
	 * <p>Accept a variable defined with all of the given information</p>
	 * 
	 * @param signature
	 * @param typeQualification
	 * @param typeSimpleName
	 * @param declaringQualification
	 * @param declaringSimpleName
	 * @param modifiers
	 * @param path
	 */
	public void acceptVariable(char[] signature,
			char[] typeQualification,
			char[] typeSimpleName,
			char[] declaringQualification,
			char[] declaringSimpleName,
			int modifiers,
			String path);
}
