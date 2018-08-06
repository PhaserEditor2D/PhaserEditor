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
import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.widgets.Composite;

import javafx.animation.Animation.Status;
import phasereditor.animation.ui.AnimationCanvas.IndexTransition;
import phasereditor.assetpack.core.animations.AnimationModel;
import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.IEditorSharedImages;
import phasereditor.ui.ImageCanvas_Zoom_1_1_Action;
import phasereditor.ui.ImageCanvas_Zoom_FitWindow_Action;

/**
 * @author arian
 *
 */
public class AnimationWithTimelineComp extends SashForm {

	private AnimationCanvas _animationCanvas;
	private AnimationTimelineCanvas _timelineCanvas;
	private AnimationModel _model;
	private Action _playAction;
	private Action _pauseAction;
	private Action _stopAction;
	private ImageCanvas_Zoom_1_1_Action _zoom_1_1_action;
	private ImageCanvas_Zoom_FitWindow_Action _zoom_fitWindow_action;
	private Action[] _playbackActions;

	public AnimationWithTimelineComp(Composite parent, int style) {
		super(parent, SWT.VERTICAL | style);

		_animationCanvas = new AnimationCanvas(this, SWT.NONE);
		_timelineCanvas = new AnimationTimelineCanvas(this, SWT.NONE);
		_timelineCanvas.setAnimationCanvas(_animationCanvas);

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
		_animationCanvas.setNoImageMessage("");
		_animationCanvas.setStepCallback(_timelineCanvas::redraw);
		_animationCanvas.setPlaybackCallback(this::animationStatusChanged);
		_animationCanvas.addPaintListener(e -> {
			if (_animationCanvas.getModel() != null) {
				e.gc.setForeground(JFaceResources.getColorRegistry().get(JFacePreferences.DECORATIONS_COLOR));
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

			_zoom_1_1_action.setEnabled(false);
			_zoom_fitWindow_action.setEnabled(false);

			_animationCanvas.setModel(null);
			_timelineCanvas.setModel(null);

			return;
		}

		_animationCanvas.setModel(_model, false);

		for (var btn : _playbackActions) {
			btn.setChecked(false);
			btn.setEnabled(btn == _playAction);
		}

		if (_timelineCanvas.getModel() != _model) {
			_timelineCanvas.setModel(_model);
		}

		_zoom_1_1_action.setEnabled(true);
		_zoom_fitWindow_action.setEnabled(true);
		
		
		_animationCanvas.setModel(model);
		_timelineCanvas.setModel(model);
	}

	public AnimationModel getModel() {
		return _model;
	}

	public AnimationCanvas getAnimationCanvas() {
		return _animationCanvas;
	}

	public AnimationTimelineCanvas getTimelineCanvas() {
		return _timelineCanvas;
	}

	private void animationStatusChanged(Status status) {

		out.println("status: " + status);

		switch (status) {
		case RUNNING:
			_playAction.setChecked(true);
			_pauseAction.setChecked(false);
			_stopAction.setChecked(false);
			break;
		case STOPPED:
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
		case PAUSED:
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

		_zoom_1_1_action.setEnabled(false);
		_zoom_fitWindow_action.setEnabled(false);
	}
	
	public void createToolBar(IToolBarManager manager) {

		_playAction = new Action("Play", IAction.AS_CHECK_BOX) {
			{
				setImageDescriptor(EditorSharedImages.getImageDescriptor(IEditorSharedImages.IMG_PLAY));
			}

			@Override
			public void run() {
				AnimationCanvas canvas = getAnimationCanvas();
				IndexTransition transition = canvas.getTransition();
				if (transition == null) {
					canvas.play();
				} else {
					switch (transition.getStatus()) {
					case PAUSED:
						transition.play();
						break;
					case STOPPED:
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

		
		_zoom_1_1_action = new ImageCanvas_Zoom_1_1_Action(_animationCanvas);
		_zoom_fitWindow_action = new ImageCanvas_Zoom_FitWindow_Action(_animationCanvas);

		manager.add(_playAction);
		manager.add(_pauseAction);
		manager.add(_stopAction);
		manager.add(new Separator());
		manager.add(_zoom_1_1_action);
		manager.add(_zoom_fitWindow_action);

		_playbackActions = new Action[] { _playAction, _pauseAction, _stopAction };
		
		disableToolbar();
	}

}
