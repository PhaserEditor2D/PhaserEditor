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
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IPartService;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowPulldownDelegate2;
import org.eclipse.ui.actions.RetargetAction;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.wst.jsdt.internal.ui.search.SearchMessages;
import org.eclipse.wst.jsdt.ui.actions.IJavaEditorActionDefinitionIds;
import org.eclipse.wst.jsdt.ui.actions.JdtActionConstants;

/**
 * <p>
 * This is required because of 
 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=79162
 * and
 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=137679
 * </p>
 */
public class OccurrencesSearchMenuAction implements IWorkbenchWindowPulldownDelegate2 {
	
	private static Action NO_ACTION_AVAILABLE= new Action(SearchMessages.group_occurrences_quickMenu_noEntriesAvailable) {
		public boolean isEnabled() {
			return false;
		}
	};

	private Menu fMenu;

	private IPartService fPartService;
	private RetargetAction[] fRetargetActions;
	
	/**
	 * {@inheritDoc}
	 */
	public Menu getMenu(Menu parent) {
		setMenu(new Menu(parent));
		fillMenu(fMenu);
		return fMenu;
	}

	/**
	 * {@inheritDoc}
	 */
	public Menu getMenu(Control parent) {
		setMenu(new Menu(parent));
		fillMenu(fMenu);
		return fMenu;
	}

	/**
	 * {@inheritDoc}
	 */
	public void dispose() {
		setMenu(null);
		disposeSubmenuActions();
	}

	private RetargetAction createSubmenuAction(IPartService partService, String actionID, String text, String actionDefinitionId) {
		RetargetAction action= new RetargetAction(actionID, text);
		action.setActionDefinitionId(actionDefinitionId);

		partService.addPartListener(action);
		IWorkbenchPart activePart = partService.getActivePart();
		if (activePart != null) {
			action.partActivated(activePart);
		}
		return action;
	}
	
	private void disposeSubmenuActions() {
		if (fPartService != null && fRetargetActions != null) {
			for (int i= 0; i < fRetargetActions.length; i++) {
				fPartService.removePartListener(fRetargetActions[i]);
				fRetargetActions[i].dispose();
			}
		}
		fRetargetActions= null;
		fPartService= null;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void init(IWorkbenchWindow window) {
		disposeSubmenuActions(); // paranoia code: double initialization should not happen
		if (window != null) {
			fPartService= window.getPartService();
			if (fPartService != null) {
				fRetargetActions= new RetargetAction[] {
					createSubmenuAction(fPartService, JdtActionConstants.FIND_OCCURRENCES_IN_FILE, SearchMessages.Search_FindOccurrencesInFile_shortLabel, IJavaEditorActionDefinitionIds.SEARCH_OCCURRENCES_IN_FILE),
//					createSubmenuAction(fPartService, JdtActionConstants.FIND_IMPLEMENT_OCCURRENCES, ActionMessages.FindImplementOccurrencesAction_text, IJavaEditorActionDefinitionIds.SEARCH_IMPLEMENT_OCCURRENCES_IN_FILE),
//					createSubmenuAction(fPartService, JdtActionConstants.FIND_EXCEPTION_OCCURRENCES, ActionMessages.FindExceptionOccurrences_text, IJavaEditorActionDefinitionIds.SEARCH_EXCEPTION_OCCURRENCES_IN_FILE),
				};
			}	
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void run(IAction action) {
		JavaEditor editor= null;
		IWorkbenchPart activePart= JavaScriptPlugin.getActivePage().getActivePart();
		if (activePart instanceof JavaEditor)
			editor= (JavaEditor) activePart;
		
		(new JDTQuickMenuAction(editor, IJavaEditorActionDefinitionIds.SEARCH_OCCURRENCES_IN_FILE_QUICK_MENU) {
			protected void fillMenu(IMenuManager menu) {
				fillQuickMenu(menu);
			}
		}).run();

	}

	/**
	 * {@inheritDoc}
	 */
	public void selectionChanged(IAction action, ISelection selection) {
	}
	
	private void fillQuickMenu(IMenuManager manager) {
		IAction[] actions= fRetargetActions;
		if (actions != null) {
			boolean hasAction= false;
			for (int i= 0; i < actions.length; i++) {
				IAction action= actions[i];
				if (action.isEnabled()) {
					hasAction= true;
					manager.add(action);
				}
			}
			if (!hasAction) {
				manager.add(NO_ACTION_AVAILABLE);
			}
		} else {
			manager.add(NO_ACTION_AVAILABLE);
		}
	}
	
	/**
	 * The menu to show in the workbench menu
	 */
	private void fillMenu(Menu menu) {
		if (fRetargetActions != null) {
			for (int i= 0; i < fRetargetActions.length; i++) {
				ActionContributionItem item= new ActionContributionItem(fRetargetActions[i]);
				item.fill(menu, -1);
			}
		} else {
			// can only happen if 'init' was not called: programming error
			ActionContributionItem item= new ActionContributionItem(NO_ACTION_AVAILABLE);
			item.fill(menu, -1);
		}
	}
	
	private void setMenu(Menu menu) {
		if (fMenu != null) {
			fMenu.dispose();
		}
		fMenu = menu;
	}
}
