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

import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.text.templates.persistence.TemplatePersistenceData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.dialogs.StatusUtil;

/*
 * The page to configure the code templates.
 */
public class CodeTemplatePreferencePage extends PropertyAndPreferencePage {

	public static final String PREF_ID= "org.eclipse.wst.jsdt.ui.preferences.CodeTemplatePreferencePage"; //$NON-NLS-1$
	public static final String PROP_ID= "org.eclipse.wst.jsdt.ui.propertyPages.CodeTemplatePreferencePage"; //$NON-NLS-1$
	
	public static final String DATA_SELECT_TEMPLATE= "CodeTemplatePreferencePage.select_template"; //$NON-NLS-1$
	
	private CodeTemplateBlock fCodeTemplateConfigurationBlock;

	public CodeTemplatePreferencePage() {
		setPreferenceStore(JavaScriptPlugin.getDefault().getPreferenceStore());
		//setDescription(PreferencesMessages.getString("CodeTemplatesPreferencePage.description")); //$NON-NLS-1$
		
		// only used when page is shown programatically
		setTitle(PreferencesMessages.CodeTemplatesPreferencePage_title);		 
	}

	/*
	 * @see PreferencePage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		fCodeTemplateConfigurationBlock= new CodeTemplateBlock(getProject());
		
		super.createControl(parent);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IJavaHelpContextIds.CODE_TEMPLATES_PREFERENCE_PAGE);
	}	

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.ui.preferences.PropertyAndPreferencePage#createPreferenceContent(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createPreferenceContent(Composite composite) {
		return fCodeTemplateConfigurationBlock.createContents(composite);
	}
	
	/*
	 * @see IPreferencePage#performOk()
	 */
	public boolean performOk() {
		if (fCodeTemplateConfigurationBlock != null) {
			return fCodeTemplateConfigurationBlock.performOk(useProjectSettings());
		}
		return true;
	}
	
	/*
	 * @see PreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		super.performDefaults();
		if (fCodeTemplateConfigurationBlock != null) {
			fCodeTemplateConfigurationBlock.performDefaults();
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.ui.wizards.IStatusChangeListener#statusChanged(org.eclipse.core.runtime.IStatus)
	 */
	public void statusChanged(IStatus status) {
		setValid(!status.matches(IStatus.ERROR));
		StatusUtil.applyToStatusLine(this, status);		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.IPreferencePage#performCancel()
	 */
	public boolean performCancel() {
		if (fCodeTemplateConfigurationBlock != null) {
			fCodeTemplateConfigurationBlock.performCancel();
		}
		return super.performCancel();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.ui.preferences.PropertyAndPreferencePage#hasProjectSpecificOptions(org.eclipse.core.resources.IProject)
	 */
	protected boolean hasProjectSpecificOptions(IProject project) {
		return fCodeTemplateConfigurationBlock.hasProjectSpecificOptions(project);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.ui.preferences.PropertyAndPreferencePage#getPreferencePageID()
	 */
	protected String getPreferencePageID() {
		return PREF_ID;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.ui.preferences.PropertyAndPreferencePage#getPropertyPageID()
	 */
	protected String getPropertyPageID() {
		return PROP_ID;
	}

	/*
	 * @see org.eclipse.jface.preference.PreferencePage#applyData(java.lang.Object)
	 */
	public void applyData(Object data) {
		if (data instanceof Map) {
			Object id= ((Map) data).get(DATA_SELECT_TEMPLATE);
			if (id instanceof String) {
				final TemplatePersistenceData[] templates= fCodeTemplateConfigurationBlock.fTemplateStore.getTemplateData();
				TemplatePersistenceData template= null;
				for (int index= 0; index < templates.length; index++) {
					template= templates[index];
					if (template.getId().equals(id)) {
						fCodeTemplateConfigurationBlock.postSetSelection(template);
						break;
					}
				}
			}
		}
		super.applyData(data);
	}
}
