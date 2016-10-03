/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Alex Blewitt - Bug 133277 Allow Sort Members to be performed on package and project levels
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.actions;

import java.util.Hashtable;

import org.eclipse.jface.window.Window;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IParent;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.dialogs.SortMembersMessageDialog;
import org.eclipse.wst.jsdt.internal.ui.fix.ICleanUp;
import org.eclipse.wst.jsdt.internal.ui.fix.SortMembersCleanUp;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.JavaEditor;

public class MultiSortMembersAction extends CleanUpAction {

	public MultiSortMembersAction(IWorkbenchSite site) {
		super(site);
		
		setText(ActionMessages.SortMembersAction_label); 
		setDescription(ActionMessages.SortMembersAction_description); 
		setToolTipText(ActionMessages.SortMembersAction_tooltip); 
		
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.SORT_MEMBERS_ACTION);
	}

	public MultiSortMembersAction(JavaEditor editor) {
		super(editor);
		
		setText(ActionMessages.SortMembersAction_label); 
		setDescription(ActionMessages.SortMembersAction_description); 
		setToolTipText(ActionMessages.SortMembersAction_tooltip); 
		
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.SORT_MEMBERS_ACTION);
	}

	/**
	 * {@inheritDoc}
	 */
	protected ICleanUp[] createCleanUps(IJavaScriptUnit[] units) {
		try {
	        if (!hasMembersToSort(units))
	        	return null;
        } catch (JavaScriptModelException e) {
        	JavaScriptPlugin.log(e);
	        return null;
        }

		SortMembersMessageDialog dialog= new SortMembersMessageDialog(getShell());
		if (dialog.open() != Window.OK)
			return null;

		Hashtable settings= new Hashtable();
		settings.put(CleanUpConstants.SORT_MEMBERS, CleanUpConstants.TRUE);
		settings.put(CleanUpConstants.SORT_MEMBERS_ALL, !dialog.isNotSortingFieldsEnabled() ? CleanUpConstants.TRUE : CleanUpConstants.FALSE);

		return new ICleanUp[] {
			new SortMembersCleanUp(settings)
		};
	}

	/**
	 * {@inheritDoc}
	 */
	protected String getActionName() {
		return ActionMessages.SortMembersAction_dialog_title; 
	}

	private boolean hasMembersToSort(IJavaScriptUnit[] units) throws JavaScriptModelException {
		for (int i= 0; i < units.length; i++) {
			if (hasMembersToSort(units[i].getChildren()))
				return true;
		}

		return false;
	}

	private boolean hasMembersToSort(IJavaScriptElement[] members) throws JavaScriptModelException {
		if (members.length > 1)
			return true;

		if (members.length == 0)
			return false;

		IJavaScriptElement elem= members[0];
		if (!(elem instanceof IParent))
			return false;

		return hasMembersToSort(((IParent)elem).getChildren());
	}

}
