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
package org.eclipse.wst.jsdt.internal.ui.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;

public class LRUWorkingSetsList {

	private final ArrayList fLRUList;
	private final int fSize;
	private final  WorkingSetsComparator fComparator= new WorkingSetsComparator();
	
	public LRUWorkingSetsList(int size) {
		fSize= size;
		fLRUList= new ArrayList(size);
	}
	
	public void add(IWorkingSet[] workingSets) {
		removeDeletedWorkingSets();
		IWorkingSet[] existingWorkingSets= find(fLRUList, workingSets);
		if (existingWorkingSets != null)
			fLRUList.remove(existingWorkingSets);
		else if (fLRUList.size() == fSize)
			fLRUList.remove(fSize - 1);
		fLRUList.add(0, workingSets);

	}
	
	public Iterator iterator() {
		removeDeletedWorkingSets();
		return fLRUList.iterator();	
	}

	public Iterator sortedIterator() {
		removeDeletedWorkingSets();
		ArrayList sortedList= new ArrayList(fLRUList);
		Collections.sort(sortedList, fComparator);
		return sortedList.iterator();	
	}
	
	private void removeDeletedWorkingSets() {
		Iterator iter= new ArrayList(fLRUList).iterator();
		while (iter.hasNext()) {
			IWorkingSet[] workingSets= (IWorkingSet[])iter.next();
			for (int i= 0; i < workingSets.length; i++) {
				if (PlatformUI.getWorkbench().getWorkingSetManager().getWorkingSet(workingSets[i].getName()) == null) {
					fLRUList.remove(workingSets);
					break;
				}
			}
		}
	}
	
	private IWorkingSet[] find(ArrayList list, IWorkingSet[] workingSets) {
		Set workingSetList= new HashSet(Arrays.asList(workingSets));
		Iterator iter= list.iterator();
		while (iter.hasNext()) {
			IWorkingSet[] lruWorkingSets= (IWorkingSet[])iter.next();
			Set lruWorkingSetList= new HashSet(Arrays.asList(lruWorkingSets));
			if (lruWorkingSetList.equals(workingSetList))
				return lruWorkingSets;
		}
		return null;
	}
}
