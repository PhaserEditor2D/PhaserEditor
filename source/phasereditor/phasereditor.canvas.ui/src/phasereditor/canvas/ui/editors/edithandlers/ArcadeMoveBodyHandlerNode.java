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
import phasereditor.canvas.core.ArcadeBodyModel;
import phasereditor.canvas.core.BaseSpriteModel;
import phasereditor.canvas.core.CircleArcadeBodyModel;
import phasereditor.canvas.core.RectArcadeBodyModel;
import phasereditor.canvas.core.EditorSettings;
import phasereditor.canvas.ui.editors.operations.ChangePropertyOperation;
import phasereditor.canvas.ui.editors.operations.CompositeOperation;
import phasereditor.canvas.ui.shapes.IObjectNode;
import phasereditor.canvas.ui.shapes.ISpriteNode;

/**
 * @author arian
 *
 */
public class ArcadeMoveBodyHandlerNode extends CircleHandlerNode {
	private double _initX;
	private double _initY;

	public ArcadeMoveBodyHandlerNode(IObjectNode object) {
		super(object);
		setFill(Color.ALICEBLUE);
		setRadius(5);
		setCursor(Cursor.MOVE);
	}
	
	@Override
	public boolean isValid() {
		return isArcadeValid();
	}

	@Override
	public void handleLocalStart(double localX, double localY) {
		ISpriteNode sprite = (ISpriteNode) _object;
		ArcadeBodyModel body = (ArcadeBodyModel) sprite.getModel().getBody();
		_initX = body.getOffsetX();
		_initY = body.getOffsetY();
	}

	@Override
	public void handleLocalDrag(double dx, double dy) {
		ISpriteNode sprite = (ISpriteNode) _object;
		ArcadeBodyModel body = (ArcadeBodyModel) sprite.getModel().getBody();

		EditorSettings settings = _canvas.getSettingsModel();

		boolean stepping = settings.isEnableStepping();
		{
			double x = _initX;
			x += dx;
			int sw = settings.getStepWidth();

			if (stepping) {
				x = Math.round(x / sw) * sw;
			}

			body.setOffsetX(x);
		}

		{
			double y = _initY;
			y += dy;
			int sh = settings.getStepHeight();

			if (stepping) {
				y = Math.round(y / sh) * sh;
			}

			body.setOffsetY(y);
		}
	}

	@Override
	public void handleDone() {
		CompositeOperation operations = new CompositeOperation();
		ISpriteNode sprite = (ISpriteNode) _object;

		ArcadeBodyModel body = (ArcadeBodyModel) sprite.getModel().getBody();

		double x = body.getOffsetX();
		double y = body.getOffsetY();

		body.setOffsetX(_initX);
		body.setOffsetX(_initY);

		String id = sprite.getModel().getId();

		operations.add(new ChangePropertyOperation<Number>(id, "body.offset.x", Double.valueOf(x)));
		operations.add(new ChangePropertyOperation<Number>(id, "body.offset.y", Double.valueOf(y)));

		_canvas.getUpdateBehavior().executeOperations(operations);
	}

	@Override
	public void updateHandler() {
		ArcadeBodyModel body = (ArcadeBodyModel) ((BaseSpriteModel) _model).getBody();

		double w;
		double h;

		if (body instanceof RectArcadeBodyModel) {
			RectArcadeBodyModel rect = (RectArcadeBodyModel) body;
			w = rect.getWidth();
			h = rect.getHeight();

			if (w == -1) {
				w = _control.getTextureWidth();
			}

			if (h == -1) {
				h = _control.getTextureHeight();
			}
		} else {
			double r = ((CircleArcadeBodyModel) body).getRadius();
			w = r * 2;
			h = r * 2;
		}

		double x = body.getOffsetX() + 0.5 * w;
		double y = body.getOffsetY() + 0.5 * h;

		Point2D p = objectToScene(x, y);

		setCenterX(p.getX());
		setCenterY(p.getY());
	}
}
