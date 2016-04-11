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
package phasereditor.canvas.ui.editors;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import phasereditor.canvas.core.BaseObjectModel;
import phasereditor.canvas.ui.shapes.BaseObjectControl;
import phasereditor.canvas.ui.shapes.IObjectNode;

/**
 * @author arian
 *
 */
public class DragBehavior {
	private ShapeCanvas _canvas;
	private Scene _scene;
	private Point2D _startScenePoint;
	private List<DragInfo> _dragInfoList;
	private SelectionBehavior _selbehavior;
	private boolean _dragging;

	static class DragInfo {
		private Node _node;
		private Point2D _start;

		public DragInfo(Node node, Point2D start) {
			super();
			this._node = node;
			this._start = start;
		}

		public Node getNode() {
			return _node;
		}

		public Point2D getStart() {
			return _start;
		}

	}

	public DragBehavior(ShapeCanvas canvas) {
		super();
		_canvas = canvas;
		_scene = _canvas.getScene();
		_selbehavior = canvas.getSelectionBehavior();
		_dragInfoList = new ArrayList<>();

		_scene.addEventHandler(MouseEvent.MOUSE_PRESSED, this::handleMousePressed);
		_scene.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::handleMouseDragged);
		_scene.addEventHandler(MouseEvent.MOUSE_RELEASED, this::handleMouseReleased);
	}

	private void handleMouseReleased(@SuppressWarnings("unused") MouseEvent event) {
		if (_dragInfoList.isEmpty()) {
			return;
		}

		_dragging = false;

		for (DragInfo draginfo : _dragInfoList) {
			Node node = draginfo.getNode();
			BaseObjectControl<?> control = ((IObjectNode) node).getControl();
			BaseObjectModel model = control.getModel();
			model.setLocation(node.getLayoutX(), node.getLayoutY());

			UpdateChangeBehavior updateBehavior = _canvas.getUpdateBehavior();
			updateBehavior.update_Grid_from_PropertyChange(control.getX_property());
			updateBehavior.update_Grid_from_PropertyChange(control.getY_property());
		}

		_dragInfoList.clear();
	}

	private void handleMouseDragged(MouseEvent event) {
		if (_dragInfoList.isEmpty()) {
			return;
		}
		_dragging = true;
		double dx = event.getSceneX() - _startScenePoint.getX();
		double dy = event.getSceneY() - _startScenePoint.getY();

		for (DragInfo draginfo : _dragInfoList) {
			Node dragnode = draginfo.getNode();
			Point2D start = draginfo.getStart();

			Point2D delta = new Point2D(dx, dy);
			try {
				delta = dragnode.getParent().getLocalToSceneTransform().inverseDeltaTransform(dx, dy);
			} catch (Exception e) {
				e.printStackTrace();
			}

			dx = delta.getX();
			dy = delta.getY();

			dragnode.setLayoutX(start.getX() + dx);
			dragnode.setLayoutY(start.getY() + dy);
		}
		_canvas.getSelectionBehavior().updateSelectedNodes();
	}

	private void handleMousePressed(MouseEvent event) {
		if (event.getButton() != MouseButton.PRIMARY) {
			return;
		}

		for (IObjectNode selnode : _selbehavior.getSelectedNodes()) {
			Node dragnode = selnode.getNode();

			if (_dragInfoList.stream().anyMatch(info -> info.getNode() == dragnode)) {
				continue;
			}

			Point2D start = new Point2D(dragnode.getLayoutX(), dragnode.getLayoutY());
			_dragInfoList.add(new DragInfo(dragnode, start));
		}

		_startScenePoint = new Point2D(event.getSceneX(), event.getSceneY());
	}

	public void abort() {
		if (_dragInfoList.isEmpty()) {
			return;
		}

		for (DragInfo draginfo : _dragInfoList) {
			Point2D start = draginfo.getStart();
			Node node = draginfo.getNode();
			node.relocate(start.getX(), start.getY());
		}
		_dragInfoList.clear();
		_selbehavior.updateSelectedNodes();
	}

	public boolean isDragging() {
		return !_dragInfoList.isEmpty() && _dragging;
	}
}
