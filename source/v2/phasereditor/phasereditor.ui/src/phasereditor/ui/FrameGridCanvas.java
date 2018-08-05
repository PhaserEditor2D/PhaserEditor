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

import org.eclipse.core.resources.IFile;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ScrollBar;

import phasereditor.ui.ImageCanvas.ZoomCalculator;

/**
 * @author arian
 *
 */
@SuppressWarnings({ "boxing", "synthetic-access" })
public class FrameGridCanvas extends BaseImageCanvas implements PaintListener, IZoomable, MouseWheelListener {

	private List<Rectangle> _frames;
	private List<Rectangle> _places;
	private List<Image> _images;
	private List<Object> _objects;
	private List<IFile> _files;
	private int _frameSize;
	private Rectangle _dst;
	private Point _origin;
	private List<String> _tooltips;
	private boolean _fitWindow;
	private FrameCanvasUtils _utils;

	public FrameGridCanvas(Composite parent, int style, boolean addDragAndDropSupport) {
		super(parent, style | SWT.DOUBLE_BUFFERED | SWT.V_SCROLL | SWT.NO_REDRAW_RESIZE);

		_frames = Collections.emptyList();
		_images = Collections.emptyList();
		_frameSize = 64;

		addPaintListener(this);

		addMouseWheelListener(this);

		_utils = new FrameCanvasUtils(this, addDragAndDropSupport) {

			@Override
			public Point getRealPosition(int x, int y) {
				return new Point(x, y - _origin.y);
			}

			@Override
			public int getFramesCount() {
				return _places.size();
			}

			@Override
			public Rectangle getPaintFrame(int index) {
				return _places.get(index);
			}

			@Override
			public Object getFrameObject(int index) {
				return _objects.get(index);
			}

			@Override
			public IFile getImageFile(int index) {
				return _files.get(index);
			}

			@Override
			public Rectangle getImageFrame(int index) {
				return _frames.get(index);
			}
		};

		_origin = new Point(0, 0);

		final ScrollBar vBar = getVerticalBar();
		vBar.addListener(SWT.Selection, e -> {
			if (_dst == null) {
				return;
			}

			_origin.y = -vBar.getSelection();
			redraw();
		});
		addListener(SWT.Resize, e -> {
			updateScroll();
		});

		PhaserEditorUI.redrawCanvasWhenPreferencesChange(this);

		afterCreateWidgets();
	}

	private void afterCreateWidgets() {
		// scrollable canvas do not get the right style
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
			_origin.y = -vSelection;
		}

		redraw();
	}

	@Override
	public void paintControl(PaintEvent e) {
		if (_fitWindow) {
			_fitWindow = false;
			fitWindow();
		}

		GC gc = e.gc;

		computeRects();

		Transform tx = new Transform(getDisplay());
		tx.translate(0, _origin.y);
		gc.setTransform(tx);

		for (int i = 0; i < _frames.size(); i++) {
			var frame = _frames.get(i);
			var place = _places.get(i);
			var selected = _utils.getSelectedIndexes().contains(i);

			if (selected) {
				gc.setBackground(PhaserEditorUI.get_pref_Preview_frameSelectionColor());
				gc.setAlpha(100);
				gc.fillRectangle(place);

				gc.setAlpha(255);
			} else {
				PhaserEditorUI.paintPreviewBackground(gc, place);
			}

			Image image = _images.get(i);

			if (image != null) {
				gc.drawImage(image, frame.x, frame.y, frame.width, frame.height, place.x, place.y, place.width,
						place.height);
			}

			if (i == _utils.getOverIndex() || selected) {
				gc.drawRectangle(place);
			}
		}
	}

	private void computeRects() {
		if (_images.isEmpty()) {
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
	public void resetZoom() {
		_fitWindow = true;
		redraw();
	}

	protected void fitWindow() {
		Rectangle b = getClientArea();
		int area = b.width * b.height;
		int count = _frames.size() * 2;
		_frameSize = count == 0 ? 1 : (int) Math.sqrt((area - count * 5) / count);
		if (_frameSize < 32) {
			_frameSize = 32;
		}
		updateScroll();
	}

	public List<Image> getImages() {
		return _images;
	}

	public List<Rectangle> getFrames() {
		return _frames;
	}

	public int getFrameSize() {
		return _frameSize;
	}

	public void setFrameSize(int frameSize) {
		_frameSize = frameSize;
	}

	public int getOverIndex() {
		return _utils.getOverIndex();
	}

	@Override
	public void setScale(float i) {
		//
	}

	@Override
	public void setPanOffsetX(int i) {
		//
	}

	@Override
	public void setOffsetY(int i) {
		//
	}

	public void loadFrameProvider(IFrameProvider provider) {
		resetFramesData();

		for (int i = 0; i < provider.getFrameCount(); i++) {
			var frame = provider.getFrameRectangle(i);
			var file = provider.getFrameImageFile(i);
			var image = loadImage(file);
			var tooltip = provider.getFrameTooltip(i);
			var object = provider.getFrameObject(i);

			_frames.add(frame);
			_images.add(image);
			_tooltips.add(tooltip);
			_objects.add(object);
			_files.add(file);

		}

		resetZoom();
	}

	private void resetFramesData() {
		_frames = new ArrayList<>();
		_images = new ArrayList<>();
		_tooltips = new ArrayList<>();
		_objects = new ArrayList<>();
		_files = new ArrayList<>();
	}

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
	
	public void selectAll() {
		_utils.selectAll();
	}
}
