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
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import phasereditor.canvas.core.BaseSpriteModel;
import phasereditor.canvas.core.RectArcadeBodyModel;
import phasereditor.canvas.ui.shapes.IObjectNode;

/**
 * @author arian
 *
 */
public class ArcadeHighlightRectBodyHandlerNode extends PathHandlerNode implements IArcadeBodyHandlerNode{

	public ArcadeHighlightRectBodyHandlerNode(IObjectNode object) {
		super(object);
		setFill(Color.GREENYELLOW);
		setOpacity(0.5);
		setMouseTransparent(true);
		setStrokeWidth(0);
	}
	
	@Override
	public boolean isValid() {
		return isRectArcadeValid();
	}

	@Override
	public void handleLocalStart(double localX, double localY, MouseEvent e) {
		// nothing
	}

	@Override
	public void handleLocalDrag(double dx, double dy, MouseEvent e) {
		// nothing
	}

	@Override
	public void handleDone() {
		// nothing
	}

	@Override
	public void updateHandler() {
		RectArcadeBodyModel body = (RectArcadeBodyModel) ((BaseSpriteModel) _model).getBody();

		double w = body.getWidth();
		double h = body.getHeight();

		if (w == -1) {
			w = _control.getTextureWidth();
		}

		if (h == -1) {
			h = _control.getTextureHeight();
		}

		double x = body.getOffsetX();
		double y = body.getOffsetY();

		Point2D p1 = objectToScene(x, y);
		Point2D p2 = objectToScene(x + w, y);
		Point2D p3 = objectToScene(x + w, y + h);
		Point2D p4 = objectToScene(x, y + h);

		x = p1.getX();
		y = p1.getY();
		w = p2.getX() - p1.getX();
		h = p2.getY() - p1.getY();

		// relocate(x, y);

		getElements().setAll(

		new MoveTo(p1.getX(), p1.getY()),

		new LineTo(p2.getX(), p2.getY()),

		new LineTo(p3.getX(), p3.getY()),

		new LineTo(p4.getX(), p4.getY()),

		new LineTo(p1.getX(), p1.getY())

		);
	}

}
