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
package org.eclipse.wst.jsdt.launching;

import java.net.URL;

import org.eclipse.core.runtime.IPath;


/**
 * The location of a library (for example rt.jar).
 * <p>
 * Clients may instantiate this class; it is not intended to be subclassed.
 * </p>
 * 
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public final class LibraryLocation {
	private IPath fSystemLibrary;
	private IPath fSystemLibrarySource;
	private IPath fPackageRootPath;
	private URL fJavadocLocation;
	
	/**
	 * Creates a new library location.
	 * 
	 * @param libraryPath	The location of the JAR containing java.lang.Object
	 * 					Must not be <code>null</code>.
	 * @param sourcePath	The location of the zip file containing the sources for <code>library</code>
	 * 					Must not be <code>null</code> (Use Path.EMPTY instead)
	 * @param packageRoot The path inside the <code>source</code> zip file where packages names
	 * 					  begin. If the source for java.lang.Object source is found at 
	 * 					  "src/java/lang/Object.java" in the zip file, the 
	 * 					  packageRoot should be "src"
	 * 					  Must not be <code>null</code>. (Use Path.EMPTY or IPath.ROOT)
	 * @throws	IllegalArgumentException	If the library path is <code>null</code>.
	 */	
	public LibraryLocation(IPath libraryPath, IPath sourcePath, IPath packageRoot) {
		this(libraryPath, sourcePath, packageRoot, null);
	}

	/**
	 * Creates a new library location.
	 * 
	 * @param libraryPath	The location of the JAR containing java.lang.Object
	 * 					Must not be <code>null</code>.
	 * @param sourcePath	The location of the zip file containing the sources for <code>library</code>
	 * 					Must not be <code>null</code> (Use Path.EMPTY instead)
	 * @param packageRoot The path inside the <code>source</code> zip file where packages names
	 * 					  begin. If the source for java.lang.Object source is found at 
	 * 					  "src/java/lang/Object.java" in the zip file, the 
	 * 					  packageRoot should be "src"
	 * 					  Must not be <code>null</code>. (Use Path.EMPTY or IPath.ROOT)
	 * @param javadocLocation The location of the jsdoc for <code>library</code>
	 * @throws	IllegalArgumentException	If the library path is <code>null</code>.
	 *  
	 */	
	public LibraryLocation(IPath libraryPath, IPath sourcePath, IPath packageRoot, URL javadocLocation) {
		if (libraryPath == null)
			throw new IllegalArgumentException(LaunchingMessages.libraryLocation_assert_libraryNotNull); 

		fSystemLibrary= libraryPath;
		fSystemLibrarySource= sourcePath;
		fPackageRootPath= packageRoot;
		fJavadocLocation= javadocLocation;
	}		
		
	/**
	 * Returns the JRE library jar location.
	 * 
	 * @return The JRE library jar location.
	 */
	public IPath getSystemLibraryPath() {
		return fSystemLibrary;
	}
	
	/**
	 * Returns the JRE library source zip location.
	 * 
	 * @return The JRE library source zip location.
	 */
	public IPath getSystemLibrarySourcePath() {
		return fSystemLibrarySource;
	}	
	
	/**
	 * Returns the path to the default package in the sources zip file
	 * 
	 * @return The path to the default package in the sources zip file.
	 */
	public IPath getPackageRootPath() {
		return fPackageRootPath;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (obj instanceof LibraryLocation) {
			LibraryLocation lib = (LibraryLocation)obj;
			return getSystemLibraryPath().equals(lib.getSystemLibraryPath()) 
				&& equals(getSystemLibrarySourcePath(), lib.getSystemLibrarySourcePath())
				&& equals(getPackageRootPath(), lib.getPackageRootPath())
				&& equalsOrNull(getJavadocLocation(), lib.getJavadocLocation());
		} 
		return false;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return getSystemLibraryPath().hashCode();
	}
	
	/**
	 * Returns whether the given paths are equal - either may be <code>null</code>.
	 * @param path1 path to be compared
	 * @param path2 path to be compared
	 * @return whether the given paths are equal
	 */
	protected boolean equals(IPath path1, IPath path2) {
		return equalsOrNull(path1, path2);
	}
	
	/**
	 * Returns whether the given objects are equal - either may be <code>null</code>.
	 * @param o1 object to be compared
	 * @param o2 object to be compared
	 * @return whether the given objects are equal or both null
	 *  
	 */	
	private boolean equalsOrNull(Object o1, Object o2) {
		if (o1 == null) {
			return o2 == null;
		}
		if (o2 == null) {
			return false;
		}
		return o1.equals(o2);
	}

	/**
	 * Returns the jsdoc location associated with this Library location.
	 * 
	 * @return a url pointing to the jsdoc location associated with
	 * 	this Library location, or <code>null</code> if none
	 *  
	 */
	public URL getJavadocLocation() {
		return fJavadocLocation;
	}
	
}
