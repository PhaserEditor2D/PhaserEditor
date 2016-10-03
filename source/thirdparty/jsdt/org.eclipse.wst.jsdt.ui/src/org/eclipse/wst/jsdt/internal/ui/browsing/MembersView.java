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
package org.eclipse.wst.jsdt.internal.ui.browsing;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.part.IShowInTargetList;
import org.eclipse.wst.jsdt.core.IClassFile;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IImportContainer;
import org.eclipse.wst.jsdt.core.IImportDeclaration;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.actions.CategoryFilterActionGroup;
import org.eclipse.wst.jsdt.internal.ui.actions.LexicalSortingAction;
import org.eclipse.wst.jsdt.internal.ui.preferences.MembersOrderPreferenceCache;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.AppearanceAwareLabelProvider;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.ColoredViewersManager;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.JavaUILabelProvider;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.ProblemTreeViewer;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabels;
import org.eclipse.wst.jsdt.ui.JavaScriptUI;
import org.eclipse.wst.jsdt.ui.PreferenceConstants;
import org.eclipse.wst.jsdt.ui.actions.MemberFilterActionGroup;

public class MembersView extends JavaBrowsingPart implements IPropertyChangeListener {

	private MemberFilterActionGroup fMemberFilterActionGroup;
	/**
	 * Category filter action group.
	 * 
	 */
	private CategoryFilterActionGroup fCategoryFilterActionGroup;


	public MembersView() {
		setHasWorkingSetFilter(false);
		setHasCustomSetFilter(true);
		JavaScriptPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.ui.browsing.JavaBrowsingPart#getAdapter(java.lang.Class)
	 */
	public Object getAdapter(Class key) {
		if (key == IShowInTargetList.class) {
			return new IShowInTargetList() {
				public String[] getShowInTargetIds() {
					return new String[] { JavaScriptUI.ID_PACKAGES };
				}

			};
		}
		return super.getAdapter(key);
	}

	/**
	 * Creates and returns the label provider for this part.
	 *
	 * @return the label provider
	 * @see org.eclipse.jface.viewers.ILabelProvider
	 */
	protected JavaUILabelProvider createLabelProvider() {
		return new AppearanceAwareLabelProvider(
						AppearanceAwareLabelProvider.DEFAULT_TEXTFLAGS | JavaScriptElementLabels.F_APP_TYPE_SIGNATURE | JavaScriptElementLabels.ALL_CATEGORY,
						AppearanceAwareLabelProvider.DEFAULT_IMAGEFLAGS
						);
	}

	/**
	 * Returns the context ID for the Help system
	 *
	 * @return	the string used as ID for the Help context
	 */
	protected String getHelpContextId() {
		return IJavaHelpContextIds.MEMBERS_VIEW;
	}

	protected String getLinkToEditorKey() {
		return PreferenceConstants.LINK_BROWSING_MEMBERS_TO_EDITOR;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.ui.browsing.JavaBrowsingPart#createViewer(org.eclipse.swt.widgets.Composite)
	 */
	protected StructuredViewer createViewer(Composite parent) {
		ProblemTreeViewer viewer= new ProblemTreeViewer(parent, SWT.MULTI);
		ColoredViewersManager.install(viewer);
		fMemberFilterActionGroup= new MemberFilterActionGroup(viewer, JavaScriptUI.ID_MEMBERS_VIEW);
		return viewer;
	}

	protected void fillToolBar(IToolBarManager tbm) {
		tbm.add(new LexicalSortingAction(getViewer(), JavaScriptUI.ID_MEMBERS_VIEW));
		fMemberFilterActionGroup.contributeToToolBar(tbm);
		super.fillToolBar(tbm);
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.ui.browsing.JavaBrowsingPart#fillActionBars(org.eclipse.ui.IActionBars)
	 * 
	 */
	protected void fillActionBars(IActionBars actionBars) {
		super.fillActionBars(actionBars);
		fCategoryFilterActionGroup= new CategoryFilterActionGroup(getViewer(), getViewSite().getId(), getCategoryFilterActionGroupInput());
		fCategoryFilterActionGroup.contributeToViewMenu(actionBars.getMenuManager());
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.ui.browsing.JavaBrowsingPart#setInput(java.lang.Object)
	 * 
	 */
	protected void setInput(Object input) {
		super.setInput(input);
		if (fCategoryFilterActionGroup != null)
			fCategoryFilterActionGroup.setInput(getCategoryFilterActionGroupInput());
	}
	
	private IJavaScriptElement[] getCategoryFilterActionGroupInput() {
		Object input= getInput();
		if (input instanceof IJavaScriptElement)
			return new IJavaScriptElement[] { (IJavaScriptElement)input };
		return new IJavaScriptElement[0];
	}

	/**
	 * Answers if the given <code>element</code> is a valid
	 * input for this part.
	 *
	 * @param 	element	the object to test
	 * @return	<true> if the given element is a valid input
	 */
	protected boolean isValidInput(Object element) {
		if (element instanceof IType) {
			IType type= (IType)element;
			return type.isBinary() || type.getDeclaringType() == null;
		}
		return false;
	}

	/**
	 * Answers if the given <code>element</code> is a valid
	 * element for this part.
	 *
	 * @param 	element	the object to test
	 * @return	<true> if the given element is a valid element
	 */
	protected boolean isValidElement(Object element) {
		if (element instanceof IMember)
			return super.isValidElement(((IMember)element).getDeclaringType());
		else if (element instanceof IImportDeclaration)
			return isValidElement(((IJavaScriptElement)element).getParent());
		else if (element instanceof IImportContainer) {
			Object input= getViewer().getInput();
			if (input instanceof IJavaScriptElement) {
				IJavaScriptUnit cu= (IJavaScriptUnit)((IJavaScriptElement)input).getAncestor(IJavaScriptElement.JAVASCRIPT_UNIT);
				if (cu != null) {
					IJavaScriptUnit importContainerCu= (IJavaScriptUnit)((IJavaScriptElement)element).getAncestor(IJavaScriptElement.JAVASCRIPT_UNIT);
					return cu.equals(importContainerCu);
				} else {
					IClassFile cf= (IClassFile)((IJavaScriptElement)input).getAncestor(IJavaScriptElement.CLASS_FILE);
					IClassFile importContainerCf= (IClassFile)((IJavaScriptElement)element).getAncestor(IJavaScriptElement.CLASS_FILE);
					return cf != null && cf.equals(importContainerCf);
				}
			}
		}
		return false;
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
			case IJavaScriptElement.TYPE:
				if (((IType)je).getDeclaringType() == null)
					return null;
				return je;
			case IJavaScriptElement.METHOD:
			case IJavaScriptElement.INITIALIZER:
			case IJavaScriptElement.FIELD:
			case IJavaScriptElement.IMPORT_CONTAINER:
				return je;
			case IJavaScriptElement.IMPORT_DECLARATION:
				IJavaScriptUnit cu= (IJavaScriptUnit)je.getParent().getParent();
				try {
					if (cu.getImports()[0].equals(je)) {
						Object selectedElement= getSingleElementFromSelection(getViewer().getSelection());
						if (selectedElement instanceof IImportContainer)
							return (IImportContainer)selectedElement;
					}
				} catch (JavaScriptModelException ex) {
					// return je;
				}
				return je;
		}
		return null;
	}

	/**
	 * Finds the closest Java element which can be used as input for
	 * this part and has the given Java element as child.
	 *
	 * @param 	je 	the Java element for which to search the closest input
	 * @return	the closest Java element used as input for this part, or <code>null</code>
	 */
	protected IJavaScriptElement findInputForJavaElement(IJavaScriptElement je) {
		if (je == null || !je.exists() || (je.getJavaScriptProject() != null && !je.getJavaScriptProject().isOnIncludepath(je)))
			return null;

		switch (je.getElementType()) {
			case IJavaScriptElement.TYPE:
				IType type= ((IType)je).getDeclaringType();
				if (type == null)
					return je;
				else
					return findInputForJavaElement(type);
			case IJavaScriptElement.JAVASCRIPT_UNIT:
				return getTypeForCU((IJavaScriptUnit)je);
			case IJavaScriptElement.CLASS_FILE:
				return findInputForJavaElement(((IClassFile)je).getType());
			case IJavaScriptElement.IMPORT_DECLARATION:
				return findInputForJavaElement(je.getParent());
			case IJavaScriptElement.IMPORT_CONTAINER:
				IJavaScriptElement parent= je.getParent();
				if (parent instanceof IJavaScriptUnit) {
					return getTypeForCU((IJavaScriptUnit)parent);
				}
				else if (parent instanceof IClassFile)
					return findInputForJavaElement(parent);
				return null;
			default:
				if (je instanceof IMember)
					return findInputForJavaElement(((IMember)je).getDeclaringType());
		}
		return null;
	}

	/*
	 * Implements method from IViewPart.
	 */
	public void saveState(IMemento memento) {
		super.saveState(memento);
		fMemberFilterActionGroup.saveState(memento);
	}

	protected void restoreState(IMemento memento) {
		super.restoreState(memento);
		fMemberFilterActionGroup.restoreState(memento);
		getViewer().getControl().setRedraw(false);
		getViewer().refresh();
 		getViewer().getControl().setRedraw(true);
	}

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

	boolean isInputAWorkingCopy() {
		Object input= getViewer().getInput();
		if (input instanceof IJavaScriptElement) {
			IJavaScriptUnit cu= (IJavaScriptUnit)((IJavaScriptElement)input).getAncestor(IJavaScriptElement.JAVASCRIPT_UNIT);
			if (cu != null)
				return cu.isWorkingCopy();
		}
		return false;
	}

	protected void restoreSelection() {
		IEditorPart editor= getViewSite().getPage().getActiveEditor();
		if (editor != null)
			setSelectionFromEditor(editor);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent event) {
		if (MembersOrderPreferenceCache.isMemberOrderProperty(event.getProperty())) {
			getViewer().refresh();
		}
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.ui.browsing.JavaBrowsingPart#dispose()
	 */
	public void dispose() {
		if (fMemberFilterActionGroup != null) {
			fMemberFilterActionGroup.dispose();
			fMemberFilterActionGroup= null;
		}
		if (fCategoryFilterActionGroup != null) {
			fCategoryFilterActionGroup.dispose();
			fCategoryFilterActionGroup= null;
		}
		super.dispose();
		JavaScriptPlugin.getDefault().getPreferenceStore().removePropertyChangeListener(this);
	}
}
