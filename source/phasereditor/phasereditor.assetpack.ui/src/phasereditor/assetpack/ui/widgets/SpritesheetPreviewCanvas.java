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
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;

import phasereditor.assetpack.core.SpritesheetAssetModel;
import phasereditor.assetpack.ui.AssetPackUI;
import phasereditor.assetpack.ui.AssetPackUI.FrameData;
import phasereditor.ui.ImageCanvas;
import phasereditor.ui.PhaserEditorUI;

public class SpritesheetPreviewCanvas extends ImageCanvas {

	private SpritesheetAssetModel _spritesheet;
	private int _frame;
	private boolean _singleFrame;

	public SpritesheetPreviewCanvas(Composite parent, int style) {
		super(parent, style);
		setPreferredSize(new Point(100, 100));
		_singleFrame = false;
	}

	@Override
	protected void drawImage(GC gc, int srcX, int srcY, int srcW, int srcH,
			int dstW, int dstH, int dstX, int dstY) {
		if (!_singleFrame) {
			super.drawImage(gc, srcX, srcY, srcW, srcH, dstW, dstH, dstX, dstY);
		}
	}

	@Override
	protected void drawBorder(GC gc, Rectangle rect) {
		if (_singleFrame) {
			Rectangle r = new Rectangle(0, 0, _spritesheet.getFrameWidth(),
					_spritesheet.getFrameHeight());
			r = PhaserEditorUI.computeImageZoom(r, getBounds());
			super.drawBorder(gc, r);
		} else {
			super.drawBorder(gc, rect);
		}
	}

	@Override
	protected void drawMore(GC gc, int srcW, int srcH, int dstW, int dstH,
			int dstX, int dstY) {
		SpritesheetAssetModel spritesheet = _spritesheet;
		if (spritesheet != null) {
			Rectangle canvasBounds = getBounds();
			List<FrameData> list;
			Rectangle imgBounds = _image.getBounds();
			if (_singleFrame) {
				Rectangle dst = PhaserEditorUI.computeImageZoom(imgBounds,
						canvasBounds);
				list = AssetPackUI.generateSpriteSheetRects(spritesheet,
						imgBounds, dst);
				if (list.isEmpty()) {
					PhaserEditorUI.paintPreviewMessage(gc, canvasBounds,
							"Cannot compute the grid.");
				} else {
					FrameData fd = list.get(_frame % list.size());

					int x = canvasBounds.width / 2 - fd.dst.width / 2;
					int y = canvasBounds.height / 2 - fd.dst.height / 2;

					try {
						gc.drawImage(_image, fd.src.x, fd.src.y, fd.src.width,
								fd.src.height, x, y, fd.dst.width,
								fd.dst.height);
					} catch (IllegalArgumentException e) {
						// wrong parameters
					}
				}
			} else {
				Rectangle dst = PhaserEditorUI.computeImageZoom(imgBounds,
						canvasBounds);
				list = AssetPackUI.generateSpriteSheetRects(spritesheet,
						imgBounds, dst);
				if (list.isEmpty()) {
					PhaserEditorUI.paintPreviewMessage(gc, canvasBounds,
							"Cannot compute the grid.");
				} else {
					gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_RED));
					for (FrameData fd : list) {
						Rectangle r = fd.dst;
						gc.drawRectangle(r.x, r.y, r.width, r.height);
					}
				}
			}
		}
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

		List<FrameData> list = AssetPackUI.generateSpriteSheetRects(
				_spritesheet, _image.getBounds(), getBounds());
		return list.size();
	}

}
