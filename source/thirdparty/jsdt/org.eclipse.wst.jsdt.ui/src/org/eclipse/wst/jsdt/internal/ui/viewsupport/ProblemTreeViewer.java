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

package org.eclipse.wst.jsdt.internal.ui.viewsupport;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.ui.IWorkingCopyProvider;
import org.eclipse.wst.jsdt.ui.ProblemsLabelDecorator.ProblemsLabelChangedEvent;


/**
 * Extends a  TreeViewer to allow more performance when showing error ticks.
 * A <code>ProblemItemMapper</code> is contained that maps all items in
 * the tree to underlying resource
 */
public class ProblemTreeViewer extends TreeViewer implements ResourceToItemsMapper.IContentViewerAccessor {

	protected ResourceToItemsMapper fResourceToItemsMapper;

	/*
	 * @see TreeViewer#TreeViewer(Composite)
	 */
	public ProblemTreeViewer(Composite parent) {
		super(parent);
		initMapper();
	}

	/*
	 * @see TreeViewer#TreeViewer(Composite, int)
	 */
	public ProblemTreeViewer(Composite parent, int style) {
		super(parent, style);
		initMapper();
	}

	/*
	 * @see TreeViewer#TreeViewer(Tree)
	 */
	public ProblemTreeViewer(Tree tree) {
		super(tree);
		initMapper();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.ui.viewsupport.ResourceToItemsMapper.IContentViewerAccessor#doUpdateItem(org.eclipse.swt.widgets.Widget)
	 */
	public void doUpdateItem(Widget item) {
		doUpdateItem(item, item.getData(), true);
	}
	
	private void initMapper() {
		fResourceToItemsMapper= new ResourceToItemsMapper(this);
	}
	
	
	/*
	 * @see StructuredViewer#mapElement(Object, Widget)
	 */
	protected void mapElement(Object element, Widget item) {
		super.mapElement(element, item);
		if (item instanceof Item) {
			fResourceToItemsMapper.addToMap(element, (Item) item);
		}
	}

	/*
	 * @see StructuredViewer#unmapElement(Object, Widget)
	 */
	protected void unmapElement(Object element, Widget item) {
		if (item instanceof Item) {
			fResourceToItemsMapper.removeFromMap(element, (Item) item);
		}		
		super.unmapElement(element, item);
	}

	/*
	 * @see StructuredViewer#unmapAllElements()
	 */
	protected void unmapAllElements() {
		fResourceToItemsMapper.clearMap();
		super.unmapAllElements();
	}
	
	
	// ---------------- filter sessions ----------------------------
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.StructuredViewer#addFilter(org.eclipse.jface.viewers.ViewerFilter)
	 */
	public void addFilter(ViewerFilter filter) {
		if (filter instanceof JavaViewerFilter) {
			((JavaViewerFilter) filter).filteringStart();
		}
		super.addFilter(filter);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.StructuredViewer#removeFilter(org.eclipse.jface.viewers.ViewerFilter)
	 */
	public void removeFilter(ViewerFilter filter) {
		super.removeFilter(filter);
		if (filter instanceof JavaViewerFilter) {
			((JavaViewerFilter) filter).filteringEnd();
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.StructuredViewer#setFilters(org.eclipse.jface.viewers.ViewerFilter[])
	 */
	public void setFilters(ViewerFilter[] filters) {
		ViewerFilter[] oldFilters= getFilters();
		for (int i= 0; i < filters.length; i++) {
			ViewerFilter curr= filters[i];
			if (curr instanceof JavaViewerFilter && !findAndRemove(oldFilters, curr)) {
				((JavaViewerFilter) curr).filteringStart();
			}
		}
    	endFilterSessions(oldFilters);
		super.setFilters(filters);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.StructuredViewer#resetFilters()
	 */
	public void resetFilters() {
    	endFilterSessions(getFilters());
		super.resetFilters();
	}
	
	private boolean findAndRemove(ViewerFilter[] filters, ViewerFilter filter) {
		for (int i= 0; i < filters.length; i++) {
			if (filters[i] == filter) {
				filters[i]= null;
				return true;
			}
		}
		return false;
	}
	
	private void endFilterSessions(ViewerFilter[] filters) {
		for (int i= 0; i < filters.length; i++) {
			ViewerFilter curr= filters[i];
			if (curr instanceof JavaViewerFilter) {
				((JavaViewerFilter) curr).filteringEnd();
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.StructuredViewer#handleDispose(org.eclipse.swt.events.DisposeEvent)
	 */
    protected void handleDispose(DisposeEvent event) {
    	endFilterSessions(getFilters());
    	super.handleDispose(event);
    }
    
	
	/*
	 * @see ContentViewer#handleLabelProviderChanged(LabelProviderChangedEvent)
	 */
	protected void handleLabelProviderChanged(LabelProviderChangedEvent event) {
		if (event instanceof ProblemsLabelChangedEvent) {
			ProblemsLabelChangedEvent e= (ProblemsLabelChangedEvent) event;
			if (!e.isMarkerChange() && canIgnoreChangesFromAnnotionModel()) {
				return;
			}
		}
		Object[] changed= addAditionalProblemParents(event.getElements());
		
		if (changed != null && !fResourceToItemsMapper.isEmpty()) {
			ArrayList others= new ArrayList();
			for (int i= 0; i < changed.length; i++) {
				Object curr= changed[i];
				if (curr instanceof IResource) {
					fResourceToItemsMapper.resourceChanged((IResource) curr);
				} else {
					others.add(curr);
				}
			}
			if (others.isEmpty()) {
				return;
			}
			event= new LabelProviderChangedEvent((IBaseLabelProvider) event.getSource(), others.toArray());
		} else {
			// we have modified the list of changed elements via add additional parents.
			if (event.getElements() != changed)
				event= new LabelProviderChangedEvent((IBaseLabelProvider) event.getSource(), changed);
		}
		super.handleLabelProviderChanged(event);
	}
	
	/**
	 * Answers whether this viewer can ignore label provider changes resulting from
	 * marker changes in annotation models
	 * @return return <code>true</code> if annotation model marker changes can be ignored
	 */
	private boolean canIgnoreChangesFromAnnotionModel() {
		Object contentProvider= getContentProvider();
		return contentProvider instanceof IWorkingCopyProvider && !((IWorkingCopyProvider)contentProvider).providesWorkingCopies();
	}
	
		
	/**
	 * Decides if {@link #isExpandable(Object)} should also test filters. The default behaviour is to
	 * do this only for IMembers. Implementors can replace this behaviour.
	 * @param parent the given element
	 * @return returns if if {@link #isExpandable(Object)} should also test filters for the given element.
	 */
	protected boolean evaluateExpandableWithFilters(Object parent) {
		return parent instanceof IMember;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.AbstractTreeViewer#isExpandable(java.lang.Object)
	 */
	public boolean isExpandable(Object parent) {
		if (hasFilters() && evaluateExpandableWithFilters(parent)) {
			// workaround for 65762
			return hasFilteredChildren(parent);
		}
		return super.isExpandable(parent);
	}
	
    protected final boolean hasFilteredChildren(Object parent) {
		Object[] rawChildren= getRawChildren(parent);
		return containsNonFiltered(rawChildren, parent);
    }
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.AbstractTreeViewer#getFilteredChildren(java.lang.Object)
	 */
	protected final Object[] getFilteredChildren(Object parent) {
		return filter(getRawChildren(parent), parent);
	}
	
	private Object[] filter(Object[] elements, Object parent) {
		if (!hasFilters() || elements.length == 0) {
			return elements;
		}
		List list= new ArrayList(elements.length);
		ViewerFilter[] filters = getFilters();
		for (int i = 0; i < elements.length; i++) {
			Object object = elements[i];
			if (!isFiltered(object, parent, filters)) {
				list.add(object);
			}
		}
		return list.toArray();
	}
	
	private boolean containsNonFiltered(Object[] elements, Object parent) {
		if (elements.length == 0) {
			return false;
		}
		if (!hasFilters()) {
			return true;
		}
		ViewerFilter[] filters = getFilters();
		for (int i = 0; i < elements.length; i++) {
			Object object = elements[i];
			if (!isFiltered(object, parent, filters)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * All element filter tests must go through this method.
	 * Can be overridden by subclasses.
	 * 
	 * @param object the object to filter
	 * @param parent the parent
	 * @param filters the filters to apply
	 * @return true if the element is filtered
	 */
	protected boolean isFiltered(Object object, Object parent, ViewerFilter[] filters) {
		for (int i = 0; i < filters.length; i++) {
			ViewerFilter filter = filters[i];
			if (!filter.select(this, parent, object))
				return true;
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.StructuredViewer#filter(java.lang.Object[])
	 */
	protected final Object[] filter(Object[] elements) {
		return filter(elements, getRoot());
	}
	
	protected Object[] addAditionalProblemParents(Object[] elements) {
		return elements;
	}
	
	/**
	 * Public method to test if a element is filtered by the views active filters
	 * @param object the element to test for
	 * @param parent the parent element
	 * @return return <code>true if the element is filtered</code>
	 */
	public boolean isFiltered(Object object, Object parent) {
		return isFiltered(object, parent, getFilters());
	}
}

