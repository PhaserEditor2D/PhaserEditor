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

import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;

import phasereditor.ui.TreeCanvas.TreeCanvasItem;

/**
 * @author arian
 *
 */
public abstract class BaseImageTreeCanvasItemRenderer extends BaseTreeCanvasItemRenderer {

	public BaseImageTreeCanvasItemRenderer(TreeCanvasItem item) {
		super(item);
	}

	@Override
	public int computeRowHeight(TreeCanvas canvas) {

		var imgSize = canvas.getImageSize();
		
		if (!isIconified(imgSize)) {
			return imgSize + 32;
		}

		return imgSize;
	}

	@Override
	public void render(PaintEvent e, int index, int x, int y) {
		var canvas = _item.getCanvas();

		var gc = e.gc;

		int imgSize = canvas.getImageSize();

		var iconified = isIconified(imgSize);

		int textX = x + ICON_AND_TEXT_SPACE;
		int textHeight = 16;

		int rowHeight = computeRowHeight(canvas);

		// paint text

		String label = _item.getLabel();

		if (label != null) {
			var extent = gc.textExtent(label);

			textHeight = extent.y;

			if (_item.isHeader()) {
				gc.setFont(SwtRM.getBoldFont(canvas.getFont()));
			}

			if (_item.isParentByNature() && _item.getChildren().isEmpty()) {
				gc.setAlpha((int) (125 * _item.getAlpha()));
			}

			gc.setAlpha((int) (255 * _item.getAlpha()));

			if (iconified) {
				gc.drawText(label, textX + imgSize, y + (rowHeight - textHeight) / 2, true);
			} else {
				gc.drawText(label, textX, y + rowHeight - textHeight - 5, true);
			}

			gc.setAlpha(255);
			gc.setFont(canvas.getFont());
		}

		// paint image

		Rectangle area = null;
		if (iconified) {
			area = new Rectangle(x + 2, y + 1, imgSize, rowHeight - 2);
		} else {
			area = new Rectangle(x + 2, y + 2, e.width - x - 5, rowHeight - textHeight - 10);
		}

		paintScaledInArea(gc, area, true);
	}

	protected boolean isIconified(int imgSize) {
		return imgSize <= 64;
	}

	protected abstract void paintScaledInArea(GC gc, Rectangle area, boolean b);
}
