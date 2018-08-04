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

import static java.util.stream.Collectors.toList;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.swt.graphics.Rectangle;

import phasereditor.assetpack.core.MultiAtlasAssetModel;
import phasereditor.assetpack.core.MultiAtlasAssetModel.Frame;
import phasereditor.ui.FrameGridCanvas.IFrameProvider;

/**
 * @author arian
 *
 */
public class MultiAtlasFrameProvider implements IFrameProvider {

	private List<Frame> _frames;

	public MultiAtlasFrameProvider(MultiAtlasAssetModel model) {
		super();
		_frames = model.getSubElements();
		_frames = _frames.stream().sorted((f1, f2) -> f1.getKey().toLowerCase().compareTo(f2.getKey().toLowerCase()))
				.collect(toList());
	}

	@Override
	public String getFrameTooltip(int index) {
		return null;
	}

	@Override
	public Rectangle getFrameRectangle(int index) {
		return getFrameObject(index).getFrameData().src;
	}

	@Override
	public IFile getFrameImageFile(int index) {
		var frame = getFrameObject(index);
		var file = frame.getAsset().getFileFromUrl(frame.getTextureUrl());
		return file;
	}

	@Override
	public int getFrameCount() {
		return _frames.size();
	}

	@Override
	public MultiAtlasAssetModel.Frame getFrameObject(int index) {
		return _frames.get(index);
	}
}
