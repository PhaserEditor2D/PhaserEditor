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

package org.eclipse.wst.jsdt.internal.ui.packageview;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.OpenStrategy;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.actions.OpenInNewWindowAction;
import org.eclipse.ui.views.framelist.BackAction;
import org.eclipse.ui.views.framelist.ForwardAction;
import org.eclipse.ui.views.framelist.Frame;
import org.eclipse.ui.views.framelist.FrameAction;
import org.eclipse.ui.views.framelist.FrameList;
import org.eclipse.ui.views.framelist.GoIntoAction;
import org.eclipse.ui.views.framelist.TreeFrame;
import org.eclipse.ui.views.framelist.UpAction;
import org.eclipse.wst.jsdt.core.IClassFile;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IOpenable;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.internal.ui.actions.CompositeActionGroup;
import org.eclipse.wst.jsdt.internal.ui.actions.NewWizardsActionGroup;
import org.eclipse.wst.jsdt.internal.ui.actions.SelectAllAction;
//import org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths.newsourcepage.GenerateBuildPathActionGroup;
import org.eclipse.wst.jsdt.internal.ui.workingsets.ViewActionGroup;
import org.eclipse.wst.jsdt.ui.IContextMenuConstants;
import org.eclipse.wst.jsdt.ui.PreferenceConstants;
import org.eclipse.wst.jsdt.ui.actions.BuildActionGroup;
import org.eclipse.wst.jsdt.ui.actions.CCPActionGroup;
import org.eclipse.wst.jsdt.ui.actions.CustomFiltersActionGroup;
import org.eclipse.wst.jsdt.ui.actions.GenerateActionGroup;
import org.eclipse.wst.jsdt.ui.actions.ImportActionGroup;
import org.eclipse.wst.jsdt.ui.actions.JavaSearchActionGroup;
import org.eclipse.wst.jsdt.ui.actions.JdtActionConstants;
import org.eclipse.wst.jsdt.ui.actions.NavigateActionGroup;
import org.eclipse.wst.jsdt.ui.actions.OpenProjectAction;
import org.eclipse.wst.jsdt.ui.actions.ProjectActionGroup;
import org.eclipse.wst.jsdt.ui.actions.RefactorActionGroup;

class PackageExplorerActionGroup extends CompositeActionGroup {

	private PackageExplorerPart fPart;

	private FrameList fFrameList;
	private GoIntoAction fZoomInAction;
 	private BackAction fBackAction;
	private ForwardAction fForwardAction;
	private UpAction fUpAction;
	private GotoTypeAction fGotoTypeAction;
	private GotoPackageAction fGotoPackageAction;
	private GotoResourceAction fGotoResourceAction;
	private CollapseAllAction fCollapseAllAction;
	private SelectAllAction fSelectAllAction;
	
	
	private ToggleLinkingAction fToggleLinkingAction;

	private RefactorActionGroup fRefactorActionGroup;
	private NavigateActionGroup fNavigateActionGroup;
	private ViewActionGroup fViewActionGroup;
	
	private CustomFiltersActionGroup fCustomFiltersActionGroup;

	private IAction fGotoRequiredProjectAction;

	private ProjectActionGroup fProjectActionGroup;	
 	
	public PackageExplorerActionGroup(PackageExplorerPart part) {
		super();
		fPart= part;
		TreeViewer viewer= part.getTreeViewer();
		
		IPropertyChangeListener workingSetListener= new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				doWorkingSetChanged(event);
			}
		};
		
		IWorkbenchPartSite site = fPart.getSite();
		setGroups(new ActionGroup[] {
			new NewWizardsActionGroup(site),
			fNavigateActionGroup= new NavigateActionGroup(fPart), 
			new CCPActionGroup(fPart),
            /*new GenerateBuildPathActionGroup(fPart),*/
			new GenerateActionGroup(fPart), 
			fRefactorActionGroup= new RefactorActionGroup(fPart),
			new ImportActionGroup(fPart),
			new BuildActionGroup(fPart),
			new JavaSearchActionGroup(fPart),
			fProjectActionGroup= new ProjectActionGroup(fPart), 
			fViewActionGroup= new ViewActionGroup(fPart.getRootMode(), workingSetListener, site),
			fCustomFiltersActionGroup= new CustomFiltersActionGroup(fPart, viewer),
			new LayoutActionGroup(fPart)
		});
		

		fViewActionGroup.fillFilters(viewer);
		
		PackagesFrameSource frameSource= new PackagesFrameSource(fPart);
		fFrameList= new FrameList(frameSource);
		frameSource.connectTo(fFrameList);
			
		fZoomInAction= new GoIntoAction(fFrameList);
		fBackAction= new BackAction(fFrameList);
		fForwardAction= new ForwardAction(fFrameList);
		fUpAction= new UpAction(fFrameList);
		
		fGotoTypeAction= new GotoTypeAction(fPart);
		fGotoPackageAction= new GotoPackageAction(fPart);
		fGotoResourceAction= new GotoResourceAction(fPart);
		fCollapseAllAction= new CollapseAllAction(fPart);	
		fToggleLinkingAction = new ToggleLinkingAction(fPart); 
		fGotoRequiredProjectAction= new GotoRequiredProjectAction(fPart);
		fSelectAllAction= new SelectAllAction(fPart.getTreeViewer());
	}

	public void dispose() {
		super.dispose();
	}
	

	//---- Persistent state -----------------------------------------------------------------------

	/* package */ void restoreFilterAndSorterState(IMemento memento) {
		fViewActionGroup.restoreState(memento);
		fCustomFiltersActionGroup.restoreState(memento);
	}
	
	/* package */ void saveFilterAndSorterState(IMemento memento) {
		fViewActionGroup.saveState(memento);
		fCustomFiltersActionGroup.saveState(memento);
	}

	//---- Action Bars ----------------------------------------------------------------------------

	public void fillActionBars(IActionBars actionBars) {
		super.fillActionBars(actionBars);
		setGlobalActionHandlers(actionBars);
		fillToolBar(actionBars.getToolBarManager());
		fillViewMenu(actionBars.getMenuManager());		
	}

	/* package  */ void updateActionBars(IActionBars actionBars) {
		actionBars.getToolBarManager().removeAll();
		actionBars.getMenuManager().removeAll();
		fillActionBars(actionBars);
		actionBars.updateActionBars();
		fZoomInAction.setEnabled(true);
	}

	private void setGlobalActionHandlers(IActionBars actionBars) {
		// Navigate Go Into and Go To actions.
		actionBars.setGlobalActionHandler(IWorkbenchActionConstants.GO_INTO, fZoomInAction);
		actionBars.setGlobalActionHandler(ActionFactory.BACK.getId(), fBackAction);
		actionBars.setGlobalActionHandler(ActionFactory.FORWARD.getId(), fForwardAction);
		actionBars.setGlobalActionHandler(IWorkbenchActionConstants.UP, fUpAction);
		actionBars.setGlobalActionHandler(IWorkbenchActionConstants.GO_TO_RESOURCE, fGotoResourceAction);
		actionBars.setGlobalActionHandler(JdtActionConstants.GOTO_TYPE, fGotoTypeAction);
		actionBars.setGlobalActionHandler(JdtActionConstants.GOTO_PACKAGE, fGotoPackageAction);
		actionBars.setGlobalActionHandler(ActionFactory.SELECT_ALL.getId(), fSelectAllAction);
		
		fRefactorActionGroup.retargetFileMenuActions(actionBars);
	}

	/* package */ void fillToolBar(IToolBarManager toolBar) {
		toolBar.add(fBackAction);
		toolBar.add(fForwardAction);
		toolBar.add(fUpAction); 
		
		toolBar.add(new Separator());
		toolBar.add(fCollapseAllAction);
		toolBar.add(fToggleLinkingAction);

	}
	
	/* package */ void fillViewMenu(IMenuManager menu) {
		menu.add(new Separator());
		menu.add(fToggleLinkingAction);
		menu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		menu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS+"-end"));//$NON-NLS-1$		
	}

	//---- Context menu -------------------------------------------------------------------------

	public void fillContextMenu(IMenuManager menu) {		
		IStructuredSelection selection= (IStructuredSelection)getContext().getSelection();
		int size= selection.size();
		Object element= selection.getFirstElement();
		
		if (element instanceof JsGlobalScopeContainer.RequiredProjectWrapper) 
			menu.appendToGroup(IContextMenuConstants.GROUP_SHOW, fGotoRequiredProjectAction);
		
		addGotoMenu(menu, element, size);
		
		addOpenNewWindowAction(menu, element);
		
		super.fillContextMenu(menu);
	}
	
	 private void addGotoMenu(IMenuManager menu, Object element, int size) {
		boolean enabled= size == 1 && fPart.getTreeViewer().isExpandable(element) && (isGoIntoTarget(element) || element instanceof IContainer);
		fZoomInAction.setEnabled(enabled);
		if (enabled)
			menu.appendToGroup(IContextMenuConstants.GROUP_GOTO, fZoomInAction);
	}
	
	private boolean isGoIntoTarget(Object element) {
		if (element == null)
			return false;
		if (element instanceof IJavaScriptElement) {
			int type= ((IJavaScriptElement)element).getElementType();
			return type == IJavaScriptElement.JAVASCRIPT_PROJECT || 
				type == IJavaScriptElement.PACKAGE_FRAGMENT_ROOT || 
				type == IJavaScriptElement.PACKAGE_FRAGMENT;
		}
		if (element instanceof IWorkingSet) {
			return true;
		}
		return false;
	}

	private void addOpenNewWindowAction(IMenuManager menu, Object element) {
		if (element instanceof IJavaScriptElement) {
			element= ((IJavaScriptElement)element).getResource();
			
		}
		// fix for 64890 Package explorer out of sync when open/closing projects [package explorer] 64890  
		if (element instanceof IProject && !((IProject)element).isOpen()) 
			return;
		
		if (!(element instanceof IContainer))
			return;
		menu.appendToGroup(
			IContextMenuConstants.GROUP_OPEN, 
			new OpenInNewWindowAction(fPart.getSite().getWorkbenchWindow(), (IContainer)element));
	}

	//---- Key board and mouse handling ------------------------------------------------------------

	/* package*/ void handleDoubleClick(DoubleClickEvent event) {
		TreeViewer viewer= fPart.getTreeViewer();
		IStructuredSelection selection= (IStructuredSelection)event.getSelection();
		Object element= selection.getFirstElement();
		if (viewer.isExpandable(element)) {
			if (doubleClickGoesInto()) {
				// don't zoom into compilation units and class files
				if (element instanceof IJavaScriptUnit || element instanceof IClassFile)
					return;
				if (element instanceof IOpenable || element instanceof IContainer || element instanceof IWorkingSet) {
					fZoomInAction.run();
				}
			} else {
				IAction openAction= fNavigateActionGroup.getOpenAction();
				if (openAction != null && openAction.isEnabled() && OpenStrategy.getOpenMethod() == OpenStrategy.DOUBLE_CLICK)
					return;
				if (selection instanceof ITreeSelection) {
					TreePath[] paths= ((ITreeSelection)selection).getPathsFor(element);
					for (int i= 0; i < paths.length; i++) {
						viewer.setExpandedState(paths[i], !viewer.getExpandedState(paths[i]));
					}
				} else {
					viewer.setExpandedState(element, !viewer.getExpandedState(element));
				}
			}
		} else if (element instanceof IProject && !((IProject) element).isOpen()) {
			OpenProjectAction openProjectAction= fProjectActionGroup.getOpenProjectAction();
			if (openProjectAction.isEnabled()) {
				openProjectAction.run();
			}
		}
	}
	
	/* package */ void handleOpen(OpenEvent event) {
		IAction openAction= fNavigateActionGroup.getOpenAction();
		if (openAction != null && openAction.isEnabled()) {
			openAction.run();
			return;
		}
	}
	
	/* package */ void handleKeyEvent(KeyEvent event) {
		if (event.stateMask != 0) 
			return;		
		
		if (event.keyCode == SWT.BS) {
			if (fUpAction != null && fUpAction.isEnabled()) {
				fUpAction.run();
				event.doit= false;
			}
		}
	}
	
	private void doWorkingSetChanged(PropertyChangeEvent event) {
		if (ViewActionGroup.MODE_CHANGED.equals(event.getProperty())) {
			fPart.rootModeChanged(((Integer)event.getNewValue()).intValue());
			Object oldInput= null;
			Object newInput= null;
			if (fPart.showProjects()) {
				oldInput= fPart.getWorkingSetModel();
				newInput= JavaScriptCore.create(ResourcesPlugin.getWorkspace().getRoot());
			} else if (fPart.showWorkingSets()) {
				oldInput= JavaScriptCore.create(ResourcesPlugin.getWorkspace().getRoot());
				newInput= fPart.getWorkingSetModel();
			}
			if (oldInput != null && newInput != null) {
				Frame frame;
				for (int i= 0; (frame= fFrameList.getFrame(i)) != null; i++) {
					if (frame instanceof TreeFrame) {
						TreeFrame treeFrame= (TreeFrame)frame;
						if (oldInput.equals(treeFrame.getInput()))
							treeFrame.setInput(newInput);
					}
				}
			}
		} else {
			IWorkingSet workingSet= (IWorkingSet) event.getNewValue();
			
			String workingSetLabel= null;
			if (workingSet != null)
				workingSetLabel= workingSet.getLabel();
			fPart.setWorkingSetLabel(workingSetLabel);
			fPart.updateTitle();
	
			String property= event.getProperty();
			if (IWorkingSetManager.CHANGE_WORKING_SET_CONTENT_CHANGE.equals(property)) {
				TreeViewer viewer= fPart.getTreeViewer();
				viewer.getControl().setRedraw(false);
				viewer.refresh();
				viewer.getControl().setRedraw(true);
			}
		}
	}

	private boolean doubleClickGoesInto() {
		return PreferenceConstants.DOUBLE_CLICK_GOES_INTO.equals(PreferenceConstants.getPreferenceStore().getString(PreferenceConstants.DOUBLE_CLICK));
	}

	public FrameAction getUpAction() {
		return fUpAction;
	}

	public FrameAction getBackAction() {
		return fBackAction;
	}
	public FrameAction getForwardAction() {
		return fForwardAction;
	}

	public ViewActionGroup getWorkingSetActionGroup() {
	    return fViewActionGroup;
	}
	
	public CustomFiltersActionGroup getCustomFilterActionGroup() {
	    return fCustomFiltersActionGroup;
	}
	
	public FrameList getFrameList() {
		return fFrameList;
	}
}
