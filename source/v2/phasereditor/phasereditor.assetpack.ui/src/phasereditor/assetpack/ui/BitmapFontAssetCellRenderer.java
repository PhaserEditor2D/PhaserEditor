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

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;

import phasereditor.assetpack.core.BitmapFontAssetModel;
import phasereditor.bmpfont.core.BitmapFontModel.RenderArgs;
import phasereditor.bmpfont.core.BitmapFontRenderer;
import phasereditor.ui.FrameData;
import phasereditor.ui.ICanvasCellRenderer;
import phasereditor.ui.ImageProxy;
import phasereditor.ui.ZoomCanvas.ZoomCalculator;

/**
 * @author arian
 *
 */
public class BitmapFontAssetCellRenderer implements ICanvasCellRenderer {
	private BitmapFontAssetModel _model;

	public BitmapFontAssetCellRenderer(BitmapFontAssetModel model) {
		_model = model;
	}

	@Override
	public void render(Canvas canvas, GC gc, int x, int y, int width, int height) {
		var frame = _model.getFrame();
		var file = frame.getImageFile();
		var model = _model.getFontModel();

		if (file != null) {
			var text = "abc123";// asset.getKey();

			var metrics = model.metrics(text);
			var calc = new ZoomCalculator(metrics.getWidth(), metrics.getHeight());
			calc.fit(new Rectangle(0, 0, width, height));

			if (calc.scale > 0) {

				model.render(new RenderArgs(text), new BitmapFontRenderer() {

					@Override
					public void render(char c, int charX, int charY, int charW, int charH, int srcX, int srcY, int srcW,
							int srcH) {
						var z = calc.modelToView(charX, charY, charW, charH);

						var fd = FrameData.fromSourceRectangle(new Rectangle(srcX, srcY, srcW, srcH));
						var proxy = ImageProxy.get(file, fd);

						proxy.paint(gc, x + z.x, y + z.y, z.width, z.height);
					}

				});

			}
		}

	}

}
