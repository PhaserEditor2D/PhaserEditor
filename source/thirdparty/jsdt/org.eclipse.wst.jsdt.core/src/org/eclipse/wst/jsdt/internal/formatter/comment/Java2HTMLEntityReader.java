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

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

/**
 * <code>SubstitutionTextReader</code> that will substitute html entities for
 * html symbols encountered in the original text. Line breaks and whitespaces
 * are preserved.
 *
 * @since 3.0
 */
public class Java2HTMLEntityReader extends SubstitutionTextReader {

	/** The hardcoded entity map. */
	private static final Map fgEntityLookup;

	static {
		fgEntityLookup= new HashMap(7);
		fgEntityLookup.put("<", "&lt;"); //$NON-NLS-1$ //$NON-NLS-2$
		fgEntityLookup.put(">", "&gt;"); //$NON-NLS-1$ //$NON-NLS-2$
		fgEntityLookup.put("&", "&amp;"); //$NON-NLS-1$ //$NON-NLS-2$
		fgEntityLookup.put("^", "&circ;"); //$NON-NLS-1$ //$NON-NLS-2$
		fgEntityLookup.put("~", "&tilde;"); //$NON-NLS-2$ //$NON-NLS-1$
		fgEntityLookup.put("\"", "&quot;"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Creates a new instance that will read from <code>reader</code>
	 *
	 * @param reader the source reader
	 */
	public Java2HTMLEntityReader(Reader reader) {
		super(reader);
		setSkipWhitespace(false);
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.ui.text.SubstitutionTextReader#computeSubstitution(int)
	 */
	protected String computeSubstitution(int c) {
		String lookup= (String) fgEntityLookup.get(String.valueOf((char) c));
		return lookup;
	}
}
