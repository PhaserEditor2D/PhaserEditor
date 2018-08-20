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
import org.eclipse.swt.graphics.Image;
import org.eclipse.wb.swt.SWTResourceManager;

import phasereditor.ui.TreeCanvas.TreeCanvasItem;

public class IconTreeCanvasItemRenderer extends BaseTreeCanvasItemRenderer {

	private Image _icon;

	public IconTreeCanvasItemRenderer(TreeCanvasItem item, Image icon) {
		super(item);
		_icon = icon;
	}

	@Override
	public void render(PaintEvent e, int index, int x, int y) {
		var canvas = _item.getCanvas();
		
		var gc = e.gc;

		int textX = x;
		int rowHeight = computeRowHeight(canvas);

		var iconBounds = _icon == null ? null : _icon.getBounds();

		if (iconBounds != null) {
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

		if (iconBounds != null) {

			// paint icon

			if (_item.isHeader()) {
				gc.drawImage(_icon, textX - 16 - ICON_AND_TEXT_SPACE, y + (rowHeight - iconBounds.height) / 2);
			} else {
				gc.drawImage(_icon, x, y + (rowHeight - iconBounds.height) / 2);
			}
		}
	}

	@Override
	public int computeRowHeight(TreeCanvas canvas) {
		return TreeCanvas.MIN_ROW_HEIGHT;
	}

	@Override
	public Image get_DND_Image() {
		return _icon;
	}

	@Override
	public FrameData get_DND_Image_FrameData() {
		return _icon == null? null : FrameData.fromImage(_icon);
	}
}