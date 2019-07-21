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

import static java.lang.System.out;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.pushingpixels.trident.Timeline.TimelineState;

import phasereditor.assetpack.core.animations.AnimationModel;
import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.IEditorSharedImages;

/**
 * @author arian
 *
 */
public class AnimationActions {
	private Action _playAction;
	private Action _pauseAction;
	private Action _stopAction;

	private AnimationTimelineCanvas<AnimationModel> _timelineCanvas;
	private AnimationCanvas _animationCanvas;

	public AnimationActions(AnimationCanvas animationCanvas, AnimationTimelineCanvas<AnimationModel> timelineCanvas) {
		_animationCanvas = animationCanvas;
		_timelineCanvas = timelineCanvas;

		createActions();
	}

	public AnimationCanvas getAnimationCanvas() {
		return _animationCanvas;
	}

	public AnimationTimelineCanvas<AnimationModel> getTimelineCanvas() {
		return _timelineCanvas;
	}

	protected void createActions() {

		_playAction = new Action("Play", IAction.AS_CHECK_BOX) {
			{
				setImageDescriptor(EditorSharedImages.getImageDescriptor(IEditorSharedImages.IMG_PLAY));
			}

			@Override
			public void run() {
				AnimationCanvas canvas = getAnimationCanvas();
				var timeline = canvas.getTimeline();
				if (timeline == null) {
					canvas.play();
				} else {
					switch (timeline.getState()) {
					case SUSPENDED:
						timeline.resume();
						break;
					case IDLE:
						canvas.play();
						break;
					default:
						break;
					}
				}

				getTimelineCanvas().redraw();
				canvas.redraw();
			}
		};

		_pauseAction = new Action("Pause", IAction.AS_CHECK_BOX) {
			{
				setImageDescriptor(EditorSharedImages.getImageDescriptor(IEditorSharedImages.IMG_PAUSE));
			}

			@Override
			public void run() {
				getAnimationCanvas().pause();
				getTimelineCanvas().redraw();
				getAnimationCanvas().redraw();
			}

		};

		_stopAction = new Action("Stop", EditorSharedImages.getImageDescriptor(IEditorSharedImages.IMG_STOP)) {

			@Override
			public void run() {
				getAnimationCanvas().stop();
				getTimelineCanvas().redraw();
				getAnimationCanvas().redraw();
			}

		};
	}

	public Action getPlayAction() {
		return _playAction;
	}

	public Action getPauseAction() {
		return _pauseAction;
	}

	public Action getStopAction() {
		return _stopAction;
	}

	public void setEnabled(boolean enabled) {
		_playAction.setEnabled(enabled);
		_pauseAction.setEnabled(enabled);
		_stopAction.setEnabled(enabled);
	}

	public void setChecked(boolean checked) {
		_playAction.setChecked(checked);
		_pauseAction.setChecked(checked);
		_stopAction.setChecked(checked);
	}

	public void animationStatusChanged(TimelineState status) {

		out.println("status: " + status);

		switch (status) {
		case PLAYING_FORWARD:
		case PLAYING_REVERSE:
			_playAction.setChecked(true);
			_pauseAction.setChecked(false);
			_stopAction.setChecked(false);
			break;
		case IDLE:
			_playAction.setChecked(false);
			_pauseAction.setChecked(false);
			break;
		case SUSPENDED:
			_playAction.setChecked(false);
			_pauseAction.setChecked(true);
			break;
		default:
			break;
		}

		_playAction.setEnabled(!_playAction.isChecked());
		_pauseAction.setEnabled(_playAction.isChecked());
		_stopAction.setEnabled(_playAction.isChecked() || _pauseAction.isChecked());
	}

	public void fillToolbar(IToolBarManager manager) {
		manager.add(_playAction);
		manager.add(_pauseAction);
		manager.add(_stopAction);
	}
}
