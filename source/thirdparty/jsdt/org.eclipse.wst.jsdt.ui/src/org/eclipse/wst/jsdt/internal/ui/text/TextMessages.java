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
package org.eclipse.wst.jsdt.internal.ui.text;

import org.eclipse.osgi.util.NLS;

/**
 * Helper class to get NLSed messages.
 */
final class TextMessages extends NLS {

	private static final String BUNDLE_NAME= TextMessages.class.getName();

	private TextMessages() {
		// Do not instantiate
	}

	public static String JavaOutlineInformationControl_SortByDefiningTypeAction_label;
	public static String JavaOutlineInformationControl_SortByDefiningTypeAction_tooltip;
	public static String JavaOutlineInformationControl_SortByDefiningTypeAction_description;
	public static String JavaOutlineInformationControl_LexicalSortingAction_label;
	public static String JavaOutlineInformationControl_LexicalSortingAction_tooltip;
	public static String JavaOutlineInformationControl_LexicalSortingAction_description;
	public static String JavaOutlineInformationControl_GoIntoTopLevelType_label;
	public static String JavaOutlineInformationControl_GoIntoTopLevelType_tooltip;
	public static String JavaOutlineInformationControl_GoIntoTopLevelType_description;

	static {
		NLS.initializeMessages(BUNDLE_NAME, TextMessages.class);
	}
}
