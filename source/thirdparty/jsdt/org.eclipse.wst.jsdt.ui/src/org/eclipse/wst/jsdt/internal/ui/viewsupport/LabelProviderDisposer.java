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


import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;

/**
 * Helper class to manage images that should be disposed when a control is disposed
 * contol.addWidgetListener(new LabelProviderDisposer(myLabelProvider));
 */
public class LabelProviderDisposer implements DisposeListener {
	
	private ILabelProvider fLabelProvider;
		
	public LabelProviderDisposer(ILabelProvider labelProvider) {
		fLabelProvider= labelProvider;
	}
	
	public void widgetDisposed(DisposeEvent e) {
		fLabelProvider.dispose();
	}
}


