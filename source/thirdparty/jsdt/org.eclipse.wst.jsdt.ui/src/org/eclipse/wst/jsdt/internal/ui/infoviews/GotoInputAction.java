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
package org.eclipse.wst.jsdt.internal.ui.infoviews;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;
import org.eclipse.wst.jsdt.ui.actions.OpenAction;

class GotoInputAction extends Action {

	private AbstractInfoView fInfoView;

	public GotoInputAction(AbstractInfoView infoView) {
		Assert.isNotNull(infoView);
		fInfoView= infoView;

		JavaPluginImages.setLocalImageDescriptors(this, "goto_input.gif"); //$NON-NLS-1$
		setText(InfoViewMessages.GotoInputAction_label);
		setToolTipText(InfoViewMessages.GotoInputAction_tooltip);
		setDescription(InfoViewMessages.GotoInputAction_description);

		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.OPEN_INPUT_ACTION);
	}

	public void run() {
		IJavaScriptElement inputElement= fInfoView.getInput();
		new OpenAction(fInfoView.getViewSite()).run(new Object[] { inputElement });
	}
}
