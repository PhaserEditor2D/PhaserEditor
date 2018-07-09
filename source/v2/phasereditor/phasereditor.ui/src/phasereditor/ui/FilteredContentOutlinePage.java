// The MIT License (MIT)
//
// Copyright (c) 2015, 2018 Arian Fornaris
//
// Permission is hereby granted, free of charge, to any person obtaining a
// copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to permit
// persons to whom the Software is furnished to do so, subject to the
// following conditions: The above copyright notice and this permission
// notice shall be included in all copies or substantial portions of the
// Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
// OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
// NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
// DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
// OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
// USE OR OTHER DEALINGS IN THE SOFTWARE.
package phasereditor.ui;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.part.Page;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

/**
 * 
 * A clone of the {@link OutlineContentProvider} but with a {@link FilteredTree}
 * 
 * @author arian
 *
 */
public class FilteredContentOutlinePage extends Page implements IContentOutlinePage, ISelectionChangedListener {

	private ListenerList<ISelectionChangedListener> selectionChangedListeners = new ListenerList<>();

	private TreeViewer treeViewer;

	private FilteredTree _filteredTree;

	/**
	 * Create a new content outline page.
	 */
	protected FilteredContentOutlinePage() {
		super();
	}

	@Override
	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		selectionChangedListeners.add(listener);
	}

	/**
	 * The <code>ContentOutlinePage</code> implementation of this
	 * <code>IContentOutlinePage</code> method creates a tree viewer. Subclasses
	 * must extend this method configure the tree viewer with a proper content
	 * provider, label provider, and input element.
	 * 
	 * @param parent
	 */
	@Override
	public void createControl(Composite parent) {
		_filteredTree = new FilteredTree(parent, getTreeStyle(), crearePattern(), true);
		treeViewer = _filteredTree.getViewer();
		treeViewer.addSelectionChangedListener(this);
	}

	@SuppressWarnings("static-method")
	public PatternFilter2 crearePattern() {
		return new PatternFilter2();
	}

	/**
	 * A hint for the styles to use while constructing the TreeViewer.
	 * <p>
	 * Subclasses may override.
	 * </p>
	 *
	 * @return the tree styles to use. By default, SWT.MULTI | SWT.H_SCROLL |
	 *         SWT.V_SCROLL
	 * @since 3.6
	 */
	@SuppressWarnings("static-method")
	protected int getTreeStyle() {
		return SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL;
	}

	/**
	 * Fires a selection changed event.
	 *
	 * @param selection
	 *            the new selection
	 */
	protected void fireSelectionChanged(ISelection selection) {
		// create an event
		final SelectionChangedEvent event = new SelectionChangedEvent(this, selection);

		// fire the event
		for (final ISelectionChangedListener l : selectionChangedListeners) {
			SafeRunner.run(new SafeRunnable() {
				@Override
				public void run() {
					l.selectionChanged(event);
				}
			});
		}
	}

	@Override
	public Control getControl() {
		if (_filteredTree == null) {
			return null;
		}
		return _filteredTree;
	}

	@Override
	public ISelection getSelection() {
		if (treeViewer == null) {
			return StructuredSelection.EMPTY;
		}
		return treeViewer.getSelection();
	}

	/**
	 * Returns this page's tree viewer.
	 *
	 * @return this page's tree viewer, or <code>null</code> if
	 *         <code>createControl</code> has not been called yet
	 */
	protected TreeViewer getTreeViewer() {
		return treeViewer;
	}

	@Override
	public void init(IPageSite pageSite) {
		super.init(pageSite);
		pageSite.setSelectionProvider(this);
	}

	@Override
	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		selectionChangedListeners.remove(listener);
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		fireSelectionChanged(event.getSelection());
	}

	/**
	 * Sets focus to a part in the page.
	 */
	@Override
	public void setFocus() {
		getControl().setFocus();
	}

	@Override
	public void setSelection(ISelection selection) {
		if (treeViewer != null) {
			treeViewer.setSelection(selection);
		}
	}
	
	public TreeViewer getViewer() {
		return getTreeViewer();
	}

	public void refresh() {
		getTreeViewer().refresh();
	}

}
