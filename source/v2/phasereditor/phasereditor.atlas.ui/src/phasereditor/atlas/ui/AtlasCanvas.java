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
package phasereditor.atlas.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;

import phasereditor.atlas.core.AtlasFrame;
import phasereditor.ui.ImageCanvas;
import phasereditor.ui.PhaserEditorUI;

public class AtlasCanvas extends ImageCanvas implements ControlListener, MouseMoveListener {

	private List<? extends AtlasFrame> _frames;
	private List<Rectangle> _framesRects;
	private AtlasFrame _overFrame;
	private AtlasFrame _frame;
	private boolean _singleFrame;
	private List<String> _tooltips;

	public AtlasCanvas(Composite parent, int style) {
		super(parent, style);
		addControlListener(this);
		addMouseMoveListener(this);
	}

	@Override
	protected void drawImage(GC gc, int srcX, int srcY, int srcW, int srcH, int dstW, int dstH, int dstX, int dstY) {
		if (_frame != null) {
			PhaserEditorUI.paintPreviewBackground(gc, new Rectangle(dstX, dstY, dstW, dstH));
			gc.setAlpha(100);
			super.drawImage(gc, srcX, srcY, srcW, srcH, dstW, dstH, dstX, dstY);
			gc.setAlpha(255);
			return;
		}

		super.drawImage(gc, srcX, srcY, srcW, srcH, dstW, dstH, dstX, dstY);
		
//		gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
//		gc.drawRectangle(dstX, dstY, dstW, dstH);
	}

	@Override
	public void paintControl(PaintEvent e) {

		generateFramesRects();

		if (_singleFrame && _frame != null) {
			paintSingleFrame(e);
			return;
		}

		super.paintControl(e);

		if (_frames != null && _image != null) {
			GC gc = e.gc;
			
			Color overColor = PhaserEditorUI.get_pref_Preview_Atlas_frameOverColor();
			
			gc.setForeground(overColor);
			
			int i = 0;
			ZoomCalculator calc = calc();
			for (Rectangle r : _framesRects) {
				AtlasFrame frame = _frames.get(i);
				if (frame == _frame) {
					gc.setClipping(r);
					Rectangle src = _image.getBounds();
					Rectangle dst = calc.imageToScreen(src);
					gc.drawImage(_image, src.x, src.y, src.width, src.height, dst.x, dst.y, dst.width, dst.height);
					gc.setClipping((Rectangle) null);
					gc.drawRectangle(r);

				}

				if (frame == _frame || frame == _overFrame) {
					gc.drawRectangle(r);
				}

				i++;
			}
		}
	}

	private void paintSingleFrame(PaintEvent e) {
		GC gc = e.gc;

		if (_image == null) {
			PhaserEditorUI.paintPreviewMessage(gc, getBounds(), getNoImageMessage());
		} else {
			Rectangle src = new Rectangle(_frame.getFrameX(), _frame.getFrameY(), _frame.getFrameW(),
					_frame.getFrameH());
			ZoomCalculator calc = calc();
			Rectangle z = calc.imageToScreen(0, 0, src.width, src.height);
			PhaserEditorUI.paintPreviewBackground(gc, z);
			gc.drawImage(_image, src.x, src.y, src.width, src.height, z.x, z.y, z.width, z.height);
		}
	}

	private void generateFramesRects() {
		List<Rectangle> list = new ArrayList<>();

		if (_frames != null && _image != null) {
			ZoomCalculator calc = calc();

			for (AtlasFrame item : _frames) {
				Rectangle r = calc.imageToScreen(item.getFrameX(), item.getFrameY(), item.getSpriteW(),
						item.getSpriteH());
				list.add(r);
			}
		}
		_framesRects = list;
	}

	@Override
	public void fitWindow() {
		if (_singleFrame && _frame != null) {
			if (_image == null) {
				return;
			}

			ZoomCalculator calc = calc();
			calc.imgWidth = _frame.getFrameW();
			calc.imgHeight = _frame.getFrameH();
			calc.fit(getBounds());

			setScaleAndOffset(calc);
		} else {
			super.fitWindow();
		}
	}

	public void setFrames(List<? extends AtlasFrame> frames) {
		_frames = frames;
		generateFramesRects();
	}

	public List<? extends AtlasFrame> getFrames() {
		return _frames;
	}

	public AtlasFrame getOverFrame() {
		return _overFrame;
	}

	public AtlasFrame getFrame() {
		return _frame;
	}

	public void setFrame(AtlasFrame frame) {
		_frame = frame;
	}

	public void setSingleFrame(boolean singleFrame) {
		_singleFrame = singleFrame;
	}

	public boolean isSingleFrame() {
		return _singleFrame;
	}

	@Override
	public void controlMoved(ControlEvent e) {
		generateFramesRects();
	}

	@Override
	public void controlResized(ControlEvent e) {
		generateFramesRects();
	}

	@Override
	public void mouseMove(MouseEvent e) {
		AtlasFrame frame = null;
		int i = 0;
		if (_framesRects != null) {
			for (Rectangle r : _framesRects) {
				if (r.contains(e.x, e.y)) {
					frame = _frames.get(i);
					break;
				}
				i++;
			}
		}
		if (frame != _overFrame) {
			_overFrame = frame;
			if (_tooltips != null && frame != null) {
				setToolTipText(_tooltips.get(i));
			}
			redraw();
		}
	}

	public void setTooltips(List<String> tooltips) {
		_tooltips = tooltips;
	}

}
