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
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Image;

import phasereditor.assetpack.core.AudioAssetModel;
import phasereditor.audio.core.AudioCore;
import phasereditor.ui.BaseTreeCanvasItemRenderer;
import phasereditor.ui.FrameData;
import phasereditor.ui.TreeCanvas;
import phasereditor.ui.TreeCanvas.TreeCanvasItem;

/**
 * @author arian
 *
 */
public class AudioTreeCanvasItemRenderer extends BaseTreeCanvasItemRenderer {

	private Image _image;
	private String _label;

	public AudioTreeCanvasItemRenderer(TreeCanvasItem item) {
		super(item);

		var canvas = item.getCanvas();

		var data = _item.getData();

		IFile audioFile;
		_label = "";

		if (data instanceof AudioAssetModel) {
			var asset = (AudioAssetModel) data;

			_label = asset.getKey();

			audioFile = null;

			for (var url : asset.getUrls()) {
				audioFile = asset.getFileFromUrl(url);
				if (audioFile != null) {
					break;
				}
			}

		} else {
			audioFile = (IFile) data;
			_label = audioFile.getName();
		}

		var imgPath = AudioCore.getSoundWavesFile(audioFile, false);

		_image = canvas.loadImage(imgPath.toFile());
	}

	public String getLabel() {
		return _label;
	}

	public void setLabel(String label) {
		_label = label;
	}

	@Override
	public void render(PaintEvent e, int index, int x, int y) {
		var canvas = _item.getCanvas();

		var gc = e.gc;

		int rowHeight = computeRowHeight(canvas);

		int textHeight = gc.stringExtent("M").y;

		int textOffset = textHeight + 5;
		int imgHeight = rowHeight - textOffset - 10;

		if (imgHeight >= 16) {
			if (_image != null) {
				int w = e.width - x - 5;
				if (w > 0) {
					gc.setAlpha(150);
					gc.drawImage(_image, 0, 0, _image.getBounds().width, _image.getBounds().height, x, y + 5, w,
							imgHeight);
					gc.setAlpha(255);
				}

			}

			gc.drawText(_label, x + 5, y + rowHeight - textOffset, true);
		}
	}

	@Override
	public int computeRowHeight(TreeCanvas canvas) {
		return canvas.getImageSize() + 32;
	}

	@Override
	public Image get_DND_Image() {
		return _image;
	}

	@Override
	public FrameData get_DND_Image_FrameData() {
		return FrameData.fromImage(_image);
	}

}
