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
package phasereditor.scene.ui.editor.outline;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.graphics.Image;

import phasereditor.assetpack.core.AssetFinder;
import phasereditor.assetpack.ui.AssetPackUI;
import phasereditor.bmpfont.core.BitmapFontModel.Align;
import phasereditor.bmpfont.core.BitmapFontModel.RenderArgs;
import phasereditor.bmpfont.core.BitmapFontRenderer;
import phasereditor.scene.core.BitmapTextComponent;
import phasereditor.scene.core.BitmapTextModel;
import phasereditor.scene.core.DynamicBitmapTextComponent;
import phasereditor.scene.core.ObjectModel;
import phasereditor.scene.core.OriginComponent;
import phasereditor.scene.core.TextualComponent;
import phasereditor.scene.core.TextureComponent;
import phasereditor.scene.core.TileSpriteComponent;
import phasereditor.scene.core.TileSpriteModel;
import phasereditor.scene.ui.ISceneObjectRendererContext;
import phasereditor.ui.BaseCanvas;
import phasereditor.ui.ImageProxy;
import phasereditor.ui.PhaserEditorUI;
import phasereditor.ui.ScaledImage;

/**
 * @author arian
 *
 */
public class SceneObjectRenderer {
	private ISceneObjectRendererContext _rendererContext;
	private Map<Object, ScaledImage> _imageCacheMap;
	private AssetFinder _finder;
	private AssetFinder _lastFinderSnapshot;

	public SceneObjectRenderer(ISceneObjectRendererContext rendererContext) {
		super();

		_rendererContext = rendererContext;

		_imageCacheMap = new HashMap<>();

		_finder = rendererContext.getAssetFinder();
		_lastFinderSnapshot = _finder;
	}

	public void dispose() {
		disposeImageCache();
	}

	private void disposeImageCache() {

		for (var scaled : _imageCacheMap.values()) {
			var image = scaled.getImage();
			if (!image.isDisposed()) {
				image.dispose();
			}
		}

		_imageCacheMap = new HashMap<>();
	}

	public ScaledImage getModelImageFromCache(Object model) {
		return _imageCacheMap.get(model);
	}

	private ScaledImage createBitmapTextImage(BitmapTextModel textModel) {
		var fontModel = textModel.getFontModel(_finder);

		if (fontModel == null) {
			return null;
		}

		var args = createBitmapTextRenderArgs(textModel);

		var metrics = fontModel.metrics(args);

		var width = metrics.getWidth();
		var height = metrics.getHeight();

		var buffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

		var g2 = buffer.createGraphics();

		BaseCanvas.prepareGC(g2);

		var asset = BitmapTextComponent.utils_getFont(textModel, _finder);

		var proxy = ImageProxy.get(asset.getTextureFile(), null);
		var fontTexture = proxy.getFileBufferedImage();

		if (textModel instanceof DynamicBitmapTextComponent) {
			// crop it

			var cropWidth = DynamicBitmapTextComponent.get_cropWidth(textModel);
			var cropHeight = DynamicBitmapTextComponent.get_cropHeight(textModel);

			// the text is not cropped if the width is 0 or the height is 0.

			if (cropWidth > 0 && cropHeight > 0) {

				var originX = OriginComponent.get_originX(textModel);
				var originY = OriginComponent.get_originY(textModel);

				var cropX = width * originX;
				var cropY = height * originY;

				g2.setClip((int) cropX, (int) cropY, cropWidth, cropHeight);

				var scrollX = DynamicBitmapTextComponent.get_scrollX(textModel);
				var scrollY = DynamicBitmapTextComponent.get_scrollY(textModel);

				g2.translate(-scrollX, -scrollY);

			}
		}

		try {

			fontModel.render(args, new BitmapFontRenderer() {

				@SuppressWarnings("hiding")
				@Override
				public void render(char c, int x, int y, int width, int height, int srcX, int srcY, int srcW,
						int srcH) {
					g2.drawImage(fontTexture, x, y, x + width, y + height, srcX, srcY, srcX + srcW, srcY + srcH, null);
				}
			});

		} catch (Exception e) {
			e.printStackTrace();
		}

		g2.dispose();

		return createScaledImage(buffer);
	}

	private static ScaledImage createScaledImage(BufferedImage buffer) {
		return ScaledImage.create(buffer, ImageProxy.MAX_SIZE);
	}

	private static RenderArgs createBitmapTextRenderArgs(BitmapTextModel textModel) {

		int fontSize = BitmapTextComponent.get_fontSize(textModel);
		var align = BitmapTextComponent.get_align(textModel);

		var args = new RenderArgs(TextualComponent.get_text(textModel));
		args.setFontSize(fontSize);
		args.setAlign(Align.values()[align]);
		args.setLetterSpacing(BitmapTextComponent.get_letterSpacing(textModel));

		return args;
	}

	/**
	 * We should use Java2D to create an empty image.
	 */
	@Deprecated
	public Image createImage(int width, int height) {
		return PhaserEditorUI.createTransparentSWTImage(_rendererContext.getDevice(), width, height);
	}

	public ScaledImage getTileSpriteImage(TileSpriteModel model) {
		ScaledImage scaledImage;

		if (asset_textureChanged(model)) {

			scaledImage = createTileSpriteTexture(model);

			var old = _imageCacheMap.put(model, scaledImage);

			if (old != null) {
				old.dispose();
			}
		} else {
			scaledImage = _imageCacheMap.get(model);
		}

		return scaledImage;
	}

	private ScaledImage createTileSpriteTexture(TileSpriteModel model) {
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

		return createScaledImage(buffer);
	}

	public ScaledImage getBitmapTextImage(BitmapTextModel model) {
		ScaledImage scaledImage;

		if (asset_bitmapFontChanged(model)) {

			scaledImage = createBitmapTextImage(model);

			var old = _imageCacheMap.put(model, scaledImage);

			if (old != null) {
				old.dispose();
			}
		} else {
			scaledImage = _imageCacheMap.get(model);
		}

		return scaledImage;
	}

	private boolean asset_textureChanged(ObjectModel model) {
		return assetChanged(TextureComponent.get_textureKey(model), TextureComponent.get_textureFrame(model));
	}

	private boolean asset_bitmapFontChanged(ObjectModel model) {
		return assetChanged(BitmapTextComponent.get_fontAssetKey(model));
	}

	private boolean assetChanged(String key) {
		return assetChanged(key, null);
	}

	private boolean assetChanged(String key, String frame) {
		var asset1 = _lastFinderSnapshot.findAssetKey(key, frame);
		var asset2 = _finder.findAssetKey(key, frame);

		var b = asset1 == null || asset2 == null || asset1 != asset2;

		return b;
	}
}
