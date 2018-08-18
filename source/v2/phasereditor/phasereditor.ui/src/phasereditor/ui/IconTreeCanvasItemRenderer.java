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
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.wb.swt.SWTResourceManager;

import phasereditor.ui.TreeCanvas.IconType;
import phasereditor.ui.TreeCanvas.TreeCanvasItem;

public class IconTreeCanvasItemRenderer extends BaseTreeCanvasItemRenderer {

	public IconTreeCanvasItemRenderer(TreeCanvasItem item) {
		super(item);
	}

	@Override
	public void render(TreeCanvas canvas, PaintEvent e, int index, int x, int y) {
		var gc = e.gc;

		int textX = x;
		int rowHeight = computeRowHeight(canvas);

		var isImageFrame = _item.getIconType() == IconType.IMAGE_FRAME;
		var icon = _item.getIcon();
		var iconBounds = icon == null ? null : icon.getBounds();

		if (isImageFrame) {
			textX += canvas.getImageSize() + ICON_AND_TEXT_SPACE;
		} else if (iconBounds != null) {
			textX += iconBounds.width + ICON_AND_TEXT_SPACE;
		}

		// paint text

		String label = _item.getLabel();

		if (label != null) {
			var extent = gc.textExtent(label);

			if (_item.isHeader()) {
				gc.setFont(SWTResourceManager.getBoldFont(canvas.getFont()));
			}

			if (_item.isParentByNature() && _item.getChildren().isEmpty()) {
				gc.setAlpha((int) (125 * _item.getAlpha()));
			}

			gc.setAlpha((int) (255 * _item.getAlpha()));
			gc.drawText(label, textX, y + (rowHeight - extent.y) / 2, true);

			gc.setAlpha(255);
			gc.setFont(canvas.getFont());
		}

		// paint icon or image

		if (isImageFrame) {

			// paint image

			var file = _item.getImageFile();
			var img = canvas.loadImage(file);
			var fd = _item.getFrameData();

			if (img != null) {
				PhaserEditorUI.paintScaledImageInArea(gc, img, fd,
						new Rectangle(x + 2, y + 2, canvas.getImageSize(), canvas.getImageSize()), false, true);
			}

		} else if (iconBounds != null) {

			// paint icon

			if (_item.isHeader()) {
				gc.drawImage(icon, textX - 16 - ICON_AND_TEXT_SPACE, y + (rowHeight - iconBounds.height) / 2);
			} else {
				gc.drawImage(icon, x, y + (rowHeight - iconBounds.height) / 2);
			}
		}
	}

	@Override
	public int computeRowHeight(TreeCanvas canvas) {

		if (_item.getIconType() == IconType.IMAGE_FRAME) {
			return canvas.getImageSize() + 4;
		}

		return TreeCanvas.MIN_ROW_HEIGHT;
	}
}