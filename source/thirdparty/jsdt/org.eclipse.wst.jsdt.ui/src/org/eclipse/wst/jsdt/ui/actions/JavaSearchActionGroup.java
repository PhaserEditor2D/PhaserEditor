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
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.part.Page;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.wst.jsdt.internal.ui.search.SearchMessages;
import org.eclipse.wst.jsdt.ui.PreferenceConstants;

/**
 * Action group that adds the JavaScript search actions to a context menu and
 * the global menu bar.
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
public class JavaSearchActionGroup extends ActionGroup {

	private JavaEditor fEditor;

	private ReferencesSearchGroup fReferencesGroup;
	private ReadReferencesSearchGroup fReadAccessGroup;
	private WriteReferencesSearchGroup fWriteAccessGroup;
	private DeclarationsSearchGroup fDeclarationsGroup;
//	private ImplementorsSearchGroup fImplementorsGroup;
	private OccurrencesSearchGroup fOccurrencesGroup;
	
	
	/**
	 * Creates a new <code>JavaSearchActionGroup</code>. The group 
	 * requires that the selection provided by the part's selection provider 
	 * is of type <code>org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param part the view part that owns this action group
	 */
	public JavaSearchActionGroup(IViewPart part) {
		this(part.getViewSite());
	}
	
	/**
	 * Creates a new <code>JavaSearchActionGroup</code>. The group 
	 * requires that the selection provided by the page's selection provider 
	 * is of type <code>org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param page the page that owns this action group
	 */
	public JavaSearchActionGroup(Page page) {
		this(page.getSite());
	}

	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * @param editor the JavaScript editor
	 */
	public JavaSearchActionGroup(JavaEditor editor) {
		Assert.isNotNull(editor);
		fEditor= editor;
		
		fReferencesGroup= new ReferencesSearchGroup(fEditor);
		fReadAccessGroup= new ReadReferencesSearchGroup(fEditor);
		fWriteAccessGroup= new WriteReferencesSearchGroup(fEditor);
		fDeclarationsGroup= new DeclarationsSearchGroup(fEditor);
//		fImplementorsGroup= new ImplementorsSearchGroup(fEditor);
		fOccurrencesGroup= new OccurrencesSearchGroup(fEditor);
	}

	private JavaSearchActionGroup(IWorkbenchSite site) {
		fReferencesGroup= new ReferencesSearchGroup(site);
		fReadAccessGroup= new ReadReferencesSearchGroup(site);
		fWriteAccessGroup= new WriteReferencesSearchGroup(site);
		fDeclarationsGroup= new DeclarationsSearchGroup(site);
//		fImplementorsGroup= new ImplementorsSearchGroup(site);
		fOccurrencesGroup= new OccurrencesSearchGroup(site);
	}

	/* 
	 * Method declared on ActionGroup.
	 */
	public void setContext(ActionContext context) {
		fReferencesGroup.setContext(context);
		fDeclarationsGroup.setContext(context);
//		fImplementorsGroup.setContext(context);
		fReadAccessGroup.setContext(context);
		fWriteAccessGroup.setContext(context);
		fOccurrencesGroup.setContext(context);
	}

	/* 
	 * Method declared on ActionGroup.
	 */
	public void fillActionBars(IActionBars actionBar) {
		super.fillActionBars(actionBar);
		fReferencesGroup.fillActionBars(actionBar);
		fDeclarationsGroup.fillActionBars(actionBar);
//		fImplementorsGroup.fillActionBars(actionBar);
		fReadAccessGroup.fillActionBars(actionBar);
		fWriteAccessGroup.fillActionBars(actionBar);
		fOccurrencesGroup.fillActionBars(actionBar);
	}
	
	/* 
	 * Method declared on ActionGroup.
	 */
	public void fillContextMenu(IMenuManager menu) {
		super.fillContextMenu(menu);
		
		if(PreferenceConstants.getPreferenceStore().getBoolean(PreferenceConstants.SEARCH_USE_REDUCED_MENU)) {
			fReferencesGroup.fillContextMenu(menu);
			fDeclarationsGroup.fillContextMenu(menu);

			if (fEditor == null) {
//				fImplementorsGroup.fillContextMenu(menu);
				fReadAccessGroup.fillContextMenu(menu);
				fWriteAccessGroup.fillContextMenu(menu);
			}
		} else {
			IMenuManager target= menu;
			IMenuManager searchSubMenu= null;
			if (fEditor != null) {
				String groupName= SearchMessages.group_search; 
				searchSubMenu= new MenuManager(groupName, ITextEditorActionConstants.GROUP_FIND);
				searchSubMenu.add(new GroupMarker(ITextEditorActionConstants.GROUP_FIND));
				target= searchSubMenu;
			}
			
			fReferencesGroup.fillContextMenu(target);
			fDeclarationsGroup.fillContextMenu(target);
//			fImplementorsGroup.fillContextMenu(target);
			fReadAccessGroup.fillContextMenu(target);
			fWriteAccessGroup.fillContextMenu(target);
			
			if (searchSubMenu != null) {
				fOccurrencesGroup.fillContextMenu(target);
				searchSubMenu.add(new Separator());
			}
			
			// no other way to find out if we have added items.
			if (searchSubMenu != null && searchSubMenu.getItems().length > 2) {		
				menu.appendToGroup(ITextEditorActionConstants.GROUP_FIND, searchSubMenu);
			}
		}
	}	

	/* 
	 * Method declared on ActionGroup.
	 */
	public void dispose() {
		fReferencesGroup.dispose();
		fDeclarationsGroup.dispose();
//		fImplementorsGroup.dispose();
		fReadAccessGroup.dispose();
		fWriteAccessGroup.dispose();
		fOccurrencesGroup.dispose();

		super.dispose();
	}
}
