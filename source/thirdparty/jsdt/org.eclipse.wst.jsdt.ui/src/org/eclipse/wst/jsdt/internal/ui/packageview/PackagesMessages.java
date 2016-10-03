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
package org.eclipse.wst.jsdt.internal.ui.packageview;

import org.eclipse.osgi.util.NLS;

public final class PackagesMessages extends NLS {

	private static final String BUNDLE_NAME= "org.eclipse.wst.jsdt.internal.ui.packageview.PackagesMessages";//$NON-NLS-1$

	private PackagesMessages() {
		// Do not instantiate
	}

	public static String DragAdapter_deleting;
	public static String DragAdapter_problem;
	public static String DragAdapter_problemTitle;
	public static String DragAdapter_refreshing;
	public static String DropAdapter_errorTitle;
	public static String DropAdapter_errorMessage;
	public static String GotoPackage_action_label;
	public static String GotoPackage_dialog_message;
	public static String GotoPackage_dialog_title;
	public static String GotoPackage_action_description;
	public static String GotoRequiredProjectAction_label;
	public static String GotoRequiredProjectAction_description;
	public static String GotoRequiredProjectAction_tooltip;
	public static String GotoType_action_label;
	public static String GotoType_action_description;
	public static String GotoType_dialog_message;
	public static String GotoType_dialog_title;
	public static String GotoType_error_message;
	public static String GotoResource_action_label;
	public static String GotoResource_dialog_title;
	public static String LayoutActionGroup_show_libraries_in_group;
	public static String LibraryContainer_name;
	public static String PackageExplorerPart_notFoundSepcific;
	public static String PackageExplorerPart_removeFiltersSpecific;
	public static String PackageExplorer_title;
	public static String PackageExplorer_toolTip;
	public static String PackageExplorer_toolTip2;
	public static String PackageExplorer_toolTip3;
	public static String PackageExplorer_element_not_present;
	public static String PackageExplorer_filteredDialog_title;
	public static String PackageExplorer_notFound;
	public static String PackageExplorer_removeFilters;
	public static String SelectionTransferDropAdapter_error_title;
	public static String SelectionTransferDropAdapter_error_message;
	public static String CollapseAllAction_label;
	public static String CollapseAllAction_tooltip;
	public static String CollapseAllAction_description;
	public static String LayoutActionGroup_label;
	public static String LayoutActionGroup_flatLayoutAction_label;
	public static String LayoutActionGroup_hierarchicalLayoutAction_label;
	public static String JsGlobalScopeContainer_unbound_label;
	public static String JsGlobalScopeContainer_unknown_label;
	public static String PackageExplorerPart_workspace;
	public static String PackageExplorerPart_workingSetModel;
	public static String PackageExplorerContentProvider_update_job_description;
	public static String LoadingJavaScriptNode;
	public static String UpdatingViewer;

	static {
		NLS.initializeMessages(BUNDLE_NAME, PackagesMessages.class);
	}
}
