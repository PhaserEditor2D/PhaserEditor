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
package org.eclipse.wst.jsdt.internal.ui.workingsets;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.wst.jsdt.ui.actions.SelectionDispatchAction;

public class RemoveWorkingSetElementAction extends SelectionDispatchAction {

	public RemoveWorkingSetElementAction(IWorkbenchSite site) {
		super(site);
		setText(WorkingSetMessages.RemoveWorkingSetElementAction_label); 
	}
	
	public void selectionChanged(IStructuredSelection selection) {
		IWorkingSet workingSet= getWorkingSet(selection);
		setEnabled(workingSet != null && !OthersWorkingSetUpdater.ID.equals(workingSet.getId()));
	}

	private IWorkingSet getWorkingSet(IStructuredSelection selection) {
		if (!(selection instanceof ITreeSelection))
			return null;
		ITreeSelection treeSelection= (ITreeSelection)selection;
		List elements= treeSelection.toList();
		IWorkingSet result= null;
		for (Iterator iter= elements.iterator(); iter.hasNext();) {
			Object element= iter.next();
			TreePath[] paths= treeSelection.getPathsFor(element);
			if (paths.length != 1)
				return null;
			TreePath path= paths[0];
			if (path.getSegmentCount() != 2)
				return null;
			Object candidate= path.getSegment(0);
			if (!(candidate instanceof IWorkingSet))
				return null;
			if (result == null) {
				result= (IWorkingSet)candidate;
			} else {
				if (result != candidate)
					return null;
			}
		}
		return result;
	}
	
	public void run(IStructuredSelection selection) {
		IWorkingSet ws= getWorkingSet(selection);
		if (ws == null)
			return;
		HashSet elements= new HashSet(Arrays.asList(ws.getElements()));
		List selectedElements= selection.toList();
		for (Iterator iter= selectedElements.iterator(); iter.hasNext();) {
			Object object= iter.next();
			if (object instanceof IAdaptable) {
				IAdaptable[] adaptedElements= ws.adaptElements(new IAdaptable[] {(IAdaptable)object});
				if (adaptedElements.length == 1) {					
					elements.remove(adaptedElements[0]);
				}
			}
		}
		ws.setElements((IAdaptable[])elements.toArray(new IAdaptable[elements.size()]));
	}
}
