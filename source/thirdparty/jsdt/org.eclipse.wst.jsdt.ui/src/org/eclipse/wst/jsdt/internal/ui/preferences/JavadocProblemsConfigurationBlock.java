/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
import org.eclipse.jface.dialogs.ControlEnableState;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.wst.jsdt.internal.ui.util.PixelConverter;
import org.eclipse.wst.jsdt.internal.ui.wizards.IStatusChangeListener;

/**
  */
public class JavadocProblemsConfigurationBlock extends OptionsConfigurationBlock {

	private static final Key PREF_JAVADOC_SUPPORT= getJDTCoreKey(JavaScriptCore.COMPILER_DOC_COMMENT_SUPPORT);

	private static final Key PREF_PB_INVALID_JAVADOC= getJDTCoreKey(JavaScriptCore.COMPILER_PB_INVALID_JAVADOC);
	private static final Key PREF_PB_INVALID_JAVADOC_TAGS= getJDTCoreKey(JavaScriptCore.COMPILER_PB_INVALID_JAVADOC_TAGS);
	private static final Key PREF_PB_INVALID_JAVADOC_TAGS_NOT_VISIBLE_REF= getJDTCoreKey(JavaScriptCore.COMPILER_PB_INVALID_JAVADOC_TAGS__NOT_VISIBLE_REF);
	private static final Key PREF_PB_INVALID_JAVADOC_TAGS_DEPRECATED_REF= getJDTCoreKey(JavaScriptCore.COMPILER_PB_INVALID_JAVADOC_TAGS__DEPRECATED_REF);
	private static final Key PREF_PB_INVALID_JAVADOC_TAGS_VISIBILITY= getJDTCoreKey(JavaScriptCore.COMPILER_PB_INVALID_JAVADOC_TAGS_VISIBILITY);

	private static final Key PREF_PB_MISSING_JAVADOC_TAGS= getJDTCoreKey(JavaScriptCore.COMPILER_PB_MISSING_JAVADOC_TAGS);
	private static final Key PREF_PB_MISSING_JAVADOC_TAGS_VISIBILITY= getJDTCoreKey(JavaScriptCore.COMPILER_PB_MISSING_JAVADOC_TAGS_VISIBILITY);
	private static final Key PREF_PB_MISSING_JAVADOC_TAGS_OVERRIDING= getJDTCoreKey(JavaScriptCore.COMPILER_PB_MISSING_JAVADOC_TAGS_OVERRIDING);

	private static final Key PREF_PB_MISSING_JAVADOC_COMMENTS= getJDTCoreKey(JavaScriptCore.COMPILER_PB_MISSING_JAVADOC_COMMENTS);
	private static final Key PREF_PB_MISSING_JAVADOC_COMMENTS_VISIBILITY= getJDTCoreKey(JavaScriptCore.COMPILER_PB_MISSING_JAVADOC_COMMENTS_VISIBILITY);
	private static final Key PREF_PB_MISSING_JAVADOC_COMMENTS_OVERRIDING= getJDTCoreKey(JavaScriptCore.COMPILER_PB_MISSING_JAVADOC_COMMENTS_OVERRIDING);
	

	// values
	private static final String ERROR= JavaScriptCore.ERROR;
	private static final String WARNING= JavaScriptCore.WARNING;
	private static final String IGNORE= JavaScriptCore.IGNORE;

	private static final String ENABLED= JavaScriptCore.ENABLED;
	private static final String DISABLED= JavaScriptCore.DISABLED;
	
	private static final String PUBLIC= JavaScriptCore.PUBLIC;
	private static final String PROTECTED= JavaScriptCore.PROTECTED;
	private static final String DEFAULT= JavaScriptCore.DEFAULT;
	private static final String PRIVATE= JavaScriptCore.PRIVATE;
	
	private PixelConverter fPixelConverter;
	private Composite fJavadocComposite;

	private ControlEnableState fBlockEnableState;


	public JavadocProblemsConfigurationBlock(IStatusChangeListener context, IProject project, IWorkbenchPreferenceContainer container) {
		super(context, project, getKeys(), container);
		fBlockEnableState= null;
	}
	
	private static Key[] getKeys() {
		Key[] keys= new Key[] {
				PREF_JAVADOC_SUPPORT,
				PREF_PB_INVALID_JAVADOC, PREF_PB_INVALID_JAVADOC_TAGS_VISIBILITY, PREF_PB_INVALID_JAVADOC_TAGS,
				PREF_PB_INVALID_JAVADOC_TAGS_VISIBILITY,
				PREF_PB_INVALID_JAVADOC_TAGS_NOT_VISIBLE_REF, PREF_PB_INVALID_JAVADOC_TAGS_DEPRECATED_REF,
				PREF_PB_MISSING_JAVADOC_TAGS, PREF_PB_MISSING_JAVADOC_TAGS_VISIBILITY, PREF_PB_MISSING_JAVADOC_TAGS_OVERRIDING,
				PREF_PB_MISSING_JAVADOC_COMMENTS, PREF_PB_MISSING_JAVADOC_COMMENTS_VISIBILITY, PREF_PB_MISSING_JAVADOC_COMMENTS_OVERRIDING,
			};
		return keys;
	}
	
	/*
	 * @see org.eclipse.jface.preference.PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {
		fPixelConverter= new PixelConverter(parent);
		setShell(parent.getShell());

		Composite javadocComposite= createJavadocTabContent(parent);
		
		validateSettings(null, null, null);
	
		return javadocComposite;
	}
	
	
	private Composite createJavadocTabContent(Composite folder) {
		String[] errorWarningIgnore= new String[] { ERROR, WARNING, IGNORE };
		
		String[] errorWarningIgnoreLabels= new String[] {
				PreferencesMessages.JavadocProblemsConfigurationBlock_error,  
				PreferencesMessages.JavadocProblemsConfigurationBlock_warning, 
				PreferencesMessages.JavadocProblemsConfigurationBlock_ignore
		};
		
		String[] enabledDisabled= new String[] { ENABLED, DISABLED };
		
		String[] visibilities= new String[] { PUBLIC, PROTECTED, DEFAULT, PRIVATE  };
		
		String[] visibilitiesLabels= new String[] {
				PreferencesMessages.JavadocProblemsConfigurationBlock_public, 
				PreferencesMessages.JavadocProblemsConfigurationBlock_protected, 
				PreferencesMessages.JavadocProblemsConfigurationBlock_default, 
				PreferencesMessages.JavadocProblemsConfigurationBlock_private
		};
		int nColumns= 3;
				

		final ScrolledPageContent sc1 = new ScrolledPageContent(folder);
		
		Composite outer= sc1.getBody();
		
		GridLayout layout = new GridLayout();
		layout.numColumns= nColumns;
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		outer.setLayout(layout);
		
		String label= PreferencesMessages.JavadocProblemsConfigurationBlock_pb_javadoc_support_label; 
		addCheckBox(outer, label, PREF_JAVADOC_SUPPORT, enabledDisabled, 0);
		
		layout = new GridLayout();
		layout.numColumns= nColumns;
		layout.marginHeight= 0;
		//layout.marginWidth= 0;
				
		Composite composite= new Composite(outer, SWT.NONE);
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, true));
		
		fJavadocComposite= composite;
		
		Label description= new Label(composite, SWT.WRAP);
		description.setText(PreferencesMessages.JavadocProblemsConfigurationBlock_javadoc_description); 
		GridData gd= new GridData();
		gd.horizontalSpan= nColumns;
		//gd.widthHint= fPixelConverter.convertWidthInCharsToPixels(60);
		description.setLayoutData(gd);
			
		int indent= fPixelConverter.convertWidthInCharsToPixels(2);
		
		label = PreferencesMessages.JavadocProblemsConfigurationBlock_pb_invalid_javadoc_label; 
		addComboBox(composite, label, PREF_PB_INVALID_JAVADOC, errorWarningIgnore, errorWarningIgnoreLabels, 0);
		
//		label = PreferencesMessages.JavadocProblemsConfigurationBlock_pb_invalid_javadoc_tags_visibility_label; 
//		addComboBox(composite, label, PREF_PB_INVALID_JAVADOC_TAGS_VISIBILITY, visibilities, visibilitiesLabels, indent);

		label= PreferencesMessages.JavadocProblemsConfigurationBlock_pb_invalid_javadoc_tags_label; 
		addCheckBox(composite, label, PREF_PB_INVALID_JAVADOC_TAGS, enabledDisabled, indent);
		
//		label= PreferencesMessages.JavadocProblemsConfigurationBlock_pb_invalid_javadoc_tags_not_visible_ref_label; 
//		addCheckBox(composite, label, PREF_PB_INVALID_JAVADOC_TAGS_NOT_VISIBLE_REF, enabledDisabled, indent);
		
//		label= PreferencesMessages.JavadocProblemsConfigurationBlock_pb_invalid_javadoc_tags_deprecated_label; 
//		addCheckBox(composite, label, PREF_PB_INVALID_JAVADOC_TAGS_DEPRECATED_REF, enabledDisabled, indent);

		gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan= nColumns;
		
		label = PreferencesMessages.JavadocProblemsConfigurationBlock_pb_missing_javadoc_label; 
		addComboBox(composite, label, PREF_PB_MISSING_JAVADOC_TAGS, errorWarningIgnore, errorWarningIgnoreLabels, 0);

//		label = PreferencesMessages.JavadocProblemsConfigurationBlock_pb_missing_javadoc_tags_visibility_label; 
//		addComboBox(composite, label, PREF_PB_MISSING_JAVADOC_TAGS_VISIBILITY, visibilities, visibilitiesLabels, indent);
		
//		label= PreferencesMessages.JavadocProblemsConfigurationBlock_pb_missing_javadoc_tags_overriding_label; 
//		addCheckBox(composite, label, PREF_PB_MISSING_JAVADOC_TAGS_OVERRIDING, enabledDisabled, indent);

		gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan= nColumns;
		
		label = PreferencesMessages.JavadocProblemsConfigurationBlock_pb_missing_comments_label; 
		addComboBox(composite, label, PREF_PB_MISSING_JAVADOC_COMMENTS, errorWarningIgnore, errorWarningIgnoreLabels, 0);

//		label = PreferencesMessages.JavadocProblemsConfigurationBlock_pb_missing_comments_visibility_label; 
//		addComboBox(composite, label, PREF_PB_MISSING_JAVADOC_COMMENTS_VISIBILITY, visibilities, visibilitiesLabels, indent);
		
//		label= PreferencesMessages.JavadocProblemsConfigurationBlock_pb_missing_comments_overriding_label; 
//		addCheckBox(composite, label, PREF_PB_MISSING_JAVADOC_COMMENTS_OVERRIDING, enabledDisabled, indent);

		return sc1;
	}
	
	/* (non-javadoc)
	 * Update fields and validate.
	 * @param changedKey Key that changed, or null, if all changed.
	 */	
	protected void validateSettings(Key changedKey, String oldValue, String newValue) {
		if (!areSettingsEnabled()) {
			return;
		}
		
		if (changedKey != null) {
			if (PREF_PB_INVALID_JAVADOC.equals(changedKey) ||
					PREF_PB_MISSING_JAVADOC_TAGS.equals(changedKey) ||
					PREF_PB_MISSING_JAVADOC_COMMENTS.equals(changedKey) ||
					PREF_JAVADOC_SUPPORT.equals(changedKey)) {				
				updateEnableStates();
			} else {
				return;
			}
		} else {
			updateEnableStates();
		}		
		fContext.statusChanged(new StatusInfo());
	}
	
	private void updateEnableStates() {		
		boolean enableJavadoc= checkValue(PREF_JAVADOC_SUPPORT, ENABLED);
		enableConfigControls(enableJavadoc);

		if (enableJavadoc) {
			boolean enableInvalidTagsErrors= !checkValue(PREF_PB_INVALID_JAVADOC, IGNORE);
			getCheckBox(PREF_PB_INVALID_JAVADOC_TAGS).setEnabled(enableInvalidTagsErrors);
			//getCheckBox(PREF_PB_INVALID_JAVADOC_TAGS_NOT_VISIBLE_REF).setEnabled(enableInvalidTagsErrors);
			//getCheckBox(PREF_PB_INVALID_JAVADOC_TAGS_DEPRECATED_REF).setEnabled(enableInvalidTagsErrors);
			//setComboEnabled(PREF_PB_INVALID_JAVADOC_TAGS_VISIBILITY, enableInvalidTagsErrors);
			
//			boolean enableMissingTagsErrors= !checkValue(PREF_PB_MISSING_JAVADOC_TAGS, IGNORE);
//			getCheckBox(PREF_PB_MISSING_JAVADOC_TAGS_OVERRIDING).setEnabled(enableMissingTagsErrors);
//			setComboEnabled(PREF_PB_MISSING_JAVADOC_TAGS_VISIBILITY, enableMissingTagsErrors);
//			
//			boolean enableMissingCommentsErrors= !checkValue(PREF_PB_MISSING_JAVADOC_COMMENTS, IGNORE);
//			getCheckBox(PREF_PB_MISSING_JAVADOC_COMMENTS_OVERRIDING).setEnabled(enableMissingCommentsErrors);
//			setComboEnabled(PREF_PB_MISSING_JAVADOC_COMMENTS_VISIBILITY, enableMissingCommentsErrors);
		}
	}
			
	protected void enableConfigControls(boolean enable) {
		if (enable) {
			if (fBlockEnableState != null) {
				fBlockEnableState.restore();
				fBlockEnableState= null;
			}
		} else {
			if (fBlockEnableState == null) {
				fBlockEnableState= ControlEnableState.disable(fJavadocComposite);
			}
		}	
	}
	
	
	protected String[] getFullBuildDialogStrings(boolean workspaceSettings) {
		String title= PreferencesMessages.JavadocProblemsConfigurationBlock_needsbuild_title; 
		String message;
		if (workspaceSettings) {
			message= PreferencesMessages.JavadocProblemsConfigurationBlock_needsfullbuild_message; 
		} else {
			message= PreferencesMessages.JavadocProblemsConfigurationBlock_needsprojectbuild_message; 
		}
		return new String[] { title, message };
	}
	
}
