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
package phasereditor.scene.ui.editor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.widgets.Display;

import phasereditor.assetpack.core.IAssetFrameModel;
import phasereditor.bmpfont.core.BitmapFontModel.Align;
import phasereditor.bmpfont.core.BitmapFontModel.RenderArgs;
import phasereditor.bmpfont.core.BitmapFontRenderer;
import phasereditor.scene.core.BaseSpriteModel;
import phasereditor.scene.core.BitmapTextComponent;
import phasereditor.scene.core.BitmapTextModel;
import phasereditor.scene.core.DynamicBitmapTextComponent;
import phasereditor.scene.core.EditorComponent;
import phasereditor.scene.core.FlipComponent;
import phasereditor.scene.core.ObjectModel;
import phasereditor.scene.core.OriginComponent;
import phasereditor.scene.core.ParentComponent;
import phasereditor.scene.core.SceneModel;
import phasereditor.scene.core.TextualComponent;
import phasereditor.scene.core.TextureComponent;
import phasereditor.scene.core.TileSpriteComponent;
import phasereditor.scene.core.TileSpriteModel;
import phasereditor.scene.core.TransformComponent;
import phasereditor.ui.BaseImageCanvas;

/**
 * @author arian
 *
 */
public class SceneObjectRenderer {
	private SceneCanvas _canvas;
	private Map<ObjectModel, float[]> _modelMatrixMap;
	private Map<ObjectModel, float[]> _modelBoundsMap;
	private Map<ObjectModel, float[]> _modelChildrenBoundsMap;
	private Map<Object, Image> _imageCacheMap;

	private boolean _debug;
	private List<Runnable> _postPaintActions;

	public SceneObjectRenderer(SceneCanvas canvas) {
		super();

		_canvas = canvas;

		_modelMatrixMap = new HashMap<>();
		_modelChildrenBoundsMap = new HashMap<>();

		_modelBoundsMap = new HashMap<>();
		_postPaintActions = new ArrayList<>();

		_imageCacheMap = new HashMap<>();
	}

	public void dispose() {
		for (var image : _imageCacheMap.values()) {
			if (!image.isDisposed()) {
				image.dispose();
			}
		}
	}

	public Image getModelImageFromCache(Object model) {
		return _imageCacheMap.get(model);
	}

	public void addPostPaintAction(Runnable action) {
		_postPaintActions.add(action);
	}

	public void renderScene(GC gc, Transform tx, SceneModel sceneModel) {

		_modelMatrixMap = new HashMap<>();
		_modelBoundsMap = new HashMap<>();
		_modelChildrenBoundsMap = new HashMap<>();

		var tx2 = newTx(gc, tx);

		try {

			{
				int dx = _canvas.getOffsetX();
				int dy = _canvas.getOffsetY();
				float scale = _canvas.getScale();

				tx2.translate(dx, dy);
				tx2.scale(scale, scale);
			}

			renderObject(gc, tx2, sceneModel.getRootObject());

		} finally {
			tx2.dispose();
			gc.setTransform(null);
		}

		if (_debug) {
			startDebug(gc, sceneModel);
		}

		for (var action : _postPaintActions) {
			try {
				action.run();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		_postPaintActions.clear();
	}

	private void startDebug(GC gc, SceneModel model) {
		var oldTx = new Transform(gc.getDevice());
		gc.getTransform(oldTx);

		var newTx = new Transform(gc.getDevice());
		gc.setTransform(newTx);

		debugObject(gc, model.getRootObject());

		gc.setTransform(oldTx);
		
	}

	private void debugObject(GC gc, ObjectModel model) {
		gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_RED));

		if (model instanceof ParentComponent) {
			for (var model2 : ParentComponent.get_children(model)) {
				debugObject(gc, model2);
			}

			gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_BLUE));
		}

		var bounds = _modelBoundsMap.get(model);

		if (bounds != null) {

			var points = new int[bounds.length];

			for (int i = 0; i < points.length; i++) {
				points[i] = (int) bounds[i];
			}

			gc.drawPolygon(points);
		}
	}

	private void renderChildren(GC gc, Transform tx, ObjectModel parent) {

		setObjectTransform(gc, tx, parent);

		var children = ParentComponent.get_children(parent);

		for (var obj : children) {

			var tx2 = newTx(gc, tx);

			renderObject(gc, tx2, obj);

			tx2.dispose();
		}

		var minX = Float.MAX_VALUE;
		var minY = Float.MAX_VALUE;
		var maxX = Float.MIN_VALUE;
		var maxY = Float.MIN_VALUE;

		for (var obj : children) {
			var points = _modelBoundsMap.get(obj);

			if (points != null) {
				for (int i = 0; i + 1 < points.length; i += 2) {
					var x = points[i];
					var y = points[i + 1];
					minX = Math.min(minX, x);
					minY = Math.min(minY, y);
					maxX = Math.max(maxX, x);
					maxY = Math.max(maxY, y);
				}
			}
		}

		_modelChildrenBoundsMap.put(parent, new float[] { minX, minY, maxX, minY, maxX, maxY, minX, maxY });
	}

	public static float[] joinBounds(float[] a, float[] b) {
		var minX = Float.MAX_VALUE;
		var minY = Float.MAX_VALUE;
		var maxX = Float.MIN_VALUE;
		var maxY = Float.MIN_VALUE;

		for (var points : new float[][] { a, b }) {
			if (points != null) {
				for (int i = 0; i + 1 < points.length; i += 2) {
					var x = points[i];
					var y = points[i + 1];
					minX = Math.min(minX, x);
					minY = Math.min(minY, y);
					maxX = Math.max(maxX, x);
					maxY = Math.max(maxY, y);
				}
			}
		}

		return new float[] { minX, minY, maxX, minY, maxX, maxY, minX, maxY };
	}

	private void renderObject(GC gc, Transform tx, ObjectModel model) {

		if (!EditorComponent.get_editorShow(model)) {
			return;
		}

		var alpha = gc.getAlpha();
		var transp = EditorComponent.get_editorTransparency(model);

		{

			if (transp != 1) {
				gc.setAlpha((int) (transp * 255));
			}
		}

		if (model instanceof TransformComponent) {
			{
				// position

				var x = TransformComponent.get_x(model);
				var y = TransformComponent.get_y(model);

				tx.translate(x, y);
			}

			{
				// rotation

				var angle = TransformComponent.get_angle(model);

				tx.rotate(angle);
			}

			// {
			// // pivot
			// var px = objModel.getPivotX();
			// var py = objModel.getPivotY();
			// tx.translate((float) (-px * objModel.getScaleX()), (float) (-py *
			// objModel.getScaleY()));
			// }

			{
				// scale

				var scaleX = TransformComponent.get_scaleX(model);
				var scaleY = TransformComponent.get_scaleY(model);

				tx.scale(scaleX, scaleY);
			}
		}

		var tx2 = tx;
		{
			// flip

			if (model instanceof FlipComponent) {
				tx2 = newTx(gc, tx);
				tx2.scale(FlipComponent.get_flipX(model) ? -1 : 1, FlipComponent.get_flipY(model) ? -1 : 1);
				gc.setTransform(tx2);
			}
		}

		// origin

		if (model instanceof OriginComponent) {

			var originX = OriginComponent.get_originX(model);
			var originY = OriginComponent.get_originY(model);

			var size = getTextureSize(model);

			double x = -size[0] * originX;
			double y = -size[1] * originY;
			tx2.translate((float) x, (float) y);
		}

		if (model instanceof BitmapTextModel) {

			renderBitmapText(gc, tx2, (BitmapTextModel) model);

		} else if (model instanceof BaseSpriteModel) {

			renderSprite(gc, tx2, (BaseSpriteModel) model);

		}

		if (model instanceof ParentComponent) {

			renderChildren(gc, tx2, model);

		}

		if (tx2 != tx) {
			gc.setTransform(tx);
			tx2.dispose();
		}

		if (transp != 1) {
			gc.setAlpha(alpha);
		}

	}

	private void renderBitmapText(GC gc, Transform tx, BitmapTextModel model) {

		setObjectTransform(gc, tx, model);

		var image = getBitmapTextImage(model);

		Transform tx2 = null;

		if (model instanceof DynamicBitmapTextComponent) {
			// crop it

			var cropWidth = DynamicBitmapTextComponent.get_cropWidth(model);
			var cropHeight = DynamicBitmapTextComponent.get_cropHeight(model);

			// if the text is not cropped, then it will be scrolled here

			if (cropWidth == 0 || cropHeight == 0) {

				var scrollX = DynamicBitmapTextComponent.get_scrollX(model);
				var scrollY = DynamicBitmapTextComponent.get_scrollY(model);

				tx2 = newTx(gc, tx);
				tx2.translate(-scrollX, -scrollY);

				setObjectTransform(gc, tx2, model);
			}
		}

		gc.drawImage(image, 0, 0);

		if (tx2 != null) {
			tx2.dispose();
		}

		var b = image.getBounds();
		setObjectBounds(gc, model, 0, 0, b.width, b.height);
	}

	private Image createBitmapTextImage(BitmapTextModel textModel) {
		var fontModel = textModel.getFontModel();

		var args = createBitmapTextRenderArgs(textModel);

		var metrics = fontModel.metrics(args);

		var width = metrics.getWidth();
		var height = metrics.getHeight();

		var buffer = createImage(width, height);

		GC gc2 = new GC(buffer);

		BaseImageCanvas.prepareGC(gc2);

		var asset = BitmapTextComponent.get_font(textModel);

		var fontTexture = loadImage(asset.getTextureFile());

		Transform tx = null;

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

				gc2.setClipping((int) cropX, (int) cropY, cropWidth, cropHeight);

				var scrollX = DynamicBitmapTextComponent.get_scrollX(textModel);
				var scrollY = DynamicBitmapTextComponent.get_scrollY(textModel);

				tx = new Transform(gc2.getDevice());
				tx.translate(-scrollX, -scrollY);
				gc2.setTransform(tx);

			}
		}

		try {

			fontModel.render(args, new BitmapFontRenderer() {

				@SuppressWarnings("hiding")
				@Override
				public void render(char c, int x, int y, int width, int height, int srcX, int srcY, int srcW,
						int srcH) {
					gc2.drawImage(fontTexture, srcX, srcY, srcW, srcH, x, y, width, height);
				}
			});

		} catch (Exception e) {
			e.printStackTrace();
		}

		if (tx != null) {
			tx.dispose();
		}

		gc2.dispose();

		return buffer;
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

	private void renderSprite(GC gc, Transform tx, BaseSpriteModel model) {
		setObjectTransform(gc, tx, model);

		if (model instanceof TileSpriteModel) {

			renderTileSprite(gc, (TileSpriteModel) model);

		} else {

			var frame = TextureComponent.get_frame(model);

			renderTexture(gc, model, frame);
		}
	}

	private Image createImage(int width, int height) {
		var temp = new Image(_canvas.getDisplay(), 1, 1);
		var tempData = temp.getImageData();

		var data = new ImageData(width, height, tempData.depth, tempData.palette);
		data.alphaData = new byte[width * height];

		var img = new Image(_canvas.getDisplay(), data);

		temp.dispose();

		return img;
	}

	private Image createTileSpriteTexture(TileSpriteModel model) {
		var assetFrame = TextureComponent.get_frame(model);

		var img = _canvas.loadImage(assetFrame.getImageFile());

		var fd = assetFrame.getFrameData();

		var tileScaleX = TileSpriteComponent.get_tileScaleX(model);
		var tileScaleY = TileSpriteComponent.get_tileScaleY(model);

		var frameWidth = fd.srcSize.x * tileScaleX;
		var frameHeight = fd.srcSize.y * tileScaleY;

		var width = TileSpriteComponent.get_width(model);
		var height = TileSpriteComponent.get_height(model);

		var buffer = createImage((int) width, (int) height);
		var gc = new GC(buffer);

		double x = 0;
		double y = 0;

		double xoffs = TileSpriteComponent.get_tilePositionX(model) % fd.srcSize.x;
		double yoffs = TileSpriteComponent.get_tilePositionY(model) % fd.srcSize.y;

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

		gc.dispose();

		return buffer;
	}

	private void renderTileSprite(GC gc, TileSpriteModel model) {
		var width = TileSpriteComponent.get_width(model);
		var height = TileSpriteComponent.get_height(model);

		Image image;

		if (model.isDirty()) {
			image = createTileSpriteTexture(model);

			model.setDirty(false);

			var old = _imageCacheMap.put(model, image);

			if (old != null) {
				old.dispose();
			}
		} else {
			image = _imageCacheMap.getOrDefault(model, null);
		}

		gc.drawImage(image, 0, 0);

		setObjectBounds(gc, model, 0, 0, width, height);

	}

	private static Transform newTx(GC gc, Transform tx) {
		var txElements = new float[6];
		tx.getElements(txElements);
		return new Transform(gc.getDevice(), txElements);
	}

	private Image loadImage(IFile file) {
		return _canvas.loadImage(file);
	}

	private int[] getTextureSize(ObjectModel model) {

		// TODO: implement the rest of the models

		if (model instanceof BitmapTextModel) {

			return getBitmapTextSize((BitmapTextModel) model);

		} else if (model instanceof TileSpriteModel) {
			return new int[] {

					(int) TileSpriteComponent.get_width(model),

					(int) TileSpriteComponent.get_height(model) };

		} else if (model instanceof TextureComponent) {

			var frame = TextureComponent.get_frame(model);

			var fd = frame.getFrameData();

			return new int[] { fd.srcSize.x, fd.srcSize.y };
		}

		return new int[] { 0, 0 };
	}

	private int[] getBitmapTextSize(BitmapTextModel model) {

		var image = getBitmapTextImage(model);

		var b = image.getBounds();

		return new int[] { b.width, b.height };

	}

	public Image getBitmapTextImage(BitmapTextModel model) {
		Image image;

		if (model.isDirty()) {
			image = createBitmapTextImage(model);

			model.setDirty(false);

			var old = _imageCacheMap.put(model, image);

			if (old != null) {
				old.dispose();
			}
		} else {
			image = _imageCacheMap.get(model);
		}

		return image;
	}

	private void renderTexture(GC gc, ObjectModel model, IAssetFrameModel assetFrame) {
		var img = loadImage(assetFrame.getImageFile());

		var fd = assetFrame.getFrameData();

		gc.drawImage(img, fd.src.x, fd.src.y, fd.src.width, fd.src.height, fd.dst.x, fd.dst.y, fd.dst.width,
				fd.dst.height);

		setObjectBounds(gc, model, 0, 0, fd.srcSize.x, fd.srcSize.y);
	}

	private void setObjectTransform(GC gc, Transform tx, ObjectModel model) {
		gc.setTransform(tx);

		var matrix = _modelMatrixMap.get(model);

		if (matrix == null) {
			matrix = new float[6];
			_modelMatrixMap.put(model, matrix);
		}

		tx.getElements(matrix);
	}

	private void setObjectBounds(GC gc, ObjectModel model, float x, float y, float width, float height) {
		var txElems = _modelMatrixMap.get(model);
		var tx = new Transform(gc.getDevice(), txElems);

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

	public float[] getObjectBounds(ObjectModel obj) {
		return _modelBoundsMap.get(obj);
	}

	public float[] getObjectChildrenBounds(ObjectModel obj) {
		return _modelChildrenBoundsMap.get(obj);
	}

	public float[] localToScene(ObjectModel model, float localX, float localY) {

		var matrix = _modelMatrixMap.get(model);

		var tx = new Transform(Display.getDefault(), matrix);

		var point = new float[] { localX, localY };

		tx.transform(point);

		tx.dispose();

		return point;
	}

	public float[] sceneToLocal(ObjectModel model, float sceneX, float sceneY) {
		var matrix = _modelMatrixMap.get(model);

		var tx = new Transform(Display.getDefault(), matrix);

		tx.invert();

		var point = new float[] { sceneX, sceneY };

		tx.transform(point);

		tx.dispose();

		return point;
	}
}
