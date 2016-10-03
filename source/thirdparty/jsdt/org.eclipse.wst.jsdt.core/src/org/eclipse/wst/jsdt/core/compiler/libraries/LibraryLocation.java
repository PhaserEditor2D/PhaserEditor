/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.core.compiler.libraries;

import org.eclipse.core.runtime.IPath;
/**
 *  
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public interface LibraryLocation {

	/**
	 * @param name
	 * @return path to the given library name
	 */
	public String getLibraryPath(String name);

	/**
	 * @return a list of files in the library
	 */
	public char[][] getLibraryFileNames();

	/**
	 * @return relative path within the plugin library files are stored
	 */
	public IPath getLibraryPathInPlugin();

	/**
	 * @return working location to store library files.
	 */
	public IPath getWorkingLibPath();

	/**
	 * @param name
	 * @return  path to the given library name
	 */
	public String getLibraryPath(char[] name);
}