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
package phasereditor.canvas.ui.editors.behaviors;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.input.MouseEvent;
import phasereditor.canvas.core.BaseObjectModel;
import phasereditor.canvas.core.EditorSettings;
import phasereditor.canvas.ui.editors.ObjectCanvas;
import phasereditor.canvas.ui.editors.edithandlers.Axis;
import phasereditor.canvas.ui.editors.operations.CompositeOperation;
import phasereditor.canvas.ui.editors.operations.UpdateFromPropertyChange;
import phasereditor.canvas.ui.shapes.IObjectNode;

/**
 * @author arian
 *
 */
public class DragBehavior {
	private ObjectCanvas _canvas;
	private Point2D _startScenePoint;
	private List<DragInfo> _dragInfoList;
	private SelectionBehavior _selbehavior;
	private boolean _dragging;
	private boolean _fixedAxisX;
	private boolean _fixedAxisY;

	static class DragInfo {
		private Node _node;
		public final double _initX;
		public final double _initY;
		public final double _initModelX;
		public final double _initModelY;

		public DragInfo(Node node) {
			super();
			this._node = node;

			BaseObjectModel model = getModel();

			_initModelX = model.getX();
			_initModelY = model.getY();

			Point2D p = getObject().getNode().getParent().localToScene(_initModelX, _initModelY);

			_initX = p.getX();
			_initY = p.getY();
		}

		public BaseObjectModel getModel() {
			return getObject().getModel();
		}

		public Node getNode() {
			return _node;
		}

		public IObjectNode getObject() {
			return (IObjectNode) _node;
		}

	}

	public DragBehavior(ObjectCanvas canvas) {
		super();
		_canvas = canvas;
		_selbehavior = canvas.getSelectionBehavior();
		_dragInfoList = new ArrayList<>();
	}

	void handleMouseReleased(@SuppressWarnings("unused") MouseEvent event) {
		_dragging = false;

		CompositeOperation operations = new CompositeOperation();
		UpdateBehavior update = _canvas.getUpdateBehavior();
		UpdateFromPropertyChange updateFromPropChanges = new UpdateFromPropertyChange();

		for (DragInfo info : _dragInfoList) {
			double x = info.getModel().getX();
			double y = info.getModel().getY();
			IObjectNode object = info.getObject();
			update.addUpdateLocationOperation(operations, object, x, y, false);
			updateFromPropChanges.add(object.getControl().getId());
		}

		operations.add(updateFromPropChanges);
		update.executeOperations(operations);

		_dragInfoList.clear();
	}

	void handleMouseDragged(MouseEvent event) {
		double dx = event.getSceneX() - _startScenePoint.getX();
		double dy = event.getSceneY() - _startScenePoint.getY();

		if (event.isShiftDown()) {
			if (!_fixedAxisX && !_fixedAxisY) {
				if (Math.abs(dx) > Math.abs(dy)) {
					_fixedAxisX = true;
					_fixedAxisY = false;
				} else {
					_fixedAxisX = false;
					_fixedAxisY = true;
				}
			}

			if (_fixedAxisX) {
				dy = 0;
			}

			if (_fixedAxisY) {
				dx = 0;
			}
		} else {
			_fixedAxisX = false;
			_fixedAxisY = false;
		}

		Axis axis = Axis.CENTER;
		if (_fixedAxisX) {
			axis = Axis.RIGHT;
		}
		if (_fixedAxisY) {
			axis = Axis.TOP;
		}

		for (DragInfo info : _dragInfoList) {
			Point2D p = null;
			
			if (axis == Axis.CENTER) {

				p = _canvas.getDragBehavior().adjustPositionToStep(info._initX + dx, info._initY + dy);

			} else {

				if (axis.changeW()) {
					p = _canvas.getDragBehavior().adjustPositionToStep(info._initX + dx, info._initY);
				}

				if (axis.changeH()) {
					p = _canvas.getDragBehavior().adjustPositionToStep(info._initX, info._initY + dy);
				}
			}

			IObjectNode object = info.getObject();
			Node node = object.getNode();
			Parent parent = node.getParent();
			p = parent.sceneToLocal(p);

			BaseObjectModel model = info.getModel();
			
			model.setX(p.getX());
			model.setY(p.getY());
			
			object.getControl().updateFromModel();
			
		}

		_canvas.getSelectionBehavior().updateSelectedNodes();
	}

	public Point2D adjustPositionToStep(double x, double y) {
		double x1 = x;
		double y1 = y;
		EditorSettings settings = _canvas.getSettingsModel();
		if (settings.isEnableStepping()) {
			x1 = Math.round(x / settings.getStepWidth()) * settings.getStepWidth();
			y1 = Math.round(y / settings.getStepHeight()) * settings.getStepHeight();
		} else {
			// TODO: round position to integer
			x1 = Math.round(x);
			y1 = Math.round(y);
		}

		return new Point2D(x1, y1);
	}

	void handleDragDetected(MouseEvent event) {
		_dragging = true;
		_fixedAxisX = false;
		_fixedAxisY = false;

		for (IObjectNode selnode : _selbehavior.getSelectedNodes()) {
			Node dragnode = selnode.getNode();

			if (_dragInfoList.stream().anyMatch(info -> info.getNode() == dragnode)) {
				continue;
			}

			DragInfo info = new DragInfo(dragnode);

			_dragInfoList.add(info);
		}

		_startScenePoint = new Point2D(event.getSceneX(), event.getSceneY());
	}

	public void abort() {
		for (DragInfo info : _dragInfoList) {

			BaseObjectModel model = info.getModel();
			model.setX(info._initModelX);
			model.setY(info._initModelY);
		}

		_dragInfoList.clear();

		_selbehavior.updateSelectedNodes();
	}

	public boolean isDragging() {
		return _dragging;
	}
}
