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
package org.eclipse.wst.jsdt.internal.ui.workingsets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchPreferenceConstants;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.wst.jsdt.internal.ui.search.WorkingSetComparator;

/**
 * Working set filter actions (set / clear)
 * 
 * 
 * 
 */
public class WorkingSetFilterActionGroup extends ActionGroup implements IWorkingSetActionGroup {

	private static final String TAG_WORKING_SET_NAME= "workingSetName"; //$NON-NLS-1$
	private static final String TAG_IS_WINDOW_WORKING_SET= "isWindowWorkingSet";  //$NON-NLS-1$
	private static final String LRU_GROUP= "workingSet_lru_group"; //$NON-NLS-1$

	private final WorkingSetFilter fWorkingSetFilter;
	
	private IWorkingSet fWorkingSet= null;
	
	private final ClearWorkingSetAction fClearWorkingSetAction;
	private final SelectWorkingSetAction fSelectWorkingSetAction;
	private final EditWorkingSetAction fEditWorkingSetAction;
	
	private IPropertyChangeListener fWorkingSetListener;
	private IPropertyChangeListener fChangeListener;
	
	private int fLRUMenuCount;
	private IMenuManager fMenuManager;
	private IMenuListener fMenuListener;
	private List fContributions= new ArrayList();
	private final IWorkbenchPage fWorkbenchPage;
	private boolean fAllowWindowWorkingSetByDefault;

	public WorkingSetFilterActionGroup(IWorkbenchPartSite site, IPropertyChangeListener changeListener) {
		Assert.isNotNull(site);
		Assert.isNotNull(changeListener);

		fChangeListener= changeListener;
		fWorkbenchPage= site.getPage();
		fAllowWindowWorkingSetByDefault= true;
		fClearWorkingSetAction= new ClearWorkingSetAction(this);
		fSelectWorkingSetAction= new SelectWorkingSetAction(this, site);
		fEditWorkingSetAction= new EditWorkingSetAction(this, site);

		fWorkingSetListener= new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				doPropertyChange(event);
			}
		};
		fWorkingSetFilter= new WorkingSetFilter();

		IWorkingSetManager manager= PlatformUI.getWorkbench().getWorkingSetManager();
		manager.addPropertyChangeListener(fWorkingSetListener);
		
		if (useWindowWorkingSetByDefault()) {
			setWorkingSet(site.getPage().getAggregateWorkingSet(), false);
		}
	}
	
	public WorkingSetFilterActionGroup(Shell shell, IWorkbenchPage page, IPropertyChangeListener changeListener) {
		Assert.isNotNull(shell);
		Assert.isNotNull(changeListener);

		fWorkbenchPage= page;
		fAllowWindowWorkingSetByDefault= false;
		fChangeListener= changeListener;
		fClearWorkingSetAction= new ClearWorkingSetAction(this);
		fSelectWorkingSetAction= new SelectWorkingSetAction(this, shell);
		fEditWorkingSetAction= new EditWorkingSetAction(this, shell);

		fWorkingSetListener= new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				doPropertyChange(event);
			}
		};

		fWorkingSetFilter= new WorkingSetFilter();

		IWorkingSetManager manager= PlatformUI.getWorkbench().getWorkingSetManager();
		manager.addPropertyChangeListener(fWorkingSetListener);
		
		setWorkingSet(null, false);
	}

	/**
	 * Returns whether the current working set filters the given element
	 * 
	 * @param parent the parent
	 * @param object the element to test
	 * @return the working set
	 */
	public boolean isFiltered(Object parent, Object object) {
	    if (fWorkingSetFilter == null)
	        return false;
	    return !fWorkingSetFilter.select(null, parent, object);
	}
	

	/**
	 * Returns the working set which is used by the filter.
	 * 
	 * @return the working set
	 */
	public IWorkingSet getWorkingSet() {
		return fWorkingSet;
	}
		
	/**
	 * Sets this filter's working set.
	 * 
	 * @param workingSet the working set
	 * @param refreshViewer Indicates if the viewer should be refreshed.
	 */
	public void setWorkingSet(IWorkingSet workingSet, boolean refreshViewer) {
		// Update action
		fClearWorkingSetAction.setEnabled(workingSet != null);
		fEditWorkingSetAction.setEnabled(workingSet != null && !workingSet.isAggregateWorkingSet());

		fWorkingSet= workingSet;

		fWorkingSetFilter.setWorkingSet(workingSet);
		if (refreshViewer) {
			fChangeListener.propertyChange(new PropertyChangeEvent(this, IWorkingSetManager.CHANGE_WORKING_SET_CONTENT_CHANGE, null, workingSet));
		}
	}
	
	/**
	 * Saves the state of the filter actions in a memento.
	 * 
	 * @param memento the memento
	 */
	public void saveState(IMemento memento) {
		String workingSetName= ""; //$NON-NLS-1$
		boolean isWindowWorkingSet= false;
		if (fWorkingSet != null) {
			if (fWorkingSet.isAggregateWorkingSet()) { 
				isWindowWorkingSet= true;
			} else {
				workingSetName= fWorkingSet.getName();
			}
		}
		memento.putString(TAG_IS_WINDOW_WORKING_SET, Boolean.toString(isWindowWorkingSet));
		memento.putString(TAG_WORKING_SET_NAME, workingSetName);
	}

	/**
	 * Restores the state of the filter actions from a memento.
	 * <p>
	 * Note: This method does not refresh the viewer.
	 * </p>
	 * @param memento
	 */	
	public void restoreState(IMemento memento) {
		boolean isWindowWorkingSet;
		if (memento.getString(TAG_IS_WINDOW_WORKING_SET) != null) {
			isWindowWorkingSet= Boolean.valueOf(memento.getString(TAG_IS_WINDOW_WORKING_SET)).booleanValue();
		} else {
			isWindowWorkingSet= useWindowWorkingSetByDefault();
		}
		String workingSetName= memento.getString(TAG_WORKING_SET_NAME);
		boolean hasWorkingSetName= workingSetName != null && workingSetName.length() > 0;
		
		IWorkingSet ws= null;
		// First handle name if present.
		if (hasWorkingSetName) {
			ws= PlatformUI.getWorkbench().getWorkingSetManager().getWorkingSet(workingSetName);
		} else if (isWindowWorkingSet && fWorkbenchPage != null) {
			ws= fWorkbenchPage.getAggregateWorkingSet();
		}
		setWorkingSet(ws, false);
	}

	private boolean useWindowWorkingSetByDefault() {
		return fAllowWindowWorkingSetByDefault && PlatformUI.getPreferenceStore().getBoolean(IWorkbenchPreferenceConstants.USE_WINDOW_WORKING_SET_BY_DEFAULT);
	}
	

	/* (non-Javadoc)
	 * @see ActionGroup#fillActionBars(IActionBars)
	 */
	public void fillActionBars(IActionBars actionBars) {
		fillToolBar(actionBars.getToolBarManager());
		fillViewMenu(actionBars.getMenuManager());
	}
	
	/**
	 * Adds the filter actions to the tool bar
	 * 
	 * @param tbm the tool bar manager
	 */
	private void fillToolBar(IToolBarManager tbm) {
		// do nothing
	}

	/**
	 * Adds the filter actions to the menu
	 * 
	 * @param mm the menu manager
	 */
	public void fillViewMenu(IMenuManager mm) {
		if (mm.find(IWorkingSetActionGroup.ACTION_GROUP) == null) {
			mm.add(new Separator(IWorkingSetActionGroup.ACTION_GROUP));			
		}
		add(mm, fSelectWorkingSetAction);
		add(mm, fClearWorkingSetAction);
		add(mm, fEditWorkingSetAction);
		add(mm, new Separator());
		add(mm, new Separator(LRU_GROUP));

		fMenuManager= mm;
		fMenuListener= new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				removePreviousLRUWorkingSetActions(manager);
				addLRUWorkingSetActions(manager);
			}
		};
		fMenuManager.addMenuListener(fMenuListener);
	}
	
	private void add(IMenuManager mm, IAction action) {
		IContributionItem item= new ActionContributionItem(action);
		mm.appendToGroup(ACTION_GROUP, item);
		fContributions.add(item);
	}
	
	private void add(IMenuManager mm, IContributionItem item) {
		mm.appendToGroup(ACTION_GROUP, item);
		fContributions.add(item);
	}
	
	private void removePreviousLRUWorkingSetActions(IMenuManager mm) {
		for (int i= 1; i < fLRUMenuCount; i++) {
			String id= WorkingSetMenuContributionItem.getId(i);
			IContributionItem item= mm.remove(id);
			if (item != null) {
				item.dispose();
				fContributions.remove(item);
			}
		}
	}

	private void addLRUWorkingSetActions(IMenuManager mm) {
		IWorkingSet[] workingSets= PlatformUI.getWorkbench().getWorkingSetManager().getRecentWorkingSets();
		Arrays.sort(workingSets, new WorkingSetComparator());
		
		int currId= 1;
		if (fWorkbenchPage != null) {
			addLRUWorkingSetAction(mm, currId++, fWorkbenchPage.getAggregateWorkingSet());
		}
		
		for (int i= 0; i < workingSets.length; i++) {
			if (!workingSets[i].isAggregateWorkingSet()) {
				addLRUWorkingSetAction(mm, currId++, workingSets[i]);
			}
		}
		fLRUMenuCount= currId;
	}
	
	private void addLRUWorkingSetAction(IMenuManager mm, int id, IWorkingSet workingSet) {
		IContributionItem item= new WorkingSetMenuContributionItem(id, this, workingSet);
		mm.insertBefore(LRU_GROUP, item);
		fContributions.add(item);
	}
	
	
	public void cleanViewMenu(IMenuManager menuManager) {
		for (Iterator iter= fContributions.iterator(); iter.hasNext();) {
			IContributionItem removed= menuManager.remove((IContributionItem) iter.next());
			if (removed != null) {
				removed.dispose();
			}
		}
		fContributions.clear();
		fMenuManager.removeMenuListener(fMenuListener);
		fMenuListener= null;
	}
	
	/* (non-Javadoc)
	 * @see ActionGroup#dispose()
	 */
	public void dispose() {
		if (fMenuManager != null && fMenuListener != null)
			fMenuManager.removeMenuListener(fMenuListener);
		
		if (fWorkingSetListener != null) {
			PlatformUI.getWorkbench().getWorkingSetManager().removePropertyChangeListener(fWorkingSetListener);
			fWorkingSetListener= null;
		}
		fChangeListener= null; // clear the reference to the viewer
		
		super.dispose();
	}
	
	/**
	 * @return Returns viewer filter always configured with the current working set. 
	 */
	public ViewerFilter getWorkingSetFilter() {
		return fWorkingSetFilter;
	}
		
	/*
	 * Called by the working set change listener
	 */
	private void doPropertyChange(PropertyChangeEvent event) {
		String property= event.getProperty();
		if (IWorkingSetManager.CHANGE_WORKING_SET_NAME_CHANGE.equals(property)) {
			fChangeListener.propertyChange(event);
		} else if  (IWorkingSetManager.CHANGE_WORKING_SET_CONTENT_CHANGE.equals(property)) {
			IWorkingSet newWorkingSet= (IWorkingSet) event.getNewValue();
			if (newWorkingSet.equals(fWorkingSet)) {
				if (fWorkingSetFilter != null) {
					fWorkingSetFilter.notifyWorkingSetContentChange(); // first refresh the filter
				}
				fChangeListener.propertyChange(event);
			}
		}
	}
}
