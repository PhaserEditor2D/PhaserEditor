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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.preference.IPreferencePageContainer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
import org.eclipse.ui.preferences.IWorkingCopyManager;
import org.eclipse.ui.preferences.WorkingCopyManager;
import org.eclipse.wst.jsdt.internal.ui.preferences.formatter.ProfileConfigurationBlock;

public abstract class ProfilePreferencePage extends PropertyAndPreferencePage {

	private ProfileConfigurationBlock fConfigurationBlock;

	public ProfilePreferencePage() {
		super();
	}

	protected abstract ProfileConfigurationBlock createConfigurationBlock(PreferencesAccess access);

	public void createControl(Composite parent) {
    	IPreferencePageContainer container= getContainer();
    	IWorkingCopyManager workingCopyManager;
    	if (container instanceof IWorkbenchPreferenceContainer) {
    		workingCopyManager= ((IWorkbenchPreferenceContainer) container).getWorkingCopyManager();
    	} else {
    		workingCopyManager= new WorkingCopyManager(); // non shared 
    	}
    	PreferencesAccess access= PreferencesAccess.getWorkingCopyPreferences(workingCopyManager);
    	fConfigurationBlock= createConfigurationBlock(access);
    	
    	super.createControl(parent);
    }

	protected Control createPreferenceContent(Composite composite) {
    	return fConfigurationBlock.createContents(composite);
    }

	protected boolean hasProjectSpecificOptions(IProject project) {
    	return fConfigurationBlock.hasProjectSpecificOptions(project);
    }

	protected void enableProjectSpecificSettings(boolean useProjectSpecificSettings) {
    	super.enableProjectSpecificSettings(useProjectSpecificSettings);
    	if (fConfigurationBlock != null) {
    		fConfigurationBlock.enableProjectSpecificSettings(useProjectSpecificSettings);
    	}
    }

	public void dispose() {
    	if (fConfigurationBlock != null) {
    		fConfigurationBlock.dispose();
    	}
    	super.dispose();
    }

	protected void performDefaults() {
    	if (fConfigurationBlock != null) {
    		fConfigurationBlock.performDefaults();
    	}
    	super.performDefaults();
    }

	public boolean performOk() {
    	if (fConfigurationBlock != null && !fConfigurationBlock.performOk()) {
    		return false;
    	}	
    	return super.performOk();
    }

	public void performApply() {
    	if (fConfigurationBlock != null) {
    		fConfigurationBlock.performApply();
    	}	
    	super.performApply();
    }

	public void setElement(IAdaptable element) {
    	super.setElement(element);
    	setDescription(null); // no description for property page
    }

}
