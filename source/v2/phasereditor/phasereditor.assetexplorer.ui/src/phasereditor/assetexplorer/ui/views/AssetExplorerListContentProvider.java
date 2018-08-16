// The MIT License (MIT)
//
// Copyright (c) 2015, 2016 Arian Fornaris
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
package phasereditor.assetexplorer.ui.views;

import java.util.ArrayList;
import java.util.List;

import phasereditor.assetpack.core.AtlasAssetModel;
import phasereditor.assetpack.core.ImageAssetModel;
import phasereditor.assetpack.core.SpritesheetAssetModel;

/**
 * @author arian
 *
 */
public class AssetExplorerListContentProvider extends AssetExplorerContentProvider {

	@Override
	public Object[] getChildren(Object parent) {

		if (parent == AssetsView.ROOT) {
			List<Object> list = new ArrayList<>();
			fillList(parent, list);
			return list.toArray();
		}

		return super.getChildren(parent);
	}

	private void fillList(Object elem, List<Object> list) {
		boolean add = false;
		add = add || elem instanceof ImageAssetModel;
		add = add || elem instanceof SpritesheetAssetModel.FrameModel;
		add = add || elem instanceof AtlasAssetModel.Frame;

		if (add) {
			list.add(elem);
		}

		Object[] children = super.getChildren(elem);

		for (Object child : children) {
			fillList(child, list);
		}
	}
}
