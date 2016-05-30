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

import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Transform;
import phasereditor.canvas.ui.editors.ObjectCanvas;
import phasereditor.canvas.ui.editors.SelectionNode;
import phasereditor.canvas.ui.shapes.GroupNode;

/**
 * @author arian
 *
 */
public class ZoomBehavior {
	private ObjectCanvas _canvas;
	private double _scale;
	private Point2D _translate;
	private Point2D _startPanPoint;
	private Point2D _startTranslationPoint;

	public ZoomBehavior(ObjectCanvas canvas) {
		super();
		_canvas = canvas;
		_scale = 1;
		_translate = new Point2D(0, 0);
	}

	void handleScroll(ScrollEvent e) {
		onZoom(e);
	}

	@SuppressWarnings("unused")
	void handleMouseReleased(MouseEvent e) {
		if (isPanning()) {
			onPanDone();
		}
	}

	void handleMouseDragged(MouseEvent e) {
		if (isPanning()) {
			onPan(e);
		}
	}

	void handleMousePressed(MouseEvent e) {
		onPanInit(e);
	}

	private void onPanDone() {
		_startPanPoint = null;
		_startTranslationPoint = null;
	}

	public boolean isPanning() {
		return _startPanPoint != null;
	}

	private void onZoom(ScrollEvent e) {
		double delta = 1.2;

		double scale = _scale;

		if (e.getDeltaY() < 0) {
			scale /= delta;
		} else {
			scale *= delta;
		}

		scale = clamp(scale, 0.01d, 10d);

		GroupNode world = getCanvas().getWorldNode();

		Bounds now = world.getBoundsInParent();
		Point2D local = world.sceneToLocal(e.getSceneX(), e.getSceneY());
		double fw = local.getX() / world.getBoundsInLocal().getWidth();
		double fh = local.getY() / world.getBoundsInLocal().getHeight();

		_scale = scale;
		updateZoomAndPan();

		Bounds later = world.getBoundsInParent();

		double wd = now.getWidth() - later.getWidth();
		double wh = now.getHeight() - later.getHeight();

		_translate = _translate.add(wd * fw, wh * fh);

		updateZoomAndPan();
	}

	private void onPan(MouseEvent e) {
		Point2D p = new Point2D(e.getSceneX(), e.getSceneY());

		Point2D delta = p.subtract(_startPanPoint);
		_translate = delta.add(_startTranslationPoint);

		updateZoomAndPan();
	}

	private void onPanInit(MouseEvent e) {
		Point2D p = new Point2D(e.getSceneX(), e.getSceneY());
		_startPanPoint = p;
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
		
		_canvas.getGridNode().repaint();
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
