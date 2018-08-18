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

import phasereditor.assetpack.core.AudioAssetModel;
import phasereditor.audio.core.AudioCore;
import phasereditor.ui.TreeCanvas;
import phasereditor.ui.TreeCanvas.TreeCanvasItem;
import phasereditor.ui.TreeCanvas.TreeCanvasItemRenderer;

/**
 * @author arian
 *
 */
public class AudioTreeCanvasItemRenderer extends TreeCanvasItemRenderer {

	public AudioTreeCanvasItemRenderer(TreeCanvasItem item) {
		super(item);
	}

	@Override
	public void render(TreeCanvas canvas, PaintEvent e, int index, int x, int y) {
		var audio = (AudioAssetModel) _item.getData();
		
		var gc = e.gc;
		
		int rowHeight =  computeRowHeight(canvas);
		
		int textHeight = gc.stringExtent("M").y;
		
		int textOffset = textHeight + 5;
		int imgHeight = rowHeight - textOffset - 10;

		if (imgHeight >= 16) {
			IFile audioFile = null;
			for(var url : audio.getUrls()) {
				audioFile = audio.getFileFromUrl(url);
				if (audioFile != null) {
					break;
				}
			}
			
			var imgPath = AudioCore.getSoundWavesFile(audioFile, false);
			
			var img = canvas.loadImage(imgPath.toFile());
			
			if (img != null) {
				gc.setAlpha(150);
				gc.drawImage(img, 0, 0, img.getBounds().width, img.getBounds().height, x, y + 5, e.width - x - 5, imgHeight);
				gc.setAlpha(255);	
			}

			gc.drawText(audio.getKey(), x + 5, y + rowHeight - textOffset, true);
		} 
	}
	
	@Override
	public int computeRowHeight(TreeCanvas canvas) {
		return canvas.getImageSize() + 32;
	}

}
