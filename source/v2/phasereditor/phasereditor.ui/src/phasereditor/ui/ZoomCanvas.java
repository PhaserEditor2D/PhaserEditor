// The MIT License (MIT)
//
// Copyright (c) 2015,2018 Arian Fornaris
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
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

public abstract class ZoomCanvas extends Canvas implements PaintListener, IZoomable {

	private Point _preferredSize;
	private int _offsetX;
	private int _offsetY;
	private float _scale = 1;
	private boolean _fitWindow;
	private boolean _zoomWhenShiftPressed;

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

		public float viewToModelWidth(float width) {
			return width / scale;
		}

		public float viewToModelHeight(float height) {
			return height / scale;
		}

		public float modelToViewWidth(float width) {
			return width * scale;
		}

		public float modelToViewHeight(float height) {
			return height * scale;
		}

		public float viewToModelX(float x) {
			return (x - offsetX) / scale;
		}

		public float viewToModelY(float y) {
			return (y - offsetY) / scale;
		}

		public float modelToViewX(float x) {
			return offsetX + x * scale;
		}

		public float modelToViewY(float y) {
			return offsetY + y * scale;
		}

		public Rectangle modelToView(float x, float y, float width, float height) {
			return new Rectangle((int) (offsetX + x * scale), (int) (offsetY + y * scale), (int) (width * scale),
					(int) (height * scale));
		}

		public Rectangle modelToView(Rectangle rect) {
			return modelToView(rect.x, rect.y, rect.width, rect.height);
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
				setPanOffsetX(_startOffset.x + e.x - _startPoint.x);
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

			if (isZoomWhenShiftPressed()) {
				if ((e.stateMask & SWT.SHIFT) == 0) {
					return;
				}
			}

			float zoom = (e.count < 0 ? 0.9f : 1.1f);

			float oldScale = getScale();

			ZoomCalculator calc = calc();

			float x1 = calc.viewToModelX(e.x);
			float y1 = calc.viewToModelY(e.y);

			float newScale = oldScale * zoom;

			calc.scale = newScale;

			float x2 = calc.viewToModelX(e.x);
			float y2 = calc.viewToModelY(e.y);

			float fx = (x2 - x1) / calc.imgWidth;
			float fy = (y2 - y1) / calc.imgHeight;

			setPanOffsetX((int) (calc.offsetX + calc.imgWidth * calc.scale * fx));
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

	public ZoomCanvas(Composite parent, int style) {
		super(parent, style);
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

		_zoomWhenShiftPressed = true;

	}

	public boolean isZoomWhenShiftPressed() {
		return _zoomWhenShiftPressed;
	}

	public void setZoomWhenShiftPressed(boolean zoomWhenShiftPressed) {
		_zoomWhenShiftPressed = zoomWhenShiftPressed;
	}

	@Override
	public final void paintControl(PaintEvent e) {
		if (_fitWindow) {
			if (fitWindow()) {
				_fitWindow = false;
			}
		}

		ImageProxyCanvas.prepareGC(e.gc);

		customPaintControl(e);
	}

	@SuppressWarnings("unused")
	protected void customPaintControl(PaintEvent e) {
		//
	}

	protected boolean fitWindow() {
		if (!hasImage()) {
			return false;
		}

		var calc = calc();
		calc.fit(getFitArea());

		setScaleAndOffset(calc);

		return true;
	}

	protected Rectangle getFitArea() {
		return getBounds();
	}

	protected abstract boolean hasImage();

	protected void setScaleAndOffset(ZoomCalculator calc) {
		setScale(calc.scale);
		setPanOffsetX((int) calc.offsetX);
		setOffsetY((int) calc.offsetY);
	}

	@Override
	public void setScale(float scale) {
		_scale = scale;
	}

	public float getScale() {
		return _scale;
	}

	@Override
	public void setPanOffsetX(int offsetX) {
		_offsetX = offsetX;
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

	@Override
	public void setOffsetY(int offsetY) {
		_offsetY = offsetY;
	}

	@Override
	public void resetZoom() {
		_fitWindow = true;
		redraw();
	}

	protected abstract Point getImageSize();

	protected ZoomCalculator calc() {
		Point size = getImageSize();
		ZoomCalculator c = new ZoomCalculator(size.x, size.y);
		c.offsetX = _offsetX;
		c.offsetY = _offsetY;
		c.scale = _scale;
		return c;
	}

	@SuppressWarnings("static-method")
	protected void drawImageBackground(GC gc, Rectangle b) {
		PhaserEditorUI.paintPreviewBackground(gc, b);
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
