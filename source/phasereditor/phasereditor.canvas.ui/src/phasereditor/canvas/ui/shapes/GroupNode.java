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

import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Shape;
import phasereditor.canvas.core.BaseObjectModel;

/**
 * @author arian
 *
 */
public class GroupNode extends Pane implements IObjectNode {

	private GroupControl _control;

	GroupNode(GroupControl control) {
		_control = control;
		setPickOnBounds(false);
	}

	@Override
	public GroupControl getControl() {
		return _control;
	}

	@Override
	public BaseObjectModel getModel() {
		return _control.getModel();
	}

	@Override
	public GroupNode getNode() {
		return this;
	}

	@Override
	public Shape computeShape() {
		Shape result = null;

		for (Node child : getChildren()) {
			Shape shape = ((IObjectNode) child).computeShape();
			if (result == null) {
				result = shape;
			} else {
				result = Shape.union(result, shape);
			}
		}
		
		return result;
	}
}
