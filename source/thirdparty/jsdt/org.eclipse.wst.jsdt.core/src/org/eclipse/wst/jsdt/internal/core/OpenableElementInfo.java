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
package org.eclipse.wst.jsdt.internal.core;


/** Element info for IOpenable elements. */
public class OpenableElementInfo extends JavaElementInfo {

	/**
	 * Is the structure of this element known
	 * @see org.eclipse.wst.jsdt.core.IJavaScriptElement#isStructureKnown()
	 */
	protected boolean isStructureKnown = false;

	/**
	 * @see org.eclipse.wst.jsdt.core.IJavaScriptElement#isStructureKnown()
	 */
	public boolean isStructureKnown() {
		return this.isStructureKnown;
	}

	/**
	 * Sets whether the structure of this element known
	 * @see org.eclipse.wst.jsdt.core.IJavaScriptElement#isStructureKnown()
	 */
	public void setIsStructureKnown(boolean newIsStructureKnown) {
		this.isStructureKnown = newIsStructureKnown;
	}
}
