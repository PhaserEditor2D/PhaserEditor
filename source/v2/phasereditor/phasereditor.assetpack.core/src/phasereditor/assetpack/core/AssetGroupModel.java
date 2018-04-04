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

import org.eclipse.core.runtime.IAdaptable;

public class AssetGroupModel implements Comparable<AssetGroupModel>, IAdaptable {
	private AssetType _type;
	private AssetSectionModel _section;

	AssetGroupModel(AssetType type, AssetSectionModel section) {
		super();
		_type = type;
		_section = section;
	}

	public AssetType getType() {
		return _type;
	}

	public AssetSectionModel getSection() {
		return _section;
	}

	@Override
	public int compareTo(AssetGroupModel o) {
		return Integer.compare(_type.ordinal(), o._type.ordinal());
	}

	public void remove(AssetModel asset) {
		if (asset.getType() == _type) {
			_section.removeAsset(asset);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
		return null;
	}
	
	public List<AssetModel> getAssets() {
		List<AssetModel> list = new ArrayList<>();
		for(AssetModel asset : _section.getAssets()) {
			if (asset.getType() == getType()) {
				list.add(asset);
			}
		}
		return list;
	}
}