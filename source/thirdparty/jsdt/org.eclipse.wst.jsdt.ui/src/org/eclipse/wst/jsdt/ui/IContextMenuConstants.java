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
package org.eclipse.wst.jsdt.ui;

import org.eclipse.ui.navigator.ICommonMenuConstants;

/**
*
* Provisional API: This class/interface is part of an interim API that is still under development and expected to
* change significantly before reaching stability. It is being made available at this early stage to solicit feedback
* from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
* (repeatedly) as the API evolves.
*/
public interface IContextMenuConstants {
	
	
	/**
	 * Type hierarchy view part: pop-up menu target ID for type hierarchy viewer
	 * (value <code>"org.eclipse.wst.jsdt.ui.TypeHierarchy.typehierarchy"</code>).
	 * 
	 * 
	 */
	public static final String TARGET_ID_HIERARCHY_VIEW= JavaScriptUI.ID_TYPE_HIERARCHY + ".typehierarchy"; //$NON-NLS-1$	

	/**
	 * Type hierarchy view part: pop-up menu target ID for supertype hierarchy viewer
	 * (value <code>"org.eclipse.wst.jsdt.ui.TypeHierarchy.supertypes"</code>).
	 * 
	 * 
	 */
	public static final String TARGET_ID_SUPERTYPES_VIEW= JavaScriptUI.ID_TYPE_HIERARCHY + ".supertypes"; //$NON-NLS-1$	

	/**
	 * Type hierarchy view part: Pop-up menu target ID for the subtype hierarchy viewer
	 * (value <code>"org.eclipse.wst.jsdt.ui.TypeHierarchy.subtypes"</code>).
	 * 
	 * 
	 */
	public static final String TARGET_ID_SUBTYPES_VIEW= JavaScriptUI.ID_TYPE_HIERARCHY + ".subtypes"; //$NON-NLS-1$	

	/**
	 * Type hierarchy view part: pop-up menu target ID for the member viewer
	 * (value <code>"org.eclipse.wst.jsdt.ui.TypeHierarchy.members"</code>).
	 * 
	 * 
	 */
	public static final String TARGET_ID_MEMBERS_VIEW= JavaScriptUI.ID_TYPE_HIERARCHY + ".members"; //$NON-NLS-1$	
	

	/**
	 * Pop-up menu: name of group for goto actions (value <code>"group.goto"</code>).
	 * <p>
	 * Examples for open actions are:
	 * <ul>
	 *  <li>Go Into</li>
	 *  <li>Go To</li>
	 * </ul>
	 * </p>
	 */
	public static final String GROUP_GOTO=		ICommonMenuConstants.GROUP_GOTO;
	/**
	 * Pop-up menu: name of group for open actions (value <code>"group.open"</code>).
	 * <p>
	 * Examples for open actions are:
	 * <ul>
	 *  <li>Open To</li>
	 *  <li>Open With</li>
	 * </ul>
	 * </p>
	 */
	public static final String GROUP_OPEN=		ICommonMenuConstants.GROUP_OPEN;
	
	/**
	 * Pop-up menu: name of group for show actions (value <code>"group.show"</code>).
	 * <p>
	 * Examples for show actions are:
	 * <ul>
	 *  <li>Show in Navigator</li>
	 *  <li>Show in Type Hierarchy</li>
	 * </ul>
	 * </p>
	 */
	public static final String GROUP_SHOW=		ICommonMenuConstants.GROUP_SHOW;
	
	/**
	 * Pop-up menu: name of group for new actions (value <code>"group.new"</code>).
	 * <p>
	 * Examples for new actions are:
	 * <ul>
	 *  <li>Create new class</li>
	 *  <li>Create new interface</li>
	 * </ul>
	 * </p>
	 */
	public static final String GROUP_NEW=		ICommonMenuConstants.GROUP_NEW;

	/**
	 * Pop-up menu: name of group for build actions (value <code>"group.build"</code>).
	 */
	public static final String GROUP_BUILD=		ICommonMenuConstants.GROUP_BUILD;
	
	/**
	 * Pop-up menu: name of group for reorganize actions (value <code>"group.reorganize"</code>).
	 */	
	public static final String GROUP_REORGANIZE=	ICommonMenuConstants.GROUP_REORGANIZE;	
	
	/**
	 * Pop-up menu: name of group for code generation actions (
	 * value <code>"group.generate"</code>).
	 */	
	public static final String GROUP_GENERATE=	ICommonMenuConstants.GROUP_GENERATE;

	/**
	 * Pop-up menu: name of group for source actions. This is an alias for
	 * <code>GROUP_GENERATE</code> to be more consistent with main menu
	 * bar structure.
	 * 
	 * 
	 */	
	public static final String GROUP_SOURCE=		ICommonMenuConstants.GROUP_SOURCE;

	/**
	 * Pop-up menu: name of group for search actions (value <code>"group.search"</code>).
	 */	
	public static final String GROUP_SEARCH=		ICommonMenuConstants.GROUP_SEARCH;
	
	/**
	 * Pop-up menu: name of group for additional actions (value <code>"additions"</code>).
	 */	
	public static final String GROUP_ADDITIONS=	ICommonMenuConstants.GROUP_ADDITIONS;

	/**
	 * Pop-up menu: name of group for viewer setup actions (value <code>"group.viewerSetup"</code>).
	 */	
	public static final String GROUP_VIEWER_SETUP=	ICommonMenuConstants.GROUP_VIEWER_SETUP;

	/**
	 * Pop-up menu: name of group for properties actions (value <code>"group.properties"</code>).
	 */	
	public static final String GROUP_PROPERTIES=	ICommonMenuConstants.GROUP_PROPERTIES;
}
