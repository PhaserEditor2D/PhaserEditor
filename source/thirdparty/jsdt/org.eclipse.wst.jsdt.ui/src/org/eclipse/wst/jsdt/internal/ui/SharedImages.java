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
package org.eclipse.wst.jsdt.internal.ui;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.wst.jsdt.ui.ISharedImages;

/**
 * Default implementation of ISharedImages
 */
public class SharedImages implements ISharedImages {
	
	public SharedImages() {
	}
		
	/* (Non-Javadoc)
	 * Method declared in ISharedImages
	 */
	public Image getImage(String key) {
		return JavaPluginImages.get(key);
	}
	
	/* (Non-Javadoc)
	 * Method declared in ISharedImages
	 */
	public ImageDescriptor getImageDescriptor(String key) {
		return JavaPluginImages.getDescriptor(key);
	}

}
