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
package phasereditor.audio.ui;

import org.eclipse.core.resources.IFile;
import org.eclipse.swt.graphics.GC;

import phasereditor.audio.core.AudioCore;
import phasereditor.ui.BaseImageCanvas;
import phasereditor.ui.ICanvasCellRenderer;
import phasereditor.ui.VirtualImage;

/**
 * @author arian
 *
 */
public class AudioCellRenderer implements ICanvasCellRenderer {

	private IFile _audioFile;
	private int _padding;

	public AudioCellRenderer(IFile audioFile, int padding) {
		_audioFile = audioFile;
		_padding = padding;
	}

	@Override
	public void render(BaseImageCanvas canvas, GC gc, int x, int y, int width, int height) {
		var imgFile = AudioCore.getSoundWavesFile(_audioFile).toFile();

		var virtualImage = VirtualImage.get(imgFile, null);
		
		if (virtualImage != null) {
			var img = virtualImage.getImage();

			var b = img.getBounds();

			gc.setAlpha(150);

			gc.drawImage(img, 0, 0, b.width, b.height, x + _padding, y + _padding, width - _padding * 2,
					height - _padding * 2);

			gc.setAlpha(255);
		}
	}

}
