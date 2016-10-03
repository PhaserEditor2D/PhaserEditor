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
package org.eclipse.wst.jsdt.internal.ui.compare;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.JavaEditor;

/**
 * A delegate for JavaHistoryActionImpls.
 */
public abstract class JavaHistoryAction extends Action implements IActionDelegate { 
	
	private JavaHistoryActionImpl fDelegate;	
	private JavaEditor fEditor;
	private String fTitle;
	private String fMessage;
	
	JavaHistoryAction() {
	}
	
	private JavaHistoryActionImpl getDelegate() {
		if (fDelegate == null) {
			fDelegate= createDelegate();
			if (fEditor != null && fTitle != null && fMessage != null)
				fDelegate.init(fEditor, fTitle, fMessage);
		}
		return fDelegate;
	}
	
	protected abstract JavaHistoryActionImpl createDelegate();
	
	final void init(JavaEditor editor, String text, String title, String message) {
		Assert.isNotNull(editor);
		Assert.isNotNull(title);
		Assert.isNotNull(message);
		fEditor= editor;
		fTitle= title;
		fMessage= message;
		//getDelegate().init(editor, text, title, message);
		setText(text);
		//setEnabled(getDelegate().checkEnabled());
	}
	
	/**
	 * Executes this action with the given selection.
	 */
	public final void run(ISelection selection) {
		getDelegate().run(selection);
	}

	public final void run() {
		getDelegate().runFromEditor(this);
	}

	final void update() {
		getDelegate().update(this);
	}
	
 	//---- IActionDelegate
	
	public final void selectionChanged(IAction uiProxy, ISelection selection) {
		getDelegate().selectionChanged(uiProxy, selection);
	}
	
	public final void run(IAction action) {
		getDelegate().run(action);
	}
}
