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
package phasereditor.scene.ui.editor.outline;

import java.awt.image.BufferedImage;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;

import phasereditor.assetpack.core.AssetFinder;
import phasereditor.assetpack.ui.AssetPackUI;
import phasereditor.scene.core.TextureComponent;
import phasereditor.scene.core.TileSpriteComponent;
import phasereditor.scene.core.TileSpriteModel;
import phasereditor.ui.BaseImageTreeCanvasItemRenderer;
import phasereditor.ui.ImageProxy;
import phasereditor.ui.ScaledImage;
import phasereditor.ui.TreeCanvas.TreeCanvasItem;

/**
 * @author arian
 *
 */
public class TileSpriteTreeItemRenderer extends BaseImageTreeCanvasItemRenderer {

	private AssetFinder _finder;

	public TileSpriteTreeItemRenderer(TreeCanvasItem item, AssetFinder finder) {
		super(item);
		_finder = finder;
	}

	@Override
	public ImageProxy get_DND_Image() {
		return null;
	}

	@Override
	protected void paintScaledInArea(GC gc, Rectangle area) {
		var model = (TileSpriteModel) _item.getData();

		// TODO: some caching here would be great!

		var scaledImage = createTileSpriteTexture(model, 256);

		if (scaledImage != null) {
			scaledImage.paintScaledInArea(gc, area, false);
			scaledImage.dispose();
		}
	}

	private ScaledImage createTileSpriteTexture(TileSpriteModel model, int size) {
		var assetFrame = TextureComponent.utils_getTexture(model, _finder);

		if (assetFrame == null) {
			return null;
		}

		var proxy = AssetPackUI.getImageProxy(assetFrame);

		var textureBufferedImage = proxy.getFileBufferedImage();

		var fd = assetFrame.getFrameData();

		var tileScaleX = TileSpriteComponent.get_tileScaleX(model);
		var tileScaleY = TileSpriteComponent.get_tileScaleY(model);

		var frameWidth = fd.src.width * tileScaleX;
		var frameHeight = fd.src.height * tileScaleY;

		var width = TileSpriteComponent.get_width(model);
		var height = TileSpriteComponent.get_height(model);

		var buffer = new BufferedImage((int) width, (int) height, BufferedImage.TYPE_INT_ARGB);
		var g2 = buffer.createGraphics();

		double x = 0;
		double y = 0;

		double xoffs = TileSpriteComponent.get_tilePositionX(model) % fd.src.width;
		double yoffs = TileSpriteComponent.get_tilePositionY(model) % fd.src.height;

		if (xoffs > 0) {
			x = -fd.src.width + xoffs;
		} else if (xoffs < 0) {
			x = xoffs;
		}

		if (yoffs > 0) {
			y = -fd.src.height + yoffs;
		} else if (yoffs < 0) {
			y = yoffs;
		}

		if (frameWidth > 0 && frameHeight > 0) {

			while (x < width) {

				var y2 = y;

				while (y2 < height) {
					g2.drawImage(textureBufferedImage,

							//

							(int) (x),

							(int) (y2),

							(int) (x + fd.dst.width * tileScaleX),

							(int) (y2 + fd.dst.height * tileScaleY),

							//

							fd.src.x,

							fd.src.y,

							fd.src.x + fd.src.width,

							fd.src.y + fd.src.height,

							null

					);

					y2 += frameHeight;
				}

				x += frameWidth;
			}
		}

		g2.dispose();

		return ScaledImage.create(buffer, size);
	}
}
