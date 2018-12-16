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

import java.util.ArrayList;

import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;

import phasereditor.assetpack.core.AnimationsAssetModel;
import phasereditor.assetpack.core.IAssetFrameModel;
import phasereditor.assetpack.core.animations.AnimationFrameModel;
import phasereditor.ui.BaseTreeCanvasItemRenderer;
import phasereditor.ui.FrameData;
import phasereditor.ui.PhaserEditorUI;
import phasereditor.ui.TreeCanvas;
import phasereditor.ui.TreeCanvas.TreeCanvasItem;

/**
 * @author arian
 *
 */
public class AnimationsAssetTreeCanvasItemRenderer extends BaseTreeCanvasItemRenderer {

	public AnimationsAssetTreeCanvasItemRenderer(TreeCanvasItem item) {
		super(item);
	}

	@Override
	public void render(PaintEvent e, int index, int x, int y) {
		var animAssetModel = (AnimationsAssetModel) _item.getData();
		var animsModel = animAssetModel.getSubElements();

		var frames = new ArrayList<AnimationFrameModel>();

		for (var anim : animsModel) {
			var animFrames = anim.getFrames();
			var size = animFrames.size();

			if (size > 0) {
				frames.add(animFrames.get(0));
				frames.add(animFrames.get(size / 2));
				frames.add(animFrames.get(size - 1));
			}
		}

		var canvas = _item.getCanvas();

		var gc = e.gc;

		int rowHeight = computeRowHeight(canvas);

		int textHeight = gc.stringExtent("M").y;

		int textOffset = textHeight + 5;
		int imgSize = rowHeight - textOffset - 10;

		for (int k = 0; k < frames.size() / 3; k++) {
			int start = k * 3;
			for (int j = start + 2; j >= start; j--) {
				var frame = frames.get(j);

				IAssetFrameModel asset = frame.getAssetFrame();

				if (asset != null) {
					var file = asset.getImageFile();
					var fd = asset.getFrameData();
					fd = adaptFrameData(fd);

					var img = canvas.loadImage(file);
					if (img != null) {
						var scale = 1 + (start - j) * 0.15;
						Rectangle area = new Rectangle((int) (x + j * imgSize * 0.5), y + 5, (int) (imgSize * scale),
								(int) (imgSize * scale));
						if (area.x > e.width) {
							break;
						}

						var t = j % 3 + 1;
						gc.setAlpha(105 + 150 / t);
						PhaserEditorUI.paintScaledImageInArea(gc, img, fd, area);
					}
				}
			}
		}

		gc.setAlpha(255);

		gc.drawText(animAssetModel.getKey(), x + 5, y + rowHeight - textOffset, true);
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
	public Image get_DND_Image() {
		return null;
	}

	@Override
	public FrameData get_DND_Image_FrameData() {
		return null;
	}

}
