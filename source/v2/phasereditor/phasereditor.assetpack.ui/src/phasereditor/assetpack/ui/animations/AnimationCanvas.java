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
package phasereditor.assetpack.ui.animations;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.widgets.Composite;

import javafx.animation.Animation.Status;
import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.embed.swt.FXCanvas;
import javafx.util.Duration;
import phasereditor.assetpack.core.animations.AnimationFrameModel;
import phasereditor.assetpack.core.animations.AnimationModel;
import phasereditor.ui.ImageCanvas;

/**
 * @author arian
 *
 */
public class AnimationCanvas extends ImageCanvas implements ControlListener {

	private AnimationModel _animModel;
	private IndexTransition _transition;
	private static boolean _initFX;

	public AnimationCanvas(Composite parent, int style) {
		super(parent, style);

		addControlListener(this);

		if (!_initFX) {
			_initFX = true;
			var temp = new FXCanvas(parent, SWT.NONE);
			temp.dispose();
		}
	}

	public void play() {
		if (_transition != null) {
			_transition.stop();
		}
		createAnimation();
	}

	public void stop() {
		if (_transition != null) {
			_transition.stop();
		}
	}

	public AnimationModel getModel() {
		return _animModel;
	}

	public void setModel(AnimationModel model) {
		_animModel = model;

		if (_transition != null) {
			_transition.stop();
		}

		if (_animModel == null || _animModel.getFrames().isEmpty()) {
			setImageFile((IFile) null);
			return;
		}

		showFrame(0);

		resetZoom();

		createAnimation();
	}

	private void createAnimation() {
		List<AnimationFrameModel> frames = _animModel.getFrames();

		long totalDuration = _animModel.getDuration();

		for (var frame : frames) {
			if (frame.getDuration() > 0) {
				totalDuration += frame.getDuration();
			}
		}

		int size = frames.size();

		double[] fractions = new double[size];

		double time = 0;

		double avgFrameTime = _animModel.getDuration() / size;

		for (int i = 0; i < size; i++) {
			fractions[i] = time / totalDuration;
			time += avgFrameTime + frames.get(i).getDuration();
		}

		_transition = new IndexTransition(Duration.millis(totalDuration), fractions);
		_transition.setDelay(Duration.millis(_animModel.getDelay()));
		_transition.setAutoReverse(_animModel.isYoyo());
		_transition.setCycleCount(_animModel.getRepeat());

		_transition.play();
	}

	public void showFrame(int index) {
		var animationFrames = _animModel.getFrames();

		if (index >= animationFrames.size()) {
			return;
		}

		var animationFrame = animationFrames.get(index);
		var textureFrame = animationFrame.getFrame();
		if (textureFrame == null) {
			_viewport = null;
			_image = null;
		} else {
			var fd = textureFrame.getFrameData();
			_image = loadImage(textureFrame.getImageFile());
			_viewport = fd.src;
		}

		if (!isDisposed()) {
			redraw();
		}
	}

	class IndexTransition extends Transition {

		private int _currentIndex;
		private double[] _fractions;

		public IndexTransition(Duration duration, double[] fractions) {
			super();
			setCycleDuration(duration);
			setInterpolator(Interpolator.LINEAR);
			_fractions = fractions;
			_currentIndex = -1;
		}

		@Override
		protected void interpolate(double frac) {
			int index = 0;

			for (int i = 0; i < _fractions.length; i++) {
				if (frac > _fractions[i]) {
					index = i;
				} else {
					break;
				}
			}

			if (index != _currentIndex) {
				showFrame(index);
				if (!isDisposed()) {
					redraw();
				}
				_currentIndex = index;
			}
		}

		public int getCurrentIndex() {
			return _currentIndex;
		}

	}

	@Override
	protected void customPaintControl(PaintEvent e) {
		super.customPaintControl(e);

		if (_animModel != null && _transition != null && _transition.getStatus() != Status.STOPPED) {
			int i = _transition.getCurrentIndex() + 1;
			if (i > 0 && !_animModel.getFrames().isEmpty()) {
				double frac = i / (double) _animModel.getFrames().size();
				int x = (int) (frac * e.width);
				e.gc.drawLine(0, e.height - 2, x, e.height - 2);
			}
		}
	}

	@Override
	public void controlMoved(ControlEvent e) {
		//
	}

	@Override
	public void controlResized(ControlEvent e) {
		resetZoom();
	}

}
