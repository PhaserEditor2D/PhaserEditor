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
package org.eclipse.wst.jsdt.internal.ui.preferences.cleanup;

import java.util.Map;

import org.eclipse.swt.widgets.Shell;
import org.eclipse.wst.jsdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.wst.jsdt.internal.ui.preferences.formatter.ModifyDialog;
import org.eclipse.wst.jsdt.internal.ui.preferences.formatter.ProfileManager;
import org.eclipse.wst.jsdt.internal.ui.preferences.formatter.ProfileStore;
import org.eclipse.wst.jsdt.internal.ui.preferences.formatter.ProfileManager.Profile;

public class CleanUpModifyDialog extends ModifyDialog {
	
	/**
	 * Constant array for boolean selection 
	 */
	static String[] FALSE_TRUE = {
		CleanUpConstants.FALSE,
		CleanUpConstants.TRUE
	};

	public CleanUpModifyDialog(Shell parentShell, Profile profile, ProfileManager profileManager, ProfileStore profileStore, boolean newProfile, String dialogPreferencesKey, String lastSavePathKey) {
	    super(parentShell, profile, profileManager, profileStore, newProfile, dialogPreferencesKey, lastSavePathKey);
    }

	/**
	 * {@inheritDoc}
	 */
	protected void addPages(final Map values) {
		addTabPage(CleanUpMessages.CleanUpModifyDialog_TabPageName_CodeStyle, new CodeStyleTabPage(this, values));
//		addTabPage(CleanUpMessages.CleanUpModifyDialog_TabPageName_MemberAccesses, new MemberAccessesTabPage(this, values));
//		addTabPage(CleanUpMessages.CleanUpModifyDialog_TabPageName_UnnecessaryCode, new UnnecessaryCodeTabPage(this, values));
//		addTabPage(CleanUpMessages.CleanUpModifyDialog_TabPageName_MissingCode, new MissingCodeTabPage(this, values));
		addTabPage(CleanUpMessages.CleanUpModifyDialog_TabPageName_CodeFormating, new CodeFormatingTabPage(this, values));
	}
}
