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
import phasereditor.canvas.core.BaseObjectModel;
import phasereditor.canvas.core.TileSpriteModel;
import phasereditor.canvas.ui.editors.operations.ChangePropertyOperation;
import phasereditor.canvas.ui.editors.operations.CompositeOperation;
import phasereditor.canvas.ui.shapes.IObjectNode;
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
	
	private DragHandlerNode _resizeTopRightHandle;
	private DragHandlerNode _resizeBottomRightHandle;
	private DragHandlerNode _resizeBottomLeftHandle;
	
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

		class TileResizeHandler extends DragHandlerNode {

			protected double _initWidth;
			protected double _initHeight;
			private boolean _height;
			private boolean _width;

			public TileResizeHandler(SelectionNode selnode, boolean width, boolean height) {
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

			@SuppressWarnings("synthetic-access")
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
				_canvas.getUpdateBehavior().executeOperations(operations);
			}

			@SuppressWarnings("synthetic-access")
			@Override
			protected void handleDrag(double dx, double dy) {
				TileSpriteNode tile = (TileSpriteNode) getObjectNode();
				TileSpriteModel tilemodel = tile.getModel();

				SceneSettings settings = _canvas.getSettingsModel();

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

		_resizeTopRightHandle = new TileResizeHandler(this, true, false);
		_resizeBottomRightHandle = new TileResizeHandler(this, true, true);
		_resizeBottomLeftHandle = new TileResizeHandler(this, false, true);

		getChildren().addAll(_resizeTopRightHandle, _resizeBottomRightHandle, _resizeBottomLeftHandle);

		updateTileHandles();
	}

	static final int HANDLE_SIZE = 10;

	public void setEnableTileHandles(boolean enable) {
		_resizeBottomLeftHandle.setVisible(enable);
		_resizeBottomRightHandle.setVisible(enable);
		_resizeTopRightHandle.setVisible(enable);
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

		if (_resizeBottomLeftHandle != null) {
			updateTileHandles();
		}
	}

	private void updateTileHandles() {
		double w = getMinWidth();
		double h = getMinHeight();

		int hs = HANDLE_SIZE / 2;
		_resizeTopRightHandle.relocate(w - hs, -HANDLE_SIZE / 2);
		_resizeBottomRightHandle.relocate(w - hs, h - hs);
		_resizeBottomLeftHandle.relocate(-HANDLE_SIZE / 2, h - hs);
	}

	public IObjectNode getObjectNode() {
		return _objectNode;
	}
}
