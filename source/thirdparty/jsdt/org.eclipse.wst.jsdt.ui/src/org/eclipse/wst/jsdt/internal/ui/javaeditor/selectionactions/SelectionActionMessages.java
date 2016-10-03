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
package org.eclipse.wst.jsdt.internal.ui.javaeditor.selectionactions;

import org.eclipse.osgi.util.NLS;

/**
 * Helper class to get NLSed messages.
 *
 * 
 */
final class SelectionActionMessages extends NLS {

	private static final String BUNDLE_NAME= SelectionActionMessages.class.getName();

	private SelectionActionMessages() {
		// Do not instantiate
	}

	public static String StructureSelect_error_title;
	public static String StructureSelect_error_message;
	public static String StructureSelectNext_label;
	public static String StructureSelectNext_tooltip;
	public static String StructureSelectNext_description;
	public static String StructureSelectPrevious_label;
	public static String StructureSelectPrevious_tooltip;
	public static String StructureSelectPrevious_description;
	public static String StructureSelectEnclosing_label;
	public static String StructureSelectEnclosing_tooltip;
	public static String StructureSelectEnclosing_description;
	public static String StructureSelectHistory_label;
	public static String StructureSelectHistory_tooltip;
	public static String StructureSelectHistory_description;

	public static String GotoNextMember_label;
	public static String GotoPreviousMember_label;

	static {
		NLS.initializeMessages(BUNDLE_NAME, SelectionActionMessages.class);
	}
}
