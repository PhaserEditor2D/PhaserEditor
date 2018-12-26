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
package phasereditor.assetpack.ui.preview;

import phasereditor.assetpack.core.SpritesheetAssetModel;
import phasereditor.assetpack.ui.AssetPackUI;
import phasereditor.ui.IFrameProvider;
import phasereditor.ui.ImageProxy;

/**
 * @author arian
 *
 */
public class SpritesheetAssetFrameProvider implements IFrameProvider {

	private SpritesheetAssetModel _model;

	public SpritesheetAssetFrameProvider(SpritesheetAssetModel model) {
		_model = model;
	}

	@Override
	public int getFrameCount() {
		return _model.getFrames().size();
	}

	@Override
	public ImageProxy getFrameImageProxy(int index) {
		return AssetPackUI.getImageProxy(getFrameObject(index));
	}

	@Override
	public SpritesheetAssetModel.FrameModel getFrameObject(int index) {
		return _model.getFrames().get(index);
	}

	@Override
	public String getFrameLabel(int index) {
		return index + " - " + _model.getKey();
	}

}
