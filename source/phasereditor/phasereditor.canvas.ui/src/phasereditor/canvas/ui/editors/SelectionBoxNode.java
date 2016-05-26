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

import javafx.geometry.Point2D;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.shape.StrokeType;

/**
 * @author arian
 *
 */
public class SelectionBoxNode extends Pane {

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

	public SelectionBoxNode() {
		setBorder(_border);
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
