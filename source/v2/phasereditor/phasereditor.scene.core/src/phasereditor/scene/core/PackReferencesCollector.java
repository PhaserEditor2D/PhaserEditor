// The MIT License (MIT)
//
// Copyright (c) 2015, 2019 Arian Fornaris
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.json.JSONObject;

import phasereditor.assetpack.core.AssetFinder;
import phasereditor.assetpack.core.AssetModel;
import phasereditor.assetpack.core.AssetPackModel;
import phasereditor.assetpack.core.AssetSectionModel;
import phasereditor.assetpack.core.IAssetKey;
import phasereditor.project.core.ProjectCore;

/**
 * @author arian
 *
 */
public class PackReferencesCollector {

	private SceneModel _sceneModel;
	private AssetFinder _finder;

	public PackReferencesCollector(SceneModel sceneModel, AssetFinder finder) {
		super();
		_sceneModel = sceneModel;
		_finder = finder;
	}

	public List<IAssetKey> collectAssetKeys() {
		var list = new ArrayList<IAssetKey>();

		_sceneModel.getDisplayList().visit(objModel -> {
			IAssetKey assetKey = null;

			if (objModel instanceof TextureComponent) {
				assetKey = TextureComponent.utils_getTexture(objModel, _finder);
			} else if (objModel instanceof BitmapTextComponent) {
				var key = BitmapTextComponent.get_fontAssetKey(objModel);
				assetKey = _finder.findAssetKey(key);
			}

			if (assetKey != null) {
				list.add(assetKey);
			}
		});

		return list;
	}

	public Collection<String[]> collectKeyUrlTuples() {
		Map<String, String[]> packSectionList = new HashMap<>();

		var assetKeys = collectAssetKeys();
		
		for (var assetKey : assetKeys) {
			var packFile = assetKey.getAsset().getPack().getFile();

			var key = packFile.getFullPath().removeFileExtension().lastSegment();
			var url = ProjectCore.getAssetUrl(packFile);

			packSectionList.put(key + "-" + url, new String[] { key, url });
		}

		return packSectionList.values();
	}

	public AssetPackModel collectNewPack(Predicate<AssetModel> filter) throws Exception {
		var assetKeys = collectAssetKeys();
		var assetModels = new HashSet<AssetModel>();
		
		for(var assetKey : assetKeys) {
			if (filter.test(assetKey.getAsset())) {
				assetModels.add(assetKey.getAsset());
			}
		}
		
		var pack = new AssetPackModel(new JSONObject(), null);
		var section = new AssetSectionModel("section", pack);
		pack.addSection(section, false);
		
		for(var model: assetModels) {
			section.addAsset(model, false);
		}
		
		return pack;
	}
}
