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

import org.json.JSONObject;

import phasereditor.assetpack.core.AtlasAssetModel;
import phasereditor.assetpack.core.AtlasAssetModel.Frame;
import phasereditor.assetpack.core.IAssetKey;
import phasereditor.assetpack.core.ImageAssetModel;
import phasereditor.assetpack.core.SpritesheetAssetModel;

/**
 * @author arian
 *
 */
public class CanvasModelFactory {
	public static BaseSpriteModel createModel(GroupModel parent, IAssetKey obj) {
		if (obj instanceof ImageAssetModel) {
			return new ImageSpriteModel(parent, ((ImageAssetModel) obj).getFrame());
		} else if (obj instanceof SpritesheetAssetModel) {
			return new SpritesheetSpriteModel(parent, ((SpritesheetAssetModel) obj).getFrames().get(0));
		} else if (obj instanceof SpritesheetAssetModel.FrameModel) {
			SpritesheetSpriteModel model = new SpritesheetSpriteModel(parent, (SpritesheetAssetModel.FrameModel) obj);
			return model;
		} else if (obj instanceof AtlasAssetModel.Frame) {
			return new AtlasSpriteModel(parent, (Frame) obj);
		}
		return null;
	}

	public static BaseObjectModel createModel(GroupModel parent, JSONObject data) {
		try {
			BaseObjectModel model = null;
			String type = data.getString("type");
			switch (type) {
			case ImageSpriteModel.TYPE_NAME:
				model = new ImageSpriteModel(parent, data);
				break;
			case SpritesheetSpriteModel.TYPE_NAME:
				model = new SpritesheetSpriteModel(parent, data);
				break;
			case AtlasSpriteModel.TYPE_NAME:
				model = new AtlasSpriteModel(parent, data);
				break;
			case TileSpriteModel.TYPE_NAME:
				model = new TileSpriteModel(parent, data);
				break;
			case ButtonSpriteModel.TYPE_NAME:
				model = new ButtonSpriteModel(parent, data);
				break;
			case GroupModel.TYPE_NAME:
				model = new GroupModel(parent, data);
				break;
			default:
				break;
			}
			return model;
		} catch (MissingAssetException e) {
			return new MissingAssetSpriteModel(parent, data);
		}
	}
}
