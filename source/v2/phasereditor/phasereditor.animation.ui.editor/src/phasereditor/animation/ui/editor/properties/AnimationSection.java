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
package phasereditor.animation.ui.editor.properties;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import phasereditor.animation.ui.editor.AnimationsEditor;
import phasereditor.assetpack.core.animations.AnimationModel;
import phasereditor.inspect.core.InspectCore;
import phasereditor.ui.properties.CheckListener;
import phasereditor.ui.properties.TextListener;
import phasereditor.ui.properties.TextToFloatListener;
import phasereditor.ui.properties.TextToIntListener;

/**
 * @author arian
 *
 */
public class AnimationSection extends BaseAnimationSection<AnimationModel> {

	private Label _computedDurationLabel;
	private Text _durationText;
	private Text _keyText;
	private Text _frameRateText;
	private Text _delayText;
	private Text _repeatText;
	private Button _showOnStartBtn;
	private Button _hideOnCompleteBtn;
	private Button _skipMissedFramesBtn;
	private Text _repeatDelayText;
	private Button _yoyoCheckBox;

	public AnimationSection(AnimationsEditor editor) {
		super(editor, "Animation");
	}

	@Override
	public boolean canEdit(Object obj) {
		return obj instanceof AnimationModel;
	}

	@SuppressWarnings({ "unused" })
	@Override
	public Control createContent(Composite parent) {

		var comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new GridLayout(2, false));

		// key

		{
			var label = new Label(comp, SWT.NONE);
			label.setText("Key");
			label.setToolTipText(InspectCore.getPhaserHelp().getMemberHelp("AnimationConfig.key"));

			_keyText = new Text(comp, SWT.BORDER);
			_keyText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			new TextListener(_keyText) {

				@Override
				protected void accept(String value) {
					getModels().forEach(model -> {

						model.setKey(value);

						model.buildTimeline();

					});

					var editor = getEditor();
					editor.refreshOutline();
					editor.getTimelineCanvas().redraw();
					editor.setDirty();
				}
			};

			var sep = new Label(comp, SWT.SEPARATOR | SWT.HORIZONTAL);
			var gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
			gd.horizontalSpan = 2;
			sep.setLayoutData(gd);

		}

		// frameRate

		{
			var label = new Label(comp, SWT.NONE);
			label.setText("Frame Rate");
			label.setToolTipText(InspectCore.getPhaserHelp().getMemberHelp("AnimationConfig.frameRate"));

			_frameRateText = new Text(comp, SWT.BORDER);
			_frameRateText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			new TextToFloatListener(_frameRateText) {

				@Override
				protected void accept(float value) {
					getModels().forEach(model -> {
						model.setFrameRate(value);
						model.buildTimeline();
					});

					var editor = getEditor();

					editor.getTimelineCanvas().redraw();
					restartPlayback();
					editor.setDirty();

					update_UI_from_Model();
				}
			};
		}

		// duration

		{
			var label = new Label(comp, SWT.NONE);
			label.setText("Duration");
			label.setToolTipText(InspectCore.getPhaserHelp().getMemberHelp("AnimationFrameConfig.duration"));

			_durationText = new Text(comp, SWT.BORDER);
			_durationText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

			new Label(comp, SWT.NONE);
			_computedDurationLabel = new Label(comp, SWT.NONE);
			_computedDurationLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			_computedDurationLabel.setToolTipText(
					"A computed duration based on the duration plus all the extra frame's durations.\\nNOTE: This is not part of the Phaser API.");
			new TextToIntListener(_durationText) {

				@SuppressWarnings("synthetic-access")
				@Override
				protected void accept(int value) {
					getModels().forEach(model -> {
						model.setDuration(value);
						model.buildTimeline();
					});

					updateTotalDuration();

					var editor = getEditor();

					editor.getTimelineCanvas().redraw();
					restartPlayback();
					editor.setDirty();

					update_UI_from_Model();

				}
			};

		}

		// delay

		{
			var label = new Label(comp, SWT.NONE);
			label.setText("Delay");
			label.setToolTipText(InspectCore.getPhaserHelp().getMemberHelp("AnimationConfig.delay"));

			_delayText = new Text(comp, SWT.BORDER);
			_delayText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

			new TextToIntListener(_delayText) {

				@SuppressWarnings("synthetic-access")
				@Override
				protected void accept(int value) {
					getModels().forEach(model -> {
						model.setDelay(value);
						model.buildTimeline();
					});

					var editor = getEditor();
					editor.getTimelineCanvas().redraw();
					restartPlayback();
					editor.setDirty();

					updateTotalDuration();
				}
			};
		}

		// repeat

		{
			var label = new Label(comp, SWT.NONE);
			label.setText("Repeat");
			label.setToolTipText(InspectCore.getPhaserHelp().getMemberHelp("AnimationConfig.repeat"));

			_repeatText = new Text(comp, SWT.BORDER);
			_repeatText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			new TextToIntListener(_repeatText) {

				@Override
				protected void accept(int value) {
					getModels().forEach(model -> {
						model.setRepeat(value);
						model.buildTimeline();
					});

					var editor = getEditor();
					editor.getTimelineCanvas().redraw();
					restartPlayback();
					editor.setDirty();

				}
			};
		}

		// repeatDelay

		{
			var label = new Label(comp, SWT.NONE);
			label.setText("Repeat Delay");
			label.setToolTipText(InspectCore.getPhaserHelp().getMemberHelp("AnimationConfig.repeatDelay"));

			_repeatDelayText = new Text(comp, SWT.BORDER);
			_repeatDelayText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			new TextToIntListener(_repeatDelayText) {

				@Override
				protected void accept(int value) {
					getModels().forEach(model -> {
						model.setRepeatDelay(value);
					});

					restartPlayback();
					getEditor().setDirty();

				}
			};
		}

		// yoyo

		{
			_yoyoCheckBox = new Button(comp, SWT.CHECK);
			_yoyoCheckBox.setText("Yoyo");
			_yoyoCheckBox.setToolTipText(InspectCore.getPhaserHelp().getMemberHelp("AnimationConfig.repeatDelay"));
			_yoyoCheckBox.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
			new CheckListener(_yoyoCheckBox) {

				@Override
				protected void accept(boolean value) {
					getModels().forEach(model -> {
						model.setYoyo(value);
					});

					restartPlayback();
					getEditor().setDirty();
				}
			};
		}

		{
			var sep = new Label(comp, SWT.SEPARATOR | SWT.HORIZONTAL);
			var gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
			gd.horizontalSpan = 2;
			sep.setLayoutData(gd);

		}

		// showOnStart

		{
			_showOnStartBtn = new Button(comp, SWT.CHECK);
			_showOnStartBtn.setToolTipText(InspectCore.getPhaserHelp().getMemberHelp("AnimationConfig.showOnStart"));
			_showOnStartBtn.setText("Show On Start");
			var gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
			gd.horizontalSpan = 2;
			_showOnStartBtn.setLayoutData(gd);
			new CheckListener(_showOnStartBtn) {

				@Override
				protected void accept(boolean value) {
					getModels().forEach(model -> {
						model.setShowOnStart(value);
						model.buildTimeline();
					});

					var editor = getEditor();
					editor.getTimelineCanvas().redraw();
					editor.setDirty();
				}
			};
		}

		// hideOnComplete

		{
			_hideOnCompleteBtn = new Button(comp, SWT.CHECK);
			_hideOnCompleteBtn
					.setToolTipText(InspectCore.getPhaserHelp().getMemberHelp("AnimationConfig.hideOnComplete"));
			_hideOnCompleteBtn.setText("Hide On Complete");
			var gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
			gd.horizontalSpan = 2;
			_hideOnCompleteBtn.setLayoutData(gd);
			new CheckListener(_hideOnCompleteBtn) {

				@Override
				protected void accept(boolean value) {
					getModels().forEach(model -> {
						model.setHideOnComplete(value);
						model.buildTimeline();
					});

					var editor = getEditor();
					editor.getTimelineCanvas().redraw();
					editor.setDirty();
				}
			};
		}

		// skipMissedFrames

		{
			_skipMissedFramesBtn = new Button(comp, SWT.CHECK);
			_skipMissedFramesBtn
					.setToolTipText(InspectCore.getPhaserHelp().getMemberHelp("AnimationConfig.skipMissedFrames"));
			_skipMissedFramesBtn.setText("Skip Missed Frames");
			var gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
			gd.horizontalSpan = 2;
			_skipMissedFramesBtn.setLayoutData(gd);
			new CheckListener(_skipMissedFramesBtn) {

				@Override
				protected void accept(boolean value) {
					getModels().forEach(model -> {
						model.setSkipMissedFrames(value);
						model.buildTimeline();
					});

					var editor = getEditor();
					editor.getTimelineCanvas().redraw();
					editor.setDirty();
				}
			};

		}

		update_UI_from_Model();

		return comp;
	}

	@SuppressWarnings("boxing")
	@Override
	public void update_UI_from_Model() {
		var models = getModels();

		_keyText.setText(flatValues_to_String(models.stream().map(model -> model.getKey())));
		_keyText.setEditable(models.size() == 1);

		_frameRateText.setText(flatValues_to_String(models.stream().map(model -> model.getFrameRate())));

		_durationText.setText(flatValues_to_String(models.stream().map(model -> model.getDuration())));
		updateTotalDuration();

		_delayText.setText(flatValues_to_String(models.stream().map(model -> model.getDelay())));

		_repeatText.setText(flatValues_to_String(models.stream().map(model -> model.getRepeat())));

		_repeatDelayText.setText(flatValues_to_String(models.stream().map(model -> model.getRepeatDelay())));

		_yoyoCheckBox.setSelection(flatValues_to_boolean(models.stream().map(model -> model.isYoyo())));

		_showOnStartBtn.setSelection(flatValues_to_Boolean(models.stream().map(model -> model.isShowOnStart())));

		_hideOnCompleteBtn.setSelection(flatValues_to_Boolean(models.stream().map(model -> model.isHideOnComplete())));

		_skipMissedFramesBtn
				.setSelection(flatValues_to_Boolean(models.stream().map(model -> model.isSkipMissedFrames())));
	}

	private void updateTotalDuration() {
		{
			var total = 0;
			for (var model : getModels()) {
				total += model.getComputedTotalDuration();
			}
			_computedDurationLabel.setText("Real duration: " + total);
		}
	}

}
