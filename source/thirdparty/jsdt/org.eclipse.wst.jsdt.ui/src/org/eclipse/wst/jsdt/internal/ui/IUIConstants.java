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

import org.eclipse.wst.jsdt.ui.JavaScriptUI;


public interface IUIConstants {
	
	public static final String KEY_OK= JavaScriptUI.ID_PLUGIN + ".ok.label"; //$NON-NLS-1$
	public static final String KEY_CANCEL= JavaScriptUI.ID_PLUGIN + ".cancel.label"; //$NON-NLS-1$
	
	public static final String P_ICON_NAME= JavaScriptUI.ID_PLUGIN + ".icon_name"; //$NON-NLS-1$
	
	public static final String DIALOGSTORE_LASTEXTJAR= JavaScriptUI.ID_PLUGIN + ".lastextjar"; //$NON-NLS-1$
	public static final String DIALOGSTORE_LASTJARATTACH= JavaScriptUI.ID_PLUGIN + ".lastjarattach"; //$NON-NLS-1$
	public static final String DIALOGSTORE_LASTVARIABLE= JavaScriptUI.ID_PLUGIN + ".lastvariable";	 //$NON-NLS-1$
	
	public static final String DIALOGSTORE_TYPECOMMENT_DEPRECATED= JavaScriptUI.ID_PLUGIN + ".typecomment.deprecated";	 //$NON-NLS-1$
	
	public static final boolean SUPPORT_REFACTORING=false;
	
}
