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
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import phasereditor.canvas.core.RectArcadeBodyModel;
import phasereditor.canvas.ui.editors.operations.ChangePropertyOperation;
import phasereditor.canvas.ui.editors.operations.CompositeOperation;
import phasereditor.canvas.ui.shapes.ISpriteNode;

/**
 * @author arian
 *
 */
public class ArcadeRectBodyHandlersGroup extends HandlersGroup {

	private ArcadeRectBodyResizeHandler _resizeArcadeRectBody_TopRight;
	private ArcadeRectBodyResizeHandler _resizeArcadeRectBody_BottomRight;
	private ArcadeRectBodyResizeHandler _resizeArcadeRectBody_BottomLeft;
	private ArcadeBodyMoveHandler _moveArcadeRectBody;
	private Rectangle _resizeArcadeRectBody_area;

	public ArcadeRectBodyHandlersGroup(SelectionNode selnode) {
		super(selnode);

		_resizeArcadeRectBody_TopRight = new ArcadeRectBodyResizeHandler(selnode, true, false);
		_resizeArcadeRectBody_BottomRight = new ArcadeRectBodyResizeHandler(selnode, true, true);
		_resizeArcadeRectBody_BottomLeft = new ArcadeRectBodyResizeHandler(selnode, false, true);
		_moveArcadeRectBody = new ArcadeBodyMoveHandler(selnode);
		_resizeArcadeRectBody_area = new Rectangle();
		_resizeArcadeRectBody_area.setFill(Color.GREENYELLOW);
		_resizeArcadeRectBody_area.setMouseTransparent(true);
		_resizeArcadeRectBody_area.setOpacity(0.5);

		getChildren().setAll(_resizeArcadeRectBody_area, _resizeArcadeRectBody_TopRight,
				_resizeArcadeRectBody_BottomRight, _resizeArcadeRectBody_BottomLeft, _moveArcadeRectBody);
	}

	public void updateHandlers() {
		if (!(_selnode.getObjectNode() instanceof ISpriteNode)) {
			return;
		}

		ISpriteNode sprite = (ISpriteNode) _selnode.getObjectNode();

		if (!(sprite.getModel().getBody() instanceof RectArcadeBodyModel)) {
			return;
		}

		RectArcadeBodyModel body = (RectArcadeBodyModel) sprite.getModel().getBody();

		if (body == null) {
			return;
		}

		double bodyX = body.getOffsetX();
		double bodyY = body.getOffsetY();
		double bodyW = body.getWidth();
		double bodyH = body.getHeight();

		if (bodyW == -1) {
			bodyW = sprite.getControl().getTextureWidth();
		}

		if (bodyH == -1) {
			bodyH = sprite.getControl().getTextureHeight();
		}

		{
			double scale = _selnode.getCanvas().getZoomBehavior().getScale();
			double scaleX = sprite.getModel().getScaleX();
			double scaleY = sprite.getModel().getScaleY();
			bodyX *= scale * scaleX;
			bodyY *= scale * scaleY;
			bodyW *= scale * scaleX;
			bodyH *= scale * scaleY;
		}

		double hs = SelectionNode.HANDLER_SIZE / 2;

		double left = bodyX;
		double right = bodyX + bodyW;
		double top = bodyY;
		double bottom = bodyY + bodyH;

		_resizeArcadeRectBody_TopRight.relocate(right - hs, top - hs);
		_resizeArcadeRectBody_BottomRight.relocate(right - hs, bottom - hs);
		_resizeArcadeRectBody_BottomLeft.relocate(left - hs, bottom - hs);
		_resizeArcadeRectBody_area.relocate(left, top);
		_resizeArcadeRectBody_area.setWidth(bodyW);
		_resizeArcadeRectBody_area.setHeight(bodyH);
		_moveArcadeRectBody.relocate(bodyX - hs, bodyY - hs);
	}

}

class ArcadeRectBodyResizeHandler extends DragHandlerNode {

	private double _initWidth;
	private double _initHeight;

	private boolean _height;
	private boolean _width;

	public ArcadeRectBodyResizeHandler(SelectionNode selnode, boolean width, boolean height) {
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
		ISpriteNode sprite = (ISpriteNode) getObjectNode();
		RectArcadeBodyModel body = (RectArcadeBodyModel) sprite.getModel().getBody();
		_initWidth = body.getWidth();
		_initHeight = body.getHeight();
	}

	@Override
	protected void handleDone() {
		CompositeOperation operations = new CompositeOperation();
		ISpriteNode sprite = (ISpriteNode) getObjectNode();
		RectArcadeBodyModel body = (RectArcadeBodyModel) sprite.getModel().getBody();
		double w = body.getWidth();
		double h = body.getHeight();
		body.setWidth(_initWidth);
		body.setHeight(_initHeight);

		operations.add(new ChangePropertyOperation<Number>(sprite.getModel().getId(), "body.width", Double.valueOf(w)));
		operations
				.add(new ChangePropertyOperation<Number>(sprite.getModel().getId(), "body.height", Double.valueOf(h)));
		getCanvas().getUpdateBehavior().executeOperations(operations);
	}

	@Override
	protected void handleDrag(double dx, double dy) {
		ISpriteNode sprite = (ISpriteNode) getObjectNode();
		RectArcadeBodyModel body = (RectArcadeBodyModel) sprite.getModel().getBody();

		SceneSettings settings = getCanvas().getSettingsModel();

		boolean stepping = settings.isEnableStepping();
		if (_width) {
			double w;
			if (_initWidth == -1) {
				w = sprite.getControl().getTextureWidth();
			} else {
				w = _initWidth;
			}

			w += dx;
			int sw = settings.getStepWidth();

			if (stepping) {
				w = Math.round(w / sw) * sw;
			}

			w = Math.max(w, stepping ? sw : 1);

			body.setWidth(w);
		}

		if (_height) {
			double h;
			if (_initHeight == -1) {
				h = sprite.getControl().getTextureHeight();
			} else {
				h = _initHeight;
			}

			h += dy;
			int sh = settings.getStepHeight();

			if (stepping) {
				h = Math.round(h / sh) * sh;
			}

			h = Math.max(h, stepping ? sh : 1);

			body.setHeight(h);
		}

		sprite.getControl().updateFromModel();
	}
}

