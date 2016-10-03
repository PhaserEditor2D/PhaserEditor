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

package org.eclipse.wst.jsdt.internal.ui.javaeditor;


import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IEditorInput;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.ui.IWorkingCopyManager;
import org.eclipse.wst.jsdt.ui.IWorkingCopyManagerExtension;


/**
 * This working copy manager works together with a given compilation unit document provider and
 * additionally offers to "overwrite" the working copy provided by this document provider.
 */
public class WorkingCopyManager implements IWorkingCopyManager, IWorkingCopyManagerExtension {

	private ICompilationUnitDocumentProvider fDocumentProvider;
	private Map fMap;
	private boolean fIsShuttingDown;

	/**
	 * Creates a new working copy manager that co-operates with the given
	 * compilation unit document provider.
	 *
	 * @param provider the provider
	 */
	public WorkingCopyManager(ICompilationUnitDocumentProvider provider) {
		Assert.isNotNull(provider);
		fDocumentProvider= provider;
	}

	/*
	 * @see org.eclipse.wst.jsdt.ui.IWorkingCopyManager#connect(org.eclipse.ui.IEditorInput)
	 */
	public void connect(IEditorInput input) throws CoreException {
		fDocumentProvider.connect(input);
	}

	/*
	 * @see org.eclipse.wst.jsdt.ui.IWorkingCopyManager#disconnect(org.eclipse.ui.IEditorInput)
	 */
	public void disconnect(IEditorInput input) {
		fDocumentProvider.disconnect(input);
	}

	/*
	 * @see org.eclipse.wst.jsdt.ui.IWorkingCopyManager#shutdown()
	 */
	public void shutdown() {
		if (!fIsShuttingDown) {
			fIsShuttingDown= true;
			try {
				if (fMap != null) {
					fMap.clear();
					fMap= null;
				}
				fDocumentProvider.shutdown();
			} finally {
				fIsShuttingDown= false;
			}
		}
	}

	/*
	 * @see org.eclipse.wst.jsdt.ui.IWorkingCopyManager#getWorkingCopy(org.eclipse.ui.IEditorInput)
	 */
	public IJavaScriptUnit getWorkingCopy(IEditorInput input) {
		return getWorkingCopy(input, true);
	}

	/**
	 * Returns the working copy remembered for the compilation unit encoded in the
	 * given editor input.
	 * <p>
	 * Note: This method must not be part of the public {@link IWorkingCopyManager} API.
	 * </p>
	 *
	 * @param input the editor input
	 * @param primaryOnly if <code>true</code> only primary working copies will be returned
	 * @return the working copy of the compilation unit, or <code>null</code> if the
	 *   input does not encode an editor input, or if there is no remembered working
	 *   copy for this compilation unit
	 * 
	 */
	public IJavaScriptUnit getWorkingCopy(IEditorInput input, boolean primaryOnly) {
		IJavaScriptUnit unit= fMap == null ? null : (IJavaScriptUnit) fMap.get(input);
		if (unit == null)
			unit= fDocumentProvider.getWorkingCopy(input);
		if (unit != null && (!primaryOnly || JavaModelUtil.isPrimary(unit)))
			return unit;
		return null;
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.ui.javaeditor.IWorkingCopyManagerExtension#setWorkingCopy(org.eclipse.ui.IEditorInput, org.eclipse.wst.jsdt.core.IJavaScriptUnit)
	 */
	public void setWorkingCopy(IEditorInput input, IJavaScriptUnit workingCopy) {
		if (fDocumentProvider.getDocument(input) != null) {
			if (fMap == null)
				fMap= new HashMap();
			fMap.put(input, workingCopy);
		}
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.ui.javaeditor.IWorkingCopyManagerExtension#removeWorkingCopy(org.eclipse.ui.IEditorInput)
	 */
	public void removeWorkingCopy(IEditorInput input) {
		fMap.remove(input);
		if (fMap.isEmpty())
			fMap= null;
	}
}
