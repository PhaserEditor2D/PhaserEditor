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
package org.eclipse.wst.jsdt.internal.ui.text.folding;

import org.eclipse.osgi.util.NLS;

/**
 * Helper class to get NLSed messages.
 */
final class FoldingMessages extends NLS {

	private static final String BUNDLE_NAME= FoldingMessages.class.getName();

	private FoldingMessages() {
		// Do not instantiate
	}

	public static String DefaultJavaFoldingPreferenceBlock_title;
	public static String DefaultJavaFoldingPreferenceBlock_comments;
//	public static String DefaultJavaFoldingPreferenceBlock_innerTypes;
	public static String DefaultJavaFoldingPreferenceBlock_methods;
//	public static String DefaultJavaFoldingPreferenceBlock_imports;
	public static String DefaultJavaFoldingPreferenceBlock_headers;
	public static String EmptyJavaFoldingPreferenceBlock_emptyCaption;
	public static String JavaFoldingStructureProviderRegistry_warning_providerNotFound_resetToDefault;

	static {
		NLS.initializeMessages(BUNDLE_NAME, FoldingMessages.class);
	}
}
