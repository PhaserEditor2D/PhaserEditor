/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     IBM Corporation - added getOption(String, boolean), getOptions(boolean) and setOptions(Map)
 *     IBM Corporation - deprecated getPackageFragmentRoots(IIncludePathEntry) and
 *                               added findPackageFragmentRoots(IIncludePathEntry)
 *     IBM Corporation - added isOnClasspath(IResource)
 *     IBM Corporation - added setOption(String, String)
 *     IBM Corporation - added forceClasspathReload(IProgressMonitor)
 *******************************************************************************/
package org.eclipse.wst.jsdt.core;

import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.jsdt.core.eval.IEvaluationContext;

/**
 * A JavaScript project represents a view of a project resource in terms of JavaScript
 * elements such as package fragments, types, methods and fields.
 * A project may contain several source folders (package roots), which contain source folders (package fragments).
 * A package root corresponds to an underlying folder.
 * <p>
 * Each JavaScript project has a includepath, defining which folders contain source code and
 * where required libraries are located. A project that
 * references packages in another project can access the packages by including
 * the required project in a includepath entry. The JavaScript model will present the
 * source elements in the required project. The includepath format is a sequence of includepath entries
 * describing the location and contents of package fragment roots.
 * </p>
 * JavaScript project elements need to be opened before they can be navigated or manipulated.
 * The children of a JavaScript project are the package fragment roots that are
 * defined by the includepath and contained in this project (in other words, it
 * does not include package fragment roots for other projects).
 * </p>
 * <p>
 * This interface is not intended to be implemented by clients. An instance
 * of one of these handles can be created via
 * <code>JavaScriptCore.create(project)</code>.
 * </p>
 *
 * @see JavaScriptCore#create(org.eclipse.core.resources.IProject)
 * @see IIncludePathEntry
 *  
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public interface IJavaScriptProject extends IParent, IJavaScriptElement, IOpenable {

	/* returns the projects scope file */
	IFile getJSDTScopeFile();
	
	/* returns the projects scope file */
	IFile getJSDTScopeFile(boolean forceCreate);
	
	/**
	 * Decodes the includepath entry that has been encoded in the given string
	 * in the context of this project.
	 * Returns null if the encoded entry is malformed.
	 *
	 * @param encodedEntry the encoded includepath entry
	 * @return the decoded includepath entry, or <code>null</code> if unable to decode it
	 */
	IIncludePathEntry decodeIncludepathEntry(String encodedEntry);

	/**
	 * Encodes the given includepath entry into a string in the context of this project.
	 *
	 * @param includepathEntry the includepath entry to encode
	 * @return the encoded includepath entry
	 */
	String encodeIncludepathEntry(IIncludePathEntry includepathEntry);

	/**
	 * Returns the <code>IJavaScriptElement</code> corresponding to the given
	 * includepath-relative path, or <code>null</code> if no such
	 * <code>IJavaScriptElement</code> is found. The result is one of an
	 * <code>IJavaScriptUnit</code>, <code>IClassFile</code>, or
	 * <code>IPackageFragment</code>.
	 * <p>
	 * When looking for a package fragment, there might be several potential
	 * matches; only one of them is returned.
	 *
	 * <p>For example, the path "java/lang/Object.js", would result in the
	 * <code>IJavaScriptUnit</code> or <code>IClassFile</code> corresponding to
	 * "java.lang.Object". The path "java/lang" would result in the
	 * <code>IPackageFragment</code> for "java.lang".
	 * @param path the given includepath-relative path
	 * @exception JavaScriptModelException if the given path is <code>null</code>
	 *  or absolute
	 * @return the <code>IJavaScriptElement</code> corresponding to the given
	 * includepath-relative path, or <code>null</code> if no such
	 * <code>IJavaScriptElement</code> is found
	 */
	IJavaScriptElement findElement(IPath path) throws JavaScriptModelException;

	/**
	 * Returns the <code>IJavaScriptElement</code> corresponding to the given
	 * includepath-relative path, or <code>null</code> if no such
	 * <code>IJavaScriptElement</code> is found. The result is one of an
	 * <code>IJavaScriptUnit</code>, <code>IClassFile</code>, or
	 * <code>IPackageFragment</code>. If it is an <code>IJavaScriptUnit</code>,
	 * its owner is the given owner.
	 * <p>
	 * When looking for a package fragment, there might be several potential
	 * matches; only one of them is returned.
	 *
	 * <p>For example, the path "java/lang/Object.js", would result in the
	 * <code>IJavaScriptUnit</code> or <code>IClassFile</code> corresponding to
	 * "java.lang.Object". The path "java/lang" would result in the
	 * <code>IPackageFragment</code> for "java.lang".
	 * @param path the given includepath-relative path
	 * @param owner the owner of the returned javaScript unit, ignored if it is
	 *   not a javaScript unit.
	 * @exception JavaScriptModelException if the given path is <code>null</code>
	 *  or absolute
	 * @return the <code>IJavaScriptElement</code> corresponding to the given
	 * includepath-relative path, or <code>null</code> if no such
	 * <code>IJavaScriptElement</code> is found
	 */
	IJavaScriptElement findElement(IPath path, WorkingCopyOwner owner) throws JavaScriptModelException;

	/**
	 * Returns the first existing package fragment on this project's includepath
	 * whose path matches the given (absolute) path, or <code>null</code> if none
	 * exist.
	 * The path can be:
	 * 	- internal to the workbench: "/Project/src"
	 *  - external to the workbench: "c:/jdk/classes.zip/java/lang"
	 * @param path the given absolute path
	 * @exception JavaScriptModelException if this project does not exist or if an
	 *		exception occurs while accessing its corresponding resource
	 * @return the first existing package fragment on this project's includepath
	 * whose path matches the given (absolute) path, or <code>null</code> if none
	 * exist
	 */
	IPackageFragment findPackageFragment(IPath path) throws JavaScriptModelException;

	/**
	 * Returns the existing package fragment root on this project's includepath
	 * whose path matches the given (absolute) path, or <code>null</code> if
	 * one does not exist.
	 * The path can be:
	 *	- internal to the workbench: "/Compiler/src"
	 *	- external to the workbench: "c:/jdk/classes.zip"
	 * @param path the given absolute path
	 * @exception JavaScriptModelException if this project does not exist or if an
	 *		exception occurs while accessing its corresponding resource
	 * @return the existing package fragment root on this project's includepath
	 * whose path matches the given (absolute) path, or <code>null</code> if
	 * one does not exist
	 */
	IPackageFragmentRoot findPackageFragmentRoot(IPath path)
		throws JavaScriptModelException;
	/**
	 * Returns the existing package fragment roots identified by the given entry.
	 * Note that a includepath entry that refers to another project may
	 * have more than one root (if that project has more than on root
	 * containing source), and includepath entries within the current
	 * project identify a single root.
	 * <p>
	 * If the includepath entry denotes a variable, it will be resolved and return
	 * the roots of the target entry (empty if not resolvable).
	 * <p>
	 * If the includepath entry denotes a container, it will be resolved and return
	 * the roots corresponding to the set of container entries (empty if not resolvable).
	 *
	 * @param entry the given entry
	 * @return the existing package fragment roots identified by the given entry
	 * @see IJsGlobalScopeContainer
	 */
	IPackageFragmentRoot[] findPackageFragmentRoots(IIncludePathEntry entry);
	/**
	 * Returns the first type found following this project's includepath
	 * with the given fully qualified name or <code>null</code> if none is found.
	 * The fully qualified name is a dot-separated name. For example,
	 * a class B defined as a member type of a class A in package x.y should have a
	 * the fully qualified name "x.y.A.B".
	 *
	 * Note that in order to be found, a type name (or its toplevel enclosing
	 * type name) must match its corresponding javaScript unit name. As a
	 * consequence, secondary types cannot be found using this functionality.
	 * To find secondary types use {@link #findType(String, IProgressMonitor)} instead.
	 *
	 * @param fullyQualifiedName the given fully qualified name
	 * @exception JavaScriptModelException if this project does not exist or if an
	 *		exception occurs while accessing its corresponding resource
	 * @return the first type found following this project's includepath
	 * with the given fully qualified name or <code>null</code> if none is found
	 * @see IType#getFullyQualifiedName(char)
	 */
	IType findType(String fullyQualifiedName) throws JavaScriptModelException;
	
	/**
	 * Returns all the types found following this project's include path with
	 * the given fully qualified name. The fully qualified name is a
	 * dot-separated name.
	 * 
	 * @param fullyQualifiedName
	 *            the given fully qualified name
	 * @exception JavaScriptModelException
	 *                if this project does not exist or if an exception occurs
	 *                while accessing its corresponding resource
	 * @return the types found following this project's include path with the
	 *         given fully qualified name
	 * @see IType#getFullyQualifiedName(char)
	 */
	IType[] findTypes(String fullyQualifiedName) throws JavaScriptModelException;
	
	/**
	 * Same functionality as {@link #findType(String)} but also look for secondary
	 * types if given name does not match a javaScript unit name.
	 *
	 * @param fullyQualifiedName the given fully qualified name
	 * @param progressMonitor the progress monitor to report progress to,
	 * 	or <code>null</code> if no progress monitor is provided
	 * @exception JavaScriptModelException if this project does not exist or if an
	 *		exception occurs while accessing its corresponding resource
	 * @return the first type found following this project's includepath
	 * with the given fully qualified name or <code>null</code> if none is found
	 * @see IType#getFullyQualifiedName(char)
	 */
	IType findType(String fullyQualifiedName, IProgressMonitor progressMonitor) throws JavaScriptModelException;
	/**
	 * Returns the first type found following this project's includepath
	 * with the given fully qualified name or <code>null</code> if none is found.
	 * The fully qualified name is a dot-separated name. For example,
	 * a class B defined as a member type of a class A in package x.y should have a
	 * the fully qualified name "x.y.A.B".
	 * If the returned type is part of a javaScript unit, its owner is the given
	 * owner.
	 *
	 * Note that in order to be found, a type name (or its toplevel enclosing
	 * type name) must match its corresponding javaScript unit name. As a
	 * consequence, secondary types cannot be found using this functionality.
	 * To find secondary types use {@link #findType(String, WorkingCopyOwner, IProgressMonitor)}
	 * instead.
	 *
	 * @param fullyQualifiedName the given fully qualified name
	 * @param owner the owner of the returned type's javaScript unit
	 * @exception JavaScriptModelException if this project does not exist or if an
	 *		exception occurs while accessing its corresponding resource
	 * @return the first type found following this project's includepath
	 * with the given fully qualified name or <code>null</code> if none is found
	 * @see IType#getFullyQualifiedName(char)
	 */
	IType findType(String fullyQualifiedName, WorkingCopyOwner owner) throws JavaScriptModelException;
	/**
	 * Returns all the types found following this project's include path with
	 * the given fully qualified name. The fully qualified name is a
	 * dot-separated name.
	 * 
	 * @param fullyQualifiedName
	 *            the given fully qualified name
	 * @param owner
	 *            the owner of the returned type's javaScript unit
	 * @exception JavaScriptModelException
	 *                if this project does not exist or if an exception occurs
	 *                while accessing its corresponding resource
	 * @return the types found following this project's include path with the
	 *         given fully qualified name
	 * @see IType#getFullyQualifiedName(char)
	 */
	IType[] findTypes(String fullyQualifiedName, WorkingCopyOwner owner) throws JavaScriptModelException;
	/**
	 * Same functionality as {@link #findType(String, WorkingCopyOwner)}
	 * but also look for secondary types if given name does not match
	 * a javaScript unit name.
	 *
	 * @param fullyQualifiedName the given fully qualified name
	 * @param owner the owner of the returned type's javaScript unit
	 * @param progressMonitor the progress monitor to report progress to,
	 * 	or <code>null</code> if no progress monitor is provided
	 * @exception JavaScriptModelException if this project does not exist or if an
	 *		exception occurs while accessing its corresponding resource
	 * @return the first type found following this project's includepath
	 * with the given fully qualified name or <code>null</code> if none is found
	 * @see IType#getFullyQualifiedName(char)
	 */
	IType findType(String fullyQualifiedName, WorkingCopyOwner owner, IProgressMonitor progressMonitor) throws JavaScriptModelException;
	/**
	 * Returns the first type found following this project's includepath
	 * with the given package name and type qualified name
	 * or <code>null</code> if none is found.
	 * The package name is a dot-separated name.
	 * The type qualified name is also a dot-separated name. For example,
	 * a class B defined as a member type of a class A should have the
	 * type qualified name "A.B".
	 *
	 * Note that in order to be found, a type name (or its toplevel enclosing
	 * type name) must match its corresponding javaScript unit name. As a
	 * consequence, secondary types cannot be found using this functionality.
	 * To find secondary types use {@link #findType(String, String, IProgressMonitor)}
	 * instead.
	 *
	 * @param packageName the given package name
	 * @param typeQualifiedName the given type qualified name
	 * @exception JavaScriptModelException if this project does not exist or if an
	 *		exception occurs while accessing its corresponding resource
	 * @return the first type found following this project's includepath
	 * with the given package name and type qualified name
	 * or <code>null</code> if none is found
	 * @see IType#getTypeQualifiedName(char)
	 */
	IType findType(String packageName, String typeQualifiedName) throws JavaScriptModelException;
	/**
	 * Same functionality as {@link #findType(String, String)} but also look for
	 * secondary types if given name does not match a javaScript unit name.
	 *
	 * @param packageName the given package name
	 * @param typeQualifiedName the given type qualified name
	 * @param progressMonitor the progress monitor to report progress to,
	 * 	or <code>null</code> if no progress monitor is provided
	 * @exception JavaScriptModelException if this project does not exist or if an
	 *		exception occurs while accessing its corresponding resource
	 * @return the first type found following this project's includepath
	 * with the given fully qualified name or <code>null</code> if none is found
	 * @see IType#getFullyQualifiedName(char)
	 */
	IType findType(String packageName, String typeQualifiedName, IProgressMonitor progressMonitor) throws JavaScriptModelException;
	/**
	 * Returns the first type found following this project's includepath
	 * with the given package name and type qualified name
	 * or <code>null</code> if none is found.
	 * The package name is a dot-separated name.
	 * The type qualified name is also a dot-separated name. For example,
	 * a class B defined as a member type of a class A should have the
	 * type qualified name "A.B".
	 * If the returned type is part of a javaScript unit, its owner is the given
	 * owner.
	 *
	 * Note that in order to be found, a type name (or its toplevel enclosing
	 * type name) must match its corresponding javaScript unit name. As a
	 * consequence, secondary types cannot be found using this functionality.
	 * To find secondary types use {@link #findType(String, String, WorkingCopyOwner, IProgressMonitor)}
	 * instead.
	 *
	 * @param packageName the given package name
	 * @param typeQualifiedName the given type qualified name
	 * @param owner the owner of the returned type's javaScript unit
	 * @exception JavaScriptModelException if this project does not exist or if an
	 *		exception occurs while accessing its corresponding resource
	 * @return the first type found following this project's includepath
	 * with the given package name and type qualified name
	 * or <code>null</code> if none is found
	 * @see IType#getTypeQualifiedName(char)
	 */
	IType findType(String packageName, String typeQualifiedName, WorkingCopyOwner owner) throws JavaScriptModelException;
	/**
	 * Same functionality as {@link #findType(String, String, WorkingCopyOwner)}
	 * but also look for secondary types if given name does not match a javaScript unit name.
	 *
	 * @param packageName the given package name
	 * @param typeQualifiedName the given type qualified name
	 * @param owner the owner of the returned type's javaScript unit
	 * @param progressMonitor the progress monitor to report progress to,
	 * 	or <code>null</code> if no progress monitor is provided
	 * @exception JavaScriptModelException if this project does not exist or if an
	 *		exception occurs while accessing its corresponding resource
	 * @return the first type found following this project's includepath
	 * with the given fully qualified name or <code>null</code> if none is found
	 * @see IType#getFullyQualifiedName(char)
	 */
	IType findType(String packageName, String typeQualifiedName, WorkingCopyOwner owner, IProgressMonitor progressMonitor) throws JavaScriptModelException;

	/**
	 * Returns all of the existing package fragment roots that exist
	 * on the includepath, in the order they are defined by the includepath.
	 *
	 * @return all of the existing package fragment roots that exist
	 * on the includepath
	 * @exception JavaScriptModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource
	 */
	IPackageFragmentRoot[] getAllPackageFragmentRoots() throws JavaScriptModelException;

	/**
	 * Returns an array of non-JavaScript resources directly contained in this project.
	 * It does not transitively answer non-JavaScript resources contained in folders;
	 * these would have to be explicitly iterated over.
	 * <p>
	 * Non-JavaScript resources includes other files and folders located in the
	 * project not accounted for by any of it source or binary package fragment
	 * roots. If the project is a source folder itself, resources excluded from the
	 * corresponding source includepath entry by one or more exclusion patterns
	 * are considered non-JavaScript resources and will appear in the result
	 * (possibly in a folder)
	 * </p>
	 *
	 * @return an array of non-JavaScript resources (<code>IFile</code>s and/or
	 *              <code>IFolder</code>s) directly contained in this project
	 * @exception JavaScriptModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource
	 */
	Object[] getNonJavaScriptResources() throws JavaScriptModelException;

	/**
	 * Helper method for returning one option value only. Equivalent to <code>(String)this.getOptions(inheritJavaCoreOptions).get(optionName)</code>
	 * Note that it may answer <code>null</code> if this option does not exist, or if there is no custom value for it.
	 * <p>
	 * For a complete description of the configurable options, see <code>JavaScriptCore#getDefaultOptions</code>.
	 * </p>
	 *
	 * @param optionName the name of an option
	 * @param inheritJavaCoreOptions - boolean indicating whether JavaScriptCore options should be inherited as well
	 * @return the String value of a given option
	 * @see JavaScriptCore#getDefaultOptions()
	 */
	String getOption(String optionName, boolean inheritJavaCoreOptions);

	/**
	 * Returns the table of the current custom options for this project. Projects remember their custom options,
	 * in other words, only the options different from the the JavaScriptCore global options for the workspace.
	 * A boolean argument allows to directly merge the project options with global ones from <code>JavaScriptCore</code>.
	 * <p>
	 * For a complete description of the configurable options, see <code>JavaScriptCore#getDefaultOptions</code>.
	 * </p>
	 *
	 * @param inheritJavaCoreOptions - boolean indicating whether JavaScriptCore options should be inherited as well
	 * @return table of current settings of all options
	 *   (key type: <code>String</code>; value type: <code>String</code>)
	 * @see JavaScriptCore#getDefaultOptions()
	 */
	Map getOptions(boolean inheritJavaCoreOptions);

	/**
	 * Returns a package fragment root for the file at the specified file system path.
	 * This is a handle-only method.  The underlying <code>java.io.File</code>
	 * may or may not exist. No resource is associated with this local file
	 * package fragment root.
	 *
	 * @param filePath the   file system path
	 * @return a package fragment root for the file at the specified file system path
	 */
	IPackageFragmentRoot getPackageFragmentRoot(String filePath);

	/**
	 * Returns a package fragment root for the given resource, which
	 * must either be a folder representing the top of a package hierarchy,
	 * or a javaScript file.
	 * This is a handle-only method.  The underlying resource may or may not exist.
	 *
	 * @param resource the given resource
	 * @return a package fragment root for the given resource, which
	 * must either be a folder representing the top of a package hierarchy,
	 * or a javaScript file
	 */
	IPackageFragmentRoot getPackageFragmentRoot(IResource resource);

	/**
	 * Returns all of the  package fragment roots contained in this
	 * project, identified on this project's resolved includepath. The result
	 * does not include package fragment roots in other projects referenced
	 * on this project's includepath.
	 *
	 * <p>NOTE: This is equivalent to <code>getChildren()</code>.
	 *
	 * @return all of the  package fragment roots contained in this
	 * project, identified on this project's resolved includepath
	 * @exception JavaScriptModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource
	 */
	IPackageFragmentRoot[] getPackageFragmentRoots() throws JavaScriptModelException;

	/**
	 * Returns all package fragments in all package fragment roots contained
	 * in this project. This is a convenience method.
	 *
	 * Note that the package fragment roots corresponds to the resolved
	 * includepath of the project.
	 *
	 * @return all package fragments in all package fragment roots contained
	 * in this project
	 * @exception JavaScriptModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource
	 */
	IPackageFragment[] getPackageFragments() throws JavaScriptModelException;

	/**
	 * Returns the <code>IProject</code> on which this <code>IJavaScriptProject</code>
	 * was created. This is handle-only method.
	 *
	 * @return the <code>IProject</code> on which this <code>IJavaScriptProject</code>
	 * was created
	 */
	IProject getProject();

	/**
	 * Returns the raw includepath for the project, as a list of includepath
	 * entries. This corresponds to the exact set of entries which were assigned
	 * using <code>setRawIncludepath</code>, in particular such a includepath may
	 * contain includepath variable and includepath container entries. Includepath
	 * variable and includepath container entries can be resolved using the
	 * helper method <code>getResolvedIncludepath</code>; includepath variable
	 * entries also can be resolved individually using
	 * <code>JavaScriptCore#getIncludepathVariable</code>).
	 * <p>
	 * Both includepath containers and includepath variables provides a level of
	 * indirection that can make the <code>.jsdtScope</code> file stable across
	 * workspaces.
	 * </p>
	 * <p>
	 * Note that in case the project isn't yet opened, the includepath will
	 * be read directly from the associated <tt>.jsdtScope</tt> file.
	 * </p>
	 *
	 * @return the raw includepath for the project, as a list of includepath entries
	 * @exception JavaScriptModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource
	 * @see IIncludePathEntry
	 */
	IIncludePathEntry[] getRawIncludepath() throws JavaScriptModelException;

	/**
	 * Returns the names of the projects that are directly required by this
	 * project. A project is required if it is in its includepath.
	 * <p>
	 * The project names are returned in the order they appear on the includepath.
	 *
	 * @return the names of the projects that are directly required by this
	 * project in includepath order
	 * @exception JavaScriptModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource
	 */
	String[] getRequiredProjectNames() throws JavaScriptModelException;

	/**
	 * This is a helper method returning the resolved includepath for the project
	 * as a list of simple (non-variable, non-container) includepath entries.
	 * All includepath variable and includepath container entries in the project's
	 * raw includepath will be replaced by the simple includepath entries they
	 * resolve to.
	 * <p>
	 * The resulting resolved includepath is accurate for the given point in time.
	 * If the project's raw includepath is later modified, or if includepath
	 * variables are changed, the resolved includepath can become out of date.
	 * Because of this, hanging on resolved includepath is not recommended.
	 * </p>
	 *
	 * @param ignoreUnresolvedEntry indicates how to handle unresolvable
	 * variables and containers; <code>true</code> indicates that missing
	 * variables and unresolvable includepath containers should be silently
	 * ignored, and that the resulting list should consist only of the
	 * entries that could be successfully resolved; <code>false</code> indicates
	 * that a <code>JavaScriptModelException</code> should be thrown for the first
	 * unresolved variable or container
	 * @return the resolved includepath for the project as a list of simple
	 * includepath entries, where all includepath variable and container entries
	 * have been resolved and substituted with their final target entries
	 * @exception JavaScriptModelException in one of the corresponding situation:
	 * <ul>
	 *    <li>this element does not exist</li>
	 *    <li>an exception occurs while accessing its corresponding resource</li>
	 *    <li>a includepath variable or includepath container was not resolvable
	 *    and <code>ignoreUnresolvedEntry</code> is <code>false</code>.</li>
	 * </ul>
	 * @see IIncludePathEntry
	 */
	IIncludePathEntry[] getResolvedIncludepath(boolean ignoreUnresolvedEntry)
	     throws JavaScriptModelException;

	/**
	 * Returns whether this project has been built at least once and thus whether it has a build state.
	 * @return true if this project has been built at least once, false otherwise
	 */
	boolean hasBuildState();

	/**
	 * Returns whether setting this project's includepath to the given includepath entries
	 * would result in a cycle.
	 *
	 * If the set of entries contains some variables, those are resolved in order to determine
	 * cycles.
	 *
	 * @param entries the given includepath entries
	 * @return true if the given includepath entries would result in a cycle, false otherwise
	 */
	boolean hasIncludepathCycle(IIncludePathEntry[] entries);
	/**
	 * Returns whether the given element is on the includepath of this project,
	 * that is, referenced from a includepath entry and not explicitly excluded
	 * using an exclusion pattern.
	 *
	 * @param element the given element
	 * @return <code>true</code> if the given element is on the includepath of
	 * this project, <code>false</code> otherwise
	 * @see IIncludePathEntry#getInclusionPatterns()
	 * @see IIncludePathEntry#getExclusionPatterns()
	 */
	boolean isOnIncludepath(IJavaScriptElement element);
	/**
	 * Returns whether the given resource is on the includepath of this project,
	 * that is, referenced from a includepath entry and not explicitly excluded
	 * using an exclusion pattern.
	 *
	 * @param resource the given resource
	 * @return <code>true</code> if the given resource is on the includepath of
	 * this project, <code>false</code> otherwise
	 * @see IIncludePathEntry#getInclusionPatterns()
	 * @see IIncludePathEntry#getExclusionPatterns()
	 */
	boolean isOnIncludepath(IResource resource);

	/**
	 * Creates a new evaluation context.
	 * @return a new evaluation context.
	 */
	IEvaluationContext newEvaluationContext();

	/**
	 * Creates and returns a type hierarchy for all types in the given
	 * region, considering subtypes within that region.
	 *
	 * @param monitor the given progress monitor
	 * @param region the given region
	 * @exception JavaScriptModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource
	 * @exception IllegalArgumentException if region is <code>null</code>
	 * @return a type hierarchy for all types in the given
	 * region, considering subtypes within that region
	 */
	ITypeHierarchy newTypeHierarchy(IRegion region, IProgressMonitor monitor)
		throws JavaScriptModelException;

	/**
	 * Creates and returns a type hierarchy for all types in the given
	 * region, considering subtypes within that region and considering types in the
	 * working copies with the given owner.
	 * In other words, the owner's working copies will take
	 * precedence over their original javaScript units in the workspace.
	 * <p>
	 * Note that if a working copy is empty, it will be as if the original javaScript
	 * unit had been deleted.
	 * <p>
	 *
	 * @param monitor the given progress monitor
	 * @param region the given region
	 * @param owner the owner of working copies that take precedence over their original javaScript units
	 * @exception JavaScriptModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource
	 * @exception IllegalArgumentException if region is <code>null</code>
	 * @return a type hierarchy for all types in the given
	 * region, considering subtypes within that region
	 */
	ITypeHierarchy newTypeHierarchy(IRegion region, WorkingCopyOwner owner, IProgressMonitor monitor)
		throws JavaScriptModelException;

	/**
	 * Creates and returns a type hierarchy for the given type considering
	 * subtypes in the specified region.
	 *
	 * @param type the given type
	 * @param region the given region
	 * @param monitor the given monitor
	 *
	 * @exception JavaScriptModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource
	 *
	 * @exception IllegalArgumentException if type or region is <code>null</code>
	 * @return a type hierarchy for the given type considering
	 * subtypes in the specified region
	 */
	ITypeHierarchy newTypeHierarchy(
		IType type,
		IRegion region,
		IProgressMonitor monitor)
		throws JavaScriptModelException;

	/**
	 * Creates and returns a type hierarchy for the given type considering
	 * subtypes in the specified region and considering types in the
	 * working copies with the given owner.
	 * In other words, the owner's working copies will take
	 * precedence over their original javaScript units in the workspace.
	 * <p>
	 * Note that if a working copy is empty, it will be as if the original javaScript
	 * unit had been deleted.
	 * <p>
	 *
	 * @param type the given type
	 * @param region the given region
	 * @param monitor the given monitor
	 * @param owner the owner of working copies that take precedence over their original javaScript units
	 *
	 * @exception JavaScriptModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource
	 *
	 * @exception IllegalArgumentException if type or region is <code>null</code>
	 * @return a type hierarchy for the given type considering
	 * subtypes in the specified region
	 */
	ITypeHierarchy newTypeHierarchy(
		IType type,
		IRegion region,
		WorkingCopyOwner owner,
		IProgressMonitor monitor)
		throws JavaScriptModelException;

	/**
	 * Returns the raw includepath for the project as defined by its
	 * <code>.jsdtScope</code> file from disk, or <code>null</code>
	 * if unable to read the file.
	 * <p>
	 * This includepath may differ from the in-memory includepath returned by
	 * <code>getRawIncludepath</code>, in case the automatic reconciliation
	 * mechanism has not been performed yet. Usually, any change to the
	 * <code>.jsdtScope</code> file is automatically noticed and reconciled at
	 * the next resource change notification event. However, if the file is
	 * modified within an operation, where this change needs to be taken into
	 * account before the operation ends, then the includepath from disk can be
	 * read using this method, and further assigned to the project using
	 * <code>setRawIncludepath(...)</code>.
	 * </p>
	 * <p>
	 * Includepath variable and includepath container entries can be resolved using
	 * the helper method <code>getResolvedIncludepath</code>; includepath variable
	 * entries also can be resolved individually using
	 * <code>JavaScriptCore#getIncludepathVariable</code>).
	 * </p>
	 * <p>
	 * Note that no check is performed whether the project has the JavaScript nature
	 * set, allowing an existing <code>.jsdtScope</code> file to be considered
	 * independantly (unlike <code>getRawIncludepath</code> which requires the
	 * JavaScript nature to be associated with the project).
	 * </p>
	 * <p>
	 * In order to manually force a project includepath refresh, one can simply
	 * assign the project includepath using the result of this method, as follows:
	 * <code>proj.setRawIncludepath(proj.readRawIncludepath(), monitor)</code>
	 * (note that the <code>readRawIncludepath</code> method
	 * could return <code>null</code>).
	 * </p>
	 *
	 * @return the raw includepath from disk for the project, as a list of
	 * includepath entries
	 * @see #getRawIncludepath()
	 * @see IIncludePathEntry
	 */
	IIncludePathEntry[] readRawIncludepath();

	/**
	 * Helper method for setting one option value only. Equivalent to <code>Map options = this.getOptions(false); map.put(optionName, optionValue); this.setOptions(map)</code>
	 * <p>
	 * For a complete description of the configurable options, see <code>JavaScriptCore#getDefaultOptions</code>.
	 * </p>
	 *
	 * @param optionName the name of an option
	 * @param optionValue the value of the option to set
	 * @see JavaScriptCore#getDefaultOptions()
	 */
	void setOption(String optionName, String optionValue);

	/**
	 * Sets the project custom options. All and only the options explicitly included in the given table
	 * are remembered; all previous option settings are forgotten, including ones not explicitly
	 * mentioned.
	 * <p>
	 * For a complete description of the configurable options, see <code>JavaScriptCore#getDefaultOptions</code>.
	 * </p>
	 *
	 * @param newOptions the new options (key type: <code>String</code>; value type: <code>String</code>),
	 *   or <code>null</code> to flush all custom options (clients will automatically get the global JavaScriptCore options).
	 * @see JavaScriptCore#getDefaultOptions()
	 */
	void setOptions(Map newOptions);

	/**
	 * Sets the includepath of this project using a list of includepath entries. In particular such a includepath may contain
	 * includepath variable entries. Includepath variable entries can be resolved individually ({@link JavaScriptCore#getIncludepathVariable(String)}),
	 * or the full includepath can be resolved at once using the helper method {@link #getResolvedIncludepath(boolean)}.
	 * <p>
	 * </p><p>
	 * If it is specified that this operation cannot modify resources, the .jsdtScope file will not be written to disk
	 * and no error marker will be generated. To synchronize the .jsdtScope with the in-memory includepath,
	 * one can use <code>setRawIncludepath(readRawIncludepath(), true, monitor)</code>.
	 * </p><p>
	 * Setting the includepath to <code>null</code> specifies a default includepath
	 * (the project root). Setting the includepath to an empty array specifies an
	 * empty includepath.
	 * </p><p>
	 * If a cycle is detected while setting this includepath (and if resources can be modified), an error marker will be added
	 * to the project closing the cycle.
	 * To avoid this problem, use {@link #hasIncludepathCycle(IIncludePathEntry[])}
	 * before setting the includepath.
	 * <p>
	 * This operation acquires a lock on the workspace's root.
	 *
	 * @param entries a list of includepath entries
	 * @param canModifyResources whether resources should be written to disk if needed
	 * @param monitor the given progress monitor
	 * @exception JavaScriptModelException if the includepath could not be set. Reasons include:
	 * <ul>
	 * <li> This JavaScript element does not exist (ELEMENT_DOES_NOT_EXIST)</li>
	 * <li> The includepath is being modified during resource change event notification (CORE_EXCEPTION)
	 * <li> The includepath failed the validation check as defined by {@link JavaScriptConventions#validateIncludepath(IJavaScriptProject, IIncludePathEntry[], IPath)}
	 * </ul>
	 * @see IIncludePathEntry
	 */
	void setRawIncludepath(IIncludePathEntry[] entries, boolean canModifyResources, IProgressMonitor monitor) throws JavaScriptModelException;

	/**
	 * Sets the includepath of this project using a list of includepath entries. In particular such a includepath may contain
	 * includepath variable entries. Includepath variable entries can be resolved individually ({@link JavaScriptCore#getIncludepathVariable(String)}),
	 * or the full includepath can be resolved at once using the helper method {@link #getResolvedIncludepath(boolean)}.
	 * <p>
	 * <p>
	 * Setting the includepath to <code>null</code> specifies a default includepath
	 * (the project root). Setting the includepath to an empty array specifies an
	 * empty includepath.
	 * <p>
	 * If a cycle is detected while setting this includepath, an error marker will be added
	 * to the project closing the cycle.
	 * To avoid this problem, use {@link #hasIncludepathCycle(IIncludePathEntry[])}
	 * before setting the includepath.
	 * <p>
	 * This operation acquires a lock on the workspace's root.
	 *
	 * @param entries a list of includepath entries
	 * @param monitor the given progress monitor
	 * @exception JavaScriptModelException if the includepath could not be set. Reasons include:
	 * <ul>
	 * <li> This JavaScript element does not exist (ELEMENT_DOES_NOT_EXIST)</li>
	 * <li> The includepath is being modified during resource change event notification (CORE_EXCEPTION)
	 * <li> The includepath failed the validation check as defined by {@link JavaScriptConventions#validateIncludepath(IJavaScriptProject, IIncludePathEntry[], IPath)}
	 * </ul>
	 * @see IIncludePathEntry
	 */
	void setRawIncludepath(IIncludePathEntry[] entries, IProgressMonitor monitor)
		throws JavaScriptModelException;
	
	ITypeRoot findTypeRoot(String fullyQualifiedName) throws JavaScriptModelException;

}
