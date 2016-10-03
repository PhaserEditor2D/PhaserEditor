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
 package org.eclipse.wst.jsdt.internal.ui.dialogs;

import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchScope;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.ui.dialogs.TypeSelectionExtension;

/**
 * @deprecated use {@link OpenTypeSelectionDialog}
 */
public class OpenTypeSelectionDialog2 extends TypeSelectionDialog2 {

	private IDialogSettings fSettings;
	private Point fLocation;
	private Point fSize;

	private static final String DIALOG_SETTINGS= "org.eclipse.wst.jsdt.internal.ui.dialogs.OpenTypeSelectionDialog2"; //$NON-NLS-1$
	private static final String WIDTH= "width"; //$NON-NLS-1$
	private static final String HEIGHT= "height"; //$NON-NLS-1$
	
	public OpenTypeSelectionDialog2(Shell parent, boolean multi, IRunnableContext context, IJavaScriptSearchScope scope, int elementKinds) {
		this(parent, multi, context, scope, elementKinds, null);
	}
	
	public OpenTypeSelectionDialog2(Shell parent, boolean multi, IRunnableContext context, 
			IJavaScriptSearchScope scope, int elementKinds, TypeSelectionExtension extension) {
		super(parent, multi, context, scope, elementKinds, extension);
		IDialogSettings settings= JavaScriptPlugin.getDefault().getDialogSettings();
		fSettings= settings.getSection(DIALOG_SETTINGS);
		if (fSettings == null) {
			fSettings= new DialogSettings(DIALOG_SETTINGS);
			settings.addSection(fSettings);
			fSettings.put(WIDTH, 480);
			fSettings.put(HEIGHT, 320);
		}
	}
	
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(newShell, IJavaHelpContextIds.OPEN_TYPE_DIALOG);
	}

	protected Point getInitialSize() {
		Point result= super.getInitialSize();
		if (fSize != null) {
			result.x= Math.max(result.x, fSize.x);
			result.y= Math.max(result.y, fSize.y);
			Rectangle display= getShell().getDisplay().getClientArea();
			result.x= Math.min(result.x, display.width);
			result.y= Math.min(result.y, display.height);
		}
		return result;
	}
	
	protected Point getInitialLocation(Point initialSize) {
		Point result= super.getInitialLocation(initialSize);
		if (fLocation != null) {
			result.x= fLocation.x;
			result.y= fLocation.y;
			Rectangle display= getShell().getDisplay().getClientArea();
			int xe= result.x + initialSize.x;
			if (xe > display.width) {
				result.x-= xe - display.width; 
			}
			int ye= result.y + initialSize.y;
			if (ye > display.height) {
				result.y-= ye - display.height; 
			}
		}
		return result;
	}
	
	protected Control createDialogArea(Composite parent) {
		readSettings();
		return super.createDialogArea(parent);
	}
	
	public boolean close() {
		writeSettings();
		return super.close();
	}
	
	/**
	 * Initializes itself from the dialog settings with the same state
	 * as at the previous invocation.
	 */
	private void readSettings() {
		try {
			int x= fSettings.getInt("x"); //$NON-NLS-1$
			int y= fSettings.getInt("y"); //$NON-NLS-1$
			fLocation= new Point(x, y);
		} catch (NumberFormatException e) {
			fLocation= null;
		}
		try {
			int width= fSettings.getInt("width"); //$NON-NLS-1$
			int height= fSettings.getInt("height"); //$NON-NLS-1$
			fSize= new Point(width, height);

		} catch (NumberFormatException e) {
			fSize= null;
		}
	}

	/**
	 * Stores it current configuration in the dialog store.
	 */
	private void writeSettings() {
		Point location= getShell().getLocation();
		fSettings.put("x", location.x); //$NON-NLS-1$
		fSettings.put("y", location.y); //$NON-NLS-1$

		Point size= getShell().getSize();
		fSettings.put("width", size.x); //$NON-NLS-1$
		fSettings.put("height", size.y); //$NON-NLS-1$
	}	
}
