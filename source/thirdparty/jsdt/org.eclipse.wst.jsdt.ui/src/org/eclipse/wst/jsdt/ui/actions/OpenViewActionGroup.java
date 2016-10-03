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
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.actions.ContributionItemFactory;
import org.eclipse.ui.dialogs.PropertyDialogAction;
import org.eclipse.ui.keys.IBindingService;
import org.eclipse.ui.part.Page;
import org.eclipse.ui.texteditor.IWorkbenchActionDefinitionIds;
import org.eclipse.wst.jsdt.internal.ui.actions.ActionMessages;
import org.eclipse.wst.jsdt.internal.ui.callhierarchy.OpenCallHierarchyAction;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.wst.jsdt.ui.IContextMenuConstants;

/**
 * Action group that adds actions to open a new JDT view part or an external 
 * viewer to a context menu and the global menu bar.
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
public class OpenViewActionGroup extends ActionGroup {

    private boolean fEditorIsOwner;
	private boolean fIsTypeHiararchyViewerOwner;
    private boolean fIsCallHiararchyViewerOwner;
    
	private ISelectionProvider fSelectionProvider;

	private OpenSuperImplementationAction fOpenSuperImplementation;
	private OpenExternalJavadocAction fOpenExternalJavadoc;
	private OpenTypeHierarchyAction fOpenTypeHierarchy;
    private OpenCallHierarchyAction fOpenCallHierarchy;
	private PropertyDialogAction fOpenPropertiesDialog;
	
	private boolean fShowOpenPropertiesAction= true;
	private boolean fShowShowInMenu= true;
	
	/**
	 * Creates a new <code>OpenActionGroup</code>. The group requires
	 * that the selection provided by the page's selection provider is 
	 * of type {@link IStructuredSelection}.
	 * 
	 * @param page the page that owns this action group
	 */
	public OpenViewActionGroup(Page page) {
		createSiteActions(page.getSite(), null);
	}
	
	/**
	 * Creates a new <code>OpenActionGroup</code>. The group requires
	 * that the selection provided by the given selection provider is 
	 * of type {@link IStructuredSelection}.
	 * 
	 * @param page the page that owns this action group
	 * @param selectionProvider the selection provider used instead of the
	 *  page selection provider.
	 * 
	 * 
	 */
	public OpenViewActionGroup(Page page, ISelectionProvider selectionProvider) {
		createSiteActions(page.getSite(), selectionProvider);
	}
	
	/**
	 * Creates a new <code>OpenActionGroup</code>. The group requires
	 * that the selection provided by the part's selection provider is 
	 * of type {@link IStructuredSelection}.
	 * 
	 * @param part the view part that owns this action group
	 */
	public OpenViewActionGroup(IViewPart part) {
		this(part, null);
	}
	
	/**
	 * Creates a new <code>OpenActionGroup</code>. The group requires
	 * that the selection provided by the given selection provider is of type
	 * {@link IStructuredSelection}.
	 * 
	 * @param part the view part that owns this action group
	 * @param selectionProvider the selection provider used instead of the
	 *  page selection provider.
	 *  
	 * 
	 */
	public OpenViewActionGroup(IViewPart part, ISelectionProvider selectionProvider) {
		createSiteActions(part.getSite(), selectionProvider);
		// we do a name check here to avoid class loading. 
		String partName= part.getClass().getName();
		fIsTypeHiararchyViewerOwner= "org.eclipse.wst.jsdt.internal.ui.typehierarchy.TypeHierarchyViewPart".equals(partName); //$NON-NLS-1$
		fIsCallHiararchyViewerOwner= "org.eclipse.wst.jsdt.internal.ui.callhierarchy.CallHierarchyViewPart".equals(partName); //$NON-NLS-1$
	}
	
	/**
	 * Creates a new <code>OpenActionGroup</code>. The group requires
	 * that the selection provided by the given selection provider is of type 
	 * {@link IStructuredSelection}.
	 * 
	 * @param site the site that will own the action group.
	 * @param selectionProvider the selection provider used instead of the
	 *  page selection provider.
	 *  
	 * 
	 */
	public OpenViewActionGroup(IWorkbenchSite site, ISelectionProvider selectionProvider) {
		createSiteActions(site, selectionProvider);
	}
	
	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * @param part the editor part
	 */
	public OpenViewActionGroup(JavaEditor part) {
		fEditorIsOwner= true;
		fShowShowInMenu= false;

		fOpenSuperImplementation= new OpenSuperImplementationAction(part);
		fOpenSuperImplementation.setActionDefinitionId(IJavaEditorActionDefinitionIds.OPEN_SUPER_IMPLEMENTATION);
		part.setAction("OpenSuperImplementation", fOpenSuperImplementation); //$NON-NLS-1$

		fOpenExternalJavadoc= new OpenExternalJavadocAction(part);
		fOpenExternalJavadoc.setActionDefinitionId(IJavaEditorActionDefinitionIds.OPEN_EXTERNAL_JAVADOC);
		part.setAction("OpenExternalJavadoc", fOpenExternalJavadoc); //$NON-NLS-1$

		fOpenTypeHierarchy= new OpenTypeHierarchyAction(part);
		fOpenTypeHierarchy.setActionDefinitionId(IJavaEditorActionDefinitionIds.OPEN_TYPE_HIERARCHY);
		part.setAction("OpenTypeHierarchy", fOpenTypeHierarchy); //$NON-NLS-1$

        fOpenCallHierarchy= new OpenCallHierarchyAction(part);
        fOpenCallHierarchy.setActionDefinitionId(IJavaEditorActionDefinitionIds.OPEN_CALL_HIERARCHY);
        part.setAction("OpenCallHierarchy", fOpenCallHierarchy); //$NON-NLS-1$

		initialize(part.getEditorSite().getSelectionProvider());
	}
	
	/**
	 * Specifies if this action group also contains the 'Properties' action ({@link PropertyDialogAction}).
	 * By default, the action is contained in the group.
	 * 
	 * @param enable If set, the 'Properties' action is part of this action group
	 * 
	 */
	public void containsOpenPropertiesAction(boolean enable) {
		fShowOpenPropertiesAction= enable;
	}
	
	/**
	 * Specifies if this action group also contains the 'Show In' menu (See {@link ContributionItemFactory#VIEWS_SHOW_IN}).
	 * By default, the action is  contained in the group except for editors.
	 * 
	 * @param enable If set, the 'Show In' menu is part of this action group
	 * 
	 */
	public void containsShowInMenu(boolean enable) {
		fShowShowInMenu= enable;
	}
	
	private void createSiteActions(IWorkbenchSite site, ISelectionProvider specialProvider) {
		fOpenSuperImplementation= new OpenSuperImplementationAction(site);
		fOpenSuperImplementation.setActionDefinitionId(IJavaEditorActionDefinitionIds.OPEN_SUPER_IMPLEMENTATION);
		fOpenSuperImplementation.setSpecialSelectionProvider(specialProvider);
		
		fOpenExternalJavadoc= new OpenExternalJavadocAction(site);
		fOpenExternalJavadoc.setActionDefinitionId(IJavaEditorActionDefinitionIds.OPEN_EXTERNAL_JAVADOC);
		fOpenExternalJavadoc.setSpecialSelectionProvider(specialProvider);

		fOpenTypeHierarchy= new OpenTypeHierarchyAction(site);
		fOpenTypeHierarchy.setActionDefinitionId(IJavaEditorActionDefinitionIds.OPEN_TYPE_HIERARCHY);
		fOpenTypeHierarchy.setSpecialSelectionProvider(specialProvider);

		fOpenCallHierarchy= new OpenCallHierarchyAction(site);
        fOpenCallHierarchy.setActionDefinitionId(IJavaEditorActionDefinitionIds.OPEN_CALL_HIERARCHY);
        fOpenCallHierarchy.setSpecialSelectionProvider(specialProvider);

        ISelectionProvider provider= specialProvider != null ? specialProvider : site.getSelectionProvider();
        
        fOpenPropertiesDialog= new PropertyDialogAction(site, provider);
        fOpenPropertiesDialog.setActionDefinitionId(IWorkbenchActionDefinitionIds.PROPERTIES);
		
        initialize(provider);
	}

	private void initialize(ISelectionProvider provider) {
		fSelectionProvider= provider;
		ISelection selection= provider.getSelection();
		fOpenSuperImplementation.update(selection);
		fOpenExternalJavadoc.update(selection);
		fOpenTypeHierarchy.update(selection);
        fOpenCallHierarchy.update(selection);
		if (!fEditorIsOwner) {
			if (fShowOpenPropertiesAction) {
				if (selection instanceof IStructuredSelection) {
					fOpenPropertiesDialog.selectionChanged((IStructuredSelection) selection);
				} else {
					fOpenPropertiesDialog.selectionChanged(selection);
				}
			}
			provider.addSelectionChangedListener(fOpenSuperImplementation);
			provider.addSelectionChangedListener(fOpenExternalJavadoc);
			provider.addSelectionChangedListener(fOpenTypeHierarchy);
            provider.addSelectionChangedListener(fOpenCallHierarchy);
			// no need to register the open properties dialog action since it registers itself
		}
	}

	/* (non-Javadoc)
	 * Method declared in ActionGroup
	 */
	public void fillActionBars(IActionBars actionBar) {
		super.fillActionBars(actionBar);
		setGlobalActionHandlers(actionBar);
	}
	
	/* (non-Javadoc)
	 * Method declared in ActionGroup
	 */
	public void fillContextMenu(IMenuManager menu) {
		super.fillContextMenu(menu);
		if (!fIsTypeHiararchyViewerOwner)
			appendToGroup(menu, fOpenTypeHierarchy);
        if (!fIsCallHiararchyViewerOwner)
            appendToGroup(menu, fOpenCallHierarchy);
        
        if (fShowShowInMenu) {
			MenuManager showInSubMenu= new MenuManager(getShowInMenuLabel());
			IWorkbenchWindow workbenchWindow= fOpenSuperImplementation.getSite().getWorkbenchWindow();
			showInSubMenu.add(ContributionItemFactory.VIEWS_SHOW_IN.create(workbenchWindow));
			menu.appendToGroup(IContextMenuConstants.GROUP_OPEN, showInSubMenu);
        }

		IStructuredSelection selection= getStructuredSelection();
		if (fShowOpenPropertiesAction && selection != null)
			menu.appendToGroup(IContextMenuConstants.GROUP_PROPERTIES, fOpenPropertiesDialog);
	}
	
	private String getShowInMenuLabel() {
		String keyBinding= null;
		
		IBindingService bindingService= (IBindingService) PlatformUI.getWorkbench().getAdapter(IBindingService.class);
		if (bindingService != null)
			keyBinding= bindingService.getBestActiveBindingFormattedFor("org.eclipse.ui.navigate.showInQuickMenu"); //$NON-NLS-1$
		
		if (keyBinding == null)
			keyBinding= ""; //$NON-NLS-1$
		
		return ActionMessages.OpenViewActionGroup_showInAction_label + '\t' + keyBinding;
	}

	/*
	 * @see ActionGroup#dispose()
	 */
	public void dispose() {
		fSelectionProvider.removeSelectionChangedListener(fOpenSuperImplementation);
		fSelectionProvider.removeSelectionChangedListener(fOpenExternalJavadoc);
		fSelectionProvider.removeSelectionChangedListener(fOpenTypeHierarchy);
		fSelectionProvider.removeSelectionChangedListener(fOpenCallHierarchy);
		super.dispose();
	}
	
	private void setGlobalActionHandlers(IActionBars actionBars) {
		actionBars.setGlobalActionHandler(JdtActionConstants.OPEN_SUPER_IMPLEMENTATION, fOpenSuperImplementation);
		actionBars.setGlobalActionHandler(JdtActionConstants.OPEN_EXTERNAL_JAVA_DOC, fOpenExternalJavadoc);
		actionBars.setGlobalActionHandler(JdtActionConstants.OPEN_TYPE_HIERARCHY, fOpenTypeHierarchy);
        actionBars.setGlobalActionHandler(JdtActionConstants.OPEN_CALL_HIERARCHY, fOpenCallHierarchy);
        
        if (!fEditorIsOwner && fShowOpenPropertiesAction)
        	actionBars.setGlobalActionHandler(ActionFactory.PROPERTIES.getId(), fOpenPropertiesDialog);		
	}
	
	private void appendToGroup(IMenuManager menu, IAction action) {
		if (action.isEnabled())
			menu.appendToGroup(IContextMenuConstants.GROUP_OPEN, action);
	}
	
	private IStructuredSelection getStructuredSelection() {
		ISelection selection= getContext().getSelection();
		if (selection instanceof IStructuredSelection)
			return (IStructuredSelection)selection;
		return null;
	}
	

	
	
}
