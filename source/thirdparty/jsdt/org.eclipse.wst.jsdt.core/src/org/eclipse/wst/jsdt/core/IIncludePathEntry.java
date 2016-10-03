/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.core;

import org.eclipse.core.runtime.IPath;

/**
 * An entry on a JavaScript project includepath identifying one or more package fragment
 * roots. A includepath entry has a content kind (either source,
 * {@link IPackageFragmentRoot#K_SOURCE}, or binary, {@link IPackageFragmentRoot#K_BINARY}), which is inherited
 * by each package fragment root and package fragment associated with the entry.
 * <p>
 * A includepath entry can refer to any of the following:<ul>
 *
 *	<li>Source code in the current project. In this case, the entry identifies a
 *		root folder in the current project containing package fragments and
 *		source files with one of the {@link JavaScriptCore#getJavaScriptLikeExtensions()
 *		JavaScript-like extensions}. The root folder itself represents a default
 *		package, subfolders represent package fragments, and files with a
 *     JavaScript-like extension (e.g. <code>.js</code> files)
 *		represent javaScript files. All javaScript files will be compiled when
 * 		the project is built. The includepath entry must specify the
 *		absolute path to the root folder. Entries of this kind are
 *		associated with the {@link #CPE_SOURCE} constant.
 *      Source includepath entries can carry inclusion and exclusion patterns for
 *      selecting which source files appear as javaScript
 *      units and get compiled when the project is built.
 *  </li>
 *
 *	<li>A binary library in the current project, in another project, or in the external
 *		file system. In this case the entry identifies non-editable files.  Entries
 *		of this kind are associated with the {@link #CPE_LIBRARY} constant.</li>
 *
 *	<li>A required project. In this case the entry identifies another project in
 *		the workspace.  When performing other
 *		"development" operations - such as code assist, code resolve, type hierarchy
 *		creation, etc. - the source code of the project is referred to. Thus, development
 *		is performed against a required project's source code.  The
 *		includepath entry must specify the absolute path to the
 *		project. Entries of this kind are  associated with the {@link #CPE_PROJECT}
 *		constant.
 * 		Note: referencing a required project with a includepath entry refers to the source
 *     code or associated <code>.class</code> files located in its output location.
 *     It will also automatically include any other libraries or projects that the required project's includepath
 *     refers to, iff the corresponding includepath entries are tagged as being exported
 *     ({@link IIncludePathEntry#isExported}).
 *    Unless exporting some includepath entries, includepaths are not chained by default -
 *    each project must specify its own includepath in its entirety.</li>
 *
 *  <li> A path beginning in a includepath variable defined globally to the workspace.
 *		Entries of this kind are  associated with the {@link #CPE_VARIABLE} constant.
 *      Includepath variables are created using {@link JavaScriptCore#setIncludepathVariable(String, IPath, org.eclipse.core.runtime.IProgressMonitor)},
 * 		and gets resolved, to either a project or library entry, using
 *      {@link JavaScriptCore#getResolvedIncludepathEntry(IIncludePathEntry)}.
 *		It is also possible to register an automatic initializer ({@link JsGlobalScopeVariableInitializer}),
 * 	which will be invoked through the extension point "org.eclipse.wst.jsdt.core.JsGlobalScopeVariableInitializer".
 * 	After resolution, a includepath variable entry may either correspond to a project or a library entry. </li>
 *
 *  <li> A named includepath container identified by its container path.
 *     A includepath container provides a way to indirectly reference a set of includepath entries through
 *     a includepath entry of kind {@link #CPE_CONTAINER}. Typically, a includepath container can
 *     be used to describe a complex library composed of multiple files, projects or includepath variables,
 *     considering also that containers can be mapped differently on each project. Several projects can
 *     reference the same generic container path, but have each of them actually bound to a different
 *     container object.
 *     The container path is a formed by a first ID segment followed with extra segments,
 *     which can be used as additional hints for resolving this container reference. If no container was ever
 *     recorded for this container path onto this project (using {@link JavaScriptCore#setJsGlobalScopeContainer},
 * 	then a {@link JsGlobalScopeContainerInitializer} will be activated if any was registered for this
 * 	container ID onto the extension point "org.eclipse.wst.jsdt.core.JsGlobalScopeContainerInitializer".
 * 	A includepath container entry can be resolved explicitly using {@link JavaScriptCore#getJsGlobalScopeContainer}
 * 	and the resulting container entries can contain any non-container entry. In particular, it may contain variable
 *     entries, which in turn needs to be resolved before being directly used.
 * 	<br> Also note that the container resolution APIs include an IJavaScriptProject argument, so as to allow the same
 * 	container path to be interpreted in different ways for different projects. </li>
 * </ul>
 * </p>
 * The result of {@link IJavaScriptProject#getResolvedClasspath} will have all entries of type
 * {@link #CPE_VARIABLE} and {@link #CPE_CONTAINER} resolved to a set of
 * {@link #CPE_SOURCE}, {@link #CPE_LIBRARY} or {@link #CPE_PROJECT}
 * includepath entries.
 * <p>
 * Any includepath entry other than a source folder (kind {@link #CPE_SOURCE}) can
 * be marked as being exported. Exported entries are automatically contributed to
 * dependent projects, along with the project's default output folder, which is
 * implicitly exported, and any auxiliary output folders specified on source
 * includepath entries. The project's output folder(s) are always listed first,
 * followed by the any exported entries.
 * <p>
 * This interface is not intended to be implemented by clients.
 * Includepath entries can be created via methods on {@link JavaScriptCore}.
 * </p>
 *
 * @see JavaScriptCore#newLibraryEntry(org.eclipse.core.runtime.IPath, org.eclipse.core.runtime.IPath, org.eclipse.core.runtime.IPath)
 * @see JavaScriptCore#newProjectEntry(org.eclipse.core.runtime.IPath)
 * @see JavaScriptCore#newSourceEntry(org.eclipse.core.runtime.IPath)
 * @see JavaScriptCore#newVariableEntry(org.eclipse.core.runtime.IPath, org.eclipse.core.runtime.IPath, org.eclipse.core.runtime.IPath)
 * @see JavaScriptCore#newContainerEntry(org.eclipse.core.runtime.IPath)
 * @see JsGlobalScopeVariableInitializer
 * @see JsGlobalScopeContainerInitializer
 *  
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public interface IIncludePathEntry {

	/**
	 * Entry kind constant describing a includepath entry identifying a
	 * library. 
	 */
	int CPE_LIBRARY = 1;

	/**
	 * Entry kind constant describing a includepath entry identifying a
	 * required project.
	 */
	int CPE_PROJECT = 2;

	/**
	 * Entry kind constant describing a includepath entry identifying a
	 * folder containing package fragments with source code
	 * to be validated.
	 */
	int CPE_SOURCE = 3;

	/**
	 * Entry kind constant describing a includepath entry defined using
	 * a path that begins with a includepath variable reference.
	 */
	int CPE_VARIABLE = 4;

	/**
	 * Entry kind constant describing a includepath entry representing
	 * a name includepath container.
	 */
	int CPE_CONTAINER = 5;

	/**
	 * Returns whether the access rules of the project's exported entries should be combined with this entry's access rules.
	 * Returns true for container entries.
	 * Returns false otherwise.
	 *
	 * @return whether the access rules of the project's exported entries should be combined with this entry's access rules
	 */
	boolean combineAccessRules();

	/**
	 * Returns the possibly empty list of access rules for this entry.
	 *
	 * @return the possibly empty list of access rules for this entry
	 */
	IAccessRule[] getAccessRules();
	/**
	 * Returns the kind of files found in the package fragments identified by this
	 * includepath entry.
	 *
	 * @return {@link IPackageFragmentRoot#K_SOURCE} for files containing
	 *   source code, and {@link IPackageFragmentRoot#K_BINARY} for binary
	 *   class files.
	 *   There is no specified value for an entry denoting a variable ({@link #CPE_VARIABLE})
	 *   or a includepath container ({@link #CPE_CONTAINER}).
	 */
	int getContentKind();

	/**
	 * Returns the kind of this includepath entry.
	 *
	 * @return one of:
	 * <ul>
	 * <li>{@link #CPE_SOURCE} - this entry describes a source root in
	 		its project
	 * <li>{@link #CPE_LIBRARY} - this entry describes a folder 
	 		containing non-editable files
	 * <li>{@link #CPE_PROJECT} - this entry describes another project
	 *
	 * <li>{@link #CPE_VARIABLE} - this entry describes a project or library
	 *  	indirectly via a includepath variable in the first segment of the path
	 * *
	 * <li>{@link #CPE_CONTAINER} - this entry describes set of entries
	 *  	referenced indirectly via a includepath container
	 * </ul>
	 */
	int getEntryKind();

	/**
	 * Returns the set of patterns used to exclude resources or classes associated with
	 * this includepath entry.
	 * <p>
	 * For source includepath entries,
	 * exclusion patterns allow specified portions of the resource tree rooted
	 * at this source entry's path to be filtered out. If no exclusion patterns
	 * are specified, this source entry includes all relevent files. Each path
	 * specified must be a relative path, and will be interpreted relative
	 * to this source entry's path. File patterns are case-sensitive. A file
	 * matched by one or more of these patterns is excluded from the
	 * corresponding package fragment root.
	 * Exclusion patterns have higher precedence than inclusion patterns;
	 * in other words, exclusion patterns can remove files for the ones that
	 * are to be included, not the other way around.
	 * </p>
	 * <p>
	 * The pattern mechanism is similar to Ant's. Each pattern is represented as
	 * a relative path. The path segments can be regular file or folder names or simple patterns
	 * involving standard wildcard characters.
	 * </p>
	 * <p>
	 * '*' matches 0 or more characters within a segment. So
	 * <code>*.js</code> matches <code>.js</code>, <code>a.js</code>
	 * and <code>Foo.js</code>, but not <code>Foo.properties</code>
	 * (does not end with <code>.js</code>).
	 * </p>
	 * <p>
	 * '?' matches 1 character within a segment. So <code>?.js</code>
	 * matches <code>a.js</code>, <code>A.js</code>,
	 * but not <code>.js</code> or <code>xyz.js</code> (neither have
	 * just one character before <code>.js</code>).
	 * </p>
	 * <p>
	 * Combinations of *'s and ?'s are allowed.
	 * </p>
	 * <p>
	 * The special pattern '**' matches zero or more segments. In a source entry,
	 * a path like <code>tests/</code> that ends in a trailing separator is interpreted
	 * as <code>tests/&#42;&#42;</code>, and would match everything under
	 * the folder named <code>tests</code>.
	 * </p>
	 * <p>
	 * Example patterns in source entries (assuming that "js" is the only {@link JavaScriptCore#getJavaScriptLikeExtensions() JavaScript-like extension}):
	 * <ul>
	 * <li>
	 * <code>tests/&#42;&#42;</code> (or simply <code>tests/</code>)
	 * matches all files under a root folder
	 * named <code>tests</code>. This includes <code>tests/Foo.js</code>
	 * and <code>tests/com/example/Foo.js</code>, but not
	 * <code>com/example/tests/Foo.js</code> (not under a root folder named
	 * <code>tests</code>).
	 * </li>
	 * <li>
	 * <code>tests/&#42;</code> matches all files directly below a root
	 * folder named <code>tests</code>. This includes <code>tests/Foo.js</code>
	 * and <code>tests/FooHelp.js</code>
	 * but not <code>tests/com/example/Foo.js</code> (not directly under
	 * a folder named <code>tests</code>) or
	 * <code>com/Foo.js</code> (not under a folder named <code>tests</code>).
	 * </li>
	 * <li>
	 * <code>&#42;&#42;/tests/&#42;&#42;</code> matches all files under any
	 * folder named <code>tests</code>. This includes <code>tests/Foo.js</code>,
	 * <code>com/examples/tests/Foo.js</code>, and
	 * <code>com/examples/tests/unit/Foo.js</code>, but not
	 * <code>com/example/Foo.js</code> (not under a folder named
	 * <code>tests</code>).
	 * </li>
	 * </ul>
	 * </p>
	 *
	 * @return the possibly empty list of resource exclusion patterns
	 *   associated with this includepath entry, or <code>null</code> if this kind
	 *   of includepath entry does not support exclusion patterns
	 */
	IPath[] getExclusionPatterns();

	/**
	 * Returns the extra includepath attributes for this includepath entry. Returns an empty array if this entry
	 * has no extra attributes.
	 *
	 * @return the possibly empty list of extra includepath attributes for this includepath entry
	 */
	IIncludePathAttribute[] getExtraAttributes();

	/**
	 * Returns the set of patterns used to explicitly define resources
	 * to be included with this includepath entry.
	 * <p>
	 * For source includepath entries,
	 * when no inclusion patterns are specified, the source entry includes all
	 * relevent files in the resource tree rooted at this source entry's path.
	 * Specifying one or more inclusion patterns means that only the specified
	 * portions of the resource tree are to be included. Each path specified
	 * must be a relative path, and will be interpreted relative to this source
	 * entry's path. File patterns are case-sensitive. A file matched by one or
	 * more of these patterns is included in the corresponding package fragment
	 * root unless it is excluded by one or more of this entrie's exclusion
	 * patterns. Exclusion patterns have higher precedence than inclusion
	 * patterns; in other words, exclusion patterns can remove files for the
	 * ones that are to be included, not the other way around.
	 * </p>
	 * <p>
	 * See {@link #getExclusionPatterns()} for a discussion of the syntax and
	 * semantics of path patterns. The absence of any inclusion patterns is
	 * semantically equivalent to the explicit inclusion pattern
	 * <code>&#42;&#42;</code>.
	 * </p>
	 * <p>
	 * Example patterns in source entries:
	 * <ul>
	 * <li>
	 * The inclusion pattern <code>src/&#42;&#42;</code> by itself includes all
	 * files under a root folder named <code>src</code>.
	 * </li>
	 * <li>
	 * The inclusion patterns <code>src/&#42;&#42;</code> and
	 * <code>tests/&#42;&#42;</code> includes all files under the root folders
	 * named <code>src</code> and <code>tests</code>.
	 * </li>
	 * <li>
	 * The inclusion pattern <code>src/&#42;&#42;</code> together with the
	 * exclusion pattern <code>src/&#42;&#42;/Foo.js</code> includes all
	 * files under a root folder named <code>src</code> except for ones
	 * named <code>Foo.js</code>.
	 * </li>
	 * </ul>
	 * </p>
	 *
	 * @return the possibly empty list of resource inclusion patterns
	 *   associated with this includepath entry, or <code>null</code> if this kind
	 *   of includepath entry does not support inclusion patterns
	 */
	IPath[] getInclusionPatterns();

	/**
	 * Returns the path of this includepath entry.
	 *
	 * The meaning of the path of a includepath entry depends on its entry kind:<ul>
	 *	<li>Source code in the current project ({@link #CPE_SOURCE}) -
	 *      The path associated with this entry is the absolute path to the root folder. </li>
	 *	<li>A binary library in the current project ({@link #CPE_LIBRARY}) - the path
	 *		associated with this entry is the absolute path to the file.
	 *	<li>A required project ({@link #CPE_PROJECT}) - the path of the entry denotes the
	 *		path to the corresponding project resource.</li>
	 *  <li>A variable entry ({@link #CPE_VARIABLE}) - the first segment of the path
	 *      is the name of a includepath variable. If this includepath variable
	 *		is bound to the path <i>P</i>, the path of the corresponding includepath entry
	 *		is computed by appending to <i>P</i> the segments of the returned
	 *		path without the variable.</li>
	 *  <li> A container entry ({@link #CPE_CONTAINER}) - the path of the entry
	 * 	is the name of the includepath container, which can be bound indirectly to a set of includepath
	 * 	entries after resolution. The containerPath is a formed by a first ID segment followed with
	 *     extra segments that can be used as additional hints for resolving this container
	 * 	reference (also see {@link IJsGlobalScopeContainer}).
	 * </li>
	 * </ul>
	 *
	 * @return the path of this includepath entry
	 */
	IPath getPath();

	/**
	 * Returns the path to the source archive or folder associated with this
	 * includepath entry, or <code>null</code> if this includepath entry has no
	 * source attachment.
	 * <p>
	 * Only library and variable includepath entries may have source attachments.
	 * For library includepath entries, the result path (if present) locates a source
	 * archive or folder. This archive or folder can be located in a project of the
	 * workspace or outside thr workspace. For variable includepath entries, the
	 * result path (if present) has an analogous form and meaning as the
	 * variable path, namely the first segment is the name of a includepath variable.
	 * </p>
	 *
	 * @return the path to the source archive or folder, or <code>null</code> if none
	 */
	IPath getSourceAttachmentPath();

	/**
	 * Returns the path within the source archive or folder where package fragments
	 * are located. An empty path indicates that packages are located at
	 * the root of the source archive or folder. Returns a non-<code>null</code> value
	 * if and only if {@link #getSourceAttachmentPath} returns
	 * a non-<code>null</code> value.
	 *
	 * @return the path within the source archive or folder, or <code>null</code> if
	 *    not applicable
	 */
	IPath getSourceAttachmentRootPath();

	/**
	 * Returns whether this entry is exported to dependent projects.
	 * Always returns <code>false</code> for source entries (kind
	 * {@link #CPE_SOURCE}), which cannot be exported.
	 *
	 * @return <code>true</code> if exported, and <code>false</code> otherwise
	 */
	boolean isExported();
}
