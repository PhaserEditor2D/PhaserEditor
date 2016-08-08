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
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import phasereditor.canvas.core.BaseSpriteModel;
import phasereditor.canvas.core.CircleArcadeBodyModel;
import phasereditor.canvas.ui.editors.operations.ChangePropertyOperation;
import phasereditor.canvas.ui.editors.operations.CompositeOperation;
import phasereditor.canvas.ui.shapes.IObjectNode;
import phasereditor.canvas.ui.shapes.ISpriteNode;

/**
 * @author arian
 *
 */
public class ArcadeResizeCircleBodyHandlerNode extends PathHandlerNode {

	private double _initRadius;
	private double _initOffsetX;
	private double _initOffsetY;

	public ArcadeResizeCircleBodyHandlerNode(IObjectNode object) {
		super(object);
		setCursor(Cursor.H_RESIZE);
		setFill(Color.GREENYELLOW);
	}

	@Override
	public void handleMousePressed(MouseEvent e) {
		super.handleMousePressed(e);
		ISpriteNode sprite = (ISpriteNode) _object;
		CircleArcadeBodyModel body = (CircleArcadeBodyModel) sprite.getModel().getBody();
		_initRadius = body.getRadius();
		_initOffsetX = body.getOffsetX();
		_initOffsetY = body.getOffsetY();
	}

	@Override
	public void handleDrag(double dx, double dy) {
		ISpriteNode sprite = (ISpriteNode) _object;
		CircleArcadeBodyModel body = (CircleArcadeBodyModel) sprite.getModel().getBody();

		body.setRadius(_initRadius + dx);
		body.setOffsetX(_initOffsetX - dx);
		body.setOffsetY(_initOffsetY - dx);
	}

	@Override
	public void handleDone() {
		CompositeOperation operations = new CompositeOperation();
		ISpriteNode sprite = (ISpriteNode) _object;
		CircleArcadeBodyModel body = (CircleArcadeBodyModel) sprite.getModel().getBody();

		double r = body.getRadius();
		double x = body.getOffsetX();
		double y = body.getOffsetY();

		body.setRadius(_initRadius);
		body.setOffsetX(_initOffsetX);
		body.setOffsetY(_initOffsetY);

		String id = sprite.getModel().getId();
		operations.add(new ChangePropertyOperation<Number>(id, "body.radius", Double.valueOf(r)));
		operations.add(new ChangePropertyOperation<Number>(id, "body.offset.x", Double.valueOf(x)));
		operations.add(new ChangePropertyOperation<Number>(id, "body.offset.y", Double.valueOf(y)));

		_canvas.getUpdateBehavior().executeOperations(operations);
	}

	@Override
	public void updateHandler() {
		CircleArcadeBodyModel body = (CircleArcadeBodyModel) ((BaseSpriteModel) _model).getBody();

		double r = body.getRadius();

		double x = body.getOffsetX() + r * 2;
		double y = body.getOffsetY() + r;

		Point2D p = objectToScene(_object, x, y);
		relocate(p.getX() - 5, p.getY() - 5);
	}

}
