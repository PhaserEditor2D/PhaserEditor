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

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.IShowInTargetList;
import org.eclipse.wst.jsdt.core.IClassFile;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.actions.SelectAllAction;
import org.eclipse.wst.jsdt.internal.ui.filters.NonJavaElementFilter;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.AppearanceAwareLabelProvider;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.ColoredViewersManager;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.DecoratingJavaLabelProvider;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.JavaUILabelProvider;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabels;
import org.eclipse.wst.jsdt.ui.JavaScriptUI;
import org.eclipse.wst.jsdt.ui.PreferenceConstants;

public class TypesView extends JavaBrowsingPart {

	private SelectAllAction fSelectAllAction;

	/**
	 * Creates and returns the label provider for this part.
	 *
	 * @return the label provider
	 * @see org.eclipse.jface.viewers.ILabelProvider
	 */
	protected JavaUILabelProvider createLabelProvider() {
		return new AppearanceAwareLabelProvider(
						AppearanceAwareLabelProvider.DEFAULT_TEXTFLAGS | JavaScriptElementLabels.T_CATEGORY,
						AppearanceAwareLabelProvider.DEFAULT_IMAGEFLAGS);
	}

	protected StructuredViewer createViewer(Composite parent) {
		StructuredViewer viewer= super.createViewer(parent);
		ColoredViewersManager.install(viewer);
		return viewer;
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

	/**
	 * Adds filters the viewer of this part.
	 */
	protected void addFilters() {
		super.addFilters();
		getViewer().addFilter(new NonJavaElementFilter());
	}

	/**
	 * Answers if the given <code>element</code> is a valid
	 * input for this part.
	 *
	 * @param 	element	the object to test
	 * @return	<true> if the given element is a valid input
	 */
	protected boolean isValidInput(Object element) {
		return element instanceof IPackageFragment;
	}

	/**
	 * Answers if the given <code>element</code> is a valid
	 * element for this part.
	 *
	 * @param 	element	the object to test
	 * @return	<true> if the given element is a valid element
	 */
	protected boolean isValidElement(Object element) {
		if (element instanceof IJavaScriptUnit)
			return super.isValidElement(((IJavaScriptUnit)element).getParent());
		else if (element instanceof IType) {
			IType type= (IType)element;
			return type.getDeclaringType() == null && isValidElement(type.getJavaScriptUnit());
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
				IType type= ((IType)je).getDeclaringType();
				if (type == null)
					type= (IType)je;
				return type;
			case IJavaScriptElement.JAVASCRIPT_UNIT:
				return getTypeForCU((IJavaScriptUnit)je);
			case IJavaScriptElement.CLASS_FILE:
				return findElementToSelect(((IClassFile)je).getType());
			case IJavaScriptElement.IMPORT_CONTAINER:
			case IJavaScriptElement.IMPORT_DECLARATION:
				return findElementToSelect(je.getParent());
			default:
				if (je instanceof IMember)
					return findElementToSelect(((IMember)je).getDeclaringType());
				return null;

		}
	}

	/**
	 * Returns the context ID for the Help system
	 *
	 * @return	the string used as ID for the Help context
	 */
	protected String getHelpContextId() {
		return IJavaHelpContextIds.TYPES_VIEW;
	}

	protected String getLinkToEditorKey() {
		return PreferenceConstants.LINK_BROWSING_TYPES_TO_EDITOR;
	}

	protected void createActions() {
		super.createActions();
		fSelectAllAction= new SelectAllAction((TableViewer)getViewer());
	}

	protected void fillActionBars(IActionBars actionBars) {
		super.fillActionBars(actionBars);

		// Add selectAll action handlers.
		actionBars.setGlobalActionHandler(ActionFactory.SELECT_ALL.getId(), fSelectAllAction);
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
			IStructuredSelection sel= (IStructuredSelection) selection;
			Object selectedElement= sel.getFirstElement();
			if (sel.size() == 1 && (selectedElement instanceof LogicalPackage)) {
				IPackageFragment[] fragments= ((LogicalPackage)selectedElement).getFragments();
				List selectedElements= Arrays.asList(fragments);
				if (selectedElements.size() > 1) {
					adjustInput(part, selectedElements);
					fPreviousSelectedElement= selectedElements;
					fPreviousSelectionProvider= part;
				} else if (selectedElements.size() == 1)
					super.selectionChanged(part, new StructuredSelection(selectedElements.get(0)));
				else
					Assert.isLegal(false);
				return;
			}
		}
		super.selectionChanged(part, selection);
	}

	private void adjustInput(IWorkbenchPart part, List selectedElements) {
		Object currentInput= getViewer().getInput();
		if (!selectedElements.equals(currentInput))
			setInput(selectedElements);
	}
	/*
	 * @see org.eclipse.wst.jsdt.internal.ui.browsing.JavaBrowsingPart#createDecoratingLabelProvider(org.eclipse.wst.jsdt.internal.ui.viewsupport.JavaUILabelProvider)
	 */
	protected DecoratingJavaLabelProvider createDecoratingLabelProvider(JavaUILabelProvider provider) {
		DecoratingJavaLabelProvider decoratingLabelProvider= super.createDecoratingLabelProvider(provider);
		provider.addLabelDecorator(new TopLevelTypeProblemsLabelDecorator(null));
		return decoratingLabelProvider;
	}

}
