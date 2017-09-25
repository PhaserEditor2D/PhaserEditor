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
package phasereditor.ui.animations;

import java.util.List;

import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.util.Duration;
import phasereditor.ui.ImageCanvas;

/**
 * @author arian
 *
 */
public class FrameAnimationCanvas extends ImageCanvas implements ControlListener {

	private IFramesAnimationModel _model;
	private IndexTransition _anim;

	public FrameAnimationCanvas(Composite parent, int style) {
		super(parent, style);

		addControlListener(this);
	}

	public void setModel(IFramesAnimationModel model) {
		_model = model;

		if (_anim != null) {
			_anim.stop();
		}

		if (_model == null || _model.getFrames().isEmpty()) {
			setImage(null);
			return;
		}

		setImageFile(_model.getImageFile());
		showFrame(0);
		getDisplay().asyncExec(() -> {
			fitWindow();
			redraw();
		});

		int size = model.getFrames().size();
		_anim = new IndexTransition(Duration.seconds(size / (double) model.getFrameRate()), size);

		if (model.isLoop()) {
			_anim.setCycleCount(Animation.INDEFINITE);
		}
		_anim.play();
	}

	public void showFrame(int index) {
		List<Rectangle> frames = _model.getFrames();
		if (index >= frames.size()) {
			return;
		}

		Rectangle frame = frames.get(index);
		setImageViewport(frame);
	}

	class IndexTransition extends Transition {

		private int _length;
		private int _last;

		public IndexTransition(Duration duration, int length) {
			super();
			setCycleDuration(duration);
			setInterpolator(Interpolator.LINEAR);
			_length = length;
			_last = -1;
		}

		@Override
		protected void interpolate(double frac) {
			int i = (int) (frac * _length);
			if (i != _last) {
				showFrame(i);
				if (!isDisposed()) {
					redraw();
				}
				_last = i;
			}
		}

	}

	@Override
	public void controlMoved(ControlEvent e) {
		//
	}

	@Override
	public void controlResized(ControlEvent e) {
		getDisplay().asyncExec(() -> {
			this.fitWindow();
			redraw();
		});
	}

}
