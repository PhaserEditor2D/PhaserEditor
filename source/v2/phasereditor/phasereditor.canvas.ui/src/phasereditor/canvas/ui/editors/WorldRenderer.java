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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.ui.statushandlers.StatusManager;
import org.eclipse.wb.swt.SWTResourceManager;
import org.json.JSONObject;

import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import phasereditor.assetpack.core.IAssetFrameModel;
import phasereditor.assetpack.core.ImageAssetModel;
import phasereditor.bmpfont.core.BitmapFontModel.RenderArgs;
import phasereditor.bmpfont.core.BitmapFontRenderer;
import phasereditor.canvas.core.Activator;
import phasereditor.canvas.core.AssetSpriteModel;
import phasereditor.canvas.core.AtlasSpriteModel;
import phasereditor.canvas.core.BaseObjectModel;
import phasereditor.canvas.core.BaseSpriteModel;
import phasereditor.canvas.core.BitmapTextModel;
import phasereditor.canvas.core.ButtonSpriteModel;
import phasereditor.canvas.core.GroupModel;
import phasereditor.canvas.core.ImageSpriteModel;
import phasereditor.canvas.core.MissingAssetSpriteModel;
import phasereditor.canvas.core.SpritesheetSpriteModel;
import phasereditor.canvas.core.TextModel;
import phasereditor.canvas.core.TileSpriteModel;
import phasereditor.canvas.core.TilemapSpriteModel;
import phasereditor.canvas.core.WorldModel;
import phasereditor.ui.BaseImageCanvas;
import phasereditor.ui.ColorUtil;

/**
 * @author arian
 *
 */
public class WorldRenderer {
	private ObjectCanvas2 _canvas;
	private Map<BaseObjectModel, float[]> _modelTransformMap;
	private Map<BaseObjectModel, float[]> _modelBoundsMap;

	public WorldRenderer(ObjectCanvas2 canvas) {
		super();

		_canvas = canvas;

		_modelTransformMap = new HashMap<>();
		_modelBoundsMap = new HashMap<>();
	}

	public void dispose() {
		for (var img : _imageCache.values()) {
			img.dispose();
		}
	}

	public void renderWorld(GC gc, Transform tx, WorldModel worldModel) {

		_modelTransformMap = new HashMap<>();
		_modelBoundsMap = new HashMap<>();

		var tx2 = newTx(gc, tx);

		try {

			{
				int dx = _canvas.getOffsetX();
				int dy = _canvas.getOffsetY();
				float scale = _canvas.getScale();

				tx2.translate(dx, dy);
				tx2.scale(scale, scale);
			}

			renderObject(gc, tx2, worldModel);

		} finally {
			tx2.dispose();
			gc.setTransform(null);
		}

		startDebug(gc, worldModel);
	}

	private void startDebug(GC gc, BaseObjectModel model) {
		var oldTx = new Transform(gc.getDevice());
		gc.getTransform(oldTx);

		var newTx = new Transform(gc.getDevice());
		gc.setTransform(newTx);

		debugObject(gc, model);

		gc.setTransform(oldTx);
	}

	private void debugObject(GC gc, BaseObjectModel model) {

		if (model instanceof GroupModel) {
			for (var model2 : ((GroupModel) model).getChildren()) {
				debugObject(gc, model2);
			}
		}

		var bounds = _modelBoundsMap.get(model);

		if (bounds != null) {
			gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_RED));

			var points = new int[bounds.length];

			for (int i = 0; i < points.length; i++) {
				points[i] = (int) bounds[i];
			}

			gc.drawPolygon(points);
		}
	}

	private void renderGroup(GC gc, Transform tx, GroupModel groupModel) {

		setObjectTransform(gc, tx, groupModel);

		for (var obj : groupModel.getChildren()) {

			var tx2 = newTx(gc, tx);

			renderObject(gc, tx2, obj);

			tx2.dispose();
		}
	}

	private void renderObject(GC gc, Transform tx, BaseObjectModel objModel) {

		if (!objModel.isEditorShow()) {
			return;
		}

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

			renderGroup(gc, tx, (GroupModel) objModel);

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

		setObjectTransform(gc, tx, model);

		if (model instanceof ImageSpriteModel) {

			renderTexture(gc, model, ((ImageSpriteModel) model).getAssetKey());

		} else if (model instanceof AtlasSpriteModel) {

			renderTexture(gc, model, ((AtlasSpriteModel) model).getAssetKey());

		} else if (model instanceof SpritesheetSpriteModel) {

			renderTexture(gc, model, ((SpritesheetSpriteModel) model).getAssetKey());

		} else if (model instanceof ButtonSpriteModel) {

			renderTexture(gc, model, ((ButtonSpriteModel) model).getAssetKey());

		} else if (model instanceof TileSpriteModel) {

			renderTileSprite(gc, (TileSpriteModel) model);

		} else if (model instanceof TilemapSpriteModel) {

			renderTilemapSprite(gc, (TilemapSpriteModel) model);

		} else if (model instanceof BitmapTextModel) {

			renderBitmapText(gc, tx, (BitmapTextModel) model);

		} else if (model instanceof MissingAssetSpriteModel) {

			renderMissingAsset(gc, (MissingAssetSpriteModel) model);

		} else if (model instanceof TextModel) {

			renderText(gc, (TextModel) model);

		}
	}

	private static class TextLine {

		public TextLine(String line, int x, int y, int width, int height) {
			this.line = line;
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;
		}

		public String line;
		public int x;
		public int y;
		public int width;
		@SuppressWarnings("unused")
		public int height;
	}

	private void renderText(GC gc, TextModel model) {

		var name = model.getStyleFont();
		int height = model.getStyleFontSize();
		var styleFill = model.getStyleFill();
		var styleBackground = model.getStyleBackgroundColor();
		var styleStroke = model.getStyleStroke();
		var styleStroleThickness = model.getStyleStrokeThickness();
		var styleAlign = model.getStyleAlign();

		var styleFillColor = SWTResourceManager.getColor(ColorUtil.web(styleFill).rgb);
		Color styleStrokeColor = styleStroke == null ? null
				: SWTResourceManager.getColor(ColorUtil.web(styleStroke).rgb);
		Color styleBackgroundColor = styleBackground == null ? null
				: SWTResourceManager.getColor(ColorUtil.web(styleBackground).rgb);

		// normal, bold, italic
		int style = SWT.NORMAL;

		if (model.getStyleFontWeight() == FontWeight.BOLD) {
			style = SWT.BOLD;
		}

		if (model.getStyleFontStyle() == FontPosture.ITALIC) {
			style |= SWT.ITALIC;
		}

		var oldFont = gc.getFont();
		Font font = new Font(gc.getDevice(), name, height, style);
		gc.setFont(font);

		gc.setForeground(styleFillColor);

		var text = model.getText();
		var lines = text.split("\\R");

		var textWidth = 0;
		var textHeight = 0;

		var textLines = new TextLine[lines.length];

		{
			var y = 0;

			for (int i = 0; i < lines.length; i++) {
				var line = lines[i];
				var m = gc.textExtent(line);
				textLines[i] = new TextLine(line, 0, y, m.x, m.y);
				y += m.y;

				textWidth = Math.max(m.x, textWidth);
			}

			textHeight = y;
		}

		if (styleAlign == TextAlignment.CENTER) {
			for (var textLine : textLines) {
				textLine.x = (textWidth - textLine.width) / 2;
			}
		} else if (styleAlign == TextAlignment.RIGHT) {
			for (var textLine : textLines) {
				textLine.x = textWidth - textLine.width;
			}
		}

		if (styleBackgroundColor != null) {
			gc.setBackground(styleBackgroundColor);
			gc.fillRectangle(0, 0, textWidth, textHeight);
		}

		setObjectBounds(gc, model, 0, 0, textWidth, textHeight);

		for (var textLine : textLines) {

			if (styleStroke == null || styleStroleThickness == 0) {
				gc.drawText(textLine.line, textLine.x, textLine.y, true);
			} else {

				var path = new Path(gc.getDevice());

				path.addString(textLine.line, textLine.x, textLine.y, font);

				gc.setBackground(styleFillColor);
				gc.fillPath(path);

				var lineWidth = gc.getLineWidth();
				gc.setLineWidth(styleStroleThickness);
				gc.setForeground(styleStrokeColor);

				gc.drawPath(path);

				gc.setLineWidth(lineWidth);

				path.dispose();
			}
		}

		gc.setFont(oldFont);
		font.dispose();
	}

	private void renderMissingAsset(GC gc, MissingAssetSpriteModel model) {

		gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_RED));
		gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_WHITE));

		String text;

		try {
			JSONObject data = model.getSrcData();
			JSONObject refObj = data.getJSONObject("asset-ref");

			StringBuilder sb = new StringBuilder();
			sb.append("MISSING ASSET\nsection=" + refObj.getString("section") + "\nkey=" + refObj.getString("asset"));
			if (refObj.has("sprite")) {
				sb.append("\nframe=" + refObj.getString("sprite"));
			}
			text = sb.toString();
			gc.drawText(text, 0, 0, false);

		} catch (Exception e) {
			StatusManager.getManager().handle(new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage(), e), 0);
			text = "Missing asset";
			gc.drawText(text, 0, 0, true);
		}

		var m = gc.textExtent(text);
		setObjectBounds(gc, model, 0, 0, m.x, m.y);
	}

	private void renderBitmapText(GC gc, Transform tx, BitmapTextModel model) {
		// TODO: we should do this in other place
		model.build();

		var fontModel = model.getFontModel();

		var img = loadImage(model.getAssetKey().getTextureFile());

		double scale = (double) model.getFontSize() / (double) fontModel.getInfoSize();

		var tx2 = newTx(gc, tx);
		tx2.scale((float) scale, (float) scale);

		setObjectTransform(gc, tx2, model);

		try {

			int[] size = new int[] { Integer.MIN_VALUE, Integer.MIN_VALUE };

			fontModel.render(new RenderArgs(model.getText()), new BitmapFontRenderer() {

				@Override
				public void render(char c, int x, int y, int srcX, int srcY, int srcW, int srcH) {
					gc.drawImage(img, srcX, srcY, srcW, srcH, x, y, srcW, srcH);
					size[0] = Math.max(x + srcW, size[0]);
					size[1] = Math.max(y + srcH, size[1]);
				}

			});

			setObjectBounds(gc, model, 0, 0, size[0], size[1]);

		} catch (Exception e) {
			e.printStackTrace();
		}

		tx2.dispose();

		gc.setTransform(tx);

	}

	private static Transform newTx(GC gc, Transform tx) {
		var txElements = new float[6];
		tx.getElements(txElements);
		return new Transform(gc.getDevice(), txElements);
	}

	private Image createTilemapImage(TilemapSpriteModel model) {

		var asset = model.getAssetKey();

		int[][] map = asset.getCsvData();

		int tileW = model.getTileWidth();
		int tileH = model.getTileHeight();

		if (map.length == 0) {
			return null;
		}

		var temp = new Image(_canvas.getDisplay(), 1, 1);
		var tempData = temp.getImageData();

		int width = map[0].length * tileW;
		int height = map.length * tileH;
		var data = new ImageData(width, height, tempData.depth, tempData.palette);
		data.alphaData = new byte[width * height];

		var tilemapImage = new Image(_canvas.getDisplay(), data);

		temp.dispose();

		GC gc = new GC(tilemapImage);

		try {

			ImageAssetModel tilesetAsset = model.getTilesetImage();

			if (tilesetAsset != null) {
				IFile file = tilesetAsset.getUrlFile();

				var img = loadImage(file);

				if (img == null) {
					return null;
				}

				BaseImageCanvas.prepareGC(gc);

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

				return tilemapImage;
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

			return null;

		} finally {
			gc.dispose();
		}
	}

	private Map<String, Image> _imageCache = new HashMap<>();

	private void renderTilemapSprite(GC gc, TilemapSpriteModel model) {

		Image img = getTilemapImage(model);

		if (img == null) {
			gc.drawString("Cannot render tilemap '" + model.getEditorName() + "'", 0, 0);
		} else {
			gc.drawImage(img, 0, 0);
			var b = img.getBounds();
			setObjectBounds(gc, model, 0, 0, b.width, b.height);
		}

	}

	public Image getTilemapImage(TilemapSpriteModel model) {

		Image img = null;

		if (model.getTilesetImage() != null) {
			var tilesetImageFile = model.getTilesetImage().getUrlFile();
			if (tilesetImageFile != null) {
				var tilesetImage = loadImage(tilesetImageFile);
				if (tilesetImage != null) {
					var mapFile = model.getAssetKey().getUrlFile();
					if (mapFile != null) {

						var key = tilesetImageFile.getFullPath().toPortableString() + "$"
								+ tilesetImageFile.getLocalTimeStamp() + "$" + mapFile.getFullPath().toPortableString()
								+ "$" + mapFile.getLocalTimeStamp() + "$" + model.getTileWidth() + "$"
								+ model.getTileHeight();

						if (_imageCache.containsKey(key)) {
							img = _imageCache.get(key);
						} else {
							img = createTilemapImage(model);
							if (img != null) {
								_imageCache.put(key, img);
							}
						}
					}
				}
			}
		}

		return img;
	}

	private Image loadImage(IFile file) {
		return _canvas.loadImage(file);
	}

	private static Point getTextureSize(BaseSpriteModel model) {

		// TODO: implement the rest of the models

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
		// var clipping = gc.getClipping();
		// gc.setClipping(0, 0, (int) width, (int) height);

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

		setObjectBounds(gc, tileModel, 0f, 0f, (float) width, (float) height);

		// gc.setClipping(clipping);
	}

	private void renderTexture(GC gc, BaseObjectModel model, IAssetFrameModel assetFrame) {
		var img = _canvas.loadImage(assetFrame.getImageFile());

		var fd = assetFrame.getFrameData();

		gc.drawImage(img, fd.src.x, fd.src.y, fd.src.width, fd.src.height, fd.dst.x, fd.dst.y, fd.dst.width,
				fd.dst.height);

		setObjectBounds(gc, model, 0, 0, fd.srcSize.x, fd.srcSize.y);
	}

	private void setObjectTransform(GC gc, Transform tx, BaseObjectModel model) {
		gc.setTransform(tx);

		var txElems = _modelTransformMap.get(model);

		if (txElems == null) {
			txElems = new float[6];
			_modelTransformMap.put(model, txElems);
		}

		tx.getElements(txElems);
	}

	private void setObjectBounds(GC gc, BaseObjectModel model, float x, float y, float width, float height) {
		var txElems = _modelTransformMap.get(model);
		var tx = new Transform(gc.getDevice(), txElems);

		// tx.invert();

		var points = new float[] {

				x, y,

				x + width, y,

				x + width, y + height,

				x, y + height

		};

		tx.transform(points);

		tx.dispose();

		_modelBoundsMap.put(model, points);

	}
}
