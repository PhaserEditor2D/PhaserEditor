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

package org.eclipse.wst.jsdt.internal.ui.text;


import java.util.List;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.wst.jsdt.ui.text.IColorManager;


/**
 *
 */
public final class SingleTokenJavaScanner extends AbstractJavaScanner{


	private String[] fProperty;

	public SingleTokenJavaScanner(IColorManager manager, IPreferenceStore store, String property) {
		super(manager, store);
		fProperty= new String[] { property };
		initialize();
	}

	/*
	 * @see AbstractJavaScanner#getTokenProperties()
	 */
	protected String[] getTokenProperties() {
		return fProperty;
	}

	/*
	 * @see AbstractJavaScanner#createRules()
	 */
	protected List createRules() {
		setDefaultReturnToken(getToken(fProperty[0]));
		return null;
	}
}

