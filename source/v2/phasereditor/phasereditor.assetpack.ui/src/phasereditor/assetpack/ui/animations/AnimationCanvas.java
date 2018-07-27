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

import org.eclipse.core.resources.IFile;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.widgets.Composite;

import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.embed.swt.FXCanvas;
import javafx.util.Duration;
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
		int size = _animModel.getFrames().size();
		_transition = new IndexTransition(Duration.seconds(size / (double) _animModel.getFrameRate()), size);

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
			setImageViewport(null);
			setImageFile((IFile) null);
		} else {
			var fd = textureFrame.getFrameData();
			_image = loadImage(textureFrame.getImageFile());
			_viewport = fd.src;
		}
		redraw();
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
		resetZoom();
	}

}
