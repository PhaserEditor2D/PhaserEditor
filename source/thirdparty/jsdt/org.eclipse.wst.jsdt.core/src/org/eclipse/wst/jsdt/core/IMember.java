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

/**
 * Common protocol for JavaScript elements that can be members of javaScript files or types.
 * This set consists of <code>IType</code>, <code>IFunction</code>,
 * <code>IField</code>, and <code>IInitializer</code>.
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 *  
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public interface IMember extends IJavaScriptElement, ISourceReference, ISourceManipulation, IParent {
/**
 * Returns the categories defined by this member's JSDoc. A category is the identifier
 * following the tag <code>@category</code> in the member's JSDoc.
 * Returns an empty array if no category is defined in this member's JSDoc.
 *
 * @return the categories defined by this member's doc
 * @exception JavaScriptModelException if this element does not exist or if an
 *      exception occurs while accessing its corresponding resource.
 */
String[] getCategories() throws JavaScriptModelException;
/*
 * Returns the class file in which this member is declared, or <code>null</code>
 * if this member is not declared in a class file (for example, a source type).
 * This is a handle-only method.
 *
 * @return the class file in which this member is declared, or <code>null</code>
 * if this member is not declared in a class file (for example, a source type)
 */
IClassFile getClassFile();
/**
 * Returns the javaScript unit in which this member is declared, or <code>null</code>
 * if this member is not declared in a javaScript unit.
 * This is a handle-only method.
 *
 * @return the javaScript unit in which this member is declared, or <code>null</code>
 * if this member is not declared in a javaScript unit (for example, a binary type)
 */
IJavaScriptUnit getJavaScriptUnit();
/**
 * Returns the type in which this member is declared, or <code>null</code>
 * if this member is not declared in a type (for example, a top-level type).
 * This is a handle-only method.
 *
 * @return the type in which this member is declared, or <code>null</code>
 * if this member is not declared in a type (for example, a top-level type)
 */
IType getDeclaringType();
/**
 * Returns the modifier flags for this member. The flags can be examined using class
 * <code>Flags</code>.
 * <p>
 * Note that only flags as indicated in the source are returned. Thus if an interface
 * defines a method <code>void myMethod();</code> the flags don't include the
 * 'public' flag.
 *
 * @exception JavaScriptModelException if this element does not exist or if an
 *      exception occurs while accessing its corresponding resource.
 * @return the modifier flags for this member
 * @see Flags
 */
int getFlags() throws JavaScriptModelException;
/**
 * Returns the JSDoc range if this element is from source or if this element
 * is a binary element with an attached source, null otherwise.
 *
 * <p>If this element is from source, the jsdoc range is
 * extracted from the corresponding source.</p>
 * <p>If this element is from a binary, the jsdoc is extracted from the
 * attached source if present.</p>
 * <p>If this element's openable is not consistent, then null is returned.</p>
 *
 * @exception JavaScriptModelException if this element does not exist or if an
 *      exception occurs while accessing its corresponding resource.
 * @return a source range corresponding to the jsdoc source or <code>null</code>
 * if no source is available, this element has no jsdoc comment or
 * this element's openable is not consistent
 * @see IOpenable#isConsistent()
 */
ISourceRange getJSdocRange() throws JavaScriptModelException;
/**
 * Returns the source range of this member's simple name,
 * or <code>null</code> if this member does not have a name
 * (for example, an initializer), or if this member does not have
 * associated source code (for example, a binary type).
 *
 * @exception JavaScriptModelException if this element does not exist or if an
 *      exception occurs while accessing its corresponding resource.
 * @return the source range of this member's simple name,
 * or <code>null</code> if this member does not have a name
 * (for example, an initializer), or if this member does not have
 * associated source code (for example, a binary type)
 */
ISourceRange getNameRange() throws JavaScriptModelException;
/**
 * Returns the position relative to the order this member is defined in the source.
 * Numbering starts at 1 (thus the first occurrence is occurrence 1, not occurrence 0).
 * <p>
 * Two members m1 and m2 that are equal (e.g. 2 fields with the same name in
 * the same type) can be distinguished using their occurrence counts. If member
 * m1 appears first in the source, it will have an occurrence count of 1. If member
 * m2 appears right after member m1, it will have an occurrence count of 2.
 * </p><p>
 * The occurrence count can be used to distinguish initializers inside a type
 * or anonymous types inside a method.
 * </p><p>
 * This is a handle-only method.  The member may or may not be present.
 * </p>
 *
 * @return the position relative to the order this member is defined in the source
 */
int getOccurrenceCount();
/**
 * Returns the JavaScript type root in which this member is declared.
 * This is a handle-only method.
 *
 * @return the JavaScript type root in which this member is declared.
 */
ITypeRoot getTypeRoot();
/**
 * Returns the local or anonymous type declared in this source member with the given simple name and/or
 * with the specified position relative to the order they are defined in the source.
 * The name is empty if it is an anonymous type.
 * Numbering starts at 1 (thus the first occurrence is occurrence 1, not occurrence 0).
 * This is a handle-only method. The type may or may not exist.
 * Throws a <code>RuntimeException</code> if this member is not a source member.
 *
 * @param name the given simple name
 * @param occurrenceCount the specified position
 * @return the type with the given name and/or with the specified position relative to the order they are defined in the source
 */
IType getType(String name, int occurrenceCount);
/**
 * Returns whether this member is from a non-editable file.
 * This is a handle-only method.
 *
 * @return <code>true</code> if from a non-editable file, and <code>false</code> if
 *   from a javaScript unit
 */
boolean isBinary();
}
