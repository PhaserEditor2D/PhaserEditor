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

package org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths.newsourcepage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.preferences.ScrolledPageContent;
import org.eclipse.wst.jsdt.internal.ui.util.PixelConverter;
import org.eclipse.wst.jsdt.internal.ui.wizards.NewWizardMessages;

/**
 * Displays a set of available links to modify or adjust the project.
 * The links contain a short description about the consequences of 
 * this action.
 * 
 * The content depends on the selection made on the project.
 * If selection changes, then the <code>HintTextGroup</code> will be 
 * notified through the <code>IPackageExplorerActionListener</code> interface. 
 */
public final class HintTextGroup implements ISelectionChangedListener {
	    
    private Composite fTopComposite;
    private DialogPackageExplorerActionGroup fActionGroup;
    private List fNewFolders;
    private HashMap fImageMap;
    
    public HintTextGroup() {
        fNewFolders= new ArrayList();
        fImageMap= new HashMap();
    }
    
    public Composite createControl(Composite parent) {
        fTopComposite= new Composite(parent, SWT.NONE);
        fTopComposite.setFont(parent.getFont());
        
        GridData gridData= new GridData(GridData.FILL_BOTH);
        PixelConverter converter= new PixelConverter(parent);
        gridData.heightHint= converter.convertHeightInCharsToPixels(12);
        gridData.widthHint= converter.convertWidthInCharsToPixels(25);
        GridLayout gridLayout= new GridLayout();
        gridLayout.marginWidth= 0;//-converter.convertWidthInCharsToPixels(2);
        gridLayout.marginHeight= 0;//= -4;
        fTopComposite.setLayout(gridLayout);
        fTopComposite.setLayoutData(gridData);
        fTopComposite.setData(null);
        fTopComposite.addDisposeListener(new DisposeListener() {
            public void widgetDisposed(DisposeEvent e) {
                Collection collection= fImageMap.values();
                Iterator iterator= collection.iterator();
                while(iterator.hasNext()) {
                    Image image= (Image)iterator.next();
                    image.dispose();
                }
            }
        });
        return fTopComposite;
    }
    
    private Shell getShell() {
        return JavaScriptPlugin.getActiveWorkbenchShell();
    }
    
    /**
     * An action group managing the actions needed by 
     * the <code>HintTextGroup</code>.
     * 
     * Note: This method has to be called on initialization.
     * Calling this method in the constructor is not 
     * possible because the actions might need a reference to 
     * this class.
     * 
     * @param actionGroup the action group containing the necessary 
     * actions
     * 
     * @see DialogPackageExplorerActionGroup
     */
    public void setActionGroup(DialogPackageExplorerActionGroup actionGroup) {
        fActionGroup= actionGroup;
    }
    
    /**
     * Creates a form text.
     * 
     * @param parent the parent to put the form text on
     * @param text the form text to be displayed
     * @return the created form text
     * 
     * @see FormToolkit#createFormText(org.eclipse.swt.widgets.Composite, boolean)
     */
    private FormText createFormText(Composite parent, String text) {
        FormToolkit toolkit= new FormToolkit(getShell().getDisplay());
        try {
        	FormText formText= toolkit.createFormText(parent, true);
        	formText.setFont(parent.getFont());
			try {
			    formText.setText(text, true, false);
			} catch (IllegalArgumentException e) {
			    formText.setText(e.getMessage(), false, false);
			    JavaScriptPlugin.log(e);
			}
			formText.marginHeight= 2;
			formText.marginWidth= 0;
			formText.setBackground(null);
			formText.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
			return formText;
		} finally {
	        toolkit.dispose();
		}
    }
    
    /**
     * Create a label with a hyperlink and a picture.
     * 
     * @param parent the parent widget of the label
     * @param text the text of the label
     * @param action the action to be executed if the hyperlink is activated
     */
    private void createLabel(Composite parent, String text, final BuildpathModifierAction action) {
        FormText formText= createFormText(parent, text);
        Image image= (Image)fImageMap.get(action.getId());
        if (image == null) {
            image= action.getImageDescriptor().createImage();
            fImageMap.put(action.getId(), image);
        }
        formText.setImage("defaultImage", image); //$NON-NLS-1$
        formText.addHyperlinkListener(new HyperlinkAdapter() {

            public void linkActivated(HyperlinkEvent e) {
                action.run();
            }
            
        });
    }
    
    /**
     * Handle folder creation. This includes:
     * <li>Set the selection of the <code>fPackageExplorer</code>
     * to the result object, unless the result object is <code>
     * null</code></li>
     * <li>Add the created folder to the list of new folders</li>
     * 
     * In this case, the list consists only of one element on which the 
     * new folder has been created
     *  
     * @param result a list with only one element to be selected by the 
     * <code>fPackageExplorer</code>, or an empty list if creation was 
     * aborted
     */
    void handleFolderCreation(List result) {
        if (result.size() == 1) {
            try {
	            fNewFolders.add(((IPackageFragmentRoot)result.get(0)).getCorrespondingResource());
            } catch (JavaScriptModelException e) {
	            JavaScriptPlugin.log(e);
            }
        }
    }
    
    public List getCreatedResources() {
    	return fNewFolders;
    }
        
	public void resetCreatedResources() {
		fNewFolders.clear();
	}
	
	/**
     * {@inheritDoc}
     */
    public void selectionChanged(SelectionChangedEvent event) {
    	if (event.getSelection() instanceof StructuredSelection) {
    		handlePostSelectionChange((StructuredSelection)event.getSelection());
    	} else {
    		handlePostSelectionChange(StructuredSelection.EMPTY);
    	}
 	}

    private void handlePostSelectionChange(StructuredSelection selection) {
    	
    	BuildpathModifierAction[] actions= fActionGroup.getHintTextGroupActions();
    	String[] descriptions= new String[actions.length];
    	for (int i= 0; i < actions.length; i++) {
	        descriptions[i]= actions[i].getDetailedDescription();
        }
    	
        // Get the child composite of the top composite
        Composite childComposite= (Composite)fTopComposite.getData();
        
        // Dispose old composite (if necessary)
        if (childComposite != null && childComposite.getParent() != null)
            childComposite.getParent().dispose();
      
    	PixelConverter converter= new PixelConverter(fTopComposite);
    	
        // Create new composite
        ScrolledPageContent spc= new ScrolledPageContent(fTopComposite, SWT.V_SCROLL);
        spc.getVerticalBar().setIncrement(5);
        
        GridData gridData= new GridData(GridData.FILL_BOTH);
        
        gridData.heightHint= converter.convertHeightInCharsToPixels(12);
        gridData.widthHint= converter.convertWidthInCharsToPixels(25);
        spc.setLayoutData(gridData);
        
        childComposite= spc.getBody();
        TableWrapLayout tableWrapLayout= new TableWrapLayout();
		tableWrapLayout.leftMargin= 0;
		tableWrapLayout.rightMargin= 0;
		childComposite.setLayout(tableWrapLayout);
		gridData= new GridData(GridData.FILL_BOTH);
        gridData.heightHint= converter.convertHeightInCharsToPixels(12);
        gridData.widthHint= converter.convertWidthInCharsToPixels(25);
        childComposite.setLayoutData(gridData);
        
		fTopComposite.setData(childComposite);
        
        if (noContextHelpAvailable(actions)) {
            String noAction= noAction(selection);
            createFormText(childComposite, Messages.format(NewWizardMessages.HintTextGroup_NoAction, noAction)); 
            fTopComposite.layout(true);
            return;
        }
        
        for (int i= 0; i < actions.length; i++) {
        	createLabel(childComposite, descriptions[i], actions[i]);
        }
        
        fTopComposite.layout(true);
    }
    
    private String noAction(ISelection selection) {
    	if (selection instanceof StructuredSelection) {
    		return noAction(((StructuredSelection)selection).toList());
    	} else {
    		return noAction(Collections.EMPTY_LIST);
    	}
    }

	private String noAction(List selectedElements) {
		if (selectedElements.size() == 0)
			return NewWizardMessages.PackageExplorerActionGroup_NoAction_NullSelection;
		
		if (selectedElements.size() == 1)
			return NewWizardMessages.PackageExplorerActionGroup_NoAction_NoReason;
		
		return NewWizardMessages.PackageExplorerActionGroup_NoAction_MultiSelection;
    }
	    
    /**
     * Check if for the current type of selection, no context specific actions can 
     * be applied. Note: this does not mean, that there are NO actions available at all.<p>
     * 
     * For example: if the default package is selected, there is no specific action for this kind 
     * of selection as no operations are allowed on the default package. Nevertheless, the 
     * <code>PackageExplorerActionEvent</code> will return at least one action that allows to 
     * link to an existing folder in the file system, but this operation is always available 
     * and does not add any supporting information to the current selection. Therefore, 
     * it can be filtered and the correct answer to the user is that there is no specific 
     * action for the default package.
     * 
     * @param actions an array of provided actions
     * @return <code>true</code> if there is at least one action that allows context 
     * sensitive operations, <code>false</code> otherwise.
     */
    private boolean noContextHelpAvailable(BuildpathModifierAction[] actions) {
        if (actions.length == 0)
            return true;
        if (actions.length == 1) {
            int id= Integer.parseInt(actions[0].getId());
            if (id == BuildpathModifierAction.CREATE_LINK)
                return true;
        }
        if (actions.length == 2) {
            int idLink= Integer.parseInt(actions[0].getId());
            int idReset= Integer.parseInt(actions[1].getId());
            if (idReset == BuildpathModifierAction.RESET_ALL && 
                idLink == BuildpathModifierAction.CREATE_LINK)
                return true;
        }
        return false;
    }
}
