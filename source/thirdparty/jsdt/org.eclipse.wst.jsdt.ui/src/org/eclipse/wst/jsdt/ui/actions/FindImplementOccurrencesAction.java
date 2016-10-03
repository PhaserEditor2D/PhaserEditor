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

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IEditorStatusLine;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.actions.ActionMessages;
import org.eclipse.wst.jsdt.internal.ui.actions.ActionUtil;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.IClassFileEditorInput;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.wst.jsdt.internal.ui.search.FindOccurrencesEngine;
import org.eclipse.wst.jsdt.internal.ui.search.ImplementOccurrencesFinder;

/**
 * Action to find all implement occurrences of an extended class or an implemented interface.
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
public class FindImplementOccurrencesAction extends SelectionDispatchAction {
	
	private JavaEditor fEditor;
	
	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * 
	 * @param editor the JavaScript editor
	 */
	public FindImplementOccurrencesAction(JavaEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(getEditorInput(editor) != null);
	}
	
	/**
	 * Creates a new <code>FindImplementOccurrencesAction</code>. The action 
	 * requires that the selection provided by the site's selection provider is of type 
	 * <code>IStructuredSelection</code>.
	 * 
	 * @param site the site providing context information for this action
	 */
	public FindImplementOccurrencesAction(IWorkbenchSite site) {
		super(site);
		setText(ActionMessages.FindImplementOccurrencesAction_text); 
		setToolTipText(ActionMessages.FindImplementOccurrencesAction_toolTip); 
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.FIND_IMPLEMENT_OCCURRENCES);
	}
	
	//---- Text Selection ----------------------------------------------------------------------
	
	/**
	 * {@inheritDoc}
	 */
	public void selectionChanged(ITextSelection selection) {
	}
	
	/**
	 * {@inheritDoc}
	 * 
	 * 
	 */
	public void selectionChanged(IStructuredSelection selection) {
		setEnabled(getMember(selection) != null);
	}

	/*
	 * @see org.eclipse.wst.jsdt.ui.actions.SelectionDispatchAction#run(org.eclipse.jface.text.ITextSelection)
	 */
	public final void run(ITextSelection ts) {
		IJavaScriptElement input= getEditorInput(fEditor);
		if (!ActionUtil.isProcessable(getShell(), input))
			return;
		FindOccurrencesEngine engine= FindOccurrencesEngine.create(input, new ImplementOccurrencesFinder());
		try {
			String result= engine.run(ts.getOffset(), ts.getLength());
			if (result != null)
				showMessage(getShell(), fEditor, result);
		} catch (JavaScriptModelException e) {
			JavaScriptPlugin.log(e);
		}
	}

	private static IJavaScriptElement getEditorInput(JavaEditor editor) {
		IEditorInput input= editor.getEditorInput();
		if (input instanceof IClassFileEditorInput)
			return ((IClassFileEditorInput)input).getClassFile();
		return  JavaScriptPlugin.getDefault().getWorkingCopyManager().getWorkingCopy(input);
	} 
		
	private static void showMessage(Shell shell, JavaEditor editor, String msg) {
		IEditorStatusLine statusLine= (IEditorStatusLine) editor.getAdapter(IEditorStatusLine.class);
		if (statusLine != null) 
			statusLine.setMessage(true, msg, null); 
		shell.getDisplay().beep();
	}

	private IMember getMember(IStructuredSelection selection) {
		return null;
	}
}
