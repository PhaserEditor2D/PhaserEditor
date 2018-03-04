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

import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

/**
 * @author arian
 *
 */
public class SelectionDragNode extends Pane {

	public SelectionDragNode() {
		// setBorder(_border);
		setEffect(new DropShadow());
		Color c = Color.GREENYELLOW;
		setBorder(new Border(new BorderStroke(c, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(2))));
		setBackground(new Background(new BackgroundFill(new Color(c.getRed(), c.getGreen(), c.getBlue(), 0.1),
				CornerRadii.EMPTY, Insets.EMPTY)));
	}

	public void setBox(Point2D start, Point2D end) {
		double x1 = Math.min(start.getX(), end.getX());
		double y1 = Math.min(start.getY(), end.getY());
		double x2 = Math.max(start.getX(), end.getX());
		double y2 = Math.max(start.getY(), end.getY());

		double w = x2 - x1;
		double h = y2 - y1;

		relocate(x1, y1);
		setMinSize(w, h);
		setMaxSize(w, h);
	}
}
