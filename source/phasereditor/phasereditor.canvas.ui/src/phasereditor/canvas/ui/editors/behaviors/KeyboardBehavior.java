// The MIT License (MIT)
//
// Copyright (c) 2015, 2017 Arian Fornaris
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
package phasereditor.canvas.ui.editors.behaviors;

import java.util.List;

import javafx.scene.input.KeyEvent;
import phasereditor.canvas.core.BaseObjectModel;
import phasereditor.canvas.core.EditorSettings;
import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.canvas.ui.editors.ObjectCanvas;
import phasereditor.canvas.ui.editors.operations.CompositeOperation;
import phasereditor.canvas.ui.editors.operations.UpdateFromPropertyChange;
import phasereditor.canvas.ui.shapes.IObjectNode;

/**
 * @author arian
 *
 */
public class KeyboardBehavior {
	private ObjectCanvas _canvas;

	public KeyboardBehavior(ObjectCanvas canvas) {
		_canvas = canvas;

		_canvas.getScene().addEventFilter(KeyEvent.KEY_PRESSED, e -> {
			if (_canvas.getEditor().isContextActive(CanvasEditor.NODES_CONTEXT_ID)) {
				try {
					handleKeyPressed(e);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		});
	}

	private void handleKeyPressed(KeyEvent e) {
		boolean shift = e.isShiftDown();
		switch (e.getCode()) {
		case LEFT:
			move(-1, 0, shift);
			break;
		case RIGHT:
			move(1, 0, shift);
			break;
		case UP:
			move(0, -1, shift);
			break;
		case DOWN:
			move(0, 1, shift);
			break;
		default:
			break;
		}
	}

	private void move(double dx, double dy, boolean shift) {
		double fx = shift ? 10 : 1;
		double fy = shift ? 10 : 1;

		EditorSettings settings = _canvas.getSettingsModel();

		boolean enabledStepping = settings.isEnableStepping();
		int stepWidth = settings.getStepWidth();
		int stepHeight = settings.getStepHeight();

		if (enabledStepping) {
			fx = fx * stepWidth;
			fy = fy * stepHeight;
		}

		CompositeOperation operations = new CompositeOperation();
		UpdateBehavior update = _canvas.getUpdateBehavior();
		UpdateFromPropertyChange updateFromPropChanges = new UpdateFromPropertyChange();

		List<IObjectNode> nodes = _canvas.getSelectionBehavior().getSelectedNodes();

		for (IObjectNode node : nodes) {
			BaseObjectModel model = node.getModel();
			double x = model.getX() + dx * fx;
			double y = model.getY() + dy * fy;

			if (enabledStepping) {
				x = Math.round(x / stepWidth) * stepWidth;
				y = Math.round(y / stepHeight) * stepHeight;
			}

			update.addUpdateLocationOperation(operations, node, x, y, false);
			updateFromPropChanges.add(node.getControl().getId());
		}

		operations.add(updateFromPropChanges);
		update.executeOperations(operations);
	}

}
