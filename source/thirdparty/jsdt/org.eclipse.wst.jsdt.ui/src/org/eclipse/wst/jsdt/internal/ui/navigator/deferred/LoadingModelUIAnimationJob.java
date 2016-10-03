/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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

public class LoadingModelUIAnimationJob extends UIJob {

	/* we update the animation frame every 250 ms */
	private static final long DELAY = 200;

	/* the node holding the place for the model (the "Loading ..." in the tree ) */
	private LoadingModelNode placeHolder;
	private AbstractTreeViewer viewer;

	public LoadingModelUIAnimationJob(AbstractTreeViewer viewer,
			LoadingModelNode placeHolder) {
		super(placeHolder.getText());
		this.viewer = viewer;
		this.placeHolder = placeHolder;
		/*
		 * this way we don't put alot of noise in the progress view, except for
		 * power users that turn on "show system jobs" in the view
		 */
		setSystem(true);
		setRule(new NonConflictingRule());
	}

	public IStatus runInUIThread(IProgressMonitor monitor) {

		if (!placeHolder.isDisposed()) {

			/* update the animation frame */
			viewer.update(placeHolder, null);

			/* reschedule for the next animation frame */
			schedule(DELAY);
		}
		return Status.OK_STATUS;

	}
}
