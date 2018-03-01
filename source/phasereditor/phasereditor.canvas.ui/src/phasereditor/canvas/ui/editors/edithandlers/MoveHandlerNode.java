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
import javafx.scene.shape.ArcTo;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.transform.Rotate;
import phasereditor.canvas.core.BaseObjectModel;
import phasereditor.canvas.core.BaseSpriteModel;
import phasereditor.canvas.ui.editors.operations.ChangePropertyOperation;
import phasereditor.canvas.ui.editors.operations.CompositeOperation;
import phasereditor.canvas.ui.shapes.IObjectNode;

/**
 * @author arian
 *
 */
public class MoveHandlerNode extends PathHandlerNode {

	public static final String HANDLES_MOVE_LOCAL_ATTR = "handles.move.local";

	private Axis _axis;
	private double _initX;
	private double _initY;
	private double _initModelX;
	private double _initModelY;

	public MoveHandlerNode(Axis axis, IObjectNode object) {
		super(object);

		setStroke(Color.BLACK);

		_axis = axis;

		updateHandler();

	}

	public boolean isLocal() {
		return _canvas.getHandlerBehavior().getData().containsKey(HANDLES_MOVE_LOCAL_ATTR);
	}

	@Override
	public void handleLocalStart(double x, double y, MouseEvent e) {
		if (!isLocal()) {
			return;
		}

		_initModelX = _model.getX();
		_initModelY = _model.getY();

		_initX = _initModelX;
		_initY = _initModelY;
	}

	@Override
	public void handleLocalDrag(double dx, double dy, MouseEvent e) {
		if (!isLocal()) {
			return;
		}

		boolean changeBoth = _axis == Axis.CENTER;

		Point2D p = _canvas.getDragBehavior().adjustPositionToStep(_initX + dx, _initY + dy);

		if (_axis.changeW() || changeBoth) {
			_model.setX(p.getX());
		}

		if (_axis.changeH() || changeBoth) {
			_model.setY(p.getY());
		}

	}

	@Override
	public void handleSceneStart(double x, double y, MouseEvent e) {
		if (isLocal()) {
			return;
		}

		_initModelX = _model.getX();
		_initModelY = _model.getY();

		Point2D p = _object.getNode().getParent().localToScene(_initModelX, _initModelY);

		_initX = p.getX();
		_initY = p.getY();
	}

	@Override
	public void handleSceneDrag(double dx, double dy, MouseEvent e) {
		if (isLocal()) {
			return;
		}

		Point2D p = null;

		if (_axis == Axis.CENTER) {
			p = _canvas.getDragBehavior().adjustPositionToStep(_initX + dx, _initY + dy);
		} else {
			if (_axis.changeW()) {
				p = _canvas.getDragBehavior().adjustPositionToStep(_initX + dx, _initY);
			}

			if (_axis.changeH()) {
				p = _canvas.getDragBehavior().adjustPositionToStep(_initX, _initY + dy);
			}
		}

		Node node = _object.getNode();
		Parent parent = node.getParent();
		p = parent.sceneToLocal(p);

		_model.setX(p.getX());
		_model.setY(p.getY());

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

		int N = 150;

		if (_axis == Axis.CENTER) {

			relocate(x - 5, y - 5);

			ArcTo arcTo1 = new ArcTo();
			arcTo1.setX(10);
			arcTo1.setY(0);
			arcTo1.setRadiusX(5);
			arcTo1.setRadiusY(5);

			ArcTo arcTo2 = new ArcTo();
			arcTo2.setX(0);
			arcTo2.setY(0);
			arcTo2.setRadiusX(5);
			arcTo2.setRadiusY(5);

			getElements().setAll(new MoveTo(0, 0), arcTo1, arcTo2);

		} else if (_axis.changeW()) {

			//@formatter:off
			getElements().setAll(

					new MoveTo(0, -1),

					new LineTo(N, -1),
					
					new LineTo(N, -5),
					new LineTo(N + 10, 0),
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
					new LineTo(0, N + 10),
					new LineTo(5, N),
					
					new LineTo(1, N),
					new LineTo(1, 0),
					
					new ClosePath()

			);
			//@formatter:on

			relocate(x - 5, y);
		}

		getTransforms().clear();

		if (isLocal()) {
			double a = 0;
			BaseObjectModel model = _model.getParent();
			while (model != _canvas.getWorldModel()) {
				a += model.getAngle();
				model = model.getParent();
			}

			if (_axis == Axis.CENTER) {
				getTransforms().setAll(new Rotate(a, 5, 0));
			} else {
				getTransforms().setAll(new Rotate(a, 0, 0));
			}
		}

		// setCursor(_axis.getResizeCursor(_object));
		setCursor(Cursor.MOVE);

		Paint color = _axis == Axis.CENTER ? (isLocal() ? Color.LIGHTBLUE : Color.WHITE)
				: (_axis.changeW() ? Color.RED.brighter() : Color.LIGHTGREEN);

		setFill(color);
	}

}
