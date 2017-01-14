// The MIT License (MIT)
//
// Copyright (c) 2015, 2016 Arian Fornaris
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
package phasereditor.canvas.core.codegen;

import phasereditor.assetpack.core.AtlasAssetModel;
import phasereditor.assetpack.core.IAssetKey;
import phasereditor.assetpack.core.ImageAssetModel;
import phasereditor.assetpack.core.SpritesheetAssetModel;

/**
 * @author arian
 *
 */
public interface ICodeGenerator {
	public String generate(String replace);

	public static class TextureArgs {
		public String key;
		public String frame = "null";
	}

	public static TextureArgs getTextureArgs(IAssetKey assetKey) {
		TextureArgs info = new TextureArgs();
		info.key = "'" + assetKey.getAsset().getKey() + "'";
		if (assetKey.getAsset() instanceof ImageAssetModel) {
			info.frame = "null";
		} else if (assetKey instanceof SpritesheetAssetModel.FrameModel) {
			info.frame = assetKey.getKey();
		} else if (assetKey instanceof AtlasAssetModel.Frame) {
			info.frame = "'" + assetKey.getKey() + "'";
		}
		return info;
	}
}
