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

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.assetpack.core.AssetType;
import phasereditor.assetpack.core.IAssetKey;
import phasereditor.assetpack.core.ImageAssetModel;
import phasereditor.assetpack.core.SpritesheetAssetModel;

/**
 * @author arian
 *
 */
public class AssetSpriteModel<T extends IAssetKey> extends BaseSpriteModel {
	private T _assetKey;

	public AssetSpriteModel(GroupModel parent, String typeName, JSONObject obj) {
		super(parent, typeName, obj);
	}

	public AssetSpriteModel(GroupModel parent, T assetKey, String typeName) {
		super(parent, typeName);
		_assetKey = assetKey;

		String name = assetKey.getKey();
		if (assetKey.getAsset() instanceof SpritesheetAssetModel) {
			name = assetKey.getAsset().getKey();
		}

		setEditorName(CanvasCore.getValidJavaScriptName(name));
	}

	public T getAssetKey() {
		return _assetKey;
	}

	public void setAssetKey(T assetKey) {
		_assetKey = assetKey;
	}

	public AssetType getAssetType() {
		return _assetKey.getAsset().getType();
	}

	@Override
	protected void writeMetadata(JSONObject obj) {
		super.writeMetadata(obj);
		// TODO: change it to use the UUID of asset packs!
		IAssetKey key = _assetKey;

		// if the key is an image frame, then we save only the refs to the image
		// asset.
		if (key instanceof ImageAssetModel.Frame) {
			key = key.getAsset();
		}

		obj.put("asset-ref", AssetPackCore.getAssetJSONReference(key));
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void readMetadata(JSONObject obj) {
		super.readMetadata(obj);

		IAssetKey asset = findAsset(obj);

		// what we really need is to get the frame of the image.
		if (asset instanceof ImageAssetModel) {
			_assetKey = (T) ((ImageAssetModel) asset).getFrame();
		} else {
			_assetKey = (T) asset;
		}
	}

	@Override
	protected List<AnimationModel> readAnimations(JSONArray array) {
		List<AnimationModel> list = new ArrayList<>();
		for (int i = 0; i < array.length(); i++) {
			JSONObject obj = array.getJSONObject(i);
			AnimationModel model = new AnimationModel(null);
			model.read(_assetKey, obj);
			list.add(model);
		}
		return list;
	}

	@Override
	public void build() {
		_assetKey = buildAssetKey(_assetKey);
		for (AnimationModel model : getAnimations()) {
			model.rebuild(_assetKey);
		}
	}

	/**
	 * @param assetKey
	 */
	@SuppressWarnings("unchecked")
	protected T buildAssetKey(T assetKey) {
		if (assetKey == null) {
			// nothing to do
			return null;
		}

		T freshVersion = (T) assetKey.findFreshVersion();

		return freshVersion;
	}
}
