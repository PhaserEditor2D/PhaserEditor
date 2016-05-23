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
package phasereditor.canvas.core;

import phasereditor.assetpack.core.AtlasAssetModel.FrameItem;
import phasereditor.assetpack.core.AtlasAssetModel;
import phasereditor.assetpack.core.ImageAssetModel;
import phasereditor.assetpack.core.SpritesheetAssetModel;
import phasereditor.assetpack.core.SpritesheetAssetModel.FrameModel;

/**
 * @author arian
 *
 */
public class ObjectModelFactory {
	public static BaseSpriteShapeModel createModel(GroupModel parent, Object obj) {
		if (obj instanceof ImageAssetModel) {
			return new ImageSpriteShapeModel(parent, (ImageAssetModel) obj);
		} else if (obj instanceof SpritesheetAssetModel) {
			return new SpritesheetShapeModel(parent, (SpritesheetAssetModel) obj);
		} else if (obj instanceof SpritesheetAssetModel.FrameModel) {
			FrameModel frame = (SpritesheetAssetModel.FrameModel) obj;
			SpritesheetAssetModel asset = frame.getAsset();
			SpritesheetShapeModel model = new SpritesheetShapeModel(parent, asset);
			model.setFrameIndex(frame.getIndex());
			return model;
		} else if (obj instanceof AtlasAssetModel.FrameItem) {
			return new AtlasSpriteShapeModel(parent, (FrameItem) obj);
		}
		return null;
	}
}
