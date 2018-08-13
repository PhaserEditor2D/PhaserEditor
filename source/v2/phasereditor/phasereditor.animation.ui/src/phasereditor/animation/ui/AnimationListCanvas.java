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
package phasereditor.animation.ui;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.wb.swt.SWTResourceManager;

import phasereditor.assetpack.core.IAssetFrameModel;
import phasereditor.assetpack.core.animations.AnimationFrameModel;
import phasereditor.assetpack.core.animations.AnimationModel;
import phasereditor.assetpack.core.animations.AnimationsModel;
import phasereditor.ui.BaseImageCanvas;
import phasereditor.ui.FrameCanvasUtils;
import phasereditor.ui.FrameData;
import phasereditor.ui.PhaserEditorUI;

/**
 * @author arian
 *
 */
public class AnimationListCanvas<T extends AnimationsModel> extends BaseImageCanvas
		implements PaintListener, MouseWheelListener {

	private T _model;
	private int _rowHeight;
	private FrameCanvasUtils _utils;
	protected List<AnimationModel> _animations;
	private String _nextFilterText;

	public AnimationListCanvas(Composite parent, int style) {
		super(parent, style | SWT.V_SCROLL);

		// TODO: move this to the preferences page
		_rowHeight = 32;
		_animations = List.of();

		addPaintListener(this);
		addMouseWheelListener(this);

		_origin = new Point(0, 0);

		final ScrollBar vBar = getVerticalBar();
		vBar.addListener(SWT.Selection, e -> {
			_origin.y = -vBar.getSelection();
			redraw();
		});
		addListener(SWT.Resize, e -> {
			updateScroll();
		});

		_utils = new FrameCanvasUtils(this, true) {

			@Override
			public Rectangle getSelectionFrameArea(int index) {
				Rectangle b = getBounds();
				return new Rectangle(0, index * getRowHeight(), b.width, getRowHeight());
			}

			@Override
			public Rectangle getRenderImageSrcFrame(int index) {
				IAssetFrameModel asset = findAssetFor(index);

				if (asset == null) {
					return null;
				}

				return asset.getFrameData().src;
			}

			@SuppressWarnings("synthetic-access")
			private IAssetFrameModel findAssetFor(int index) {
				IAssetFrameModel frame = null;
				Image image = null;
				for (var f : _animations.get(index).getFrames()) {
					frame = f.getFrameAsset();
					if (frame != null) {
						image = loadImage(frame.getImageFile());
						if (image != null) {
							return frame;
						}
					}
				}
				return null;
			}

			@Override
			public Point viewToModel(int x, int y) {
				return new Point(x, y - _origin.y);
			}

			@Override
			public Point modelToView(int x, int y) {
				return new Point(x, y + _origin.y);
			}
			
			@Override
			public IFile getImageFile(int index) {
				IAssetFrameModel asset = findAssetFor(index);

				if (asset == null) {
					return null;
				}

				return asset.getImageFile();
			}

			@Override
			public int getFramesCount() {
				return _animations.size();
			}

			@Override
			public Object getFrameObject(int index) {
				return _animations.get(index);
			}
		};
	}

	public FrameCanvasUtils getUtils() {
		return _utils;
	}

	public void selectAll() {
		_utils.selectAll();
	}

	@SuppressWarnings("null")
	@Override
	public void paintControl(PaintEvent e) {

		if (_nextFilterText != null) {
			getVerticalBar().setSelection(0);
			_origin.y = 0;
			buildFilterMap(_nextFilterText);
			_nextFilterText = null;

			updateScroll();
		}

		var gc = e.gc;

		var tx = new Transform(getDisplay());
		tx.translate(0, _origin.y);
		gc.setTransform(tx);

		Font boldFont = SWTResourceManager.getBoldFont(getFont());

		int textHeight = gc.stringExtent("M").y;

		int y = 0;

		for (int i = 0; i < _animations.size(); i++) {
			var anim = _animations.get(i);

			if (_utils.isSelectedIndex(i)) {
				gc.setBackground(PhaserEditorUI.getListSelectionColor());
				gc.setForeground(PhaserEditorUI.getListSelectionTextColor());
				gc.fillRectangle(0, y, e.width, _rowHeight);
			} else {
				gc.setForeground(getForeground());
			}

			PhaserEditorUI.paintListItemBackground(gc, i, 0, _rowHeight * i, e.width, _rowHeight);

			int textOffset = textHeight + 5;
			int imgHeight = _rowHeight - textOffset - 10;

			if (imgHeight >= 32) {
				List<AnimationFrameModel> frames = anim.getFrames();

				for (int j = 0; j < frames.size(); j++) {
					var frame = frames.get(j);

					IAssetFrameModel asset = frame.getFrameAsset();

					if (asset != null) {
						var file = asset.getImageFile();
						var fd = asset.getFrameData();
						fd = adaptFrameData(fd);

						var img = loadImage(file);
						if (img != null) {
							Rectangle area = new Rectangle(j * imgHeight, y + 5, imgHeight, imgHeight);

							if (area.x > e.width) {
								break;
							}

							PhaserEditorUI.paintScaledImageInArea(gc, img, fd, area);
						}
					}
				}

				gc.setFont(boldFont);
				gc.drawText(anim.getKey(), 5, y + _rowHeight - textOffset, true);
			} else {
				IAssetFrameModel frame = null;
				Image image = null;
				for (var f : anim.getFrames()) {
					frame = f.getFrameAsset();
					if (frame != null) {
						image = loadImage(frame.getImageFile());
						if (image != null) {
							break;
						}
					}
				}

				if (image != null) {
					var area = new Rectangle(2, y + 2, _rowHeight - 4, _rowHeight - 4);
					var fd = adaptFrameData(frame.getFrameData());
					PhaserEditorUI.paintScaledImageInArea(gc, image, fd, area);
				}

				gc.setFont(getFont());
				gc.drawText(anim.getKey(), _rowHeight + 5, y + _rowHeight / 2 - textHeight / 2, true);
			}

			y += _rowHeight;
		}
	}

	private void buildFilterMap(String patten) {
		_animations = getModel().getAnimations().stream()
				.filter(a -> a.getKey().toLowerCase().contains(patten.toLowerCase())).collect(toList());
	}

	public void filter(String pattern) {
		_nextFilterText = pattern;
		redraw();
	}

	private static FrameData adaptFrameData(FrameData fd1) {
		var fd = fd1.clone();
		fd.srcSize.x = fd.src.width;
		fd.srcSize.y = fd.src.height;
		fd.dst = new Rectangle(0, 0, fd.src.width, fd.src.height);
		return fd;
	}

	public int getRowHeight() {
		return _rowHeight;
	}

	public void setRowHeight(int rowHeight) {
		_rowHeight = rowHeight;
	}

	public T getModel() {
		return _model;
	}

	public void setModel(T model) {
		_model = model;
		refresh();
	}

	public void refresh() {
		if (_model == null) {
			_animations = new ArrayList<>();
		} else {
			_animations = new ArrayList<>(_model.getAnimations());
		}
		
		
		updateScroll();
		
		redraw();
	}

	@Override
	public void mouseScrolled(MouseEvent e) {
		if ((e.stateMask & SWT.SHIFT) == 0) {
			return;
		}

		double f = e.count < 0 ? 0.8 : 1.2;
		_rowHeight = (int) (_rowHeight * f);
		if (_rowHeight < 16) {
			_rowHeight = 16;
		}

		updateScroll();

		redraw();
	}

	protected Point _origin;

	void updateScroll() {
		ScrollBar vBar = getVerticalBar();
		
		if (_animations.isEmpty()) {
			vBar.setMaximum(0);
			vBar.setSelection(0);
			vBar.setThumb(0);
			_origin.y = 0;
			return;
		}
		
		var b = getBounds();
		b.height = _rowHeight * _animations.size();
		Rectangle rect = b;
		Rectangle client = getClientArea();
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

}
