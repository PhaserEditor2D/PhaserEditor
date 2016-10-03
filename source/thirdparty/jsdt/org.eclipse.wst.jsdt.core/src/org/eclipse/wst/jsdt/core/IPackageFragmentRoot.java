/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     IBM Corporation - specified that a source archive or a source folder can be attached to a binary
 *                               package fragment root.
 *     IBM Corporation - added root manipulation APIs: copy, delete, move
 *     IBM Corporation - added DESTINATION_PROJECT_INCLUDEPATH
 *     IBM Corporation - added OTHER_REFERRING_PROJECTS_INCLUDEPATH
 *     IBM Corporation - added NO_RESOURCE_MODIFICATION
 *     IBM Corporation - added REPLACE
 *     IBM Corporation - added ORIGINATING_PROJECT_INCLUDEPATH
 *******************************************************************************/
package org.eclipse.wst.jsdt.core;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * A package fragment root (or source folder root) contains a set of source folders (package fragments).
 * It corresponds to an underlying resource which is either folder.  All descendant folders represent
 * package fragments.  For a given child folder representing a package fragment,
 * the corresponding package name is composed of the folder names between the folder
 * for this root and the child folder representing the package, separated by '.'.
 * Package fragment roots need to be opened before they can be navigated or manipulated.
 * The children are of type <code>IPackageFragment</code>, and are in no particular order.
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 *  
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public interface IPackageFragmentRoot
	extends IParent, IJavaScriptElement, IOpenable {
	/**
	 * Kind constant for a source path root. Indicates this root
	 * only contains source files.
	 */
	int K_SOURCE = 1;
	/**
	 * Kind constant for a binary path root. Indicates this
	 * root only contains binary (non-editable) files.
	 */
	int K_BINARY = 2;
	/**
	 * Empty root path
	 */
	String DEFAULT_PACKAGEROOT_PATH = ""; //$NON-NLS-1$
	/**
	 * Update model flag constant (bit mask value 1) indicating that the operation
	 * is to not copy/move/delete the package fragment root resource.
	 */
	int NO_RESOURCE_MODIFICATION = 1;
	/**
	 * Update model flag constant (bit mask value 2) indicating that the operation
	 * is to update the includepath of the originating project.
	 */
	int ORIGINATING_PROJECT_INCLUDEPATH = 2;
	/**
	 * Update model flag constant (bit mask value 4) indicating that the operation
	 * is to update the includepath of all referring projects except the originating project.
	 */
	int OTHER_REFERRING_PROJECTS_INCLUDEPATH = 4;
	/**
	 * Update model flag constant (bit mask value 8) indicating that the operation
	 * is to update the includepath of the destination project.
	 */
	int DESTINATION_PROJECT_INCLUDEPATH = 8;
	/**
	 * Update model flag constant (bit mask value 16) indicating that the operation
	 * is to replace the resource and the destination project's includepath entry.
	 */
	int REPLACE = 16;
	/*
	 * Attaches the source archive identified by the given absolute path to this
	 * binary package fragment root. <code>rootPath</code> specifies the location
	 * of the root within the archive or folder (empty specifies the default root
	 * and <code>null</code> specifies the root path should be detected).
	 * Once a source archive or folder is attached to the package fragment root,
	 * the <code>getSource</code> and <code>getSourceRange</code>
	 * methods become operational for binary types/members.
	 * To detach a source archive or folder from a package fragment root, specify
	 * <code>null</code> as the source path.
	 *
	 * @param sourcePath the given absolute path to the source archive or folder
	 * @param rootPath specifies the location of the root within the archive
	 *              (empty specifies the default root and <code>null</code> specifies
	 *               automatic detection of the root path)
	 * @param monitor the given progress monitor
	 * @exception JavaScriptModelException if this operation fails. Reasons include:
	 * <ul>
	 * <li> This JavaScript element does not exist (ELEMENT_DOES_NOT_EXIST)</li>
	 * <li> A <code>CoreException</code> occurred while updating a server property
	 * <li> This package fragment root is not of kind binary (INVALID_ELEMENT_TYPES)
	 * <li> The path provided is not absolute (RELATIVE_PATH)
	 * </ul>
	 */
	void attachSource(IPath sourcePath, IPath rootPath, IProgressMonitor monitor)
		throws JavaScriptModelException;

	/**
	 * Copies the resource of this package fragment root to the destination path
	 * as specified by <code>IResource.copy(IPath, int, IProgressMonitor)</code>
	 * but excluding nested source folders.
	 * <p>
	 * If <code>NO_RESOURCE_MODIFICATION</code> is specified in
	 * <code>updateModelFlags</code> or if this package fragment root is external,
	 * this operation doesn't copy the resource. <code>updateResourceFlags</code>
	 * is then ignored.
	 * </p><p>
	 * If <code>DESTINATION_PROJECT_INCLUDEPATH</code> is specified in
	 * <code>updateModelFlags</code>, updates the includepath of the
	 * destination's project (if it is a JavaScript project). If a non-<code>null</code>
	 * sibling is specified, a copy of this root's includepath entry is inserted before the
	 * sibling on the destination project's raw includepath. If <code>null</code> is
	 * specified, the includepath entry is added at the end of the raw includepath.
	 * </p><p>
	 * If <code>REPLACE</code> is specified in <code>updateModelFlags</code>,
	 * overwrites the resource at the destination path if any.
	 * If the same includepath entry already exists on the destination project's raw
	 * includepath, then the sibling is ignored and the new includepath entry replaces the
	 * existing one.
	 * </p><p>
	 * If no flags is specified in <code>updateModelFlags</code> (using
	 * <code>IResource.NONE</code>), the default behavior applies: the
	 * resource is copied (if this package fragment root is not external) and the
	 * includepath is not updated.
	 * </p>
	 *
	 * @param destination the destination path
	 * @param updateResourceFlags bit-wise or of update resource flag constants
	 *   (<code>IResource.FORCE</code> and <code>IResource.SHALLOW</code>)
	 * @param updateModelFlags bit-wise or of update resource flag constants
	 *   (<code>DESTINATION_PROJECT_INCLUDEPATH</code> and
	 *   <code>NO_RESOURCE_MODIFICATION</code>)
	 * @param sibling the includepath entry before which a copy of the includepath
	 * entry should be inserted or <code>null</code> if the includepath entry should
	 * be inserted at the end
	 * @param monitor a progress monitor
	 *
	 * @exception JavaScriptModelException if this root could not be copied. Reasons
	 * include:
	 * <ul>
	 * <li> This root does not exist (ELEMENT_DOES_NOT_EXIST)</li>
	 * <li> A <code>CoreException</code> occurred while copying the
	 * resource or updating a includepath</li>
	 * <li>
	 * The destination is not inside an existing project and <code>updateModelFlags</code>
	 * has been specified as <code>DESTINATION_PROJECT_INCLUDEPATH</code>
	 * (INVALID_DESTINATION)</li>
	 * <li> The sibling is not a includepath entry on the destination project's
	 * raw includepath (INVALID_SIBLING)</li>
	 * <li> The same includepath entry already exists on the destination project's
	 * includepath (NAME_COLLISION) and <code>updateModelFlags</code>
	 * has not been specified as <code>REPLACE</code></li>
	 * </ul>
	 * @see org.eclipse.core.resources.IResource#copy(IPath, boolean, IProgressMonitor)
	 */
	void copy(IPath destination, int updateResourceFlags, int updateModelFlags, IIncludePathEntry sibling, IProgressMonitor monitor) throws JavaScriptModelException;
	/**
	 * Creates and returns a package fragment in this root with the
	 * given dot-separated package name.  An empty string specifies the default package.
	 * This has the side effect of creating all package
	 * fragments that are a prefix of the new package fragment which
	 * do not exist yet. If the package fragment already exists, this
	 * has no effect.
	 *
	 * For a description of the <code>force</code> flag, see <code>IFolder.create</code>.
	 *
	 * @param name the given dot-separated package name
	 * @param force a flag controlling how to deal with resources that
	 *    are not in sync with the local file system
	 * @param monitor the given progress monitor
	 * @exception JavaScriptModelException if the element could not be created. Reasons include:
	 * <ul>
	 * <li> This JavaScript element does not exist (ELEMENT_DOES_NOT_EXIST)</li>
	 * <li> A <code>CoreException</code> occurred while creating an underlying resource
	 * <li> This package fragment root is read only (READ_ONLY)
	 * <li> The name is not a valid package name (INVALID_NAME)
	 * </ul>
	 * @return a package fragment in this root with the given dot-separated package name
	 * @see org.eclipse.core.resources.IFolder#create(boolean, boolean, IProgressMonitor)
	 */
	IPackageFragment createPackageFragment(
		String name,
		boolean force,
		IProgressMonitor monitor)
		throws JavaScriptModelException;
	/**
	 * Deletes the resource of this package fragment root as specified by
	 * <code>IResource.delete(int, IProgressMonitor)</code> but excluding nested
	 * source folders.
	 * <p>
	 * If <code>NO_RESOURCE_MODIFICATION</code> is specified in
	 * <code>updateModelFlags</code> or if this package fragment root is external,
	 * this operation doesn't delete the resource. <code>updateResourceFlags</code>
	 * is then ignored.
	 * </p><p>
	 * If <code>ORIGINATING_PROJECT_INCLUDEPATH</code> is specified in
	 * <code>updateModelFlags</code>, update the raw includepath of this package
	 * fragment root's project by removing the corresponding includepath entry.
	 * </p><p>
	 * If <code>OTHER_REFERRING_PROJECTS_INCLUDEPATH</code> is specified in
	 * <code>updateModelFlags</code>, update the raw includepaths of all other JavaScript
	 * projects referring to this root's resource by removing the corresponding includepath
	 * entries.
	 * </p><p>
	 * If no flags is specified in <code>updateModelFlags</code> (using
	 * <code>IResource.NONE</code>), the default behavior applies: the
	 * resource is deleted (if this package fragment root is not external) and no
	 * includepaths are updated.
	 * </p>
	 *
	 * @param updateResourceFlags bit-wise or of update resource flag constants
	 *   (<code>IResource.FORCE</code> and <code>IResource.KEEP_HISTORY</code>)
	 * @param updateModelFlags bit-wise or of update resource flag constants
	 *   (<code>ORIGINATING_PROJECT_INCLUDEPATH</code>,
	 *   <code>OTHER_REFERRING_PROJECTS_INCLUDEPATH</code> and
	 *   <code>NO_RESOURCE_MODIFICATION</code>)
	 * @param monitor a progress monitor
	 *
	 * @exception JavaScriptModelException if this root could not be deleted. Reasons
	 * include:
	 * <ul>
	 * <li> This root does not exist (ELEMENT_DOES_NOT_EXIST)</li>
	 * <li> A <code>CoreException</code> occurred while deleting the resource
	 * or updating a includepath
	 * </li>
	 * </ul>
	 * @see org.eclipse.core.resources.IResource#delete(boolean, IProgressMonitor)
	 */
	void delete(int updateResourceFlags, int updateModelFlags, IProgressMonitor monitor) throws JavaScriptModelException;
	/**
	 * Returns this package fragment root's kind encoded as an integer.
	 * A package fragment root can contain source files (i.e. files with one
	 * of the {@link JavaScriptCore#getJavaScriptLikeExtensions() JavaScript-like extensions},
	 * or <code>.class</code> files, but not both.
	 * If the underlying folder or archive contains other kinds of files, they are ignored.
	 * In particular, <code>.class</code> files are ignored under a source package fragment root,
	 * and source files are ignored under a binary package fragment root.
	 *
	 * @exception JavaScriptModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource.
	 * @return this package fragment root's kind encoded as an integer
	 * @see IPackageFragmentRoot#K_SOURCE
	 * @see IPackageFragmentRoot#K_BINARY
	 */
	int getKind() throws JavaScriptModelException;

	/**
	 * Returns an array of non-JavaScript resources contained in this package fragment root.
	 * <p>
	 * Non-JavaScript resources includes other files and folders located in the same
	 * directories as the compilation units or class files under this package
	 * fragment root. Resources excluded from this package fragment root
	 * by virtue of inclusion/exclusion patterns on the corresponding source includepath
	 * entry are considered non-JavaScript resources and will appear in the result
	 * (possibly in a folder). Thus when a nested source folder is excluded, it will appear
	 * in the non-JavaScript resources of the outer folder.
	 * </p>
	 * @return an array of non-JavaScript resources (<code>IFile</code>s,
	 *              <code>IFolder</code>s, or <code>IStorage</code>s if the
	 *              package fragment root is in archive) contained in this package
	 *              fragment root
	 * @exception JavaScriptModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource.
	 * @see IIncludePathEntry#getInclusionPatterns()
	 * @see IIncludePathEntry#getExclusionPatterns()
	 */
	Object[] getNonJavaScriptResources() throws JavaScriptModelException;

	/**
	 * Returns the package fragment with the given package name.
	 * An empty string indicates the default package.
	 * This is a handle-only operation.  The package fragment
	 * may or may not exist.
	 *
	 * @param packageName the given package name
	 * @return the package fragment with the given package name
	 */
	IPackageFragment getPackageFragment(String packageName);


	/**
	 * Returns the first raw includepath entry that corresponds to this package
	 * fragment root.
	 * A raw includepath entry corresponds to a package fragment root if once resolved
	 * this entry's path is equal to the root's path.
	 *
	 * @exception JavaScriptModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource.
	 * @return the first raw includepath entry that corresponds to this package fragment root
	 */
	IIncludePathEntry getRawIncludepathEntry() throws JavaScriptModelException;

	/**
	 * Returns the absolute path to the source archive attached to
	 * this package fragment root's binary archive.
	 *
	 * @return the absolute path to the corresponding source archive,
	 *   or <code>null</code> if this package fragment root's binary archive
	 *   has no corresponding source archive, or if this package fragment root
	 *   is not a binary archive
	 * @exception JavaScriptModelException if this operation fails
	 */
	IPath getSourceAttachmentPath() throws JavaScriptModelException;

	/**
	 * Returns the path within this package fragment root's source archive.
	 * An empty path indicates that packages are located at the root of the
	 * source archive.
	 *
	 * @return the path within the corresponding source archive,
	 *   or <code>null</code> if this package fragment root's binary archive
	 *   has no corresponding source archive, or if this package fragment root
	 *   is not a binary archive
	 * @exception JavaScriptModelException if this operation fails
	 */
	IPath getSourceAttachmentRootPath() throws JavaScriptModelException;

	/**
	 * Returns whether this package fragment root's underlying
	 * resource is a binary archive (a JAR or zip file).
	 * <p>
	 * This is a handle-only method.
	 * </p>
	 *
	 * @return true if this package fragment root's underlying resource is a binary archive, false otherwise
	 */
	public boolean isArchive();

	public boolean isLanguageRuntime();
	/**
	 * Returns whether this package fragment root is external
	 * to the workbench (that is, a local file), and has no
	 * underlying resource.
	 * <p>
	 * This is a handle-only method.
	 * </p>
	 *
	 * @return true if this package fragment root is external
	 * to the workbench (that is, a local file), and has no
	 * underlying resource, false otherwise
	 */
	boolean isExternal();

	/**
	 * Moves the resource of this package fragment root to the destination path
	 * as specified by <code>IResource.move(IPath,int,IProgressMonitor)</code>
	 * but excluding nested source folders.
	 * <p>
	 * If <code>NO_RESOURCE_MODIFICATION</code> is specified in
	 * <code>updateModelFlags</code> or if this package fragment root is external,
	 * this operation doesn't move the resource. <code>updateResourceFlags</code>
	 * is then ignored.
	 * </p><p>
	 * If <code>DESTINATION_PROJECT_INCLUDEPATH</code> is specified in
	 * <code>updateModelFlags</code>, updates the includepath of the
	 * destination's project (if it is a JavaScript project). If a non-<code>null</code>
	 * sibling is specified, a copy of this root's includepath entry is inserted before the
	 * sibling on the destination project's raw includepath. If <code>null</code> is
	 * specified, the includepath entry is added at the end of the raw includepath.
	 * </p><p>
	 * If <code>ORIGINATING_PROJECT_INCLUDEPATH</code> is specified in
	 * <code>updateModelFlags</code>, update the raw includepath of this package
	 * fragment root's project by removing the corresponding includepath entry.
	 * </p><p>
	 * If <code>OTHER_REFERRING_PROJECTS_INCLUDEPATH</code> is specified in
	 * <code>updateModelFlags</code>, update the raw includepaths of all other JavaScript
	 * projects referring to this root's resource by removing the corresponding includepath
	 * entries.
	 * </p><p>
	 * If <code>REPLACE</code> is specified in <code>updateModelFlags</code>,
	 * overwrites the resource at the destination path if any.
	 * If the same includepath entry already exists on the destination project's raw
	 * includepath, then the sibling is ignored and the new includepath entry replaces the
	 * existing one.
	 * </p><p>
	 * If no flags is specified in <code>updateModelFlags</code> (using
	 * <code>IResource.NONE</code>), the default behavior applies: the
	 * resource is moved (if this package fragment root is not external) and no
	 * includepaths are updated.
	 * </p>
	 *
	 * @param destination the destination path
	 * @param updateResourceFlags bit-wise or of update flag constants
	 * (<code>IResource.FORCE</code>, <code>IResource.KEEP_HISTORY</code>
	 * and <code>IResource.SHALLOW</code>)
	 * @param updateModelFlags bit-wise or of update resource flag constants
	 *   (<code>DESTINATION_PROJECT_INCLUDEPATH</code>,
	 *   <code>ORIGINATING_PROJECT_INCLUDEPATH</code>,
	 *   <code>OTHER_REFERRING_PROJECTS_INCLUDEPATH</code> and
	 *   <code>NO_RESOURCE_MODIFICATION</code>)
	 * @param sibling the includepath entry before which a copy of the includepath
	 * entry should be inserted or <code>null</code> if the includepath entry should
	 * be inserted at the end
	 * @param monitor a progress monitor
	 *
	 * @exception JavaScriptModelException if this root could not be moved. Reasons
	 * include:
	 * <ul>
	 * <li> This root does not exist (ELEMENT_DOES_NOT_EXIST)</li>
	 * <li> A <code>CoreException</code> occurred while copying the
	 * resource or updating a includepath</li>
	 * <li>
	 * The destination is not inside an existing project and <code>updateModelFlags</code>
	 * has been specified as <code>DESTINATION_PROJECT_INCLUDEPATH</code>
	 * (INVALID_DESTINATION)</li>
	 * <li> The sibling is not a includepath entry on the destination project's
	 * raw includepath (INVALID_SIBLING)</li>
	 * <li> The same includepath entry already exists on the destination project's
	 * includepath (NAME_COLLISION) and <code>updateModelFlags</code>
	 * has not been specified as <code>REPLACE</code></li>
	 * </ul>
	 * @see org.eclipse.core.resources.IResource#move(IPath, boolean, IProgressMonitor)
	 */
	void move(IPath destination, int updateResourceFlags, int updateModelFlags, IIncludePathEntry sibling, IProgressMonitor monitor) throws JavaScriptModelException;

	public IIncludePathAttribute[] getIncludepathAttributes();

	public IIncludePathEntry getResolvedIncludepathEntry() throws JavaScriptModelException;

	public boolean isLibrary();
}
