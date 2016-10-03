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
package org.eclipse.wst.jsdt.internal.ui.refactoring.reorg;

import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.CopyProjectAction;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringAvailabilityTester;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringExecutionStarter;
import org.eclipse.wst.jsdt.internal.corext.refactoring.reorg.ReorgUtils;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.wst.jsdt.internal.ui.util.ExceptionHandler;
import org.eclipse.wst.jsdt.ui.actions.SelectionDispatchAction;


public class ReorgCopyAction extends SelectionDispatchAction {

	public ReorgCopyAction(IWorkbenchSite site) {
		super(site);
		setText(ReorgMessages.ReorgCopyAction_3); 
		setDescription(ReorgMessages.ReorgCopyAction_4); 

		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.COPY_ACTION);
	}

	public void selectionChanged(IStructuredSelection selection) {
		if (!selection.isEmpty()) {
			if (ReorgUtils.containsOnlyProjects(selection.toList())) {
				setEnabled(createWorkbenchAction(selection).isEnabled());
				return;
			}
			try {
				List elements= selection.toList();
				IResource[] resources= ReorgUtils.getResources(elements);
				IJavaScriptElement[] javaElements= ReorgUtils.getJavaElements(elements);
				if (elements.size() != resources.length + javaElements.length)
					setEnabled(false);
				else
					setEnabled(RefactoringAvailabilityTester.isCopyAvailable(resources, javaElements));
			} catch (JavaScriptModelException e) {
				// no ui here - this happens on selection changes
				// http://bugs.eclipse.org/bugs/show_bug.cgi?id=19253
				if (JavaModelUtil.isExceptionToBeLogged(e))
					JavaScriptPlugin.log(e);
				setEnabled(false);
			}
		} else
			setEnabled(false);
	}

	private CopyProjectAction createWorkbenchAction(IStructuredSelection selection) {
		CopyProjectAction action= new CopyProjectAction(getShell());
		action.selectionChanged(selection);
		return action;
	}
	
	public void run(IStructuredSelection selection) {
		if (ReorgUtils.containsOnlyProjects(selection.toList())){
			createWorkbenchAction(selection).run();
			return;
		}
		try {
			List elements= selection.toList();
			IResource[] resources= ReorgUtils.getResources(elements);
			IJavaScriptElement[] javaElements= ReorgUtils.getJavaElements(elements);
			if (RefactoringAvailabilityTester.isCopyAvailable(resources, javaElements)) 
				RefactoringExecutionStarter.startCopyRefactoring(resources, javaElements, getShell());
		} catch (JavaScriptModelException e) {
			ExceptionHandler.handle(e, RefactoringMessages.OpenRefactoringWizardAction_refactoring, RefactoringMessages.OpenRefactoringWizardAction_exception); 
		}
	}
}
