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
package org.eclipse.wst.jsdt.internal.ui.callhierarchy;

import org.eclipse.osgi.util.NLS;

public final class CallHierarchyMessages extends NLS {

	private static final String BUNDLE_NAME= "org.eclipse.wst.jsdt.internal.ui.callhierarchy.CallHierarchyMessages";//$NON-NLS-1$

	private CallHierarchyMessages() {
		// Do not instantiate
	}

	public static String HistoryDropDownAction_clearhistory_label;
	public static String LocationCopyAction_copy;
	public static String ToggleCallModeAction_callers_label;
	public static String ToggleCallModeAction_callers_tooltip;
	public static String ToggleCallModeAction_callers_description;
	public static String ToggleCallModeAction_callees_label;
	public static String ToggleCallModeAction_callees_tooltip;
	public static String ToggleCallModeAction_callees_description;
	public static String HistoryDropDownAction_tooltip;
	public static String HistoryAction_description;
	public static String HistoryAction_tooltip;
	public static String HistoryListDialog_title;
	public static String HistoryListDialog_label;
	public static String HistoryListDialog_remove_button;
	public static String HistoryListAction_label;
	public static String ToggleOrientationAction_vertical_label;
	public static String ToggleOrientationAction_vertical_description;
	public static String ToggleOrientationAction_vertical_tooltip;
	public static String ToggleOrientationAction_horizontal_label;
	public static String ToggleOrientationAction_horizontal_tooltip;
	public static String ToggleOrientationAction_horizontal_description;
	public static String ToggleOrientationAction_automatic_label;
	public static String ToggleOrientationAction_automatic_tooltip;
	public static String ToggleOrientationAction_automatic_description;
	public static String ToggleOrientationAction_single_label;
	public static String ToggleOrientationAction_single_tooltip;
	public static String ToggleOrientationAction_single_description;
	public static String ShowFilterDialogAction_text;
	public static String FiltersDialog_filter;
	public static String FiltersDialog_filterOnNames;
	public static String FiltersDialog_filterOnNamesSubCaption;
	public static String FiltersDialog_maxCallDepth;
	public static String FiltersDialog_messageMaxCallDepthInvalid;
	public static String CallHierarchyContentProvider_searchError_title;
	public static String CallHierarchyContentProvider_searchError_message;
	public static String CallHierarchyLabelProvider_root;
	public static String CallHierarchyLabelProvider_searchCanceled;
	public static String CallHierarchyLabelProvider_noMethodSelected;
	public static String CallHierarchyLabelProvider_updatePending;
	public static String CallHierarchyLabelProvider_matches;
	public static String CallHierarchyViewPart_empty;
	public static String CallHierarchyViewPart_callsToMethod;
	public static String CallHierarchyViewPart_callsFromMethod;
	public static String FocusOnSelectionAction_focusOnSelection_text;
	public static String FocusOnSelectionAction_focusOnSelection_description;
	public static String FocusOnSelectionAction_focusOnSelection_tooltip;
	public static String FocusOnSelectionAction_focusOn_text;
	public static String RefreshAction_text;
	public static String RefreshAction_tooltip;
	public static String SearchScopeActionGroup_searchScope;
	public static String SearchScopeActionGroup_hierarchy_text;
	public static String SearchScopeActionGroup_hierarchy_tooltip;
	public static String SearchScopeActionGroup_project_text;
	public static String SearchScopeActionGroup_project_tooltip;
	public static String SearchScopeActionGroup_workingset_tooltip;
	public static String SearchScopeActionGroup_workspace_text;
	public static String SearchScopeActionGroup_workspace_tooltip;
	public static String SearchScopeActionGroup_workingset_select_text;
	public static String SearchScopeActionGroup_workingset_select_tooltip;
	public static String WorkingSetScope;
	public static String SearchUtil_workingSetConcatenation;
	public static String SelectWorkingSetAction_error_title;
	public static String SelectWorkingSetAction_error_message;
	public static String OpenLocationAction_error_title;
	public static String CallHierarchyUI_open_in_editor_error_message;
	public static String CallHierarchyUI_open_in_editor_error_messageArgs;
	public static String CallHierarchyUI_error_open_view;
	public static String CopyCallHierarchyAction_label;
	public static String CopyCallHierarchyAction_problem;
	public static String CopyCallHierarchyAction_clipboard_busy;
	public static String OpenCallHierarchyAction_label;
	public static String OpenCallHierarchyAction_tooltip;
	public static String OpenCallHierarchyAction_description;
	public static String OpenCallHierarchyAction_messages_no_java_element;
	public static String OpenCallHierarchyAction_messages_no_valid_java_element;
	public static String OpenCallHierarchyAction_messages_title;
	public static String OpenCallHierarchyAction_dialog_title;
	public static String CancelSearchAction_label;
	public static String CancelSearchAction_tooltip;
	public static String CallHierarchyUI_selectionDialog_title;
	public static String CallHierarchyUI_selectionDialog_message;
	public static String OpenLocationAction_label;
	public static String OpenLocationAction_tooltip;
	public static String LocationViewer_ColumnIcon_header;
	public static String LocationViewer_ColumnLine_header;
	public static String LocationViewer_ColumnInfo_header;
	public static String LocationLabelProvider_unknown;

	static {
		NLS.initializeMessages(BUNDLE_NAME, CallHierarchyMessages.class);
	}

	public static String CallHierarchyViewPart_layout_menu;
}
