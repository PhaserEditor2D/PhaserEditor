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
package org.eclipse.wst.jsdt.internal.ui.javadocexport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Tree;

/**
 *	Combines a CheckboxTreeViewer and CheckboxListViewer.
 *	All viewer selection-driven interactions are handled within this viewer
 */
public class CheckboxTreeAndListGroup implements ICheckStateListener, ISelectionChangedListener, ITreeViewerListener {

	private Object fRoot;
	private Object fCurrentTreeSelection;
	private List fExpandedTreeNodes= new ArrayList();
	private Map fCheckedStateStore= new HashMap(9);
	private List fWhiteCheckedTreeItems= new ArrayList();
	private List fListeners= new ArrayList();

	private ITreeContentProvider fTreeContentProvider;
	private IStructuredContentProvider fListContentProvider;
	private ILabelProvider fTreeLabelProvider;
	private ILabelProvider fListLabelProvider;

	// widgets
	private CheckboxTreeViewer fTreeViewer;
	private CheckboxTableViewer fListViewer;

	/**
	 *	Creates an instance of this class.  Use this constructor if you wish to specify
	 *	the width and/or height of the combined widget (to only hardcode one of the
	 *	sizing dimensions, specify the other dimension's value as -1)
	 */
	public CheckboxTreeAndListGroup(
			Composite parent,
			Object rootObject,
			ITreeContentProvider treeContentProvider,
			ILabelProvider treeLabelProvider,
			IStructuredContentProvider listContentProvider,
			ILabelProvider listLabelProvider,
			int style,
			int width,
			int height) {
		fRoot= rootObject;
		fTreeContentProvider= treeContentProvider;
		fListContentProvider= listContentProvider;
		fTreeLabelProvider= treeLabelProvider;
		fListLabelProvider= listLabelProvider;
		createContents(parent, width, height, style);
	}
	/**
	 * This method must be called just before this window becomes visible.
	 */
	public void aboutToOpen() {
		determineWhiteCheckedDescendents(fRoot);
		checkNewTreeElements(getTreeChildren(fRoot));
		fCurrentTreeSelection= null;

		//select the first element in the list
		Object[] elements= getTreeChildren(fRoot);
		Object primary= elements.length > 0 ? elements[0] : null;
		if (primary != null) {
			fTreeViewer.setSelection(new StructuredSelection(primary));
		}
		fTreeViewer.getControl().setFocus();
	}
	/**
	 *	Adds the passed listener to self's collection of clients
	 *	that listen for changes to element checked states
	 *
	 *	@param listener ICheckStateListener
	 */
	public void addCheckStateListener(ICheckStateListener listener) {
		fListeners.add(listener);
	}
	/**
	 * Adds the receiver and all of it's ancestors to the checkedStateStore if they
	 * are not already there.
	 */
	private void addToHierarchyToCheckedStore(Object treeElement) {

		// if this tree element is already gray then its ancestors all are as well
		if (!fCheckedStateStore.containsKey(treeElement))
			fCheckedStateStore.put(treeElement, new ArrayList());

		Object parent= fTreeContentProvider.getParent(treeElement);
		if (parent != null)
			addToHierarchyToCheckedStore(parent);
	}
	/**
	 *	Returns a boolean indicating whether all children of the passed tree element
	 *	are currently white-checked
	 *
	 *	@return boolean
	 *	@param treeElement java.lang.Object
	 */
	protected boolean areAllChildrenWhiteChecked(Object treeElement) {
		Object[] children= getTreeChildren(treeElement);
		for (int i= 0; i < children.length; ++i) {
			if (!fWhiteCheckedTreeItems.contains(children[i]))
				return false;
		}

		return true;
	}
	/**
	 *	Returns a boolean indicating whether all list elements associated with
	 *	the passed tree element are currently checked
	 *
	 *	@return boolean
	 *	@param treeElement java.lang.Object
	 */
	protected boolean areAllElementsChecked(Object treeElement) {
		List checkedElements= (List)fCheckedStateStore.get(treeElement);
		if (checkedElements == null) // ie.- tree item not even gray-checked
			return false;

		return getListItemsSize(treeElement) == checkedElements.size();
	}
	/**
	 *	Iterates through the passed elements which are being realized for the first
	 *	time and check each one in the tree viewer as appropriate
	 */
	protected void checkNewTreeElements(Object[] elements) {
		for (int i= 0; i < elements.length; ++i) {
			Object currentElement= elements[i];
			boolean checked= fCheckedStateStore.containsKey(currentElement);
			fTreeViewer.setChecked(currentElement, checked);
			fTreeViewer.setGrayed(
				currentElement,
				checked && !fWhiteCheckedTreeItems.contains(currentElement));
		}
	}
	/**
	*	An item was checked in one of self's two views.  Determine which
	*	view this occurred in and delegate appropriately
	*
	*	@param event CheckStateChangedEvent
	*/
	public void checkStateChanged(final CheckStateChangedEvent event) {

		//Potentially long operation - show a busy cursor
		BusyIndicator.showWhile(fTreeViewer.getControl().getDisplay(), new Runnable() {
			public void run() {
				if (event.getCheckable().equals(fTreeViewer))
					treeItemChecked(event.getElement(), event.getChecked());
				else
					listItemChecked(event.getElement(), event.getChecked(), true);

				notifyCheckStateChangeListeners(event);
			}
		});
	}
	/**
	 *	Lay out and initialize self's visual components.
	 *
	 *	@param parent org.eclipse.swt.widgets.Composite
	 *	@param width int
	 *	@param height int
	 */
	protected void createContents(
		Composite parent,
		int width,
		int height,
		int style) {
		// group pane
		Composite composite= new Composite(parent, style);
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		layout.makeColumnsEqualWidth= true;
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));

		createTreeViewer(composite, width / 2, height);
		createListViewer(composite, width / 2, height);

		initialize();
	}
	/**
	 *	Creates this group's list viewer.
	 */
	protected void createListViewer(Composite parent, int width, int height) {
		fListViewer= CheckboxTableViewer.newCheckList(parent, SWT.BORDER);
		fListViewer.setUseHashlookup(true);
		GridData data= new GridData(GridData.FILL_BOTH);
		data.widthHint= width;
		data.heightHint= height;
		fListViewer.getTable().setLayoutData(data);
		fListViewer.setContentProvider(fListContentProvider);
		fListViewer.setLabelProvider(fListLabelProvider);
		fListViewer.addCheckStateListener(this);
	}
	/**
	 *	Creates this group's tree viewer.
	 */
	protected void createTreeViewer(Composite parent, int width, int height) {
		Tree tree= new Tree(parent, SWT.CHECK | SWT.BORDER);
		GridData data= new GridData(GridData.FILL_BOTH);
		data.widthHint= width;
		data.heightHint= height;
		tree.setLayoutData(data);

		fTreeViewer= new CheckboxTreeViewer(tree);
		fTreeViewer.setUseHashlookup(true);
		fTreeViewer.setContentProvider(fTreeContentProvider);
		fTreeViewer.setLabelProvider(fTreeLabelProvider);
		fTreeViewer.addTreeListener(this);
		fTreeViewer.addCheckStateListener(this);
		fTreeViewer.addSelectionChangedListener(this);
	}
	/**
	 * Returns a boolean indicating whether the passed tree element should be
	 * at LEAST gray-checked.  Note that this method does not consider whether
	 * it should be white-checked, so a specified tree item which should be
	 * white-checked will result in a <code>true</code> answer from this method.
	 * To determine whether a tree item should be white-checked use method
	 * #determineShouldBeWhiteChecked(Object).
	 *
	 * @param treeElement java.lang.Object
	 * @return boolean
	 * @see #determineShouldBeWhiteChecked(java.lang.Object)
	 */
	protected boolean determineShouldBeAtLeastGrayChecked(Object treeElement) {
		// if any list items associated with treeElement are checked then it
		// retains its gray-checked status regardless of its children
		List checked= (List) fCheckedStateStore.get(treeElement);
		if (checked != null && (!checked.isEmpty()))
			return true;

		// if any children of treeElement are still gray-checked then treeElement
		// must remain gray-checked as well
		Object[] children= getTreeChildren(treeElement);
		for (int i= 0; i < children.length; ++i) {
			if (fCheckedStateStore.containsKey(children[i]))
				return true;
		}

		return false;
	}
	/**
	 * Returns a boolean indicating whether the passed tree item should be
	 * white-checked.
	 *
	 * @return boolean
	 * @param treeElement java.lang.Object
	 */
	protected boolean determineShouldBeWhiteChecked(Object treeElement) {
		return areAllChildrenWhiteChecked(treeElement)
			&& areAllElementsChecked(treeElement);
	}
	/**
	 *	Recursively adds appropriate tree elements to the collection of
	 *	known white-checked tree elements.
	 *
	 *	@param treeElement java.lang.Object
	 */
	protected void determineWhiteCheckedDescendents(Object treeElement) {
		// always go through all children first since their white-checked
		// statuses will be needed to determine the white-checked status for
		// this tree element
		Object[] children= getTreeChildren(treeElement);
		for (int i= 0; i < children.length; ++i)
			determineWhiteCheckedDescendents(children[i]);

		// now determine the white-checked status for this tree element
		if (determineShouldBeWhiteChecked(treeElement))
			setWhiteChecked(treeElement, true);
	}
	/**
	 * Causes the tree viewer to expand all its items
	 */
	public void expandAll() {
		fTreeViewer.expandAll();
	}
	/**
	 *	Answers a flat collection of all of the checked elements in the
	 *	list portion of self
	 *
	 *	@return java.util.Vector
	 */
	public Iterator getAllCheckedListItems() {
		Set result= new HashSet();
		Iterator listCollectionsEnum= fCheckedStateStore.values().iterator();
		while (listCollectionsEnum.hasNext())
			result.addAll((List)listCollectionsEnum.next());
		return result.iterator();
	}
	/**
	 *	Answer a collection of all of the checked elements in the tree portion
	 *	of self
	 *
	 *	@return java.util.Vector
	 */
	public Set getAllCheckedTreeItems() {
		return new HashSet(fCheckedStateStore.keySet());
	}
	/**
	 *	Answers the number of elements that have been checked by the
	 *	user.
	 *
	 *	@return int
	 */
	public int getCheckedElementCount() {
		return fCheckedStateStore.size();
	}
	/**
	 *	Returns a count of the number of list items associated with a
	 *	given tree item.
	 *
	 *	@return int
	 *	@param treeElement java.lang.Object
	 */
	protected int getListItemsSize(Object treeElement) {
		Object[] elements= getListElements(treeElement);
		return elements.length;
	}
	/**
	 * Gets the table that displays the folder content
	 * 
	 * @return the table used to show the list
	 */
	public Table getTable() {
		return fListViewer.getTable();
	}
	/**
	 * Gets the tree that displays the list for a folder
	 * 
	 * @return the tree used to show the folders
	 */
	public Tree getTree() {
		return fTreeViewer.getTree();
	}
	/**
	 * Adds the given filter to the tree viewer and
	 * triggers refiltering and resorting of the elements.
	 *
	 * @param filter a viewer filter
	 */
	public void addTreeFilter(ViewerFilter filter) {
		fTreeViewer.addFilter(filter);
	}
	/**
	 * Adds the given filter to the list viewer and
	 * triggers refiltering and resorting of the elements.
	 *
	 * @param filter a viewer filter
	 */
	public void addListFilter(ViewerFilter filter) {
		fListViewer.addFilter(filter);
	}
	/**
	 *	Logically gray-check all ancestors of treeItem by ensuring that they
	 *	appear in the checked table
	 */
	protected void grayCheckHierarchy(Object treeElement) {

		// if this tree element is already gray then its ancestors all are as well
		if (fCheckedStateStore.containsKey(treeElement))
			return; // no need to proceed upwards from here

		fCheckedStateStore.put(treeElement, new ArrayList());
		if (determineShouldBeWhiteChecked(treeElement)) {
			setWhiteChecked(treeElement, true);
		}
		Object parent= fTreeContentProvider.getParent(treeElement);
		if (parent != null)
			grayCheckHierarchy(parent);
	}
	/**
	 *	Sets the initial checked state of the passed list element to true.
	 */
	public void initialCheckListItem(Object element) {
		Object parent= fTreeContentProvider.getParent(element);
		fCurrentTreeSelection= parent;
		//As this is not done from the UI then set the box for updating from the selection to false 
		listItemChecked(element, true, false);
		updateHierarchy(parent);
	}
	/**
	 *	Sets the initial checked state of the passed element to true,
	 *	as well as to all of its children and associated list elements
	 */
	public void initialCheckTreeItem(Object element) {
		treeItemChecked(element, true);
	}
	/**
	 *	Initializes this group's viewers after they have been laid out.
	 */
	protected void initialize() {
		fTreeViewer.setInput(fRoot);
	}
	/**
	 *	Callback that's invoked when the checked status of an item in the list
	 *	is changed by the user. Do not try and update the hierarchy if we are building the
	 *  initial list.
	 */
	protected void listItemChecked(
		Object listElement,
		boolean state,
		boolean updatingFromSelection) {
		List checkedListItems= (List) fCheckedStateStore.get(fCurrentTreeSelection);

		if (state) {
			if (checkedListItems == null) {
				// since the associated tree item has gone from 0 -> 1 checked
				// list items, tree checking may need to be updated
				grayCheckHierarchy(fCurrentTreeSelection);
				checkedListItems= (List) fCheckedStateStore.get(fCurrentTreeSelection);
			}
			checkedListItems.add(listElement);
		} else {
			checkedListItems.remove(listElement);
			if (checkedListItems.isEmpty()) {
				// since the associated tree item has gone from 1 -> 0 checked
				// list items, tree checking may need to be updated
				ungrayCheckHierarchy(fCurrentTreeSelection);
			}
		}

		if (updatingFromSelection)
			updateHierarchy(fCurrentTreeSelection);
	}
	/**
	 *	Notifies all checked state listeners that the passed element has had
	 *	its checked state changed to the passed state
	 */
	protected void notifyCheckStateChangeListeners(CheckStateChangedEvent event) {
		Iterator listenersEnum= fListeners.iterator();
		while (listenersEnum.hasNext())
			 ((ICheckStateListener) listenersEnum.next()).checkStateChanged(event);
	}
	/**
	 *Sets the contents of the list viewer based upon the specified selected
	 *tree element.  This also includes checking the appropriate list items.
	 *
	 *@param treeElement java.lang.Object
	 */
	protected void populateListViewer(final Object treeElement) {
		if (treeElement == fCurrentTreeSelection)
			return;
		fCurrentTreeSelection= treeElement;
		fListViewer.setInput(treeElement);
		List listItemsToCheck= (List) fCheckedStateStore.get(treeElement);

		if (listItemsToCheck != null) {
			Iterator listItemsEnum= listItemsToCheck.iterator();
			while (listItemsEnum.hasNext())
				fListViewer.setChecked(listItemsEnum.next(), true);
		}
	}
	/**
	 *	Removes the passed listener from self's collection of clients
	 *	that listen for changes to element checked states
	 *
	 *	@param listener ICheckStateListener
	 */
	public void removeCheckStateListener(ICheckStateListener listener) {
		fListeners.remove(listener);
	}
	/**
	 *	Handles the selection of an item in the tree viewer
	 *
	 *	@param event ISelection
	 */
	public void selectionChanged(final SelectionChangedEvent event) {
		BusyIndicator.showWhile(getTable().getShell().getDisplay(), new Runnable() {
			public void run() {
				IStructuredSelection selection= (IStructuredSelection) event.getSelection();
				Object selectedElement= selection.getFirstElement();
				if (selectedElement == null) {
					fCurrentTreeSelection= null;
					fListViewer.setInput(fCurrentTreeSelection);
					return;
				}
				populateListViewer(selectedElement);
			}
		});
	}

	/**
	 * Selects or deselect all of the elements in the tree depending on the value of the selection
	 * boolean. Be sure to update the displayed files as well.
	 */
	public void setAllSelections(final boolean selection) {

		//Potentially long operation - show a busy cursor
		BusyIndicator.showWhile(fTreeViewer.getControl().getDisplay(), new Runnable() {
			public void run() {
				setTreeChecked(fRoot, selection);
				fListViewer.setAllChecked(selection);
			}
		});
	}

	/**
	 *	Sets the list viewer's providers to those passed
	 *
	 *	@param contentProvider ITreeContentProvider
	 *	@param labelProvider ILabelProvider
	 */
	public void setListProviders(
		IStructuredContentProvider contentProvider,
		ILabelProvider labelProvider) {
		fListViewer.setContentProvider(contentProvider);
		fListViewer.setLabelProvider(labelProvider);
	}
	/**
	 *	Sets the sorter that is to be applied to self's list viewer
	 */
	public void setListComparator(ViewerComparator comparator) {
		fListViewer.setComparator(comparator);
	}
	/**
	 * Sets the root of the widget to be new Root. Regenerate all of the tables and lists from this
	 * value.
	 * 
	 * @param newRoot 
	 */
	public void setRoot(Object newRoot) {
		this.fRoot= newRoot;
		initialize();
	}
	/**
	 *	Sets the checked state of the passed tree element appropriately, and
	 *	do so recursively to all of its child tree elements as well
	 */
	protected void setTreeChecked(Object treeElement, boolean state) {

		if (treeElement.equals(fCurrentTreeSelection)) {
			fListViewer.setAllChecked(state);
		}

		if (state) {
			Object[] listItems= getListElements(treeElement);
			List listItemsChecked= new ArrayList();
			for (int i= 0; i < listItems.length; ++i)
				listItemsChecked.add(listItems[i]);

			fCheckedStateStore.put(treeElement, listItemsChecked);
		} else
			fCheckedStateStore.remove(treeElement);

		setWhiteChecked(treeElement, state);
		fTreeViewer.setChecked(treeElement, state);
		fTreeViewer.setGrayed(treeElement, false);

		// now logically check/uncheck all children as well
		Object[] children= getTreeChildren(treeElement);
		for (int i= 0; i < children.length; ++i) {
			setTreeChecked(children[i], state);
		}
	}
	/**
	 *	Sets the tree viewer's providers to those passed
	 *
	 *	@param contentProvider ITreeContentProvider
	 *	@param labelProvider ILabelProvider
	 */
	public void setTreeProviders(
		ITreeContentProvider contentProvider,
		ILabelProvider labelProvider) {
		fTreeViewer.setContentProvider(contentProvider);
		fTreeViewer.setLabelProvider(labelProvider);
	}
	/**
	 *	Sets the sorter that is to be applied to self's tree viewer
	 */
	public void setTreeComparator(ViewerComparator sorter) {
		fTreeViewer.setComparator(sorter);
	}
	/**
	 *	Adjusts the collection of references to white-checked tree elements appropriately.
	 *
	 *	@param treeElement java.lang.Object
	 *	@param isWhiteChecked boolean
	 */
	protected void setWhiteChecked(Object treeElement, boolean isWhiteChecked) {
		if (isWhiteChecked) {
			if (!fWhiteCheckedTreeItems.contains(treeElement))
				fWhiteCheckedTreeItems.add(treeElement);
		} else
			fWhiteCheckedTreeItems.remove(treeElement);
	}
	/**
	 *	Handle the collapsing of an element in a tree viewer
	 */
	public void treeCollapsed(TreeExpansionEvent event) {
		// We don't need to do anything with this
	}

	/**
	 *	Handles the expansionsion of an element in a tree viewer
	 */
	public void treeExpanded(TreeExpansionEvent event) {

		Object item= event.getElement();

		// First see if the children need to be given their checked state at all.  If they've
		// already been realized then this won't be necessary
		if (!fExpandedTreeNodes.contains(item)) {
			fExpandedTreeNodes.add(item);
			checkNewTreeElements(getTreeChildren(item));
		}
	}

	/**
	 *  Callback that's invoked when the checked status of an item in the tree
	 *  is changed by the user.
	 */
	protected void treeItemChecked(Object treeElement, boolean state) {

		// recursively adjust all child tree elements appropriately
		setTreeChecked(treeElement, state);

		Object parent= fTreeContentProvider.getParent(treeElement);
		if (parent == null)
			return;

		// now update upwards in the tree hierarchy 
		if (state)
			grayCheckHierarchy(parent);
		else
			ungrayCheckHierarchy(parent);

		updateHierarchy(treeElement);
	}
	/**
	 *	Logically un-gray-check all ancestors of treeItem iff appropriate.
	 */
	protected void ungrayCheckHierarchy(Object treeElement) {
		if (!determineShouldBeAtLeastGrayChecked(treeElement))
			fCheckedStateStore.remove(treeElement);

		Object parent= fTreeContentProvider.getParent(treeElement);
		if (parent != null)
			ungrayCheckHierarchy(parent);
	}
	/**
	 *	Sets the checked state of self and all ancestors appropriately
	 */
	protected void updateHierarchy(Object treeElement) {

		boolean whiteChecked= determineShouldBeWhiteChecked(treeElement);
		boolean shouldBeAtLeastGray= determineShouldBeAtLeastGrayChecked(treeElement);

		fTreeViewer.setChecked(treeElement, whiteChecked || shouldBeAtLeastGray);
		setWhiteChecked(treeElement, whiteChecked);
		if (whiteChecked)
			fTreeViewer.setGrayed(treeElement, false);
		else
			fTreeViewer.setGrayed(treeElement, shouldBeAtLeastGray);

		// proceed up the tree element hierarchy
		Object parent= fTreeContentProvider.getParent(treeElement);
		if (parent != null) {
			updateHierarchy(parent);
		}
	}
	/**
	 * Update the selections of the tree elements in items to reflect the new
	 * selections provided.
	 * 
	 * @param items with keys of Object (the tree element) and values of List (the selected
	 * list elements).
	 */
	public void updateSelections(final Map items) {

		//Potentially long operation - show a busy cursor
		BusyIndicator.showWhile(fTreeViewer.getControl().getDisplay(), new Runnable() {
			public void run() {
				handleUpdateSelection(items);
			}
		});
	}
	/**
	 * Returns the result of running the given elements through the filters.
	 *
	 * @param elements the elements to filter
	 * @return only the elements which all filters accept
	 */
	protected Object[] filter(ViewerFilter[] filters, Object[] elements) {
		if (filters != null) {
			ArrayList filtered = new ArrayList(elements.length);
			for (int i = 0; i < elements.length; i++) {
				boolean add = true;
				for (int j = 0; j < filters.length; j++) {
					add = filters[j].select(null, null, elements[i]);
					if (!add)
						break;
				}
				if (add)
					filtered.add(elements[i]);
			}
			return filtered.toArray();
		}
		return elements;
	}

	private Object[] getTreeChildren(Object element) {
		return filter(fTreeViewer.getFilters(), fTreeContentProvider.getChildren(element));
	}

	private Object[] getListElements(Object element) {
		return filter(fListViewer.getFilters(), fListContentProvider.getElements(element));
	}

	public Set getWhiteCheckedTreeItems() {
		return new HashSet(fWhiteCheckedTreeItems);
	}

	private void handleUpdateSelection(Map items) {
		Iterator keyIterator= items.keySet().iterator();

		//Update the store before the hierarchy to prevent updating parents before all of the children are done
		while (keyIterator.hasNext()) {
			Object key= keyIterator.next();
			//Replace the items in the checked state store with those from the supplied items
			List selections= (List) items.get(key);
			if (selections.size() == 0)
				//If it is empty remove it from the list
				fCheckedStateStore.remove(key);
			else {
				fCheckedStateStore.put(key, selections);
				// proceed up the tree element hierarchy
				Object parent= fTreeContentProvider.getParent(key);
				if (parent != null) {
					addToHierarchyToCheckedStore(parent);
				}
			}
		}

		//Now update hierarchies
		keyIterator= items.keySet().iterator();

		while (keyIterator.hasNext()) {
			Object key= keyIterator.next();
			updateHierarchy(key);
			if (fCurrentTreeSelection != null && fCurrentTreeSelection.equals(key)) {
				fListViewer.setAllChecked(false);
				fListViewer.setCheckedElements(((List) items.get(key)).toArray());
			}
		}
	}		
	
	/**
	 * Checks if an element is grey checked.
	 */
	public boolean isTreeItemGreyChecked(Object object) {
		return fTreeViewer.getGrayed(object);	
	}	

	/**
	 * For a given element, expand its chidren to a level.
	 */	
	public void expandTreeToLevel(Object object, int level) {
		fTreeViewer.expandToLevel(object, level);	
	}
	/**
	 * @param selection
	 */
	public void setTreeSelection(ISelection selection) {
		fTreeViewer.setSelection(selection);
	}
}
