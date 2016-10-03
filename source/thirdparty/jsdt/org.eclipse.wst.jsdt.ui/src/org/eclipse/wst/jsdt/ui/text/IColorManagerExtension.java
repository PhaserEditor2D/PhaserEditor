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
package org.eclipse.wst.jsdt.ui.text;

import org.eclipse.swt.graphics.RGB;


/**
 * Extends {@link org.eclipse.wst.jsdt.ui.text.IColorManager} with
 * the ability to bind and un-bind colors.
 *
 *
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves. */
public interface IColorManagerExtension {

	/**
	 * Remembers the given color specification under the given key.
	 *
	 * @param key the color key
	 * @param rgb the color specification
	 * @throws java.lang.UnsupportedOperationException if there is already a
	 * 	color specification remembered under the given key
	 */
	void bindColor(String key, RGB rgb);


	/**
	 * Forgets the color specification remembered under the given key.
	 *
	 * @param key the color key
	 */
	void unbindColor(String key);
}
