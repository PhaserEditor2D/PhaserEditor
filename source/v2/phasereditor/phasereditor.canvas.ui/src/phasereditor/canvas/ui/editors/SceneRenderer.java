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
package phasereditor.canvas.ui.editors;

import org.eclipse.core.resources.IFile;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.wb.swt.SWTResourceManager;

import phasereditor.assetpack.core.IAssetFrameModel;
import phasereditor.assetpack.core.ImageAssetModel;
import phasereditor.canvas.core.AssetSpriteModel;
import phasereditor.canvas.core.AtlasSpriteModel;
import phasereditor.canvas.core.BaseObjectModel;
import phasereditor.canvas.core.BaseSpriteModel;
import phasereditor.canvas.core.GroupModel;
import phasereditor.canvas.core.ImageSpriteModel;
import phasereditor.canvas.core.SpritesheetSpriteModel;
import phasereditor.canvas.core.TileSpriteModel;
import phasereditor.canvas.core.TilemapSpriteModel;
import phasereditor.canvas.core.WorldModel;

/**
 * @author arian
 *
 */
public class SceneRenderer {
	private ObjectCanvas2 _canvas;

	public SceneRenderer(ObjectCanvas2 canvas) {
		super();
		_canvas = canvas;
	}

	public void renderWorld(GC gc, WorldModel worldModel) {

		var tx = new Transform(_canvas.getDisplay());

		{
			int dx = _canvas.getOffsetX();
			int dy = _canvas.getOffsetY();
			float scale = _canvas.getScale();

			tx.translate(dx, dy);
			tx.scale(scale, scale);
		}

		renderObject(gc, tx, worldModel);

		tx.dispose();
	}

	private void renderGroup(GC gc, Transform tx, WorldModel groupModel) {

		var txElements = new float[6];
		tx.getElements(txElements);

		for (var obj : groupModel.getChildren()) {

			var tx2 = new Transform(gc.getDevice(), txElements);

			renderObject(gc, tx2, obj);

			tx2.dispose();
		}
	}

	private void renderObject(GC gc, Transform tx, BaseObjectModel objModel) {

		{
			// position
			tx.translate((float) objModel.getX(), (float) objModel.getY());
		}

		{
			// rotation
			var a = objModel.getAngle();
			tx.rotate((float) a);
		}

		{
			// pivot
			var px = objModel.getPivotX();
			var py = objModel.getPivotY();
			tx.translate((float) (-px * objModel.getScaleX()), (float) (-py * objModel.getScaleY()));
		}

		{
			// scale
			tx.scale((float) objModel.getScaleX(), (float) objModel.getScaleY());
		}

		if (objModel instanceof GroupModel) {

			renderGroup(gc, tx, (WorldModel) objModel);

		} else if (objModel instanceof BaseSpriteModel) {

			renderSprite(gc, tx, (BaseSpriteModel) objModel);

		}
	}

	private void renderSprite(GC gc, Transform tx, BaseSpriteModel model) {
		{
			// anchor
			double anchorX = model.getAnchorX();
			double anchorY = model.getAnchorY();

			var size = getTextureSize(model);

			double x = -size.x * anchorX;
			double y = -size.y * anchorY;
			tx.translate((float) x, (float) y);
		}

		gc.setTransform(tx);

		if (model instanceof ImageSpriteModel) {

			renderTexture(gc, ((ImageSpriteModel) model).getAssetKey());

		} else if (model instanceof AtlasSpriteModel) {

			renderTexture(gc, ((AtlasSpriteModel) model).getAssetKey());

		} else if (model instanceof SpritesheetSpriteModel) {

			renderTexture(gc, ((SpritesheetSpriteModel) model).getAssetKey());

		} else if (model instanceof TileSpriteModel) {

			renderTileSprite(gc, (TileSpriteModel) model);

		} else if (model instanceof TilemapSpriteModel) {

			renderTilemapSprite(gc, (TilemapSpriteModel) model);
		}

	}

	private void renderTilemapSprite(GC gc, TilemapSpriteModel model) {

		var asset = model.getAssetKey();

		int[][] map = asset.getCsvData();

		int tileW = model.getTileWidth();
		int tileH = model.getTileHeight();

		if (map.length == 0) {

			gc.drawText("Tilemap CSV: empty map", 0, 0, true);

		} else {
			ImageAssetModel tilesetAsset = model.getTilesetImage();

			if (tilesetAsset != null) {
				IFile file = tilesetAsset.getUrlFile();

				var img = loadImage(file);

				if (img != null) {
					var imgSize = img.getBounds();

					for (int i = 0; i < map.length; i++) {
						int[] row = map[i];

						for (int j = 0; j < row.length; j++) {
							int frame = map[i][j];
							if (frame < 0) {
								// nothing, empty space
							} else {

								int tilesetW = imgSize.width;
								int srcX = frame * tileW % tilesetW;
								int srcY = frame * tileW / tilesetW * tileH;

								gc.drawImage(img, srcX, srcY, tileW, tileH, j * tileW, i * tileH, tileW, tileH);
							}
						}
					}

					return;
				}
			}

			// render fallback image

			int max = 0;
			for (int i = 0; i < map.length; i++) {
				for (int j = 0; j < map[i].length; j++) {
					max = Math.max(map[i][j], max);
				}
			}

			var colors = new Color[max];
			for (int i = 0; i < max; i++) {
				java.awt.Color c = java.awt.Color.getHSBColor((float) i / (float) max, 0.85f, 1.0f);
				colors[i] = SWTResourceManager.getColor(c.getRed(), c.getGreen(), c.getBlue());
			}

			for (int i = 0; i < map.length; i++) {
				int[] row = map[i];

				for (int j = 0; j < row.length; j++) {

					int frame = map[i][j];

					if (frame < 0) {
						continue;
					}

					int x = j * tileW;
					int y = i * tileH;

					// paint map with colors
					Color c = colors[frame % colors.length];
					gc.setBackground(c);
					gc.fillRectangle(x, y, tileW + 1, tileH + 1);
				}
			}
		}

	}

	private Image loadImage(IFile file) {
		return _canvas.loadImage(file);
	}

	private static Point getTextureSize(BaseSpriteModel model) {

		if (model instanceof AssetSpriteModel) {
			var key = ((AssetSpriteModel<?>) model).getAssetKey();

			if (key instanceof ImageAssetModel) {
				key = ((ImageAssetModel) key).getFrame();
			}

			if (key instanceof IAssetFrameModel) {
				var fd = ((IAssetFrameModel) key).getFrameData();
				return fd.srcSize;
			}
		}

		return new Point(0, 0);
	}

	private void renderTileSprite(GC gc, TileSpriteModel tileModel) {

		var assetFrame = (IAssetFrameModel) tileModel.getAssetKey();

		var img = _canvas.loadImage(assetFrame.getImageFile());

		var fd = assetFrame.getFrameData();

		var tileScaleX = tileModel.getTileScaleX();
		var tileScaleY = tileModel.getTileScaleY();

		var frameWidth = fd.srcSize.x * tileScaleX;
		var frameHeight = fd.srcSize.y * tileScaleY;

		var width = tileModel.getWidth();
		var height = tileModel.getHeight();

		double x = 0;
		double y = 0;

		double xoffs = tileModel.getTilePositionX() % fd.srcSize.x;
		double yoffs = tileModel.getTilePositionY() % fd.srcSize.y;

		if (xoffs > 0) {
			x = -fd.srcSize.x + xoffs;
		} else if (xoffs < 0) {
			x = xoffs;
		}

		if (yoffs > 0) {
			y = -fd.srcSize.y + yoffs;
		} else if (yoffs < 0) {
			y = yoffs;
		}

		// x = x * tileScaleX;
		// y = y * tileScaleY;

		// TODO: do not do clipping when it is not needed. Or better, do some match and
		// just paint the portion it needs!
		gc.setClipping(0, 0, (int) width, (int) height);

		if (frameWidth > 0 && frameHeight > 0) {

			while (x < width) {

				var y2 = y;

				while (y2 < height) {
					gc.drawImage(img,

							fd.src.x,

							fd.src.y,

							fd.src.width,

							fd.src.height,

							//

							(int) (x + fd.dst.x * tileScaleX),

							(int) (y2 + fd.dst.y * tileScaleY),

							(int) (fd.dst.width * tileScaleX),

							(int) (fd.dst.height * tileScaleY));

					y2 += frameHeight;
				}

				x += frameWidth;
			}
		}

		gc.setClipping((Rectangle) null);
	}

	private void renderTexture(GC gc, IAssetFrameModel assetFrame) {
		var img = _canvas.loadImage(assetFrame.getImageFile());

		var fd = assetFrame.getFrameData();

		gc.drawImage(img, fd.src.x, fd.src.y, fd.src.width, fd.src.height, fd.dst.x, fd.dst.y, fd.dst.width,
				fd.dst.height);
	}
}
