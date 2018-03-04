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
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import phasereditor.canvas.core.BaseObjectModel;
import phasereditor.canvas.core.GroupModel;
import phasereditor.canvas.ui.shapes.BaseObjectControl;
import phasereditor.canvas.ui.shapes.GroupNode;
import phasereditor.canvas.ui.shapes.IObjectNode;

/**
 * @author arian
 *
 */
public class SelectionNode extends Pane {
	private IObjectNode _objectNode;
	private ObjectCanvas _canvas;
	private Label _label;
	private Pane _frame;

	public SelectionNode(ObjectCanvas canvas, IObjectNode inode) {
		_objectNode = inode;
		_canvas = canvas;

		_label = new Label(inode.getModel().getEditorName());
		_label.setTextFill(Color.WHITE);
		_label.setEffect(new DropShadow());

		_frame = new Pane();
		_frame.setEffect(new DropShadow());
		Color color = inode.getModel() instanceof GroupModel ? Color.LIGHTGREEN : Color.GREENYELLOW;
		_frame.setBorder(
				new Border(new BorderStroke(color, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(2))));

		getChildren().setAll(_label, _frame);

		update();
	}

	public static final int HANDLER_SIZE = 10;

	public ObjectCanvas getCanvas() {
		return _canvas;
	}

	public void update() {
		Node node = _objectNode.getNode();
		BaseObjectControl<?> control = _objectNode.getControl();
		BaseObjectModel model = _objectNode.getModel();

		double pw;
		double ph;

		Point2D p1;
		if (node instanceof GroupNode) {
			GroupNode group = (GroupNode) node;
			double minX = Double.MAX_VALUE;
			double minY = Double.MAX_VALUE;
			double maxX = Double.MIN_VALUE;
			double maxY = Double.MIN_VALUE;

			Point2D[] points = new Point2D[4];

			for (Node child : group.getChildren()) {
				Bounds b = child.getBoundsInLocal();

				points[0] = child.localToScene(0, 0);
				points[1] = child.localToScene(b.getWidth(), 0);
				points[2] = child.localToScene(b.getWidth(), b.getHeight());
				points[3] = child.localToScene(0, b.getHeight());

				for (Point2D point : points) {
					minX = Math.min(point.getX(), minX);
					minY = Math.min(point.getY(), minY);
					maxX = Math.max(point.getX(), maxX);
					maxY = Math.max(point.getY(), maxY);
				}
			}

			p1 = new Point2D(minX, minY);

			Point2D p2 = new Point2D(maxX, minY);
			Point2D p4 = new Point2D(minX, maxY);

			pw = p1.distance(p2);
			ph = p1.distance(p4);

		} else {
			p1 = node.localToScene(0, 0);
			double w = control.getTextureWidth();
			double h = control.getTextureHeight();
			Point2D p2 = node.localToScene(w, 0);
			Point2D p3 = node.localToScene(w, h);
			Point2D p4 = node.localToScene(0, h);
			pw = p1.distance(p2);
			ph = p1.distance(p4);
			
			double sx = model.getScaleX();
			double sy = model.getScaleY();
			
			if (sx < 0) {
				if (sy < 0) {
					p1 = p3;
				} else {
					p1 = p2;
				}
			} else {
				if (sy < 0) {
					p1 = p4;	
				} else {
					// p1 == p1
				}
			}
		}

		_frame.setMaxWidth(pw);
		_frame.setMinWidth(pw);
		_frame.setMaxHeight(ph);
		_frame.setMinHeight(ph);

		
		_frame.relocate(p1.getX(), p1.getY());
		_label.relocate(p1.getX(), p1.getY());

		if (_objectNode instanceof GroupNode) {
			_label.getTransforms().setAll(new Translate(0, -20));
		} else {
			double a = model.getGlobalAngle();
			_frame.getTransforms().setAll(new Rotate(a, 0, 0));
			_label.getTransforms().setAll(new Rotate(a, 0, 0), new Translate(0, -20));
		}

	}

	public IObjectNode getObjectNode() {
		return _objectNode;
	}
}