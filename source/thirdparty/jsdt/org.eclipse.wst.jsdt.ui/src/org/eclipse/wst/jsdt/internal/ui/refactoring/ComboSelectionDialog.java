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
package org.eclipse.wst.jsdt.internal.ui.refactoring;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;


class ComboSelectionDialog extends Dialog{
			
	private String fSelection= null;
	private final String fShellTitle;
	private final String fLabelText;
	private final String[] fAllowedStrings;
	private final int fInitialSelectionIndex;
	public ComboSelectionDialog(Shell parentShell, String shellTitle, String labelText, String[] comboStrings, int initialSelectionIndex) {
		super(parentShell);
		Assert.isNotNull(shellTitle);
		Assert.isNotNull(labelText);
		Assert.isTrue(comboStrings.length > 0);
		Assert.isTrue(initialSelectionIndex >= 0 && initialSelectionIndex < comboStrings.length);
		fShellTitle= shellTitle;
		fLabelText= labelText;
		fAllowedStrings= comboStrings;
		fInitialSelectionIndex= initialSelectionIndex;
	}

	String getSelectedString(){
		return fSelection;
	}
	
	/*
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createDialogArea(Composite parent) {
		getShell().setText(fShellTitle);
		
		Composite composite = (Composite)super.createDialogArea(parent);
		Composite innerComposite = new Composite(composite, SWT.NONE);
		innerComposite.setLayoutData(new GridData());
		GridLayout gl= new GridLayout();
		gl.numColumns= 2;
		innerComposite.setLayout(gl);
		
		Label label= new Label(innerComposite, SWT.NONE);
		label.setText(fLabelText);
		label.setLayoutData(new GridData());

		final Combo combo= new Combo(innerComposite, SWT.READ_ONLY);
		for (int i = 0; i < fAllowedStrings.length; i++) {
			combo.add(fAllowedStrings[i]);
		}
		combo.select(fInitialSelectionIndex);
		fSelection= combo.getItem(combo.getSelectionIndex());
		GridData gd= new GridData();
		gd.widthHint= convertWidthInCharsToPixels(getMaxStringLength());
		combo.setLayoutData(gd);
		combo.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				fSelection= combo.getItem(combo.getSelectionIndex());
			}
		});
		applyDialogFont(composite);		
		return composite;
	}
	
	private int getMaxStringLength() {
		int max= 0;
		for (int i= 0; i < fAllowedStrings.length; i++) {
			max= Math.max(max, fAllowedStrings[i].length());
		}
		return max;
	}
}
