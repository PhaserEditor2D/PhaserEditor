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
package org.eclipse.wst.jsdt.internal.ui.text.javadoc;

import org.eclipse.osgi.util.NLS;

/**
 * Helper class to get NLSed messages.
 */
final class JavaDocMessages extends NLS {

	private static final String BUNDLE_NAME= JavaDocMessages.class.getName();

	private JavaDocMessages() {
		// Do not instantiate
	}

	public static String JavaDoc2HTMLTextReader_parameters_section;
	public static String JavaDoc2HTMLTextReader_returns_section;
	public static String JavaDoc2HTMLTextReader_throws_section;
	public static String JavaDoc2HTMLTextReader_author_section;
	public static String JavaDoc2HTMLTextReader_see_section;
	public static String JavaDoc2HTMLTextReader_since_section;

	static {
		NLS.initializeMessages(BUNDLE_NAME, JavaDocMessages.class);
	}
}
