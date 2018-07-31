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

import phasereditor.animation.ui.AnimationModel_in_Editor;
import phasereditor.assetpack.core.animations.AnimationModel;
import phasereditor.inspect.core.InspectCore;
import phasereditor.ui.properties.PGridBooleanProperty;
import phasereditor.ui.properties.PGridNumberProperty;
import phasereditor.ui.properties.PGridStringProperty;

/**
 * @author arian
 *
 */
@SuppressWarnings("boxing")
public class AnimationModel_in_Editor_PGridModel extends BaseAnimationPGridModel {
	private AnimationModel_in_Editor _animation;
	private String _id;

	public AnimationModel_in_Editor_PGridModel(AnimationModel_in_Editor anim) {
		super();
		_animation = anim;

		_id = anim.getKey();

		addSection("Animation",

				new PGridStringProperty(_id, "key", "<tooltip>") {

					@Override
					public void setValue(String value, boolean notify) {
						anim.setKey(value);
					}

					@Override
					public boolean isModified() {
						return true;
					}

					@Override
					public String getValue() {
						return anim.getKey();
					}

				},

				new PGridNumberProperty(_id, "frameRate", help("frameRate")) {

					@Override
					public Double getValue() {
						return (double) anim.getFrameRate();
					}

					@Override
					public void setValue(Double value, boolean notify) {
						anim.setFrameRate(value);
						refreshGrid();
					}

					@Override
					public boolean isModified() {
						return anim.getFrameRate() != 24;
					}
				},

				new PGridNumberProperty(_id, "duration", help("duration")) {

					@Override
					public Double getValue() {
						return (double) anim.getDuration();
					}

					@Override
					public void setValue(Double value, boolean notify) {
						anim.setDuration(value.intValue());
						refreshGrid();
					}

					@Override
					public boolean isModified() {
						return anim.getDuration() != 0;
					}
				},

				
				new PGridNumberProperty(_id, "delay", help("delay"), true) {

					@Override
					public Double getValue() {
						return (double) anim.getDelay();
					}

					@Override
					public void setValue(Double value, boolean notify) {
						anim.setDelay(value.intValue());
					}

					@Override
					public boolean isModified() {
						return anim.getDelay() != 0;
					}
				},

				new PGridNumberProperty(_id, "repeat", help("repeat"), true) {

					@Override
					public Double getValue() {
						return (double) anim.getRepeat();
					}

					@Override
					public void setValue(Double value, boolean notify) {
						anim.setRepeat(value.intValue());
					}

					@Override
					public Double getDefaultValue() {
						return 0d;
					}

					@Override
					public boolean isModified() {
						return anim.getRepeat() != 0;
					}
				},

				new PGridNumberProperty(_id, "repeatDelay", help("repeatDelay"), true) {

					@Override
					public Double getValue() {
						return (double) anim.getRepeatDelay();
					}

					@Override
					public void setValue(Double value, boolean notify) {
						anim.setRepeatDelay(value.intValue());
					}

					@Override
					public boolean isModified() {
						return anim.getRepeatDelay() != 0;
					}

					@Override
					public Double getDefaultValue() {
						return 0d;
					}
				},

				new PGridBooleanProperty(_id, "yoyo", help("yoyo")) {

					@Override
					public Boolean getValue() {
						return anim.isYoyo();
					}

					@Override
					public void setValue(Boolean value, boolean notify) {
						anim.setYoyo(value);
					}

					@Override
					public boolean isModified() {
						return anim.isYoyo();
					}

					@Override
					public Boolean getDefaultValue() {
						return false;
					}

				},

				new PGridBooleanProperty(_id, "showOnStart", help("showOnStart")) {

					@Override
					public Boolean getValue() {
						return anim.isShowOnStart();
					}

					@Override
					public void setValue(Boolean value, boolean notify) {
						anim.setShowOnStart(value);
					}

					@Override
					public boolean isModified() {
						return anim.isShowOnStart();
					}

					@Override
					public Boolean getDefaultValue() {
						return false;
					}

				},

				new PGridBooleanProperty(_id, "hideOnComplete", help("hideOnComplete")) {

					@Override
					public Boolean getValue() {
						return anim.isHideOnComplete();
					}

					@Override
					public void setValue(Boolean value, boolean notify) {
						anim.setHideOnComplete(value);
					}

					@Override
					public boolean isModified() {
						return anim.isHideOnComplete();
					}

					@Override
					public Boolean getDefaultValue() {
						return false;
					}

				},

				new PGridBooleanProperty(_id, "skipMissedFrames", help("skipMissedFrames")) {

					@Override
					public Boolean getValue() {
						return anim.isSkipMissedFrames();
					}

					@Override
					public void setValue(Boolean value, boolean notify) {
						anim.setSkipMissedFrames(value);
					}

					@Override
					public boolean isModified() {
						return !anim.isSkipMissedFrames();
					}

					@Override
					public Boolean getDefaultValue() {
						return true;
					}

				}

		);
	}

	private static String help(String field) {
		return InspectCore.getPhaserHelp().getMemberHelp("Phaser.Animations.Animation." + field);
	}

	public AnimationModel getAnimation() {
		return _animation;
	}
}
