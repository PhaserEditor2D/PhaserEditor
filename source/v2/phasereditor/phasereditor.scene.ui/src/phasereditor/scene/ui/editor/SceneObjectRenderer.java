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
import phasereditor.bmpfont.core.BitmapFontRenderer;
import phasereditor.bmpfont.core.BitmapFontModel.RenderArgs;
import phasereditor.scene.core.BitmapTextComponent;
import phasereditor.scene.core.BitmapTextModel;
import phasereditor.scene.core.EditorComponent;
import phasereditor.scene.core.FlipComponent;
import phasereditor.scene.core.ObjectModel;
import phasereditor.scene.core.OriginComponent;
import phasereditor.scene.core.ParentComponent;
import phasereditor.scene.core.SceneModel;
import phasereditor.scene.core.SpriteModel;
import phasereditor.scene.core.TextualComponent;
import phasereditor.scene.core.TextureComponent;
import phasereditor.scene.core.TileSpriteComponent;
import phasereditor.scene.core.TileSpriteModel;
import phasereditor.scene.core.TransformComponent;

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
		for (var buffer : _imageCacheMap.values()) {
			if (!buffer.isDisposed()) {
				buffer.dispose();
			}
		}
	}

	public void clearImageInCache(Object key) {
		var buffer = _imageCacheMap.remove(key);

		if (buffer != null) {
			buffer.dispose();
		}
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

	private void renderObject(GC gc, Transform tx, ObjectModel objModel) {

		if (!EditorComponent.get_editorShow(objModel)) {
			return;
		}

		if (objModel instanceof TransformComponent) {
			{
				// position

				var x = TransformComponent.get_x(objModel);
				var y = TransformComponent.get_y(objModel);

				tx.translate(x, y);
			}

			{
				// rotation

				var angle = TransformComponent.get_angle(objModel);

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

				var scaleX = TransformComponent.get_scaleX(objModel);
				var scaleY = TransformComponent.get_scaleY(objModel);

				tx.scale(scaleX, scaleY);
			}
		}

		var tx2 = tx;
		{
			// flip

			if (objModel instanceof FlipComponent) {
				tx2 = newTx(gc, tx);
				tx2.scale(FlipComponent.get_flipX(objModel) ? -1 : 1, FlipComponent.get_flipY(objModel) ? -1 : 1);
				gc.setTransform(tx2);
			}
		}

		// origin

		if (objModel instanceof OriginComponent) {

			var originX = OriginComponent.get_originX(objModel);
			var originY = OriginComponent.get_originY(objModel);

			var size = getTextureSize(objModel);

			double x = -size[0] * originX;
			double y = -size[1] * originY;
			tx2.translate((float) x, (float) y);
		}

		if (objModel instanceof BitmapTextModel) {

			renderBitmapText(gc, tx2, (BitmapTextModel) objModel);

		} else if (objModel instanceof SpriteModel) {

			renderSprite(gc, tx2, (SpriteModel) objModel);

		}

		if (objModel instanceof ParentComponent) {

			renderChildren(gc, tx2, objModel);

		}

		if (tx2 != tx) {
			gc.setTransform(tx);
			tx2.dispose();
		}

	}

	private void renderBitmapText(GC gc, Transform tx, BitmapTextModel textModel) {
		
		setObjectTransform(gc, tx, textModel);
		
		var fontModel = textModel.createFontModel();

		var asset = BitmapTextComponent.get_font(textModel);

		var img = loadImage(asset.getTextureFile());

		double scale = (double) BitmapTextComponent.get_fontSize(textModel) / (double) fontModel.getInfoSize();

		var tx2 = newTx(gc, tx);
		tx2.scale((float) scale, (float) scale);

		setObjectTransform(gc, tx2, textModel);

		try {

			int[] size = new int[] { Integer.MIN_VALUE, Integer.MIN_VALUE };

			fontModel.render(new RenderArgs(TextualComponent.get_text(textModel)), new BitmapFontRenderer() {

				@Override
				public void render(char c, int x, int y, int srcX, int srcY, int srcW, int srcH) {
					gc.drawImage(img, srcX, srcY, srcW, srcH, x, y, srcW, srcH);
					size[0] = Math.max(x + srcW, size[0]);
					size[1] = Math.max(y + srcH, size[1]);
				}

			});

			setObjectBounds(gc, textModel, 0, 0, size[0], size[1]);

		} catch (Exception e) {
			e.printStackTrace();
		}

		tx2.dispose();

		gc.setTransform(tx);
	}

	private void renderSprite(GC gc, Transform tx, SpriteModel model) {
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

		var buffer = _imageCacheMap.getOrDefault(model, null);

		if (buffer == null) {
			buffer = createTileSpriteTexture(model);
			_imageCacheMap.put(model, buffer);
		}

		gc.drawImage(buffer, 0, 0);

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

	private static int[] getTextureSize(ObjectModel model) {

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

	private static int[] getBitmapTextSize(BitmapTextModel textModel) {

		var fontModel = textModel.createFontModel();

		double scale = (double) BitmapTextComponent.get_fontSize(textModel) / (double) fontModel.getInfoSize();

		int[] size = new int[] { Integer.MIN_VALUE, Integer.MIN_VALUE };

		fontModel.render(new RenderArgs(TextualComponent.get_text(textModel)), new BitmapFontRenderer() {

			@Override
			public void render(char c, int x, int y, int srcX, int srcY, int srcW, int srcH) {
				size[0] = Math.max(x + srcW, size[0]);
				size[1] = Math.max(y + srcH, size[1]);
			}

		});

		size[0] = (int) (size[0] * scale);
		size[1] = (int) (size[1] * scale);

		return size;

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
