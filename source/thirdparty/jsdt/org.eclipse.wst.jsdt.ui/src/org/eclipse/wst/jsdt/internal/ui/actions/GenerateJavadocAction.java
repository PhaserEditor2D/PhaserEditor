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
package org.eclipse.wst.jsdt.internal.ui.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.wst.jsdt.internal.ui.javadocexport.JavadocWizard;

public class GenerateJavadocAction implements IWorkbenchWindowActionDelegate {

	private ISelection fSelection;
	private Shell fCurrentShell;

	public void dispose() {
	}

	public void init(IWorkbenchWindow window) {
		fCurrentShell= window.getShell();
	}

	public void run(IAction action) {
		JavadocWizard wizard= new JavadocWizard();
		IStructuredSelection selection= null;
		if (fSelection instanceof IStructuredSelection) {
			selection= (IStructuredSelection)fSelection;
		} else {
			selection= new StructuredSelection();
		}
		JavadocWizard.openJavadocWizard(wizard, fCurrentShell, selection);
	}

	public void selectionChanged(IAction action, ISelection selection) {
		fSelection= selection;
	}
}
