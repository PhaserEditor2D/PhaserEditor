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
package org.eclipse.wst.jsdt.internal.ui.text;


/**
 * Mix-in for any rule that changes its behavior based on the Java source
 * version.
 *
 * 
 */
public interface ISourceVersionDependent {

	/**
	 * Sets the configured java source version to one of the
	 * <code>JavaScriptCore.VERSION_X_Y</code> values.
	 *
	 * @param version the new java source version
	 * @see org.eclipse.wst.jsdt.core.JavaScriptCore
	 */
	void setSourceVersion(String version);
}
