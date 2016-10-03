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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.wst.jsdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;
import org.eclipse.wst.jsdt.ui.PreferenceConstants;

public class AppearancePreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private static final String SHOW_CU_CHILDREN= PreferenceConstants.SHOW_CU_CHILDREN;
	private static final String PREF_METHOD_RETURNTYPE= PreferenceConstants.APPEARANCE_METHOD_RETURNTYPE;
//	private static final String PREF_METHOD_TYPEPARAMETERS= PreferenceConstants.APPEARANCE_METHOD_TYPEPARAMETERS;
//	private static final String PREF_COMPRESS_PACKAGE_NAMES= PreferenceConstants.APPEARANCE_COMPRESS_PACKAGE_NAMES;
//	private static final String PREF_PKG_NAME_PATTERN_FOR_PKG_VIEW= PreferenceConstants.APPEARANCE_PKG_NAME_PATTERN_FOR_PKG_VIEW;
//	private static final String STACK_BROWSING_VIEWS_VERTICALLY= PreferenceConstants.BROWSING_STACK_VERTICALLY;
	private static final String PREF_FOLD_PACKAGES_IN_PACKAGE_EXPLORER= PreferenceConstants.APPEARANCE_FOLD_PACKAGES_IN_PACKAGE_EXPLORER;
	private static final String PREF_CATEGORY= PreferenceConstants.APPEARANCE_CATEGORY;
	
	public static final String PREF_COLORED_LABELS= "colored_labels_in_views"; //$NON-NLS-1$

	private SelectionButtonDialogField fShowMethodReturnType;
	private SelectionButtonDialogField fShowCategory;
//	private SelectionButtonDialogField fCompressPackageNames;
//	private SelectionButtonDialogField fStackBrowsingViewsVertically;
	private SelectionButtonDialogField fShowMembersInPackageView;
//	private StringDialogField fPackageNamePattern;
	private SelectionButtonDialogField fFoldPackagesInPackageExplorer;
//	private SelectionButtonDialogField fShowMethodTypeParameters;
	private SelectionButtonDialogField fShowColoredLabels;
	
	public AppearancePreferencePage() {
		setPreferenceStore(JavaScriptPlugin.getDefault().getPreferenceStore());
		setDescription(PreferencesMessages.AppearancePreferencePage_description); 
	
		IDialogFieldListener listener= new IDialogFieldListener() {
			public void dialogFieldChanged(DialogField field) {
				doDialogFieldChanged(field);
			}
		};
	
		fShowMethodReturnType= new SelectionButtonDialogField(SWT.CHECK);
		fShowMethodReturnType.setDialogFieldListener(listener);
//		fShowMethodReturnType.setLabelText(PreferencesMessages.AppearancePreferencePage_methodreturntype_label); 
		fShowMethodReturnType.setLabelText(PreferencesMessages.AppearancePreferencePage_inferredmethodreturntype_label); 

//		fShowMethodTypeParameters= new SelectionButtonDialogField(SWT.CHECK);
//		fShowMethodTypeParameters.setDialogFieldListener(listener);
//		fShowMethodTypeParameters.setLabelText(PreferencesMessages.AppearancePreferencePage_methodtypeparams_label); 
		
		fShowCategory= new SelectionButtonDialogField(SWT.CHECK);
		fShowCategory.setDialogFieldListener(listener);
		fShowCategory.setLabelText(PreferencesMessages.AppearancePreferencePage_showCategory_label); 
		
		fShowMembersInPackageView= new SelectionButtonDialogField(SWT.CHECK);
		fShowMembersInPackageView.setDialogFieldListener(listener);
		fShowMembersInPackageView.setLabelText(PreferencesMessages.AppearancePreferencePage_showMembersInPackagesView); 

//		fStackBrowsingViewsVertically= new SelectionButtonDialogField(SWT.CHECK);
//		fStackBrowsingViewsVertically.setDialogFieldListener(listener);
//		fStackBrowsingViewsVertically.setLabelText(PreferencesMessages.AppearancePreferencePage_stackViewsVerticallyInTheJavaBrowsingPerspective); 
		
		fFoldPackagesInPackageExplorer= new SelectionButtonDialogField(SWT.CHECK);
		fFoldPackagesInPackageExplorer.setDialogFieldListener(listener);
//		fFoldPackagesInPackageExplorer.setLabelText(PreferencesMessages.AppearancePreferencePage_foldEmptyPackages); 
		fFoldPackagesInPackageExplorer.setLabelText(PreferencesMessages.AppearancePreferencePage_foldEmptySourceFolders); 

//		fCompressPackageNames= new SelectionButtonDialogField(SWT.CHECK);
//		fCompressPackageNames.setDialogFieldListener(listener);
//		fCompressPackageNames.setLabelText(PreferencesMessages.AppearancePreferencePage_pkgNamePatternEnable_label); 

//		fPackageNamePattern= new StringDialogField();
//		fPackageNamePattern.setDialogFieldListener(listener);
//		fPackageNamePattern.setLabelText(PreferencesMessages.AppearancePreferencePage_pkgNamePattern_label);
		
		fShowColoredLabels= new SelectionButtonDialogField(SWT.CHECK);
		fShowColoredLabels.setDialogFieldListener(listener);
		fShowColoredLabels.setLabelText(PreferencesMessages.AppearancePreferencePage_coloredlabels_label);
	}	

	private void initFields() {
		IPreferenceStore prefs= getPreferenceStore();
		fShowMethodReturnType.setSelection(prefs.getBoolean(PREF_METHOD_RETURNTYPE));
//		fShowMethodTypeParameters.setSelection(prefs.getBoolean(PREF_METHOD_TYPEPARAMETERS));
		fShowMembersInPackageView.setSelection(prefs.getBoolean(SHOW_CU_CHILDREN));
		fShowCategory.setSelection(prefs.getBoolean(PREF_CATEGORY));
//		fStackBrowsingViewsVertically.setSelection(prefs.getBoolean(STACK_BROWSING_VIEWS_VERTICALLY));
//		fPackageNamePattern.setText(prefs.getString(PREF_PKG_NAME_PATTERN_FOR_PKG_VIEW));
//		fCompressPackageNames.setSelection(prefs.getBoolean(PREF_COMPRESS_PACKAGE_NAMES));
//		fPackageNamePattern.setEnabled(fCompressPackageNames.isSelected());
		fFoldPackagesInPackageExplorer.setSelection(prefs.getBoolean(PREF_FOLD_PACKAGES_IN_PACKAGE_EXPLORER));
		
		fShowColoredLabels.setSelection(prefs.getBoolean(PREF_COLORED_LABELS));
	}
	
	/*
	 * @see PreferencePage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		super.createControl(parent);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IJavaHelpContextIds.APPEARANCE_PREFERENCE_PAGE);
	}	

	/*
	 * @see PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {
		initializeDialogUnits(parent);
		int nColumns= 1;
				
		Composite result= new Composite(parent, SWT.NONE);
		result.setFont(parent.getFont());
		
		GridLayout layout= new GridLayout();
		layout.marginHeight= convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
		layout.marginWidth= 0;
		layout.numColumns= nColumns;
		result.setLayout(layout);
				
		fShowMethodReturnType.doFillIntoGrid(result, nColumns);
//		fShowMethodTypeParameters.doFillIntoGrid(result, nColumns);
		fShowCategory.doFillIntoGrid(result, nColumns);
		fShowMembersInPackageView.doFillIntoGrid(result, nColumns);				
		fShowColoredLabels.doFillIntoGrid(result, nColumns);
		fFoldPackagesInPackageExplorer.doFillIntoGrid(result, nColumns);

//		new Separator().doFillIntoGrid(result, nColumns);
		
//		fCompressPackageNames.doFillIntoGrid(result, nColumns);
//		fPackageNamePattern.doFillIntoGrid(result, 2);
//		LayoutUtil.setHorizontalGrabbing(fPackageNamePattern.getTextControl(null));
//		LayoutUtil.setWidthHint(fPackageNamePattern.getLabelControl(null), convertWidthInCharsToPixels(65));
		
//		new Separator().doFillIntoGrid(result, nColumns);
//		fStackBrowsingViewsVertically.doFillIntoGrid(result, nColumns);
		
//		String noteTitle= PreferencesMessages.AppearancePreferencePage_note; 
//		String noteMessage= PreferencesMessages.AppearancePreferencePage_preferenceOnlyEffectiveForNewPerspectives; 
//		Composite noteControl= createNoteComposite(JFaceResources.getDialogFont(), result, noteTitle, noteMessage);
//		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
//		gd.horizontalSpan= 2;
//		noteControl.setLayoutData(gd);
		
		initFields();
		
		Dialog.applyDialogFont(result);
		return result;
	}
	
	private void doDialogFieldChanged(DialogField field) {
//		if (field == fCompressPackageNames)
//			fPackageNamePattern.setEnabled(fCompressPackageNames.isSelected());
	
		updateStatus(getValidationStatus());
	}
	
	private IStatus getValidationStatus(){
//		if (fCompressPackageNames.isSelected() && fPackageNamePattern.getText().equals("")) //$NON-NLS-1$
//			return new StatusInfo(IStatus.ERROR, PreferencesMessages.AppearancePreferencePage_packageNameCompressionPattern_error_isEmpty); 
//		else	
			return new StatusInfo();
	}
	
	private void updateStatus(IStatus status) {
		setValid(!status.matches(IStatus.ERROR));
		StatusUtil.applyToStatusLine(this, status);
	}		
	
	/*
	 * @see IWorkbenchPreferencePage#init(IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}
	
	/*
	 * @see IPreferencePage#performOk()
	 */
	public boolean performOk() {
		IPreferenceStore prefs= getPreferenceStore();
		prefs.setValue(PREF_METHOD_RETURNTYPE, fShowMethodReturnType.isSelected());
//		prefs.setValue(PREF_METHOD_TYPEPARAMETERS, fShowMethodTypeParameters.isSelected());
		prefs.setValue(PREF_CATEGORY, fShowCategory.isSelected());
		prefs.setValue(SHOW_CU_CHILDREN, fShowMembersInPackageView.isSelected());
//		prefs.setValue(STACK_BROWSING_VIEWS_VERTICALLY, fStackBrowsingViewsVertically.isSelected());
//		prefs.setValue(PREF_PKG_NAME_PATTERN_FOR_PKG_VIEW, fPackageNamePattern.getText());
//		prefs.setValue(PREF_COMPRESS_PACKAGE_NAMES, fCompressPackageNames.isSelected());
		prefs.setValue(PREF_FOLD_PACKAGES_IN_PACKAGE_EXPLORER, fFoldPackagesInPackageExplorer.isSelected());
		prefs.setValue(PREF_COLORED_LABELS, fShowColoredLabels.isSelected());
		JavaScriptPlugin.getDefault().savePluginPreferences();
		return super.performOk();
	}	
	
	/*
	 * @see PreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		IPreferenceStore prefs= getPreferenceStore();
		fShowMethodReturnType.setSelection(prefs.getDefaultBoolean(PREF_METHOD_RETURNTYPE));
//		fShowMethodTypeParameters.setSelection(prefs.getDefaultBoolean(PREF_METHOD_TYPEPARAMETERS));
		fShowCategory.setSelection(prefs.getDefaultBoolean(PREF_CATEGORY));
		fShowMembersInPackageView.setSelection(prefs.getDefaultBoolean(SHOW_CU_CHILDREN));
//		fStackBrowsingViewsVertically.setSelection(prefs.getDefaultBoolean(STACK_BROWSING_VIEWS_VERTICALLY));
//		fPackageNamePattern.setText(prefs.getDefaultString(PREF_PKG_NAME_PATTERN_FOR_PKG_VIEW));
//		fCompressPackageNames.setSelection(prefs.getDefaultBoolean(PREF_COMPRESS_PACKAGE_NAMES));
		fFoldPackagesInPackageExplorer.setSelection(prefs.getDefaultBoolean(PREF_FOLD_PACKAGES_IN_PACKAGE_EXPLORER));
		fShowColoredLabels.setSelection(false);
		super.performDefaults();
	}
}

