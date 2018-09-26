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

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import phasereditor.animation.ui.editor.AnimationModel_in_Editor;
import phasereditor.inspect.core.InspectCore;
import phasereditor.ui.properties.FormPropertySection;

/**
 * @author arian
 *
 */
public class AnimationSection extends FormPropertySection {

	private Label _computedDurationLabel;
	private Text _durationText;
	private Text _keyText;
	private Text _frameRateText;
	private Text _delayText;
	private Text _repeatText;
	private Button _showOnStartBtn;
	private Button _hideOnCompleteBtn;
	private Button _skipMissedFramesBtn;

	public AnimationSection() {
		super("Animation");
	}

	@Override
	public boolean canEdit(Object obj) {
		return obj instanceof AnimationModel_in_Editor;
	}

	@SuppressWarnings("unused")
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

		}

		// delay

		{
			var label = new Label(comp, SWT.NONE);
			label.setText("Delay");
			label.setToolTipText(InspectCore.getPhaserHelp().getMemberHelp("AnimationConfig.delay"));

			_delayText = new Text(comp, SWT.BORDER);
			_delayText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		}

		// repeat

		{
			var label = new Label(comp, SWT.NONE);
			label.setText("Repeat");
			label.setToolTipText(InspectCore.getPhaserHelp().getMemberHelp("AnimationConfig.repeat"));

			_repeatText = new Text(comp, SWT.BORDER);
			_repeatText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
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
		}

		update_UI_from_Model();

		return comp;
	}

	@SuppressWarnings("boxing")
	@Override
	public void update_UI_from_Model() {
		var models = List.of(getModels());

		// key

		_keyText.setText(
				flatValues_to_String(models.stream().map(model -> ((AnimationModel_in_Editor) model).getKey())));

		listen(_keyText, value -> {
			models.stream().forEach(model -> {
				var animation = (AnimationModel_in_Editor) model;
				animation.setKey(value);

				var editor = animation.getEditor();

				animation.buildTimeline();

				editor.refreshOutline();
				editor.getTimelineCanvas().redraw();
				editor.setDirty();
			});
		});

		_keyText.setEditable(models.size() == 1);

		// frameRate

		_frameRateText.setText(
				flatValues_to_String(models.stream().map(model -> ((AnimationModel_in_Editor) model).getFrameRate())));

		listenFloat(_frameRateText, value -> {
			models.stream().forEach(model -> {
				var animation = (AnimationModel_in_Editor) model;
				animation.setFrameRate(value);

				var editor = animation.getEditor();

				animation.buildTimeline();

				editor.getTimelineCanvas().redraw();
				editor.setDirty();
			});
		});

		// duration

		_durationText.setText(
				flatValues_to_String(models.stream().map(model -> ((AnimationModel_in_Editor) model).getDuration())));

		listenInt(_durationText, value -> {
			models.stream().forEach(model -> {
				var animation = (AnimationModel_in_Editor) model;
				animation.setDuration(value);

				var editor = animation.getEditor();

				animation.buildTimeline();

				editor.getTimelineCanvas().redraw();
				editor.setDirty();
			});

			updateTotalDuration();

		});

		updateTotalDuration();

		// delay

		_delayText.setText(
				flatValues_to_String(models.stream().map(model -> ((AnimationModel_in_Editor) model).getDelay())));

		listenInt(_delayText, value -> {
			models.stream().forEach(model -> {
				var animation = (AnimationModel_in_Editor) model;

				animation.setDelay(value);

				var editor = animation.getEditor();

				animation.buildTimeline();

				editor.getTimelineCanvas().redraw();
				editor.setDirty();
			});
		});

		// repeat

		_repeatText.setText(
				flatValues_to_String(models.stream().map(model -> ((AnimationModel_in_Editor) model).getRepeat())));

		listenInt(_repeatText, value -> {
			models.stream().forEach(model -> {
				var animation = (AnimationModel_in_Editor) model;

				animation.setRepeat(value);

				var editor = animation.getEditor();

				animation.buildTimeline();

				editor.getTimelineCanvas().redraw();
				editor.setDirty();
			});
		});

		// showOnStart

		_showOnStartBtn.setSelection(flatValues_to_Boolean(
				models.stream().map(model -> ((AnimationModel_in_Editor) model).isShowOnStart())));

		listen(_showOnStartBtn, value -> {
			models.stream().forEach(model -> {
				var animation = (AnimationModel_in_Editor) model;

				animation.setShowOnStart(value);

				var editor = animation.getEditor();

				animation.buildTimeline();

				editor.getTimelineCanvas().redraw();
				editor.setDirty();
			});
		});

		// hideOnComplete

		_hideOnCompleteBtn.setSelection(flatValues_to_Boolean(
				models.stream().map(model -> ((AnimationModel_in_Editor) model).isHideOnComplete())));

		listen(_hideOnCompleteBtn, value -> {
			models.stream().forEach(model -> {
				var animation = (AnimationModel_in_Editor) model;

				animation.setHideOnComplete(value);

				var editor = animation.getEditor();

				animation.buildTimeline();

				editor.getTimelineCanvas().redraw();
				editor.setDirty();
			});
		});

		// skipMissedFrames

		_skipMissedFramesBtn.setSelection(flatValues_to_Boolean(
				models.stream().map(model -> ((AnimationModel_in_Editor) model).isSkipMissedFrames())));

		listen(_skipMissedFramesBtn, value -> {
			models.stream().forEach(model -> {
				var animation = (AnimationModel_in_Editor) model;

				animation.setSkipMissedFrames(value);

				var editor = animation.getEditor();

				animation.buildTimeline();

				editor.getTimelineCanvas().redraw();
				editor.setDirty();
			});
		});
	}

	private void updateTotalDuration() {
		{
			var total = 0;
			for (var model : getModels()) {
				total += ((AnimationModel_in_Editor) model).getComputedTotalDuration();
			}
			_computedDurationLabel.setText("Real duration: " + total);
		}
	}

}
