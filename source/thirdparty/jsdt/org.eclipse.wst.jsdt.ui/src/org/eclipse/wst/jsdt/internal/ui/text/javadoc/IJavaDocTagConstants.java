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

package org.eclipse.wst.jsdt.internal.ui.text.javadoc;

/**
 * Javadoc tag constants.
 *
 * 
 */
public interface IJavaDocTagConstants {

	/** Javadoc general tags */
	public static final String[] JAVADOC_GENERAL_TAGS= new String[] { "@author", "@deprecated", "@docRoot", "@exception", "@inheritDoc", "@link", "@linkplain", "@param", "@return", "@see", "@serial", "@serialData", "@serialField", "", "@throws", "@value", "@version" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$ //$NON-NLS-12$ //$NON-NLS-13$ //$NON-NLS-14$ //$NON-NLS-15$ //$NON-NLS-16$ //$NON-NLS-17$ 

	/** Javadoc link tags */
	public static final String[] JAVADOC_LINK_TAGS= new String[] { "@docRoot", "@inheritDoc", "@link", "@linkplain" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

	/** Javadoc parameter tags */
	public static final String[] JAVADOC_PARAM_TAGS= new String[] { "@exception", "@param", "@serialField", "@throws" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

	/** Javadoc reference tags */
	public static final String[] JAVADOC_REFERENCE_TAGS= new String[] { "@see" }; //$NON-NLS-1$

	/** Javadoc root tags */
	public static final String[] JAVADOC_ROOT_TAGS= new String[] { "@author", "@deprecated", "@return", "@see", "@serial", "@serialData", "", "@version", "@inheritDoc", "@category", "@value", "@literal", "@code" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$ //$NON-NLS-12$ //$NON-NLS-13$

	/** Javadoc tag prefix */
	public static final char JAVADOC_TAG_PREFIX= '@';
}
