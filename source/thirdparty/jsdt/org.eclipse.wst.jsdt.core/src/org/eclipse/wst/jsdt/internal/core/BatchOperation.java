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
package org.eclipse.wst.jsdt.internal.core;

import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.jsdt.core.IJavaScriptModelStatus;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;

/**
 * An operation created as a result of a call to JavaScriptCore.run(IWorkspaceRunnable, IProgressMonitor)
 * that encapsulates a user defined IWorkspaceRunnable.
 */
public class BatchOperation extends JavaModelOperation {
	protected IWorkspaceRunnable runnable;
	public BatchOperation(IWorkspaceRunnable runnable) {
		this.runnable = runnable;
	}

	protected boolean canModifyRoots() {
		// anything in the workspace runnable can modify the roots
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.core.JavaModelOperation#executeOperation()
	 */
	protected void executeOperation() throws JavaScriptModelException {
		try {
			this.runnable.run(this.progressMonitor);
		} catch (CoreException ce) {
			if (ce instanceof JavaScriptModelException) {
				throw (JavaScriptModelException)ce;
			} else {
				if (ce.getStatus().getCode() == IResourceStatus.OPERATION_FAILED) {
					Throwable e= ce.getStatus().getException();
					if (e instanceof JavaScriptModelException) {
						throw (JavaScriptModelException) e;
					}
				}
				throw new JavaScriptModelException(ce);
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.core.JavaModelOperation#verify()
	 */
	protected IJavaScriptModelStatus verify() {
		// cannot verify user defined operation
		return JavaModelStatus.VERIFIED_OK;
	}


}
