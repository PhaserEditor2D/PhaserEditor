// The MIT License (MIT)
//
// Copyright (c) 2015, 2017 Arian Fornaris
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ScrollBar;

import phasereditor.ui.ImageCanvas.ZoomCalculator;

/**
 * @author arian
 *
 */
public class SpriteGridCanvas extends Canvas implements PaintListener, IZoomable {

	private Image _image;
	private List<Rectangle> _frames;
	private List<Rectangle> _places;
	private int _frameSize;
	private Rectangle _dst;
	private Point origin;
	private int _overIndex;
	private List<String> _tooltips;

	public SpriteGridCanvas(Composite parent, int style) {
		super(parent, style | SWT.DOUBLE_BUFFERED | SWT.V_SCROLL | SWT.NO_REDRAW_RESIZE);

		_frames = Collections.emptyList();
		_frameSize = 64;
		_overIndex = -1;

		addPaintListener(this);
		addMouseTrackListener(new MouseTrackAdapter() {
			@Override
			public void mouseEnter(MouseEvent e) {
				if (!isFocusControl()) {
					forceFocus();
				}
			}
		});
		addMouseMoveListener(new MouseMoveListener() {

			@SuppressWarnings("synthetic-access")
			@Override
			public void mouseMove(MouseEvent e) {
				if (_places == null) {
					return;
				}
				
				int old = _overIndex;
				int index = -1;
				for (int i = 0; i < _places.size(); i++) {
					Rectangle place = _places.get(i);
					if (place.contains(e.x, e.y - origin.y)) {
						index = i;
						break;
					}
				}
				if (old != index) {
					_overIndex = index;
					if (index != -1 && _tooltips != null) {
						setToolTipText(_tooltips.get(_overIndex));
					}
					redraw();
				}
			}
		});
		addMouseWheelListener(new MouseWheelListener() {

			@Override
			public void mouseScrolled(MouseEvent e) {
				double f = e.count < 0 ? 0.8 : 1.2;
				int newSize = (int) (getFrameSize() * f);
				if (newSize == getFrameSize()) {
					newSize = e.button < 0 ? 1 : newSize * 2;
				}
				setFrameSize(newSize);
				updateScroll();
			}
		});

		origin = new Point(0, 0);

		final ScrollBar vBar = getVerticalBar();
		vBar.addListener(SWT.Selection, e -> {
			if (_dst == null) {
				return;
			}

			origin.y = -vBar.getSelection();
			redraw();
		});
		addListener(SWT.Resize, e -> {
			updateScroll();
		});
		
		afterCreateWidgets();
	}

	private void afterCreateWidgets() {
		// scrollable canvas do not get the rigth style
		PhaserEditorUI.forceApplyCompositeStyle(this);
	}

	void updateScroll() {
		if (_dst == null) {
			return;
		}

		computeRects();

		Rectangle rect = _dst;
		Rectangle client = getClientArea();
		ScrollBar vBar = getVerticalBar();
		vBar.setMaximum(rect.height);
		vBar.setThumb(Math.min(rect.height, client.height));
		int vPage = rect.height - client.height;
		int vSelection = vBar.getSelection();
		if (vSelection >= vPage) {
			if (vPage <= 0)
				vSelection = 0;
			origin.y = -vSelection;
		}

		redraw();
	}

	@Override
	public void paintControl(PaintEvent e) {
		GC gc = e.gc;

		computeRects();

		Transform tx = new Transform(getDisplay());
		tx.translate(0, origin.y);
		gc.setTransform(tx);

		for (int i = 0; i < _frames.size(); i++) {
			Rectangle frame = _frames.get(i);
			Rectangle place = _places.get(i);

			PhaserEditorUI.paintPreviewBackground(gc, place);

			gc.drawImage(_image, frame.x, frame.y, frame.width, frame.height, place.x, place.y, place.width,
					place.height);

			if (i == _overIndex) {
				gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_RED));
				gc.drawRectangle(place);
			}
		}
	}

	private void computeRects() {
		if (_image == null) {
			return;
		}

		Rectangle b = getClientArea();

		int S = 5;
		int box = _frameSize + S;

		int x = 0;
		int y = 0;

		_places = new ArrayList<>();

		for (Rectangle frame : _frames) {

			ZoomCalculator c = new ZoomCalculator(frame.width, frame.height);
			c.fit(_frameSize, _frameSize);

			Rectangle place = new Rectangle(x + (int) c.offsetX, y + (int) c.offsetY, (int) (frame.width * c.scale),
					(int) (frame.height * c.scale));
			_places.add(place);
			x += S + _frameSize;

			if (x + box > b.width) {
				y += box;
				x = 0;
			}
		}

		Point min = new Point(Integer.MAX_VALUE, Integer.MAX_VALUE);
		Point max = new Point(Integer.MIN_VALUE, Integer.MIN_VALUE);

		for (Rectangle place : _places) {
			min.x = Math.min(min.x, place.x);
			min.y = Math.min(min.y, place.y);
			max.x = Math.max(max.x, place.x + place.width);
			max.y = Math.max(max.y, place.y + place.height);
		}

		x = 0;
		y = 0;
		if (max.x < b.width) {
			x = (b.width - max.x - min.x) / 2;
		}
		if (max.y < b.height) {
			y = (b.height - max.y - min.y) / 2;
		}

		_dst = new Rectangle(x, y, max.x - min.x, max.y - min.y);

		for (Rectangle place : _places) {
			place.x += x;
			place.y += y;
		}
	}

	@Override
	public void fitWindow() {
		Rectangle b = getClientArea();
		int area = b.width * b.height;
		int count = _frames.size() * 2;
		_frameSize = (int) Math.sqrt((area - count * 5) / count);
		if (_frameSize < 32) {
			_frameSize = 32;
		}
		updateScroll();
	}

	public Image getImage() {
		return _image;
	}

	public void setImage(Image image) {
		_image = image;
	}

	public List<Rectangle> getFrames() {
		return _frames;
	}

	public void setFrames(List<Rectangle> frames) {
		_frames = frames;
	}

	public void setTooltips(List<String> tooltips) {
		_tooltips = tooltips;
	}

	public int getFrameSize() {
		return _frameSize;
	}

	public void setFrameSize(int frameSize) {
		_frameSize = frameSize;
	}

	public int getOverIndex() {
		return _overIndex;
	}
}
