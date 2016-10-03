/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/**
 * 
 */
package org.eclipse.wst.jsdt.ui;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.action.IAction;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.wst.jsdt.internal.ui.preferences.BuildPathsPropertyPage;

/**
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 *
 */
public class JavaScriptSuperTypeAction extends JavaScriptLibrariesAction {

	private static final int BUILD_PATH_PAGE_INDEX = 1;
	
	public void run(IAction arg0) {
		Map data = new HashMap();
		data.put(BuildPathsPropertyPage.DATA_PAGE_INDEX, Integer.valueOf(BUILD_PATH_PAGE_INDEX));
		String ID = arg0.getId();
		String propertyPage = (String)PROPS_TO_IDS.get(ID);
		
		PreferencesUtil.createPropertyDialogOn(getShell(), project, propertyPage , null, data).open();
	}
	
	
}
