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
package phasereditor.canvas.ui.editors;

import javafx.scene.Cursor;
import javafx.scene.input.MouseEvent;
import phasereditor.canvas.core.TileSpriteModel;
import phasereditor.canvas.ui.editors.operations.ChangePropertyOperation;
import phasereditor.canvas.ui.editors.operations.CompositeOperation;
import phasereditor.canvas.ui.shapes.TileSpriteNode;

/**
 * @author arian
 *
 */
public class TileHandlerGroup extends HandlersGroup {
	private TileResizeHandlerNode _resizeTile_TopRight;
	private TileResizeHandlerNode _resizeTile_BottomRight;
	private TileResizeHandlerNode _resizeTile_BottomLeft;

	public TileHandlerGroup(SelectionNode selnode) {
		super(selnode);
		_resizeTile_TopRight = new TileResizeHandlerNode(selnode, true, false);
		_resizeTile_BottomRight = new TileResizeHandlerNode(selnode, true, true);
		_resizeTile_BottomLeft = new TileResizeHandlerNode(selnode, false, true);
		getChildren().setAll(_resizeTile_TopRight, _resizeTile_BottomRight, _resizeTile_BottomLeft);
	}

	@Override
	public void updateHandlers() {
		double w = _selnode.getMinWidth();
		double h = _selnode.getMinHeight();

		int hs = SelectionNode.HANDLER_SIZE / 2;
		_resizeTile_TopRight.relocate(w - hs, -hs);
		_resizeTile_BottomRight.relocate(w - hs, h - hs);
		_resizeTile_BottomLeft.relocate(-hs, h - hs);
	}
}

class TileResizeHandlerNode extends DragHandlerNode {

	protected double _initWidth;
	protected double _initHeight;
	private boolean _height;
	private boolean _width;

	public TileResizeHandlerNode(SelectionNode selnode, boolean width, boolean height) {
		super(selnode);
		
		_width = width;
		_height = height;

		if (_width && _height) {
			setCursor(Cursor.SE_RESIZE);
		} else if (_width) {
			setCursor(Cursor.E_RESIZE);
		} else {
			setCursor(Cursor.S_RESIZE);
		}
	}

	@Override
	public void handleMousePressed(MouseEvent e) {
		super.handleMousePressed(e);
		TileSpriteNode tile = (TileSpriteNode) getObjectNode();
		TileSpriteModel tilemodel = tile.getModel();
		_initWidth = tilemodel.getWidth();
		_initHeight = tilemodel.getHeight();
	}

	@Override
	protected void handleDone() {
		CompositeOperation operations = new CompositeOperation();
		TileSpriteNode tile = (TileSpriteNode) getObjectNode();
		TileSpriteModel tilemodel = tile.getModel();
		double w = tilemodel.getWidth();
		double h = tilemodel.getHeight();
		tilemodel.setWidth(_initWidth);
		tilemodel.setHeight(_initHeight);
		operations.add(new ChangePropertyOperation<Number>(tilemodel.getId(), "width", Double.valueOf(w)));
		operations.add(new ChangePropertyOperation<Number>(tilemodel.getId(), "height", Double.valueOf(h)));
		getCanvas().getUpdateBehavior().executeOperations(operations);
	}

	@Override
	protected void handleDrag(double dx, double dy) {
		TileSpriteNode tile = (TileSpriteNode) getObjectNode();
		TileSpriteModel tilemodel = tile.getModel();

		SceneSettings settings = getCanvas().getSettingsModel();

		boolean stepping = settings.isEnableStepping();
		if (_width) {
			double w = _initWidth + dx;
			int sw = settings.getStepWidth();

			if (stepping) {
				w = Math.round(w / sw) * sw;
			}

			w = Math.max(w, stepping ? sw : 1);

			tilemodel.setWidth(w);
		}

		if (_height) {
			double h = _initHeight + dy;
			int sh = settings.getStepHeight();

			if (stepping) {
				h = Math.round(h / sh) * sh;
			}

			h = Math.max(h, stepping ? sh : 1);

			tilemodel.setHeight(h);
		}

		tile.updateFromModel();
	}
}
