// The MIT License (MIT)
//
// Copyright (c) 2015 Arian Fornaris
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

import org.eclipse.core.resources.IFile;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

public class ImageCanvas extends Canvas implements PaintListener {

	protected Image _image;
	private Point _preferredSize;
	private boolean _paintBorder;
	private String _noImageMessage = "(no image)";

	public ImageCanvas(Composite parent, int style) {
		super(parent, style | SWT.DOUBLE_BUFFERED);
		addPaintListener(this);
		_preferredSize = new Point(0, 0);
		_paintBorder = true;
	}

	public String getNoImageMessage() {
		return _noImageMessage;
	}

	public void setNoImageMessage(String noImageMessage) {
		_noImageMessage = noImageMessage;
	}

	public void setImageFile(IFile file) {
		setImageFile(file == null ? null : file.getLocation().toFile().getAbsolutePath());
	}

	public void setImageFile(String filepath) {
		if (filepath == null) {
			setImage(null);
			return;
		}

		loadImage(filepath);
	}

	public void loadImage(String filepath) {
		Image image;
		try {
			image = new Image(getDisplay(), filepath);
		} catch (Exception e) {
			e.printStackTrace();
			image = null;
		}
		setImage(image);
	}

	public Image getImage() {
		return _image;
	}

	public void setImage(Image image) {
		if (_image != null) {
			_image.dispose();
		}
		_image = image;
		redraw();
	}

	@Override
	public void dispose() {
		if (_image != null) {
			_image.dispose();
		}
		super.dispose();
	}

	@Override
	public void paintControl(PaintEvent e) {
		GC gc = e.gc;

		Rectangle dst = getBounds();
		PhaserEditorUI.paintPreviewBackground(gc, new Rectangle(0, 0, dst.width, dst.height));

		if (_image == null) {
			PhaserEditorUI.paintPreviewMessage(gc, dst, _noImageMessage);
		} else {
			Rectangle src = _image.getBounds();
			Rectangle b = PhaserEditorUI.computeImageZoom(src, dst);

			drawImage(gc, src.x, src.y, src.width, src.height, b.width, b.height, b.x, b.y);

			if (_paintBorder) {
				drawBorder(gc, b);
			}

			drawMore(gc, src.width, src.height, b.width, b.height, b.x, b.y);
		}
	}

	protected void drawBorder(GC gc, Rectangle rect) {
		gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_BLACK));
		gc.drawRectangle(rect.x, rect.y, rect.width - 1, rect.height - 1);
		gc.setLineWidth(1);
	}

	@SuppressWarnings("unused")
	protected void drawMore(GC gc, int srcW, int srcH, int dstW, int dstH, int dstX, int dstY) {
		// empty
	}

	protected void drawImage(GC gc, int srcX, int srcY, int srcW, int srcH, int dstW, int dstH, int dstX, int dstY) {
		gc.drawImage(_image, srcX, srcY, srcW, srcH, dstX, dstY, dstW, dstH);
	}

	public String getResolution() {
		if (_image != null) {
			Rectangle b = _image.getBounds();
			return b.width + " x " + b.height;
		}
		return "";
	}

	public Rectangle getImageDimension() {
		if (_image == null) {
			return null;
		}
		return _image.getBounds();
	}

	public Point getPreferredSize() {
		return _preferredSize;
	}

	public void setPreferredSize(Point preferredSize) {
		_preferredSize = preferredSize;
	}

	public boolean isPaintBorder() {
		return _paintBorder;
	}

	public void setPaintBorder(boolean paintBorder) {
		_paintBorder = paintBorder;
	}

	@Override
	public Point computeSize(int wHint, int hHint, boolean changed) {
		if (_preferredSize != null && _preferredSize.x != 0) {
			return _preferredSize;
		}
		return super.computeSize(wHint, hHint, changed);
	}
}
