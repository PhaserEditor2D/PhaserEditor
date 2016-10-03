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
package org.eclipse.wst.jsdt.internal.ui.browsing;

import java.util.Iterator;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.IShowInTargetList;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptModel;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.ColoredViewersManager;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.FilterUpdater;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.ProblemTreeViewer;
import org.eclipse.wst.jsdt.ui.JavaScriptUI;
import org.eclipse.wst.jsdt.ui.PreferenceConstants;
import org.eclipse.wst.jsdt.ui.actions.ProjectActionGroup;

public class ProjectsView extends JavaBrowsingPart {

	private FilterUpdater fFilterUpdater;

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.ui.browsing.JavaBrowsingPart#createViewer(org.eclipse.swt.widgets.Composite)
	 */
	protected StructuredViewer createViewer(Composite parent) {
		ProblemTreeViewer result= new ProblemTreeViewer(parent, SWT.MULTI);
		ColoredViewersManager.install(result);
		fFilterUpdater= new FilterUpdater(result);
		ResourcesPlugin.getWorkspace().addResourceChangeListener(fFilterUpdater);
		return result;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.ui.browsing.JavaBrowsingPart#dispose()
	 */
	public void dispose() {
		if (fFilterUpdater != null)
			ResourcesPlugin.getWorkspace().removeResourceChangeListener(fFilterUpdater);
		super.dispose();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.ui.browsing.JavaBrowsingPart#getAdapter(java.lang.Class)
	 */
	public Object getAdapter(Class key) {
		if (key == IShowInTargetList.class) {
			return new IShowInTargetList() {
				public String[] getShowInTargetIds() {
					return new String[] { JavaScriptUI.ID_PACKAGES, IPageLayout.ID_RES_NAV  };
				}

			};
		}
		return super.getAdapter(key);
	}


	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.ui.browsing.JavaBrowsingPart#createContentProvider()
	 */
	protected IContentProvider createContentProvider() {
		return new ProjectAndSourceFolderContentProvider(this);
	}

	/**
	 * Returns the context ID for the Help system.
	 *
	 * @return	the string used as ID for the Help context
	 */
	protected String getHelpContextId() {
		return IJavaHelpContextIds.PROJECTS_VIEW;
	}

	protected String getLinkToEditorKey() {
		return PreferenceConstants.LINK_BROWSING_PROJECTS_TO_EDITOR;
	}


	/**
	 * Adds additional listeners to this view.
	 */
	protected void hookViewerListeners() {
		super.hookViewerListeners();
		getViewer().addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				TreeViewer viewer= (TreeViewer)getViewer();
				Object element= ((IStructuredSelection)event.getSelection()).getFirstElement();
				if (viewer.isExpandable(element))
					viewer.setExpandedState(element, !viewer.getExpandedState(element));
			}
		});
	}

	protected void setInitialInput() {
		IJavaScriptElement root= JavaScriptCore.create(JavaScriptPlugin.getWorkspace().getRoot());
		getViewer().setInput(root);
		updateTitle();
	}

	/**
	 * Answers if the given <code>element</code> is a valid
	 * input for this part.
	 *
	 * @param 	element	the object to test
	 * @return	<true> if the given element is a valid input
	 */
	protected boolean isValidInput(Object element) {
		return element instanceof IJavaScriptModel;
	}

	/**
	 * Answers if the given <code>element</code> is a valid
	 * element for this part.
	 *
	 * @param 	element	the object to test
	 * @return	<true> if the given element is a valid element
	 */
	protected boolean isValidElement(Object element) {
		return element instanceof IJavaScriptProject || element instanceof IPackageFragmentRoot;
	}

	/**
	 * Finds the element which has to be selected in this part.
	 *
	 * @param je	the Java element which has the focus
	 * @return the element to select
	 */
	protected IJavaScriptElement findElementToSelect(IJavaScriptElement je) {
		if (je == null)
			return null;

		switch (je.getElementType()) {
			case IJavaScriptElement.JAVASCRIPT_MODEL :
				return null;
			case IJavaScriptElement.JAVASCRIPT_PROJECT:
				return je;
			case IJavaScriptElement.PACKAGE_FRAGMENT_ROOT:
				if (je.getElementName().equals(IPackageFragmentRoot.DEFAULT_PACKAGEROOT_PATH))
					return je.getParent();
				else
					return je;
			default :
				return findElementToSelect(je.getParent());
		}
	}

	/*
	 * @see JavaBrowsingPart#setInput(Object)
	 */
	protected void setInput(Object input) {
		// Don't allow to clear input for this view
		if (input != null)
			super.setInput(input);
		else
			getViewer().setSelection(null);
	}

	protected void createActions() {
		super.createActions();
		fActionGroups.addGroup(new ProjectActionGroup(this));
	}

	/**
	 * Handles selection of LogicalPackage in Packages view.
	 *
	 * @see org.eclipse.ui.ISelectionListener#selectionChanged(org.eclipse.ui.IWorkbenchPart, org.eclipse.jface.viewers.ISelection)
	 * 
	 */
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if (!needsToProcessSelectionChanged(part, selection))
			return;

		if (selection instanceof IStructuredSelection) {
			IStructuredSelection sel= (IStructuredSelection)selection;
			Iterator iter= sel.iterator();
			while (iter.hasNext()) {
				Object selectedElement= iter.next();
				if (selectedElement instanceof LogicalPackage) {
					selection= new StructuredSelection(((LogicalPackage)selectedElement).getJavaProject());
					break;
				}
			}
		}
		super.selectionChanged(part, selection);
	}
}
