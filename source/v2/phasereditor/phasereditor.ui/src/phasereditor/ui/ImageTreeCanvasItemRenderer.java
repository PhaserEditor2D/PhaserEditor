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

import java.util.function.Supplier;

import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;

import phasereditor.ui.TreeCanvas.TreeCanvasItem;

/**
 * @author arian
 *
 */
public class ImageTreeCanvasItemRenderer extends BaseTreeCanvasItemRenderer {

	private Image _image;
	private Supplier<Image> _imageProvider;
	private FrameData _fd;

	public ImageTreeCanvasItemRenderer(TreeCanvasItem item, Image image) {
		this(item, image, FrameData.fromImage(image));
	}

	public ImageTreeCanvasItemRenderer(TreeCanvasItem item, Image image, FrameData fd) {
		super(item);
		_image = image;
		_fd = fd;
	}

	public ImageTreeCanvasItemRenderer(TreeCanvasItem item, Supplier<Image> imageProvider, FrameData fd) {
		super(item);
		_imageProvider = imageProvider;
		_fd = fd;
	}

	@Override
	public int computeRowHeight(TreeCanvas canvas) {

		if (canvas.getImageSize() > 64) {
			return canvas.getImageSize() + 32;
		}

		return canvas.getImageSize();
	}

	@Override
	public void render(PaintEvent e, int index, int x, int y) {
		var canvas = _item.getCanvas();

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

		// get the image
		
		buildImage();

		// paint image
		
		if (_image != null && !_image.isDisposed()) {
			if (iconified) {
				PhaserEditorUI.paintScaledImageInArea(gc, _image, _fd,
						new Rectangle(x + 2, y + 1, imgSize, rowHeight - 2), false, true);
			} else {
				PhaserEditorUI.paintScaledImageInArea(gc, _image, _fd,
						new Rectangle(x + 2, y + 2, e.width - x - 5, rowHeight - textHeight - 10), false, false);
			}
		}
	}

	private void buildImage() {
		if (_image == null) {
			if (_imageProvider != null) {
				var image = _imageProvider.get();
				if (image != null) {
					_image = image;
				}
			}
		}
	}

	@Override
	public Image get_DND_Image() {
		
		buildImage();
		
		return _image;
	}

	@Override
	public FrameData get_DND_Image_FrameData() {
		return _fd;
	}
}
