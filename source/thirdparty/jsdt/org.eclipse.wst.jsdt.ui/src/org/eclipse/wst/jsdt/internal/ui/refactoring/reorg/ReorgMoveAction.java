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
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.MoveProjectAction;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringAvailabilityTester;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringExecutionStarter;
import org.eclipse.wst.jsdt.internal.corext.refactoring.reorg.ReorgUtils;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.JavaTextSelection;
import org.eclipse.wst.jsdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.wst.jsdt.internal.ui.util.ExceptionHandler;
import org.eclipse.wst.jsdt.ui.actions.SelectionDispatchAction;

public class ReorgMoveAction extends SelectionDispatchAction {
	public ReorgMoveAction(IWorkbenchSite site) {
		super(site);
		setText(ReorgMessages.ReorgMoveAction_3); 
		setDescription(ReorgMessages.ReorgMoveAction_4); 
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.MOVE_ACTION);
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
					setEnabled(RefactoringAvailabilityTester.isMoveAvailable(resources, javaElements));
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

	public void selectionChanged(ITextSelection selection) {
		setEnabled(true);
	}

	/**
	 * Note: This method is for internal use only. Clients should not call this method.
	 */
	public void selectionChanged(JavaTextSelection selection) {
		try {
			setEnabled(RefactoringAvailabilityTester.isMoveAvailable(selection));
		} catch (JavaScriptModelException e) {
			setEnabled(false);
		}
	}

	private MoveProjectAction createWorkbenchAction(IStructuredSelection selection) {
		MoveProjectAction action= new MoveProjectAction(getShell());
		action.selectionChanged(selection);
		return action;
	}

	public void run(IStructuredSelection selection) {
		if (ReorgUtils.containsOnlyProjects(selection.toList())) {
			createWorkbenchAction(selection).run();
			return;
		}
		try {
			List elements= selection.toList();
			IResource[] resources= ReorgUtils.getResources(elements);
			IJavaScriptElement[] javaElements= ReorgUtils.getJavaElements(elements);
			if (RefactoringAvailabilityTester.isMoveAvailable(resources, javaElements))
				RefactoringExecutionStarter.startMoveRefactoring(resources, javaElements, getShell());
		} catch (JavaScriptModelException e) {
			ExceptionHandler.handle(e, RefactoringMessages.OpenRefactoringWizardAction_refactoring, RefactoringMessages.OpenRefactoringWizardAction_exception); 
		}
	}
}
