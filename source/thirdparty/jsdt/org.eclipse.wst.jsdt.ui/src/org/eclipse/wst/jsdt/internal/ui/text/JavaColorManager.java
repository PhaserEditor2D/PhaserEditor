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


import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.wst.jsdt.ui.text.IColorManager;
import org.eclipse.wst.jsdt.ui.text.IColorManagerExtension;

/**
 * Java color manager.
 */
public class JavaColorManager implements IColorManager, IColorManagerExtension {

	protected Map fKeyTable= new HashMap(10);
	protected Map fDisplayTable= new HashMap(2);

	/**
	 * Flag which tells if the colors are automatically disposed when
	 * the current display gets disposed.
	 */
	private boolean fAutoDisposeOnDisplayDispose;


	/**
	 * Creates a new Java color manager which automatically
	 * disposes the allocated colors when the current display
	 * gets disposed.
	 */
	public JavaColorManager() {
		this(true);
	}

	/**
	 * Creates a new Java color manager.
	 *
	 * @param autoDisposeOnDisplayDispose 	if <code>true</code>  the color manager
	 * automatically disposes all managed colors when the current display gets disposed
	 * and all calls to {@link org.eclipse.jface.text.source.ISharedTextColors#dispose()} are ignored.
	 *
	 * 
	 */
	public JavaColorManager(boolean autoDisposeOnDisplayDispose) {
		fAutoDisposeOnDisplayDispose= autoDisposeOnDisplayDispose;
	}

	public void dispose(Display display) {
		Map colorTable= (Map) fDisplayTable.get(display);
		if (colorTable != null) {
			Iterator e= colorTable.values().iterator();
			while (e.hasNext()) {
				Color color= (Color)e.next();
				if (color != null && !color.isDisposed())
					color.dispose();
			}
		}
	}

	/*
	 * @see IColorManager#getColor(RGB)
	 */
	public Color getColor(RGB rgb) {

		if (rgb == null)
			return null;

		final Display display= Display.getCurrent();
		Map colorTable= (Map) fDisplayTable.get(display);
		if (colorTable == null) {
			colorTable= new HashMap(10);
			fDisplayTable.put(display, colorTable);
			if (fAutoDisposeOnDisplayDispose) {
				display.disposeExec(new Runnable() {
					public void run() {
						dispose(display);
					}
				});
			}
		}

		Color color= (Color) colorTable.get(rgb);
		if (color == null) {
			color= new Color(Display.getCurrent(), rgb);
			colorTable.put(rgb, color);
		}

		return color;
	}

	/*
	 * @see IColorManager#dispose
	 */
	public void dispose() {
		if (!fAutoDisposeOnDisplayDispose)
			dispose(Display.getCurrent());
	}

	/*
	 * @see IColorManager#getColor(String)
	 */
	public Color getColor(String key) {

		if (key == null)
			return null;

		RGB rgb= (RGB) fKeyTable.get(key);
		return getColor(rgb);
	}

	/*
	 * @see IColorManagerExtension#bindColor(String, RGB)
	 */
	public void bindColor(String key, RGB rgb) {
		Object value= fKeyTable.get(key);
		if (value != null)
			throw new UnsupportedOperationException();

		fKeyTable.put(key, rgb);
	}

	/*
	 * @see IColorManagerExtension#unbindColor(String)
	 */
	public void unbindColor(String key) {
		fKeyTable.remove(key);
	}
}
