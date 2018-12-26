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
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.widgets.Composite;
import org.pushingpixels.trident.Timeline.TimelineState;

import phasereditor.assetpack.core.animations.AnimationModel;
import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.IEditorSharedImages;
import phasereditor.ui.ImageCanvas_Zoom_1_1_Action;
import phasereditor.ui.ImageCanvas_Zoom_FitWindow_Action;

/**
 * @author arian
 *
 */
public class AnimationPreviewComp extends SashForm {

	private AnimationCanvas _animationCanvas;
	private AnimationTimelineCanvas<AnimationModel> _timelineCanvas;
	private AnimationModel _model;
	private Action _playAction;
	private Action _pauseAction;
	private Action _stopAction;
	private ImageCanvas_Zoom_1_1_Action _zoom_1_1_action;
	private ImageCanvas_Zoom_FitWindow_Action _zoom_fitWindow_action;
	private Action[] _playbackActions;
	private Action _showTimeline;

	public AnimationPreviewComp(Composite parent, int style) {
		super(parent, SWT.VERTICAL | style);

		_animationCanvas = new AnimationCanvas(this, SWT.BORDER);
		_timelineCanvas = new AnimationTimelineCanvas<>(this, SWT.BORDER);
		_timelineCanvas.setAnimationCanvas(_animationCanvas);

		addControlListener(ControlListener.controlResizedAdapter(e -> _animationCanvas.resetZoom()));

		setWeights(new int[] { 2, 1 });

		afterCreateWidgets();

	}

	private void afterCreateWidgets() {

		_animationCanvas.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				var anim = getTimelineCanvas().getModel();

				if (anim != null) {
					getTimelineCanvas().clearSelection();
				}
			}
		});
		_animationCanvas.setStepCallback(_timelineCanvas::redraw);
		_animationCanvas.setPlaybackCallback(this::animationStatusChanged);
		_animationCanvas.addPaintListener(e -> {
			if (_animationCanvas.getModel() != null) {
				e.gc.setAlpha(40);
				e.gc.setForeground(_animationCanvas.getForeground());
				e.gc.drawText(_animationCanvas.getModel().getKey(), 0, 0, true);
			}
		});
	}

	public void setModel(AnimationModel model) {
		_model = model;

		if (_model == null) {
			for (var btn : _playbackActions) {
				btn.setChecked(false);
				btn.setEnabled(false);
			}

			if (_zoom_1_1_action != null) {
				_zoom_1_1_action.setEnabled(false);
				_zoom_fitWindow_action.setEnabled(false);
			}

			_animationCanvas.setModel(null);
			_timelineCanvas.setModel(null);

			return;
		}

		_animationCanvas.setModel(_model, false);

		for (var btn : _playbackActions) {
			btn.setChecked(false);
			btn.setEnabled(btn == _playAction);
		}

		if (_timelineCanvas.getModel() == _model) {
			_timelineCanvas.redraw();
		} else {
			_timelineCanvas.setModel(_model);
		}

		if (_zoom_1_1_action != null) {
			_zoom_1_1_action.setEnabled(true);
			_zoom_fitWindow_action.setEnabled(true);
		}
	}

	public AnimationModel getModel() {
		return _model;
	}

	public AnimationCanvas getAnimationCanvas() {
		return _animationCanvas;
	}

	public AnimationTimelineCanvas<AnimationModel> getTimelineCanvas() {
		return _timelineCanvas;
	}

	private void animationStatusChanged(TimelineState status) {

		out.println("status: " + status);

		switch (status) {
		case PLAYING_FORWARD:
		case PLAYING_REVERSE:
			_playAction.setChecked(true);
			_pauseAction.setChecked(false);
			_stopAction.setChecked(false);
			break;
		case IDLE:
			// TODO: do we really want to do this? it breaks the animation, it looks like
			// the first frame is actually the last frame of the animation.
			//
			// AnimationCanvas animCanvas = getAnimationCanvas();
			// var anim = animCanvas.getModel();
			// var frames = anim.getFrames();
			//
			// if (!frames.isEmpty()) {
			// animCanvas.showFrame(0);
			// }
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

	private void disableToolbar() {
		for (var btn : _playbackActions) {
			btn.setEnabled(false);
		}

		if (_zoom_1_1_action != null) {
			_zoom_1_1_action.setEnabled(false);
			_zoom_fitWindow_action.setEnabled(false);
		}
	}

	public void createToolBar(IToolBarManager manager) {

		_showTimeline = new Action("Timeline",
				EditorSharedImages.getImageDescriptor(IEditorSharedImages.IMG_APPLICATION_SPLIT)) {
			@Override
			public void run() {
				if (getMaximizedControl() == null) {
					setMaximizedControl(getAnimationCanvas());
				} else {
					setMaximizedControl(null);
				}
			}
		};

		_showTimeline.setChecked(true);

		_zoom_1_1_action = new ImageCanvas_Zoom_1_1_Action(_animationCanvas);
		_zoom_fitWindow_action = new ImageCanvas_Zoom_FitWindow_Action(_animationCanvas);

		createPlaybackToolbar(manager);

		manager.add(new Separator());
		manager.add(_showTimeline);
		manager.add(new Separator());
		manager.add(_zoom_1_1_action);
		manager.add(_zoom_fitWindow_action);

		disableToolbar();
	}

	public void createPlaybackToolbar(IToolBarManager manager) {
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
						timeline.play();
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

		manager.add(_playAction);
		manager.add(_pauseAction);
		manager.add(_stopAction);

		_playbackActions = new Action[] { _playAction, _pauseAction, _stopAction };
	}

}
