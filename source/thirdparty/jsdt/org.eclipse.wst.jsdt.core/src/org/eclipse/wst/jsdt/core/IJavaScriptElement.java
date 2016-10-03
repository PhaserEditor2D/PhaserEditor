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

import java.net.URI;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

/**
 * Common protocol for all elements provided by the JavaScript model.
 * JavaScript model elements are exposed to clients as handles to the actual underlying element.
 * The JavaScript model may hand out any number of handles for each element. Handles
 * that refer to the same element are guaranteed to be equal, but not necessarily identical.
 * <p>
 * Methods annotated as "handle-only" do not require underlying elements to exist.
 * Methods that require underlying elements to exist throw
 * a <code>JavaScriptModelException</code> when an underlying element is missing.
 * <code>JavaScriptModelException.isDoesNotExist</code> can be used to recognize
 * this common special case.
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
public interface IJavaScriptElement extends IAdaptable, ILookupScope{

	/**
	 * Constant representing a JavaScript model (workspace level object).
	 * A JavaScript element with this type can be safely cast to {@link IJavaScriptModel}.
	 */
	int JAVASCRIPT_MODEL = 1;

	/**
	 * Constant representing a JavaScript project.
	 * A JavaScript element with this type can be safely cast to {@link IJavaScriptProject}.
	 */
	int JAVASCRIPT_PROJECT = 2;

	/**
	 * Constant representing a root source folder (package fragment root).
	 * A JavaScript element with this type can be safely cast to {@link IPackageFragmentRoot}.
	 */
	int PACKAGE_FRAGMENT_ROOT = 3;

	/**
	 * Constant representing a source folder (package fragment).
	 * A JavaScript element with this type can be safely cast to {@link IPackageFragment}.
	 */
	int PACKAGE_FRAGMENT = 4;

	/**
	 * Constant representing a JavaScript file.
	 * A JavaScript element with this type can be safely cast to {@link IJavaScriptUnit}.
	 */
	int JAVASCRIPT_UNIT = 5;

	/**
	 * Constant representing a non-editable javaScript file.
	 * A JavaScript element with this type can be safely cast to {@link IClassFile}.
	 */
	int CLASS_FILE = 6;

	/**
	 * Constant representing a type (a class or interface).
	 * A JavaScript element with this type can be safely cast to {@link IType}.
	 */
	int TYPE = 7;

	/**
	 * Constant representing a field or a var with file scope.
	 * A JavaScript element with this type can be safely cast to {@link IField}.
	 */
	int FIELD = 8;

	/**
	 * Constant representing a function, method or constructor.
	 * A JavaScript element with this type can be safely cast to {@link IFunction}.
	 */
	int METHOD = 9;

	/**
	 * Constant representing a stand-alone instance or class initializer.
	 * A JavaScript element with this type can be safely cast to {@link IInitializer}.
	 */
	int INITIALIZER = 10;

	/**
	 * Constant representing all import declarations within a compilation unit.
	 * A JavaScript element with this type can be safely cast to {@link IImportContainer}.
	 * 
	 * <b>This type only applies to ECMAScript 4 which is not yet supported</b>
	 */
	int IMPORT_CONTAINER = 12;

	/**
	 * Constant representing an import declaration within a compilation unit.
	 * A JavaScript element with this type can be safely cast to {@link IImportDeclaration}.
	 * 
	 * <b>This type only applies to ECMAScript 4 which is not yet supported</b>
	 */
	int IMPORT_DECLARATION = 13;

	/**
	 * Constant representing a local variable declaration.
	 * A JavaScript element with this type can be safely cast to {@link ILocalVariable}.
	 */
	int LOCAL_VARIABLE = 14;

	/**
	 * Returns whether this JavaScript element exists in the model.
	 * <p>
	 * JavaScript elements are handle objects that may or may not be backed by an
	 * actual element. JavaScript elements that are backed by an actual element are
	 * said to "exist", and this method returns <code>true</code>. For JavaScript
	 * elements that are not working copies, it is always the case that if the
	 * element exists, then its parent also exists (provided it has one) and
	 * includes the element as one of its children. It is therefore possible
	 * to navigated to any existing JavaScript element from the root of the JavaScript model
	 * along a chain of existing JavaScript elements. On the other hand, working
	 * copies are said to exist until they are destroyed (with
	 * <code>IWorkingCopy.destroy</code>). Unlike regular JavaScript elements, a
	 * working copy never shows up among the children of its parent element
	 * (which may or may not exist).
	 * </p>
	 *
	 * @return <code>true</code> if this element exists in the JavaScript model, and
	 * <code>false</code> if this element does not exist
	 */
	boolean exists();

	/**
	 * Returns the first ancestor of this JavaScript element that has the given type.
	 * Returns <code>null</code> if no such an ancestor can be found.
	 * This is a handle-only method.
	 *
	 * @param ancestorType the given type
	 * @return the first ancestor of this JavaScript element that has the given type, null if no such an ancestor can be found
	 */
	IJavaScriptElement getAncestor(int ancestorType);

	/**
	 * <p>Returns the Jsdoc as an html source if this element has an attached jsdoc,
	 * null otherwise.</p>
	 * <p>This should be used only for binary elements. Source elements will always return null.</p>
	 * <p>The encoding used to read the jsdoc is the one defined by the content type of the
	 * file. If none is defined, then the project's encoding of this java element is used. If the project's
	 * encoding cannot be retrieved, then the platform encoding is used.</p>
	 * <p>In case of the jsdoc doesn't exist for this element, null is returned.</p>
	 *
	 * <p>The html is extracted from the attached jsdoc and provided as is. No
	 * transformation or validation is done.</p>
	 *
	 * @param monitor the given progress monitor
	 * @exception JavaScriptModelException if:<ul>
	 *  <li>this element does not exist</li>
	 *  <li>retrieving the attached jsdoc fails (timed-out, invalid URL, ...)</li>
	 *  <li>the format of the jsdoc doesn't match expected standards (different anchors,...)</li>
	 *  </ul>
	 * @return the extracted jsdoc from the attached jsdoc, null if none
	 * @see IIncludePathAttribute#JSDOC_LOCATION_ATTRIBUTE_NAME
	 */
	String getAttachedJavadoc(IProgressMonitor monitor) throws JavaScriptModelException;

	/**
	 * Returns the resource that corresponds directly to this element,
	 * or <code>null</code> if there is no resource that corresponds to
	 * this element.
	 * <p>
	 * For example, the corresponding resource for an <code>IJavaScriptUnit</code>
	 * is its underlying <code>IFile</code>. The corresponding resource for
	 * an <code>IPackageFragment</code> that is not contained in an archive
	 * is its underlying <code>IFolder</code>. An <code>IPackageFragment</code>
	 * contained in an archive has no corresponding resource. Similarly, there
	 * are no corresponding resources for <code>IMethods</code>,
	 * <code>IFields</code>, etc.
	 * <p>
	 *
	 * @return the corresponding resource, or <code>null</code> if none
	 * @exception JavaScriptModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource
	 */
	IResource getCorrespondingResource() throws JavaScriptModelException;

	/**
	 * Returns the name of this element. This is a handle-only method.
	 *
	 * @return the element name
	 */
	String getElementName();

	/**
	 * Returns this element's kind encoded as an integer.
	 * This is a handle-only method.
	 *
	 * @return the kind of element; one of the constants declared in
	 *   <code>IJavaScriptElement</code>
	 * @see IJavaScriptElement
	 */
	int getElementType();

	/**
	 * Returns a string representation of this element handle. The format of
	 * the string is not specified; however, the identifier is stable across
	 * workspace sessions, and can be used to recreate this handle via the
	 * <code>JavaScriptCore.create(String)</code> method.
	 *
	 * @return the string handle identifier
	 * @see JavaScriptCore#create(java.lang.String)
	 */
	String getHandleIdentifier();

	/**
	 * Returns the JavaScript model.
	 * This is a handle-only method.
	 *
	 * @return the JavaScript model
	 */
	IJavaScriptModel getJavaScriptModel();

	/**
	 * Returns the JavaScript project this element is contained in,
	 * or <code>null</code> if this element is not contained in any JavaScript project
	 * (for instance, the <code>IJavaScriptModel</code> is not contained in any JavaScript
	 * project).
	 * This is a handle-only method.
	 *
	 * @return the containing JavaScript project, or <code>null</code> if this element is
	 *   not contained in a JavaScript project
	 */
	IJavaScriptProject getJavaScriptProject();

	/**
	 * Returns the first openable parent. If this element is openable, the element
	 * itself is returned. Returns <code>null</code> if this element doesn't have
	 * an openable parent.
	 * This is a handle-only method.
	 *
	 * @return the first openable parent or <code>null</code> if this element doesn't have
	 * an openable parent.
	 */
	IOpenable getOpenable();

	/**
	 * Returns the element directly containing this element,
	 * or <code>null</code> if this element has no parent.
	 * This is a handle-only method.
	 *
	 * @return the parent element, or <code>null</code> if this element has no parent
	 */
	IJavaScriptElement getParent();

	/**
	 * Returns the path to the innermost resource enclosing this element.
	 * If this element is not included in an external archive,
	 * the path returned is the full, absolute path to the underlying resource,
	 * relative to the workbench.
	 * If this element is included in an external archive,
	 * the path returned is the absolute path to the archive in the file system.
	 * This is a handle-only method.
	 *
	 * @return the path to the innermost resource enclosing this element
	 */
	IPath getPath();

	/**
	 * Returns the primary element (whose compilation unit is the primary compilation unit)
	 * this working copy element was created from, or this element if it is a descendant of a
	 * primary javaScript unit or if it is not a descendant of a working copy (e.g. it is a
	 * binary member).
	 * The returned element may or may not exist.
	 *
	 * @return the primary element this working copy element was created from, or this
	 * 			element.
	 */
	IJavaScriptElement getPrimaryElement();

	/**
	 * Returns the innermost resource enclosing this element.
	 * If this element is included in an archive and this archive is not external,
	 * this is the underlying resource corresponding to the archive.
	 * If this element is included in an external archive, <code>null</code>
	 * is returned.
	 * This is a handle-only method.
	 *
	 * @return the innermost resource enclosing this element, <code>null</code> if this
	 * element is included in an external archive
	 */
	IResource getResource();

	/**
	 * Returns the scheduling rule associated with this JavaScript element.
	 * This is a handle-only method.
	 *
	 * @return the scheduling rule associated with this JavaScript element
	 */
	ISchedulingRule getSchedulingRule();

	/**
	 * Returns the smallest underlying resource that contains
	 * this element, or <code>null</code> if this element is not contained
	 * in a resource.
	 *
	 * @return the underlying resource, or <code>null</code> if none
	 * @exception JavaScriptModelException if this element does not exist or if an
	 *		exception occurs while accessing its underlying resource
	 */
	IResource getUnderlyingResource() throws JavaScriptModelException;

	/**
	 * Returns whether this JavaScript element is read-only. An element is read-only
	 * if its structure cannot be modified by the java model.
	 * <p>
	 * Note this is different from IResource.isReadOnly(). For example, .jar
	 * files are read-only as the javaScript model doesn't know how to add/remove
	 * elements in this file, but the underlying IFile can be writable.
	 * <p>
	 * This is a handle-only method.
	 *
	 * @return <code>true</code> if this element is read-only
	 */
	boolean isReadOnly();

	/**
	 * Returns whether the structure of this element is known. For example, for a
	 * javaScript file that could not be parsed, <code>false</code> is returned.
	 * If the structure of an element is unknown, navigations will return reasonable
	 * defaults. For example, <code>getChildren</code> will return an empty collection.
	 * <p>
	 * Note: This does not imply anything about consistency with the
	 * underlying resource/buffer contents.
	 * </p>
	 *
	 * @return <code>true</code> if the structure of this element is known
	 * @exception JavaScriptModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource
	 */
	boolean isStructureKnown() throws JavaScriptModelException;
	/**
	 * Returns a readable (non mangled) name.  In virtual elements this is derived from a JsGlobalScopeContainerInitializer
	 *
	 * @return a human friendly element name.
	 */
	String getDisplayName();
	/**
	 * Returns if this is a virtual element (ie actually exists in the model or filesystem).
	 *
	 * @return if this is a virtual element.
	 */
	boolean isVirtual();

	/**
	 * If a resource is virtual, then return a real host path for the element.  (Query the container initializer).
	 *
	 * @return if this is a virtual element.
	 */
	URI getHostPath();

	/**
	 * Returns the Super type this file is considered to be a member of. For Browser base javaScript, this would be "Window".
	 *
	 * @return the supertype for the javascript file.
	 */
	LibrarySuperType getCommonSuperType();

}
