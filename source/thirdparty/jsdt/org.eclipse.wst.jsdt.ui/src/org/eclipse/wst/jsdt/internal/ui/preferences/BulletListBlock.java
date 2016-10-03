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
package org.eclipse.wst.jsdt.internal.ui.preferences;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.Bullet;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GlyphMetrics;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

/**
 * Each line of the given text is preceded by a bullet.
 */
public class BulletListBlock {

	private StyledText fStyledText;
	private boolean fEnabled;
	private String fText;
	
	public BulletListBlock() {
		fEnabled= true;
		fText= ""; //$NON-NLS-1$
	}

	public Control createControl(Composite parent) {
		fStyledText= new StyledText(parent, SWT.FLAT | SWT.BORDER | SWT.READ_ONLY);
		
		final GridData data= new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL);
		fStyledText.setLayoutData(data);
		configureStyledText(fText, fEnabled);
		
		return fStyledText;
	}

	public void setText(String text) {
		fText= text;
		configureStyledText(fText, fEnabled);
	}
	
	public void setEnabled(boolean enabled) {
		fEnabled= enabled;
		configureStyledText(fText, fEnabled);
	}

	private void configureStyledText(String text, boolean enabled) {
		if (fStyledText == null)
			return;
		
		fStyledText.setText(text);
		int count= fStyledText.getCharCount();
		if (count == 0)
			return;
		
		Color foreground= enabled ? null : Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY);

		fStyledText.setStyleRange(new StyleRange(0, count, foreground, null));
					
		StyleRange styleRange= new StyleRange(0, count, foreground, null);
		styleRange.metrics= new GlyphMetrics(0, 0, 20);
		fStyledText.setLineBullet(0, fStyledText.getLineCount(), new Bullet(styleRange));	
		
		fStyledText.setEnabled(enabled);
	}
}
