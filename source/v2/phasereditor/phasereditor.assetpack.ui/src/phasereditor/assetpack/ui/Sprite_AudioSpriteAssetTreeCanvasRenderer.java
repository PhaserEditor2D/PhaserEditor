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
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;

import phasereditor.assetpack.core.AudioSpriteAssetModel;
import phasereditor.ui.TreeCanvas.TreeCanvasItem;

/**
 * @author arian
 *
 */
public class Sprite_AudioSpriteAssetTreeCanvasRenderer extends AudioSpriteAssetTreeCanvasItemRenderer {

	public Sprite_AudioSpriteAssetTreeCanvasRenderer(TreeCanvasItem item) {
		super(item);
	}

	@Override
	protected void renderImage(GC gc, int dstX, int dstY, int dstWidth, int dstHeight) {
		var duration = getDuration();

		if (duration == 0) {
			return;
		}

		var sprite = (AudioSpriteAssetModel.AssetAudioSprite) _item.getData();
		var b = getImage().getBounds();
		var srcX = sprite.getStart() / duration * b.width;
		var srcWidth = sprite.getEnd() / duration * b.width - srcX;
		
		gc.drawImage(getImage(), (int) srcX, 0, (int) srcWidth, b.height, dstX, dstY + 2, dstWidth, dstHeight - 2);

		gc.setAlpha(100);
		gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_WHITE));
		gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_BLACK));
		gc.fillGradientRectangle(dstX, dstY + 2, dstWidth, dstHeight - 2, false);
		gc.setAlpha(255);
	}

	@Override
	protected IFile getAudioFile(Object data) {
		return super.getAudioFile(((AudioSpriteAssetModel.AssetAudioSprite) data).getAsset());
	}

	@Override
	protected String getAudioLabel(Object data) {
		return ((AudioSpriteAssetModel.AssetAudioSprite) data).getKey();
	}

}
