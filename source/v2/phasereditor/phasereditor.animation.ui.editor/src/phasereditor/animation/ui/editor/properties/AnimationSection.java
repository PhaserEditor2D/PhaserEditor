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

	public AnimationSection(AnimationsPropertyPage page) {
		super(page, "Animation");
	}

	@Override
	public boolean canEdit(Object obj) {
		return obj instanceof AnimationModel;
	}

	@SuppressWarnings({ "unused", "boxing" })
	@Override
	public Control createContent(Composite parent) {

		var comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new GridLayout(2, false));

		// key

		{
			var label = new Label(comp, SWT.NONE);
			label.setText("Key");
			label.setToolTipText(InspectCore.getPhaserHelp().getMemberHelp("AnimationConfig.key"));

			var text = new Text(comp, SWT.BORDER);
			text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			new TextListener(text) {

				@Override
				protected void accept(String value) {
					wrapOperation(getModels(), model -> {
						model.setKey(value);
					});

					var editor = getEditor();
					editor.refreshOutline();
					editor.getTimelineCanvas().redraw();
					editor.setDirty();
				}
			};

			addUpdate(() -> {
				text.setText(flatValues_to_String(getModels().stream().map(model -> model.getKey())));
				text.setEditable(getModels().size() == 1);
			});

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

			var text = new Text(comp, SWT.BORDER);
			text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			new TextToFloatListener(text) {

				@Override
				protected void accept(float value) {
					wrapOperation(getModels(), model -> {
						model.setFrameRate(value);
						model.buildTimeline();
					});
				}
			};

			addUpdate(() -> {
				text.setText(flatValues_to_String(getModels().stream().map(model -> model.getFrameRate())));
			});
		}

		// duration

		{
			var label = new Label(comp, SWT.NONE);
			label.setText("Duration");
			label.setToolTipText(InspectCore.getPhaserHelp().getMemberHelp("AnimationFrameConfig.duration"));

			var text = new Text(comp, SWT.BORDER);
			text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

			addUpdate(() -> {
				text.setText(flatValues_to_String(getModels().stream().map(model -> model.getDuration())));
			});

			// computed duration

			new Label(comp, SWT.NONE);
			var computedLabel = new Label(comp, SWT.NONE);
			computedLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			computedLabel.setToolTipText(
					"A computed duration based on the duration plus all the extra frame's durations.\\nNOTE: This is not part of the Phaser API.");
			new TextToIntListener(text) {

				@Override
				protected void accept(int value) {
					wrapOperation(getModels(), model -> {
						model.setDuration(value);
					});
				}
			};

			addUpdate(() -> {
				var total = 0;
				for (var model : getModels()) {
					total += model.getComputedTotalDuration();
				}
				computedLabel.setText("Real duration: " + total);
			});

		}

		// delay

		{
			var label = new Label(comp, SWT.NONE);
			label.setText("Delay");
			label.setToolTipText(InspectCore.getPhaserHelp().getMemberHelp("AnimationConfig.delay"));

			var text = new Text(comp, SWT.BORDER);
			text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

			new TextToIntListener(text) {

				@Override
				protected void accept(int value) {
					wrapOperation(getModels(), model -> {
						model.setDelay(value);
					});
				}
			};

			addUpdate(() -> {
				text.setText(flatValues_to_String(getModels().stream().map(model -> model.getDelay())));
			});
		}

		// repeat

		{
			var label = new Label(comp, SWT.NONE);
			label.setText("Repeat");
			label.setToolTipText(InspectCore.getPhaserHelp().getMemberHelp("AnimationConfig.repeat"));

			// here
			var text = new Text(comp, SWT.BORDER);
			text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			new TextToIntListener(text) {

				@Override
				protected void accept(int value) {
					wrapOperation(getModels(), model -> {
						model.setRepeat(value);
					});
				}
			};
			addUpdate(() -> {
				text.setText(flatValues_to_String(getModels().stream().map(model -> model.getRepeat())));
			});
		}

		// repeatDelay

		{
			var label = new Label(comp, SWT.NONE);
			label.setText("Repeat Delay");
			label.setToolTipText(InspectCore.getPhaserHelp().getMemberHelp("AnimationConfig.repeatDelay"));

			var text = new Text(comp, SWT.BORDER);
			text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			new TextToIntListener(text) {

				@Override
				protected void accept(int value) {
					wrapOperation(getModels(), model -> {
						model.setRepeatDelay(value);
					});
				}
			};
			addUpdate(() -> {
				text.setText(flatValues_to_String(getModels().stream().map(model -> model.getRepeatDelay())));

			});
		}

		// yoyo

		{
			var btn = new Button(comp, SWT.CHECK);
			btn.setText("Yoyo");
			btn.setToolTipText(InspectCore.getPhaserHelp().getMemberHelp("AnimationConfig.repeatDelay"));
			btn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
			new CheckListener(btn) {

				@Override
				protected void accept(boolean value) {
					wrapOperation(getModels(), model -> {
						model.setYoyo(value);
					});
				}
			};

			addUpdate(() -> {
				btn.setSelection(flatValues_to_boolean(getModels().stream().map(model -> model.isYoyo())));
			});
		}

		{
			var sep = new Label(comp, SWT.SEPARATOR | SWT.HORIZONTAL);
			var gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
			gd.horizontalSpan = 2;
			sep.setLayoutData(gd);

		}

		// showOnStart

		{
			var btn = new Button(comp, SWT.CHECK);
			btn.setToolTipText(InspectCore.getPhaserHelp().getMemberHelp("AnimationConfig.showOnStart"));
			btn.setText("Show On Start");
			var gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
			gd.horizontalSpan = 2;
			btn.setLayoutData(gd);
			new CheckListener(btn) {

				@Override
				protected void accept(boolean value) {
					wrapOperation(getModels(), model -> {
						model.setShowOnStart(value);
					});
				}
			};

			addUpdate(() -> {
				btn.setSelection(flatValues_to_Boolean(getModels().stream().map(model -> model.isShowOnStart())));
			});
		}

		// hideOnComplete

		{
			var btn = new Button(comp, SWT.CHECK);
			btn.setToolTipText(InspectCore.getPhaserHelp().getMemberHelp("AnimationConfig.hideOnComplete"));
			btn.setText("Hide On Complete");
			var gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
			gd.horizontalSpan = 2;
			btn.setLayoutData(gd);
			new CheckListener(btn) {

				@Override
				protected void accept(boolean value) {
					wrapOperation(getModels(), model -> {
						model.setHideOnComplete(value);
					});
				}
			};

			addUpdate(() -> {
				btn.setSelection(flatValues_to_Boolean(getModels().stream().map(model -> model.isHideOnComplete())));
			});
		}

		// skipMissedFrames

		{
			var btn = new Button(comp, SWT.CHECK);
			btn.setToolTipText(InspectCore.getPhaserHelp().getMemberHelp("AnimationConfig.skipMissedFrames"));
			btn.setText("Skip Missed Frames");
			var gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
			gd.horizontalSpan = 2;
			btn.setLayoutData(gd);
			new CheckListener(btn) {

				@Override
				protected void accept(boolean value) {
					wrapOperation(getModels(), model -> {
						model.setSkipMissedFrames(value);
					});
				}
			};

			addUpdate(() -> {
				btn.setSelection(flatValues_to_Boolean(getModels().stream().map(model -> model.isSkipMissedFrames())));
			});

		}

		return comp;
	}

}