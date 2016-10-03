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

package org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths.newsourcepage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;

/**
 * Drop down action for toolbars containing <code>BuildpathModifierAction</code>s.
 * The drop down action manages a list of actions that are displayed when invoking 
 * the drop down. If there is at least one valid action, then the drop down action 
 * itself will also be valid and invoking run will delegate the call to the 
 * first valid action in the list.
 */
public class ClasspathModifierDropDownAction extends BuildpathModifierAction implements IMenuCreator {
    
	/** The menu to be populated with items*/
    private Menu fMenu;
    private List fActions;
    //The action to execute on run iff enabled
	private BuildpathModifierAction fFirstValidAction;
    
    /**
     * Create a drop down action using the same descriptors as the provided action, but it's on 
     * tool tip text. The action will automatically be put in the list of actions that are 
     * managed by this drop down menu.
     */
    public ClasspathModifierDropDownAction() {
        super(null, null, BuildpathModifierAction.DROP_DOWN_ACTION, IAction.AS_DROP_DOWN_MENU);
        
        fActions= new ArrayList();
        fFirstValidAction= null;
        
        setText(""); //$NON-NLS-1$
        setToolTipText(""); //$NON-NLS-1$
    }
    
	/**
     * {@inheritDoc}
     */
    public String getDetailedDescription() {
    	if (fFirstValidAction != null) {
    		return fFirstValidAction.getDetailedDescription();
    	} else if (fActions.size() > 0) {
    		return ((BuildpathModifierAction)fActions.get(0)).getDetailedDescription();
    	} else {
    		return ""; //$NON-NLS-1$
    	}
    }
        
    /**
     * Runs the first action of the list of managed actions that is valid.
     */
    public void run() {
    	fFirstValidAction.run();
    }

    public IMenuCreator getMenuCreator() {
        return this;
    }

    public Menu getMenu(Control parent) {
        if (fMenu != null) {
            fMenu.dispose();
        }
        fMenu = new Menu(parent);
        createEntries(fMenu);
        return fMenu;

    }

    public Menu getMenu(Menu parent) {
        return fMenu;
    }
    
    /**
     * Add dynamically an action to the drop down menu.
     * 
     * @param action the action to be added
     */
    public void addAction(BuildpathModifierAction action) {
        fActions.add(action);
        update();
    }
    
    /**
     * Remove an action from the drop down menu
     *  
     * @param action the action to be removed
     */
    public void removeAction(BuildpathModifierAction action) {
        fActions.remove(action);
        update();
    }
    
    /**
     * Populate the menu with the given action item
     *  
     * @param parent the menu to add an action for
     * @param action the action to be added
     */
    private void addActionToMenu(Menu parent, IAction action) {
        ActionContributionItem item = new ActionContributionItem(action);
        item.fill(parent, -1);
    }
    
    /**
     * Fill the menu with all actions
     * 
     * @param menu the menu to be populated
     */
    private void createEntries(Menu menu) {
        for(int i= 0; i < fActions.size(); i++) {
            IAction action= (IAction)fActions.get(i);
            addActionToMenu(menu, action);
        }
    }
    
    public void dispose() {
        if (fMenu != null) {
            fMenu.dispose();
            fMenu = null;
        }
    }

	/**
     * {@inheritDoc}
     */
    protected boolean canHandle(IStructuredSelection elements) {
    	update();
    	return fFirstValidAction != null;
    }

	private void update() {
		for (Iterator iterator= fActions.iterator(); iterator.hasNext();) {
	        BuildpathModifierAction action= (BuildpathModifierAction)iterator.next();
	        if (action.isEnabled()) {
	        	if (action != fFirstValidAction) {
	        		updateButton(action);
	        	}
	        	fFirstValidAction= action;
	        	return;
	        }
        }
		if (fFirstValidAction != null) {
			if (fActions.size() > 0) {
				updateButton((BuildpathModifierAction)fActions.get(0));
			} else {
				updateButton(this);
			}
		}
    	fFirstValidAction= null;
    }

	private void updateButton(BuildpathModifierAction action) {
		setImageDescriptor(action.getImageDescriptor());
		setDisabledImageDescriptor(action.getDisabledImageDescriptor());
		setText(action.getText());
		setToolTipText(action.getToolTipText());
    }
}
