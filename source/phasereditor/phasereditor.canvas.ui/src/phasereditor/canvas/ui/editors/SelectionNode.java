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
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.shape.StrokeType;
import phasereditor.canvas.core.ArcadeBodyModel;
import phasereditor.canvas.core.BaseObjectModel;
import phasereditor.canvas.ui.editors.operations.ChangePropertyOperation;
import phasereditor.canvas.ui.editors.operations.CompositeOperation;
import phasereditor.canvas.ui.shapes.IObjectNode;
import phasereditor.canvas.ui.shapes.ISpriteNode;

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

	private TileHandlerGroup _tileHandlers;
	private ArcadeRectBodyHandlersGroup _arcadeRectHandlers;
	private ArcadeCircleBodyHandlersGroup _arcadeCircleHandlers;

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
		_tileHandlers = new TileHandlerGroup(this);
		getChildren().add(_tileHandlers);
		_tileHandlers.updateHandlers();

		// arcade rect
		_arcadeRectHandlers = new ArcadeRectBodyHandlersGroup(this);
		getChildren().add(_arcadeRectHandlers);
		_arcadeRectHandlers.updateHandlers();

		// arcade circle
		_arcadeCircleHandlers = new ArcadeCircleBodyHandlersGroup(this);
		getChildren().add(_arcadeCircleHandlers);
		_arcadeCircleHandlers.updateHandlers();
	}

	public static final int HANDLER_SIZE = 10;

	public void setEnableTileHandlers(boolean enable) {
		hideHandlers();
		_tileHandlers.setVisible(enable);
	}

	public void setEnableArcadeRectHandlers(boolean enable) {
		hideHandlers();
		_arcadeRectHandlers.setVisible(enable);
	}

	public void setEnableArcadeCircleHandlers(boolean enable) {
		hideHandlers();
		_arcadeCircleHandlers.setVisible(enable);
	}

	private void hideHandlers() {
		getChildren().forEach(n -> n.setVisible(false));
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

		if (_tileHandlers != null) {
			_tileHandlers.updateHandlers();
		}

		if (_arcadeRectHandlers != null) {
			_arcadeRectHandlers.updateHandlers();
		}

		if (_arcadeCircleHandlers != null) {
			_arcadeCircleHandlers.updateHandlers();
		}
	}

	public IObjectNode getObjectNode() {
		return _objectNode;
	}
}

class ArcadeBodyMoveHandler extends DragHandlerNode {

	protected double _initX;
	protected double _initY;

	public ArcadeBodyMoveHandler(SelectionNode selnode) {
		super(selnode);
		setCursor(Cursor.MOVE);
		setFill(Color.ALICEBLUE);
		setArcWidth(10);
		setArcHeight(10);
	}

	@Override
	public void handleMousePressed(MouseEvent e) {
		super.handleMousePressed(e);
		ISpriteNode sprite = (ISpriteNode) getObjectNode();
		ArcadeBodyModel body = (ArcadeBodyModel) sprite.getModel().getBody();
		_initX = body.getOffsetX();
		_initY = body.getOffsetY();
	}

	@Override
	protected void handleDone() {
		CompositeOperation operations = new CompositeOperation();
		ISpriteNode sprite = (ISpriteNode) getObjectNode();
		ArcadeBodyModel body = (ArcadeBodyModel) sprite.getModel().getBody();
		double x = body.getOffsetX();
		double y = body.getOffsetY();
		body.setOffsetX(_initX);
		body.setOffsetX(_initY);

		String id = sprite.getModel().getId();
		
		operations.add(new ChangePropertyOperation<Number>(id, "body.offset.x", Double.valueOf(x)));
		operations.add(new ChangePropertyOperation<Number>(id, "body.offset.y", Double.valueOf(y)));
		
		getCanvas().getUpdateBehavior().executeOperations(operations);
	}

	@Override
	protected void handleDrag(double dx, double dy) {
		ISpriteNode sprite = (ISpriteNode) getObjectNode();
		ArcadeBodyModel body = (ArcadeBodyModel) sprite.getModel().getBody();

		SceneSettings settings = getCanvas().getSettingsModel();

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
