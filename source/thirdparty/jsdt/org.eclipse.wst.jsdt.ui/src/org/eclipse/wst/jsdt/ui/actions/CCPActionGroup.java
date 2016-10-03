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

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.navigator.ICommonMenuConstants;
import org.eclipse.ui.part.Page;
import org.eclipse.ui.texteditor.IWorkbenchActionDefinitionIds;
import org.eclipse.wst.jsdt.internal.ui.actions.CopyQualifiedNameAction;
import org.eclipse.wst.jsdt.internal.ui.refactoring.reorg.CopyToClipboardAction;
import org.eclipse.wst.jsdt.internal.ui.refactoring.reorg.CutAction;
import org.eclipse.wst.jsdt.internal.ui.refactoring.reorg.DeleteAction;
import org.eclipse.wst.jsdt.internal.ui.refactoring.reorg.PasteAction;

/**
 * Action group that adds the copy, cut, paste actions to a view part's context
 * menu and installs handlers for the corresponding global menu actions.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 */
public class CCPActionGroup extends ActionGroup {

	private IWorkbenchSite fSite;
	private Clipboard fClipboard;

 	private SelectionDispatchAction[] fActions;

 	private SelectionDispatchAction fDeleteAction;
	private SelectionDispatchAction fCopyAction;
	private SelectionDispatchAction fCopyQualifiedNameAction;
	private SelectionDispatchAction fPasteAction;
	private SelectionDispatchAction fCutAction;
	
	private boolean showLimited;
	
	/**
	 * Creates a new <code>CCPActionGroup</code>. The group requires that
	 * the selection provided by the view part's selection provider is of type
	 * <code>org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param part the view part that owns this action group
	 */
	public CCPActionGroup(IViewPart  part) {
		this(part.getSite());
	}
	
	/**
	 * Creates a new <code>CCPActionGroup</code>.  The group requires that
	 * the selection provided by the page's selection provider is of type
	 * <code>org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param page the page that owns this action group
	 */
	public CCPActionGroup(Page page) {
		this(page.getSite());
	}

	private CCPActionGroup(IWorkbenchSite site) {
		fSite= site;
		fClipboard= new Clipboard(site.getShell().getDisplay());
		
		fPasteAction= new PasteAction(fSite, fClipboard);
		fPasteAction.setActionDefinitionId(IWorkbenchActionDefinitionIds.PASTE);
		
		fCopyAction= new CopyToClipboardAction(fSite, fClipboard);
		fCopyAction.setActionDefinitionId(IWorkbenchActionDefinitionIds.COPY);
		
		fCopyQualifiedNameAction= new CopyQualifiedNameAction(fSite);
		fCopyQualifiedNameAction.setActionDefinitionId(CopyQualifiedNameAction.ACTION_DEFINITION_ID);
		
		fCutAction= new CutAction(fSite, fClipboard);
		fCutAction.setActionDefinitionId(IWorkbenchActionDefinitionIds.CUT);
		
		fDeleteAction= new DeleteAction(fSite);
		fDeleteAction.setActionDefinitionId(IWorkbenchActionDefinitionIds.DELETE);
		
		fActions= new SelectionDispatchAction[] { fCutAction, fCopyAction, fCopyQualifiedNameAction, fPasteAction, fDeleteAction };
		registerActionsAsSelectionChangeListeners();
	}

	private void registerActionsAsSelectionChangeListeners() {
		ISelectionProvider provider = fSite.getSelectionProvider();
		ISelection selection= provider.getSelection();
		for (int i= 0; i < fActions.length; i++) {
			SelectionDispatchAction action= fActions[i];
			action.update(selection);
			provider.addSelectionChangedListener(action);
		}
	}
	
	private void deregisterActionsAsSelectionChangeListeners() {
		ISelectionProvider provider = fSite.getSelectionProvider();
		for (int i= 0; i < fActions.length; i++) {
			provider.removeSelectionChangedListener(fActions[i]);
		}
	}
	
	
	/**
	 * Returns the delete action managed by this action group. 
	 * 
	 * @return the delete action. Returns <code>null</code> if the group
	 * 	doesn't provide any delete action
	 */
	public IAction getDeleteAction() {
		return fDeleteAction;
	}

	/* (non-Javadoc)
	 * Method declared in ActionGroup
	 */
	public void fillActionBars(IActionBars actionBars) {
		super.fillActionBars(actionBars);
		actionBars.setGlobalActionHandler(ActionFactory.DELETE.getId(), fDeleteAction);
		actionBars.setGlobalActionHandler(ActionFactory.COPY.getId(), fCopyAction);
		actionBars.setGlobalActionHandler(CopyQualifiedNameAction.ACTION_HANDLER_ID, fCopyQualifiedNameAction);
		actionBars.setGlobalActionHandler(ActionFactory.CUT.getId(), fCutAction);
		actionBars.setGlobalActionHandler(ActionFactory.PASTE.getId(), fPasteAction);
	}
	
	/* (non-Javadoc)
	 * Method declared in ActionGroup
	 */
	public void fillContextMenu(IMenuManager menu) {
		super.fillContextMenu(menu);
		for (int i= 0; i < fActions.length; i++) {
			SelectionDispatchAction action= fActions[i];
			if (action == fCutAction && !fCutAction.isEnabled())
				continue;
			if (this.showLimited/*  -- in case you want paste action to always show up--  &&  action!=fPasteAction */)
				continue;
			menu.appendToGroup(ICommonMenuConstants.GROUP_EDIT, action);
		}		
	}		
	
	/*
	 * @see ActionGroup#dispose()
	 */
	public void dispose() {
		super.dispose();
		if (fClipboard != null){
			fClipboard.dispose();
			fClipboard= null;
		}
		deregisterActionsAsSelectionChangeListeners();
	}
	public void setShowLimited(boolean showLimited) {
		this.showLimited = showLimited;
	}


}
