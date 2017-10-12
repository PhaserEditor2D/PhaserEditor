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

import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.paint.Color;
import javafx.scene.transform.Transform;
import phasereditor.canvas.ui.editors.operations.ChangePropertyOperation;
import phasereditor.canvas.ui.editors.operations.CompositeOperation;
import phasereditor.canvas.ui.shapes.IObjectNode;

/**
 * @author arian
 *
 */
public class PivotHandlerNode extends PathHandlerNode {

	private double _initPivotX;
	private double _initPivotY;
	private double _initX;
	private double _initY;

	public PivotHandlerNode(IObjectNode object) {
		super(object);
		setFill(Color.ORANGE);
		setCursor(Cursor.MOVE);
	}

	@Override
	public boolean isValid() {
		return true;
	}

	@Override
	public void handleLocalStart(double localX, double localY) {
		_initX = _model.getX();
		_initY = _model.getY();
		_initPivotX = _model.getPivotX();
		_initPivotY = _model.getPivotY();
	}

	@Override
	public void handleLocalDrag(double dx, double dy) {
		_model.setPivotX(_initPivotX + dx);
		_model.setPivotY(_initPivotY + dy);

		Point2D p = new Point2D(dx, dy);
		for (Transform t : _node.getTransforms()) {
			p = t.deltaTransform(p);
		}

		_model.setX(_initX + p.getX());
		_model.setY(_initY + p.getY());

	}

	@Override
	public void handleDone() {
		CompositeOperation operations = new CompositeOperation();

		double x = _model.getX();
		double y = _model.getY();
		double pivotX = _model.getPivotX();
		double pivotY = _model.getPivotY();

		_model.setX(_initX);
		_model.setX(_initY);
		_model.setPivotX(_initPivotX);
		_model.setPivotY(_initPivotY);

		String id = _model.getId();

		operations.add(new ChangePropertyOperation<Number>(id, "x", Double.valueOf(x)));
		operations.add(new ChangePropertyOperation<Number>(id, "y", Double.valueOf(y)));
		operations.add(new ChangePropertyOperation<Number>(id, "pivot.x", Double.valueOf(pivotX)));
		operations.add(new ChangePropertyOperation<Number>(id, "pivot.y", Double.valueOf(pivotY)));

		_canvas.getUpdateBehavior().executeOperations(operations);
	}

	@Override
	public void updateHandler() {

		double x = _model.getPivotX();
		double y = _model.getPivotY();

		Point2D p = objectToScene(x, y);
		relocate(p.getX() - 5, p.getY() - 5);
	}

}
