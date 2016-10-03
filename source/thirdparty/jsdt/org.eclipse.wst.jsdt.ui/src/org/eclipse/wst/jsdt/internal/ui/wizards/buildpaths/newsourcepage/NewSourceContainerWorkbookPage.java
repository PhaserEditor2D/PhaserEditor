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
import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.corext.buildpath.BuildpathDelta;
import org.eclipse.wst.jsdt.internal.corext.buildpath.ClasspathModifier;
import org.eclipse.wst.jsdt.internal.corext.buildpath.IBuildpathModifierListener;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.preferences.ScrolledPageContent;
import org.eclipse.wst.jsdt.internal.ui.util.PixelConverter;
import org.eclipse.wst.jsdt.internal.ui.util.ViewerPane;
import org.eclipse.wst.jsdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths.BuildPathBasePage;
import org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths.BuildPathsBlock;
import org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths.CPListElement;
import org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths.CPListElementAttribute;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.ListDialogField;

public class NewSourceContainerWorkbookPage extends BuildPathBasePage implements IBuildpathModifierListener {
    
    public static final String OPEN_SETTING= "org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths.NewSourceContainerPage.openSetting";  //$NON-NLS-1$
    
    private ListDialogField fClassPathList;
    private HintTextGroup fHintTextGroup;
    private DialogPackageExplorer fPackageExplorer;
	private DialogPackageExplorerActionGroup fActionGroup;
	
	private IJavaScriptProject fJavaScriptProject;

	private final IRunnableContext fContext;

    /**
     * Constructor of the <code>NewSourceContainerWorkbookPage</code> which consists of 
     * a tree representing the project, a toolbar with the available actions, an area 
     * containing hyperlinks that perform the same actions as those in the toolbar but 
     * additionally with some short description.
     * 
     * @param classPathList
     * @param context a runnable context, can be <code>null</code>
     * @param buildPathsBlock 
     */
    public NewSourceContainerWorkbookPage(ListDialogField classPathList, IRunnableContext context, BuildPathsBlock buildPathsBlock) {
        fClassPathList= classPathList;
		fContext= context;

		fPackageExplorer= new DialogPackageExplorer();
		fHintTextGroup= new HintTextGroup();
     }
    
    /**
     * Initialize the controls displaying
     * the content of the java project and saving 
     * the '.classpath' and '.project' file.
     * 
     * Must be called before initializing the 
     * controls using <code>getControl(Composite)</code>.
     * 
     * @param javaProject the current java project
     */
    public void init(IJavaScriptProject javaProject) {
		fJavaScriptProject= javaProject;
		fPackageExplorer.addPostSelectionChangedListener(fHintTextGroup);
	    fActionGroup.getResetAllAction().setBreakPoint(javaProject);

		if (Display.getCurrent() != null) {
			doUpdateUI();
		} else {
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					doUpdateUI();
				}
			});
		}
    }
    
	private void doUpdateUI() {
        fPackageExplorer.setInput(fJavaScriptProject);
    }
    
    public void dispose() {
    	if (fActionGroup != null) {
    		fActionGroup.removeBuildpathModifierListener(this);
    		fActionGroup= null;
    	}
    	fPackageExplorer.removePostSelectionChangedListener(fHintTextGroup);
    	fPackageExplorer.dispose();
    }
     
    /**
     * Initializes controls and return composite containing
     * these controls.
     * 
     * Before calling this method, make sure to have 
     * initialized this instance with a java project 
     * using <code>init(IJavaScriptProject)</code>.
     * 
     * @param parent the parent composite
     * @return composite containing controls
     * 
     * @see #init(IJavaScriptProject)
     */
    public Control getControl(Composite parent) {
        final int[] sashWeight= {60};
        final IPreferenceStore preferenceStore= JavaScriptPlugin.getDefault().getPreferenceStore();
        preferenceStore.setDefault(OPEN_SETTING, true);
        
        // ScrolledPageContent is needed for resizing on expand the expandable composite
        ScrolledPageContent scrolledContent = new ScrolledPageContent(parent);
        Composite body= scrolledContent.getBody();
        body.setLayout(new GridLayout());
        
        final SashForm sashForm= new SashForm(body, SWT.VERTICAL | SWT.NONE);
        sashForm.setFont(sashForm.getFont());
        
        ViewerPane pane= new ViewerPane(sashForm, SWT.BORDER | SWT.FLAT);
        pane.setContent(fPackageExplorer.createControl(pane));
		fPackageExplorer.setContentProvider();
        
        final ExpandableComposite excomposite= new ExpandableComposite(sashForm, SWT.NONE, ExpandableComposite.TWISTIE | ExpandableComposite.CLIENT_INDENT);
        excomposite.setFont(sashForm.getFont());
        excomposite.setText(NewWizardMessages.NewSourceContainerWorkbookPage_HintTextGroup_title);
        final boolean isExpanded= preferenceStore.getBoolean(OPEN_SETTING);
        excomposite.setExpanded(isExpanded);
        excomposite.addExpansionListener(new ExpansionAdapter() {
                       public void expansionStateChanged(ExpansionEvent e) {
                           ScrolledPageContent parentScrolledComposite= getParentScrolledComposite(excomposite);
                           if (parentScrolledComposite != null) {
                              boolean expanded= excomposite.isExpanded();
                              parentScrolledComposite.reflow(true);
                              adjustSashForm(sashWeight, sashForm, expanded);
                              preferenceStore.setValue(OPEN_SETTING, expanded);
                           }
                       }
                 });
        
        excomposite.setClient(fHintTextGroup.createControl(excomposite));
		
        fActionGroup= new DialogPackageExplorerActionGroup(fHintTextGroup, fContext, fPackageExplorer, this);
		fActionGroup.addBuildpathModifierListener(this);
          
        // Create toolbar with actions on the left
        ToolBarManager tbm= fActionGroup.createLeftToolBarManager(pane);
        pane.setTopCenter(null);
        pane.setTopLeft(tbm.getControl());
        
        // Create toolbar with help on the right
        tbm= fActionGroup.createLeftToolBar(pane);
        pane.setTopRight(tbm.getControl());
        
        fHintTextGroup.setActionGroup(fActionGroup);
        fPackageExplorer.setActionGroup(fActionGroup);
        
		sashForm.setWeights(new int[] {60, 40});
		adjustSashForm(sashWeight, sashForm, excomposite.isExpanded());
		GridData gd= new GridData(GridData.FILL_BOTH);
		PixelConverter converter= new PixelConverter(parent);
		gd.heightHint= converter.convertHeightInCharsToPixels(20);
		sashForm.setLayoutData(gd);
        
        parent.layout(true);

        return scrolledContent;
    }
    
    /**
     * Adjust the size of the sash form.
     * 
     * @param sashWeight the weight to be read or written
     * @param sashForm the sash form to apply the new weights to
     * @param isExpanded <code>true</code> if the expandable composite is 
     * expanded, <code>false</code> otherwise
     */
    private void adjustSashForm(int[] sashWeight, SashForm sashForm, boolean isExpanded) {
        if (isExpanded) {
            int upperWeight= sashWeight[0];
            sashForm.setWeights(new int[]{upperWeight, 100 - upperWeight});
        }
        else {
            // TODO Dividing by 10 because of https://bugs.eclipse.org/bugs/show_bug.cgi?id=81939
            sashWeight[0]= sashForm.getWeights()[0] / 10;
            sashForm.setWeights(new int[]{95, 5});
        }
        sashForm.layout(true);
    }
    
    /**
     * Get the scrolled page content of the given control by 
     * traversing the parents.
     * 
     * @param control the control to get the scrolled page content for 
     * @return the scrolled page content or <code>null</code> if none found
     */
    private ScrolledPageContent getParentScrolledComposite(Control control) {
       Control parent= control.getParent();
       while (!(parent instanceof ScrolledPageContent)) {
           parent= parent.getParent();
       }
       if (parent instanceof ScrolledPageContent) {
           return (ScrolledPageContent) parent;
       }
       return null;
   }
    
    /* (non-Javadoc)
     * @see org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths.BuildPathBasePage#getSelection()
     */
    public List getSelection() {
        List selectedList= new ArrayList();
        
        IJavaScriptProject project= fJavaScriptProject;
        try {
            List list= ((StructuredSelection)fPackageExplorer.getSelection()).toList();
            List existingEntries= ClasspathModifier.getExistingEntries(project);
        
            for(int i= 0; i < list.size(); i++) {
                Object obj= list.get(i);
                if (obj instanceof IPackageFragmentRoot) {
                    IPackageFragmentRoot element= (IPackageFragmentRoot)obj;
                    CPListElement cpElement= ClasspathModifier.getClasspathEntry(existingEntries, element); 
                    selectedList.add(cpElement);
                }
                else if (obj instanceof IJavaScriptProject) {
                    IIncludePathEntry entry= ClasspathModifier.getClasspathEntryFor(project.getPath(), project, IIncludePathEntry.CPE_SOURCE);
                    if (entry == null)
                        continue;
                    CPListElement cpElement= CPListElement.createFromExisting(entry, project);
                    selectedList.add(cpElement);
                }
            }
        } catch (JavaScriptModelException e) {
            return new ArrayList();
        }
        return selectedList;
    }

    /* (non-Javadoc)
     * @see org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths.BuildPathBasePage#setSelection(java.util.List)
     */
    public void setSelection(List selection, boolean expand) {
		// page switch
		
        if (selection.size() == 0)
            return;
	    
		List cpEntries= new ArrayList();
		
		for (int i= 0; i < selection.size(); i++) {
			Object obj= selection.get(i);
			if (obj instanceof CPListElement) {
				CPListElement element= (CPListElement) obj;
				if (element.getEntryKind() == IIncludePathEntry.CPE_SOURCE) {
					cpEntries.add(element);
				}
			} else if (obj instanceof CPListElementAttribute) {
				CPListElementAttribute attribute= (CPListElementAttribute)obj;
				CPListElement element= attribute.getParent();
				if (element.getEntryKind() == IIncludePathEntry.CPE_SOURCE) {
					cpEntries.add(element);
				}
			}
		}
		
        // refresh classpath
        List list= fClassPathList.getElements();
        IIncludePathEntry[] entries= new IIncludePathEntry[list.size()];
        for(int i= 0; i < list.size(); i++) {
            CPListElement entry= (CPListElement) list.get(i);
            entries[i]= entry.getClasspathEntry(); 
        }
        try {
			fJavaScriptProject.setRawIncludepath(entries, null);
        } catch (JavaScriptModelException e) {
            JavaScriptPlugin.log(e);
        }
        
        fPackageExplorer.setSelection(cpEntries);
    }

    /* (non-Javadoc)
     * @see org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths.BuildPathBasePage#isEntryKind(int)
     */
    public boolean isEntryKind(int kind) {
        return kind == IIncludePathEntry.CPE_SOURCE;
    }
    
    /**
     * Update <code>fClassPathList</code>.
     */
    public void buildpathChanged(BuildpathDelta delta) {
        fClassPathList.setElements(Arrays.asList(delta.getNewEntries()));
    }

	/**
     * {@inheritDoc}
     */
    public void setFocus() {
    	fPackageExplorer.getViewerControl().setFocus();
    }

	public IJavaScriptProject getJavaScriptProject() {
		return fJavaScriptProject;
	}
}
