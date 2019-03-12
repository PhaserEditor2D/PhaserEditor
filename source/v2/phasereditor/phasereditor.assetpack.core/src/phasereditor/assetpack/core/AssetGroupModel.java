// The MIT License (MIT)
//
// Copyright (c) 2015 Arian Fornaris
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
import java.util.List;

import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.IAdaptable;

import phasereditor.inspect.core.jsdoc.IJsdocProvider;

public class AssetGroupModel implements Comparable<AssetGroupModel>, IAdaptable, IAssetPackEelement {
	private AssetType _type;
	private AssetPackModel _pack;

	AssetGroupModel(AssetType type, AssetPackModel pack) {
		super();
		_type = type;
		_pack = pack;
	}

	public AssetType getType() {
		return _type;
	}

	public AssetPackModel getPack() {
		return _pack;
	}

	@Override
	public int compareTo(AssetGroupModel o) {
		return Integer.compare(_type.ordinal(), o._type.ordinal());
	}

	public void remove(AssetModel asset) {
		if (asset.getType() == _type) {
			for (var section : _pack.getSections()) {
				section.removeAsset(asset);
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
		if (adapter == IJsdocProvider.class) {
			return Adapters.adapt(_type, adapter);
		}
		return null;
	}

	public List<AssetModel> getAssets() {
		List<AssetModel> list = new ArrayList<>();
		for (var section : _pack.getSections()) {
			for (AssetModel asset : section.getAssets()) {
				if (asset.getType() == getType()) {
					list.add(asset);
				}
			}
		}
		return list;
	}
}