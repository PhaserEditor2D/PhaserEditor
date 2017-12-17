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

import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import phasereditor.canvas.core.GroupModel;
import phasereditor.canvas.ui.shapes.IObjectNode;

/**
 * @author arian
 *
 */
public class SelectionNode extends Pane {

//	private static Border _borderSprite;
//	private static Border _borderGroup;

//	static {
//		BorderWidths bw = new BorderWidths(1);
//
//		List<Double> dashed = Arrays.asList(5d, 2d);
//		BorderStrokeStyle style1 = new BorderStrokeStyle(StrokeType.INSIDE, StrokeLineJoin.MITER, StrokeLineCap.BUTT,
//				10, 10, dashed);
//		BorderStrokeStyle style2 = new BorderStrokeStyle(StrokeType.INSIDE, StrokeLineJoin.MITER, StrokeLineCap.BUTT,
//				10, 0, dashed);
//
//		BorderStroke s1 = new BorderStroke(Color.WHITE, style1, null, bw);
//		BorderStroke s2 = new BorderStroke(Color.BLACK, style2, null, bw);
//		BorderStroke s3 = new BorderStroke(Color.BLACK, style2, null, new BorderWidths(2));
//
//		_borderSprite = new Border(s1, s2);
//
//		_borderGroup = new Border(s1, s3);
//	}

	private IObjectNode _objectNode;
	protected Bounds _rect;
	private ObjectCanvas _canvas;
	private Label _label;

	public SelectionNode(ObjectCanvas canvas, IObjectNode inode, Bounds rect) {
		_objectNode = inode;
		_rect = rect;
		_canvas = canvas;
		
		_label = new Label(inode.getModel().getEditorName());
		_label.setTextFill(Color.WHITE);
		_label.setEffect(new DropShadow());
		_label.relocate(0, -20);
		
		getChildren().add(_label);

		updateFromZoomAndPanVariables();

		setEffect(new DropShadow());
		Color color = inode.getModel() instanceof GroupModel ? Color.LIGHTGREEN: Color.GREENYELLOW;
		setBorder(new Border(new BorderStroke(color, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(2))));
	}

	public static final int HANDLER_SIZE = 10;

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

	}

	public IObjectNode getObjectNode() {
		return _objectNode;
	}
}