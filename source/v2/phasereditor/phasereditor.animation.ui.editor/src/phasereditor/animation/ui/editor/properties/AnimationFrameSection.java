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

import static java.util.stream.Collectors.toList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import phasereditor.assetpack.core.animations.AnimationFrameModel;
import phasereditor.inspect.core.jsdoc.IPhaserFullnames;
import phasereditor.ui.properties.TextToIntListener;

/**
 * @author arian
 *
 */
@SuppressWarnings("boxing")
public class AnimationFrameSection extends BaseAnimationSection<AnimationFrameModel> implements IPhaserFullnames {

	public AnimationFrameSection(AnimationsPropertyPage page) {
		super(page, "Animation Frame");
	}

	@Override
	public boolean canEdit(Object obj) {
		return obj instanceof AnimationFrameModel;
	}

	@Override
	public boolean supportThisNumberOfModels(int number) {
		return number == 1;
	}

	@SuppressWarnings("unused")
	@Override
	public Control createContent(Composite parent) {

		var comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new GridLayout(2, false));

		// duration

		{
			label(comp, "Duration", AnimationFrame_duration);
			Text text = new Text(comp, SWT.BORDER);
			text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			new TextToIntListener(text) {

				@Override
				protected void accept(int value) {
					var animations = getModels().stream().map(model -> model.getAnimation()).distinct()
							.collect(toList());
					wrapOperation(animations, () -> {
						getModels().forEach(model -> {
							model.setDuration(value);
						});
					});
				}
			};

			addUpdate(() -> {
				text.setText(flatValues_to_String(getModels().stream().map(model -> model.getDuration())));
			});

		}

		{
			new Label(comp, SWT.NONE);
			var label = new Label(comp, SWT.NONE);
			label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			label.setToolTipText(
					"The computed duration of the frame. It is the frameRate-based duration plus the extra duration set in the 'duration' property.\nNOTE: This is not part of the Phaser API.");
			addUpdate(() -> {
				var total = 0;
				for (var model : getModels()) {
					total += model.getComputedDuration();
				}
				label.setText("Real duration: " + total);
			});

		}

		return comp;
	}

}
