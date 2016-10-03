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
package org.eclipse.wst.jsdt.internal.ui.viewsupport;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.widgets.Control;
import org.eclipse.wst.jsdt.core.JavaScriptCore;


public class FilterUpdater implements IResourceChangeListener {

	private ProblemTreeViewer fViewer;
	
	public FilterUpdater(ProblemTreeViewer viewer) {
		Assert.isNotNull(viewer);
		fViewer= viewer;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.IResourceChangeListener#resourceChanged(org.eclipse.core.resources.IResourceChangeEvent)
	 */
	public void resourceChanged(IResourceChangeEvent event) {
		if (fViewer.getInput() == null) {
			return;
		}
		IResourceDelta delta= event.getDelta();
		if (delta == null)
			return;
		IResourceDelta[] projDeltas = delta.getAffectedChildren(IResourceDelta.CHANGED);
		for (int i= 0; i < projDeltas.length; i++) {
			IResourceDelta pDelta= projDeltas[i];
			if ((pDelta.getFlags() & IResourceDelta.DESCRIPTION) != 0) {
				IProject project= (IProject) pDelta.getResource();
				if (needsRefiltering(project)) {
					final Control ctrl= fViewer.getControl();
					if (ctrl != null && !ctrl.isDisposed()) {
						// async is needed due to bug 33783
						ctrl.getDisplay().asyncExec(new Runnable() {
							public void run() {
								if (!ctrl.isDisposed())
									fViewer.refresh(false);
							}
						});
					}
					return; // one refresh is good enough
				}
			}
		}
	}
	
	private boolean needsRefiltering(IProject project) {
		try {
			Object element= project;
			if (project.hasNature(JavaScriptCore.NATURE_ID)) {
				element= JavaScriptCore.create(project);
			}
			boolean inView= fViewer.testFindItem(element) != null;
			boolean afterFilter= !fViewer.isFiltered(element, fViewer.getInput());
			
			return inView != afterFilter;
		} catch (CoreException e) {
			return true;
		}
	}
}
