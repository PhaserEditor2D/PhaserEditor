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
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
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
public class FrameGridCanvas extends BaseImageCanvas
		implements PaintListener, IZoomable, MouseWheelListener, KeyListener {

	private int _listIconSize = 32;
	private List<Rectangle> _renderImageSrcFrames;
	private List<Rectangle> _renderImageDstFrames;
	private List<Rectangle> _selectionFrameArea;
	private List<Image> _images;
	private List<Object> _objects;
	private List<IFile> _files;
	private int _frameSize;
	private Rectangle _dst;
	private Point _origin;
	private List<String> _tooltips;
	private boolean _fitWindow;
	private FrameCanvasUtils _utils;
	private boolean _listLayout;

	public FrameGridCanvas(Composite parent, int style, boolean addDragAndDropSupport) {
		super(parent, style | SWT.DOUBLE_BUFFERED | SWT.V_SCROLL | SWT.NO_REDRAW_RESIZE);

		_renderImageSrcFrames = Collections.emptyList();
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
				return _renderImageSrcFrames.size();
			}

			@Override
			public Rectangle getRenderImageSrcFrame(int index) {
				return _renderImageSrcFrames.get(index);
			}

			@Override
			public Rectangle getRenderImageDstFrame(int index) {
				return _renderImageDstFrames.get(index);
			}

			@Override
			public Rectangle getSelectionFrameArea(int index) {
				return _selectionFrameArea.get(index);
			}

			@Override
			public Object getFrameObject(int index) {
				return _objects.get(index);
			}

			@Override
			public IFile getImageFile(int index) {
				return _files.get(index);
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

		addKeyListener(this);
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

		for (int i = 0; i < _renderImageSrcFrames.size(); i++) {
			var src = _renderImageSrcFrames.get(i);
			var selRect = _selectionFrameArea.get(i);

			var selected = _utils.getSelectedIndexes().contains(i);

			if (selected) {
				gc.setBackground(PhaserEditorUI.get_pref_Preview_frameSelectionColor());
				gc.setAlpha(100);
				gc.fillRectangle(selRect);
				gc.setAlpha(255);
			} else {
				PhaserEditorUI.paintPreviewBackground(gc, selRect);
			}

			{
				var dst = _renderImageDstFrames.get(i);

				var image = _images.get(i);

				if (image != null) {
					gc.drawImage(image, src.x, src.y, src.width, src.height, dst.x, dst.y, dst.width, dst.height);
				}

			}

			if (i == _utils.getOverIndex() || selected) {
				gc.drawRectangle(selRect);
			}
		}

		if (_listLayout) {
			for (var r : _selectionFrameArea) {
				String str = "name";
				var size = gc.stringExtent(str);
				gc.drawText(str, _frameSize + 20, r.y + r.height / 2 - size.y / 2, true);
			}
		}

	}

	public boolean isListLayout() {
		return _listLayout;
	}

	public void setListLayout(boolean singleColumnLayout) {
		_listLayout = singleColumnLayout;
		updateScroll();
		redraw();
	}

	public void setListLayoutIconSize(int listLayoutIconSize) {
		_listIconSize = listLayoutIconSize;
		updateScroll();
		redraw();
	}

	public int getListLayoutIconSize() {
		return _listIconSize;
	}

	private void computeRects() {

		if (_images.isEmpty()) {
			return;
		}

		Rectangle b = getClientArea();

		int S = 5;
		int box = _frameSize + S;

		{

			int x = 0;
			int y = 0;

			_renderImageDstFrames = new ArrayList<>();
			_selectionFrameArea = new ArrayList<>();

			int maxWidth = b.width;

			if (_listLayout) {
				maxWidth = _listIconSize;
			}

			for (Rectangle frame : _renderImageSrcFrames) {

				ZoomCalculator c = new ZoomCalculator(frame.width, frame.height);
				c.fit(_frameSize, _frameSize);

				Rectangle dst = new Rectangle(x + (int) c.offsetX, y + (int) c.offsetY, (int) (frame.width * c.scale),
						(int) (frame.height * c.scale));

				_renderImageDstFrames.add(dst);

				x += S + _frameSize;

				if (x + box > maxWidth) {
					y += box;
					x = 0;
				}
			}

			Point min = new Point(Integer.MAX_VALUE, Integer.MAX_VALUE);
			Point max = new Point(Integer.MIN_VALUE, Integer.MIN_VALUE);

			for (Rectangle place : _renderImageDstFrames) {
				min.x = Math.min(min.x, place.x);
				min.y = Math.min(min.y, place.y);
				max.x = Math.max(max.x, place.x + place.width);
				max.y = Math.max(max.y, place.y + place.height);
			}

			x = 0;
			y = 0;
			if (max.x < maxWidth) {
				x = (maxWidth - max.x - min.x) / 2;
			}
			if (max.y < b.height) {
				y = (b.height - max.y - min.y) / 2;
			}

			_dst = new Rectangle(x, y, max.x - min.x, max.y - min.y);

			for (Rectangle place : _renderImageDstFrames) {
				place.x += x;
				place.y += y;
			}
		}

		{
			if (_listLayout) {
				int i = 0;
				for (var dst : _renderImageDstFrames) {
					dst.y = i * box;
					var r = new Rectangle(0, dst.y, b.width, box);
					_selectionFrameArea.add(r);
					i++;
				}
			} else {
				_selectionFrameArea = new ArrayList<>(_renderImageDstFrames);
			}
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
		int count = _renderImageSrcFrames.size() * 2;
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
		return _renderImageSrcFrames;
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

			_renderImageSrcFrames.add(frame);
			_images.add(image);
			_tooltips.add(tooltip);
			_objects.add(object);
			_files.add(file);

		}

		resetZoom();
	}

	private void resetFramesData() {
		_renderImageSrcFrames = new ArrayList<>();
		_images = new ArrayList<>();
		_tooltips = new ArrayList<>();
		_objects = new ArrayList<>();
		_files = new ArrayList<>();
	}

	@Override
	public void mouseScrolled(MouseEvent e) {
		if (_listLayout) {
			return;
		}

		int dir = e.count;

		zoom(dir);

		_utils.updateOverIndex(e);
	}

	private void zoom(int dir) {
		double f = dir < 0 ? 0.8 : 1.2;
		int newSize = (int) (getFrameSize() * f);
		if (newSize == getFrameSize()) {
			newSize = dir < 0 ? 1 : newSize * 2;
		}

		if (newSize < 16) {
			newSize = 16;
		}

		setFrameSize(newSize);

		updateScroll();
	}

	@Override
	public void keyPressed(KeyEvent e) {
		switch (e.keyCode) {
		case SWT.KEYPAD_ADD:
			zoom(1);
			break;
		case SWT.KEYPAD_SUBTRACT:
			zoom(-1);
			break;

		default:
			break;
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
		//
	}

	public void selectAll() {
		_utils.selectAll();
	}
}
