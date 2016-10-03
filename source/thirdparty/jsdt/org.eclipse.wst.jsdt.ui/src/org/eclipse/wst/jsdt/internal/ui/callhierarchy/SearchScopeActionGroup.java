/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Jesper Kamstrup Linnet (eclipse@kamstrup-linnet.dk) - initial API and implementation 
 * 			(report 36180: Callers/Callees view)
 *   Michael Fraenkel (fraenkel@us.ibm.com) - patch
 *          (report 60714: Call Hierarchy: display search scope in view title)
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.callhierarchy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchScope;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.ui.IContextMenuConstants;

class SearchScopeActionGroup extends ActionGroup {
	private static final String TAG_SEARCH_SCOPE_TYPE= "search_scope_type"; //$NON-NLS-1$
	private static final String TAG_SELECTED_WORKING_SET= "working_set"; //$NON-NLS-1$
	private static final String TAG_WORKING_SET_COUNT = "working_set_count"; //$NON-NLS-1$
	
	private static final String DIALOGSTORE_SCOPE_TYPE= "SearchScopeActionGroup.search_scope_type"; //$NON-NLS-1$
	private static final String DIALOGSTORE_SELECTED_WORKING_SET= "SearchScopeActionGroup.working_set";  //$NON-NLS-1$
	
	static final int SEARCH_SCOPE_TYPE_WORKSPACE= 1;
	static final int SEARCH_SCOPE_TYPE_PROJECT= 2;
	static final int SEARCH_SCOPE_TYPE_HIERARCHY= 3;
	static final int SEARCH_SCOPE_TYPE_WORKING_SET= 4;
	
	private SearchScopeAction fSelectedAction = null;
	private String[] fSelectedWorkingSetNames = null;
	private CallHierarchyViewPart fView;
	private IDialogSettings fDialogSettings;
	private SearchScopeHierarchyAction fSearchScopeHierarchyAction;
	private SearchScopeProjectAction fSearchScopeProjectAction;
	private SearchScopeWorkspaceAction fSearchScopeWorkspaceAction;
	private SelectWorkingSetAction fSelectWorkingSetAction;
	
	public SearchScopeActionGroup(CallHierarchyViewPart view, IDialogSettings dialogSettings) {
		this.fView= view;
		this.fDialogSettings= dialogSettings;
		createActions();
	}
	
	/**
	 * @return IJavaScriptSearchScope
	 */
	public IJavaScriptSearchScope getSearchScope() {
		if (fSelectedAction != null) {
			return fSelectedAction.getSearchScope();
		}
		
		return null;
	}
	
	public void fillActionBars(IActionBars actionBars) {
		super.fillActionBars(actionBars);
		fillContextMenu(actionBars.getMenuManager());
	}
	
	protected void setActiveWorkingSets(IWorkingSet[] sets) {
		if (sets != null) {
			fSelectedWorkingSetNames = getWorkingSetNames(sets);
			fSelectedAction = new SearchScopeWorkingSetAction(this, sets, getScopeDescription(sets));
		} else {
			fSelectedWorkingSetNames = null;
			fSelectedAction = null;
		}
	}
	
	private String[] getWorkingSetNames(IWorkingSet[] sets) {
		String[] result= new String[sets.length];
		for (int i = 0; i < sets.length; i++) {
			result[i]= sets[i].getName();
		}
		return result;
	}
	
	protected IWorkingSet[] getActiveWorkingSets() {
		if (fSelectedWorkingSetNames != null) {
			return getWorkingSets(fSelectedWorkingSetNames);
		}
		
		return null;
	}
	
	private IWorkingSet[] getWorkingSets(String[] workingSetNames) {
		if (workingSetNames == null) {
			return null;   
		}
		Set workingSets= new HashSet(2);
		for (int j= 0; j < workingSetNames.length; j++) {
			IWorkingSet workingSet= getWorkingSetManager().getWorkingSet(workingSetNames[j]);
			if (workingSet != null) {
				workingSets.add(workingSet);
			}
		}
		
		return (IWorkingSet[])workingSets.toArray(new IWorkingSet[workingSets.size()]);
	}
	
	/**
	 * Sets the new search scope type.
	 *  
	 * @param newSelection New action which should be the checked one
	 * @param ignoreUnchecked Ignores actions which are unchecked (necessary since both the old and the new action fires).
	 */
	protected void setSelected(SearchScopeAction newSelection, boolean ignoreUnchecked) {
		if (!ignoreUnchecked || newSelection.isChecked()) {
			if (newSelection instanceof SearchScopeWorkingSetAction) {
				fSelectedWorkingSetNames = getWorkingSetNames(((SearchScopeWorkingSetAction) newSelection).getWorkingSets());
			} else {
				fSelectedWorkingSetNames = null;
			}
			
			if (newSelection != null) {
				fSelectedAction= newSelection;
			} else {
				fSelectedAction= fSearchScopeWorkspaceAction;
			}
			
			fDialogSettings.put(DIALOGSTORE_SCOPE_TYPE, getSearchScopeType());
			fDialogSettings.put(DIALOGSTORE_SELECTED_WORKING_SET, fSelectedWorkingSetNames);
		}
	}
	
	protected CallHierarchyViewPart getView() {
		return fView;
	}
	
	protected IWorkingSetManager getWorkingSetManager() {
		IWorkingSetManager workingSetManager = PlatformUI.getWorkbench()
		.getWorkingSetManager();
		
		return workingSetManager;
	}
	
	protected void fillSearchActions(IMenuManager javaSearchMM) {
		Action[] actions = getActions();
		
		for (int i = 0; i < actions.length; i++) {
			Action action = actions[i];
			
			if (action.isEnabled()) {
				javaSearchMM.add(action);
			}
		}
		
		javaSearchMM.setVisible(!javaSearchMM.isEmpty());
	}
	
	public void fillContextMenu(IMenuManager menu) {
		menu.add(new Separator(IContextMenuConstants.GROUP_SEARCH));
		
		MenuManager javaSearchMM = new MenuManager(CallHierarchyMessages.SearchScopeActionGroup_searchScope, 
				IContextMenuConstants.GROUP_SEARCH);
		javaSearchMM.setRemoveAllWhenShown(true);
		
		javaSearchMM.addMenuListener(new IMenuListener() {
			/* (non-Javadoc)
			 * @see org.eclipse.jface.action.IMenuListener#menuAboutToShow(org.eclipse.jface.action.IMenuManager)
			 */
			public void menuAboutToShow(IMenuManager manager) {
				fillSearchActions(manager);
			}
		});
		
		fillSearchActions(javaSearchMM);
		menu.appendToGroup(IContextMenuConstants.GROUP_SEARCH, javaSearchMM);
	}
	
	private Action[] getActions() {
		List actions = new ArrayList(SearchUtil.LRU_WORKINGSET_LIST_SIZE + 4);
		addAction(actions, fSearchScopeWorkspaceAction);
		addAction(actions, fSearchScopeProjectAction);
		addAction(actions, fSearchScopeHierarchyAction);
		addAction(actions, fSelectWorkingSetAction);
		
		Iterator iter= SearchUtil.getLRUWorkingSets().sortedIterator();
		while (iter.hasNext()) {
			IWorkingSet[] workingSets= (IWorkingSet[])iter.next();
			String description = SearchUtil.toString(workingSets);
			SearchScopeWorkingSetAction workingSetAction = new SearchScopeWorkingSetAction(this, workingSets, description);
			
			if (isSelectedWorkingSet(workingSets)) {
				workingSetAction.setChecked(true);
			}
			
			actions.add(workingSetAction);
		}
		
		Action[] result = (Action[]) actions.toArray(new Action[actions.size()]);
		
		ensureExactlyOneCheckedAction(result);
		return result;
	}
	
	private void ensureExactlyOneCheckedAction(Action[] result) {
		int checked = getCheckedActionCount(result);
		if (checked != 1) {
			if (checked > 1) {
				for (int i = 0; i < result.length; i++) {
					Action action = result[i];
					action.setChecked(false);
				}
			}
			fSearchScopeWorkspaceAction.setChecked(true);
		}
	}
	
	private int getCheckedActionCount(Action[] result) {
		// Ensure that exactly one action is selected
		int checked= 0;
		for (int i = 0; i < result.length; i++) {
			Action action = result[i];
			if (action.isChecked()) {
				checked++;
			}
		}
		return checked;
	}
	
	private void addAction(List actions, Action action) {
		if (action == fSelectedAction) {
			action.setChecked(true);
		} else {
			action.setChecked(false);
		}
		
		actions.add(action);
	}
	
	private void createActions() {
		fSearchScopeWorkspaceAction = new SearchScopeWorkspaceAction(this);
		fSelectWorkingSetAction = new SelectWorkingSetAction(this);
		fSearchScopeHierarchyAction = new SearchScopeHierarchyAction(this);
		fSearchScopeProjectAction = new SearchScopeProjectAction(this);
		
		int searchScopeType;
		try {
			searchScopeType= fDialogSettings.getInt(DIALOGSTORE_SCOPE_TYPE);
		} catch (NumberFormatException e) {
			searchScopeType= SEARCH_SCOPE_TYPE_WORKSPACE;
		}
		String[] workingSetNames= fDialogSettings.getArray(DIALOGSTORE_SELECTED_WORKING_SET);
		setSelected(getSearchScopeAction(searchScopeType, workingSetNames), false);
	}
	
	public void saveState(IMemento memento) {
		int type= getSearchScopeType();
		memento.putInteger(TAG_SEARCH_SCOPE_TYPE, type);
		if (type == SEARCH_SCOPE_TYPE_WORKING_SET) {
			memento.putInteger(TAG_WORKING_SET_COUNT, fSelectedWorkingSetNames.length);
			for (int i = 0; i < fSelectedWorkingSetNames.length; i++) {
				String workingSetName = fSelectedWorkingSetNames[i];
				memento.putString(TAG_SELECTED_WORKING_SET+i, workingSetName);
			}
		}
	}
	
	public void restoreState(IMemento memento) {
		String[] workingSetNames= null;
		Integer scopeType= memento.getInteger(TAG_SEARCH_SCOPE_TYPE);
		if (scopeType != null) {
			if (scopeType.intValue() == SEARCH_SCOPE_TYPE_WORKING_SET) {
				Integer workingSetCount= memento.getInteger(TAG_WORKING_SET_COUNT);
				if (workingSetCount != null) {
					workingSetNames = new String[workingSetCount.intValue()];
					for (int i = 0; i < workingSetCount.intValue(); i++) {
						workingSetNames[i]= memento.getString(TAG_SELECTED_WORKING_SET+i);
					}   
				}
			}
			setSelected(getSearchScopeAction(scopeType.intValue(), workingSetNames), false);
		}
	}
	
	private SearchScopeAction getSearchScopeAction(int searchScopeType, String[] workingSetNames) {
		switch (searchScopeType) {
			case SEARCH_SCOPE_TYPE_WORKSPACE: 
				return fSearchScopeWorkspaceAction;
			case SEARCH_SCOPE_TYPE_PROJECT: 
				return fSearchScopeProjectAction;
			case SEARCH_SCOPE_TYPE_HIERARCHY: 
				return fSearchScopeHierarchyAction;
			case SEARCH_SCOPE_TYPE_WORKING_SET:
				IWorkingSet[] workingSets= getWorkingSets(workingSetNames);
				if (workingSets != null && workingSets.length > 0) {
					return new SearchScopeWorkingSetAction(this, workingSets, getScopeDescription(workingSets));
				}
				return null;
		}
		return null;
	}
	
	private int getSearchScopeType() {
		if (fSelectedAction != null) {
			return fSelectedAction.getSearchScopeType();
		}
		return 0;
	}
	
	private String getScopeDescription(IWorkingSet[] workingSets) {
		return Messages.format(CallHierarchyMessages.WorkingSetScope, new String[] {SearchUtil.toString(workingSets)}); 
	}
	
	/**
	 * Determines whether the specified working sets correspond to the currently selected working sets.
	 * @param workingSets
	 * @return Returns true if the specified working sets correspond to the currently selected working sets
	 */
	private boolean isSelectedWorkingSet(IWorkingSet[] workingSets) {
		if (fSelectedWorkingSetNames != null && fSelectedWorkingSetNames.length == workingSets.length) {
			Set workingSetNames= new HashSet(workingSets.length);
			for (int i = 0; i < workingSets.length; i++) {
				workingSetNames.add(workingSets[i].getName());
			}
			for (int i = 0; i < fSelectedWorkingSetNames.length; i++) {
				if (!workingSetNames.contains(fSelectedWorkingSetNames[i])) {
					return false;
				}
			}
			return true;
		}
		return false;
	}
	
	public String getFullDescription() {
		if (fSelectedAction != null)
			return fSelectedAction.getFullDescription();
		return null;
	}
}

