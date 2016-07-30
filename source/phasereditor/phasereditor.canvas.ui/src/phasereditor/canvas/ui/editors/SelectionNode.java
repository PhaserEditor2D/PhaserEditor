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

import java.util.Arrays;
import java.util.List;

import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.shape.StrokeType;
import phasereditor.canvas.core.BaseObjectModel;
import phasereditor.canvas.core.RectArcadeBodyModel;
import phasereditor.canvas.core.TileSpriteModel;
import phasereditor.canvas.ui.editors.operations.ChangePropertyOperation;
import phasereditor.canvas.ui.editors.operations.CompositeOperation;
import phasereditor.canvas.ui.shapes.IObjectNode;
import phasereditor.canvas.ui.shapes.ISpriteNode;
import phasereditor.canvas.ui.shapes.TileSpriteNode;

/**
 * @author arian
 *
 */

public class SelectionNode extends Pane {

	private static Border _border;

	static {
		BorderWidths bw = new BorderWidths(1);
		@SuppressWarnings("boxing")
		List<Double> dashed = Arrays.asList(5d, 2d);
		BorderStrokeStyle style1 = new BorderStrokeStyle(StrokeType.INSIDE, StrokeLineJoin.MITER, StrokeLineCap.BUTT,
				10, 10, dashed);
		BorderStrokeStyle style2 = new BorderStrokeStyle(StrokeType.INSIDE, StrokeLineJoin.MITER, StrokeLineCap.BUTT,
				10, 0, dashed);

		BorderStroke s1 = new BorderStroke(Color.WHITE, style1, null, bw);
		BorderStroke s2 = new BorderStroke(Color.BLACK, style2, null, bw);

		_border = new Border(s1, s2);
	}

	private IObjectNode _objectNode;
	protected Bounds _rect;
	private ObjectCanvas _canvas;
	private Label _label;

	private DragHandlerNode _resizeTile_TopRight;
	private DragHandlerNode _resizeTile_BottomRight;
	private DragHandlerNode _resizeTile_BottomLeft;
	private ArcadeRectBodyResizeHandler _resizeArcadeRectBody_TopRight;
	private ArcadeRectBodyResizeHandler _resizeArcadeRectBody_BottomRight;
	private ArcadeRectBodyResizeHandler _resizeArcadeRectBody_BottomLeft;
	private ArcadeRectBodyMoveHandler _moveArcadeRectBody;
	private Rectangle _resizeArcadeRectBody_area;

	public SelectionNode(ObjectCanvas canvas, IObjectNode inode, Bounds rect) {
		_objectNode = inode;
		_rect = rect;
		_canvas = canvas;

		updateFromZoomAndPanVariables();

		BaseObjectModel model = inode.getModel();

		setBorder(_border);

		Node node = inode.getNode();
		StringBuilder sb = new StringBuilder();
		sb.append(model.getLabel());
		sb.append(" ");
		sb.append((long) node.getLayoutX());
		sb.append(",");
		sb.append((long) node.getLayoutY());

		_label = new Label(sb.toString());
		_label.setId("label " + node.getId());
		_label.setStyle("-fx-background-color:white;-fx-padding:0px 5px 0px 5px;");
		_label.setMinHeight(20);
		_label.setMaxHeight(20);
		_label.relocate(0, -_label.getMinHeight());
		_label.setVisible(false);
		getChildren().add(_label);

		// tile

		// tile handlers

		_resizeTile_TopRight = new TileResizeHandlerNode(this, true, false);
		_resizeTile_BottomRight = new TileResizeHandlerNode(this, true, true);
		_resizeTile_BottomLeft = new TileResizeHandlerNode(this, false, true);

		getChildren().addAll(_resizeTile_TopRight, _resizeTile_BottomRight, _resizeTile_BottomLeft);

		updateTileHandlers();

		// body handlers
		_resizeArcadeRectBody_TopRight = new ArcadeRectBodyResizeHandler(this, true, false);
		_resizeArcadeRectBody_BottomRight = new ArcadeRectBodyResizeHandler(this, true, true);
		_resizeArcadeRectBody_BottomLeft = new ArcadeRectBodyResizeHandler(this, false, true);
		_moveArcadeRectBody = new ArcadeRectBodyMoveHandler(this);
		_resizeArcadeRectBody_area = new Rectangle();
		_resizeArcadeRectBody_area.setFill(Color.GREENYELLOW);
		_resizeArcadeRectBody_area.setMouseTransparent(true);
		_resizeArcadeRectBody_area.setOpacity(0.5);
		_resizeArcadeRectBody_area.setVisible(false);

		getChildren().addAll(_resizeArcadeRectBody_area, _resizeArcadeRectBody_TopRight,
				_resizeArcadeRectBody_BottomRight, _resizeArcadeRectBody_BottomLeft, _moveArcadeRectBody);

		updateArcadeRectBodyHandlers();
	}

	static final int HANDLE_SIZE = 10;

	public void setEnableTileHandlers(boolean enable) {
		hideHandlers();

		_resizeTile_BottomLeft.setVisible(enable);
		_resizeTile_BottomRight.setVisible(enable);
		_resizeTile_TopRight.setVisible(enable);
	}

	public void setEnableArcadeRectHandlers(boolean enable) {
		hideHandlers();

		_resizeArcadeRectBody_TopRight.setVisible(enable);
		_resizeArcadeRectBody_BottomRight.setVisible(enable);
		_resizeArcadeRectBody_BottomLeft.setVisible(enable);
		_resizeArcadeRectBody_area.setVisible(enable);
		_moveArcadeRectBody.setVisible(enable);
	}

	private void hideHandlers() {
		_resizeTile_TopRight.setVisible(false);
		_resizeTile_BottomRight.setVisible(false);
		_resizeTile_BottomLeft.setVisible(false);

		_resizeArcadeRectBody_TopRight.setVisible(false);
		_resizeArcadeRectBody_BottomRight.setVisible(false);
		_resizeArcadeRectBody_BottomLeft.setVisible(false);
		_resizeArcadeRectBody_area.setVisible(false);
		_moveArcadeRectBody.setVisible(false);
	}

	public ObjectCanvas getCanvas() {
		return _canvas;
	}

	public void updateBounds(Bounds rect) {
		_rect = rect;
		updateFromZoomAndPanVariables();
	}

	public void updateFromZoomAndPanVariables() {
		double scale = _canvas.getZoomBehavior().getScale();
		Point2D translate = _canvas.getZoomBehavior().getTranslate();
		double x = translate.getX() + _rect.getMinX() * scale;
		double y = translate.getY() + _rect.getMinY() * scale;

		double h = _rect.getHeight() * scale;
		double w = _rect.getWidth() * scale;

		relocate(x, y);
		setMinSize(w, h);
		setMaxSize(w, h);

		if (_resizeTile_BottomLeft != null) {
			updateTileHandlers();
		}

		if (_resizeArcadeRectBody_BottomLeft != null) {
			updateArcadeRectBodyHandlers();
		}
	}

	private void updateTileHandlers() {
		double w = getMinWidth();
		double h = getMinHeight();

		int hs = HANDLE_SIZE / 2;
		_resizeTile_TopRight.relocate(w - hs, -HANDLE_SIZE / 2);
		_resizeTile_BottomRight.relocate(w - hs, h - hs);
		_resizeTile_BottomLeft.relocate(-HANDLE_SIZE / 2, h - hs);
	}

	private void updateArcadeRectBodyHandlers() {
		if (!(getObjectNode() instanceof ISpriteNode)) {
			return;
		}

		ISpriteNode sprite = (ISpriteNode) getObjectNode();
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
			double scale = _canvas.getZoomBehavior().getScale();
			double scaleX = sprite.getModel().getScaleX();
			double scaleY = sprite.getModel().getScaleY();
			bodyX *= scale * scaleX;
			bodyY *= scale * scaleY;
			bodyW *= scale * scaleX;
			bodyH *= scale * scaleY;
		}

		double hs = HANDLE_SIZE / 2;

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

	public IObjectNode getObjectNode() {
		return _objectNode;
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

class ArcadeRectBodyMoveHandler extends DragHandlerNode {

	protected double _initX;
	protected double _initY;

	public ArcadeRectBodyMoveHandler(SelectionNode selnode) {
		super(selnode);
		setCursor(Cursor.MOVE);
		setFill(Color.ALICEBLUE);
		setArcWidth(3);
		setArcHeight(3);
	}

	@Override
	public void handleMousePressed(MouseEvent e) {
		super.handleMousePressed(e);
		ISpriteNode sprite = (ISpriteNode) getObjectNode();
		RectArcadeBodyModel body = (RectArcadeBodyModel) sprite.getModel().getBody();
		_initX = body.getOffsetX();
		_initY = body.getOffsetY();
	}

	@Override
	protected void handleDone() {
		CompositeOperation operations = new CompositeOperation();
		ISpriteNode sprite = (ISpriteNode) getObjectNode();
		RectArcadeBodyModel body = (RectArcadeBodyModel) sprite.getModel().getBody();
		double x = body.getOffsetX();
		double y = body.getOffsetY();
		body.setOffsetX(_initX);
		body.setOffsetX(_initY);

		operations.add(
				new ChangePropertyOperation<Number>(sprite.getModel().getId(), "body.offset.x", Double.valueOf(x)));
		operations.add(
				new ChangePropertyOperation<Number>(sprite.getModel().getId(), "body.offset.y", Double.valueOf(y)));
		getCanvas().getUpdateBehavior().executeOperations(operations);
	}

	@Override
	protected void handleDrag(double dx, double dy) {
		ISpriteNode sprite = (ISpriteNode) getObjectNode();
		RectArcadeBodyModel body = (RectArcadeBodyModel) sprite.getModel().getBody();

		SceneSettings settings = getCanvas().getSettingsModel();

		boolean stepping = settings.isEnableStepping();
		{
			double x = _initX;
			x += dx;
			int sw = settings.getStepWidth();

			if (stepping) {
				x = Math.round(x / sw) * sw;
			}

			x = Math.max(x, stepping ? sw : 1);

			body.setOffsetX(x);
		}
		{
			double y = _initY;
			y += dy;
			int sh = settings.getStepHeight();

			if (stepping) {
				y = Math.round(y / sh) * sh;
			}

			y = Math.max(y, stepping ? sh : 1);

			body.setOffsetY(y);
		}
		sprite.getControl().updateFromModel();
	}

	@Override
	public void handleMouseExited(MouseEvent e) {
		super.handleMouseExited(e);
	}

	@Override
	public void handleMouseReleased(MouseEvent e) {
		super.handleMouseReleased(e);
	}
}
