/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Aaron Luchko, aluchko@redhat.com - 105926 [Formatter] Exporting Unnamed profile fails silently
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.preferences.cleanup;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wst.jsdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.wst.jsdt.internal.corext.fix.CleanUpPreferenceUtil;
import org.eclipse.wst.jsdt.internal.corext.fix.CleanUpRefactoring;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.fix.ICleanUp;
import org.eclipse.wst.jsdt.internal.ui.preferences.BulletListBlock;
import org.eclipse.wst.jsdt.internal.ui.preferences.PreferencesAccess;
import org.eclipse.wst.jsdt.internal.ui.preferences.formatter.IProfileVersioner;
import org.eclipse.wst.jsdt.internal.ui.preferences.formatter.ModifyDialog;
import org.eclipse.wst.jsdt.internal.ui.preferences.formatter.ProfileConfigurationBlock;
import org.eclipse.wst.jsdt.internal.ui.preferences.formatter.ProfileManager;
import org.eclipse.wst.jsdt.internal.ui.preferences.formatter.ProfileStore;
import org.eclipse.wst.jsdt.internal.ui.preferences.formatter.ProfileManager.CustomProfile;
import org.eclipse.wst.jsdt.internal.ui.preferences.formatter.ProfileManager.Profile;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;
import org.eclipse.wst.jsdt.ui.JavaScriptUI;


/**
 * The clean up configuration block for the clean up preference page. 
 */
public class CleanUpConfigurationBlock extends ProfileConfigurationBlock {
	
	private static final String CLEANUP_PAGE_SETTINGS_KEY= "cleanup_page"; //$NON-NLS-1$
	private static final String DIALOGSTORE_LASTSAVELOADPATH= JavaScriptUI.ID_PLUGIN + ".cleanup"; //$NON-NLS-1$

	private final IScopeContext fCurrContext;
	private SelectionButtonDialogField fShowCleanUpWizardDialogField;
	private CleanUpProfileManager fProfileManager;
	private ProfileStore fProfileStore;
	private BulletListBlock fBrowserBlock;
    
    public CleanUpConfigurationBlock(IProject project, PreferencesAccess access) {
	    super(project, access, DIALOGSTORE_LASTSAVELOADPATH);
	    
		if (project != null) {
			fCurrContext= null;
		} else {
			fCurrContext= access.getInstanceScope();
		}
    }

	protected IProfileVersioner createProfileVersioner() {
	    return new CleanUpProfileVersioner();
    }
	
	protected ProfileStore createProfileStore(IProfileVersioner versioner) {
	    fProfileStore= new ProfileStore(CleanUpConstants.CLEANUP_PROFILES, versioner);
		return fProfileStore;
    }
	
	protected ProfileManager createProfileManager(List profiles, IScopeContext context, PreferencesAccess access, IProfileVersioner profileVersioner) {
		profiles.addAll(CleanUpPreferenceUtil.getBuiltInProfiles());
	    fProfileManager= new CleanUpProfileManager(profiles, context, access, profileVersioner);
		return fProfileManager;
    }
	
	/**
     * {@inheritDoc}
     */
    protected void configurePreview(Composite composite, int numColumns, final ProfileManager profileManager) {
    	Map settings= profileManager.getSelected().getSettings();
		final Map sharedSettings= new Hashtable();
		fill(settings, sharedSettings);
		
		final ICleanUp[] cleanUps= CleanUpRefactoring.createCleanUps(sharedSettings);
		
		createLabel(composite, CleanUpMessages.CleanUpConfigurationBlock_SelectedCleanUps_label, numColumns);
		
		fBrowserBlock= new BulletListBlock();
		Control control= fBrowserBlock.createControl(composite);
		((GridData)control.getLayoutData()).horizontalSpan= numColumns;
		fBrowserBlock.setText(getSelectedCleanUpsInfo(cleanUps));
		
		profileManager.addObserver(new Observer() {

			public void update(Observable o, Object arg) {
				final int value= ((Integer)arg).intValue();
				switch (value) {
				case ProfileManager.PROFILE_CREATED_EVENT:
				case ProfileManager.PROFILE_DELETED_EVENT:
				case ProfileManager.SELECTION_CHANGED_EVENT:
				case ProfileManager.SETTINGS_CHANGED_EVENT:
					fill(profileManager.getSelected().getSettings(), sharedSettings);
					fBrowserBlock.setText(getSelectedCleanUpsInfo(cleanUps));
				}
            }
			
		});
    }

    private String getSelectedCleanUpsInfo(ICleanUp[] cleanUps) {
    	if (cleanUps.length == 0)
    		return ""; //$NON-NLS-1$
    	
    	StringBuffer buf= new StringBuffer();
    	
    	boolean first= true;
    	for (int i= 0; i < cleanUps.length; i++) {
	        String[] descriptions= cleanUps[i].getDescriptions();
	        if (descriptions != null) {
    	        for (int j= 0; j < descriptions.length; j++) {
    	        	if (first) {
    	        		first= false;
    	        	} else {
    	        		buf.append('\n');
    	        	}
    	            buf.append(descriptions[j]);
                }
	        }
        }
    	
    	return buf.toString();
    }

	private void fill(Map settings, Map sharedSettings) {
		sharedSettings.clear();
		for (Iterator iterator= settings.keySet().iterator(); iterator.hasNext();) {
	        String key= (String)iterator.next();
	        sharedSettings.put(key, settings.get(key));
        }
    }

	protected ModifyDialog createModifyDialog(Shell shell, Profile profile, ProfileManager profileManager, ProfileStore profileStore, boolean newProfile) {
        return new CleanUpModifyDialog(shell, profile, profileManager, profileStore, newProfile, CLEANUP_PAGE_SETTINGS_KEY, DIALOGSTORE_LASTSAVELOADPATH);
    }
	
	/**
	 * {@inheritDoc}
	 */
	public Composite createContents(Composite parent) {
	    Composite composite= super.createContents(parent);
	    
	    if (fCurrContext == null)
	    	return composite;
	    
	    fShowCleanUpWizardDialogField= new SelectionButtonDialogField(SWT.CHECK);
		fShowCleanUpWizardDialogField.setLabelText(CleanUpMessages.CleanUpConfigurationBlock_ShowCleanUpWizard_checkBoxLabel);
	    fShowCleanUpWizardDialogField.doFillIntoGrid(composite, 5);
	    
	    IEclipsePreferences node= fCurrContext.getNode(JavaScriptUI.ID_PLUGIN);
		boolean showWizard;
		if (node.get(CleanUpConstants.SHOW_CLEAN_UP_WIZARD, null) != null) {
			showWizard= node.getBoolean(CleanUpConstants.SHOW_CLEAN_UP_WIZARD, true);
		} else {
			showWizard= new DefaultScope().getNode(JavaScriptUI.ID_PLUGIN).getBoolean(CleanUpConstants.SHOW_CLEAN_UP_WIZARD, true);
		}
		if (showWizard)
			fShowCleanUpWizardDialogField.setSelection(true);
		
	    fShowCleanUpWizardDialogField.setDialogFieldListener(new IDialogFieldListener() {
			public void dialogFieldChanged(DialogField field) {
				doShowCleanUpWizard(fShowCleanUpWizardDialogField.isSelected());
            }
	    });
	        
		return composite;
	}

	private void doShowCleanUpWizard(boolean showWizard) {
		IEclipsePreferences preferences= fCurrContext.getNode(JavaScriptUI.ID_PLUGIN);
		if (preferences.get(CleanUpConstants.SHOW_CLEAN_UP_WIZARD, null) != null &&
				preferences.getBoolean(CleanUpConstants.SHOW_CLEAN_UP_WIZARD, true) == showWizard)
			return;
		
		preferences.putBoolean(CleanUpConstants.SHOW_CLEAN_UP_WIZARD, showWizard);
    }
	
	/**
	 * {@inheritDoc}
	 */
	public void performDefaults() {
		super.performDefaults();
		if (fCurrContext == null)
			return;
		
		fCurrContext.getNode(JavaScriptUI.ID_PLUGIN).remove(CleanUpConstants.SHOW_CLEAN_UP_WIZARD);
		boolean showWizard= new DefaultScope().getNode(JavaScriptUI.ID_PLUGIN).getBoolean(CleanUpConstants.SHOW_CLEAN_UP_WIZARD, true);
		fShowCleanUpWizardDialogField.setDialogFieldListener(null);
		fShowCleanUpWizardDialogField.setSelection(showWizard);
		fShowCleanUpWizardDialogField.setDialogFieldListener(new IDialogFieldListener() {
			public void dialogFieldChanged(DialogField field) {
				doShowCleanUpWizard(fShowCleanUpWizardDialogField.isSelected());
            }
	    });
	}
	
	/**
	 * {@inheritDoc}
	 */
	protected void preferenceChanged(PreferenceChangeEvent event) {
		if (CleanUpConstants.CLEANUP_PROFILES.equals(event.getKey())) {
			try {
				String id= fCurrContext.getNode(JavaScriptUI.ID_PLUGIN).get(CleanUpConstants.CLEANUP_PROFILE, null);
				if (id == null)
					fProfileManager.getDefaultProfile().getID();
				
				List oldProfiles= fProfileManager.getSortedProfiles();
				Profile[] oldProfilesArray= (Profile[])oldProfiles.toArray(new Profile[oldProfiles.size()]);
				for (int i= 0; i < oldProfilesArray.length; i++) {
					if (oldProfilesArray[i] instanceof CustomProfile) {
						fProfileManager.deleteProfile((CustomProfile)oldProfilesArray[i]);
					}
				}

				List newProfiles= fProfileStore.readProfilesFromString((String)event.getNewValue());
				for (Iterator iterator= newProfiles.iterator(); iterator.hasNext();) {
					CustomProfile profile= (CustomProfile)iterator.next();
					fProfileManager.addProfile(profile);
				}

				Profile profile= fProfileManager.getProfile(id);
				if (profile != null) {
					fProfileManager.setSelected(profile);
				} else {
					fProfileManager.setSelected(fProfileManager.getDefaultProfile());
				}
			} catch (CoreException e) {
				JavaScriptPlugin.log(e);
			}
		} else if (CleanUpConstants.CLEANUP_PROFILE.equals(event.getKey())) {
			if (event.getNewValue() == null) {
				fProfileManager.setSelected(fProfileManager.getDefaultProfile());
			} else {
				Profile profile= fProfileManager.getProfile((String)event.getNewValue());
				if (profile != null) {
					fProfileManager.setSelected(profile);
				}
			}
		}
	}
	
	public void enableProjectSpecificSettings(boolean useProjectSpecificSettings) {
		fBrowserBlock.setEnabled(useProjectSpecificSettings);
	}
}
