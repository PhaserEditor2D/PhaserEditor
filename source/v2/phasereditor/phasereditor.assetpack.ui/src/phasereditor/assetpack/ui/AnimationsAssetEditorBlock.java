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

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;

import phasereditor.assetpack.core.AnimationsAssetModel;
import phasereditor.assetpack.core.IAssetFrameModel;
import phasereditor.assetpack.core.animations.AnimationFrameModel;
import phasereditor.ui.Colors;
import phasereditor.ui.FrameData;
import phasereditor.ui.ICanvasCellRenderer;
import phasereditor.ui.IEditorBlock;
import phasereditor.ui.ImageProxy;

/**
 * @author arian
 *
 */
public class AnimationsAssetEditorBlock extends AssetKeyEditorBlock<AnimationsAssetModel> {

	public static final RGB ANIMATION_COLOR = Colors.BLUEVIOLET.rgb;
	private List<IEditorBlock> _children;

	public AnimationsAssetEditorBlock(AnimationsAssetModel asset) {
		super(asset);
		_children = asset.getSubElements().stream()

				.map(anim -> AssetPackUI.getAssetEditorBlock(anim))

				.collect(toList());
	}
	
	@Override
	public String getKeywords() {
		return "animation";
	}

	@Override
	public boolean isTerminal() {
		return false;
	}

	@Override
	public List<IEditorBlock> getChildren() {
		return _children;
	}

	@Override
	public ICanvasCellRenderer getRenderer() {
		return new ICanvasCellRenderer() {

			@Override
			public void render(Canvas canvas, GC gc, int x, int y, int width, int height) {
				var model = getAssetKey().getAnimationsModel();
				
				if (model == null) {
					return;
				}

				var frameX = x;
				var frameY = y;

				var anims = model.getAnimations();

				var frames = new ArrayList<AnimationFrameModel>();
				
				if (anims.isEmpty()) {
					return;
				}

				for (var anim : anims) {
					var animFrames = anim.getFrames();
					var size = animFrames.size();

					if (size > 0) {
						frames.add(animFrames.get(0));
						frames.add(animFrames.get(size / 2));
						frames.add(animFrames.get(size - 1));
					}
				}

				var frameSize = width / 3;
				var minRowHeight = frameSize * 0.75;
				var rowHeight = (height - frameSize / 2) / anims.size();
				if (rowHeight < minRowHeight) {
					rowHeight = (int) minRowHeight;
				}

				var i = 0;
				while (i < frames.size()) {
					for (int j = 0; j < 3; j++) {
						var frame = frames.get(i);

						IAssetFrameModel asset = frame.getAssetFrame();

						if (asset != null) {
							var file = asset.getImageFile();
							var fd = asset.getFrameData();
							fd = adaptFrameData(fd);

							var proxy = ImageProxy.get(file, fd);

							if (proxy != null) {
								if (frameY + frameSize <= y + height) {
									//gc.setAlpha(105 + 150 / (j + 1));

									proxy.paintStripScaledInArea(gc,
											new Rectangle(frameX, frameY, frameSize, frameSize), true);
									gc.setAlpha(255);
								}
							}

							frameX += frameSize;
						}

						i++;
					}

					frameY += rowHeight;
					frameX = x;
				}

				gc.setAlpha(255);
			}

			private FrameData adaptFrameData(FrameData fd1) {
				var fd = fd1.clone();
				fd.srcSize.x = fd.src.width;
				fd.srcSize.y = fd.src.height;
				fd.dst = new Rectangle(0, 0, fd.src.width, fd.src.height);
				return fd;
			}
		};
	}

	@Override
	public String getSortName() {
		return "001";
	}

	@Override
	public RGB getColor() {
		return ANIMATION_COLOR;
	}

}
