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
package phasereditor.canvas.ui.editors.edithandlers;

import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import phasereditor.canvas.ui.shapes.IObjectNode;

/**
 * @author arian
 *
 */
public class HandlerNodeHelper {

	private IEditHandlerNode _node;
	private Point2D _start;

	public HandlerNodeHelper(IEditHandlerNode node) {
		_node = node;
	}

	public void handleMousePressed(MouseEvent e) {
		_start = new Point2D(e.getSceneX(), e.getSceneY());
		_node.handleSceneStart(e.getSceneX(), e.getSceneY());
		Point2D startLocal = _node.sceneToObject(e.getSceneX(), e.getSceneY());
		_node.handleLocalStart(startLocal.getX(), startLocal.getY());
	}

	public void handleMouseDragged(MouseEvent e) {
		if (_start == null) {
			return;
		}

		double cursorX = e.getSceneX();
		double cursorY = e.getSceneY();
		double startX = _start.getX();
		double startY = _start.getY();

		IObjectNode obj = _node.getObject();
		Node node = obj.getNode();

		{
			double dx = cursorX - _start.getX();
			double dy = cursorY - _start.getY();
			_node.handleSceneDrag(dx, dy);
		}

		{
			Point2D localCursor = node.sceneToLocal(cursorX, cursorY);
			Point2D localStart = node.sceneToLocal(startX, startY);

			double localDX = localCursor.getX() - localStart.getX();
			double localDY = localCursor.getY() - localStart.getY();

			_node.handleLocalDrag(localDX, localDY);
		}
		obj.getControl().updateFromModel();
		obj.getControl().getCanvas().getSelectionBehavior().updateSelectedNodes();
	}

	public void handleMouseReleased(@SuppressWarnings("unused") MouseEvent e) {
		_node.handleDone();
	}
}
