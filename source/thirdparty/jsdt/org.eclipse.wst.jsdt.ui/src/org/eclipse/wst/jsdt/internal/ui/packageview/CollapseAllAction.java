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
package org.eclipse.wst.jsdt.internal.ui.packageview;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;

/**
 * Collapse all nodes.
 */
class CollapseAllAction extends Action {
	
	private PackageExplorerPart fPackageExplorer;
	
	CollapseAllAction(PackageExplorerPart part) {
		super(PackagesMessages.CollapseAllAction_label); 
		setDescription(PackagesMessages.CollapseAllAction_description); 
		setToolTipText(PackagesMessages.CollapseAllAction_tooltip); 
		JavaPluginImages.setLocalImageDescriptors(this, "collapseall.gif"); //$NON-NLS-1$
		
		fPackageExplorer= part;
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.COLLAPSE_ALL_ACTION);
	}
 
	public void run() { 
		fPackageExplorer.collapseAll();
	}
}
