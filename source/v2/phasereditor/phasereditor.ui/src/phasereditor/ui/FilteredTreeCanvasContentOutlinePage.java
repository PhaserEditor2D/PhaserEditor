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
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.part.Page;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

/**
 * @author arian
 *
 */
public abstract class FilteredTreeCanvasContentOutlinePage extends Page
		implements IContentOutlinePage, ISelectionChangedListener {
	private FilteredTreeCanvas _filteredTreeCanvas;
	private SelectionProviderImpl _selProvider;
	private TreeCanvasViewer _viewer;
	private TreeCanvas _canvas;

	public FilteredTreeCanvasContentOutlinePage() {
	}

	public FilteredTreeCanvas getFilteredTreeCanvas() {
		return _filteredTreeCanvas;
	}

	public TreeCanvas getCanvas() {
		return _canvas;
	}

	@Override
	public void createControl(Composite parent) {
		_filteredTreeCanvas = new FilteredTreeCanvas(parent, SWT.NONE);
		_canvas = _filteredTreeCanvas.getTree();
		_viewer = createViewer();

		for (var l : _initialListeners) {
			_filteredTreeCanvas.getUtils().addSelectionChangedListener(l);
		}

		if (_initialSelection != null) {
			_filteredTreeCanvas.getUtils().setSelection(_initialSelection);
			_filteredTreeCanvas.redraw();
		}

		_initialListeners.clear();
		_initialSelection = null;
	}

	public TreeCanvasViewer getTreeViewer() {
		return _viewer;
	}

	protected abstract TreeCanvasViewer createViewer();

	private ListenerList<ISelectionChangedListener> _initialListeners = new ListenerList<>();
	private ISelection _initialSelection;

	@Override
	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		if (_filteredTreeCanvas == null) {
			_initialListeners.add(listener);
			return;
		}

		_filteredTreeCanvas.getUtils().addSelectionChangedListener(listener);
	}

	@Override
	public ISelection getSelection() {
		return _filteredTreeCanvas.getUtils().getSelection();
	}

	@Override
	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		if (_filteredTreeCanvas == null) {
			_initialListeners.remove(listener);
			return;
		}

		_filteredTreeCanvas.getUtils().removeSelectionChangedListener(listener);
	}

	@Override
	public void setSelection(ISelection selection) {
		if (_filteredTreeCanvas == null) {
			_initialSelection = selection;
			return;
		}

		_filteredTreeCanvas.getUtils().setSelection(selection);
		_filteredTreeCanvas.redraw();
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		_selProvider.fireSelectionChanged();
	}

	@Override
	public Control getControl() {
		return _filteredTreeCanvas;
	}

	@Override
	public void setFocus() {
		_filteredTreeCanvas.setFocus();
	}

	public void refresh() {
		if (_filteredTreeCanvas != null) {
			TreeCanvas canvas = getCanvas();

			var expanded = canvas.getExpandedObjects();

			_viewer.refresh();

			canvas.setExpandedObjects(expanded);
		}
	}
}
