/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.core.infer;

/**
 *  This class provides configuration information for when the inferred class
 *  gets resolved
 * 
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public class ResolutionConfiguration {

	
	/**
	 * Get the default list of files to be looked at when resolving
	 * a name 
	 * @return a list of file paths as strings, relative to the JavaScript project root 
	 */
	public String [] getContextIncludes()
	{
		return null;
	}
	
	/**
	 * Determine if all files in include path should be searched to resolve a name.
	 * If false, names will be resolved using only libraries, imports, and context includes 
	 * 
	 * @return true 
	 */
	public boolean searchAllFiles()
	{
		return true;
	}
	
}
