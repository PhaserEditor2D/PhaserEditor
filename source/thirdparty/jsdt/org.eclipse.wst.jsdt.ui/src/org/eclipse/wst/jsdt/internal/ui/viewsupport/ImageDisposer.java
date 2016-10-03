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
package org.eclipse.wst.jsdt.internal.ui.viewsupport;

import org.eclipse.core.runtime.Assert;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;


/**
 * Helper class to manage images that should be disposed when a control is disposed
 * contol.addWidgetListener(new ImageDisposer(myImage));
 */
public class ImageDisposer implements DisposeListener {
	
	private Image[] fImages;
		
	public ImageDisposer(Image image) {
		this(new Image[] { image });
	}
	
	public ImageDisposer(Image[] images) {
		Assert.isNotNull(images);
		fImages= images;		
	}
	
	/*
	 * @see WidgetListener#widgetDisposed
	 */
	public void widgetDisposed(DisposeEvent e) {
		if (fImages != null) {
			for (int i= 0; i < fImages.length; i++) {
				fImages[i].dispose();
			}
		}
	}
}
