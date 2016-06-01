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
import phasereditor.assetpack.core.AssetType;
import phasereditor.assetpack.core.IAssetKey;

/**
 * @author arian
 *
 */
public class AssetSpriteModel<T extends IAssetKey> extends BaseSpriteModel {
	private T _assetKey;

	@SuppressWarnings("unchecked")
	public AssetSpriteModel(GroupModel parent, String typeName, JSONObject obj) {
		super(parent, typeName, obj);
		_assetKey = (T) findAsset(obj);
	}

	public AssetSpriteModel(GroupModel parent, T asset, String typeName) {
		super(parent, typeName);
		_assetKey = asset;
		setEditorName(asset.getKey());
	}

	public T getAssetKey() {
		return _assetKey;
	}

	public AssetType getAssetType() {
		return _assetKey.getAsset().getType();
	}

	@Override
	protected void writeMetadata(JSONObject obj) {
		super.writeMetadata(obj);
		// TODO: change it to use the UUID of asset packs!
		obj.put("asset-ref", AssetPackCore.getAssetJSONReference(_assetKey));
	}
}
