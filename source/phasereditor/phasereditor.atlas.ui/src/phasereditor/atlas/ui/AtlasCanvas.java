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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.wb.swt.SWTResourceManager;

import phasereditor.atlas.core.AtlasFrame;
import phasereditor.ui.ImageCanvas;
import phasereditor.ui.PhaserEditorUI;

public class AtlasCanvas extends ImageCanvas implements ControlListener, MouseMoveListener {

	private List<? extends AtlasFrame> _frames;
	private List<Rectangle> _framesRects;
	private AtlasFrame _overFrame;
	private AtlasFrame _frame;

	public AtlasCanvas(Composite parent, int style) {
		super(parent, style);
		addControlListener(this);
		addMouseMoveListener(this);
	}

	@Override
	public void paintControl(PaintEvent e) {
		super.paintControl(e);

		if (_frames != null && _image != null) {
			GC gc = e.gc;

			gc.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
			int i = 0;
			for (Rectangle r : _framesRects) {
				AtlasFrame frame = _frames.get(i);
				if (_frame != null && _frame != frame) {
					gc.setAlpha(140);
					PhaserEditorUI.paintPreviewBackground(gc, r);
				} else if (_frame == frame || _overFrame == frame) {
					gc.setAlpha(255);
					gc.drawRectangle(r);
				}
				i++;
			}
		}
	}

	private void generateFramesRects() {
		List<Rectangle> list = new ArrayList<>();

		if (_frames != null && _image != null) {
			Rectangle img = _image.getBounds();

			Rectangle canvas = getBounds();

			Rectangle zoom = PhaserEditorUI.computeImageZoom(img, canvas);

			for (AtlasFrame item : _frames) {
				double wfactor = zoom.width / (double) img.width;
				double hfactor = zoom.height / (double) img.height;

				int x = zoom.x + (int) (item.getFrameX() * wfactor);
				int y = zoom.y + (int) (item.getFrameY() * hfactor);
				int w = (int) (item.getSpriteW() * wfactor);
				int h = (int) (item.getSpriteH() * hfactor);

				list.add(new Rectangle(x, y, w, h));
			}
		}
		_framesRects = list;
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
		if (_framesRects != null) {
			int i = 0;
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
			redraw();
		}
	}

}
