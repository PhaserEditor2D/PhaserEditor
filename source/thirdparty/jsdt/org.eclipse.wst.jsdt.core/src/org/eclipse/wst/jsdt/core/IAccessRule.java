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

import org.eclipse.core.runtime.IPath;

/**
 * Describes an access rule to source and class files on a includepath entry.
 * An access rule is composed of a file pattern and a kind (accessible,
 * non accessible, or discouraged).
 * <p>
 * On a given includepath entry, the access rules are considered in the order given
 * when the entry was created. When a source matches an access
 * rule's pattern, the access rule's kind define whether the file is considered
 * accessible, non accessible, or its access is discouraged. If the source
 * file doesn't match any accessible rule, it is considered accessible. A source
 * file that is not accessible or discouraged can still be refered to but it is tagged as being not
 * accessible - the JavaScript validator will create a problem marker for example.
 * The severity of the marker created from a non accessible rule is controled through
 * the {@link JavaScriptCore#COMPILER_PB_FORBIDDEN_REFERENCE} compiler option.
 * The severity of the marker created from a discouraged rule is controled through
 * the {@link JavaScriptCore#COMPILER_PB_DISCOURAGED_REFERENCE} compiler option.
 * Note this is different from inclusion and exclusion patterns on source includepath entries,
 * where a source file that is excluded is not even validated.
 * Files patterns look like relative file paths with wildcards and are interpreted relative
 * to each entry's path.
 * File patterns are case-sensitive and they can contain '**', '*' or '?' wildcards (see
 * {@link IIncludePathEntry#getExclusionPatterns()} for the full description
 * of their syntax and semantics).
 * Note that file patterns must not include the file extension.
 * <code>com/xyz/tests/MyClass</code> is a valid file pattern, whereas
 * <code>com/xyz/tests/MyClass.class</code> is not valid.
 * </p>
 *
 *  
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public interface IAccessRule {

	/**
	 * Constant indicating that files matching the rule's pattern are accessible.
	 */
	int K_ACCESSIBLE = 0;

	/**
	 * Constant indicating that files matching the rule's pattern are non accessible.
	 */
	int K_NON_ACCESSIBLE = 1;

	/**
	 * Constant indicating that access to the files matching the rule's pattern is discouraged.
	 */
	int K_DISCOURAGED = 2;

	/**
	 * <p>Flag indicating that whether a type matching this rule should be ignored iff a type with
	 * the same qualified name can be found on a later includepath entry with a better
	 * accessibility.</p>
	 * <p>E.g. if a type p.X matches a rule K_NON_ACCESSIBLE | IGNORE_IF_BETTER
	 * on a library entry 'lib1' and another type p.X also matches a rule
	 * K_DISCOURAGED on library entry 'lib2' ('lib2' being after 'lib1' on the
	 * includepath), then p.X from 'lib2' will be used and reported as
	 * discouraged.</p>
	 *
	 */
	int IGNORE_IF_BETTER = 0x100;

	/**
	 * Returns the file pattern for this access rule.
	 *
	 * @return the file pattern for this access rule
	 */
	IPath getPattern();

	/**
	 * Returns the kind of this access rule (one of {@link #K_ACCESSIBLE}, {@link #K_NON_ACCESSIBLE}
	 * or {@link #K_DISCOURAGED}).
	 *
	 * @return the kind of this access rule
	 */
	int getKind();

	/**
	 * <p>Returns whether a type matching this rule should be ignored iff a type with
	 * the same qualified name can be found on a later includepath entry with a better
	 * accessibility.</p>
	 * <p>E.g. if a type p.X matches a rule K_NON_ACCESSIBLE | IGNORE_IF_BETTER
	 * on a library entry 'lib1' and another type p.X also matches a rule
	 * K_DISCOURAGED on library entry 'lib2' ('lib2' being after 'lib1' on the
	 * includepath), then p.X from 'lib2' will be used and reported as
	 * discouraged.</p>
	 *
	 * @return whether a type matching this rule should be ignored iff a type
	 *              with the same qualified name can be found on a later includepath
	 *              entry with a better accessibility
	 */
	boolean ignoreIfBetter();

}
