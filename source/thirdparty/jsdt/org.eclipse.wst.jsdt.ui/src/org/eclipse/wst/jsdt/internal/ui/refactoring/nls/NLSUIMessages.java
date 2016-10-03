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
package org.eclipse.wst.jsdt.internal.ui.refactoring.nls;

import org.eclipse.osgi.util.NLS;

/**
 * Helper class to get NLSed messages.
 */
final class NLSUIMessages extends NLS {

	private static final String BUNDLE_NAME= NLSUIMessages.class.getName();

	private NLSUIMessages() {
		// Do not instantiate
	}

	public static String ExternalizeWizard_name;
	public static String ExternalizeWizard_select;
	public static String ExternalizeWizardPage_title;
	public static String ExternalizeWizardPage_description;
	public static String ExternalizeWizardPage_exception_title;
	public static String ExternalizeWizardPage_exception_message;
	public static String ExternalizeWizardPage_common_prefix;
	public static String ExternalizeWizardPage_context;
	public static String ExternalizeWizardPage_strings_to_externalize;
	public static String ExternalizeWizardPage_Externalize_Selected;
	public static String ExternalizeWizardPage_Ignore_Selected;
	public static String ExternalizeWizardPage_Internalize_Selected;
	public static String ExternalizeWizardPage_Revert_Selected;
	public static String ExternalizeWizardPage_isEclipseNLSCheckbox;
	public static String ExternalizeWizardPage_Rename_Keys;
	public static String ExternalizeWizardPage_NLSInputDialog_Label;
	public static String ExternalizeWizardPage_NLSInputDialog_Enter_key;
	public static String ExternalizeWizardPage_NLSInputDialog_ext_Label;
	public static String ExternalizeWizardPage_NLSInputDialog_Enter_value;
	public static String ExternalizeWizardPage_NLSInputDialog_Title;
	public static String ExternalizeWizardPage_NLSInputDialog_Error_empty_key;
	public static String ExternalizeWizardPage_NLSInputDialog_Error_invalid_key;
	public static String ExternalizeWizardPage_NLSInputDialog_Error_invalid_EclipseNLS_key;
	public static String NLSAccessorConfigurationDialog_browse1;
	public static String NLSAccessorConfigurationDialog_browse2;
	public static String NLSAccessorConfigurationDialog_browse3;
	public static String NLSAccessorConfigurationDialog_browse4;
	public static String NLSAccessorConfigurationDialog_browse5;
	public static String NLSAccessorConfigurationDialog_browse6;
	public static String NLSAccessorConfigurationDialog_property_file_name;
	public static String NLSAccessorConfigurationDialog_substitutionPattern;
	public static String NLSAccessorConfigurationDialog_externalizing;
	public static String NLSAccessorConfigurationDialog_exception;
	public static String NLSAccessorConfigurationDialog_must_exist;
	public static String NLSAccessorConfigurationDialog_incorrect_package;
	public static String NLSAccessorConfigurationDialog_enter_name;
	public static String NLSAccessorConfigurationDialog_file_name_must_end;
	public static String NLSAccessorConfigurationDialog_className;
	public static String NLSAccessorConfigurationDialog_no_dot;
	public static String NLSAccessorConfigurationDialog_default_package;
	public static String NLSAccessorConfigurationDialog_default;
	public static String NLSAccessorConfigurationDialog_property_location;
	public static String NLSAccessorConfigurationDialog_Choose_the_property_file;
	public static String NLSAccessorConfigurationDialog_Property_File_Selection;
	public static String NLSAccessorConfigurationDialog_Choose_the_accessor_file;
	public static String NLSAccessorConfigurationDialog_Accessor_Selection;
	public static String NLSAccessorConfigurationDialog_accessor_path;
	public static String NLSAccessorConfigurationDialog_accessor_package;
	public static String NLSAccessorConfigurationDialog_resourceBundle_title;
	public static String NLSAccessorConfigurationDialog_property_path;
	public static String NLSAccessorConfigurationDialog_property_package;
	public static String NLSAccessorConfigurationDialog_accessor_package_root_invalid;
	public static String NLSAccessorConfigurationDialog_accessor_package_invalid;
	public static String NLSAccessorConfigurationDialog_property_package_root_invalid;
	public static String NLSAccessorConfigurationDialog_property_package_invalid;
	public static String NLSAccessorConfigurationDialog_substitution_pattern_missing;
	public static String NLSAccessorConfigurationDialog_accessor_dialog_title;
	public static String NLSAccessorConfigurationDialog_accessor_dialog_message;
	public static String NLSAccessorConfigurationDialog_accessor_dialog_emtpyMessage;
	public static String NLSAccessorConfigurationDialog_property_dialog_title;
	public static String NLSAccessorConfigurationDialog_property_dialog_message;
	public static String NLSAccessorConfigurationDialog_property_dialog_emptyMessage;
	public static String ExternalizeWizardPage_key;
	public static String ExternalizeWizardPage_value;
	public static String ExternalizeWizardPage_Edit_key_and_value;
	public static String ExternalizeWizardPage_accessorclass_label;
	public static String ExternalizeWizardPage_configure_button;
	public static String ExternalizeWizardPage_warning_conflicting;
	public static String ExternalizeWizardPage_warning_keymissing;
	public static String ExternalizeWizardPage_warning_keyInvalid;
	public static String ExternalizeWizardPage_warning_EclipseNLS_keyInvalid;
	public static String ExternalizeWizardPage_filter_label;
	public static String NLSAccessorConfigurationDialog_title;
	public static String PackageBrowseAdapter_package_selection;
	public static String PackageBrowseAdapter_choose_package;
	public static String RenameKeysDialog_title;
	public static String RenameKeysDialog_description_noprefix;
	public static String RenameKeysDialog_description_withprefix;
	public static String SourceFirstPackageSelectionDialogField_ChooseSourceContainerDialog_title;
	public static String SourceFirstPackageSelectionDialogField_ChooseSourceContainerDialog_description;

	static {
		NLS.initializeMessages(BUNDLE_NAME, NLSUIMessages.class);
	}
}
