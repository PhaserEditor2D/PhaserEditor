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

import java.io.File;

import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.wb.swt.SWTResourceManager;

import phasereditor.ui.TreeCanvas.TreeCanvasItem;

/**
 * @author arian
 *
 */
public class ImageTreeCanvasItemRenderer extends BaseTreeCanvasItemRenderer {

	public ImageTreeCanvasItemRenderer(TreeCanvasItem item) {
		super(item);
	}

	@Override
	public int computeRowHeight(TreeCanvas canvas) {

		if (canvas.getImageSize() > 64) {
			return canvas.getImageSize() + 32;
		}

		return canvas.getImageSize();
	}

	@Override
	public void render(TreeCanvas canvas, PaintEvent e, int index, int x, int y) {
		var gc = e.gc;

		int imgSize = canvas.getImageSize();

		var iconified = imgSize <= 64;

		int textX = x + ICON_AND_TEXT_SPACE;
		int textHeight = 16;

		int rowHeight = computeRowHeight(canvas);

		// paint text

		String label = _item.getLabel();

		if (label != null) {
			var extent = gc.textExtent(label);
			
			textHeight = extent.y;
			
			if (_item.isHeader()) {
				gc.setFont(SWTResourceManager.getBoldFont(canvas.getFont()));
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

		var img = getItemImage(canvas);
		var fd = getItemFrameData();

		if (img != null) {
			if (iconified) {
				PhaserEditorUI.paintScaledImageInArea(gc, img, fd, new Rectangle(x + 2, y + 1, imgSize, rowHeight - 2),
						false, true);
			} else {
				PhaserEditorUI.paintScaledImageInArea(gc, img, fd,
						new Rectangle(x + 2, y + 2, e.width - x - 5, rowHeight - textHeight - 10), false, false);
			}
		}
	}

	protected FrameData getItemFrameData() {
		return _item.getFrameData();
	}

	protected Image getItemImage(TreeCanvas canvas) {
		File file = _item.getImageFile();
		return canvas.loadImage(file);
	}

}
