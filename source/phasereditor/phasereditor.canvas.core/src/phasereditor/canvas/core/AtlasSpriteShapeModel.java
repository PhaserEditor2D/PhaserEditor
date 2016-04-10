// The MIT License (MIT)
//
// Copyright (c) 2016 Arian Fornaris
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

import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.assetpack.core.AtlasAssetModel;
import phasereditor.assetpack.core.AtlasAssetModel.FrameItem;

/**
 * @author arian
 *
 */
public class AtlasSpriteShapeModel extends BaseSpriteShapeModel {
	public static final String TYPE_NAME = "atlas-sprite";
	private FrameItem _frame;

	public AtlasSpriteShapeModel(GroupModel parent, FrameItem frame) {
		super(parent, TYPE_NAME);
		_frame = frame;
		setEditorName(frame.getName());
	}

	public AtlasSpriteShapeModel(GroupModel parent, JSONObject obj) {
		super(parent, TYPE_NAME, obj);
	}
	
	@Override
	protected void writeMetadata(JSONObject obj) {
		super.writeMetadata(obj);
		JSONObject asset = AssetPackCore.getAssetJSONReference(_frame.getAsset());
		obj.put("asset-ref", asset);
		obj.put("frame-name", _frame.getName());
	}

	@Override
	protected void readMetadata(JSONObject obj) {
		super.readMetadata(obj);
		JSONObject ref = obj.getJSONObject("asset-ref");
		AtlasAssetModel asset = (AtlasAssetModel) AssetPackCore.findAssetElement(ref);
		String name = obj.getString("frame-name");

		for (FrameItem frame : asset.getAtlasFrames()) {
			if (frame.getName().equals(name)) {
				_frame = frame;
				break;
			}
		}
	}

	public FrameItem getFrame() {
		return _frame;
	}
}
