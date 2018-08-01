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
package phasereditor.animation.ui.properties;

import phasereditor.animation.ui.AnimationFrameModel_in_Editor;
import phasereditor.animation.ui.AnimationModel_in_Editor;
import phasereditor.inspect.core.InspectCore;
import phasereditor.ui.properties.PGridInfoProperty;
import phasereditor.ui.properties.PGridNumberProperty;

/**
 * @author arian
 *
 */
@SuppressWarnings("boxing")
public class AnimationFrameModel_in_Editor_PGridModel extends BaseAnimationPGridModel {
	private AnimationFrameModel_in_Editor _frame;
	private AnimationModel_in_Editor _anim;

	public AnimationFrameModel_in_Editor_PGridModel(AnimationModel_in_Editor anim,
			AnimationFrameModel_in_Editor frame) {

		_anim = anim;
		_frame = frame;

		var id = frame.getTextureKey() + ":" + frame.getFrameName();

		addSection("Frame",

				new PGridInfoProperty("key", help("key"), getFrame()::getTextureKey),

				new PGridInfoProperty("frame", help("frame"), () -> {
					return getFrame().getFrameName() == null ? "" : getFrame().getFrameName();
				}),

				new PGridNumberProperty(id, "duration", help("duration"), true) {

					@Override
					public void setValue(Double value, boolean notify) {
						getFrame().setDuration(value.intValue());
						updateAndRestartAnimation();
					}

					@Override
					public Double getValue() {
						return (double) getFrame().getDuration();
					}

					@Override
					public boolean isModified() {
						return getFrame().getDuration() != 0;
					}
				}

		);
	}

	private static String help(String field) {
		return InspectCore.getPhaserHelp().getMemberHelp("AnimationFrameConfig." + field);
	}

	public AnimationFrameModel_in_Editor getFrame() {
		return _frame;
	}

	public AnimationModel_in_Editor getAnimation() {
		return _anim;
	}

}
