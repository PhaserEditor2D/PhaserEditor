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
package phasereditor.animation.ui;

import org.eclipse.core.resources.IFile;
import org.eclipse.swt.graphics.Rectangle;

import phasereditor.assetpack.core.animations.AnimationFrameModel;
import phasereditor.assetpack.core.animations.AnimationModel;
import phasereditor.ui.FrameGridCanvas.IFrameProvider;

/**
 * @author arian
 *
 */
public class AnimationFrameProvider implements IFrameProvider {

	private AnimationModel _model;

	public AnimationFrameProvider(AnimationModel model) {
		super();
		_model = model;
	}

	@Override
	public int getFrameCount() {
		return _model.getFrames().size();
	}

	@Override
	public Rectangle getFrameRectangle(int index) {
		var frame = getFrameObject(index).getFrameAsset();

		if (frame == null) {
			return null;
		}

		return frame.getFrameData().src;
	}

	@Override
	public IFile getFrameImageFile(int index) {
		var frame = getFrameObject(index).getFrameAsset();

		if (frame == null) {
			return null;
		}

		return frame.getImageFile();
	}

	@Override
	public String getFrameTooltip(int index) {
		return null;
	}
	
	@Override
	public AnimationFrameModel getFrameObject(int index) {
		return  _model.getFrames().get(index);
	}

}
