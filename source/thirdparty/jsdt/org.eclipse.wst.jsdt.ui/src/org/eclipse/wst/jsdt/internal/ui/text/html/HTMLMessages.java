// COPIED FROM org.eclipse.jface.internal.text.html
// to get around "discouraged access" errors
/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.text.html;

import com.ibm.icu.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;


/**
 * Helper class to get NLSed messages.
 *
 * 
 */
class HTMLMessages {

	private static final String RESOURCE_BUNDLE= HTMLMessages.class.getName();

	private static ResourceBundle fgResourceBundle= ResourceBundle.getBundle(RESOURCE_BUNDLE);

	private HTMLMessages() {
	}

	/**
	 * Gets a string from the resource bundle.
	 *
	 * @param key the string used to get the bundle value, must not be null
	 * @return the string from the resource bundle
	 */
	public static String getString(String key) {
		try {
			return fgResourceBundle.getString(key);
		} catch (MissingResourceException e) {
			return "!" + key + "!";//$NON-NLS-2$ //$NON-NLS-1$
		}
	}

	/**
	 * Gets a string from the resource bundle and formats it with the given arguments.
	 *
	 * @param key the string used to get the bundle value, must not be null
	 * @param args the arguments used to format the string
	 * @return the formatted string
	 */
	public static String getFormattedString(String key, Object[] args) {
		String format= null;
		try {
			format= fgResourceBundle.getString(key);
		} catch (MissingResourceException e) {
			return "!" + key + "!";//$NON-NLS-2$ //$NON-NLS-1$
		}
		return MessageFormat.format(format, args);
	}

	/**
	 * Gets a string from the resource bundle and formats it with the given argument.
	 *
	 * @param key the string used to get the bundle value, must not be null
	 * @param arg the argument used to format the string
	 * @return the formatted string
	 */
	public static String getFormattedString(String key, Object arg) {
		String format= null;
		try {
			format= fgResourceBundle.getString(key);
		} catch (MissingResourceException e) {
			return "!" + key + "!";//$NON-NLS-2$ //$NON-NLS-1$
		}
		if (arg == null)
			arg= ""; //$NON-NLS-1$
		return MessageFormat.format(format, new Object[] { arg });
	}
}
