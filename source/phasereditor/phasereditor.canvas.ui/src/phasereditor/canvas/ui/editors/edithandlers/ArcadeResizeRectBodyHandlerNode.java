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
import phasereditor.canvas.core.RectArcadeBodyModel;
import phasereditor.canvas.core.SceneSettings;
import phasereditor.canvas.ui.editors.operations.ChangePropertyOperation;
import phasereditor.canvas.ui.editors.operations.CompositeOperation;
import phasereditor.canvas.ui.shapes.IObjectNode;
import phasereditor.canvas.ui.shapes.ISpriteNode;

/**
 * @author arian
 *
 */
public class ArcadeResizeRectBodyHandlerNode extends PathHandlerNode {

	private Axis _axis;
	private double _initX;
	private double _initY;
	private double _initWidth;
	private double _initHeight;

	public ArcadeResizeRectBodyHandlerNode(IObjectNode object, Axis axis) {
		super(object);
		_axis = axis;
		setFill(Color.GREENYELLOW);
	}

	@Override
	public boolean isValid() {
		return super.isRectArcadeValid();
	}

	@Override
	public void handleLocalStart(double localX, double localY) {
		ISpriteNode sprite = (ISpriteNode) _object;
		RectArcadeBodyModel body = (RectArcadeBodyModel) sprite.getModel().getBody();
		_initX = body.getOffsetX();
		_initY = body.getOffsetY();
		_initWidth = body.getWidth();
		_initHeight = body.getHeight();
	}

	@Override
	public void handleLocalDrag(double dx, double dy) {
		ISpriteNode sprite = (ISpriteNode) _object;
		RectArcadeBodyModel body = (RectArcadeBodyModel) sprite.getModel().getBody();

		SceneSettings settings = _canvas.getSettingsModel();

		boolean stepping = settings.isEnableStepping();
		double ax = _axis.x;
		double ay = _axis.y;

		{
			double x = _initX;
			x += dx * (ax == 0 ? 1 : 0);
			int sw = settings.getStepWidth();

			if (stepping) {
				x = Math.round(x / sw) * sw;
			}

			body.setOffsetX(x);
		}

		{
			double y = _initY;
			y += dy * (ay == 0 ? 1 : 0);
			int sh = settings.getStepHeight();

			if (stepping) {
				y = Math.round(y / sh) * sh;
			}

			body.setOffsetY(y);
		}

		{
			double w;
			if (_initWidth == -1) {
				w = sprite.getControl().getTextureWidth();
			} else {
				w = _initWidth;
			}

			w += dx * (ax == 0 ? -1 : (ax == 1 ? 1 : 0));
			int sw = settings.getStepWidth();

			if (stepping) {
				w = Math.round(w / sw) * sw;
			}

			body.setWidth(w);
		}

		{
			double h;
			if (_initHeight == -1) {
				h = sprite.getControl().getTextureHeight();
			} else {
				h = _initHeight;
			}

			h += dy * (ay == 0 ? -1 : (ay == 1 ? 1 : 0));
			int sh = settings.getStepHeight();

			if (stepping) {
				h = Math.round(h / sh) * sh;
			}

			body.setHeight(h);
		}
	}

	@Override
	public void handleDone() {
		CompositeOperation operations = new CompositeOperation();
		ISpriteNode sprite = (ISpriteNode) _object;
		RectArcadeBodyModel body = (RectArcadeBodyModel) sprite.getModel().getBody();
		double x = body.getOffsetX();
		double y = body.getOffsetY();
		double w = body.getWidth();
		double h = body.getHeight();
		body.setOffsetX(_initX);
		body.setOffsetX(_initY);
		body.setWidth(_initWidth);
		body.setHeight(_initHeight);

		String id = sprite.getModel().getId();

		operations.add(new ChangePropertyOperation<Number>(id, "body.offset.x", Double.valueOf(x)));
		operations.add(new ChangePropertyOperation<Number>(id, "body.offset.y", Double.valueOf(y)));
		operations.add(new ChangePropertyOperation<Number>(id, "body.width", Double.valueOf(w)));
		operations.add(new ChangePropertyOperation<Number>(id, "body.height", Double.valueOf(h)));

		_canvas.getUpdateBehavior().executeOperations(operations);
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

		double x = body.getOffsetX() + _axis.x * w;
		double y = body.getOffsetY() + _axis.y * h;

		Point2D p = objectToScene(x, y);
		relocate(p.getX() - 5, p.getY() - 5);

		setCursor(_axis.getResizeCursor(_object));
	}

}
