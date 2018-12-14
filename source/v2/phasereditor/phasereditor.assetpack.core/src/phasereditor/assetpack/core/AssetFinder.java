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
package phasereditor.assetpack.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;

/**
 * @author arian
 *
 */
public class AssetFinder {

	private Map<String, IAssetKey> _map;
	private IProject _project;
	private AssetPackModel[] _extraPacks;

	public AssetFinder(IProject project, AssetPackModel... extraPacks) {
		_project = project;
		_extraPacks = extraPacks;

		_map = new HashMap<>();
	}

	public void build() {
		var packs = new ArrayList<>(AssetPackCore.getAssetPackModels(_project));
		packs.addAll(List.of(_extraPacks));

		_map = new HashMap<>();

		for (var pack : packs) {
			for (var asset : pack.getAssets()) {
				var key = asset.getKey();
				_map.put(key, asset);

				for (var elem : asset.getSubElements()) {
					var frame = elem.getKey();
					_map.put(hashKey(key, frame), elem);
				}
			}
		}
	}

	public IAssetKey findAssetKey(String key) {
		return findAssetKey(key, null);
	}
	
	public IAssetKey findAssetKey(String key, String frame) {
		
		if (key == null) {
			return null;
		}

		IAssetKey assetKey = null;

		if (frame == null) {
			assetKey = _map.get(key);
		} else {
			assetKey = _map.get(hashKey(key, frame));
		}
		
		return assetKey;
	}

	public ImageAssetModel findImage(String key) {
		var texture = findTexture(key, key);
		
		if (texture != null && texture.getAsset() instanceof ImageAssetModel) {
			return (ImageAssetModel) texture.getAsset();
		}
		
		return null;
	}
	
	public IAssetFrameModel findTexture(String key, String frame) {
		var assetKey = findAssetKey(key, frame);

		if (assetKey != null) {

			if (assetKey instanceof IAssetFrameModel) {
				return (IAssetFrameModel) assetKey;
			}

		}

		return null;
	}

	private static String hashKey(String key, String frame) {
		return frame + "@" + key;
	}

	public AssetFinder snapshot() {
		var finder = new AssetFinder(_project);
		
		finder._map = new HashMap<>(_map);
		
		return finder;
	}

}
