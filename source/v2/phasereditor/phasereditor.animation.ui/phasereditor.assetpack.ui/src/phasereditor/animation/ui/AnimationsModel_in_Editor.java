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

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.assetpack.core.IAssetFrameModel;
import phasereditor.assetpack.core.animations.AnimationModel;
import phasereditor.assetpack.core.animations.AnimationsModel;

/**
 * @author arian
 *
 */
public class AnimationsModel_in_Editor extends AnimationsModel {

	private AnimationsEditor _editor;

	public AnimationsModel_in_Editor(AnimationsEditor editor, JSONObject jsonData) {
		super(jsonData);
		_editor = editor;
	}
	
	public AnimationsEditor getEditor() {
		return _editor;
	}

	@Override
	protected AnimationModel createAnimation(JSONObject jsonData) {
		return new AnimationModel_in_Editor(this, jsonData);
	}

	public void build() {
		for (var anim : getAnimations()) {
			Map<String, IAssetFrameModel> cache = new HashMap<>();

			for (var animFrame : anim.getFrames()) {

				var textureKey = animFrame.getTextureKey();
				var frameName = animFrame.getFrameName();

				var cacheKey = frameName + "@" + textureKey;
				var frame = cache.get(cacheKey);

				if (frame != null) {
					animFrame.setFrameAsset(frame);
					continue;
				}

				var packs = AssetPackCore.getAssetPackModels(_editor.getEditorInput().getFile().getProject());
				for (var pack : packs) {
					frame = pack.findFrame(textureKey, frameName);
				}

				if (frame == null) {
					// problems.add(errorStatus(
					// "Cannot find the frame '" + frameName + "' in the texture '" + textureKey +
					// "'."));
					out.println("AssetPackEditor: Cannot find the frame '" + frameName + "' in the texture '"
							+ textureKey + "'.");
				} else {
					cache.put(cacheKey, frame);
				}

				animFrame.setFrameAsset(frame);

			}
		}
	}
}
