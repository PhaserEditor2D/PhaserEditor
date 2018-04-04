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

import phasereditor.assetpack.core.AssetModel;
import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.assetpack.core.AssetPackModel;
import phasereditor.assetpack.core.IAssetElementModel;
import phasereditor.assetpack.core.IAssetFrameModel;
import phasereditor.assetpack.core.ImageAssetModel;

public class TextureListContentProvider extends AssetsContentProvider {

	@Override
	public Object[] getChildren(Object parent) {
		if (parent instanceof IProject) {
			List<Object> list = new ArrayList<>();
			List<AssetPackModel> packs = AssetPackCore.getAssetPackModels((IProject) parent);
			for (AssetPackModel pack : packs) {
				for (AssetModel asset : pack.getAssets()) {
					if (acceptAsset(asset)) {
						list.add(asset);
					} else {
						for (IAssetElementModel elem : asset.getSubElements()) {
							if (acceptAsset(elem)) {
								list.add(elem);
							}
						}
					}
				}
			}
			return list.toArray();
		}
		return super.getChildren(parent);
	}

	@SuppressWarnings("static-method")
	protected boolean acceptAsset(Object assetKey) {
		return assetKey instanceof ImageAssetModel || assetKey instanceof IAssetFrameModel;
	}
}