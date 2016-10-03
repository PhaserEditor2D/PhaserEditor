/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.preferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.ControlEnableState;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.wst.jsdt.internal.ui.wizards.IStatusChangeListener;
import org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths.BuildPathSupport;
import org.eclipse.wst.jsdt.launching.IVMInstall;
import org.eclipse.wst.jsdt.launching.IVMInstall2;
import org.eclipse.wst.jsdt.launching.JavaRuntime;

/**
  */
public class ComplianceConfigurationBlock extends OptionsConfigurationBlock {

	// Preference store keys, see JavaScriptCore.getOptions
	private static final Key PREF_LOCAL_VARIABLE_ATTR=  getJDTCoreKey(JavaScriptCore.COMPILER_LOCAL_VARIABLE_ATTR);
	private static final Key PREF_LINE_NUMBER_ATTR= getJDTCoreKey(JavaScriptCore.COMPILER_LINE_NUMBER_ATTR);
	private static final Key PREF_SOURCE_FILE_ATTR= getJDTCoreKey(JavaScriptCore.COMPILER_SOURCE_FILE_ATTR);
	private static final Key PREF_CODEGEN_UNUSED_LOCAL= getJDTCoreKey(JavaScriptCore.COMPILER_CODEGEN_UNUSED_LOCAL);
	private static final Key PREF_CODEGEN_TARGET_PLATFORM= getJDTCoreKey(JavaScriptCore.COMPILER_CODEGEN_TARGET_PLATFORM);
	private static final Key PREF_CODEGEN_INLINE_JSR_BYTECODE= getJDTCoreKey(JavaScriptCore.COMPILER_CODEGEN_INLINE_JSR_BYTECODE);
	
	private static final Key PREF_SOURCE_COMPATIBILITY= getJDTCoreKey(JavaScriptCore.COMPILER_SOURCE);
	private static final Key PREF_COMPLIANCE= getJDTCoreKey(JavaScriptCore.COMPILER_COMPLIANCE);
	private static final Key PREF_PB_ASSERT_AS_IDENTIFIER= getJDTCoreKey(JavaScriptCore.COMPILER_PB_ASSERT_IDENTIFIER);
	
	private static final Key INTR_DEFAULT_COMPLIANCE= getJDTUIKey("internal.default.compliance"); //$NON-NLS-1$

	// values
//	private static final String GENERATE= JavaScriptCore.GENERATE;
//	private static final String DO_NOT_GENERATE= JavaScriptCore.DO_NOT_GENERATE;
//	
//	private static final String PRESERVE= JavaScriptCore.PRESERVE;
//	private static final String OPTIMIZE_OUT= JavaScriptCore.OPTIMIZE_OUT;
	
	private static final String VERSION_0_0= JavaScriptCore.VERSION_0_0;
	private static final String VERSION_1_1= JavaScriptCore.VERSION_1_1;
	private static final String VERSION_1_2= JavaScriptCore.VERSION_1_2;
	private static final String VERSION_1_3= JavaScriptCore.VERSION_1_3;
	private static final String VERSION_1_4= JavaScriptCore.VERSION_1_4;
	private static final String VERSION_1_5= JavaScriptCore.VERSION_1_5;
	private static final String VERSION_1_6= JavaScriptCore.VERSION_1_6;
	
	private static final String ERROR= JavaScriptCore.ERROR;
	private static final String WARNING= JavaScriptCore.WARNING;
	private static final String IGNORE= JavaScriptCore.IGNORE;

	private static final String ENABLED= JavaScriptCore.ENABLED;
//	private static final String DISABLED= JavaScriptCore.DISABLED;
	
	
	private static final String DEFAULT_CONF= "default"; //$NON-NLS-1$
	private static final String USER_CONF= "user";	 //$NON-NLS-1$

	private ArrayList fComplianceControls;
//	private PixelConverter fPixelConverter;

	private String[] fRememberedUserCompliance;
	
	private static final int IDX_ASSERT_AS_IDENTIFIER= 0;
	private static final int IDX_ENUM_AS_IDENTIFIER= 1;
	private static final int IDX_SOURCE_COMPATIBILITY= 2;
	private static final int IDX_CODEGEN_TARGET_PLATFORM= 3;
	private static final int IDX_COMPLIANCE= 4;
	private static final int IDX_INLINE_JSR_BYTECODE= 5;

	private IStatus fComplianceStatus;

	private Link fJRE50InfoText;
	private Composite fControlsComposite;
	private ControlEnableState fBlockEnableState;

	public ComplianceConfigurationBlock(IStatusChangeListener context, IProject project, IWorkbenchPreferenceContainer container) {
		super(context, project, getKeys(), container);

		fBlockEnableState= null;
		fComplianceControls= new ArrayList();
			
		fComplianceStatus= new StatusInfo();
		
		fRememberedUserCompliance= new String[] { // caution: order depends on IDX_* constants
			getValue(PREF_PB_ASSERT_AS_IDENTIFIER),
			getValue(PREF_SOURCE_COMPATIBILITY),
			getValue(PREF_CODEGEN_TARGET_PLATFORM),
			getValue(PREF_COMPLIANCE),
			getValue(PREF_CODEGEN_INLINE_JSR_BYTECODE),
		};
	}
	
	private static Key[] getKeys() {
		return new Key[] {
				PREF_LOCAL_VARIABLE_ATTR, PREF_LINE_NUMBER_ATTR, PREF_SOURCE_FILE_ATTR, PREF_CODEGEN_UNUSED_LOCAL,
				PREF_CODEGEN_INLINE_JSR_BYTECODE,
				PREF_COMPLIANCE, PREF_SOURCE_COMPATIBILITY,
				PREF_CODEGEN_TARGET_PLATFORM, PREF_PB_ASSERT_AS_IDENTIFIER
			};
	}
		
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.ui.preferences.OptionsConfigurationBlock#settingsUpdated()
	 */
	protected void settingsUpdated() {
		setValue(INTR_DEFAULT_COMPLIANCE, getCurrentCompliance());
		super.settingsUpdated();
	}
	
	
	/*
	 * @see org.eclipse.jface.preference.PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {
//		fPixelConverter= new PixelConverter(parent);
		setShell(parent.getShell());
		
		Composite complianceComposite= createComplianceTabContent(parent);
		
		validateSettings(null, null, null);
	
		return complianceComposite;
	}
	
	public void enablePreferenceContent(boolean enable) {
		if (fControlsComposite != null && !fControlsComposite.isDisposed()) {
			if (enable) {
				if (fBlockEnableState != null) {
					fBlockEnableState.restore();
					fBlockEnableState= null;
				}
			} else {
				if (fBlockEnableState == null) {
					fBlockEnableState= ControlEnableState.disable(fControlsComposite);
				}
			}
		}
	}
	
	private Composite createComplianceTabContent(Composite folder) {


		String[] values3456= new String[] {
					VERSION_0_0, VERSION_1_4
//					, VERSION_1_5 // <<-- this was uncommented before adding external build/validation
					};
		String[] values3456Labels= new String[] {
			PreferencesMessages.ComplianceConfigurationBlock_version00,
//			PreferencesMessages.ComplianceConfigurationBlock_version13,  
			PreferencesMessages.ComplianceConfigurationBlock_version14, 
//			PreferencesMessages.ComplianceConfigurationBlock_version15 // <<-- this was uncommented before adding external build/validation
//			PreferencesMessages.ComplianceConfigurationBlock_version16
		};

		final ScrolledPageContent sc1 = new ScrolledPageContent(folder);
		Composite composite= sc1.getBody();
		GridLayout layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		composite.setLayout(layout);
		
		fControlsComposite= new Composite(composite, SWT.NONE);
		fControlsComposite.setFont(composite.getFont());
		fControlsComposite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
		
		layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		layout.numColumns= 1;
		fControlsComposite.setLayout(layout);

		int nColumns= 3;

		layout= new GridLayout();
		layout.numColumns= nColumns;

		Group group= new Group(fControlsComposite, SWT.NONE);
		group.setFont(fControlsComposite.getFont());
		group.setText(PreferencesMessages.ComplianceConfigurationBlock_compliance_group_label); 
		group.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
		group.setLayout(layout);
	
		String label= PreferencesMessages.ComplianceConfigurationBlock_compiler_compliance_label; 
		Combo combo = addComboBox(group, label, PREF_COMPLIANCE, values3456, values3456Labels, 1);
//		combo.setEnabled(false);

//		label= PreferencesMessages.ComplianceConfigurationBlock_default_settings_label; 
//		addCheckBox(group, label, INTR_DEFAULT_COMPLIANCE, new String[] { DEFAULT_CONF, USER_CONF }, 0);	
//
//		int indent= fPixelConverter.convertWidthInCharsToPixels(2);
//		Control[] otherChildren= group.getChildren();	
//				
//		String[] versions= new String[] { VERSION_1_1, VERSION_1_2, VERSION_1_3, VERSION_1_4, VERSION_1_5, VERSION_1_6 };
//		String[] versionsLabels= new String[] {
//			PreferencesMessages.ComplianceConfigurationBlock_version11,  
//			PreferencesMessages.ComplianceConfigurationBlock_version12, 
//			PreferencesMessages.ComplianceConfigurationBlock_version13, 
//			PreferencesMessages.ComplianceConfigurationBlock_version14,
//			PreferencesMessages.ComplianceConfigurationBlock_version15, 
//			PreferencesMessages.ComplianceConfigurationBlock_version16
//		};
//		
//		label= PreferencesMessages.ComplianceConfigurationBlock_codegen_targetplatform_label; 
//		addComboBox(group, label, PREF_CODEGEN_TARGET_PLATFORM, versions, versionsLabels, indent);	
//
//		label= PreferencesMessages.ComplianceConfigurationBlock_source_compatibility_label; 
//		addComboBox(group, label, PREF_SOURCE_COMPATIBILITY, values3456, values3456Labels, indent);	
//
//		String[] errorWarningIgnore= new String[] { ERROR, WARNING, IGNORE };
//		
//		String[] errorWarningIgnoreLabels= new String[] {
//			PreferencesMessages.ComplianceConfigurationBlock_error,  
//			PreferencesMessages.ComplianceConfigurationBlock_warning, 
//			PreferencesMessages.ComplianceConfigurationBlock_ignore
//		};
//
//		label= PreferencesMessages.ComplianceConfigurationBlock_pb_assert_as_identifier_label; 
//		addComboBox(group, label, PREF_PB_ASSERT_AS_IDENTIFIER, errorWarningIgnore, errorWarningIgnoreLabels, indent);		
//
//		label= PreferencesMessages.ComplianceConfigurationBlock_pb_enum_as_identifier_label; 
//		addComboBox(group, label, PREF_PB_ENUM_AS_IDENTIFIER, errorWarningIgnore, errorWarningIgnoreLabels, indent);		
//
//		
//		Control[] allChildren= group.getChildren();
//		fComplianceControls.addAll(Arrays.asList(allChildren));
//		fComplianceControls.removeAll(Arrays.asList(otherChildren));
//
//		layout= new GridLayout();
//		layout.numColumns= nColumns;
//
//		group= new Group(fControlsComposite, SWT.NONE);
//		group.setFont(fControlsComposite.getFont());
//		group.setText(PreferencesMessages.ComplianceConfigurationBlock_classfiles_group_label); 
//		group.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
//		group.setLayout(layout);
//
//		String[] generateValues= new String[] { GENERATE, DO_NOT_GENERATE };
//		String[] enableDisableValues= new String[] { ENABLED, DISABLED };
//
//		label= PreferencesMessages.ComplianceConfigurationBlock_variable_attr_label; 
//		addCheckBox(group, label, PREF_LOCAL_VARIABLE_ATTR, generateValues, 0);
//		
//		label= PreferencesMessages.ComplianceConfigurationBlock_line_number_attr_label; 
//		addCheckBox(group, label, PREF_LINE_NUMBER_ATTR, generateValues, 0);		
//
//		label= PreferencesMessages.ComplianceConfigurationBlock_source_file_attr_label; 
//		addCheckBox(group, label, PREF_SOURCE_FILE_ATTR, generateValues, 0);		
//
//		label= PreferencesMessages.ComplianceConfigurationBlock_codegen_unused_local_label; 
//		addCheckBox(group, label, PREF_CODEGEN_UNUSED_LOCAL, new String[] { PRESERVE, OPTIMIZE_OUT }, 0);	
//
//		label= PreferencesMessages.ComplianceConfigurationBlock_codegen_inline_jsr_bytecode_label; 
//		addCheckBox(group, label, PREF_CODEGEN_INLINE_JSR_BYTECODE, enableDisableValues, 0);	
//		
//		fJRE50InfoText= new Link(composite, SWT.WRAP);
//		fJRE50InfoText.setFont(composite.getFont());
//		// set a text: not the real one, just for layouting
//		fJRE50InfoText.setText(Messages.format(PreferencesMessages.ComplianceConfigurationBlock_jrecompliance_info_project, new String[] { VERSION_1_3, VERSION_1_3 }));
//		fJRE50InfoText.setVisible(false);
//		fJRE50InfoText.addSelectionListener(new SelectionListener() {
//			public void widgetDefaultSelected(SelectionEvent e) {
//				if ("1".equals(e.text)) { //$NON-NLS-1$
//					openJREInstallPreferencePage();
//				} else {
//					openBuildPathPropertyPage();
//				}
//			}
//			public void widgetSelected(SelectionEvent e) {
//				widgetDefaultSelected(e);
//			}
//		});
//		GridData gd= new GridData(GridData.FILL, GridData.FILL, true, true);
//		gd.widthHint= fPixelConverter.convertWidthInCharsToPixels(50);
//		fJRE50InfoText.setLayoutData(gd);
//		validateJRE50Status();
		
		return sc1;
	}
	
	protected final void openBuildPathPropertyPage() {
		if (getPreferenceContainer() != null) {
			Map data= new HashMap();
			data.put(BuildPathsPropertyPage.DATA_REVEAL_ENTRY, JavaRuntime.getDefaultJREContainerEntry());
			getPreferenceContainer().openPage(BuildPathsPropertyPage.PROP_ID, data);
		}
		validateJRE50Status();
	}
	
	protected final void openJREInstallPreferencePage() {
		String jreID= BuildPathSupport.JRE_PREF_PAGE_ID;
		if (fProject == null && getPreferenceContainer() != null) {
			getPreferenceContainer().openPage(jreID, null);
		} else {
			PreferencesUtil.createPreferenceDialogOn(getShell(), jreID, new String[] { jreID }, null).open();
		}
		validateJRE50Status();
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
			if (INTR_DEFAULT_COMPLIANCE.equals(changedKey)) {
				updateComplianceEnableState();
				updateComplianceDefaultSettings(true, null);
				fComplianceStatus= validateCompliance();
			} else if (PREF_COMPLIANCE.equals(changedKey)) {
			    // set compliance settings to default
//			    Object oldDefault= setValue(INTR_DEFAULT_COMPLIANCE, DEFAULT_CONF);
			    updateComplianceEnableState();
//				updateComplianceDefaultSettings(USER_CONF.equals(oldDefault), oldValue);
				fComplianceStatus= validateCompliance();
				validateJRE50Status();
			} else if (PREF_SOURCE_COMPATIBILITY.equals(changedKey)) {
				updateAssertEnumAsIdentifierEnableState();
				fComplianceStatus= validateCompliance();
			} else if (PREF_CODEGEN_TARGET_PLATFORM.equals(changedKey)) {
				updateInlineJSREnableState();
				fComplianceStatus= validateCompliance();
			} else {
				return;
			}
		} else {
//			updateComplianceEnableState();
//			updateAssertEnumAsIdentifierEnableState();
//			updateInlineJSREnableState();
			fComplianceStatus= validateCompliance();
//			validateJRE50Status();
		}		
		fContext.statusChanged(fComplianceStatus);
	}
	
	private void validateJRE50Status() {
		if (fJRE50InfoText != null && !fJRE50InfoText.isDisposed()) {
			boolean isVisible= false;
			String compliance= getStoredValue(PREF_COMPLIANCE); // get actual value
			IVMInstall install= null;
			if (fProject != null) { // project specific settings: only test if a 50 JRE is installed
				try {
					install= JavaRuntime.getVMInstall(JavaScriptCore.create(fProject));
				} catch (CoreException e) {
					JavaScriptPlugin.log(e);
				}
			} else {
				install= JavaRuntime.getDefaultVMInstall();
			}
			if (install instanceof IVMInstall2) {
				String compilerCompliance= JavaModelUtil.getCompilerCompliance((IVMInstall2) install, compliance);
				if (JavaModelUtil.isVersionLessThan(compilerCompliance, compliance)) { // Discourage using compiler with version less than compliance
					String[] args= { getVersionLabel(compliance), getVersionLabel(compilerCompliance) };
					if (fProject == null) {
						fJRE50InfoText.setText(Messages.format(PreferencesMessages.ComplianceConfigurationBlock_jrecompliance_info, args));
					} else {
						fJRE50InfoText.setText(Messages.format(PreferencesMessages.ComplianceConfigurationBlock_jrecompliance_info_project, args));
					}
					isVisible= true;
				}
			}
			fJRE50InfoText.setVisible(isVisible);
		}
	}

	private String getVersionLabel(String version) {
		if (JavaModelUtil.isVersionLessThan(version, VERSION_1_5)) {
			return version; // for version <= 1.4, we can use the version as is
		}
		if (VERSION_1_5.equals(version))
			return PreferencesMessages.ComplianceConfigurationBlock_version15;
		return PreferencesMessages.ComplianceConfigurationBlock_version16;
	}

	
	private IStatus validateCompliance() {
		StatusInfo status= new StatusInfo();
		String compliance= getValue(PREF_COMPLIANCE);
		String source= getValue(PREF_SOURCE_COMPATIBILITY);
		String target= getValue(PREF_CODEGEN_TARGET_PLATFORM);
		
		// compliance must not be smaller than source or target
		if (!VERSION_0_0.equals(compliance) && JavaModelUtil.isVersionLessThan(compliance, source)) {
			status.setError(PreferencesMessages.ComplianceConfigurationBlock_src_greater_compliance); 
			return status;
		}
		
		if (!VERSION_0_0.equals(compliance) && JavaModelUtil.isVersionLessThan(compliance, target)) {
			status.setError(PreferencesMessages.ComplianceConfigurationBlock_classfile_greater_compliance); 
			return status;
		}
		
		// target must not be smaller than source
		if (!VERSION_1_3.equals(source) && JavaModelUtil.isVersionLessThan(target, source)) {
			status.setError(PreferencesMessages.ComplianceConfigurationBlock_classfile_greater_source); 
			return status;
		}
		
		return status;
	}
			
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.ui.preferences.OptionsConfigurationBlock#useProjectSpecificSettings(boolean)
	 */
	public void useProjectSpecificSettings(boolean enable) {
		super.useProjectSpecificSettings(enable);
		validateJRE50Status();
	}
		
	/*
	 * Update the compliance controls' enable state
	 */		
	private void updateComplianceEnableState() {
		boolean enabled= checkValue(INTR_DEFAULT_COMPLIANCE, USER_CONF);
		for (int i= fComplianceControls.size() - 1; i >= 0; i--) {
			Control curr= (Control) fComplianceControls.get(i);
			curr.setEnabled(enabled);
		}
	}
	
	private void updateAssertEnumAsIdentifierEnableState() {
		if (checkValue(INTR_DEFAULT_COMPLIANCE, USER_CONF)) {
			String compatibility= getValue(PREF_SOURCE_COMPATIBILITY);
			
			boolean isLessThan14= VERSION_1_3.equals(compatibility);
			updateRememberedComplianceOption(PREF_PB_ASSERT_AS_IDENTIFIER, IDX_ASSERT_AS_IDENTIFIER, isLessThan14);
		}
	}
	
	private void updateRememberedComplianceOption(Key prefKey, int idx, boolean enabled) {
		Combo combo= getComboBox(prefKey);
		combo.setEnabled(enabled);
		
		if (!enabled) {
			String val= getValue(prefKey);
			if (!ERROR.equals(val)) {
				setValue(prefKey, ERROR);
				updateCombo(combo);
				fRememberedUserCompliance[idx]= val;
			}
		} else {
			String val= fRememberedUserCompliance[idx];
			if (!ERROR.equals(val)) {
				setValue(prefKey, val);
				updateCombo(combo);
			}
		}
	}

	private void updateInlineJSREnableState() {
		String target= getValue(PREF_CODEGEN_TARGET_PLATFORM);
		
		boolean enabled= JavaModelUtil.isVersionLessThan(target, VERSION_1_5);
		Button checkBox= getCheckBox(PREF_CODEGEN_INLINE_JSR_BYTECODE);
		checkBox.setEnabled(enabled);
		
		if (!enabled) {
			String val= getValue(PREF_CODEGEN_INLINE_JSR_BYTECODE);
			fRememberedUserCompliance[IDX_INLINE_JSR_BYTECODE]= val;
			
			if (!ENABLED.equals(val)) {
				setValue(PREF_CODEGEN_INLINE_JSR_BYTECODE, ENABLED);
				updateCheckBox(checkBox);
			}
		} else {
			String val= fRememberedUserCompliance[IDX_INLINE_JSR_BYTECODE];
			if (!ENABLED.equals(val)) {
				setValue(PREF_CODEGEN_INLINE_JSR_BYTECODE, val);
				updateCheckBox(checkBox);
			}
		}
	}

	/*
	 * Set the default compliance values derived from the chosen level
	 */	
	private void updateComplianceDefaultSettings(boolean rememberOld, String oldComplianceLevel) {
		String assertAsId, enumAsId, source, target;
		boolean isDefault= checkValue(INTR_DEFAULT_COMPLIANCE, DEFAULT_CONF);
		String complianceLevel= getValue(PREF_COMPLIANCE);
		
		if (isDefault) {
			if (rememberOld) {
				if (oldComplianceLevel == null) {
					oldComplianceLevel= complianceLevel;
				}
				
				fRememberedUserCompliance[IDX_ASSERT_AS_IDENTIFIER]= getValue(PREF_PB_ASSERT_AS_IDENTIFIER);
				fRememberedUserCompliance[IDX_SOURCE_COMPATIBILITY]= getValue(PREF_SOURCE_COMPATIBILITY);
				fRememberedUserCompliance[IDX_CODEGEN_TARGET_PLATFORM]= getValue(PREF_CODEGEN_TARGET_PLATFORM);
				fRememberedUserCompliance[IDX_COMPLIANCE]= oldComplianceLevel;
			}

			if (VERSION_1_4.equals(complianceLevel)) {
				assertAsId= WARNING;
				enumAsId= WARNING;
				source= VERSION_1_3;
				target= VERSION_1_2;
			} else if (VERSION_1_5.equals(complianceLevel)) {
				assertAsId= ERROR;
				enumAsId= ERROR;
				source= VERSION_1_5;
				target= VERSION_1_5;
			} else if (VERSION_1_6.equals(complianceLevel)) {
				assertAsId= ERROR;
				enumAsId= ERROR;
				source= VERSION_1_6;
				target= VERSION_1_6;				
			} else {
				assertAsId= IGNORE;
				enumAsId= IGNORE;
				source= VERSION_1_3;
				target= VERSION_1_1;
			}
		} else {
			if (rememberOld && complianceLevel.equals(fRememberedUserCompliance[IDX_COMPLIANCE])) {
				assertAsId= fRememberedUserCompliance[IDX_ASSERT_AS_IDENTIFIER];
				source= fRememberedUserCompliance[IDX_SOURCE_COMPATIBILITY];
				target= fRememberedUserCompliance[IDX_CODEGEN_TARGET_PLATFORM];
			} else {
				updateInlineJSREnableState();
				updateAssertEnumAsIdentifierEnableState();
				return;
			}
		}
		setValue(PREF_PB_ASSERT_AS_IDENTIFIER, assertAsId);
		setValue(PREF_SOURCE_COMPATIBILITY, source);
		setValue(PREF_CODEGEN_TARGET_PLATFORM, target);
		updateControls();
		updateInlineJSREnableState();
		updateAssertEnumAsIdentifierEnableState();
	}
	
	/*
	 * Evaluate if the current compliance setting correspond to a default setting
	 */
	private String getCurrentCompliance() {
		Object complianceLevel= getValue(PREF_COMPLIANCE);
		if ((VERSION_1_3.equals(complianceLevel)
				&& IGNORE.equals(getValue(PREF_PB_ASSERT_AS_IDENTIFIER))
				&& VERSION_1_3.equals(getValue(PREF_SOURCE_COMPATIBILITY))
				&& VERSION_1_1.equals(getValue(PREF_CODEGEN_TARGET_PLATFORM)))
			|| (VERSION_1_4.equals(complianceLevel)
				&& WARNING.equals(getValue(PREF_PB_ASSERT_AS_IDENTIFIER))
				&& VERSION_1_3.equals(getValue(PREF_SOURCE_COMPATIBILITY))
				&& VERSION_1_2.equals(getValue(PREF_CODEGEN_TARGET_PLATFORM)))
			|| (VERSION_1_5.equals(complianceLevel)
				&& ERROR.equals(getValue(PREF_PB_ASSERT_AS_IDENTIFIER))
				&& VERSION_1_5.equals(getValue(PREF_SOURCE_COMPATIBILITY))
				&& VERSION_1_5.equals(getValue(PREF_CODEGEN_TARGET_PLATFORM)))
			|| (VERSION_1_6.equals(complianceLevel)
				&& ERROR.equals(getValue(PREF_PB_ASSERT_AS_IDENTIFIER))
				&& VERSION_1_6.equals(getValue(PREF_SOURCE_COMPATIBILITY))
				&& VERSION_1_6.equals(getValue(PREF_CODEGEN_TARGET_PLATFORM)))) {
			return DEFAULT_CONF;
		}
		return USER_CONF;
	}
	
	
	protected String[] getFullBuildDialogStrings(boolean workspaceSettings) {
		String title= PreferencesMessages.ComplianceConfigurationBlock_needsbuild_title; 
		String message;
		if (workspaceSettings) {
			message= PreferencesMessages.ComplianceConfigurationBlock_needsfullbuild_message; 
		} else {
			message= PreferencesMessages.ComplianceConfigurationBlock_needsprojectbuild_message; 
		}
		return new String[] { title, message };
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.ui.preferences.OptionsConfigurationBlock#performOk()
	 */
	public boolean performOk() {
		setValue(INTR_DEFAULT_COMPLIANCE, null);
		return super.performOk();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.ui.preferences.OptionsConfigurationBlock#performApply()
	 */
	public boolean performApply() {
		setValue(INTR_DEFAULT_COMPLIANCE, null);
		boolean result= super.performApply();
		setValue(INTR_DEFAULT_COMPLIANCE, getCurrentCompliance());
		return result;
	}
		
}
