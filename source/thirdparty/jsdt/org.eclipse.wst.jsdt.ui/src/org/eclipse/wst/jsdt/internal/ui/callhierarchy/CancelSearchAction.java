/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Jesper Kamstrup Linnet (eclipse@kamstrup-linnet.dk) - initial API and implementation 
 *          (report 36180: Callers/Callees view)
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.callhierarchy;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;

/**
 * This class is copied from the org.eclipse.search2.internal.ui.CancelSearchAction class. 
 */
public class CancelSearchAction extends Action {

	private CallHierarchyViewPart fView;

	public CancelSearchAction(CallHierarchyViewPart view) {
		super(CallHierarchyMessages.CancelSearchAction_label); 
		fView= view;
		setToolTipText(CallHierarchyMessages.CancelSearchAction_tooltip); 
        JavaPluginImages.setLocalImageDescriptors(this, "ch_cancel.gif"); //$NON-NLS-1$

        PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.CALL_HIERARCHY_CANCEL_SEARCH_ACTION);
}
	
	public void run() {
		fView.cancelJobs();
	}
}
