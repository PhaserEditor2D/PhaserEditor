/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.wst.jsdt.internal.formatter.comment;

/**
 * Javadoc tag constants.
 *
 * @since 3.0
 */
public interface IJavaDocTagConstants {

	/** Javadoc break tags */
	public static final char[][] JAVADOC_BREAK_TAGS = new char[][] {
		"dd".toCharArray(), //$NON-NLS-1$
		"dt".toCharArray(), //$NON-NLS-1$
		"li".toCharArray(), //$NON-NLS-1$
		"td".toCharArray(), //$NON-NLS-1$
		"th".toCharArray(), //$NON-NLS-1$
		"tr".toCharArray(), //$NON-NLS-1$
		"h1".toCharArray(), //$NON-NLS-1$
		"h2".toCharArray(), //$NON-NLS-1$
		"h3".toCharArray(), //$NON-NLS-1$
		"h4".toCharArray(), //$NON-NLS-1$
		"h5".toCharArray(), //$NON-NLS-1$
		"h6".toCharArray(), //$NON-NLS-1$
		"q".toCharArray() //$NON-NLS-1$
	};

	/** Javadoc single break tag */
	public static final char[][] JAVADOC_SINGLE_BREAK_TAG= new char[][] { "br".toCharArray() }; //$NON-NLS-1$

	/** Javadoc code tags */
	public static final char[][] JAVADOC_CODE_TAGS= new char[][] { "pre".toCharArray() }; //$NON-NLS-1$

	/** Javadoc immutable tags */
	public static final char[][] JAVADOC_IMMUTABLE_TAGS= new char[][] {
			"code".toCharArray(), //$NON-NLS-1$
			"em".toCharArray(), //$NON-NLS-1$
			"pre".toCharArray(), //$NON-NLS-1$
			"q".toCharArray(), //$NON-NLS-1$
			"tt".toCharArray() //$NON-NLS-1$
	};

	/** Javadoc new line tags */
	public static final char[][] JAVADOC_NEWLINE_TAGS= new char[][] {
			"dd".toCharArray(), //$NON-NLS-1$
			"dt".toCharArray(), //$NON-NLS-1$
			"li".toCharArray(), //$NON-NLS-1$
			"td".toCharArray(), //$NON-NLS-1$
			"th".toCharArray(), //$NON-NLS-1$
			"tr".toCharArray(), //$NON-NLS-1$
			"h1".toCharArray(), //$NON-NLS-1$
			"h2".toCharArray(), //$NON-NLS-1$
			"h3".toCharArray(), //$NON-NLS-1$
			"h4".toCharArray(), //$NON-NLS-1$
			"h5".toCharArray(), //$NON-NLS-1$
			"h6".toCharArray(), //$NON-NLS-1$
			"q".toCharArray() //$NON-NLS-1$
	};

	/** Javadoc parameter tags */
	public static final char[][] JAVADOC_PARAM_TAGS= new char[][] {
			"@exception".toCharArray(), //$NON-NLS-1$
			"@param".toCharArray(), //$NON-NLS-1$
			"@serialField".toCharArray(), //$NON-NLS-1$
			"@throws".toCharArray() //$NON-NLS-1$
	};

	/** Javadoc separator tags */
	public static final char[][] JAVADOC_SEPARATOR_TAGS= new char[][] {
			"dl".toCharArray(), //$NON-NLS-1$
			"hr".toCharArray(), //$NON-NLS-1$
			"nl".toCharArray(), //$NON-NLS-1$
			"p".toCharArray(), //$NON-NLS-1$
			"pre".toCharArray(), //$NON-NLS-1$
			"ul".toCharArray(), //$NON-NLS-1$
			"ol".toCharArray() //$NON-NLS-1$
	};

	/** Javadoc tag prefix */
	public static final char JAVADOC_TAG_PREFIX= '@';

	/** Link tag postfix */
	public static final char LINK_TAG_POSTFIX= '}';

	/** Link tag prefix */
	public static final String LINK_TAG_PREFIX_STRING = "{@"; //$NON-NLS-1$

	public static final char[] LINK_TAG_PREFIX= LINK_TAG_PREFIX_STRING.toCharArray();


	/** Comment root tags */
	public static final char[][] COMMENT_ROOT_TAGS= new char[][] {
			"@deprecated".toCharArray(), //$NON-NLS-1$
			"@see".toCharArray(), //$NON-NLS-1$
			"@since".toCharArray(), //$NON-NLS-1$
			"@version".toCharArray() //$NON-NLS-1$
	};

	/** Tag prefix of comment tags */
	public static final char COMMENT_TAG_PREFIX= '@';
}
