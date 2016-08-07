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
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcTo;
import javafx.scene.shape.MoveTo;
import phasereditor.canvas.core.BaseSpriteModel;
import phasereditor.canvas.core.CircleArcadeBodyModel;
import phasereditor.canvas.ui.shapes.IObjectNode;

/**
 * @author arian
 *
 */
public class ArcadeHighlightCircleBodyHandlerNode extends PathHandlerNode {

	public ArcadeHighlightCircleBodyHandlerNode(IObjectNode object) {
		super(object);
		setOpacity(0.5);
		setFill(Color.GREENYELLOW);
		setStrokeWidth(0);
	}

	@Override
	protected void handleDrag(double dx, double dy) {
		// nothing
	}

	@Override
	protected void handleDone() {
		// nothing
	}

	@Override
	public void updateHandler() {
		CircleArcadeBodyModel body = (CircleArcadeBodyModel) ((BaseSpriteModel) _model).getBody();

		double r = body.getRadius();

		double x = body.getOffsetX();
		double y = body.getOffsetY();

		Point2D p1 = objectToScene(_object, x + r, y);
		Point2D p2 = objectToScene(_object, x + r * 2, y + r);
		Point2D p3 = objectToScene(_object, x + r, y + r * 2);
		Point2D p4 = objectToScene(_object, x, y + r);

		double d2 = p2.distance(p4) / 2;

		getElements().setAll(

		new MoveTo(p1.getX(), p1.getY()),

		new ArcTo(d2, d2, 0, p4.getX(), p4.getY(), false, false),

		new ArcTo(d2, d2, 0, p3.getX(), p3.getY(), false, false),

		new ArcTo(d2, d2, 0, p2.getX(), p2.getY(), false, false),

		new ArcTo(d2, d2, 0, p1.getX(), p1.getY(), false, false)

		);

	}

}
