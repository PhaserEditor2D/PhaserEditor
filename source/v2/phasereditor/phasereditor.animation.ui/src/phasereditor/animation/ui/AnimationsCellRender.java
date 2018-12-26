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

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;

import phasereditor.assetpack.core.IAssetFrameModel;
import phasereditor.assetpack.core.animations.AnimationFrameModel;
import phasereditor.assetpack.core.animations.AnimationsModel;
import phasereditor.ui.BaseImageCanvas;
import phasereditor.ui.FrameData;
import phasereditor.ui.ICanvasCellRenderer;
import phasereditor.ui.ImageProxy;

/**
 * @author arian
 *
 */
public class AnimationsCellRender implements ICanvasCellRenderer {

	private AnimationsModel _model;
	private int _padding;

	public AnimationsCellRender(AnimationsModel model, int padding) {
		super();
		_model = model;
		_padding = padding;
	}

	@SuppressWarnings("all")
	@Override
	public void render(BaseImageCanvas canvas, GC gc, int x, int y, int width, int height) {
		x = x + _padding;
		y = y + _padding;
		width = width - _padding * 2;
		height = height - _padding * 2;

		var animsModel = _model.getAnimations();

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

		int imgSize = height;

		for (int k = 0; k < frames.size() / 3; k++) {
			int start = k * 3;
			for (int j = start + 2; j >= start; j--) {
				var frame = frames.get(j);

				IAssetFrameModel asset = frame.getAssetFrame();

				if (asset != null) {
					var file = asset.getImageFile();
					var fd = asset.getFrameData();
					fd = adaptFrameData(fd);

					var proxy = ImageProxy.get(file, fd);
					if (proxy != null) {
						var scale = 1 + (start - j) * 0.15;
						Rectangle area = new Rectangle((int) (x + j * imgSize * 0.5), y, (int) (imgSize * scale),
								(int) (imgSize * scale));
						if (area.x > width) {
							break;
						}

						var t = j % 3 + 1;
						gc.setAlpha(105 + 150 / t);
						proxy.paintScaledInArea(gc, area);
					}
				}
			}
		}

		gc.setAlpha(255);
	}

	private static FrameData adaptFrameData(FrameData fd1) {
		var fd = fd1.clone();
		fd.srcSize.x = fd.src.width;
		fd.srcSize.y = fd.src.height;
		fd.dst = new Rectangle(0, 0, fd.src.width, fd.src.height);
		return fd;
	}

}
