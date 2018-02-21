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
package phasereditor.canvas.ui.editors.behaviors;

import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import phasereditor.canvas.core.BaseSpriteModel;
import phasereditor.canvas.ui.editors.edithandlers.PathHandlerNode;
import phasereditor.canvas.ui.shapes.IObjectNode;

/**
 * @author arian
 *
 */
public class MoveAxisHandlerNode extends PathHandlerNode {

	public MoveAxisHandlerNode(IObjectNode object) {
		super(object);
	}

	@Override
	public void updateHandler() {

		double centerX = _model.getPivotX();
		double centerY = _model.getPivotY();

		double width = _control.getTextureWidth();
		double height = _control.getTextureHeight();

		if (_model instanceof BaseSpriteModel) {
			BaseSpriteModel spriteModel = (BaseSpriteModel) _model;

			double x = spriteModel.getAnchorX() * width;
			double y = spriteModel.getAnchorY() * height;

			centerX = centerX + x;
			centerY = centerY + y;
		}

		double x1 = centerX;
		double y1 = centerY - height;

		double x2 = centerX;
		double y2 = centerY;

		double x3 = centerX + width;
		double y3 = centerY;

		Point2D p1 = _node.localToScene(x1, y1);
		Point2D p2 = _node.localToScene(x2, y2);
		Point2D p3 = _node.localToScene(x3, y3);

		getElements().setAll(

				new MoveTo(p2.getX(), p2.getY()),

				new LineTo(p1.getX(), p1.getY()),

				new MoveTo(p2.getX(), p2.getY()),

				new LineTo(p3.getX(), p3.getY()));

		relocate(0, 0);

		setCursor(Cursor.HAND);

	}

}
