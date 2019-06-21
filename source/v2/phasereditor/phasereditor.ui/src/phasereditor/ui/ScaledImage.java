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

import java.awt.image.BufferedImage;
import java.io.IOException;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;

/**
 * @author arian
 *
 */
public class ScaledImage {
	private float _scale;
	private Image _image;
	private int _viewWidth;
	private int _viewHeight;

	public static ScaledImage create(BufferedImage buffer, int maxSize) {
		return create(buffer, FrameData.fromImage(buffer), maxSize);
	}

	public static ScaledImage create(BufferedImage src, FrameData fd, int maxSize) {

		int viewWidth = fd.srcSize.x;
		int viewHeight = fd.srcSize.y;

		var resize = resizeInfo(viewWidth, viewHeight, maxSize);
		var buffer = resize.createImage(src, fd);
		try {
			var img = PhaserEditorUI.image_Swing_To_SWT(buffer);
			return new ScaledImage(img, viewWidth, viewHeight, resize.scale_view_to_proxy);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public ScaledImage(Image image, int viewWidth, int viewHeight, float scale) {
		super();
		_image = image;
		_viewWidth = viewWidth;
		_viewHeight = viewHeight;
		_scale = scale;
	}

	public static class ResizeInfo {
		public int width;
		public int height;
		public boolean changed;
		public float scale_view_to_proxy;

		public BufferedImage createImage(BufferedImage src, FrameData fd) {
			var buf = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

			var g2 = buf.createGraphics();

			g2.scale(scale_view_to_proxy, scale_view_to_proxy);

			g2.drawImage(src,

					fd.dst.x, fd.dst.y, fd.dst.x + fd.dst.width, fd.dst.y + fd.dst.height,

					fd.src.x, fd.src.y, fd.src.x + fd.src.width, fd.src.y + fd.src.height,

					null);

			g2.dispose();

			return buf;
		}
	}

	public static ResizeInfo resizeInfo(int w, int h, int maxSize) {

		var info = new ResizeInfo();
		info.scale_view_to_proxy = 1;
		info.width = w;
		info.height = h;
		info.changed = false;

		if (w > maxSize || h > maxSize) {
			if (w > h) {
				var ratio = (float) h / w;
				info.width = maxSize;
				info.height = (int) (info.width * ratio);
				info.scale_view_to_proxy = (float) info.width / w;
				info.changed = true;
			} else {
				var ratio = (float) h / w;
				info.height = maxSize;
				info.width = (int) (info.height / ratio);
				info.scale_view_to_proxy = (float) info.height / h;
				info.changed = true;
			}
		}

		return info;
	}

	public void paint(GC gc, int x, int y) {
		var b = _image.getBounds();
		gc.drawImage(_image, 0, 0, b.width, b.height, x, y, _viewWidth, _viewHeight);
	}

	public void paintScaledInArea(GC gc, Rectangle renderArea, boolean center) {

		var image = getImage();

		if (image == null) {
			return;
		}

		var bounds = image.getBounds();

		int renderHeight = renderArea.height;
		int renderWidth = renderArea.width;

		double imgW = bounds.width;
		double imgH = bounds.height;

		// compute the right width
		imgW = imgW * (renderHeight / imgH);
		imgH = renderHeight;

		// fix width if it goes beyond the area
		if (imgW > renderWidth) {
			imgH = imgH * (renderWidth / imgW);
			imgW = renderWidth;
		}

		double scale = imgW / bounds.width;

		var imgX = renderArea.x + (center ? renderWidth / 2 - imgW / 2 : 0);
		var imgY = renderArea.y + renderHeight / 2 - imgH / 2;

		double imgDstW = bounds.width * scale;
		double imgDstH = bounds.height * scale;

		if (imgDstW > 0 && imgDstH > 0) {
			gc.drawImage(image, 0, 0, bounds.width, bounds.height, (int) imgX, (int) imgY, (int) imgDstW, (int) imgDstH);
		}
	}

	public Image getImage() {
		return _image;
	}

	public void dispose() {
		_image.dispose();
	}

	public Rectangle getBounds() {
		return new Rectangle(0, 0, _viewWidth, _viewHeight);
	}

	public boolean hits(int x, int y) {

		var alpha = 0;

		Rectangle b = getBounds();

		if (b.contains(x, y)) {
			var img = getImage();

			if (img != null) {
				var data = img.getImageData();
				alpha = data.getAlpha((int) (x * _scale), (int) (y * _scale));
			}
		}

		return alpha != 0;
	}
}
