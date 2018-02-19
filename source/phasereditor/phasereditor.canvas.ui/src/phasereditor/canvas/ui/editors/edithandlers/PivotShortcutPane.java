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
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Transform;
import phasereditor.canvas.ui.editors.operations.ChangePropertyOperation;
import phasereditor.canvas.ui.editors.operations.CompositeOperation;
import phasereditor.canvas.ui.shapes.IObjectNode;

/**
 * @author arian
 *
 */
public class PivotShortcutPane extends ShortcutPane {

	private Label _xLabel;
	private Label _yLabel;

	public PivotShortcutPane(IObjectNode object) {
		super(object);

		add(_xLabel = createValueLabel(), 0, 0, 3, 1);
		add(_yLabel = createValueLabel(), 0, 1, 3, 1);

		double[] values = { 0, 0.5, 1 };

		int row = 3;
		for (double y : values) {
			int col = 0;
			for (double x : values) {
				add(new PivotBtn(x, y), col++, row);
			}
			row++;
		}
	}

	@Override
	public void updateHandler() {

		_xLabel.setText("x = " + _model.getPivotX());
		_yLabel.setText("y = " + _model.getPivotY());

		for (Object node : getChildren()) {
			if (node instanceof PivotBtn) {
				((PivotBtn) node).updateHandler();
			}
		}

		super.updateHandler();
	}

	private class PivotBtn extends ShortcutButton {

		private double _anchorX;
		private double _anchorY;
		private Circle _rect2;

		public PivotBtn(double anchorX, double anchorY) {
			_anchorX = anchorX;
			_anchorY = anchorY;

			double size = 30;

			setSize(size, size);

			Rectangle rect1 = new Rectangle(size, size);
			rect1.setFill(Color.TRANSPARENT);
			rect1.setStrokeWidth(0);

			_rect2 = new Circle(size / 8);
			_rect2.setStroke(Color.WHITESMOKE);
			_rect2.setStrokeWidth(1);
			_rect2.setEffect(new DropShadow(1, Color.WHITE));

			double x = 0;
			double y = 0;

			switch ((int) (_anchorX * 10)) {
			case 5:
				x = size / 2 - _rect2.getRadius();
				break;
			case 10:
				x = size - _rect2.getRadius();
				break;
			default:
				break;
			}

			switch ((int) (_anchorY * 10)) {
			case 5:
				y = size / 2 - _rect2.getRadius();
				break;
			case 10:
				y = size - _rect2.getRadius();
				break;
			default:
				break;
			}

			_rect2.relocate(x, y);

			setGraphic(new Group(rect1, _rect2));

		}

		public void updateHandler() {
			double width = _control.getTextureWidth();
			double height = _control.getTextureHeight();

			double pivotX = _anchorX * width;
			double pivotY = _anchorY * height;

			_rect2.setFill(_model.getPivotX() == pivotX && _model.getPivotY() == pivotY ? PivotHandlerNode.HANDLER_COLOR
					: Color.BLACK);
		}

		@Override
		protected void doAction() {
			CompositeOperation operations = new CompositeOperation();

			double width = _control.getTextureWidth();
			double height = _control.getTextureHeight();

			double pivotX = _anchorX * width;
			double pivotY = _anchorY * height;

			double dx = pivotX - _model.getPivotX();

			double dy = pivotY - _model.getPivotY();

			Point2D p = new Point2D(dx, dy);
			for (Transform t : _node.getTransforms()) {
				p = t.deltaTransform(p);
			}

			double x = _model.getX() + p.getX();
			double y = _model.getY() + p.getY();

			String id = _model.getId();

			operations.add(new ChangePropertyOperation<Number>(id, "x", Double.valueOf(x)));
			operations.add(new ChangePropertyOperation<Number>(id, "y", Double.valueOf(y)));
			operations.add(new ChangePropertyOperation<Number>(id, "pivot.x", Double.valueOf(pivotX)));
			operations.add(new ChangePropertyOperation<Number>(id, "pivot.y", Double.valueOf(pivotY)));

			_canvas.getUpdateBehavior().executeOperations(operations);
		}
	}

}
