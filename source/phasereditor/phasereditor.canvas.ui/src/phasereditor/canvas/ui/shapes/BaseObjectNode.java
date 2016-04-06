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

import javafx.scene.layout.Pane;
import phasereditor.canvas.core.BaseObjectModel;

/**
 * @author arian
 *
 */
public class BaseObjectNode extends Pane {
	private static final int SELECTION_BORDER_WIDTH = 2;
	private BaseObjectControl<?> _control;
	private boolean _selected;

	BaseObjectNode(BaseObjectControl<?> control) {
		_control = control;
		updateStyle();
		setPickOnBounds(false);
	}

	protected void updateStyle() {
		String style = "";
		style += "-fx-border-color: blue;";
		style += "-fx-border-width: " + (_selected ? SELECTION_BORDER_WIDTH : 0) + "px;";
		style += "-fx-border-style: dashed;";
		setStyle(style);
	}

	public BaseObjectModel getModel() {
		return getControl().getModel();
	}

	public int getDepthLevel() {
		GroupNode group = getGroup();

		if (group == null) {
			return 0;
		}

		return group.getDepthLevel() + 1;
	}

	public GroupNode getGroup() {
		if (getParent() instanceof GroupNode) {
			return (GroupNode) getParent();
		}
		return null;
	}

	public void setSelected(boolean selected) {
		_selected = selected;
		updateStyle();
	}

	public boolean isSelected() {
		return _selected;
	}

	public BaseObjectControl<?> getControl() {
		return _control;
	}
}
