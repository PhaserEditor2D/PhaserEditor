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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ScrollBar;

import javafx.animation.Animation.Status;
import phasereditor.ui.BaseImageCanvas;

/**
 * @author arian
 *
 */
public class AnimationTimelineCanvas extends BaseImageCanvas implements PaintListener, MouseWheelListener {

	private AnimationModel_in_Editor _animation;
	private AnimationsEditor _editor;
	private double _widthFactor;
	private int _origin;
	private int _fullWidth;
	private boolean _updateScroll;

	public AnimationTimelineCanvas(Composite parent, int style) {
		super(parent, style | SWT.H_SCROLL | SWT.NO_REDRAW_RESIZE);

		_widthFactor = 1;

		addPaintListener(this);
		addMouseWheelListener(this);

		_origin = 0;

		final ScrollBar hBar = getHorizontalBar();
		hBar.addListener(SWT.Selection, e -> {
			_origin = -hBar.getSelection();
			redraw();
		});

		addListener(SWT.Resize, e -> {
			_updateScroll = true;
		});

	}

	void updateScroll() {
		Rectangle client = getClientArea();
		ScrollBar hBar = getHorizontalBar();
		hBar.setMaximum(_fullWidth);
		hBar.setThumb(Math.min(_fullWidth, client.width));
		hBar.setVisible(_fullWidth > client.width);
		int hPage = _fullWidth - client.width;
		int hSelection = hBar.getSelection();
		if (hSelection >= hPage) {
			if (hPage <= 0) {
				hSelection = 0;
			}
			_origin = -hSelection;
		}
	}

	public void setEditor(AnimationsEditor editor) {
		_editor = editor;
	}

	public AnimationsEditor getEditor() {
		return _editor;
	}

	public void setAnimation(AnimationModel_in_Editor animation) {
		_animation = animation;
		redraw();
	}

	public AnimationModel_in_Editor getAnimation() {
		return _animation;
	}

	@Override
	public void paintControl(PaintEvent e) {
		if (_animation == null) {
			return;
		}

		if (_editor != null) {
			var transition = _editor.getAnimationCanvas().getTransition();
			if (transition != null && transition.getStatus() != Status.STOPPED) {
				var frac = transition.getFraction();

				var x = (int) (_fullWidth * frac);

				var hBar = getHorizontalBar();
				var viewX = _origin + x;

				int thumb = hBar.getThumb();
				if (viewX > e.width) {
					var sel = x - thumb + e.width;
					if (sel + thumb > _fullWidth) {
						sel = _fullWidth - thumb;
					}
					hBar.setSelection(sel);
					_origin = -sel;
				} else if (viewX < 0) {
					var sel = x - thumb + e.width;
					if (sel < 0) {
						sel = 0;
					}
					hBar.setSelection(sel);
					_origin = -sel;
				}
			}
		}

		var gc = e.gc;

		Transform tx = new Transform(getDisplay());
		tx.translate(_origin, 0);
		gc.setTransform(tx);

		_fullWidth = (int) (e.width * _widthFactor);

		int margin = 20;

		int imgHeight = e.height - margin * 2;

		for (var animFrame : _animation.getFrames()) {
			var frame = animFrame.getFrameAsset();

			if (frame == null) {
				continue;
			}

			var img = loadImage(frame.getImageFile());
			var src = frame.getFrameData().src;

			var heightFactor = imgHeight / (float) src.height;

			int keyWidth = (int) (src.width * heightFactor);

			int frameX = (int) (animFrame.getComputedFraction() * _fullWidth);

			gc.drawLine(frameX, 0, frameX, e.height);

			int imgX = frameX - keyWidth / 2;

			if (imgX < 0) {
				imgX = 0;
			}

			if (imgHeight > 0) {
				gc.drawImage(img, src.x, src.y, src.width, src.height, imgX, margin, keyWidth, imgHeight);
			}

		}

		if (_editor != null) {
			var transition = _editor.getAnimationCanvas().getTransition();
			if (transition != null && transition.getStatus() != Status.STOPPED) {
				var frac = transition.getFraction();
				var x = (int) (_fullWidth * frac);
				gc.drawLine(x, 0, x, e.height);
			}
		}

		if (_updateScroll) {
			_updateScroll = false;
			updateScroll();
		}

	}

	@Override
	public void mouseScrolled(MouseEvent e) {
		if (e.count < 0) {
			_widthFactor -= 0.2;
		} else {
			_widthFactor += 0.2;
		}

		if (_widthFactor < 0.2) {
			_widthFactor = 0.1;
		}

		_updateScroll = true;

		redraw();
	}
}
