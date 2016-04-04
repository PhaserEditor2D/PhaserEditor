// The MIT License (MIT)
//
// Copyright (c) 2015 Arian Fornaris
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
package com.subshell.snippets.jface.tooltip.tooltipsupport;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.TreeItem;

/**
 * An {@link IInformationProvider} that provides information about the
 * {@link TreeItem}s of a {@link TreeViewer}.
 */
public class TreeViewerInformationProvider implements IInformationProvider {

	private final TreeViewer _viewer;

	/**
	 * Creates a new information provider for the given table viewer.
	 * 
	 * @param viewer
	 *            the table viewer
	 */
	public TreeViewerInformationProvider(TreeViewer viewer) {
		this._viewer = viewer;
	}

	@Override
	public Object getInformation(Point location) {
		Item item = _viewer.getTree().getItem(location);
		if (item != null) {
			Object data = item.getData();
			return data;
		}
		return null;
	}

	@Override
	public Rectangle getArea(Point location) {
		TreeItem item = _viewer.getTree().getItem(location);
		if (item != null) {
			return item.getBounds();
		}
		return null;
	}

}
