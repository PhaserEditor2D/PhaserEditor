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
package org.eclipse.wst.jsdt.internal.ui.browsing;

import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.wst.jsdt.core.IPackageFragment;

/**
 * Wrapper who transfers listeners and filters and to which clients
 * can refer.
 *
 * @deprecated needs to be replaced by a manager who handles transfer of listeners and filters
 */
class PackageViewerWrapper extends StructuredViewer {

	private StructuredViewer fViewer;
	private ListenerList fListenerList;
	private ListenerList fSelectionChangedListenerList;
	private ListenerList fPostSelectionChangedListenerList;

	public PackageViewerWrapper() {
		fListenerList= new ListenerList(ListenerList.IDENTITY);
		fPostSelectionChangedListenerList= new ListenerList(ListenerList.IDENTITY);
		fSelectionChangedListenerList= new ListenerList(ListenerList.IDENTITY);
	}

	public void setViewer(StructuredViewer viewer) {
		Assert.isNotNull(viewer);

		StructuredViewer oldViewer= fViewer;
		fViewer= viewer;

		if (fViewer.getContentProvider() != null)
			super.setContentProvider(fViewer.getContentProvider());
		transferFilters(oldViewer);
		transferListeners();
	}

	StructuredViewer getViewer(){
		return fViewer;
	}

	private void transferFilters(StructuredViewer oldViewer) {
		//set filters
		if (oldViewer != null) {
			ViewerFilter[] filters= oldViewer.getFilters();
			for (int i= 0; i < filters.length; i++) {
				ViewerFilter filter= filters[i];
				fViewer.addFilter(filter);
			}
		}
	}

	private void transferListeners() {

		Object[] listeners= fPostSelectionChangedListenerList.getListeners();
		for (int i= 0; i < listeners.length; i++) {
			Object object= listeners[i];
			ISelectionChangedListener listener= (ISelectionChangedListener)object;
			fViewer.addPostSelectionChangedListener(listener);
		}

		listeners= fSelectionChangedListenerList.getListeners();
		for (int i= 0; i < listeners.length; i++) {
			Object object= listeners[i];
			ISelectionChangedListener listener= (ISelectionChangedListener)object;
			fViewer.addSelectionChangedListener(listener);
		}

		// Add all other listeners
		listeners= fListenerList.getListeners();
		for (int i= 0; i < listeners.length; i++) {
			Object object= listeners[i];

			if (object instanceof IOpenListener) {
				IOpenListener listener= (IOpenListener) object;
				addOpenListener(listener);
			} else if (object instanceof HelpListener) {
				HelpListener listener= (HelpListener) object;
				addHelpListener(listener);
			} else if (object instanceof IDoubleClickListener) {
				IDoubleClickListener listener= (IDoubleClickListener) object;
				addDoubleClickListener(listener);
			}
		}
	}

	public void setSelection(ISelection selection, boolean reveal) {
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection sel= (IStructuredSelection) selection;

			//try and give the two a common super class
			IContentProvider provider= getContentProvider();
			if (provider instanceof LogicalPackagesProvider) {
				LogicalPackagesProvider fprovider= (LogicalPackagesProvider) provider;

				Object object= sel.getFirstElement();
				if (object instanceof IPackageFragment) {
					IPackageFragment pkgFragment= (IPackageFragment)object;
					LogicalPackage logicalPkg= fprovider.findLogicalPackage(pkgFragment);
					if (logicalPkg != null)
						object= logicalPkg;
					else
						object= pkgFragment;
				}
				if (object != null)
					fViewer.setSelection(new StructuredSelection(object), reveal);
				else
					fViewer.setSelection(StructuredSelection.EMPTY, reveal);
			}
		} else
			fViewer.setSelection(selection, reveal);
	}

	public void addPostSelectionChangedListener(ISelectionChangedListener listener) {
		fPostSelectionChangedListenerList.add(listener);
		fViewer.addPostSelectionChangedListener(listener);
	}

	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		fSelectionChangedListenerList.add(listener);
		fViewer.addSelectionChangedListener(listener);
	}

	public void addDoubleClickListener(IDoubleClickListener listener) {
		fViewer.addDoubleClickListener(listener);
		fListenerList.add(listener);
	}

	public void addOpenListener(IOpenListener listener) {
		fViewer.addOpenListener(listener);
		fListenerList.add(listener);
	}

	public void addHelpListener(HelpListener listener) {
		fViewer.addHelpListener(listener);
		fListenerList.add(listener);
	}

	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		fViewer.removeSelectionChangedListener(listener);
		fSelectionChangedListenerList.remove(listener);
	}

	public void removePostSelectionChangedListener(ISelectionChangedListener listener) {
		fViewer.removePostSelectionChangedListener(listener);
		fPostSelectionChangedListenerList.remove(listener);
	}

	public void removeHelpListener(HelpListener listener) {
		fListenerList.remove(listener);
		fViewer.removeHelpListener(listener);
	}

	public void removeDoubleClickListener(IDoubleClickListener listener) {
		fViewer.removeDoubleClickListener(listener);
		fListenerList.remove(listener);
	}

	public void removeOpenListener(IOpenListener listener) {
		fViewer.removeOpenListener(listener);
		fListenerList.remove(listener);
	}

	// --------- simply delegate to wrapped viewer ---------
	public Control getControl() {
		return fViewer.getControl();
	}

	public void addFilter(ViewerFilter filter) {
		fViewer.addFilter(filter);
	}
	
	public void setFilters(ViewerFilter[] filters) {
		fViewer.setFilters(filters);
	}

	public void refresh() {
		fViewer.refresh();
	}

	public void removeFilter(ViewerFilter filter) {
		fViewer.removeFilter(filter);
	}

	public ISelection getSelection() {
		return fViewer.getSelection();
	}

	public void refresh(boolean updateLabels) {
		fViewer.refresh(updateLabels);
	}

	public void refresh(Object element, boolean updateLabels) {
		fViewer.refresh(element, updateLabels);
	}

	public void refresh(Object element) {
		fViewer.refresh(element);
	}

	public void resetFilters() {
		fViewer.resetFilters();
	}

	public void reveal(Object element) {
		fViewer.reveal(element);
	}

	public void setContentProvider(IContentProvider contentProvider) {
		fViewer.setContentProvider(contentProvider);
	}

	public void setSorter(ViewerSorter sorter) {
		fViewer.setSorter(sorter);
	}
	
	public void setComparator(ViewerComparator comparator) {
		fViewer.setComparator(comparator);
	}
	
	public void setUseHashlookup(boolean enable) {
		fViewer.setUseHashlookup(enable);
	}

	public Widget testFindItem(Object element) {
		return fViewer.testFindItem(element);
	}

	public void update(Object element, String[] properties) {
		fViewer.update(element, properties);
	}

	public void update(Object[] elements, String[] properties) {
		fViewer.update(elements, properties);
	}

	public IContentProvider getContentProvider() {
		return fViewer.getContentProvider();
	}

	public Object getInput() {
		return fViewer.getInput();
	}

	public IBaseLabelProvider getLabelProvider() {
		return fViewer.getLabelProvider();
	}

	public void setLabelProvider(IBaseLabelProvider labelProvider) {
		fViewer.setLabelProvider(labelProvider);
	}

	public Object getData(String key) {
		return fViewer.getData(key);
	}

	public Item scrollDown(int x, int y) {
		return fViewer.scrollDown(x, y);
	}

	public Item scrollUp(int x, int y) {
		return fViewer.scrollUp(x, y);
	}

	public void setData(String key, Object value) {
		fViewer.setData(key, value);
	}

	public void setSelection(ISelection selection) {
		fViewer.setSelection(selection);
	}

	public boolean equals(Object obj) {
		return fViewer.equals(obj);
	}

	public int hashCode() {
		return fViewer.hashCode();
	}

	public String toString() {
		return fViewer.toString();
	}

	public void setViewerInput(Object input){
		fViewer.setInput(input);
	}

	// need to provide implementation for abstract methods
	protected Widget doFindInputItem(Object element) {
		return ((IPackagesViewViewer)fViewer).doFindInputItem(element);
	}

	protected Widget doFindItem(Object element) {
		return ((IPackagesViewViewer)fViewer).doFindItem(element);
	}

	protected void doUpdateItem(Widget item, Object element, boolean fullMap) {
		((IPackagesViewViewer)fViewer).doUpdateItem(item, element, fullMap);
	}

	protected List getSelectionFromWidget() {
		return ((IPackagesViewViewer)fViewer).getSelectionFromWidget();
	}

	protected void internalRefresh(Object element) {
		((IPackagesViewViewer)fViewer).internalRefresh(element);
	}

	protected void setSelectionToWidget(List l, boolean reveal) {
		((IPackagesViewViewer)fViewer).setSelectionToWidget(l, reveal);
	}


}
