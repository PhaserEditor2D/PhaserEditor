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
package phasereditor.assetpack.ui;

import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;

import phasereditor.assetpack.core.BitmapFontAssetModel;
import phasereditor.bmpfont.core.BitmapFontModel.RenderArgs;
import phasereditor.bmpfont.core.BitmapFontRenderer;
import phasereditor.ui.BaseTreeCanvasItemRenderer;
import phasereditor.ui.FrameData;
import phasereditor.ui.ImageCanvas.ZoomCalculator;
import phasereditor.ui.TreeCanvas;
import phasereditor.ui.TreeCanvas.TreeCanvasItem;

/**
 * @author arian
 *
 */
public class BitmapFontTreeCanvasRenderer extends BaseTreeCanvasItemRenderer {

	public BitmapFontTreeCanvasRenderer(TreeCanvasItem item) {
		super(item);
	}

	@Override
	public void render(PaintEvent e, int index, int x, int y) {

		GC gc = e.gc;

		var canvas = _item.getCanvas();

		var asset = (BitmapFontAssetModel) _item.getData();
		var frame = asset.getFrame();
		var image = canvas.loadImage(frame.getImageFile());
		var model = asset.getFontModel();
		var rowHeight = computeRowHeight(canvas);

		if (image != null && model != null) {
			var text = "abc123";// asset.getKey();

			var metrics = model.metrics(text);
			var calc = new ZoomCalculator(metrics.getWidth(), metrics.getHeight());
			calc.fit(new Rectangle(0, 0, e.width - x, canvas.getImageSize()));
			calc.offsetX = 0;
				
			model.render(new RenderArgs(text), new BitmapFontRenderer() {

				@Override
				public void render(char c, int charX, int charY, int charW, int charH, int srcX, int srcY, int srcW, int srcH) {
					Rectangle z = calc.imageToScreen(charX, charY, charW, charH);
					gc.drawImage(image, srcX, srcY, srcW, srcH, x + z.x, y + z.y, z.width, z.height);
				}

			});
		}

		{
			String label = asset.getKey();
			var extent = gc.textExtent(label);
			var textHeight = extent.y;
			gc.drawText(label, x, y + rowHeight - textHeight - 5, true);
		}

	}

	@Override
	public int computeRowHeight(TreeCanvas canvas) {
		return canvas.getImageSize() + 32;
	}

	@Override
	public Image get_DND_Image() {
		var frame = ((BitmapFontAssetModel) _item.getData()).getFrame();
		var file = frame.getImageFile();
		var img = _item.getCanvas().loadImage(file);
		return img;
	}

	@Override
	public FrameData get_DND_Image_FrameData() {
		var frame = ((BitmapFontAssetModel) _item.getData()).getFrame();
		var fd = frame.getFrameData();
		return fd;
	}

}
