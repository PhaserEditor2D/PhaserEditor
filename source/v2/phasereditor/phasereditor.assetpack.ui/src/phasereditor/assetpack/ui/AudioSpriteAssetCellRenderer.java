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
package phasereditor.assetpack.ui;

import org.eclipse.core.resources.IFile;
import org.eclipse.swt.graphics.GC;

import phasereditor.assetpack.core.AudioSpriteAssetModel;
import phasereditor.audio.core.AudioCore;
import phasereditor.ui.BaseImageCanvas;
import phasereditor.ui.ICanvasCellRenderer;
import phasereditor.ui.VirtualImage;

/**
 * @author arian
 *
 */
public class AudioSpriteAssetCellRenderer implements ICanvasCellRenderer {
	private AudioSpriteAssetModel _model;
	private double _duration;
	private IFile _audioFile;
	private int _padding;

	public AudioSpriteAssetCellRenderer(AudioSpriteAssetModel model, int padding) {
		_model = model;

		for (var url : _model.getUrls()) {
			var file = _model.getFileFromUrl(url);
			if (file != null) {
				_audioFile = file;
				break;
			}
		}

		if (_audioFile == null) {
			_duration = 0;
		} else {
			_duration = AudioCore.getSoundDuration(_audioFile);
		}

		_padding = padding;
	}

	@SuppressWarnings("all")
	@Override
	public void render(BaseImageCanvas canvas, GC gc, int x, int y, int width, int height) {
		if (_duration == 0) {
			return;
		}

		x += _padding;
		y += _padding;
		width -= _padding * 2;
		height -= _padding * 2;

		var sprites = _model.getSpriteMap();

		var imgFile = AudioCore.getSoundWavesFile(_audioFile);

		var virtualImage = VirtualImage.get(imgFile.toFile(), null);
		
		if (virtualImage == null) {
			return;
		}
		
		var img = virtualImage.getImage();


		var b = img.getBounds();

		var count = sprites.size();
		var spacing = 3;

		if (spacing * count > width / 3) {
			spacing = 0;
		}

		var dstWidth2 = width / (double) count - spacing;

		var lastDstX = x;

		gc.setAlpha(150);
		
		for (var sprite : sprites) {

			var startFactor = sprite.getStart() / _duration;
			var endFactor = sprite.getEnd() / _duration;

			var srcX1 = startFactor * b.width;
			var srcX2 = endFactor * b.width;
			var srcWidth = srcX2 - srcX1;

			gc.drawImage(img, (int) srcX1, 0, (int) srcWidth, b.height, lastDstX, y, (int) dstWidth2, height);

			lastDstX += dstWidth2 + spacing;
		}
		gc.setAlpha(150);
	}

}
