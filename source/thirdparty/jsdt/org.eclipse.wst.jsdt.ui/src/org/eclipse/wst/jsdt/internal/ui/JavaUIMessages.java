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
package org.eclipse.wst.jsdt.internal.ui;

import org.eclipse.osgi.util.NLS;

public final class JavaUIMessages extends NLS {

	private static final String BUNDLE_NAME= "org.eclipse.wst.jsdt.internal.ui.JavaUIMessages";//$NON-NLS-1$

	private JavaUIMessages() {
		// Do not instantiate
	}

	public static String FilteredTypesSelectionDialog_TypeFiltersPreferencesAction_label;
	public static String JavaPlugin_internal_error;
	public static String JavaPlugin_initializing_ui;
	
	public static String JavaElementProperties_name;
	public static String JsNature_Global;

	public static String OpenTypeAction_description;
	public static String OpenTypeAction_tooltip;
	public static String OpenTypeAction_errorMessage;
	public static String OpenTypeAction_errorTitle;
	public static String OpenTypeAction_label;
	public static String OpenTypeAction_dialogTitle;
	public static String OpenTypeAction_dialogMessage;
	
	public static String JavaUI_defaultDialogMessage;
	
	public static String MultiElementListSelectionDialog_pageInfoMessage;
	public static String PackageSelectionDialog_progress_findEmpty;
	public static String PackageSelectionDialog_progress_search;
	
	public static String TypeSelectionDialog_dialogMessage;
	public static String TypeSelectionDialog_errorTitle;
	public static String TypeSelectionDialog_error3Message;
	public static String TypeSelectionDialog_error3Title;
	public static String TypeSelectionDialog_progress_consistency;
	public static String TypeSelectionDialog_error_type_doesnot_exist;

	public static String ExceptionDialog_seeErrorLogMessage;
	
	public static String MainTypeSelectionDialog_errorTitle;
	public static String MultiMainTypeSelectionDialog_errorTitle;
	
	public static String PackageSelectionDialog_error_title;
	public static String PackageSelectionDialog_error3Message;
	public static String PackageSelectionDialog_nopackages_title;
	public static String PackageSelectionDialog_nopackages_message;
	
	public static String BuildPathDialog_title;
	
	public static String OverrideMethodDialog_groupMethodsByTypes;
	public static String OverrideMethodDialog_dialog_title;
	public static String OverrideMethodDialog_dialog_description;
	public static String OverrideMethodDialog_selectioninfo_more;
	public static String OverrideMethodDialog_link_tooltip;
	public static String OverrideMethodDialog_link_message;
	
		
	
	public static String JavaElementLabels_default_package;
	public static String JavaElementLabels_anonym_type;
	public static String JavaElementLabels_anonym;
	public static String JavaElementLabels_import_container;
	public static String JavaElementLabels_initializer;
	public static String JavaElementLabels_category;
	public static String JavaElementLabels_concat_string;
	public static String JavaElementLabels_comma_string;
	public static String JavaElementLabels_declseparator_string;
	public static String JavaElementLabels_category_separator_string;
	
	public static String StatusBarUpdater_num_elements_selected;
	
	public static String OpenTypeHierarchyUtil_error_open_view;
	public static String OpenTypeHierarchyUtil_error_open_perspective;
	public static String OpenTypeHierarchyUtil_error_open_editor;
	public static String OpenTypeHierarchyUtil_selectionDialog_title;
	public static String OpenTypeHierarchyUtil_selectionDialog_message;
	
	public static String TypeInfoLabelProvider_default_package;
	
	public static String JavaUIHelp_link_label;
	public static String JavaUIHelpContext_javaHelpCategory_label;
	
	public static String ResourceTransferDragAdapter_cannot_delete_resource;
	public static String ResourceTransferDragAdapter_moving_resource;
	public static String ResourceTransferDragAdapter_cannot_delete_files;
	
	public static String Spelling_error_label;
	public static String Spelling_correct_label;
	public static String Spelling_add_info;
	public static String Spelling_add_label;
	public static String Spelling_add_askToConfigure_title;
	public static String Spelling_add_askToConfigure_question;
	public static String Spelling_add_askToConfigure_ignoreMessage;
	public static String Spelling_ignore_info;
	public static String Spelling_ignore_label;
	public static String Spelling_disable_label;
	public static String Spelling_disable_info;
	public static String Spelling_case_label;
	public static String Spelling_error_case_label;
	public static String AbstractSpellingDictionary_encodingError;
	
	public static String JavaAnnotationHover_multipleMarkersAtThisLine;
	public static String JavaEditor_codeassist_noCompletions;
	
	public static String OptionalMessageDialog_dontShowAgain;
	public static String ElementValidator_cannotPerform;
	public static String SelectionListenerWithASTManager_job_title;
	
	public static String JavaOutlineControl_statusFieldText_hideInheritedMembers;
	public static String JavaOutlineControl_statusFieldText_showInheritedMembers;
	
	public static String RenameSupport_not_available;
	public static String RenameSupport_dialog_title;
	
	public static String CoreUtility_job_title;
	public static String CoreUtility_buildall_taskname;
	public static String CoreUtility_buildproject_taskname;
	
	public static String FilteredTypesSelectionDialog_default_package;
	public static String FilteredTypesSelectionDialog_dialogMessage;
	public static String FilteredTypesSelectionDialog_error_type_doesnot_exist;
	public static String FilteredTypesSelectionDialog_library_name_format;
	public static String FilteredTypesSelectionDialog_searchJob_taskName;
	public static String FilteredTypeSelectionDialog_showContainerForDuplicatesAction;
	public static String FilteredTypeSelectionDialog_titleFormat;
	
	public static String TypeSelectionDialog2_title_format;
	
	public static String TypeSelectionComponent_label;
	public static String TypeSelectionComponent_menu;
	public static String TypeSelectionComponent_show_status_line_label;
	public static String TypeSelectionComponent_fully_qualify_duplicates_label;
	
	public static String TypeInfoViewer_job_label;
	public static String TypeInfoViewer_job_error;
	public static String TypeInfoViewer_job_cancel;
	public static String TypeInfoViewer_default_package;
	public static String TypeInfoViewer_progress_label;
	public static String TypeInfoViewer_searchJob_taskName;
	public static String TypeInfoViewer_syncJob_label;
	public static String TypeInfoViewer_syncJob_taskName;
	public static String TypeInfoViewer_progressJob_label;
	public static String TypeInfoViewer_remove_from_history;
	public static String TypeInfoViewer_separator_message;
	public static String TypeInfoViewer_library_name_format;
	
	public static String InitializeAfterLoadJob_starter_job_name;
	
	
	static {
		NLS.initializeMessages(BUNDLE_NAME, JavaUIMessages.class);
	}

	public static String HistoryListAction_remove;
	public static String HistoryListAction_max_entries_constraint;
	public static String HistoryListAction_remove_all;

}
