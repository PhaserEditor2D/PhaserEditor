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
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

public class ImageCanvas extends Canvas implements PaintListener, IZoomable {

	protected Image _image;
	private Point _preferredSize;
	private String _noImageMessage = "(no image)";
	private int _offsetX;
	private int _offsetY;
	private float _scale = 1;
	private Rectangle _viewport;

	public static class ZoomCalculator {
		public float imgWidth;
		public float imgHeight;
		public float offsetX;
		public float offsetY;
		public float scale;

		public ZoomCalculator(int imgWidth, int imgHeight) {
			this.imgWidth = imgWidth;
			this.imgHeight = imgHeight;
		}

		public float screenToImageX(float x) {
			return (x - offsetX) / scale;
		}

		public float screenToImageY(float y) {
			return (y - offsetY) / scale;
		}

		public Rectangle imageToScreen(float x, float y, float width, float height) {
			return new Rectangle((int) (offsetX + x * scale), (int) (offsetY + y * scale), (int) (width * scale),
					(int) (height * scale));
		}

		public Rectangle imageToScreen(Rectangle rect) {
			return imageToScreen(rect.x, rect.y, rect.width, rect.height);
		}

		public void imageSize(Rectangle rect) {
			imgWidth = rect.width;
			imgHeight = rect.height;
		}

		public void fit(Rectangle window) {
			scale = Math.min(window.width / imgWidth, window.height / imgHeight);
			offsetX = window.width / 2 - imgWidth * scale / 2;
			offsetY = window.height / 2 - imgHeight * scale / 2;
		}

		public void fit(int width, int height) {
			fit(new Rectangle(0, 0, width, height));
		}
	}

	class MyMouseListener implements MouseMoveListener, MouseListener, MouseWheelListener, MouseTrackListener {

		private Point _startPoint;
		private Point _startOffset;

		@Override
		public void mouseMove(MouseEvent e) {
			if (_startPoint != null) {
				setOffsetX(_startOffset.x + e.x - _startPoint.x);
				setOffsetY(_startOffset.y + e.y - _startPoint.y);
				redraw();
			}
		}

		@Override
		public void mouseDoubleClick(MouseEvent e) {
			//
		}

		@Override
		public void mouseDown(MouseEvent e) {
			if (!isFocusControl()) {
				setFocus();
			}

			if (e.button == 2) {
				_startPoint = new Point(e.x, e.y);
				_startOffset = new Point(getOffsetX(), getOffsetY());
			}
		}

		@Override
		public void mouseUp(MouseEvent e) {
			_startPoint = null;
		}

		@Override
		public void mouseScrolled(MouseEvent e) {
			float zoom = (e.count < 0 ? 0.9f : 1.1f);

			float oldScale = getScale();

			ZoomCalculator calc = calc();

			float x1 = calc.screenToImageX(e.x);
			float y1 = calc.screenToImageY(e.y);

			float newScale = oldScale * zoom;

			calc.scale = newScale;

			float x2 = calc.screenToImageX(e.x);
			float y2 = calc.screenToImageY(e.y);

			float fx = (x2 - x1) / calc.imgWidth;
			float fy = (y2 - y1) / calc.imgHeight;

			setOffsetX((int) (calc.offsetX + calc.imgWidth * calc.scale * fx));
			setOffsetY((int) (calc.offsetY + calc.imgHeight * calc.scale * fy));

			setScale(newScale);

			redraw();
		}

		@Override
		public void mouseEnter(MouseEvent e) {
			// This is silly!

			// if (!isFocusControl()) {
			// setFocus();
			// }
		}

		@Override
		public void mouseExit(MouseEvent e) {
			//
		}

		@Override
		public void mouseHover(MouseEvent e) {
			//
		}

	}

	public ImageCanvas(Composite parent, int style) {
		super(parent, style | SWT.DOUBLE_BUFFERED);
		addPaintListener(this);
		_preferredSize = new Point(0, 0);

		MyMouseListener listener = new MyMouseListener();
		addMouseMoveListener(listener);
		addMouseListener(listener);
		addMouseWheelListener(listener);
		addMouseTrackListener(listener);
		addKeyListener(new KeyListener() {

			@Override
			public void keyReleased(KeyEvent e) {
				//
			}

			@Override
			public void keyPressed(KeyEvent e) {
				//
			}
		});

		PhaserEditorUI.forceApplyCompositeStyle(this);

		PhaserEditorUI.redrawCanvasWhenPreferencesChange(this);
	}

	@Override
	public void fitWindow() {
		if (_image == null) {
			return;
		}

		ZoomCalculator calc = calc();
		calc.fit(getBounds());

		setScaleAndOffset(calc);
	}

	protected void setScaleAndOffset(ZoomCalculator calc) {
		setScale(calc.scale);
		setOffsetX((int) calc.offsetX);
		setOffsetY((int) calc.offsetY);
	}

	public void setScale(float scale) {
		_scale = scale;
	}

	public float getScale() {
		return _scale;
	}

	public int getOffsetX() {
		return _offsetX;
	}

	public void setOffsetX(int offsetX) {
		_offsetX = offsetX;
	}

	public int getOffsetY() {
		return _offsetY;
	}

	public void setOffsetY(int offsetY) {
		_offsetY = offsetY;
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
		setImage(image, image == null ? null : image.getBounds());
	}

	public void setImage(Image image, Rectangle viewport) {
		if (_image != null) {
			_image.dispose();
		}

		_image = image;

		if (image == null) {
			_viewport = null;
		} else {
			_viewport = viewport;
		}

		reset();
	}

	public void reset() {
		// fit the window when it is fully sized
		getDisplay().asyncExec(() -> {
			if (!isDisposed()) {
				fitWindow();
				redraw();
			}
		});
	}

	public void setImageViewport(Rectangle viewport) {
		_viewport = viewport;
	}

	public Rectangle getImageViewport() {
		return _viewport;
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

		if (_image == null) {
			PhaserEditorUI.paintPreviewMessage(gc, dst, _noImageMessage);
		} else {
			Rectangle src = _viewport;

			dst = new Rectangle(_offsetX, _offsetY, (int) (src.width * _scale), (int) (src.height * _scale));

			drawImageBackground(gc, dst);

			drawImage(gc, src.x, src.y, src.width, src.height, dst.width, dst.height, dst.x, dst.y);

			drawMore(gc, src.width, src.height, dst.width, dst.height, dst.x, dst.y);
		}
	}

	protected ZoomCalculator calc() {
		ZoomCalculator c = new ZoomCalculator(_viewport.width, _viewport.height);
		c.offsetX = _offsetX;
		c.offsetY = _offsetY;
		c.scale = _scale;
		return c;
	}

	@SuppressWarnings("static-method")
	protected void drawImageBackground(GC gc, Rectangle b) {
		PhaserEditorUI.paintPreviewBackground(gc, b);
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

	@Override
	public Point computeSize(int wHint, int hHint, boolean changed) {
		if (_preferredSize != null && _preferredSize.x != 0) {
			return _preferredSize;
		}
		return super.computeSize(wHint, hHint, changed);
	}
}
