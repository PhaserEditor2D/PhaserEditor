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
package phasereditor.canvas.ui.editors.outline;

import java.util.ArrayList;
import java.util.Collections;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import phasereditor.canvas.core.GroupModel;
import phasereditor.canvas.ui.editors.ObjectCanvas2;

/**
 * @author arian
 *
 */
public class OutlineContentProvider2 implements ITreeContentProvider {

	private static final Object[] EMPTY = {};

	private boolean _showRoot;

	public OutlineContentProvider2(boolean showRoot) {
		_showRoot = showRoot;
	}

	public OutlineContentProvider2() {
		this(false);
	}

	public boolean isShowRoot() {
		return _showRoot;
	}

	public void setShowRoot(boolean showRoot) {
		_showRoot = showRoot;
	}

	@Override
	public void dispose() {
		// nothing
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		// nothing
	}

	@Override
	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	@Override
	public Object[] getChildren(Object parent) {
		if (parent instanceof ObjectCanvas2) {
			var model = ((ObjectCanvas2) parent).getWorldModel();

			if (_showRoot) {
				return new Object[] { model };
			}

			return getChildren(model);
		}

		if (parent instanceof GroupModel) {
			var group = (GroupModel) parent;
			if (group.isPrefabInstance()) {
				return EMPTY;
			}

			var list = new ArrayList<>(group.getChildren());
			Collections.reverse(list);

			return list.toArray();
		}

		return EMPTY;
	}

	@Override
	public Object getParent(Object element) {
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		return getChildren(element).length > 0;
	}
}
