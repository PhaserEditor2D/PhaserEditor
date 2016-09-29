// The MIT License (MIT)
//
// Copyright (c) 2015, 2016 Arian Fornaris
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
package phasereditor.canvas.ui.shapes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javafx.scene.Node;
import phasereditor.canvas.core.BaseObjectModel;

/**
 * @author arian
 *
 */
public interface IObjectNode {
	public BaseObjectModel getModel();

	public BaseObjectControl<?> getControl();

	public Node getNode();

	public default GroupNode getGroup() {
		return getControl().getGroup();
	}

	public default List<GroupNode> getAncestors() {
		List<GroupNode> list = new ArrayList<>();

		GroupNode parent = getGroup();

		if (parent != null && parent != getControl().getCanvas().getWorldNode()) {
			list.add(parent);
			list.addAll(parent.getAncestors());
		}
		return list;
	}

	public static Comparator<IObjectNode> DISPLAY_ORDER_COMPARATOR = (a, b) -> {
		BaseObjectModel i = a.getModel();
		BaseObjectModel j = b.getModel();
		int c = Integer.compare(i.getDepth(), j.getDepth());
		if (c == 0) {
			c = Integer.compare(i.getIndex(), j.getIndex());
		}
		return c;
	};
}
