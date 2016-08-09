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
import phasereditor.canvas.core.TileSpriteModel;
import phasereditor.canvas.ui.editors.SceneSettings;
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

	public TileHandlerNode(IObjectNode object, Axis axis) {
		super(object);
		_axis = axis;
		setFill(Color.DARKSEAGREEN);
	}

	@Override
	public void handleLocalStart(double localX, double localY) {
		TileSpriteNode tile = (TileSpriteNode) _object;
		TileSpriteModel tilemodel = tile.getModel();
		_initWidth = tilemodel.getWidth();
		_initHeight = tilemodel.getHeight();
	}

	@Override
	public void handleLocalDrag(double dx, double dy) {
		TileSpriteNode tile = (TileSpriteNode) _object;
		TileSpriteModel tilemodel = tile.getModel();

		SceneSettings settings = _canvas.getSettingsModel();

		boolean stepping = settings.isEnableStepping();

		if (_axis.changeW()) {
			double w = _initWidth + dx;
			int sw = settings.getStepWidth();

			if (stepping) {
				w = Math.round(w / sw) * sw;
			}

			w = Math.max(w, stepping ? sw : 1);

			tilemodel.setWidth(w);
		}

		if (_axis.changeH()) {
			double h = _initHeight + dy;
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
		TileSpriteModel tilemodel = tile.getModel();
		double w = tilemodel.getWidth();
		double h = tilemodel.getHeight();
		tilemodel.setWidth(_initWidth);
		tilemodel.setHeight(_initHeight);
		operations.add(new ChangePropertyOperation<Number>(tilemodel.getId(), "width", Double.valueOf(w)));
		operations.add(new ChangePropertyOperation<Number>(tilemodel.getId(), "height", Double.valueOf(h)));
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
