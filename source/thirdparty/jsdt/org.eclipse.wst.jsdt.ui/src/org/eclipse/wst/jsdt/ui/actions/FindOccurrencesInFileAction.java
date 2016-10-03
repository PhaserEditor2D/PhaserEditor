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

import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.part.Page;
import org.eclipse.ui.texteditor.IEditorStatusLine;
import org.eclipse.wst.jsdt.core.IClassFile;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.ISourceRange;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.actions.ActionUtil;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.IClassFileEditorInput;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.wst.jsdt.internal.ui.search.FindOccurrencesEngine;
import org.eclipse.wst.jsdt.internal.ui.search.OccurrencesFinder;
import org.eclipse.wst.jsdt.internal.ui.search.SearchMessages;

/**
 * Action to find all occurrences of a compilation unit member (e.g.
 * fields, methods, types, and local variables) in a file. 
 * <p>
 * Action is applicable to selections containing elements of type
 * <tt>IMember</tt>.
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
public class FindOccurrencesInFileAction extends SelectionDispatchAction {
	
	private JavaEditor fEditor;
	private IActionBars fActionBars;
	
	/**
	 * Creates a new <code>FindOccurrencesInFileAction</code>. The action requires 
	 * that the selection provided by the view part's selection provider is of type <code>
	 * org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param part the part providing context information for this action
	 */
	public FindOccurrencesInFileAction(IViewPart part) {
		this(part.getSite());
	}
	
	/**
	 * Creates a new <code>FindOccurrencesInFileAction</code>. The action requires 
	 * that the selection provided by the page's selection provider is of type <code>
	 * org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param page the page providing context information for this action
	 */
	public FindOccurrencesInFileAction(Page page) {
		this(page.getSite());
	}
 	
	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * @param editor the JavaScript editor
	 */
	public FindOccurrencesInFileAction(JavaEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(getEditorInput(editor) != null);
	}
	
	/**
	 * Creates a new <code>FindOccurrencesInFileAction</code>. The action 
	 * requires that the selection provided by the site's selection provider is of type 
	 * <code>IStructuredSelection</code>.
	 * 
	 * @param site the site providing context information for this action
	 * 
	 */
	public FindOccurrencesInFileAction(IWorkbenchSite site) {
		super(site);
		
		if (site instanceof IViewSite)
			fActionBars= ((IViewSite)site).getActionBars();
		else if (site instanceof IEditorSite)
			fActionBars= ((IEditorSite)site).getActionBars();
		else if (site instanceof IPageSite)
			fActionBars= ((IPageSite)site).getActionBars();
		
		setText(SearchMessages.Search_FindOccurrencesInFile_label); 
		setToolTipText(SearchMessages.Search_FindOccurrencesInFile_tooltip); 
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.FIND_OCCURRENCES_IN_FILE_ACTION);
	}
	
	//---- Structured Selection -------------------------------------------------------------
	
	/* (non-JavaDoc)
	 * Method declared in SelectionDispatchAction.
	 */
	public void selectionChanged(IStructuredSelection selection) {
		setEnabled(getMember(selection) != null);
	}
	
	/* (non-JavaDoc)
	 * Method declared in SelectionDispatchAction.
	 */
	private IMember getMember(IStructuredSelection selection) {
		if (selection.size() != 1)
			return null;
		Object o= selection.getFirstElement();
		if (o instanceof IMember) {
			IMember member= (IMember)o;
			try {
				if (member.getNameRange() == null)
					return null;
			} catch (JavaScriptModelException ex) {
				return null;
			}
			
			IClassFile file= member.getClassFile();
			if (file != null) {
				try {
					if (file.getSourceRange() != null)
						return member;
				} catch (JavaScriptModelException e) {
					return null;
				}
			}
			return member;
		}
		return null;
	}
	
	public void run(IStructuredSelection selection) {
		IMember member= getMember(selection);
		if (!ActionUtil.isProcessable(getShell(), member))
			return;
		FindOccurrencesEngine engine= FindOccurrencesEngine.create(member, new OccurrencesFinder());
		try {
			ISourceRange range= member.getNameRange();
			String result= engine.run(range.getOffset(), range.getLength());
			if (result != null)
				showMessage(getShell(), fActionBars, result);
		} catch (JavaScriptModelException e) {
			JavaScriptPlugin.log(e);
		}
	}
	
	private static void showMessage(Shell shell, IActionBars actionBars, String msg) {
		if (actionBars != null) {
			IStatusLineManager statusLine= actionBars.getStatusLineManager();
			if (statusLine != null)
				statusLine.setMessage(msg);
		}
		shell.getDisplay().beep();
	}
	
	//---- Text Selection ----------------------------------------------------------------------
	
	/* (non-JavaDoc)
	 * Method declared in SelectionDispatchAction.
	 */
	public void selectionChanged(ITextSelection selection) {
	}

	/* (non-JavaDoc)
	 * Method declared in SelectionDispatchAction.
	 */
	public final void run(ITextSelection ts) {
		IJavaScriptElement input= getEditorInput(fEditor);
		if (!ActionUtil.isProcessable(getShell(), input))
			return;
		FindOccurrencesEngine engine= FindOccurrencesEngine.create(input, new OccurrencesFinder());
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
}
