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
package phasereditor.assetpack.ui.refactorings;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;

import phasereditor.assetpack.core.AssetModel;

/**
 * @author arian
 *
 */
public class AssetMoveList {
	private List<String[]> _assets;
	private IFile _packFile;

	private AssetMoveList() {
	}

	public AssetMoveList(AssetModel[] assets, String dstSection) {
		_packFile = assets[0].getPack().getFile();
		_assets = new ArrayList<>();

		for (AssetModel asset : assets) {
			_assets.add(new String[] { asset.getKey(), asset.getSection().getKey(), dstSection });
		}
	}

	public AssetMoveList reverse() {
		AssetMoveList list = new AssetMoveList();
		list._assets = new ArrayList<>();
		list._packFile = _packFile;

		for (String[] tuple : _assets) {
			list._assets.add(new String[] { tuple[0], tuple[2], tuple[1] });
		}

		return list;
	}

	public int size() {
		return _assets.size();
	}

	public String getAssetName(int i) {
		return _assets.get(i)[0];
	}

	public String getInitialSectionName(int i) {
		return _assets.get(i)[1];
	}

	public String getDestinySectionName(int i) {
		return _assets.get(i)[2];
	}

	public IFile getPackFile() {
		return _packFile;
	}
}
