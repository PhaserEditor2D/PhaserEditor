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
package org.eclipse.wst.jsdt.internal.ui.refactoring.nls.search;

import org.eclipse.osgi.util.NLS;

/**
 * Helper class to get NLSed messages.
 */
final class NLSSearchMessages extends NLS {

	private static final String BUNDLE_NAME= NLSSearchMessages.class.getName();

	private NLSSearchMessages() {
		// Do not instantiate
	}

	public static String NLSSearchQuery_label;
	public static String NLSSearchQuery_oneProblemInScope_description;
	public static String NLSSearchQuery_propertiesNotExists;
	public static String NLSSearchQuery_wrapperNotExists;
	public static String NLSSearchQuery_xProblemsInScope_description;
	
	public static String NLSSearchResultCollector_duplicateKeys;
	public static String NLSSearchResultCollector_unusedKeys;
	public static String NLSSearchResultLabelProvider2_undefinedKeys;
	public static String NLSSearchResultRequestor_searching;
	
	public static String SearchOperation_pluralLabelPatternPostfix;
	public static String SearchOperation_singularLabelPostfix;
	
	static {
		NLS.initializeMessages(BUNDLE_NAME, NLSSearchMessages.class);
	}
}
