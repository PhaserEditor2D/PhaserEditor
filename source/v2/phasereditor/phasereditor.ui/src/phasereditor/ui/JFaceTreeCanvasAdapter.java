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
import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;

import phasereditor.ui.TreeCanvas.IconType;
import phasereditor.ui.TreeCanvas.TreeCanvasItem;

/**
 * @author arian
 *
 */
public class JFaceTreeCanvasAdapter implements IEditorSharedImages {

	private ITreeContentProvider _contentProvider;
	private LabelProvider _labelProvider;

	public JFaceTreeCanvasAdapter(ITreeContentProvider contentProvider, LabelProvider labelProvider) {
		super();
		_contentProvider = contentProvider;
		_labelProvider = labelProvider;
	}

	public List<TreeCanvasItem> build(Object input) {
		var roots = new ArrayList<TreeCanvasItem>();

		for (Object elem : _contentProvider.getElements(input)) {
			var item = buildItem(elem);
			roots.add(item);
		}

		return roots;
	}

	private TreeCanvasItem buildItem(Object elem) {
		var item = new TreeCanvasItem();

		setItemProperties(item, elem);

		var children = _contentProvider.getChildren(elem);

		for (var child : children) {
			var item2 = buildItem(child);
			item.getChildren().add(0, item2);
		}

		return item;
	}

	protected void setItemProperties(TreeCanvasItem item, Object elem) {
		item.setData(elem);
		item.setLabel(_labelProvider.getText(elem));
		setItemIconProperties(item, elem);
	}

	protected void setItemIconProperties(TreeCanvasItem item, Object elem) {
		item.setIcon(_labelProvider.getImage(elem));
		item.setIconType(IconType.COMMON_ICON);
	}

	public LabelProvider getLabelProvider() {
		return _labelProvider;
	}

	public ITreeContentProvider getContentProvider() {
		return _contentProvider;
	}
}
