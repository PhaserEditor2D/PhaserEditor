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
package org.eclipse.wst.jsdt.internal.ui.browsing;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.OpenInNewWindowAction;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.JavaScriptCore;

/*
 * XXX: This is a workaround for: http://dev.eclipse.org/bugs/show_bug.cgi?id=13070
 * This class can be removed once the bug is fixed.
 *
 * 
 */
public class PatchedOpenInNewWindowAction extends OpenInNewWindowAction {

	private IWorkbenchWindow fWorkbenchWindow;

	public PatchedOpenInNewWindowAction(IWorkbenchWindow window, IAdaptable input) {
		super(window, input);
		fWorkbenchWindow= window;
	}

	public void run() {
		JavaBrowsingPerspectiveFactory.setInputFromAction(getSelectedJavaElement());
		try {
			super.run();
		} finally {
			JavaBrowsingPerspectiveFactory.setInputFromAction(null);
		}
	}

	private IJavaScriptElement getSelectedJavaElement() {
		if (fWorkbenchWindow.getActivePage() != null) {
			ISelection selection= fWorkbenchWindow.getActivePage().getSelection();
			if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
				Object selectedElement= ((IStructuredSelection)selection).getFirstElement();
				if (selectedElement instanceof IJavaScriptElement)
					return (IJavaScriptElement)selectedElement;
				if (!(selectedElement instanceof IJavaScriptElement) && selectedElement instanceof IAdaptable)
					return (IJavaScriptElement)((IAdaptable)selectedElement).getAdapter(IJavaScriptElement.class);
				else if (selectedElement instanceof IWorkspace)
						return JavaScriptCore.create(((IWorkspace)selectedElement).getRoot());
			}
		}
		return null;
	}
}
