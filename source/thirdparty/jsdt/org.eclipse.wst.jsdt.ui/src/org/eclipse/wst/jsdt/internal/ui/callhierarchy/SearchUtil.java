/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Jesper Kamstrup Linnet (eclipse@kamstrup-linnet.dk) - initial API and implementation 
 *          (report 36180: Callers/Callees view)
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.callhierarchy;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.search.LRUWorkingSetsList;
import org.eclipse.wst.jsdt.internal.ui.search.WorkingSetComparator;

public class SearchUtil {
	
	// LRU working sets
	public static int LRU_WORKINGSET_LIST_SIZE= 3;
	private static LRUWorkingSetsList fgLRUWorkingSets;
	// Settings store
	private static final String DIALOG_SETTINGS_KEY= "CallHierarchySearchScope"; //$NON-NLS-1$
	private static final String STORE_LRU_WORKING_SET_NAMES= "lastUsedWorkingSetNames"; //$NON-NLS-1$
	private static IDialogSettings fgSettingsStore;
	
	/**
	 * Updates the LRU list of working sets.
	 * 
	 * @param workingSets   the workings sets to be added to the LRU list
	 */
	public static void updateLRUWorkingSets(IWorkingSet[] workingSets) {
		if (workingSets == null || workingSets.length < 1)
			return;
		
		SearchUtil.getLRUWorkingSets().add(workingSets);
		SearchUtil.saveState();
	}
	
	private static void saveState() {
		IWorkingSet[] workingSets;
		Iterator iter= SearchUtil.fgLRUWorkingSets.iterator();
		int i= 0;
		while (iter.hasNext()) {
			workingSets= (IWorkingSet[])iter.next();
			String[] names= new String[workingSets.length];
			for (int j= 0; j < workingSets.length; j++)
				names[j]= workingSets[j].getName();
			SearchUtil.fgSettingsStore.put(SearchUtil.STORE_LRU_WORKING_SET_NAMES + i, names);
			i++;
		}
	}
	
	public static LRUWorkingSetsList getLRUWorkingSets() {
		if (SearchUtil.fgLRUWorkingSets == null) {
			restoreState();
		}
		return SearchUtil.fgLRUWorkingSets;
	}
	
	static void restoreState() {
		SearchUtil.fgLRUWorkingSets= new LRUWorkingSetsList(SearchUtil.LRU_WORKINGSET_LIST_SIZE);
		SearchUtil.fgSettingsStore= JavaScriptPlugin.getDefault().getDialogSettings().getSection(SearchUtil.DIALOG_SETTINGS_KEY);
		if (SearchUtil.fgSettingsStore == null)
			SearchUtil.fgSettingsStore= JavaScriptPlugin.getDefault().getDialogSettings().addNewSection(SearchUtil.DIALOG_SETTINGS_KEY);
		
		boolean foundLRU= false;
		for (int i= SearchUtil.LRU_WORKINGSET_LIST_SIZE - 1; i >= 0; i--) {
			String[] lruWorkingSetNames= SearchUtil.fgSettingsStore.getArray(SearchUtil.STORE_LRU_WORKING_SET_NAMES + i);
			if (lruWorkingSetNames != null) {
				Set workingSets= new HashSet(2);
				for (int j= 0; j < lruWorkingSetNames.length; j++) {
					IWorkingSet workingSet= PlatformUI.getWorkbench().getWorkingSetManager().getWorkingSet(lruWorkingSetNames[j]);
					if (workingSet != null) {
						workingSets.add(workingSet);
					}
				}
				foundLRU= true;
				if (!workingSets.isEmpty())
					SearchUtil.fgLRUWorkingSets.add((IWorkingSet[])workingSets.toArray(new IWorkingSet[workingSets.size()]));
			}
		}
		if (!foundLRU)
			// try old preference format
			restoreFromOldFormat();
	}
	
	private static void restoreFromOldFormat() {
		SearchUtil.fgLRUWorkingSets= new LRUWorkingSetsList(SearchUtil.LRU_WORKINGSET_LIST_SIZE);
		SearchUtil.fgSettingsStore= JavaScriptPlugin.getDefault().getDialogSettings().getSection(SearchUtil.DIALOG_SETTINGS_KEY);
		if (SearchUtil.fgSettingsStore == null)
			SearchUtil.fgSettingsStore= JavaScriptPlugin.getDefault().getDialogSettings().addNewSection(SearchUtil.DIALOG_SETTINGS_KEY);
		
		boolean foundLRU= false;
		String[] lruWorkingSetNames= SearchUtil.fgSettingsStore.getArray(SearchUtil.STORE_LRU_WORKING_SET_NAMES);
		if (lruWorkingSetNames != null) {
			for (int i= lruWorkingSetNames.length - 1; i >= 0; i--) {
				IWorkingSet workingSet= PlatformUI.getWorkbench().getWorkingSetManager().getWorkingSet(lruWorkingSetNames[i]);
				if (workingSet != null) {
					foundLRU= true;
					SearchUtil.fgLRUWorkingSets.add(new IWorkingSet[]{workingSet});
				}
			}
		}
		if (foundLRU)
			// save in new format
			saveState();
	}
	
	public static String toString(IWorkingSet[] workingSets) {
		Arrays.sort(workingSets, new WorkingSetComparator());
		String result= ""; //$NON-NLS-1$
		if (workingSets != null && workingSets.length > 0) {
			boolean firstFound= false;
			for (int i= 0; i < workingSets.length; i++) {
				String workingSetName= workingSets[i].getLabel();
				if (firstFound)
					result= Messages.format(CallHierarchyMessages.SearchUtil_workingSetConcatenation, new String[] {result, workingSetName}); 
				else {
					result= workingSetName;
					firstFound= true;
				}
			}
		}
		return result;
	}
}
