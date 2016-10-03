/*******************************************************************************
 * Copyright (c) 2003, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.navigator;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;
import org.eclipse.ui.navigator.ICommonViewerWorkbenchSite;
import org.eclipse.wst.jsdt.ui.actions.RefactorActionGroup;

/**
 * Contributes the following actions to the menu on behalf of the JDT content
 * extension.
 * 
 * <ul>
 * <li>{@link RefactorActionGroup}. Contributes the "Refactor>" and "Source>" submenus to the context menu.</li>
 * </ul>
 */
public class JavaNavigatorRefactorActionProvider extends CommonActionProvider {

	private RefactorActionGroup fRefactorGroup;

	public void fillActionBars(IActionBars actionBars) {
		if (fRefactorGroup != null) {
			fRefactorGroup.fillActionBars(actionBars);
			fRefactorGroup.retargetFileMenuActions(actionBars);
		}
	}

	public void fillContextMenu(IMenuManager menu) {
		if (fRefactorGroup != null) {
			fRefactorGroup.fillContextMenu(menu);
		}
	}

	public void init(ICommonActionExtensionSite site) {
		ICommonViewerWorkbenchSite workbenchSite= null;
		if (site.getViewSite() instanceof ICommonViewerWorkbenchSite)
			workbenchSite= (ICommonViewerWorkbenchSite) site.getViewSite();

		// we only initialize the refactor group when in a view part 
		// (required for the constructor)
		if (workbenchSite != null) {
			if (workbenchSite.getPart() != null && workbenchSite.getPart() instanceof IViewPart) {
				IViewPart viewPart= (IViewPart) workbenchSite.getPart();

				fRefactorGroup= new RefactorActionGroup(viewPart);
			}
		}
	}

	public void setContext(ActionContext context) {
		if (fRefactorGroup != null) {
			fRefactorGroup.setContext(context);
		}
	}
}
