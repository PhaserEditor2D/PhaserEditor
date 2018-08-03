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

import org.json.JSONObject;

import phasereditor.animation.ui.properties.AnimationModel_in_Editor_PGridModel;
import phasereditor.assetpack.core.animations.AnimationFrameModel;
import phasereditor.assetpack.core.animations.AnimationModel;
import phasereditor.ui.properties.PGridModel;

/**
 * @author arian
 *
 */
public class AnimationModel_in_Editor extends AnimationModel {


	public AnimationModel_in_Editor(AnimationsModel_in_Editor animations) {
		super(animations);
	}

	public AnimationModel_in_Editor(AnimationsModel_in_Editor animations, JSONObject jsonData) {
		super(animations, jsonData);
	}

	@Override
	protected AnimationFrameModel createAnimationFrame(JSONObject jsonData) {
		return new AnimationFrameModel_in_Editor(this, jsonData);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Object getAdapter(Class adapter) {
		
		if (adapter == PGridModel.class) {
			return new AnimationModel_in_Editor_PGridModel(this);
		}
		
		return super.getAdapter(adapter);
	}

	public AnimationsEditor getEditor() {
		return getAnimations().getEditor();
	}

	@Override
	public AnimationsModel_in_Editor getAnimations() {
		return (AnimationsModel_in_Editor) super.getAnimations();
	}
}
