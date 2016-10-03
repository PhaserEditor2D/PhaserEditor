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
package org.eclipse.wst.jsdt.internal.ui.text.folding;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.wst.jsdt.ui.text.folding.IJavaFoldingPreferenceBlock;


/**
 * Empty preference block for extensions to the
 * <code>org.eclipse.wst.jsdt.ui.javaFoldingStructureProvider</code> extension
 * point that do not specify their own.
 *
 * 
 */
class EmptyJavaFoldingPreferenceBlock implements IJavaFoldingPreferenceBlock {
	/*
	 * @see org.eclipse.wst.jsdt.internal.ui.text.folding.IJavaFoldingPreferences#createControl(org.eclipse.swt.widgets.Group)
	 */
	public Control createControl(Composite composite) {
		Composite inner= new Composite(composite, SWT.NONE);
		inner.setLayout(new GridLayout(3, false));

		Label label= new Label(inner, SWT.CENTER);
		GridData gd= new GridData(GridData.FILL_BOTH);
		gd.widthHint= 30;
		label.setLayoutData(gd);

		label= new Label(inner, SWT.CENTER);
		label.setText(FoldingMessages.EmptyJavaFoldingPreferenceBlock_emptyCaption);
		gd= new GridData(GridData.CENTER);
		label.setLayoutData(gd);

		label= new Label(inner, SWT.CENTER);
		gd= new GridData(GridData.FILL_BOTH);
		gd.widthHint= 30;
		label.setLayoutData(gd);

		return inner;
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.ui.text.folding.IJavaFoldingPreferenceBlock#initialize()
	 */
	public void initialize() {
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.ui.text.folding.IJavaFoldingPreferenceBlock#performOk()
	 */
	public void performOk() {
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.ui.text.folding.IJavaFoldingPreferenceBlock#performDefaults()
	 */
	public void performDefaults() {
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.ui.text.folding.IJavaFoldingPreferenceBlock#dispose()
	 */
	public void dispose() {
	}

}
