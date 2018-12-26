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

import java.util.List;

import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Rectangle;

import phasereditor.assetpack.core.IAssetFrameModel;
import phasereditor.assetpack.core.animations.AnimationFrameModel;
import phasereditor.assetpack.core.animations.AnimationModel;
import phasereditor.ui.BaseTreeCanvasItemRenderer;
import phasereditor.ui.FrameData;
import phasereditor.ui.ImageProxy;
import phasereditor.ui.TreeCanvas;
import phasereditor.ui.TreeCanvas.TreeCanvasItem;

/**
 * @author arian
 *
 */
public class AnimationTreeCanvasItemRenderer extends BaseTreeCanvasItemRenderer {

	public AnimationTreeCanvasItemRenderer(TreeCanvasItem item) {
		super(item);
	}

	@Override
	public void render(PaintEvent e, int index, int x, int y) {
		var anim = (AnimationModel) _item.getData();
		
		var canvas = _item.getCanvas();

		var gc = e.gc;

		int rowHeight = computeRowHeight(canvas);

		int textHeight = gc.stringExtent("M").y;

		int textOffset = textHeight + 5;
		int imgHeight = rowHeight - textOffset - 10;

		List<AnimationFrameModel> frames = anim.getFrames();

		for (int j = 0; j < frames.size(); j++) {
			var frame = frames.get(j);

			IAssetFrameModel asset = frame.getAssetFrame();

			if (asset != null) {
				var file = asset.getImageFile();
				var fd = asset.getFrameData();
				fd = adaptFrameData(fd);
				
				
				var proxy = ImageProxy.get(file, fd);
				if (proxy != null) {
					var area = new Rectangle(x + j * imgHeight, y + 5, imgHeight, imgHeight);

					if (area.x > e.width) {
						break;
					}

					proxy.paintScaledInArea(gc, area);
				}
			}
		}

		gc.drawText(anim.getKey(), x + 5, y + rowHeight - textOffset, true);

	}

	private static FrameData adaptFrameData(FrameData fd1) {
		var fd = fd1.clone();
		fd.srcSize.x = fd.src.width;
		fd.srcSize.y = fd.src.height;
		fd.dst = new Rectangle(0, 0, fd.src.width, fd.src.height);
		return fd;
	}

	@Override
	public int computeRowHeight(TreeCanvas canvas) {
		return canvas.getImageSize() + 32;
	}

	@Override
	public ImageProxy get_DND_Image() {
		return null;
	}
}
