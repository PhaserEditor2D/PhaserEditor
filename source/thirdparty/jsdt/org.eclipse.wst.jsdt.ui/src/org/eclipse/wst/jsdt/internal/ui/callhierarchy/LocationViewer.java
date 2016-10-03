/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Jesper Kamstrup Linnet (eclipse@kamstrup-linnet.dk) - initial API and implementation 
 *          (report 36180: Callers/Callees view)
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.callhierarchy;

import java.util.ArrayList;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLayoutData;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.actions.ActionFactory;



class LocationViewer extends TableViewer {
    private final String columnHeaders[] = {
        CallHierarchyMessages.LocationViewer_ColumnIcon_header,
        CallHierarchyMessages.LocationViewer_ColumnLine_header,
        CallHierarchyMessages.LocationViewer_ColumnInfo_header}; 
                                                
    private ColumnLayoutData columnLayouts[] = {
        new ColumnPixelData(18, false, true),
        new ColumnWeightData(60),
        new ColumnWeightData(300)};
    

    LocationViewer(Composite parent) {
        super(createTable(parent));

        setContentProvider(new ArrayContentProvider());
        setLabelProvider(new LocationLabelProvider());
        setInput(new ArrayList());

        createColumns();
    }

    /**
     * Creates the table control.
     */
    private static Table createTable(Composite parent) {
        return new Table(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.FULL_SELECTION);
    }
    
    private void createColumns() {
        TableLayout layout = new TableLayout();
        getTable().setLayout(layout);
        getTable().setHeaderVisible(true);
        for (int i = 0; i < columnHeaders.length; i++) {
            layout.addColumnData(columnLayouts[i]);
            TableColumn tc = new TableColumn(getTable(), SWT.NONE,i);
            tc.setResizable(columnLayouts[i].resizable);
            tc.setText(columnHeaders[i]);
        }
    }

    /**
     * Attaches a contextmenu listener to the tree
     */
    void initContextMenu(IMenuListener menuListener, String popupId, IWorkbenchPartSite viewSite) {
        MenuManager menuMgr= new MenuManager();
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(menuListener);
        Menu menu= menuMgr.createContextMenu(getControl());
        getControl().setMenu(menu);
        viewSite.registerContextMenu(popupId, menuMgr, this);
    }
    
    /**
     * Initializes and returns the Copy action for the location viewer.
     */
    LocationCopyAction initCopyAction(final IViewSite viewSite, final Clipboard clipboard) {
    	final LocationCopyAction copyAction= new LocationCopyAction(viewSite, clipboard, this);
    	
        getTable().addFocusListener(new FocusListener() {
        	IAction fViewCopyHandler;
			public void focusLost(FocusEvent e) {
				if (fViewCopyHandler != null) {
					IActionBars actionBars= viewSite.getActionBars();
					actionBars.setGlobalActionHandler(ActionFactory.COPY.getId(), fViewCopyHandler);
					actionBars.updateActionBars();
					fViewCopyHandler= null;
				}
			}
			
			public void focusGained(FocusEvent e) {
				IActionBars actionBars= viewSite.getActionBars();
				fViewCopyHandler= actionBars.getGlobalActionHandler(ActionFactory.COPY.getId());
				actionBars.setGlobalActionHandler(ActionFactory.COPY.getId(), copyAction);
				actionBars.updateActionBars();
			}
		});
        
        return copyAction;
    }

    /**
     * 
     */
    void clearViewer() {
        setInput(""); //$NON-NLS-1$
    }
}
