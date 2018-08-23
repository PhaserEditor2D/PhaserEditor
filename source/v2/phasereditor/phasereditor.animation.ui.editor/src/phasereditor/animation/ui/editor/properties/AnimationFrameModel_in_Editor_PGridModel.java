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

import phasereditor.animation.ui.editor.AnimationFrameModel_in_Editor;
import phasereditor.animation.ui.editor.AnimationModel_in_Editor;
import phasereditor.inspect.core.InspectCore;
import phasereditor.ui.properties.PGridNumberProperty;
import phasereditor.ui.properties.PGridStringProperty;

/**
 * @author arian
 *
 */
@SuppressWarnings({ "boxing" })
public class AnimationFrameModel_in_Editor_PGridModel extends BaseAnimationPGridModel {
	private AnimationFrameModel_in_Editor _frame;
	private AnimationModel_in_Editor _anim;

	public AnimationFrameModel_in_Editor_PGridModel(AnimationModel_in_Editor anim,
			AnimationFrameModel_in_Editor frame) {
		super(anim.getAnimations().getEditor());
		_anim = anim;
		_frame = frame;

		var id = frame.getTextureKey() + ":" + frame.getFrameName();

		addSection("Frame",

				new PGridStringProperty(id, "key", help("key")) {

					@Override
					public String getValue() {
						return getFrame().getTextureKey();
					}

					@Override
					public boolean isModified() {
						return false;
					}

					@Override
					public void setValue(String value, boolean notify) {
						//
					}

					@Override
					public boolean isReadOnly() {
						return true;
					}

				},

				new PGridStringProperty(id, "frame", help("frame")) {

					@Override
					public String getValue() {
						return getFrame().getFrameName() == null ? "" : getFrame().getFrameName().toString();
					}

					@Override
					public boolean isModified() {
						return false;
					}

					@Override
					public void setValue(String value, boolean notify) {
						//
					}

					@Override
					public boolean isReadOnly() {
						return true;
					}

				},

				new PGridNumberProperty(id, "duration", help("duration"), true) {

					@Override
					public void setValue(Double value, boolean notify) {
						getFrame().setDuration(value.intValue());
						getAnimation().buildTimeline();
					}

					@Override
					public Double getValue() {
						return (double) getFrame().getDuration();
					}

					@Override
					public boolean isModified() {
						return getFrame().getDuration() != 0;
					}
				},

				new PGridNumberProperty(id, "-realDuration",
						"The computed duration of the frame. It is the frameRate-based duration plus the extra duration set in the 'duration' property.\nNOTE: This is not part of the Phaser API.") {

					@Override
					public Double getValue() {
						return (double) getFrame().getComputedDuration();
					}

					@Override
					public boolean isModified() {
						return getFrame().getDuration() != 0;
					}

					@Override
					public boolean isReadOnly() {
						return true;
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
