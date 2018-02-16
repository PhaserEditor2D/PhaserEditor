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

import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import phasereditor.canvas.core.EditorSettings;
import phasereditor.canvas.ui.editors.ObjectCanvas;
import phasereditor.canvas.ui.editors.edithandlers.IEditHandlerNode;

/**
 * @author arian
 *
 */
public class MouseBehavior {
	private ObjectCanvas _canvas;
	private ZoomBehavior _zoomPan;
	private DragBehavior _drag;
	private SelectionBehavior _selection;
	private IEditHandlerNode _editHandler;
	private Point2D _mousePosition;

	public MouseBehavior(ObjectCanvas canvas) {
		super();
		_canvas = canvas;

		_canvas.getScene().addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
			try {
				handleMousePressed(e);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		});

		_canvas.getScene().addEventHandler(MouseEvent.MOUSE_RELEASED, e -> {
			try {
				handleMouseReleased(e);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		});

		_canvas.getScene().addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
			try {
				handleMouseDragged(e);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		});

		_canvas.getScene().addEventHandler(MouseEvent.DRAG_DETECTED, e -> {
			try {
				handleDragDetected(e);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		});

		_canvas.getScene().addEventHandler(MouseEvent.MOUSE_MOVED, e -> {
			try {
				handleMouseMoved(e);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		});

		_canvas.getScene().addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
			try {
				handleMouseExited(e);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		});

		_canvas.getScene().addEventHandler(ScrollEvent.ANY, e -> {
			try {
				handleScroll(e);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		});

		_selection = _canvas.getSelectionBehavior();
		_drag = _canvas.getDragBehavior();
		_zoomPan = _canvas.getZoomBehavior();
	}

	public Point2D getMousePosition() {
		return _mousePosition;
	}

	private void handleMouseMoved(MouseEvent e) {
		_mousePosition = new Point2D(e.getSceneX(), e.getSceneY());
		if (_editHandler != null) {
			_editHandler.handleMouseMoved(e);
		}
	}

	private void handleMouseExited(MouseEvent e) {
		_mousePosition = null;
		if (_editHandler != null) {
			_editHandler.handleMouseExited(e);
		}
	}

	private void handleScroll(ScrollEvent e) {
		_zoomPan.handleScroll(e);
	}

	private void handleMousePressed(MouseEvent e) {

		Node node = e.getPickResult().getIntersectedNode();
		if (node instanceof IEditHandlerNode) {
			_editHandler = (IEditHandlerNode) node;
			_editHandler.handleMousePressed(e);
			return;
		}

		if (isInsideEditHandler(node)) {
			// do nothing
			return;
		}

		_editHandler = null;

		if (isPanGesture(e)) {
			_zoomPan.handleMousePressed(e);
			return;
		}

		if (e.isSecondaryButtonDown()) {
			_selection.handleMouseReleased(e);
		}
	}

	private boolean isInsideEditHandler(Node node) {
		Parent parent = node.getParent();

		if (parent == null) {
			return false;
		}

		if (parent instanceof IEditHandlerNode) {
			return true;
		}

		return isInsideEditHandler(parent);
	}

	private void handleDragDetected(MouseEvent e) {
		if (e.isPrimaryButtonDown()) {
			if (_editHandler != null) {
				return;
			}

			if (_selection.isPointingToSelection(e)) {
				_drag.handleDragDetected(e);
			} else {
				_selection.handleDragDetected(e);
			}
		}
	}

	private void handleMouseDragged(MouseEvent e) {
		if (isPanGesture(e)) {
			_zoomPan.handleMouseDragged(e);
			return;
		}

		if (e.isPrimaryButtonDown()) {

			if (_editHandler != null) {
				_editHandler.handleMouseDragged(e);
				return;
			}

			if (_drag.isDragging()) {
				_drag.handleMouseDragged(e);
				return;
			}
			_selection.handleMouseDragged(e);
		}
	}

	private static boolean isPanGesture(MouseEvent e) {
		return e.isMiddleButtonDown() || e.isPrimaryButtonDown() && e.isAltDown();
	}

	private void handleMouseReleased(MouseEvent e) {
		if (isPanGesture(e)) {
			_zoomPan.handleMouseReleased(e);
			return;
		}

		if (e.getButton() == MouseButton.PRIMARY) {
			if (_editHandler != null) {
				_editHandler.handleMouseReleased(e);
				_editHandler = null;
				return;
			}

			if (_drag.isDragging()) {
				_drag.handleMouseReleased(e);
				return;
			}

			_selection.handleMouseReleased(e);
		}
	}

	public double stepX(double x, boolean round) {
		EditorSettings s = _canvas.getSettingsModel();
		if (s.isEnableStepping()) {
			double v = x / s.getStepWidth();
			if (round) {
				v = Math.round(v);
			} else {
				v = Math.floor(v);
			}
			return v * s.getStepWidth();
		}
		return x;
	}

	public double stepY(double y, boolean round) {
		EditorSettings s = _canvas.getSettingsModel();
		if (s.isEnableStepping()) {
			double v = y / s.getStepHeight();
			if (round) {
				v = Math.round(v);
			} else {
				v = Math.floor(v);
			}
			return v * s.getStepHeight();
		}
		return y;
	}
}
