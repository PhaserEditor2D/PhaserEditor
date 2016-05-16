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

import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;

/**
 * @author arian
 *
 */
public class ZoomBehavior {
	private ObjectCanvas _canvas;

	public ZoomBehavior(ObjectCanvas canvas) {
		super();
		_canvas = canvas;

		_canvas.getScene().addEventFilter(ScrollEvent.ANY, e -> {
			double delta = 1.2;

			double scale = canvas.getScale();

			// double oldScale = scale;

			if (e.getDeltaY() < 0)
				scale /= delta;
			else
				scale *= delta;

			scale = clamp(scale, 0.1d, 10d);

			// double f = (scale / oldScale) - 1;
			//
			// double dx = (e.getSceneX()
			// - (world.getBoundsInParent().getWidth() / 2 +
			// world.getBoundsInParent().getMinX()));
			// double dy = (e.getSceneY()
			// - (world.getBoundsInParent().getHeight() / 2 +
			// world.getBoundsInParent().getMinY()));

			canvas.setScale(scale);

			// note: pivot value must be untransformed, i. e. without scaling
			// canvas.setPivot(f * dx, f * dy);

			canvas.getSelectionPane().getChildren().forEach(n -> {
				((SelectionNode) n).updateScale();
			});

			e.consume();

		});
	}

	static double clamp(double value, double min, double max) {

		if (Double.compare(value, min) < 0)
			return min;

		if (Double.compare(value, max) > 0)
			return max;

		return value;
	}

	public ObjectCanvas getCanvas() {
		return _canvas;
	}

}
