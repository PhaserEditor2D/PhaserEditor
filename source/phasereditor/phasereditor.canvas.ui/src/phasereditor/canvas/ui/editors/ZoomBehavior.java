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

import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Transform;
import phasereditor.canvas.ui.shapes.GroupNode;

/**
 * @author arian
 *
 */
public class ZoomBehavior {
	private ObjectCanvas _canvas;
	private double _scale;
	private Point2D _translate;
	private Point2D _startDragPoint;
	private Point2D _startTranslationPoint;

	public ZoomBehavior(ObjectCanvas canvas) {
		super();
		_canvas = canvas;
		_scale = 1;
		_translate = new Point2D(0, 0);

		_canvas.getScene().addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
			if (e.getButton() == MouseButton.MIDDLE) {
				onPanInit(e);
				e.consume();
			}
		});

		_canvas.getScene().addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> {
			if (isDragging()) {
				onPan(e);
				e.consume();
			}
		});

		_canvas.getScene().addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
			if (isDragging()) {
				onPanDone();
				e.consume();
			}
		});

		_canvas.getScene().addEventFilter(ScrollEvent.ANY, e -> {
			onZoom(e);
			e.consume();
		});
	}

	private void onPanDone() {
		_startDragPoint = null;
		_startTranslationPoint = null;
	}

	private boolean isDragging() {
		return _startDragPoint != null;
	}

	private void onZoom(ScrollEvent e) {
		double delta = 1.2;

		double scale = _scale;

		if (e.getDeltaY() < 0) {
			scale /= delta;
		} else {
			scale *= delta;
		}

		scale = clamp(scale, 0.1d, 10d);

		_scale = scale;

		updateZoomAndPan();
	}

	private void onPan(MouseEvent e) {
		Point2D p = new Point2D(e.getSceneX(), e.getSceneY());

		Point2D delta = p.subtract(_startDragPoint);
		_translate = delta.add(_startTranslationPoint);

		updateZoomAndPan();
	}

	private void onPanInit(MouseEvent e) {
		Point2D p = new Point2D(e.getSceneX(), e.getSceneY());
		_startDragPoint = p;
		GroupNode world = _canvas.getWorldNode();
		_startTranslationPoint = new Point2D(world.getTranslateX(), world.getTranslateY());
		return;
	}

	private void updateZoomAndPan() {
		GroupNode world = _canvas.getWorldNode();

		world.setTranslateX(_translate.getX());
		world.setTranslateY(_translate.getY());

		ObservableList<Transform> tx = world.getTransforms();
		tx.clear();
		tx.add(new Scale(_scale, _scale));

		_canvas.getSelectionPane().getChildren().forEach(n -> {
			((SelectionNode) n).updateZoomAndPan();
		});
	}

	public double getScale() {
		return _scale;
	}

	public void setScale(double scale) {
		_scale = scale;
		updateZoomAndPan();
	}

	public Point2D getTranslate() {
		GroupNode world = getCanvas().getWorldNode();
		return new Point2D(world.getTranslateX(), world.getTranslateY());
	}

	public void setTranslate(Point2D translate) {
		_translate = translate;
		updateZoomAndPan();
	}

	static double clamp(double value, double min, double max) {

		if (Double.compare(value, min) < 0)
			return min;

		if (Double.compare(value, max) > 0)
			return max;

		return value;
	}

	public ObjectCanvas getCanvas() {
		return _canvas;
	}

}
