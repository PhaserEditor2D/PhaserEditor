/*******************************************************************************
 * Copyright (c) 2003, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.navigator;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionConstants;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;
import org.eclipse.ui.navigator.ICommonViewerWorkbenchSite;
import org.eclipse.wst.jsdt.ui.actions.OpenAction;
import org.eclipse.wst.jsdt.ui.actions.OpenEditorActionGroup;


public class PackageExplorerOpenActionProvider extends CommonActionProvider {


	private IAction fOpenAndExpand;
	private OpenEditorActionGroup fOpenGroup;

	private boolean fInViewPart = false;

	public void fillActionBars(IActionBars actionBars) {
		if (fInViewPart) {
			fOpenGroup.fillActionBars(actionBars);
			
			if (fOpenAndExpand == null) // TODO: is not updated!
				actionBars.setGlobalActionHandler(ICommonActionConstants.OPEN, fOpenGroup.getOpenAction());
			else if (fOpenAndExpand != null)
				actionBars.setGlobalActionHandler(ICommonActionConstants.OPEN, fOpenAndExpand);
		}

	}

	public void fillContextMenu(IMenuManager menu) {

		if (fInViewPart) {
			if (fOpenGroup.getOpenAction().isEnabled()) {
				fOpenGroup.fillContextMenu(menu);
			}
		}
	}

	public void init(ICommonActionExtensionSite site) {

		ICommonViewerWorkbenchSite workbenchSite = null;
		if (site.getViewSite() instanceof ICommonViewerWorkbenchSite)
			workbenchSite = (ICommonViewerWorkbenchSite) site.getViewSite();

		if (workbenchSite != null) {
			if (workbenchSite.getPart() != null && workbenchSite.getPart() instanceof IViewPart) {
				IViewPart viewPart = (IViewPart) workbenchSite.getPart();

				fOpenGroup = new OpenEditorActionGroup(viewPart);

				if (site.getStructuredViewer() instanceof TreeViewer)
				{
					fOpenAndExpand = new OpenAndExpand(workbenchSite.getSite(), (OpenAction) fOpenGroup.getOpenAction(), (TreeViewer) site.getStructuredViewer());
					fOpenAndExpand.setActionDefinitionId( "org.eclipse.wst.jsdt.ui.edit.text.java.openandexpand" ); // Make this different from F3 file open handler to avoid handler conflicts
				}
				fInViewPart = true;
			}
		}
	}

	public void setContext(ActionContext context) {
		super.setContext(context);
		if (fInViewPart) {
			fOpenGroup.setContext(context);
		}
	}

	/*
	 * @see org.eclipse.ui.actions.ActionGroup#dispose()
	 * @since 3.5
	 */
	public void dispose() {
		if (fOpenGroup != null)
			fOpenGroup.dispose();
		super.dispose();
	}

}
