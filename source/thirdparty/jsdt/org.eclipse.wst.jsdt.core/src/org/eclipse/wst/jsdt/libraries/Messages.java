/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.libraries;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.wst.jsdt.libraries.messages"; //$NON-NLS-1$

	public static String BasicBrowserLibraryJsGlobalScopeContainerInitializer_CommonWebBrowser;

	public static String BasicBrowserLibraryJsGlobalScopeContainerInitializer_ECMA3Browser;
	public static String BasicBrowserLibraryJsGlobalScopeContainerInitializer_HTML5Browser;

	public static String BasicBrowserLibraryJsGlobalScopeContainerInitializer_ECMA3BrowserLibrary;

	public static String BasicBrowserLibraryJsGlobalScopeContainerInitializer_ECMA3DOM;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
