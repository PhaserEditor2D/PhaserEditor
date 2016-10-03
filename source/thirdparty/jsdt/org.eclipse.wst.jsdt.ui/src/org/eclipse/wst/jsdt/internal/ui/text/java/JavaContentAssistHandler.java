/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.text.java;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.SpecificContentAssistExecutor;

/**
 * 
 * 
 */
public final class JavaContentAssistHandler extends AbstractHandler {
	private final SpecificContentAssistExecutor fExecutor= new SpecificContentAssistExecutor(CompletionProposalComputerRegistry.getDefault());
	
	public JavaContentAssistHandler() {
	}

	/*
	 * @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ITextEditor editor= getActiveEditor();
		if (editor == null)
			return null;
		
		String categoryId= event.getParameter("org.eclipse.wst.jsdt.ui.specific_content_assist.category_id"); //$NON-NLS-1$
		if (categoryId == null)
			return null;
		
		fExecutor.invokeContentAssist(editor, categoryId);

		return null;
	}

	private ITextEditor getActiveEditor() {
		IWorkbenchWindow window= PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window != null) {
			IWorkbenchPage page= window.getActivePage();
			if (page != null) {
				IEditorPart editor= page.getActiveEditor();
				if (editor instanceof ITextEditor)
					return (JavaEditor) editor;
			}
		}
		return null;
	}

}
