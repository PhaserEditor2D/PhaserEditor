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
package org.eclipse.wst.jsdt.internal.ui.compare;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.CompilationUnitEditor;

public class LocalHistoryActionGroup extends ActionGroup {

	private String fGroupName;

	private JavaHistoryAction fCompareWith;
	private JavaHistoryAction fReplaceWithPrevious;
	private JavaHistoryAction fReplaceWith;
	private JavaHistoryAction fAddFrom;
	
	public LocalHistoryActionGroup(CompilationUnitEditor editor, String groupName) {
		Assert.isNotNull(groupName);
		fGroupName= groupName;
		fCompareWith= new JavaCompareWithEditionAction();
		fCompareWith.init(editor,
			CompareMessages.LocalHistoryActionGroup_action_compare_with, 
			CompareMessages.LocalHistoryActionGroup_action_compare_with_title, 
			CompareMessages.LocalHistoryActionGroup_action_compare_with_message); 
		PlatformUI.getWorkbench().getHelpSystem().setHelp(fCompareWith, IJavaHelpContextIds.COMPARE_WITH_HISTORY_ACTION);

		fReplaceWithPrevious= new JavaReplaceWithPreviousEditionAction();
		fReplaceWithPrevious.init(editor, 
			CompareMessages.LocalHistoryActionGroup_action_replace_with_previous, 
			CompareMessages.LocalHistoryActionGroup_action_replace_with_previous_title, 
			CompareMessages.LocalHistoryActionGroup_action_replace_with_previous_message); 
		PlatformUI.getWorkbench().getHelpSystem().setHelp(fReplaceWithPrevious, IJavaHelpContextIds.REPLACE_WITH_PREVIOUS_FROM_HISTORY_ACTION);
		
		fReplaceWith= new JavaReplaceWithEditionAction();
		fReplaceWith.init(editor,
			CompareMessages.LocalHistoryActionGroup_action_replace_with, 
			CompareMessages.LocalHistoryActionGroup_action_replace_with_title, 
			CompareMessages.LocalHistoryActionGroup_action_replace_with_message); 
		PlatformUI.getWorkbench().getHelpSystem().setHelp(fReplaceWith, IJavaHelpContextIds.REPLACE_WITH_HISTORY_ACTION);

		fAddFrom= new JavaAddElementFromHistory();
		fAddFrom.init(editor, 
			CompareMessages.LocalHistoryActionGroup_action_add, 
			CompareMessages.LocalHistoryActionGroup_action_add_title, 
			CompareMessages.LocalHistoryActionGroup_action_add_message); 
		PlatformUI.getWorkbench().getHelpSystem().setHelp(fAddFrom, IJavaHelpContextIds.ADD_FROM_HISTORY_ACTION);
	}

	public void fillContextMenu(IMenuManager menu) {
		super.fillContextMenu(menu);
		IMenuManager localMenu= new MenuManager(CompareMessages.LocalHistoryActionGroup_menu_local_history); 
		int added= 0;
		added+= addAction(localMenu, fCompareWith);
		added+= addAction(localMenu, fReplaceWithPrevious);
		added+= addAction(localMenu, fReplaceWith);
		added+= addAction(localMenu, fAddFrom);
		if (added > 0)
			menu.appendToGroup(fGroupName, localMenu);
	}
	
	private int addAction(IMenuManager menu, JavaHistoryAction action) {
		action.update();
		if (action.isEnabled()) {
			menu.add(action);
			return 1;
		}
		return 0;
	}
}
