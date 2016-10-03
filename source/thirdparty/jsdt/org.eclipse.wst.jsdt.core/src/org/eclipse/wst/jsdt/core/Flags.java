/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     IBM Corporation - added constant AccDefault
 *     IBM Corporation - added constants AccBridge and AccVarargs for J2SE 1.5
 *******************************************************************************/
package org.eclipse.wst.jsdt.core;

import org.eclipse.wst.jsdt.internal.compiler.classfmt.ClassFileConstants;

/**
 * Utility class for decoding modifier flags in JavaScript elements.
 * <p>
 * This class provides static methods only; it is not intended to be
 * instantiated or subclassed by clients.
 * </p>
 *
 * @see IMember#getFlags()
 *  
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public final class Flags {

	/**
	 * Constant representing the absence of any flag
	 */
	public static final int AccDefault = ClassFileConstants.AccDefault;
	/**
	 * Public access flag. 
	 *
	 * <b>This flag only applies to ECMAScript 4 which is not yet supported</b>
	 */
	public static final int AccPublic = ClassFileConstants.AccPublic;
	/**
	 * Private access flag.  
	 *
	 * <b>This flag only applies to ECMAScript 4 which is not yet supported</b>
	 */
	public static final int AccPrivate = ClassFileConstants.AccPrivate;
	/**
	 * Protected access flag.  
	 *
	 * <b>This flag only applies to ECMAScript 4 which is not yet supported</b>
	 */
	public static final int AccProtected = ClassFileConstants.AccProtected;
	/**
	 * Static access flag. 
	 */
	public static final int AccStatic = ClassFileConstants.AccStatic;
	/**
	 * Abstract property flag. 
	 *
	 * <b>This flag only applies to ECMAScript 4 which is not yet supported</b>
	 */
	public static final int AccAbstract = ClassFileConstants.AccAbstract;
	/**
	 * Super property flag.  
	 *
	 * <b>This flag only applies to ECMAScript 4 which is not yet supported</b>
	 */
	public static final int AccSuper = ClassFileConstants.AccSuper;
	/**
	 * Deprecated property flag.  
	 */
	public static final int AccDeprecated = ClassFileConstants.AccDeprecated;

	/**=
	 * Varargs method property 
	 * Used to flag variable arity method declarations.
	 *
	 * <b>This flag only applies to ECMAScript 4 which is not yet supported</b>
	 */
	public static final int AccVarargs = ClassFileConstants.AccVarargs;

	/**
	 * Not instantiable.
	 */
	private Flags() {
		// Not instantiable
	}
	/**
	 * Returns whether the given integer includes the <code>abstract</code> modifier.
	 *
	 * <p><b>Note: This Method only applies to ECMAScript 4 which is not yet supported</b></p>
	 *
	 * @param flags the flags
	 * @return <code>true</code> if the <code>abstract</code> modifier is included
	 */
	public static boolean isAbstract(int flags) {
		return (flags & AccAbstract) != 0;
	}
	/**
	 * Returns whether the given integer includes the indication that the
	 * element is deprecated (<code>@deprecated</code> tag in jsdoc comment).
	 *
	 * @param flags the flags
	 * @return <code>true</code> if the element is marked as deprecated
	 */
	public static boolean isDeprecated(int flags) {
		return (flags & AccDeprecated) != 0;
	}
	/*
	 * Returns whether the given integer does not include one of the
	 * <code>public</code>, <code>private</code>, or <code>protected</code> flags.
	 *
	 * <p><b>Note: This Method only applies to ECMAScript 4 which is not yet supported</b></p>
	 *
	 * @param flags the flags
	 * @return <code>true</code> if no visibility flag is set
	 */
	public static boolean isPackageDefault(int flags) {
		return (flags & (AccPublic | AccProtected | AccPrivate)) == 0;
	}
	/**
	 * Returns whether the given integer includes the <code>private</code> modifier.
	 *
	 * <p><b>Note: This Method only applies to ECMAScript 4 which is not yet supported</b></p>
	 *
	 * @param flags the flags
	 * @return <code>true</code> if the <code>private</code> modifier is included
	 */
	public static boolean isPrivate(int flags) {
		return (flags & AccPrivate) != 0;
	}
	/**
	 * Returns whether the given integer includes the <code>protected</code> modifier.
	 *
	 * <p><b>Note: This Method only applies to ECMAScript 4 which is not yet supported</b></p>
	 *
	 * @param flags the flags
	 * @return <code>true</code> if the <code>protected</code> modifier is included
	 */
	public static boolean isProtected(int flags) {
		return (flags & AccProtected) != 0;
	}
	/**
	 * Returns whether the given integer includes the <code>public</code> modifier.
	 *
	 * <p><b>Note: This Method only applies to ECMAScript 4 which is not yet supported</b></p>
	 *
	 * @param flags the flags
	 * @return <code>true</code> if the <code>public</code> modifier is included
	 */
	public static boolean isPublic(int flags) {
		return (flags & AccPublic) != 0;
	}
	/**
	 * Returns whether the given integer includes the <code>static</code> modifier.
	 *
	 * @param flags the flags
	 * @return <code>true</code> if the <code>static</code> modifier is included
	 */
	public static boolean isStatic(int flags) {
		return (flags & AccStatic) != 0;
	}
	/**
	 * Returns whether the given integer includes the <code>super</code> modifier.
	 *
	 * <p><b>Note: This Method only applies to ECMAScript 4 which is not yet supported</b></p>
	 *
	 * @param flags the flags
	 * @return <code>true</code> if the <code>super</code> modifier is included
	 */
	public static boolean isSuper(int flags) {
		return (flags & AccSuper) != 0;
	}

	/*
	 * Returns whether the given integer has the <code>AccVarargs</code>
	 * bit set.
	 *
	 * @param flags the flags
	 * @return <code>true</code> if the <code>AccVarargs</code> flag is included
	 * @see #AccVarargs
	 */
	public static boolean isVarargs(int flags) {
		return (flags & AccVarargs) != 0;
	}

	/**
	 * Returns a standard string describing the given modifier flags.
	 * Only modifier flags are included in the output; deprecated,
	 * synthetic, bridge, etc. flags are ignored.
	 * <p>
	 * The flags are output in the following order:
	 * <pre>
	 *   <code>public</code> <code>protected</code> <code>private</code>
	 *   <code>static</code>
	 *   <code>abstract</code> <code>final</code> <code>native</code> <code>synchronized</code> <code>transient</code> <code>volatile</code> <code>strictfp</code>
	 * </pre>
	 * </p>
	 * <p>
	 * Examples results:
	 * <pre>
	 *	  <code>"public static final"</code>
	 *	  <code>"private native"</code>
	 * </pre>
	 * </p>
	 *
	 * @param flags the flags
	 * @return the standard string representation of the given flags
	 */
	public static String toString(int flags) {
		StringBuffer sb = new StringBuffer();

		if (isPublic(flags))
			sb.append("public "); //$NON-NLS-1$
		if (isPrivate(flags))
			sb.append("private "); //$NON-NLS-1$
		if (isStatic(flags))
			sb.append("static "); //$NON-NLS-1$
		if (isAbstract(flags))
			sb.append("abstract "); //$NON-NLS-1$

		int len = sb.length();
		if (len == 0)
			return ""; //$NON-NLS-1$
		sb.setLength(len - 1);
		return sb.toString();
	}
}
