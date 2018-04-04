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
package phasereditor.assetpack.ui.preview;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.swt.graphics.Rectangle;

import phasereditor.assetpack.core.IAssetFrameModel;
import phasereditor.canvas.core.AnimationModel;
import phasereditor.ui.animations.IFramesAnimationModel;

public class SpritesheetAnimationModel implements IFramesAnimationModel {
	private AnimationModel _base;
	private ArrayList<Rectangle> _frames;

	public SpritesheetAnimationModel(AnimationModel base) {
		super();
		_base = base;
		_frames = new ArrayList<>();
		for (IAssetFrameModel assetFrame : _base.getFrames()) {
			_frames.add(assetFrame.getFrameData().src);
		}
	}

	@Override
	public List<Rectangle> getFrames() {
		return _frames;
	}

	@Override
	public boolean isLoop() {
		return _base.isLoop();
	}

	@Override
	public int getFrameRate() {
		return _base.getFrameRate();
	}
	
	public void setFrameRates(int fps) {
		_base.setFrameRate(fps);
	}

	@Override
	public IFile getImageFile() {
		return _base.getFrames().get(0).getImageFile();
	}
}