// The MIT License (MIT)
//
// Copyright (c) 2015, 2018 Arian Fornaris
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
package phasereditor.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.ScrollBar;

/**
 * @author arian
 *
 */
public abstract class ScrollUtils {
	private Canvas _canvas;
	private Rectangle _computedScrollArea;
	private Point _origin;

	public ScrollUtils(Canvas canvas) {
		_canvas = canvas;
		_origin = new Point(0, 0);

		final ScrollBar vBar = _canvas.getVerticalBar();

		vBar.addListener(SWT.Selection, e -> {
			if (_computedScrollArea == null) {
				return;
			}

			_origin.y = -vBar.getSelection();
			_canvas.redraw();
		});

		_canvas.addListener(SWT.Resize, e -> {
			updateScroll();
		});

	}

	public void updateScroll() {
		_computedScrollArea = computeScrollArea();

		Rectangle client = _canvas.getClientArea();
		ScrollBar vBar = _canvas.getVerticalBar();
		vBar.setMaximum(_computedScrollArea.height);
		vBar.setThumb(Math.min(_computedScrollArea.height, client.height));
		int vPage = _computedScrollArea.height - client.height;
		int vSelection = vBar.getSelection();
		if (vSelection >= vPage) {
			if (vPage <= 0)
				vSelection = 0;
			_origin.y = -vSelection;
		}

		_canvas.redraw();
	}
	
	public void scrollTo(int y) {
		_canvas.getVerticalBar().setSelection(y);
		_origin.y = -y;
		updateScroll();
	}

	public Point getOrigin() {
		return _origin;
	}

	public abstract Rectangle computeScrollArea();

}
