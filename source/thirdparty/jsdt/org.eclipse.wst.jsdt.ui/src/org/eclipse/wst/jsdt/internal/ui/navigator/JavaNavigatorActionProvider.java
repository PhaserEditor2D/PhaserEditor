/*******************************************************************************
 * Copyright (c) 2003, 2008 IBM Corporation and others.
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
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;
import org.eclipse.ui.navigator.ICommonViewerWorkbenchSite;
import org.eclipse.wst.jsdt.ui.actions.CCPActionGroup;
import org.eclipse.wst.jsdt.ui.actions.GenerateActionGroup;
import org.eclipse.wst.jsdt.ui.actions.JavaSearchActionGroup;
import org.eclipse.wst.jsdt.ui.actions.OpenViewActionGroup;

/**
 * Contributes the following actions to the menu on behalf of the JDT content
 * extension.
 * 
 * <ul>
 * <li>{@link OpenViewActionGroup}. Contributes the Open View actions (type
 * hierarchy, call hierarchy, etc).</li>
 * <li>{@link CCPActionGroup}. Contributes the Cut/Copy/Paste/Delete.</li>
 * <li>{@link JavaSearchActionGroup}. Contributes the search functionality (find in workspace, project, etc).</li>
 * <li>{@link GenerateActionGroup}. Contributes the "Source>" and "Generate>" submenu .</li>
 * </ul>
 */
public class JavaNavigatorActionProvider extends CommonActionProvider {

	private OpenViewActionGroup fOpenViewGroup;

	private CCPActionGroup fCCPGroup;

	private JavaSearchActionGroup fSearchGroup;

	//private GenerateBuildPathActionGroup fBuildPathGroup;

	private GenerateActionGroup fGenerateGroup;

	private boolean fInViewPart= false;

	public void fillActionBars(IActionBars actionBars) {
		if (fInViewPart) {
			fOpenViewGroup.fillActionBars(actionBars);
			fCCPGroup.fillActionBars(actionBars);
		//	fBuildPathGroup.fillActionBars(actionBars);
			fGenerateGroup.fillActionBars(actionBars);
			fSearchGroup.fillActionBars(actionBars);
		}
	}

	public void fillContextMenu(IMenuManager menu) {
		if (fInViewPart) {
			 ISelection selection = (this.getContext()!=null) ? this.getContext().getSelection() : null;
			 boolean isEmpty = (selection!=null)? selection.isEmpty() : false;
			 fOpenViewGroup.containsShowInMenu(!isEmpty);
			fOpenViewGroup.fillContextMenu(menu);
			fCCPGroup.setShowLimited(isEmpty);
			fCCPGroup.fillContextMenu(menu);
		//	fBuildPathGroup.fillContextMenu(menu);
			fGenerateGroup.fillContextMenu(menu);
			fSearchGroup.fillContextMenu(menu);
		}
	}

	public void init(ICommonActionExtensionSite site) {

		ICommonViewerWorkbenchSite workbenchSite= null;
		if (site.getViewSite() instanceof ICommonViewerWorkbenchSite)
			workbenchSite= (ICommonViewerWorkbenchSite) site.getViewSite();

		if (workbenchSite != null) {
			if (workbenchSite.getPart() != null && workbenchSite.getPart() instanceof IViewPart) {
				IViewPart viewPart= (IViewPart) workbenchSite.getPart();

				fOpenViewGroup= new OpenViewActionGroup(viewPart, site.getStructuredViewer());
				fOpenViewGroup.containsOpenPropertiesAction(false);
				fCCPGroup= new CCPActionGroup(viewPart);
				fGenerateGroup= new GenerateActionGroup(viewPart);
				fSearchGroup= new JavaSearchActionGroup(viewPart);
				//fBuildPathGroup= new GenerateBuildPathActionGroup(viewPart);

				fInViewPart= true;
			}
		}
	}

	public void setContext(ActionContext context) {
		super.setContext(context);
		if (fInViewPart) {
			fOpenViewGroup.setContext(context);
			fCCPGroup.setContext(context);
			fGenerateGroup.setContext(context);
			fSearchGroup.setContext(context);
			//fBuildPathGroup.setContext(context);
		}
	}
}
