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
package phasereditor.bmpfont.ui;

import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;

import phasereditor.bmpfont.core.BitmapFontModel;
import phasereditor.bmpfont.core.BitmapFontModel.MetricsRenderer;
import phasereditor.bmpfont.core.BitmapFontModel.RenderArgs;
import phasereditor.bmpfont.core.BitmapFontRenderer;
import phasereditor.ui.FrameData;
import phasereditor.ui.ImageProxy;
import phasereditor.ui.ImageProxyCanvas;
import phasereditor.ui.PhaserEditorUI;

/**
 * @author arian
 *
 */
public class BitmapFontCanvas extends ImageProxyCanvas {

	private BitmapFontModel _model;
	private String _text;

	public BitmapFontCanvas(Composite parent, int style) {
		super(parent, style);

		_text = "";
	}

	@Override
	public void customPaintControl(PaintEvent e) {
		GC gc = e.gc;

		var _image = getProxy();

		if (_image == null) {
			PhaserEditorUI.paintPreviewMessage(gc, getBounds(), "Missing texture.");
		} else if (_model == null) {
			PhaserEditorUI.paintPreviewMessage(gc, getBounds(), "Missing bitmap font descriptor.");
		} else {
			ZoomCalculator calc = calc();

			{
				Rectangle z = calc.modelToView(0, 0, calc.imgWidth, calc.imgHeight);
				PhaserEditorUI.paintPreviewBackground(gc, new Rectangle(z.x, z.y, z.width, z.height));
			}

			var file = getFile();

			_model.render(new RenderArgs(_text), new BitmapFontRenderer() {

				@Override
				public void render(char c, int x, int y, int charW, int charH, int srcX, int srcY, int srcW, int srcH) {
					var z = calc.modelToView(x, y, charW, charH);

					var fd = FrameData.fromSourceRectangle(new Rectangle(srcX, srcY, srcW, srcH));
					var proxy = ImageProxy.get(file, fd);

					proxy.paint(gc, x + z.x, y + z.y, z.width, z.height);
				}

			});
		}

	}

	@Override
	protected ZoomCalculator calc() {
		ZoomCalculator calc = super.calc();

		if (_model != null && _text != null) {
			MetricsRenderer metrics = _model.metrics(_text);
			calc.imgWidth = metrics.getWidth();
			calc.imgHeight = metrics.getHeight();
		}

		return calc;
	}

	public void setModel(BitmapFontModel model) {
		_model = model;

		if (model != null) {
			_text = model.getInfoFace();
		}
	}

	public BitmapFontModel getModel() {
		return _model;
	}

	public String getText() {
		return _text;
	}

	public void setText(String text) {
		_text = text;
	}
}
