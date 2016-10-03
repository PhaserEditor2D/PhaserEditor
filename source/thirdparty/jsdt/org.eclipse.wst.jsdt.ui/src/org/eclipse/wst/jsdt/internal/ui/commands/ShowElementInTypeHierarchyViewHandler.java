/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.commands;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.util.OpenTypeHierarchyUtil;

/**
 * A command handler to show a java element in the type hierarchy view.
 * 
 * 
 */
public class ShowElementInTypeHierarchyViewHandler extends AbstractHandler {

	private static final String PARAM_ID_ELEMENT_REF= "elementRef"; //$NON-NLS-1$

	public Object execute(ExecutionEvent event) throws ExecutionException {

		IWorkbenchWindow window= JavaScriptPlugin.getActiveWorkbenchWindow();
		if (window == null)
			return null;

		IJavaScriptElement javaElement= (IJavaScriptElement) event.getObjectParameterForExecution(PARAM_ID_ELEMENT_REF);

		OpenTypeHierarchyUtil.open(javaElement, window);

		return null;
	}
}
