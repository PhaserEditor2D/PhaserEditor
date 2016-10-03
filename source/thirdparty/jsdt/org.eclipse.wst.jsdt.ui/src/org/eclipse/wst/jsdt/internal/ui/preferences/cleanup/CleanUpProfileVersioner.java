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

import org.eclipse.wst.jsdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.wst.jsdt.internal.ui.preferences.formatter.IProfileVersioner;
import org.eclipse.wst.jsdt.internal.ui.preferences.formatter.ProfileManager.CustomProfile;


public class CleanUpProfileVersioner implements IProfileVersioner {
	
	public static final String PROFILE_KIND= "CleanUpProfile"; //$NON-NLS-1$

	private static final int VERSION_1= 1; // 3.3M2
	private static final int VERSION_2= 2; // 3.3M3 Added ORGANIZE_IMPORTS
	
	public static final int CURRENT_VERSION= VERSION_2;
	
	/* (non-Javadoc)
     * @see org.eclipse.wst.jsdt.internal.ui.preferences.cleanup.IProfileVersioner#getFirstVersion()
     */
	public int getFirstVersion() {
	    return VERSION_1;
    }

	/* (non-Javadoc)
     * @see org.eclipse.wst.jsdt.internal.ui.preferences.cleanup.IProfileVersioner#getCurrentVersion()
     */
	public int getCurrentVersion() {
	    return CURRENT_VERSION;
    }

	/* (non-Javadoc)
     * @see org.eclipse.wst.jsdt.internal.ui.preferences.cleanup.IProfileVersioner#updateAndComplete(org.eclipse.wst.jsdt.internal.ui.preferences.cleanup.ProfileManager.CustomProfile)
     */
	public void update(CustomProfile profile) {
		if (profile.getVersion() == VERSION_1)
			updateFrom1To2(profile);
		
		profile.setVersion(CURRENT_VERSION);
	}

	/**
     * {@inheritDoc}
     */
    public String getProfileKind() {
	    return PROFILE_KIND;
    }
    
	private static void updateFrom1To2(CustomProfile profile) {
		Map defaultSettings= CleanUpConstants.getEclipseDefaultSettings();
		profile.getSettings().put(CleanUpConstants.ORGANIZE_IMPORTS, defaultSettings.get(CleanUpConstants.ORGANIZE_IMPORTS));
    }
	
 }
