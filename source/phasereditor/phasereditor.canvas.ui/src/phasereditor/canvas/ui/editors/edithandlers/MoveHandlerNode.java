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
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
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

	public MoveHandlerNode(Axis axis, IObjectNode object) {
		super(object);

		setFill(Color.WHITESMOKE);

		_axis = axis;

		if (_axis == Axis.TOP) {
			setRotate(-90);
		}

	}

	@Override
	protected void createElements() {
		getElements().setAll(

				new MoveTo(0, 0),

				new LineTo(16, 8),

				new LineTo(0, 16),

				new LineTo(0, 0)

		);
	}

	@Override
	public void handleLocalStart(double x, double y, MouseEvent e) {
		_initX = _model.getX();
		_initY = _model.getY();
	}

	@Override
	public void handleLocalDrag(double dx, double dy, MouseEvent e) {
		Point2D p = _canvas.getDragBehavior().adjustPositionToStep(_initX + dx, _initY + dy);

		if (_axis.changeW()) {
			_model.setX(p.getX());
		} else {
			_model.setY(p.getY());
		}
	}

	@Override
	public void handleDone() {

		double x = _model.getX();
		double y = _model.getY();

		_model.setX(_initX);
		_model.setY(_initY);

		String id = _model.getId();

		_canvas.getUpdateBehavior().executeOperations(

				new CompositeOperation(

						new ChangePropertyOperation<Number>(id, "x", Double.valueOf(x)),

						new ChangePropertyOperation<Number>(id, "y", Double.valueOf(y))

				));
	}

	@Override
	public void updateHandler() {
		double x = _axis.x * _control.getTextureWidth();
		double y = _axis.y * _control.getTextureHeight();

		Point2D p = objectToScene(x, y);

		if (_axis == Axis.TOP) {
			relocate(p.getX() - 8, p.getY() - 16 - 5);
		} else {
			relocate(p.getX() + 5, p.getY() - 8);
		}

		setCursor(Cursor.HAND);
	}

}
