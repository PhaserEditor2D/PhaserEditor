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
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.ui.JavaScriptUI;

/**
 * A command handler to open a java element in its editor.
 * 
 * 
 */
public class OpenElementInEditorHandler extends AbstractHandler {

	private static final String PARAM_ID_ELEMENT_REF= "elementRef"; //$NON-NLS-1$

	public Object execute(ExecutionEvent event) throws ExecutionException {

		IJavaScriptElement javaElement= (IJavaScriptElement) event.getObjectParameterForExecution(PARAM_ID_ELEMENT_REF);

		try {
			IEditorPart editorPart= JavaScriptUI.openInEditor(javaElement);
			JavaScriptUI.revealInEditor(editorPart, javaElement);
		} catch (JavaScriptModelException ex) {
			throw new ExecutionException("Error opening java element in editor", ex); //$NON-NLS-1$
		} catch (PartInitException ex) {
			throw new ExecutionException("Error opening java element in editor", ex); //$NON-NLS-1$
		}

		return null;
	}

}
