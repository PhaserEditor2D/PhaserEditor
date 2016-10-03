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
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.internal.ui.packageview.PackageExplorerPart;

/**
 * A command handler to show a java element in the package view.
 * 
 * 
 */
public class ShowElementInPackageViewHandler extends AbstractHandler {

	private static final String PARAM_ID_ELEMENT_REF= "elementRef"; //$NON-NLS-1$

	public Object execute(ExecutionEvent event) throws ExecutionException {

		IJavaScriptElement javaElement= (IJavaScriptElement) event.getObjectParameterForExecution(PARAM_ID_ELEMENT_REF);

		PackageExplorerPart view= PackageExplorerPart.openInActivePerspective();
		view.tryToReveal(javaElement);

		return null;
	}

}
