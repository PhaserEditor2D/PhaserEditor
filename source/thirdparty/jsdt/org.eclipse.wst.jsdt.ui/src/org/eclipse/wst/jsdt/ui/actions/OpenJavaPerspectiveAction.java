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
package org.eclipse.wst.jsdt.ui.actions;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.actions.ActionMessages;
import org.eclipse.wst.jsdt.internal.ui.util.ExceptionHandler;
import org.eclipse.wst.jsdt.ui.JavaScriptUI;

/**
 * Action to programmatically open a JavaScript perspective.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 *
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 */
public class OpenJavaPerspectiveAction extends Action {

	/**
	 * Create a new <code>OpenJavaPerspectiveAction</code>.
	 */
	public OpenJavaPerspectiveAction() {
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.OPEN_JAVA_PERSPECTIVE_ACTION);
	}

	public void run() {
		IWorkbench workbench= JavaScriptPlugin.getDefault().getWorkbench();
		IWorkbenchWindow window= workbench.getActiveWorkbenchWindow();
		IWorkbenchPage page= window.getActivePage();
		IAdaptable input;
		if (page != null)
			input= page.getInput();
		else
			input= ResourcesPlugin.getWorkspace().getRoot();
		try {
			workbench.showPerspective(JavaScriptUI.ID_PERSPECTIVE, window, input);
		} catch (WorkbenchException e) {
			ExceptionHandler.handle(e, window.getShell(), 
				ActionMessages.OpenJavaPerspectiveAction_dialog_title, 
				ActionMessages.OpenJavaPerspectiveAction_error_open_failed); 
		}
	}
}
