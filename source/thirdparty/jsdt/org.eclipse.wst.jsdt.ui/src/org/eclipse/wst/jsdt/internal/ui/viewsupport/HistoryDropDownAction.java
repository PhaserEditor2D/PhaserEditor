/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Jesper Kamstrup Linnet (eclipse@kamstrup-linnet.dk) - initial API and implementation 
 * 			(report 36180: Callers/Callees view)
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.viewsupport;

import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IWorkbenchActionConstants;

/*package*/ class HistoryDropDownAction extends Action {

	private class HistoryAction extends Action {
		private final Object fElement;

		public HistoryAction(Object element, int accelerator) {
	        super("", AS_RADIO_BUTTON); //$NON-NLS-1$
			Assert.isNotNull(element);
			fElement= element;

			String label= fHistory.getText(element);
		    if (accelerator < 10) {
		    		//add the numerical accelerator
			    label= new StringBuffer().append('&').append(accelerator).append(' ').append(label).toString();
			}

			setText(label);
			setImageDescriptor(fHistory.getImageDescriptor(element));
		}

		public void run() {
			fHistory.setActiveEntry(fElement);
		}
	}

	private class HistoryMenuCreator implements IMenuCreator {

		public Menu getMenu(Menu parent) {
			return null;
		}

		public Menu getMenu(Control parent) {
			if (fMenu != null) {
				fMenu.dispose();
			}
			final MenuManager manager= new MenuManager();
			manager.setRemoveAllWhenShown(true);
			manager.addMenuListener(new IMenuListener() {
				public void menuAboutToShow(IMenuManager manager2) {
					List entries= fHistory.getHistoryEntries();
					boolean checkOthers= addEntryMenuItems(manager2, entries);
					
					manager2.add(new Separator());
					
					Action others= new HistoryListAction(fHistory);
					others.setChecked(checkOthers);
					manager2.add(others);
					
					Action clearAction= fHistory.getClearAction();
					if (clearAction != null) {
						manager2.add(clearAction);
					}
					
					manager2.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
					
					fHistory.addMenuEntries(manager);
				}
				
				private boolean addEntryMenuItems(IMenuManager manager2, List entries) {
					if (entries.isEmpty()) {
						return false;
					}
					
					boolean checkOthers= true;
					int min= Math.min(entries.size(), RESULTS_IN_DROP_DOWN);
					for (int i= 0; i < min; i++) {
						Object entry= entries.get(i);
						HistoryAction action= new HistoryAction(entry, i + 1);
						boolean check= entry.equals(fHistory.getCurrentEntry());
						action.setChecked(check);
						if (check)
							checkOthers= false;
						manager2.add(action);
					}
					return checkOthers;
				}
			});
			
			fMenu= manager.createContextMenu(parent);
			
			//workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=129973
			final Display display= parent.getDisplay();
			fMenu.addMenuListener(new MenuAdapter() {
				public void menuHidden(final MenuEvent e) {
					display.asyncExec(new Runnable() {
						public void run() {
							manager.removeAll();
							if (fMenu != null) {
								fMenu.dispose();
								fMenu= null;
							}
						}
					});
				}
			});
			return fMenu;
		}

		public void dispose() {
			fHistory= null;
		
			if (fMenu != null) {
				fMenu.dispose();
				fMenu= null;
			}
		}
	}

	public static final int RESULTS_IN_DROP_DOWN= 10;

	private ViewHistory fHistory;
	private Menu fMenu;

	public HistoryDropDownAction(ViewHistory history) {
		fHistory= history;
		fMenu= null;
		setMenuCreator(new HistoryMenuCreator());
		fHistory.configureHistoryDropDownAction(this);
	}

	public void run() {
		new HistoryListAction(fHistory).run();
	}
}
