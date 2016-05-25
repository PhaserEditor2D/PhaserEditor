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
import javafx.scene.Node;
import javafx.scene.control.Label;
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
import phasereditor.canvas.ui.shapes.IObjectNode;

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
	private Bounds _rect;
	private ObjectCanvas _canvas;
	private Label _label;

	public SelectionNode(ObjectCanvas canvas, IObjectNode inode, Bounds rect) {
		_objectNode = inode;
		_rect = rect;
		_canvas = canvas;

		updateZoomAndPan();

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
	}
	
	public ObjectCanvas getCanvas() {
		return _canvas;
	}

	public void updateZoomAndPan() {
		double scale = _canvas.getZoomBehavior().getScale();
		Point2D translate = _canvas.getZoomBehavior().getTranslate();
		double x = translate.getX() + _rect.getMinX() * scale;
		double y = translate.getY() + _rect.getMinY() * scale;

		double h = _rect.getHeight() * scale;
		double w = _rect.getWidth() * scale;

		relocate(x, y);
		setMinSize(w, h);
		setMaxSize(w, h);
	}

	public IObjectNode getObjectNode() {
		return _objectNode;
	}
}
