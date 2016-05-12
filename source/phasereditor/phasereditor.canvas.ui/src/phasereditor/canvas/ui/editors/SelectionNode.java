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

	public SelectionNode(IObjectNode inode, Bounds rect) {
		_objectNode = inode;
		setLayoutX(rect.getMinX());
		setLayoutY(rect.getMinY());
		setMinSize(rect.getWidth(), rect.getHeight());
		setMaxSize(rect.getWidth(), rect.getHeight());
		setBorder(_border);

		BaseObjectModel model = inode.getModel();
		Node node = inode.getNode();
		StringBuilder sb = new StringBuilder();
		sb.append(model.getLabel());
		sb.append(" ");
		sb.append((long) node.getLayoutX());
		sb.append(",");
		sb.append((long) node.getLayoutY());

		Label label = new Label(sb.toString());
		label.setId("label " + node.getId());
		label.setStyle("-fx-background-color:white;-fx-padding:0px 5px 0px 5px;");
		label.setMinHeight(20);
		label.setMaxHeight(20);
		label.relocate(0, -label.getMinHeight());
		getChildren().add(label);
	}

	public IObjectNode getObjectNode() {
		return _objectNode;
	}
}
