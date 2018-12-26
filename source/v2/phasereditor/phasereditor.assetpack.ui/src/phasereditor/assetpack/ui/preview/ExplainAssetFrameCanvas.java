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

import org.eclipse.swt.widgets.Composite;

import phasereditor.assetpack.core.IAssetFrameModel;
import phasereditor.ui.ExplainFrameDataCanvas;

/**
 * @author arian
 *
 */
public class ExplainAssetFrameCanvas extends ExplainFrameDataCanvas {

	private IAssetFrameModel _model;

	public ExplainAssetFrameCanvas(Composite parent, int style) {
		super(parent, style);
	}

	public void setModel(IAssetFrameModel model) {
		_model = model;
		if (_model == null) {
			clear();
		} else {
			setImageInfo(model.getImageFile(), model.getFrameData(), false);
		}

		resetZoom();
	}

	public IAssetFrameModel getModel() {
		return _model;
	}
}
