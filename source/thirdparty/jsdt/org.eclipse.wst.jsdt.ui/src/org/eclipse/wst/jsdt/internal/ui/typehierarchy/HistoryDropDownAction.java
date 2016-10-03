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
package org.eclipse.wst.jsdt.internal.ui.typehierarchy;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;

public class HistoryDropDownAction extends Action implements IMenuCreator {
	
	public static class ClearHistoryAction extends Action {

		private TypeHierarchyViewPart fView;
		
		public ClearHistoryAction(TypeHierarchyViewPart view) {
			super(TypeHierarchyMessages.HistoryDropDownAction_clearhistory_label);
			fView= view;
		}
			
		public void run() {
			fView.setHistoryEntries(new IJavaScriptElement[0]);
			fView.setInputElement(null);
		}
	}

	public static final int RESULTS_IN_DROP_DOWN= 10;

	private TypeHierarchyViewPart fHierarchyView;
	private Menu fMenu;
	
	public HistoryDropDownAction(TypeHierarchyViewPart view) {
		fHierarchyView= view;
		fMenu= null;
		setToolTipText(TypeHierarchyMessages.HistoryDropDownAction_tooltip); 
		JavaPluginImages.setLocalImageDescriptors(this, "history_list.gif"); //$NON-NLS-1$
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.TYPEHIERARCHY_HISTORY_ACTION);
		setMenuCreator(this);
	}

	public void dispose() {
		// action is reused, can be called several times.
		if (fMenu != null) {
			fMenu.dispose();
			fMenu= null;
		}
	}

	public Menu getMenu(Menu parent) {
		return null;
	}

	public Menu getMenu(Control parent) {
		if (fMenu != null) {
			fMenu.dispose();
		}
		fMenu= new Menu(parent);
		IJavaScriptElement[] elements= fHierarchyView.getHistoryEntries();
		addEntries(fMenu, elements);
		new MenuItem(fMenu, SWT.SEPARATOR);
		addActionToMenu(fMenu, new HistoryListAction(fHierarchyView));
		addActionToMenu(fMenu, new ClearHistoryAction(fHierarchyView));
		return fMenu;
	}
	
	private boolean addEntries(Menu menu, IJavaScriptElement[] elements) {
		boolean checked= false;
		
		int min= Math.min(elements.length, RESULTS_IN_DROP_DOWN);
		for (int i= 0; i < min; i++) {
			HistoryAction action= new HistoryAction(fHierarchyView, elements[i]);
			action.setChecked(elements[i].equals(fHierarchyView.getInputElement()));
			checked= checked || action.isChecked();
			addActionToMenu(menu, action);
		}
		
		
		return checked;
	}
	

	protected void addActionToMenu(Menu parent, Action action) {
		ActionContributionItem item= new ActionContributionItem(action);
		item.fill(parent, -1);
	}

	public void run() {
		(new HistoryListAction(fHierarchyView)).run();
	}
}
