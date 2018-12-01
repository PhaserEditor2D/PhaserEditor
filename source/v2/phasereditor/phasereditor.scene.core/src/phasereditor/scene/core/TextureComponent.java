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
package phasereditor.scene.core;

import phasereditor.assetpack.core.AssetFinder;
import phasereditor.assetpack.core.IAssetFrameModel;

/**
 * @author arian
 *
 */
public interface TextureComponent {

	// textureKey

	static String textureKey_name = "textureKey";

	static String textureKey_default = null;

	static String get_textureKey(ObjectModel obj) {
		return (String) obj.get("textureKey");
	}

	static void set_textureKey(ObjectModel obj, String textureKey) {
		obj.put("textureKey", textureKey);
	}

	// textureFrame

	static String textureFrame_name = "textureFrame";

	static String textureFrame_default = null;

	static String get_textureFrame(ObjectModel obj) {
		return (String) obj.get("textureFrame");
	}

	static void set_textureFrame(ObjectModel obj, String textureFrame) {
		obj.put("textureFrame", textureFrame);
	}

	static boolean is(Object model) {
		return model instanceof TextureComponent;
	}

	// utils

	static IAssetFrameModel utils_getTexture(ObjectModel model, AssetFinder finder) {
		var key = get_textureKey(model);

		var frame = get_textureFrame(model);

		var asset = finder.findTexture(key, frame);
		
		return asset;
	}

	static void utils_setTexture(ObjectModel model, IAssetFrameModel frame) {
		if (frame == null) {
			set_textureKey(model, null);
			set_textureFrame(model, null);
		} else {
			set_textureKey(model, frame.getAsset().getKey());
			set_textureFrame(model, frame.getKey());
		}
	}

	static void init(ObjectModel obj) {
		set_textureKey(obj, textureKey_default);
		set_textureFrame(obj, textureFrame_default);
	}
}
