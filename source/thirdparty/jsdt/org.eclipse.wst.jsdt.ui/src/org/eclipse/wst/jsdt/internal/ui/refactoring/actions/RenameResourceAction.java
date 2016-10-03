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
package org.eclipse.wst.jsdt.internal.ui.refactoring.actions;


import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringAvailabilityTester;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringExecutionStarter;
import org.eclipse.wst.jsdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.wst.jsdt.internal.ui.util.ExceptionHandler;
import org.eclipse.wst.jsdt.ui.actions.SelectionDispatchAction;

public class RenameResourceAction extends SelectionDispatchAction {

	public RenameResourceAction(IWorkbenchSite site) {
		super(site);
	}
	
	public void selectionChanged(IStructuredSelection selection) {
		IResource element= getResource(selection);
		if (element == null)
			setEnabled(false);
		else
			setEnabled(RefactoringAvailabilityTester.isRenameAvailable(element));
	}

	public void run(IStructuredSelection selection) {
		IResource resource = getResource(selection);
		if (!RefactoringAvailabilityTester.isRenameAvailable(resource))
			return;
		try {
			RefactoringExecutionStarter.startRenameResourceRefactoring(resource, getShell());
		} catch (CoreException e) {
			ExceptionHandler.handle(e, RefactoringMessages.RenameJavaElementAction_name, RefactoringMessages.RenameJavaElementAction_exception);  
		}
	}

	private static IResource getResource(IStructuredSelection selection) {
		if (selection.size() != 1)
			return null;
		Object first= selection.getFirstElement();
		if (! (first instanceof IResource))
			return null;
		return (IResource)first;
	}
}
