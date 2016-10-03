/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.javaeditor;

import java.util.ResourceBundle;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.ITextEditorExtension;
import org.eclipse.ui.texteditor.RetargetTextEditorAction;
import org.eclipse.ui.texteditor.StatusLineContributionItem;
import org.eclipse.wst.jsdt.ui.IContextMenuConstants;
import org.eclipse.wst.jsdt.ui.actions.JdtActionConstants;

public class CompilationUnitEditorActionContributor extends BasicCompilationUnitEditorActionContributor {
	private static final boolean _showOffset = Boolean.valueOf((Platform.getDebugOption("org.eclipse.wst.jsdt.ui/statusbar/offset"))).booleanValue() || Platform.inDebugMode() || Platform.inDevelopmentMode(); //$NON-NLS-1$

	private RetargetTextEditorAction fToggleInsertModeAction;

	private StatusLineContributionItem fOffsetStatusField = null;

	public CompilationUnitEditorActionContributor() {
		super();

		ResourceBundle b= JavaEditorMessages.getBundleForConstructedKeys();

		fToggleInsertModeAction= new RetargetTextEditorAction(b, "CompilationUnitEditorActionContributor.ToggleInsertMode.", IAction.AS_CHECK_BOX); //$NON-NLS-1$
		fToggleInsertModeAction.setActionDefinitionId(ITextEditorActionDefinitionIds.TOGGLE_INSERT_MODE);
		
		if (_showOffset) {
			fOffsetStatusField = new StatusLineContributionItem(IJavaEditorActionConstants.STATUS_CATEGORY_OFFSET, true, 10);
		}
	}


	/*
	 * @see org.eclipse.wst.jsdt.internal.ui.javaeditor.BasicEditorActionContributor#contributeToMenu(org.eclipse.jface.action.IMenuManager)
	 */
	public void contributeToMenu(IMenuManager menu) {
		super.contributeToMenu(menu);

		IMenuManager editMenu= menu.findMenuUsingPath(IWorkbenchActionConstants.M_EDIT);
		if (editMenu != null) {
			editMenu.appendToGroup(IContextMenuConstants.GROUP_ADDITIONS, fToggleInsertModeAction);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.EditorActionBarContributor#contributeToStatusLine(org.eclipse.jface.action.IStatusLineManager)
	 */
	public void contributeToStatusLine(IStatusLineManager manager) {
		super.contributeToStatusLine(manager);
		if (_showOffset) {
			manager.add(fOffsetStatusField);
		}
	}

	/*
	 * @see IEditorActionBarContributor#setActiveEditor(IEditorPart)
	 */
	public void setActiveEditor(IEditorPart part) {
		super.setActiveEditor(part);

		ITextEditor textEditor= null;
		ITextEditorExtension textEditorExtension= null;
		if (part instanceof ITextEditor)
			textEditor= (ITextEditor) part;
		if (part instanceof ITextEditorExtension)
			textEditorExtension= (ITextEditorExtension) part;
		
		if(_showOffset && textEditorExtension !=null) {
			textEditorExtension.setStatusField(null, IJavaEditorActionConstants.STATUS_CATEGORY_OFFSET);
		}

		// Source menu.
		IActionBars bars= getActionBars();
		bars.setGlobalActionHandler(JdtActionConstants.COMMENT, getAction(textEditor, "Comment")); //$NON-NLS-1$
		bars.setGlobalActionHandler(JdtActionConstants.UNCOMMENT, getAction(textEditor, "Uncomment")); //$NON-NLS-1$
		bars.setGlobalActionHandler(JdtActionConstants.TOGGLE_COMMENT, getAction(textEditor, "ToggleComment")); //$NON-NLS-1$
		bars.setGlobalActionHandler(JdtActionConstants.FORMAT, getAction(textEditor, "Format")); //$NON-NLS-1$
		bars.setGlobalActionHandler(JdtActionConstants.FORMAT_ELEMENT, getAction(textEditor, "QuickFormat")); //$NON-NLS-1$
		bars.setGlobalActionHandler(JdtActionConstants.ADD_BLOCK_COMMENT, getAction(textEditor, "AddBlockComment")); //$NON-NLS-1$
		bars.setGlobalActionHandler(JdtActionConstants.REMOVE_BLOCK_COMMENT, getAction(textEditor, "RemoveBlockComment")); //$NON-NLS-1$
		bars.setGlobalActionHandler(JdtActionConstants.INDENT, getAction(textEditor, "Indent")); //$NON-NLS-1$

		IAction action= getAction(textEditor, ActionFactory.REFRESH.getId());
		bars.setGlobalActionHandler(ActionFactory.REFRESH.getId(), action);

		fToggleInsertModeAction.setAction(getAction(textEditor, ITextEditorActionConstants.TOGGLE_INSERT_MODE));

		if(_showOffset && textEditorExtension !=null) {
			textEditorExtension.setStatusField(fOffsetStatusField, IJavaEditorActionConstants.STATUS_CATEGORY_OFFSET);
			// fOffsetStatusField.setActionHandler(action);
		}
	}
}
