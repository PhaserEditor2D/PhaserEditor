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
import phasereditor.canvas.core.CanvasMainSettings;
import phasereditor.canvas.core.TileSpriteModel;
import phasereditor.canvas.ui.editors.operations.ChangePropertyOperation;
import phasereditor.canvas.ui.editors.operations.CompositeOperation;
import phasereditor.canvas.ui.shapes.IObjectNode;
import phasereditor.canvas.ui.shapes.TileSpriteNode;

/**
 * @author arian
 *
 */
public class TileHandlerNode extends PathHandlerNode {

	protected double _initWidth;
	protected double _initHeight;
	private Axis _axis;
	private double _initX;
	private double _initY;

	public TileHandlerNode(IObjectNode object, Axis axis) {
		super(object);
		_axis = axis;
		setFill(Color.DARKSEAGREEN);
	}
	
	@Override
	public boolean isValid() {
		return _object instanceof TileSpriteNode;
	}

	@Override
	public void handleLocalStart(double localX, double localY) {
		TileSpriteNode tile = (TileSpriteNode) _object;
		TileSpriteModel tilemodel = tile.getModel();

		_initX = _model.getX();
		_initY = _model.getY();
		_initWidth = tilemodel.getWidth();
		_initHeight = tilemodel.getHeight();
	}

	@Override
	public void handleLocalDrag(double dx, double dy) {
		TileSpriteNode tile = (TileSpriteNode) _object;
		TileSpriteModel tilemodel = tile.getModel();

		CanvasMainSettings settings = _canvas.getSettingsModel();

		boolean stepping = settings.isEnableStepping();

		if (_axis.x == 0) {
			double x = _initX + dx;
			int sw = settings.getStepWidth();

			if (stepping) {
				x = Math.round(x / sw) * sw;
			}

			_model.setX(x);
		}

		if (_axis.changeW()) {
			double dx2 = dx;

			if (_axis.x == 0) {
				dx2 = -dx;
			}

			double w = _initWidth + dx2;

			int sw = settings.getStepWidth();

			if (stepping) {
				w = Math.round(w / sw) * sw;
			}

			w = Math.max(w, stepping ? sw : 1);

			tilemodel.setWidth(w);
		}

		if (_axis.y == 0) {
			double y = _initY + dy;
			int sh = settings.getStepHeight();

			if (stepping) {
				y = Math.round(y / sh) * sh;
			}

			_model.setY(y);
		}

		if (_axis.changeH()) {
			double dy2 = dy;

			if (_axis.y == 0) {
				dy2 = -dy;
			}

			double h = _initHeight + dy2;
			int sh = settings.getStepHeight();

			if (stepping) {
				h = Math.round(h / sh) * sh;
			}

			h = Math.max(h, stepping ? sh : 1);

			tilemodel.setHeight(h);
		}
	}

	@Override
	public void handleDone() {
		CompositeOperation operations = new CompositeOperation();
		TileSpriteNode tile = (TileSpriteNode) _object;
		TileSpriteModel model = tile.getModel();
		double x = model.getX();
		double y = model.getY();
		double w = model.getWidth();
		double h = model.getHeight();
		model.setX(_initX);
		model.setY(_initY);
		model.setWidth(_initWidth);
		model.setHeight(_initHeight);
		operations.add(new ChangePropertyOperation<Number>(model.getId(), "x", Double.valueOf(x)));
		operations.add(new ChangePropertyOperation<Number>(model.getId(), "y", Double.valueOf(y)));
		operations.add(new ChangePropertyOperation<Number>(model.getId(), "width", Double.valueOf(w)));
		operations.add(new ChangePropertyOperation<Number>(model.getId(), "height", Double.valueOf(h)));
		_canvas.getUpdateBehavior().executeOperations(operations);
	}

	@Override
	public void updateHandler() {
		double x = _axis.x * _control.getTextureWidth();
		double y = _axis.y * _control.getTextureHeight();

		Point2D p = objectToScene(x, y);
		relocate(p.getX() - 5, p.getY() - 5);

		setCursor(_axis.getResizeCursor(_object));
	}

}
