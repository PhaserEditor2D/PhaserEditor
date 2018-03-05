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

import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import phasereditor.canvas.ui.editors.operations.ChangePropertyOperation;
import phasereditor.canvas.ui.editors.operations.CompositeOperation;
import phasereditor.canvas.ui.shapes.GroupNode;
import phasereditor.canvas.ui.shapes.IObjectNode;

/**
 * @author arian
 *
 */
public class ScaleHandlerNode extends PathHandlerNode {

	private Axis _axis;
	private double _initScaleX;
	private double _initScaleY;
	private double _initWidth;
	private double _initHeight;
	private double _scaledInitWidth;
	private double _scaledInitHeight;
	private double _initX;
	private double _initY;

	public ScaleHandlerNode(IObjectNode object, Axis axis) {
		super(object);
		_axis = axis;
		
		setCursor(Cursor.MOVE);

		Paint color = _axis == Axis.CENTER ? Color.BLUE
				: (_axis.changeW() ? Color.RED.brighter() : Color.LIGHTGREEN);

		setFill(color);

	}

	@Override
	public void handleLocalStart(double localX, double localY, MouseEvent e) {
		Bounds bounds = _node.getBoundsInLocal();

		_initScaleX = _model.getScaleX();
		_initScaleY = _model.getScaleY();
		_initWidth = bounds.getWidth();
		_initHeight = bounds.getHeight();
		_scaledInitWidth = _initWidth * _model.getScaleX();
		_scaledInitHeight = _initHeight * _model.getScaleY();
		_initX = _model.getX();
		_initY = _model.getY();

	}

	@Override
	public void handleLocalDrag(double dx, double dy, MouseEvent e) {
		if (e.isShiftDown()) {
			boolean updateX = false;
			boolean updateY = false;

			if (_axis.x != 0.5 && _axis.y != 0.5) {
				updateY = _axis.y == 0;
				updateX = _axis.y == 1;
			} else if (_axis.y == 0.5) {
				updateX = true;
			} else if (_axis.x == 0.5) {
				updateY = true;
			}

			if (updateX) {
				double x = updateScaleX(dx);
				_model.setScaleY(x);
			}

			if (updateY) {
				double y = updateScaleY(dy);
				_model.setScaleX(y);
			}

		} else {
			if (_axis.changeW()) {
				updateScaleX(dx);
			}

			if (_axis.changeH()) {
				updateScaleY(dy);
			}
		}

	}

	private double updateScaleY(double dy) {
		double sign = _axis.signH();
		double y = (_scaledInitHeight + sign * dy * _model.getScaleY()) / _initHeight;
		_model.setScaleY(y);

		if (_axis.y == 0) {
			_model.setY(_initY + dy * _model.getScaleY());
		}
		return y;
	}

	private double updateScaleX(double dx) {
		double sign = _axis.signW();
		double x = (_scaledInitWidth + sign * dx * _model.getScaleX()) / _initWidth;
		_model.setScaleX(x);

		if (_axis.x == 0) {
			_model.setX(_initX + dx * _model.getScaleX());
		}
		return x;
	}

	@Override
	public void handleDone() {
		double scaleX = _model.getScaleX();
		double scaleY = _model.getScaleY();

		double x = _model.getX();
		double y = _model.getY();

		_model.setScaleX(_initScaleX);
		_model.setScaleY(_initScaleY);

		_model.setX(_initX);
		_model.setY(_initY);

		String id = _model.getId();

		_canvas.getUpdateBehavior().executeOperations(

				new CompositeOperation(

						new ChangePropertyOperation<Number>(id, "scale.x", Double.valueOf(scaleX)),

						new ChangePropertyOperation<Number>(id, "scale.y", Double.valueOf(scaleY)),

						new ChangePropertyOperation<Number>(id, "x", Double.valueOf(x)),

						new ChangePropertyOperation<Number>(id, "y", Double.valueOf(y))

				));
	}

	@Override
	public void updateHandler() {
		Point2D p;
		if (_object instanceof GroupNode) {

			Point2D center = computeObjectCenter_global();

			p = new Point2D(center.getX(), center.getY());

			double N = 50;

			if (_axis == Axis.RIGHT) {

				//@formatter:off
				getElements().setAll(

						new MoveTo(0, -1),

						new LineTo(N, -1),
						
						new LineTo(N, -5),
						new LineTo(N + 10, -5),
						new LineTo(N + 10, 5),
						new LineTo(N, 5),
						
						new LineTo(N, 1),
						new LineTo(0, 1),
						
						new ClosePath()

				);
				//@formatter:on
				relocate(p.getX(), p.getY() - 5);

			} else {
				//@formatter:off
				getElements().setAll(

						new MoveTo(-1, 0),

						new LineTo(-1, N),
						
						new LineTo(-5, N),
						new LineTo(-5, N + 10),
						new LineTo(5, N + 10),
						new LineTo(5, N),
						
						new LineTo(1, N),
						new LineTo(1, 0),
						
						new ClosePath()

				);
				//@formatter:on
				relocate(p.getX() - 5, p.getY());
			}
		} else {
			double x = _axis.x * _control.getTextureWidth();
			double y = _axis.y * _control.getTextureHeight();

			p = objectToScene(x, y);

			relocate(p.getX() - 5, p.getY() - 5);
		}

	}

}
