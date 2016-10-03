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
package org.eclipse.wst.jsdt.ui.actions;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.actions.ExportResourcesAction;
import org.eclipse.ui.actions.ImportResourcesAction;
import org.eclipse.wst.jsdt.ui.IContextMenuConstants;

/**
 * Action group to add the Import and Export action to a view part's
 * context menu.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 *
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 */
public class ImportActionGroup extends ActionGroup {

	private static final String GROUP_IMPORT= "group.import"; //$NON-NLS-1$
	
	private ImportResourcesAction fImportAction;
	private ExportResourcesAction fExportAction;

	/**
	 * Creates a new <code>ImportActionGroup</code>. The group 
	 * requires that the selection provided by the part's selection provider 
	 * is of type <code>org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param part the view part that owns this action group
	 */
	public ImportActionGroup(IViewPart part) {
		IWorkbenchWindow workbenchWindow = part.getSite().getWorkbenchWindow();
		fImportAction= new ImportResourcesAction(workbenchWindow);
		fExportAction= new ExportResourcesAction(workbenchWindow);			
	}
	
	/* (non-Javadoc)
	 * Method declared in ActionGroup
	 */
	public void fillContextMenu(IMenuManager menu) {
		menu.appendToGroup(IContextMenuConstants.GROUP_REORGANIZE, new Separator(GROUP_IMPORT));
		menu.appendToGroup(GROUP_IMPORT, fImportAction);
		menu.appendToGroup(GROUP_IMPORT, fExportAction);
		super.fillContextMenu(menu);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void dispose() {
		fImportAction.dispose();
		fExportAction.dispose();
		super.dispose();
	}
}
