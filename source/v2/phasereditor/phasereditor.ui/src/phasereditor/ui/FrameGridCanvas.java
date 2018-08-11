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
@SuppressWarnings({ "synthetic-access" })
public class FrameGridCanvas extends BaseImageCanvas
		implements PaintListener, IZoomable, MouseWheelListener, KeyListener {

	private static int S = 5;

	private List<Rectangle> _visibleRenderSelectionFrameAreas;

	private List<Rectangle> _renderImageSrcFrames;
	private List<Rectangle> _visibleRenderImageSrcFrames;

	private List<Rectangle> _visibleRenderImageDstFrames;

	private List<String> _labels;
	private List<String> _visibleLabels;

	private List<Image> _images;
	private List<Image> _visibleImages;

	private List<Object> _objects;
	private List<Object> _visibleObjects;

	private List<IFile> _files;
	private List<IFile> _visibleFiles;

	private int _frameSize;
	private Rectangle _dst;
	private Point _origin;

	private boolean _fitWindow;
	private FrameCanvasUtils _utils;
	private boolean _listLayout;
	private String _filter;
	private boolean[] _visibleMap;
	private int _visibleCount;
	private int _total;
	private String _nextFilterText;

	public FrameGridCanvas(Composite parent, int style, boolean initDND) {
		super(parent, style | SWT.DOUBLE_BUFFERED | SWT.V_SCROLL | SWT.NO_REDRAW_RESIZE);

		_renderImageSrcFrames = List.of();
		_images = List.of();
		_labels = List.of();
		_frameSize = 64;

		addPaintListener(this);

		addMouseWheelListener(this);

		_utils = new FrameCanvasUtils(this, initDND) {

			@Override
			public Point scrollPositionToReal(int x, int y) {
				return new Point(x, y - _origin.y);
			}
			
			@Override
			public Point realPositionToScroll(int x, int y) {
				return new Point(x, y + _origin.y);
			}

			@Override
			public int getFramesCount() {
				return _visibleCount;
			}

			@Override
			public Rectangle getRenderImageSrcFrame(int index) {
				return _visibleRenderImageSrcFrames.get(index);
			}

			@Override
			public Rectangle getSelectionFrameArea(int index) {
				return _visibleRenderSelectionFrameAreas.get(index);
			}

			@Override
			public Object getFrameObject(int index) {
				return _visibleObjects.get(index);
			}

			@Override
			public IFile getImageFile(int index) {
				return _visibleFiles.get(index);
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

		resetFramesData();
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

		if (_nextFilterText != null) {
			getVerticalBar().setSelection(0);
			_origin.y = 0;
			buildFilterMap(_nextFilterText);
			_nextFilterText = null;

			updateScroll();
		}

		GC gc = e.gc;

		computeRects();

		Transform tx = new Transform(getDisplay());
		tx.translate(0, _origin.y);
		gc.setTransform(tx);

		for (int i = 0; i < _visibleCount; i++) {
			var obj = _visibleObjects.get(i);
			var src = _visibleRenderImageSrcFrames.get(i);
			var area = _visibleRenderSelectionFrameAreas.get(i);

			var selected = _utils.isSelected(obj);

			if (selected) {
				gc.setBackground(PhaserEditorUI.getListSelectionColor());
				gc.fillRectangle(area);
			}

			if (_listLayout) {
				PhaserEditorUI.paintListItemBackground(gc, i, area);
			}

			{
				var dst = _visibleRenderImageDstFrames.get(i);

				var image = _visibleImages.get(i);

				if (image != null) {
					if (_listLayout) {
						int y = dst.y + (_frameSize + S - dst.height) / 2;
						gc.drawImage(image, src.x, src.y, src.width, src.height, dst.x, y, dst.width, dst.height);
					} else {
						gc.drawImage(image, src.x, src.y, src.width, src.height, dst.x, dst.y, dst.width, dst.height);
					}
				}

			}

			if (!_listLayout) {
				if (selected || _utils.isOver(obj)) {
					gc.drawRectangle(area);
				}
			}
		}

		if (_listLayout) {
			int i = 0;
			for (var r : _visibleRenderSelectionFrameAreas) {
				String str = _visibleLabels.get(i);
				if (str != null) {
					var size = gc.stringExtent(str);

					var fg = gc.getForeground();

					{
						var selected = _utils.isSelectedIndex(i);

						if (selected) {
							gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT));
						}

						gc.drawText(str, _frameSize + 20, r.y + r.height / 2 - size.y / 2, true);
					}

					gc.setForeground(fg);
				}
				i++;
			}
		}

	}

	private void buildFilterMap(String text) {
		if (text.trim().length() == 0) {
			_visibleMap = null;
		} else {
			_filter = text;
			_visibleMap = new boolean[_total];

			for (int i = 0; i < _visibleMap.length; i++) {
				_visibleMap[i] = matches(_filter, _labels.get(i));
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

	private void computeRects() {
		if (_images.isEmpty()) {
			return;
		}

		Rectangle b = getClientArea();

		int box = _frameSize + S;

		{

			int x = 0;
			int y = 0;

			_visibleRenderImageDstFrames = new ArrayList<>();

			_visibleRenderImageSrcFrames = new ArrayList<>();
			_visibleObjects = new ArrayList<>();
			_visibleFiles = new ArrayList<>();
			_visibleImages = new ArrayList<>();
			_visibleLabels = new ArrayList<>();

			_visibleCount = 0;

			int maxWidth = b.width;

			if (_listLayout) {
				maxWidth = _frameSize;
			}

			for (int i = 0; i < _total; i++) {
				var src = _renderImageSrcFrames.get(i);

				if (!isVisible(i)) {
					continue;
				}

				ZoomCalculator c = new ZoomCalculator(src.width, src.height);
				c.fit(_frameSize, _frameSize);

				Rectangle dst = new Rectangle(x + (int) c.offsetX, y + (int) c.offsetY, (int) (src.width * c.scale),
						(int) (src.height * c.scale));

				_visibleRenderImageDstFrames.add(dst);
				_visibleRenderImageSrcFrames.add(src);

				x += S + _frameSize;

				if (x + box > maxWidth) {
					y += box;
					x = 0;
				}

				_visibleObjects.add(_objects.get(i));
				_visibleFiles.add(_files.get(i));
				_visibleImages.add(_images.get(i));
				_visibleLabels.add(_labels.get(i));

				_visibleCount++;
			}

			Point min = new Point(Integer.MAX_VALUE, Integer.MAX_VALUE);
			Point max = new Point(Integer.MIN_VALUE, Integer.MIN_VALUE);

			for (Rectangle place : _visibleRenderImageDstFrames) {
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

			for (Rectangle place : _visibleRenderImageDstFrames) {
				place.x += x;
				place.y += y;
			}
		}

		{
			if (_listLayout) {
				_visibleRenderSelectionFrameAreas = new ArrayList<>();
				int i = 0;
				for (var dst : _visibleRenderImageDstFrames) {
					dst.y = i * box;
					var r = new Rectangle(0, dst.y, b.width, box);
					_visibleRenderSelectionFrameAreas.add(r);
					i++;
				}
			} else {
				_visibleRenderSelectionFrameAreas = new ArrayList<>(_visibleRenderImageDstFrames);
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
		int count = _visibleCount * 2;
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
		loadFrameProvider(provider, true);
	}

	public void loadFrameProvider(IFrameProvider provider, boolean resetZoom) {
		resetFramesData();

		_total = provider.getFrameCount();

		for (int i = 0; i < _total; i++) {
			var frame = provider.getFrameRectangle(i);
			var file = provider.getFrameImageFile(i);
			var image = loadImage(file);
			var object = provider.getFrameObject(i);
			var label = provider.getFrameLabel(i);

			_renderImageSrcFrames.add(frame);
			_images.add(image);
			_objects.add(object);
			_files.add(file);
			_labels.add(label);

		}

		_visibleImages = _images;
		_visibleObjects = _objects;
		_visibleFiles = _files;
		_visibleLabels = _labels;
		_visibleCount = _total;

		if (resetZoom) {
			resetZoom();
		} else {
			redraw();
		}
	}

	private void resetFramesData() {
		_renderImageSrcFrames = new ArrayList<>();
		_visibleRenderImageDstFrames = new ArrayList<>();
		_visibleRenderSelectionFrameAreas = new ArrayList<>();

		_images = new ArrayList<>();
		_objects = new ArrayList<>();
		_files = new ArrayList<>();
		_labels = new ArrayList<>();

		_visibleImages = _images;
		_visibleObjects = _objects;
		_visibleFiles = _files;
		_visibleLabels = _labels;
		_visibleCount = 0;

		_nextFilterText = null;
		_visibleMap = null;

		_origin.x = 0;
		getVerticalBar().setSelection(0);
	}

	@Override
	public void mouseScrolled(MouseEvent e) {
		if (_listLayout && (e.stateMask & SWT.SHIFT) == 0) {
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

	public void filter(String text) {
		_nextFilterText = text;
		redraw();
	}

	private static boolean matches(String filter, String label) {
		if (label != null) {
			if (label.toLowerCase().contains(filter.toLowerCase())) {
				return true;
			}
		}
		return false;
	}

	private boolean isVisible(int i) {
		return _visibleMap == null || _visibleMap[i];
	}
}
