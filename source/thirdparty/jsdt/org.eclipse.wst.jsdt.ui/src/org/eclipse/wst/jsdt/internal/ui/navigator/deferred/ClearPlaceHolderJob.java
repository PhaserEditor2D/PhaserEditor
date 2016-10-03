/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.navigator.deferred;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.wst.jsdt.internal.ui.packageview.PackagesMessages;

public class ClearPlaceHolderJob extends UIJob {

	private AbstractTreeViewer viewer;
	private LoadingModelNode placeHolder;
	private Object[] children;
	private Object parent;

	public ClearPlaceHolderJob(AbstractTreeViewer viewer, LoadingModelNode placeHolder, Object parent, Object[] children) {
		super(PackagesMessages.UpdatingViewer);
		this.viewer = viewer;
		this.placeHolder = placeHolder; 
		this.parent = parent;
		this.children = children;
		setRule(new NonConflictingRule());
	}
	
	public IStatus runInUIThread(IProgressMonitor monitor) { 
		if (!viewer.getControl().isDisposed()) {
			try {
				viewer.getControl().setRedraw(false);
				viewer.add(parent, children);
				viewer.remove(placeHolder);
			}
			finally {
				viewer.getControl().setRedraw(true);
			}
		}
		return Status.OK_STATUS;
	}
}