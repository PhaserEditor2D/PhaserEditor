// The MIT License (MIT)
//
// Copyright (c) 2015, 2018 Arian Fornaris
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
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.transform.Rotate;
import phasereditor.canvas.core.BaseSpriteModel;
import phasereditor.canvas.ui.editors.operations.ChangePropertyOperation;
import phasereditor.canvas.ui.editors.operations.CompositeOperation;
import phasereditor.canvas.ui.shapes.IObjectNode;

/**
 * @author arian
 *
 */
public class MoveHandlerNode extends PathHandlerNode {

	private Axis _axis;
	private double _initX;
	private double _initY;
	private double _initModelX;
	private double _initModelY;

	public MoveHandlerNode(Axis axis, IObjectNode object) {
		super(object);

		_axis = axis;

		setStroke(Color.BLACK);
		setStrokeWidth(0.5);

		setCursor(Cursor.MOVE);

		Paint color = _axis == Axis.CENTER ? Color.BLUE : (_axis.changeW() ? Color.RED.brighter() : Color.LIGHTGREEN);

		setFill(color);

		updateHandler();

	}

	@Override
	public void handleSceneStart(double x, double y, MouseEvent e) {
		_initModelX = _model.getX();
		_initModelY = _model.getY();

		Point2D p = _object.getNode().getParent().localToScene(_initModelX, _initModelY);

		_initX = p.getX();
		_initY = p.getY();
	}

	@Override
	public void handleSceneDrag(double dx, double dy, MouseEvent e) {
		Point2D p = null;

		if (isLocalCoords() && _axis != Axis.CENTER) {

			p = new Point2D(_initX + dx, _initY + dy);

			Point2D vector;

			if (_axis.changeW()) {
				vector = new Point2D(1, 0);
			} else {
				vector = new Point2D(0, 1);
			}

			Rotate rot = new Rotate(_model.getAngle(), 0, 0);
			vector = rot.transform(vector);

			p = _node.getParent().sceneToLocal(p);
			Point2D p0 = _node.getParent().sceneToLocal(new Point2D(_initX, _initY));
			p = p.add(p0.multiply(-1));

			double d = 0;

			if (_axis.changeW()) {
				d = p.getX();
			} else {
				d = p.getY();
			}

			vector = vector.multiply(d);

			p = _canvas.getDragBehavior().adjustPositionToStep(_initModelX + vector.getX(),
					_initModelY + vector.getY());

			_model.setX(p.getX());
			_model.setY(p.getY());

		} else {

			if (_axis == Axis.CENTER) {

				p = new Point2D(_initX + dx, _initY + dy);

			} else {

				if (_axis.changeW()) {
					p = new Point2D(_initX + dx, _initY);
				}

				if (_axis.changeH()) {
					p = new Point2D(_initX, _initY + dy);
				}
			}
			
			Node node = _object.getNode();
			Parent parent = node.getParent();
			p = parent.sceneToLocal(p);

			p = _canvas.getDragBehavior().adjustPositionToStep(p.getX(), p.getY());
			
			_model.setX(p.getX());
			_model.setY(p.getY());
		}
	}

	@Override
	public void handleDone() {

		double x = _model.getX();
		double y = _model.getY();

		_model.setX(_initModelX);
		_model.setY(_initModelY);

		String id = _model.getId();

		_canvas.getUpdateBehavior().executeOperations(

				new CompositeOperation(

						new ChangePropertyOperation<Number>(id, "x", Double.valueOf(x)),

						new ChangePropertyOperation<Number>(id, "y", Double.valueOf(y))

				));
	}

	@Override
	public void updateHandler() {
		double centerX = _model.getPivotX();
		double centerY = _model.getPivotY();

		if (_model instanceof BaseSpriteModel) {
			BaseSpriteModel spriteModel = (BaseSpriteModel) _model;
			double x2 = spriteModel.getAnchorX() * _control.getTextureWidth();
			double y2 = spriteModel.getAnchorY() * _control.getTextureHeight();
			centerX = centerX + x2;
			centerY = centerY + y2;
		}

		Point2D p = _node.localToScene(centerX, centerY);
		centerX = p.getX();
		centerY = p.getY();

		double x = centerX;
		double y = centerY;

		int N = 80;

		if (_axis == Axis.CENTER) {

			relocate(x - 5, y - 5);

		} else if (_axis.changeW()) {

			//@formatter:off
			getElements().setAll(

					new MoveTo(0, -1),

					new LineTo(N, -1),
					
					new LineTo(N, -5),
					new LineTo(N + 20, 0),
					new LineTo(N, 5),
					
					new LineTo(N, 1),
					new LineTo(0, 1),
					
					new ClosePath()

			);
			//@formatter:on

			relocate(x, y - 5);

		} else {

			//@formatter:off
			getElements().setAll(

					new MoveTo(-1, 0),

					new LineTo(-1, N),
					
					new LineTo(-5, N),
					new LineTo(0, N + 20),
					new LineTo(5, N),
					
					new LineTo(1, N),
					new LineTo(1, 0),
					
					new ClosePath()

			);
			//@formatter:on

			relocate(x - 5, y);
		}

		getTransforms().clear();

		if (isLocalCoords()) {
			double a = _model.getGlobalAngle();

			if (_axis == Axis.CENTER) {
				getTransforms().setAll(new Rotate(a, 5, 5));
			} else {
				getTransforms().setAll(new Rotate(a, 0, 0));
			}
		}

	}

}
