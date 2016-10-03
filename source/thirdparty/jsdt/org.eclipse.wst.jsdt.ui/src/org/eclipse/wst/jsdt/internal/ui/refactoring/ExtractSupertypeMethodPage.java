/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.refactoring;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.ExtractSupertypeProcessor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.ExtractSupertypeRefactoring;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;

/**
 * Wizard page to select methods to be deleted after extract supertype.
 * 
 * 
 */
public class ExtractSupertypeMethodPage extends PullUpMethodPage {

	/**
	 * Returns the extract supertype refactoring.
	 */
	public ExtractSupertypeRefactoring getExtractSuperTypeRefactoring() {
		return (ExtractSupertypeRefactoring) getRefactoring();
	}

	/**
	 * Returns the refactoring processor.
	 * 
	 * @return the refactoring processor
	 */
	protected ExtractSupertypeProcessor getProcessor() {
		return getExtractSuperTypeRefactoring().getExtractSupertypeProcessor();
	}

	/**
	 * {@inheritDoc}
	 */
	public void createControl(Composite parent) {
		super.createControl(parent);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IJavaHelpContextIds.EXTRACT_SUPERTYPE_WIZARD_PAGE);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void setVisible(final boolean visible) {
		if (visible) {
			final ExtractSupertypeProcessor processor= getProcessor();
			processor.resetChanges();
			try {
				getWizard().getContainer().run(false, false, new IRunnableWithProgress() {

					public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						processor.createWorkingCopyLayer(monitor);
					}
				});
			} catch (InvocationTargetException exception) {
				JavaScriptPlugin.log(exception);
			} catch (InterruptedException exception) {
				// Does not happen
			}
		}
		super.setVisible(visible);
	}
}
