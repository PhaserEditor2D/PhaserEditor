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

import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import phasereditor.canvas.ui.editors.ObjectCanvas;

/**
 * @author arian
 *
 */
public class MouseBehavior {
	private ObjectCanvas _canvas;
	private ZoomBehavior _zoomPan;
	private DragBehavior _drag;
	private SelectionBehavior _selection;

	public MouseBehavior(ObjectCanvas canvas) {
		super();
		_canvas = canvas;

		_canvas.getScene().addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
			try {
				handleMousePressed(e);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		});

		_canvas.getScene().addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
			try {
				handleMouseReleased(e);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		});

		_canvas.getScene().addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> {
			try {
				handleMouseDragged(e);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		});

		_canvas.getScene().addEventFilter(MouseEvent.DRAG_DETECTED, e -> {
			try {
				handleDragDetected(e);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		});

		_canvas.getScene().addEventFilter(ScrollEvent.ANY, e -> {
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

	private void handleScroll(ScrollEvent e) {
		_zoomPan.handleScroll(e);
	}

	private void handleMousePressed(MouseEvent e) {
		if (e.isMiddleButtonDown()) {
			_zoomPan.handleMousePressed(e);
		}
	}

	private void handleDragDetected(MouseEvent e) {
		if (e.isPrimaryButtonDown()) {
			if (_selection.isPointingToSelection(e)) {
				_drag.handleDragDetected(e);
			} else {
				_selection.handleDragDetected(e);
			}
		}
	}

	private void handleMouseDragged(MouseEvent e) {
		if (e.isMiddleButtonDown()) {
			_zoomPan.handleMouseDragged(e);
			return;
		}

		if (e.isPrimaryButtonDown()) {
			if (_drag.isDragging()) {
				_drag.handleMouseDragged(e);
				return;
			}
			_selection.handleMouseDragged(e);
		}
	}

	private void handleMouseReleased(MouseEvent e) {
		if (e.isMiddleButtonDown()) {
			_zoomPan.handleMouseReleased(e);
			return;
		}

		if (e.getButton() == MouseButton.PRIMARY) {
			if (_drag.isDragging()) {
				_drag.handleMouseReleased(e);
				return;
			}
			_selection.handleMouseReleased(e);
		}
	}

}
