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
package phasereditor.animation.ui;

import java.util.List;
import java.util.function.Consumer;

import org.eclipse.core.resources.IFile;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.widgets.Composite;
import org.pushingpixels.trident.Timeline;
import org.pushingpixels.trident.Timeline.RepeatBehavior;
import org.pushingpixels.trident.Timeline.TimelineState;
import org.pushingpixels.trident.callback.RunOnUIThread;
import org.pushingpixels.trident.callback.TimelineCallback;
import org.pushingpixels.trident.callback.TimelineCallbackAdapter;
import org.pushingpixels.trident.ease.Linear;

import phasereditor.assetpack.core.animations.AnimationFrameModel;
import phasereditor.assetpack.core.animations.AnimationModel;
import phasereditor.ui.ImageCanvas;

/**
 * @author arian
 *
 */
public class AnimationCanvas extends ImageCanvas implements ControlListener {

	private AnimationModel _animModel;
	private IndexTimeline _timeline;
	private boolean _showProgress = true;
	protected Runnable _stepCallback;
	protected Consumer<TimelineState> _playbackCallback;
	private TimelineCallback _statusListener;
	private int _currentFrame;

	public AnimationCanvas(Composite parent, int style) {
		super(parent, style);

		addControlListener(this);

		_showProgress = true;
	}

	public void play() {
		if (_timeline != null) {
			_timeline.end();
		}
		startNewAnimation();
	}

	public void stop() {
		if (_timeline != null) {
			_timeline.end();
		}
	}

	public void pause() {
		if (_timeline != null) {
			_timeline.suspend();
		}
	}

	public Runnable getStepCallback() {
		return _stepCallback;
	}

	public void setStepCallback(Runnable stepCallback) {
		_stepCallback = stepCallback;
	}

	public Consumer<TimelineState> getPlaybackCallback() {
		return _playbackCallback;
	}

	public void setPlaybackCallback(Consumer<TimelineState> playbackCallback) {
		_playbackCallback = playbackCallback;
	}

	public IndexTimeline getTimeline() {
		return _timeline;
	}

	public AnimationModel getModel() {
		return _animModel;
	}

	public void setModel(AnimationModel model) {
		setModel(model, true);
	}

	public void setModel(AnimationModel model, boolean autoPlay) {
		_animModel = model;

		if (_timeline != null) {
			_timeline.end();
		}

		if (_animModel == null || _animModel.getFrames().isEmpty()) {
			setImageFile((IFile) null);
			return;
		}

		showFrame(0);

		resetZoom();

		if (autoPlay) {
			startNewAnimation();
		}
	}

	private void startNewAnimation() {
		if (_statusListener == null) {
			_statusListener = new TimelineCallbackAdapter() {
				@Override
				public void onTimelineStateChanged(TimelineState oldState, TimelineState newState,
						float durationFraction, float timelinePosition) {
					if (_playbackCallback != null) {
						_playbackCallback.accept(newState);
					}
				}
			};
		}

		if (_timeline != null) {
			_timeline.removeCallback(_statusListener);
		}

		_animModel.buildTimeline();

		_timeline = new IndexTimeline(_animModel.getComputedTotalDuration());

		_timeline.setInitialDelay(_animModel.getDelay());
		// _transition.setAutoReverse(_animModel.isYoyo());
		// _transition.setCycleCount(_animModel.getRepeat());
		_timeline.addCallback(_statusListener);

		var repeatBehavior = _animModel.isYoyo() ? RepeatBehavior.REVERSE : RepeatBehavior.LOOP;
		_timeline.playLoop(_animModel.getRepeat(), repeatBehavior);
	}

	public void showFrame(int index) {
		var animationFrames = _animModel.getFrames();

		if (index >= animationFrames.size()) {
			return;
		}

		_currentFrame = index;

		if (!isDisposed()) {
			redraw();
		}
	}

	public int getCurrentFrame() {
		return _currentFrame;
	}

	@RunOnUIThread
	public class IndexTimeline extends Timeline implements TimelineCallback {
		private int _currentIndex;
		private float _currentFraction;

		public IndexTimeline(long duration) {
			super(AnimationCanvas.this);

			setDuration(duration);
			setEase(new Linear());

			addCallback(this);

			_currentIndex = -1;
		}

		public int getCurrentIndex() {
			return _currentIndex;
		}

		public float getFraction() {
			return _currentFraction;
		}

		@Override
		public void onTimelineStateChanged(TimelineState oldState, TimelineState newState, float durationFraction,
				float timelinePosition) {
			//
		}

		@Override
		public void onTimelinePulse(float durationFraction, float timelinePosition) {
			_currentFraction = timelinePosition;

			int index = 0;

			AnimationModel animModel = getModel();

			if (animModel == null) {
				return;
			}

			List<AnimationFrameModel> frames = animModel.getFrames();

			for (int i = 0; i < frames.size(); i++) {
				var frame = frames.get(i);
				if (timelinePosition > frame.getComputedFraction()) {
					index = i;
				} else {
					break;
				}
			}

			if (index != _currentIndex) {
				showFrame(index);
				_currentIndex = index;
			}

			if (!isDisposed()) {
				redraw();
				if (_stepCallback != null) {
					_stepCallback.run();
				}
			}
		}
	}

	@Override
	protected void customPaintControl(PaintEvent e) {
		_frameData = null;
		_image = null;
		
		var finder = getModel().getAnimations().createAndBuildFinder();

		var frames = getModel().getFrames();

		if (_currentFrame < frames.size()) {

			var frame = frames.get(_currentFrame);

			var texture = frame.getAssetFrame(finder);

			if (texture == null) {
				_image = null;
				_frameData = null;
			} else {
				_image = loadImage(texture.getImageFile());
				_frameData = texture.getFrameData();
			}

		}

		super.customPaintControl(e);

		if (_showProgress) {

			paintProgressLine(e);
		}
	}

	private void paintProgressLine(PaintEvent e) {
		if (_timeline != null) {

			e.gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_GREEN));
			e.gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_RED));

			e.gc.setLineWidth(3);

			if (_animModel != null) {

				if (_timeline.getState() != TimelineState.IDLE) {
					double frac = _timeline.getFraction();
					int x = (int) (frac * e.width);
					e.gc.drawLine(0, e.height - 5, x, e.height - 5);
				}

				e.gc.setAlpha(110);

				for (var frame : _animModel.getFrames()) {
					var frac = frame.getComputedFraction();
					e.gc.fillOval((int) (frac * e.width) - 3, e.height - 3 - 5, 6, 6);
				}
			}
		}
	}

	public void setShowProgress(boolean showProgress) {
		_showProgress = showProgress;
	}

	public boolean isShowProgress() {
		return _showProgress;
	}

	@Override
	public void controlMoved(ControlEvent e) {
		//
	}

	@Override
	public void controlResized(ControlEvent e) {
		// resetZoom();
	}

	public boolean isStopped() {
		return _timeline == null || _timeline.getState() == TimelineState.IDLE;
	}

	public void playOrPause() {
		if (_animModel == null) {
			return;
		}

		if (_timeline == null) {
			play();
			return;
		}

		switch (_timeline.getState()) {
		case PLAYING_FORWARD:
		case PLAYING_REVERSE:
			_timeline.suspend();
			break;
		case SUSPENDED:
			_timeline.resume();
			break;
		case IDLE:
			play();
			break;
		default:
			break;
		}
	}

}
