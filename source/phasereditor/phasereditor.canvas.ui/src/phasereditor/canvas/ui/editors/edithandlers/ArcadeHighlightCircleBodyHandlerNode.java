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
import phasereditor.canvas.core.BaseSpriteModel;
import phasereditor.canvas.core.CircleArcadeBodyModel;
import phasereditor.canvas.ui.shapes.IObjectNode;

/**
 * @author arian
 *
 */
public class ArcadeHighlightCircleBodyHandlerNode extends CircleHandlerNode {

	public ArcadeHighlightCircleBodyHandlerNode(IObjectNode object) {
		super(object);
		setOpacity(0.5);
		setFill(Color.GREENYELLOW);
		setStrokeWidth(0);
	}

	@Override
	public void updateHandler() {
		CircleArcadeBodyModel body = (CircleArcadeBodyModel) ((BaseSpriteModel) _model).getBody();

		double r = body.getRadius();

		double x = body.getOffsetX();
		double y = body.getOffsetY();

		Point2D p1 = objectToScene(x + r, y + r);
		Point2D p2 = objectToScene(x + r * 2, y + r);

		double r2 = p1.distance(p2);

		setCenterX(p1.getX());
		setCenterY(p1.getY());
		setRadius(r2);
	}
}
