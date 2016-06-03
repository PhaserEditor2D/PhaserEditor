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
package phasereditor.assetpack.ui.views;

import phasereditor.assetpack.core.AtlasAssetModel;
import phasereditor.assetpack.core.AtlasAssetModel.Frame;
import phasereditor.assetpack.core.SpritesheetAssetModel;

/**
 * @author arian
 *
 */
public class AssetExplorerListLabelProvider extends AssetExplorerLabelProvider {

	public AssetExplorerListLabelProvider() {
		super(48);
	}

	@Override
	public String getText(Object element) {
		if (element instanceof SpritesheetAssetModel.FrameModel) {
			SpritesheetAssetModel.FrameModel frame = (SpritesheetAssetModel.FrameModel) element;
			SpritesheetAssetModel asset = frame.getAsset();
			return frame.getIndex() + " (" + asset.getKey() + ")";
		}

		if (element instanceof AtlasAssetModel.Frame) {
			AtlasAssetModel.Frame frame = (Frame) element;
			return frame.getName() + " (" + frame.getAsset().getKey() + ")";
		}
		return super.getText(element);
	}

}
