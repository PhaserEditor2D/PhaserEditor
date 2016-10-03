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

package org.eclipse.wst.jsdt.internal.ui.preferences;

import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;



/**
 * Occurrences preference page.
 * <p>
 * Note: Must be public since it is referenced from plugin.xml
 * </p>
 * 
 * 
 */
public class OccurrencesPreferencePage extends AbstractConfigurationBlockPreferencePage {

	/*
	 * @see org.eclipse.wst.jsdt.internal.ui.preferences.AbstractConfigureationBlockPreferencePage#getHelpId()
	 */
	protected String getHelpId() {
		return IJavaHelpContextIds.JAVA_EDITOR_PREFERENCE_PAGE;
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.ui.preferences.AbstractConfigurationBlockPreferencePage#setDescription()
	 */
	protected void setDescription() {
		// This page has no description
	}
	
	/*
	 * @see org.eclipse.wst.jsdt.internal.ui.preferences.AbstractConfigurationBlockPreferencePage#setPreferenceStore()
	 */
	protected void setPreferenceStore() {
		setPreferenceStore(JavaScriptPlugin.getDefault().getPreferenceStore());
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.ui.preferences.AbstractConfigureationBlockPreferencePage#createConfigurationBlock(org.eclipse.wst.jsdt.internal.ui.preferences.OverlayPreferenceStore)
	 */
	protected IPreferenceConfigurationBlock createConfigurationBlock(OverlayPreferenceStore overlayPreferenceStore) {
		return new MarkOccurrencesConfigurationBlock(overlayPreferenceStore);
	}
}
