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
package org.eclipse.wst.jsdt.internal.ui.infoviews;

import org.eclipse.osgi.util.NLS;

/**
 * Helper class to get NLSed messages.
 */
final class InfoViewMessages extends NLS {

	private static final String BUNDLE_NAME= InfoViewMessages.class.getName();

	private InfoViewMessages() {
		// Do not instantiate
	}

	public static String CopyAction_label;
	public static String CopyAction_tooltip;
	public static String CopyAction_description;
	public static String SelectAllAction_label;
	public static String SelectAllAction_tooltip;
	public static String SelectAllAction_description;
	public static String GotoInputAction_label;
	public static String GotoInputAction_tooltip;
	public static String GotoInputAction_description;
	public static String CopyToClipboard_error_title;
	public static String CopyToClipboard_error_message;
	public static String JavadocView_error_noBrowser_title;
	public static String JavadocView_error_noBrowser_message;
	public static String JavadocView_error_noBrowser_doNotWarn;
	public static String JavadocView_noAttachments;
	public static String JavadocView_noAttachedSource;
	public static String JavadocView_noAttachedJavadoc;
	public static String JavadocView_noInformation;
	public static String JavadocView_error_gettingJavadoc;

	static {
		NLS.initializeMessages(BUNDLE_NAME, InfoViewMessages.class);
	}
}
