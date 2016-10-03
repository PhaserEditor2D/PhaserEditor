/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.core.infer;

import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;

/**
 *  Support for extending the automatic import insertion mechanisms
 *  
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public class ImportRewriteSupport {

	/**
	 * @return true if the import value matches a type name, false if the import value is a file path value
	 */
	public boolean isImportMatchesType()
	{
		return true;
	}
	
	/**
	 * @return true if existing imports should be modified if necessary
	 */
	public boolean isRewriteExisting()
	{
		return true;
	}
	
	/**
	 * @return the string which represents the import
	 */
	public String getImportString(String importName, boolean isStatic, String lineDelim)
	{
		return null;
	}

	/**
	 * Find the starting position of an import when none already exist
	 * @return the starting position, -1 for default action
	 */
	public int getImportStartPosition(JavaScriptUnit root) {
		return -1;
	}
}
