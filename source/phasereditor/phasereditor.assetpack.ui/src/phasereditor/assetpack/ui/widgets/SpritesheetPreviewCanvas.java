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
package phasereditor.assetpack.ui.widgets;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;

import phasereditor.assetpack.core.SpritesheetAssetModel;
import phasereditor.assetpack.core.SpritesheetAssetModel.FrameModel;
import phasereditor.assetpack.ui.AssetPackUI;
import phasereditor.assetpack.ui.AssetPackUI.FrameData;
import phasereditor.ui.ImageCanvas;
import phasereditor.ui.PhaserEditorUI;

public class SpritesheetPreviewCanvas extends ImageCanvas implements MouseMoveListener {

	private SpritesheetAssetModel _spritesheet;
	private int _frame;
	private boolean _singleFrame;
	private FrameModel _over;
	private List<FrameData> _rects;

	public SpritesheetPreviewCanvas(Composite parent, int style) {
		super(parent, style);
		setPreferredSize(new Point(100, 100));
		_singleFrame = false;
		addMouseMoveListener(this);
	}

	@Override
	protected void drawImage(GC gc, int srcX, int srcY, int srcW, int srcH, int dstW, int dstH, int dstX, int dstY) {
		if (!_singleFrame) {
			super.drawImage(gc, srcX, srcY, srcW, srcH, dstW, dstH, dstX, dstY);
		}
	}

	@Override
	protected void drawBorder(GC gc, Rectangle rect) {
		if (_singleFrame) {
			Rectangle r = new Rectangle(0, 0, _spritesheet.getFrameWidth(), _spritesheet.getFrameHeight());
			r = PhaserEditorUI.computeImageZoom(r, getBounds());
			super.drawBorder(gc, r);
		}
		// do not draw border when it shows the whole tape
		// else {
		// super.drawBorder(gc, rect);
		// }
	}

	@Override
	protected void drawMore(GC gc, int srcW, int srcH, int dstW, int dstH, int dstX, int dstY) {
		if (_spritesheet == null) {
			return;
		}

		SpritesheetAssetModel spritesheet = _spritesheet;
		Rectangle canvasBounds = getBounds();
		Rectangle imgBounds = _image.getBounds();
		if (_singleFrame) {
			Rectangle dst = PhaserEditorUI.computeImageZoom(imgBounds, canvasBounds);
			_rects = AssetPackUI.generateSpriteSheetRects(spritesheet, imgBounds, dst);
			if (_rects.isEmpty()) {
				PhaserEditorUI.paintPreviewMessage(gc, canvasBounds, "Cannot compute the grid.");
			} else {
				FrameData fd = _rects.get(_frame % _rects.size());

				Rectangle r = new Rectangle(0, 0, fd.src.width, fd.src.height);
				r = PhaserEditorUI.computeImageZoom(r, getBounds());

				try {
					gc.drawImage(_image, fd.src.x, fd.src.y, fd.src.width, fd.src.height, r.x, r.y, r.width, r.height);
				} catch (IllegalArgumentException e) {
					// wrong parameters
				}
			}
		} else {
			Rectangle dst = PhaserEditorUI.computeImageZoom(imgBounds, canvasBounds);
			_rects = AssetPackUI.generateSpriteSheetRects(spritesheet, imgBounds, dst);
			if (_rects.isEmpty()) {
				PhaserEditorUI.paintPreviewMessage(gc, canvasBounds, "Cannot compute the grid.");
			} else {
				int i = 0;
				for (FrameData fd : _rects) {
					gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_RED));
					Rectangle r = fd.dst;
					gc.drawRectangle(r.x, r.y, r.width, r.height);
					String label = Integer.toString(i);
					Point labelRect = gc.stringExtent(Integer.toString(i));
					int left = r.x + r.width / 2 - labelRect.x / 2;
					int top = Math.min(r.y + r.height + 5, getBounds().height - labelRect.y - 5);
					gc.setAlpha(200);
					gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_WHITE));
					gc.fillRectangle(left - 2, top, labelRect.x + 4, labelRect.y);
					gc.setAlpha(255);
					gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_BLACK));
					gc.drawString(label, left, top, true);
					i++;
				}
			}
		}
	}

	@Override
	public void mouseMove(MouseEvent e) {
		_over = null;

		if (_singleFrame) {
			_over = _spritesheet.getFrames().get(_frame);
			return;
		}

		if (_rects == null || _rects.isEmpty()) {
			return;
		}

		int i = 0;
		for (FrameData rect : _rects) {
			if (rect.dst.contains(e.x, e.y)) {
				_over = _spritesheet.getFrames().get(i);
				return;
			}
			i++;
		}

	}

	public SpritesheetAssetModel.FrameModel getOverFrame() {
		return _over;
	}

	public SpritesheetAssetModel getSpritesheet() {
		return _spritesheet;
	}

	public void setSpritesheet(SpritesheetAssetModel spritesheet) {
		_spritesheet = spritesheet;
	}

	public boolean isSingleFrame() {
		return _singleFrame;
	}

	public void setSingleFrame(boolean singleFrame) {
		_singleFrame = singleFrame;
	}

	public int getFrame() {
		return _frame;
	}

	public void setFrame(int frame) {
		_frame = frame;
	}

	public int getFrameCount() {
		int max = _spritesheet.getFrameMax();
		if (max >= 0) {
			return max;
		}

		if (_image == null) {
			return 0;
		}

		List<FrameData> list = AssetPackUI.generateSpriteSheetRects(_spritesheet, _image.getBounds(), getBounds());
		return list.size();
	}

}
