// The MIT License (MIT)
//
// Copyright (c) 2015, 2019 Arian Fornaris
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

import java.util.List;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;

import phasereditor.assetpack.core.AnimationsAssetModel.AnimationModel_in_AssetPack;
import phasereditor.ui.FrameData;
import phasereditor.ui.ICanvasCellRenderer;
import phasereditor.ui.IEditorBlock;
import phasereditor.ui.ImageProxy;

/**
 * @author arian
 *
 */
public class AnimationAssetEditorBlock extends AssetKeyEditorBlock<AnimationModel_in_AssetPack> {

	public AnimationAssetEditorBlock(AnimationModel_in_AssetPack assetKey) {
		super(assetKey);
	}
	
	@Override
	public String getKeywords() {
		return "animation";
	}

	@Override
	public boolean isTerminal() {
		return true;
	}

	@Override
	public List<IEditorBlock> getChildren() {
		return null;
	}

	@Override
	public ICanvasCellRenderer getRenderer() {
		return new ICanvasCellRenderer() {

			private FrameData adaptFrameData(FrameData fd1) {
				var fd = fd1.clone();
				fd.srcSize.x = fd.src.width;
				fd.srcSize.y = fd.src.height;
				fd.dst = new Rectangle(0, 0, fd.src.width, fd.src.height);
				return fd;
			}

			@Override
			public void render(Canvas canvas, GC gc, int x, int y, int width, int height) {

				var frames = getAssetKey().getFrames();

				if (frames.isEmpty()) {
					return;
				}

				var size = width / 3;
				var x2 = x;
				var y2 = y + height / 2 - size / 2;

				for (var f : new float[] { 0, 0.5f, 1f }) {
					var frame = frames.get((int) ((frames.size() - 1) * f));
					var asset = frame.getAssetFrame();
					var file = asset.getImageFile();
					var fd = adaptFrameData(asset.getFrameData());
					var proxy = ImageProxy.get(file, fd);
					if (proxy != null) {
						proxy.paintScaledInArea(gc, new Rectangle(x2, y2, size, size));
					}
					x2 += size;
				}
			}
		};
	}

	@Override
	public String getSortName() {
		return "001";
	}

	@Override
	public RGB getColor() {
		return AnimationsAssetEditorBlock.ANIMATION_COLOR;
	}

}
