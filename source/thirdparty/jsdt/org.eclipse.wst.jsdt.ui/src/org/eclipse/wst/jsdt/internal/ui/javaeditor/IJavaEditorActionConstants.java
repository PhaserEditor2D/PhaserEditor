/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.javaeditor;

/**
 * Defines action IDs for private JavaEditor actions.
 */
public interface IJavaEditorActionConstants {

	/**
	 * ID of the action to toggle smart typing.
	 * Value: <code>"smartTyping"</code>
	 * 
	 */
	public static final String TOGGLE_SMART_TYPING= "smartTyping"; //$NON-NLS-1$

	/**
	 * ID of the smart typing status item
	 * Value: <code>"SmartTyping"</code>
	 * 
	 */
	public static final String STATUS_CATEGORY_SMART_TYPING= "SmartTyping"; //$NON-NLS-1$

	/**
	 * ID of the action to toggle the style of the presentation.
	 */
	public static final String TOGGLE_PRESENTATION= "togglePresentation"; //$NON-NLS-1$
	
	/**
	 * ID of the action to copy the qualified name.
	 * 
	 */
	public static final String COPY_QUALIFIED_NAME= "copyQualifiedName"; //$NON-NLS-1$

	/**
	 * ID of the action to show debugging information
	 * 
	 */
	public static final String STATUS_CATEGORY_OFFSET = "showOffset"; //$NON-NLS-1$
}
