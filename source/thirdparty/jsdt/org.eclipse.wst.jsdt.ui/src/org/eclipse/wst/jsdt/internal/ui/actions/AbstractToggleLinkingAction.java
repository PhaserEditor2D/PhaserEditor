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
package org.eclipse.wst.jsdt.internal.ui.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;


/**
 * This is an action template for actions that toggle whether
 * it links its selection to the active editor.
 * 
 * 
 */
public abstract class AbstractToggleLinkingAction extends Action {
	
	/**
	 * Constructs a new action.
	 */
	public AbstractToggleLinkingAction() {
		super(ActionMessages.ToggleLinkingAction_label); 
		setDescription(ActionMessages.ToggleLinkingAction_description); 
		setToolTipText(ActionMessages.ToggleLinkingAction_tooltip); 
		JavaPluginImages.setLocalImageDescriptors(this, "synced.gif"); //$NON-NLS-1$		
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.LINK_EDITOR_ACTION);
	}

	/**
	 * Runs the action.
	 */
	public abstract void run();
}
