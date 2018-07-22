// The MIT License (MIT)
//
// Copyright (c) 2015, 2017 Arian Fornaris
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
package phasereditor.assetpack.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;

import phasereditor.assetpack.core.AssetGroupModel;
import phasereditor.assetpack.core.AssetModel;
import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.assetpack.core.AssetPackModel;
import phasereditor.assetpack.core.AssetSectionModel;
import phasereditor.assetpack.core.AssetType;
import phasereditor.assetpack.core.AtlasAssetModel;
import phasereditor.assetpack.core.AtlasAssetModel.Frame;
import phasereditor.assetpack.ui.AssetsContentProvider;

public class TextureTreeContentProvider extends AssetsContentProvider {
	@Override
	public Object[] getChildren(Object parent) {

		if (parent instanceof IProject) {
			List<AssetPackModel> packs = AssetPackCore.getAssetPackModels((IProject) parent);
			return packs.toArray();
		}

		if (parent instanceof AssetSectionModel) {
			AssetSectionModel section = (AssetSectionModel) parent;

			List<Object> list = new ArrayList<>();

			AssetType[] types = { AssetType.image, AssetType.spritesheet, AssetType.atlas, AssetType.atlasXML,
					AssetType.multiatlas, AssetType.unityAtlas };

			for (AssetType type : types) {
				AssetGroupModel group = section.getGroup(type);
				if (hasChildren(group)) {
					list.add(group);
				}
			}

			return list.toArray();
		}

		if (parent instanceof AssetModel) {
			AssetModel asset = (AssetModel) parent;

			switch (asset.getType()) {
			case atlas:
				List<Frame> frames = ((AtlasAssetModel) asset).getAtlasFrames();
				return frames.toArray();
			case spritesheet:
				return asset.getSubElements().toArray();
			default:
				break;
			}
		}

		return super.getChildren(parent);
	}
}