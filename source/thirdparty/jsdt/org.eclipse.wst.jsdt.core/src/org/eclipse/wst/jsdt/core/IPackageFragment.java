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
package org.eclipse.wst.jsdt.core;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * A package fragment (or source folder) is a portion of the workspace corresponding to an entire package,
 * or to a portion thereof. The distinction between a package fragment and a package
 * is that a package with some name is the union of all package fragments in the includepath
 * which have the same name.
 * <p>
 * Package fragments elements need to be opened before they can be navigated or manipulated.
 * The children are of type <code>IJavaScriptUnit</code> (representing a source file) or
 * <code>IClassFile</code> (representing a read-only file).
 * The children are listed in no particular order.
 * </p>
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 *  
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public interface IPackageFragment extends IParent, IJavaScriptElement, IOpenable, ISourceManipulation {

	/**
	 * <p>
	 * The name of package fragment for the default package (value: the empty
	 * string, <code>""</code>).
	 * </p>
 	*/
	public static final String DEFAULT_PACKAGE_NAME = ""; //$NON-NLS-1$
	/**
	 * Returns whether this fragment contains at least one JavaScript resource.
	 * @return true if this fragment contains at least one JavaScript resource, false otherwise
	 * @exception JavaScriptModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource.
	 */
	boolean containsJavaResources() throws JavaScriptModelException;
	/**
	 * Creates and returns a javaScript unit in this package fragment
	 * with the specified name and contents. No verification is performed
	 * on the contents.
	 *
	 * <p>It is possible that a javaScript unit with the same name already exists in this
	 * package fragment.
	 * The value of the <code>force</code> parameter effects the resolution of
	 * such a conflict:<ul>
	 * <li> <code>true</code> - in this case the compilation is created with the new contents</li>
	 * <li> <code>false</code> - in this case a <code>JavaScriptModelException</code> is thrown</li>
	 * </ul>
	 *
	 * @param contents the given contents
	 * @param force specify how to handle conflict is the same name already exists
	 * @param monitor the given progress monitor
	 * @param name the given name
	 * @exception JavaScriptModelException if the element could not be created. Reasons include:
	 * <ul>
	 * <li> This JavaScript element does not exist (ELEMENT_DOES_NOT_EXIST)</li>
	 * <li> A <code>CoreException</code> occurred while creating an underlying resource
	 * <li> The name is not a valid javaScript unit name (INVALID_NAME)
	 * <li> The contents are <code>null</code> (INVALID_CONTENTS)
	 * </ul>
	 * @return a javaScript unit in this package fragment
	 * with the specified name and contents
	 */
	IJavaScriptUnit createCompilationUnit(String name, String contents, boolean force, IProgressMonitor monitor) throws JavaScriptModelException;
	/**
	 * Returns the non-editable file with the specified name
	 * in this folder .
	 * This is a handle-only method.  The file may or may not be present.
	 * @param name the given name
	 * @return the file with the specified name in this package
	 */
	IClassFile getClassFile(String name);
	/**
	 * Returns all of the non-editable files in this source folder.
	 *
	 * <p>Note: it is possible that a package fragment contains only
	 * javaScript units (in other words, its kind is <code>K_SOURCE</code>), in
	 * which case this method returns an empty collection.
	 *
	 * @exception JavaScriptModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource.
	 * @return all of the files in this source folder
	 */
	IClassFile[] getClassFiles() throws JavaScriptModelException;
	/**
	 * Returns the javaScript unit with the specified name
	 * in this package (for example, <code>"Object.js"</code>).
	 * The name has to be a valid javaScript unit name.
	 * This is a handle-only method.  The javaScript unit may or may not be present.
	 *
	 * @param name the given name
	 * @return the javaScript unit with the specified name in this package
	 * @see JavaScriptConventions#validateCompilationUnitName(String name, String sourceLevel, String complianceLevel)
	 */
	IJavaScriptUnit getJavaScriptUnit(String name);
	/**
	 * Returns all of the javaScript units in this source folder.
	 *
	 * <p>Note: it is possible that a source folder contains only
	 * read-only files (in other words, its kind is <code>K_BINARY</code>), in which
	 * case this method returns an empty collection.
	 * </p>
	 *
	 * @exception JavaScriptModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource.
	 * @return all of the javaScript units in this source folder
	 */
	IJavaScriptUnit[] getJavaScriptUnits() throws JavaScriptModelException;
	/**
	 * Returns all of the javaScript units in this source folder that are
	 * in working copy mode and that have the given owner.
	 * <p>
	 * Only existing working copies are returned. So a javaScript unit handle that has no
	 * corresponding resource on disk will be included if and only if is in working copy mode.
	 * </p>
	 * <p>Note: it is possible that a source folder contains only
	 * read-only files (in other words, its kind is <code>K_BINARY</code>), in which
	 * case this method returns an empty collection.
	 * </p>
	 *
	 * @param owner the owner of the returned javaScript units
	 * @exception JavaScriptModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource.
	 * @return all of the javaScript units in this source folder
	 */
	IJavaScriptUnit[] getJavaScriptUnits(WorkingCopyOwner owner) throws JavaScriptModelException;
	/**
	 * Returns the dot-separated package name of this fragment, for example
	 * <code>"java.lang"</code>, or <code>""</code> (the empty string),
	 * for the default package.
	 *
	 * @return the dot-separated package name of this fragment
	 */
	String getElementName();
	/**
	 * Returns this package fragment's root kind encoded as an integer.
	 * A package fragment can contain source files (i.e. files with one of
	 * the {@link JavaScriptCore#getJavaScriptLikeExtensions() JavaScript-like extensions}),
	 * or <code>.class</code> files. This is a convenience method.
	 *
	 * @exception JavaScriptModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource.
	 * @return this package fragment's root kind encoded as an integer
	 * @see IPackageFragmentRoot#K_SOURCE
	 * @see IPackageFragmentRoot#K_BINARY
	 */
	int getKind() throws JavaScriptModelException;
	/**
	 * Returns an array of non-JavaScript resources contained in this source folder.
	 * <p>
	 * Non-JavaScript resources includes other files and folders located in the same
	 * directory as the javaScript units   for this package
	 * fragment. Source files excluded from this package by virtue of
	 * inclusion/exclusion patterns on the corresponding source includepath entry
	 * are considered non-JavaScript resources and will appear in the result
	 * (possibly in a folder).
	 * </p>
	 *
	 * @exception JavaScriptModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource.
	 * @return an array of non-JavaScript resources (<code>IFile</code>s,
	 *              <code>IFolder</code>s, or <code>IStorage</code>s if the
	 *              package fragment is in an archive) contained in this package
	 *              fragment
	 * @see IIncludePathEntry#getInclusionPatterns()
	 * @see IIncludePathEntry#getExclusionPatterns()
	 */
	Object[] getNonJavaScriptResources() throws JavaScriptModelException;
	/**
	 * Returns whether this package fragment's name is
	 * a prefix of other package fragments in this package fragment's
	 * root.
	 *
	 * @exception JavaScriptModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource.
	 * @return true if this package fragment's name is a prefix of other package fragments in this package fragment's root, false otherwise
	 */
	boolean hasSubpackages() throws JavaScriptModelException;
	/**
	 * Returns whether this package fragment is a default package.
	 * This is a handle-only method.
	 *
	 * @return true if this package fragment is a default package
	 */
	boolean isDefaultPackage();

	boolean isSource();
}
