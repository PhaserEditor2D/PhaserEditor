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

import java.util.ArrayList;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;

import phasereditor.ui.TreeCanvas.IconType;
import phasereditor.ui.TreeCanvas.TreeCanvasItem;

/**
 * @author arian
 *
 */
public class TreeCanvasViewer implements IEditorSharedImages, ISelectionProvider {

	private ITreeContentProvider _contentProvider;
	private LabelProvider _labelProvider;
	private Object _input;
	private TreeCanvas _canvas;

	public TreeCanvasViewer(TreeCanvas canvas, ITreeContentProvider contentProvider, LabelProvider labelProvider) {
		super();
		_canvas = canvas;
		_contentProvider = contentProvider;
		_labelProvider = labelProvider;
	}

	public void setInput(Object input) {
		_input = input;
		refreshContent();
	}

	public Object getInput() {
		return _input;
	}

	public void refresh() {
		refreshContent();
		refreshLabels();
	}

	protected void refreshContent() {
		var roots = new ArrayList<TreeCanvasItem>();

		if (_input != null) {
			for (Object elem : _contentProvider.getElements(_input)) {
				var item = buildItem(elem);
				roots.add(item);
			}
		}

		_canvas.setRoots(roots);

	}
	
	public void expandToLevel(Object elem, int level) {
		_canvas.expandToLevel(elem, level);
	}

	protected void refreshLabels() {
		for (var item : _canvas.getItems()) {
			setItemProperties(item);
		}
	}

	public TreeCanvas getCanvas() {
		return _canvas;
	}

	private TreeCanvasItem buildItem(Object elem) {
		var item = new TreeCanvasItem();
		item.setData(elem);

		refreshItem(item);

		return item;
	}

	private void refreshItem(TreeCanvasItem item) {
		setItemProperties(item);

		var children = _contentProvider.getChildren(item.getData());

		for (var child : children) {
			var item2 = buildItem(child);
			item.getChildren().add(item2);
		}
	}

	protected void setItemProperties(TreeCanvasItem item) {
		item.setLabel(_labelProvider.getText(item.getData()));
		setItemIconProperties(item);
	}

	protected void setItemIconProperties(TreeCanvasItem item) {
		item.setIcon(_labelProvider.getImage(item.getData()));
		item.setIconType(IconType.COMMON_ICON);
	}

	public LabelProvider getLabelProvider() {
		return _labelProvider;
	}

	public ITreeContentProvider getContentProvider() {
		return _contentProvider;
	}

	@Override
	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		_canvas.getUtils().addSelectionChangedListener(listener);
	}

	@Override
	public ISelection getSelection() {
		return _canvas.getUtils().getSelection();
	}

	@Override
	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		_canvas.getUtils().removeSelectionChangedListener(listener);
	}

	@Override
	public void setSelection(ISelection selection) {
		_canvas.getUtils().setSelection(selection);
		_canvas.redraw();
	}
}
