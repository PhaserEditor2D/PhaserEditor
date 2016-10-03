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

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.ITypeRoot;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.actions.ActionUtil;
import org.eclipse.wst.jsdt.internal.ui.actions.SelectionConverter;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.wst.jsdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.wst.jsdt.internal.ui.refactoring.actions.InlineConstantAction;
import org.eclipse.wst.jsdt.internal.ui.refactoring.actions.InlineMethodAction;

/**
 * Inlines a method, local variable or a static final field.
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
public class InlineAction extends SelectionDispatchAction {

	private JavaEditor fEditor;
	private final InlineTempAction fInlineTemp;
	private final InlineMethodAction fInlineMethod;
	private final InlineConstantAction fInlineConstant;

	/**
	 * Creates a new <code>InlineAction</code>. The action requires
	 * that the selection provided by the site's selection provider is of type <code>
	 * org.eclipse.jface.viewers.IStructuredSelection</code>.
	 *
	 * @param site the site providing context information for this action
	 */
	public InlineAction(IWorkbenchSite site) {
		super(site);
		setText(RefactoringMessages.InlineAction_Inline); 
		fInlineTemp		= new InlineTempAction(site);
		fInlineConstant	= new InlineConstantAction(site);
		fInlineMethod	= new InlineMethodAction(site);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.INLINE_ACTION);
	}

	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * @param editor the compilation unit editor
	 */
	public InlineAction(JavaEditor editor) {
		//don't want to call 'this' here - it'd create useless action objects
		super(editor.getEditorSite());
		setText(RefactoringMessages.InlineAction_Inline); 
		fEditor= editor;
		fInlineTemp		= new InlineTempAction(editor);
		fInlineConstant	= new InlineConstantAction(editor);
		fInlineMethod	= new InlineMethodAction(editor);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.INLINE_ACTION);
		setEnabled(SelectionConverter.getInputAsCompilationUnit(fEditor) != null);
	}

	/*
	 * @see org.eclipse.wst.jsdt.ui.actions.SelectionDispatchAction#selectionChanged(org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(ISelection selection) {
		fInlineConstant.update(selection);
		fInlineMethod.update(selection);
		fInlineTemp.update(selection);
		setEnabled((fInlineTemp.isEnabled() || fInlineConstant.isEnabled() || fInlineMethod.isEnabled()));
	}

	/*
	 * @see org.eclipse.wst.jsdt.ui.actions.SelectionDispatchAction#run(org.eclipse.jface.text.ITextSelection)
	 */
	public void run(ITextSelection selection) {
		if (!ActionUtil.isEditable(fEditor))
			return;

		ITypeRoot typeRoot= SelectionConverter.getInputAsTypeRoot(fEditor);
		if (typeRoot == null)
			return;

		JavaScriptUnit node= RefactoringASTParser.parseWithASTProvider(typeRoot, true, null);
		
		if (typeRoot instanceof IJavaScriptUnit) {
			IJavaScriptUnit cu= (IJavaScriptUnit) typeRoot;
			if (fInlineTemp.isEnabled() && fInlineTemp.tryInlineTemp(cu, node, selection, getShell()))
				return;

			if (fInlineConstant.isEnabled() && fInlineConstant.tryInlineConstant(cu, node, selection, getShell()))
				return;
		}
		//InlineMethod is last (also tries enclosing element):
		if (fInlineMethod.isEnabled() && fInlineMethod.tryInlineMethod(typeRoot, node, selection, getShell()))
			return;

		MessageDialog.openInformation(getShell(), RefactoringMessages.InlineAction_dialog_title, RefactoringMessages.InlineAction_select); 
	}
	
	/*
	 * @see org.eclipse.wst.jsdt.ui.actions.SelectionDispatchAction#run(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void run(IStructuredSelection selection) {
		if (fInlineConstant.isEnabled())
			fInlineConstant.run(selection);
		else if (fInlineMethod.isEnabled())
			fInlineMethod.run(selection);
		else	
			//inline temp will never be enabled on IStructuredSelection
			//don't bother running it
			Assert.isTrue(! fInlineTemp.isEnabled());
	}
}
