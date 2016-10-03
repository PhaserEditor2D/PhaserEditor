/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.workingsets;

import org.eclipse.osgi.util.NLS;

public final class WorkingSetMessages extends NLS {

	private static final String BUNDLE_NAME= "org.eclipse.wst.jsdt.internal.ui.workingsets.WorkingSetMessages";//$NON-NLS-1$

	private WorkingSetMessages() {
		// Do not instantiate
	}

	public static String AbstractWorkingSetPage_workingSet_name;
	public static String AbstractWorkingSetPage_warning_nameMustNotBeEmpty;
	public static String AbstractWorkingSetPage_warning_workingSetExists;
	public static String AbstractWorkingSetPage_warning_nameWhitespace;
	public static String ConfigureWorkingSetAssignementAction_DialogMessage_multi;
	public static String ConfigureWorkingSetAssignementAction_DialogMessage_specific;
	public static String ConfigureWorkingSetAssignementAction_OnlyShowVisible_link;
	public static String ConfigureWorkingSetAssignementAction_WorkingSetAssignments_title;
	public static String ConfigureWorkingSetAssignementAction_WorkingSets_actionLabel;
	public static String ConfigureWorkingSetAssignementAction_XofY_label;
	
	public static String JavaWorkingSetPage_title;
	public static String JavaWorkingSetPage_workingSet_name;
	public static String JavaWorkingSetPage_workingSet_description;
	public static String JavaWorkingSetPage_workingSet_content;
	public static String JavaWorkingSetPage_warning_nameMustNotBeEmpty;
	public static String JavaWorkingSetPage_warning_workingSetExists;
	public static String JavaWorkingSetPage_warning_resourceMustBeChecked;
	public static String JavaWorkingSetPage_warning_nameWhitespace;
	public static String JavaWorkingSetPage_selectAll_label;
	public static String JavaWorkingSetPage_selectAll_toolTip;
	public static String JavaWorkingSetPage_deselectAll_label;
	public static String JavaWorkingSetPage_deselectAll_toolTip;
	
	public static String SelectWorkingSetAction_text;
	public static String SelectWorkingSetAction_toolTip;
	
	public static String EditWorkingSetAction_text;
	public static String EditWorkingSetAction_toolTip;
	public static String EditWorkingSetAction_error_nowizard_title;
	public static String EditWorkingSetAction_error_nowizard_message;
	
	public static String ClearWorkingSetAction_text;
	public static String ClearWorkingSetAction_toolTip;
	
	public static String ConfigureWorkingSetAction_label;
	public static String SimpleWorkingSetSelectionDialog_DeselectAll_button;
	public static String SimpleWorkingSetSelectionDialog_New_button;
	public static String SimpleWorkingSetSelectionDialog_SelectAll_button;
	public static String SimpleWorkingSetSelectionDialog_show_only_js_working_sets_button;
	public static String SimpleWorkingSetSelectionDialog_SimpleSelectWorkingSetDialog_title;
	public static String ViewActionGroup_show_label;
	public static String ViewActionGroup_projects_label;
	public static String ViewActionGroup_workingSets_label;
	public static String WorkingSetConfigurationBlock_SelectWorkingSet_button;
	public static String WorkingSetConfigurationBlock_WorkingSetText_name;
	
	public static String WorkingSetModel_others_name;
	
	public static String WorkingSetConfigurationDialog_title;
	public static String WorkingSetConfigurationDialog_message;
	public static String WorkingSetConfigurationDialog_new_label;
	public static String WorkingSetConfigurationDialog_edit_label;
	public static String WorkingSetConfigurationDialog_remove_label;
	public static String WorkingSetConfigurationDialog_up_label;
	public static String WorkingSetConfigurationDialog_down_label;
	public static String WorkingSetConfigurationDialog_selectAll_label;
	public static String WorkingSetConfigurationDialog_deselectAll_label;
	
	public static String OpenPropertiesWorkingSetAction_label;
	public static String RemoveWorkingSetElementAction_label;

	static {
		NLS.initializeMessages(BUNDLE_NAME, WorkingSetMessages.class);
	}
}
