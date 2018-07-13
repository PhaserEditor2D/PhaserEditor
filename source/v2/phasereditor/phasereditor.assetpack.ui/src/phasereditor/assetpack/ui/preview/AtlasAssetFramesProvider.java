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
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

import phasereditor.assetpack.core.AtlasAssetModel;
import phasereditor.assetpack.core.AtlasAssetModel.Frame;
import phasereditor.ui.FrameGridCanvas.IFrameProvider;

/**
 * @author arian
 *
 */
public class AtlasAssetFramesProvider implements IFrameProvider {

	private AtlasAssetModel _asset;
	private List<Frame> _frames;
	private Image _image;

	public AtlasAssetFramesProvider(AtlasAssetModel asset) {
		super();
		_asset = asset;
		var frames = asset.getAtlasFrames();
		_frames = frames.stream().sorted((f1, f2) -> f1.getKey().toLowerCase().compareTo(f2.getKey().toLowerCase()))
				.collect(toList());

		IFile file = _asset.getTextureFile();
		if (file != null) {
			_image = new Image(Display.getDefault(), file.getLocation().toFile().getAbsolutePath());
		}
	}

	@Override
	public int getFrameCount() {
		return _frames.size();
	}

	@Override
	public Rectangle getFrameRectangle(int index) {
		return _frames.get(index).getFrameData().src;
	}

	@Override
	public Image getFrameImage(int index) {
		return _image;
	}

	@Override
	public String getFrameTooltip(int index) {
		var rect = getFrameRectangle(index);
		return rect.width + "x" + rect.height;
	}

}
