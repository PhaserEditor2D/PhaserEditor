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
package phasereditor.scene.ui;

import static phasereditor.ui.Colors.BLACK;
import static phasereditor.ui.Colors.BLUE;
import static phasereditor.ui.Colors.RED;
import static phasereditor.ui.Colors.WHITE;
import static phasereditor.ui.Colors.YELLOW;
import static phasereditor.ui.Colors.color;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.widgets.Display;

import phasereditor.assetpack.core.AssetFinder;
import phasereditor.assetpack.ui.AssetPackUI;
import phasereditor.bmpfont.core.BitmapFontModel.Align;
import phasereditor.bmpfont.core.BitmapFontModel.RenderArgs;
import phasereditor.bmpfont.core.BitmapFontRenderer;
import phasereditor.scene.core.BaseSpriteModel;
import phasereditor.scene.core.BitmapTextComponent;
import phasereditor.scene.core.BitmapTextModel;
import phasereditor.scene.core.ContainerComponent;
import phasereditor.scene.core.ContainerModel;
import phasereditor.scene.core.DynamicBitmapTextComponent;
import phasereditor.scene.core.FlipComponent;
import phasereditor.scene.core.GameObjectEditorComponent;
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
import phasereditor.ui.ImageProxy;
import phasereditor.ui.PhaserEditorUI;

/**
 * @author arian
 *
 */
public class SceneObjectRenderer {
	private ISceneObjectRendererContext _rendererContext;
	private Map<ObjectModel, float[]> _modelMatrixMap;
	private Map<ObjectModel, float[]> _modelBoundsMap;
	private Map<ObjectModel, float[]> _modelChildrenBoundsMap;
	private Map<Object, Image> _imageCacheMap;

	private boolean _debug;
	private List<Runnable> _postPaintActions;
	private AssetFinder _finder;
	private AssetFinder _lastFinderSnapshot;

	private Color _COLOR_RED;
	private Color _COLOR_BLUE;
	private Color _COLOR_BLACK;
	private Color _COLOR_WHITE;

	public SceneObjectRenderer(ISceneObjectRendererContext rendererContext) {
		super();

		_rendererContext = rendererContext;

		_modelMatrixMap = new HashMap<>();
		_modelChildrenBoundsMap = new HashMap<>();

		_modelBoundsMap = new HashMap<>();
		_postPaintActions = new ArrayList<>();

		_imageCacheMap = new HashMap<>();

		_finder = rendererContext.getAssetFinder();
		_lastFinderSnapshot = _finder;
	}

	public void dispose() {
		disposeImageCache();
	}

	private void disposeImageCache() {

		for (var image : _imageCacheMap.values()) {

			if (!image.isDisposed()) {
				image.dispose();
			}
		}

		_imageCacheMap = new HashMap<>();
	}

	public Image getModelImageFromCache(Object model) {
		return _imageCacheMap.get(model);
	}

	public void addPostPaintAction(Runnable action) {
		_postPaintActions.add(action);
	}

	public void renderScene(GC gc, Transform tx, SceneModel sceneModel) {
		_COLOR_BLACK = color(BLACK);
		_COLOR_BLUE = color(BLUE);
		_COLOR_RED = color(RED);
		_COLOR_WHITE = color(WHITE);

		_modelMatrixMap = new HashMap<>();
		_modelBoundsMap = new HashMap<>();
		_modelChildrenBoundsMap = new HashMap<>();

		var tx2 = newTx(gc, tx);

		try {

			{

				int dx = _rendererContext.getOffsetX();
				int dy = _rendererContext.getOffsetY();
				float scale = _rendererContext.getScale();

				tx2.translate(dx, dy);
				tx2.scale(scale, scale);
			}

			// renderObject(gc, tx2, sceneModel.getDisplayList());
			renderChildren(gc, tx2, sceneModel.getDisplayList());

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

		gc.setBackground(_COLOR_WHITE);
		gc.setForeground(_COLOR_BLACK);

		for (var model : sceneModel.getDisplayList().getChildren()) {
			renderBones(gc, model, false);
		}

		_postPaintActions.clear();

		_lastFinderSnapshot = _finder.snapshot();
	}

	private void renderBones(GC gc, ObjectModel parent, boolean forceRender) {
		var modelRender = GameObjectEditorComponent.get_gameObjectEditorShowBones(parent);

		if (ParentComponent.is(parent)) {

			var children = ParentComponent.get_children(parent);

			if (forceRender || modelRender) {
				float[] parentPoint = getScenePointAtOrigin(parent);

				for (var child : children) {

					var childPoint = getScenePointAtOrigin(child);

					var vector = PhaserEditorUI.vector(parentPoint, childPoint);
					var leftVector = PhaserEditorUI.unitarianVector(PhaserEditorUI.rotate90(vector, -1));
					var rightVector = PhaserEditorUI.unitarianVector(PhaserEditorUI.rotate90(vector, 1));

					var n = 3;
					var points = new int[] {

							(int) (parentPoint[0] + leftVector[0] * n), (int) (parentPoint[1] + leftVector[1] * n),

							(int) (parentPoint[0] + rightVector[0] * n), (int) (parentPoint[1] + rightVector[1] * n),

							(int) (childPoint[0]), (int) (childPoint[1])

					};

					gc.fillPolygon(points);
					gc.drawPolygon(points);

					renderBones(gc, child, true);
				}

			}

			for (var child : children) {
				renderBones(gc, child, false);
			}
		}
	}

	private void startDebug(GC gc, SceneModel model) {
		var oldTx = new Transform(gc.getDevice());
		gc.getTransform(oldTx);

		var newTx = new Transform(gc.getDevice());
		gc.setTransform(newTx);

		debugObject(gc, model.getDisplayList());

		gc.setTransform(oldTx);

	}

	private void debugObject(GC gc, ObjectModel model) {
		gc.setForeground(_COLOR_RED);

		if (model instanceof ParentComponent) {
			for (var model2 : ParentComponent.get_children(model)) {
				debugObject(gc, model2);
			}

			gc.setForeground(_COLOR_BLUE);
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

		if (!GameObjectEditorComponent.get_gameObjectEditorShow(model)) {
			return;
		}

		var alpha = gc.getAlpha();
		var transp = GameObjectEditorComponent.get_gameObjectEditorTransparency(model);

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

		if (model instanceof OriginComponent || model instanceof ContainerComponent) {

			// default origin for container
			var originX = 0.5f;
			var originY = 0.5f;

			if (model instanceof OriginComponent) {
				originX = OriginComponent.get_originX(model);
				originY = OriginComponent.get_originY(model);
			}

			var size = getObjectSize(model);

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

		model.updateSizeFromBitmapFont(_finder);

		var image = getBitmapTextImage(model);

		Rectangle bounds;

		if (image == null) {
			bounds = new Rectangle(0, 0, 0, 0);
		} else {

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

			bounds = image.getBounds();
		}

		setObjectBounds(gc, model, 0, 0, bounds.width, bounds.height);
	}

	private Image createBitmapTextImage(BitmapTextModel textModel) {
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

		BaseImageCanvas.prepareGC(g2);

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

		try {
			
			return PhaserEditorUI.image_Swing_To_SWT(buffer);
			
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
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

			var key = TextureComponent.get_textureKey(model);
			var frame = TextureComponent.get_textureFrame(model);

			renderTexture(gc, model, key, frame);
		}
	}

	/**
	 * We should use Java2D to create an empty image.
	 */
	@Deprecated
	public Image createImage(int width, int height) {
		return PhaserEditorUI.createTransparentSWTImage(_rendererContext.getDisplay(), width, height);
	}

	public Image getTileSpriteTextImage(TileSpriteModel model) {
		Image image;

		if (GameObjectEditorComponent.get_gameObjectEditorDirty(model) || asset_textureChanged(model)) {

			image = createTileSpriteTexture(model);

			GameObjectEditorComponent.set_gameObjectEditorDirty(model, false);

			var old = _imageCacheMap.put(model, image);

			if (old != null) {
				old.dispose();
			}
		} else {
			image = _imageCacheMap.get(model);
		}

		return image;
	}

	private Image createTileSpriteTexture(TileSpriteModel model) {
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

		try {

			return PhaserEditorUI.image_Swing_To_SWT(buffer);

		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	private void renderTileSprite(GC gc, TileSpriteModel model) {
		var width = TileSpriteComponent.get_width(model);
		var height = TileSpriteComponent.get_height(model);

		var image = getTileSpriteTextImage(model);

		if (image != null) {
			gc.drawImage(image, 0, 0);
		}

		setObjectBounds(gc, model, 0, 0, width, height);

	}

	private static Transform newTx(GC gc, Transform tx) {
		var txElements = new float[6];
		tx.getElements(txElements);
		return new Transform(gc.getDevice(), txElements);
	}

	public float[] getObjectSize(ObjectModel model) {

		if (model instanceof BitmapTextModel) {

			return getBitmapTextSize((BitmapTextModel) model);

		} else if (model instanceof TileSpriteModel) {

			return new float[] {

					(int) TileSpriteComponent.get_width(model),

					(int) TileSpriteComponent.get_height(model) };

		} else if (model instanceof TextureComponent) {

			var frame = TextureComponent.utils_getTexture(model, _finder);

			if (frame == null) {
				return new float[] { 0, 0 };
			}

			var fd = frame.getFrameData();

			return new float[] { fd.srcSize.x, fd.srcSize.y };

		} else if (model instanceof ContainerModel) {

			return getContainerSize((ContainerModel) model);

		}

		return new float[] { 0, 0 };
	}

	private float[] getContainerSize(ContainerModel model) {

		var bounds = getObjectBounds(model);

		var w = bounds[2] - bounds[0];
		var h = bounds[3] - bounds[1];

		return new float[] { w, h };
	}

	public float[] getLocalPointAtOrigin(ObjectModel model) {
		if (OriginComponent.is(model)) {
			var size = getObjectSize(model);
			return new float[] {

					OriginComponent.get_originX(model) * size[0],

					OriginComponent.get_originY(model) * size[1]

			};
		}

		return new float[] { 0, 0 };

	}

	public float[] getScenePointAtOrigin(ObjectModel model) {
		var local = getLocalPointAtOrigin(model);
		return localToScene(model, local);
	}

	private float[] getBitmapTextSize(BitmapTextModel model) {

		var image = getBitmapTextImage(model);

		if (image == null) {
			return new float[] { 0, 0 };
		}

		var b = image.getBounds();

		return new float[] { b.width, b.height };

	}

	public Image getBitmapTextImage(BitmapTextModel model) {
		Image image;

		if (GameObjectEditorComponent.get_gameObjectEditorDirty(model) || asset_bitmapFontChanged(model)) {

			image = createBitmapTextImage(model);

			GameObjectEditorComponent.set_gameObjectEditorDirty(model, false);

			var old = _imageCacheMap.put(model, image);

			if (old != null) {
				old.dispose();
			}
		} else {
			image = _imageCacheMap.get(model);
		}

		return image;
	}

	private void renderTexture(GC gc, ObjectModel model, String key, String frame) {

		var assetFrame = TextureComponent.utils_getTexture(model, _finder);

		if (assetFrame == null) {
			gc.setForeground(color(YELLOW));
			gc.setBackground(color(RED));
			gc.drawText("Missing Texture (" + key + "," + frame + ")", 0, 0);

			setObjectBounds(gc, model, 0, 0, 0, 0);
			return;
		}

		var fd = assetFrame.getFrameData();
		var proxy = AssetPackUI.getImageProxy(assetFrame);
		proxy.paint(gc, 0, 0);

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

	public float[] localToOther(ObjectModel a, ObjectModel b, float ax, float ay) {
		var globalPoint = localToScene(a, ax, ay);
		var otherPoint = sceneToLocal(b, globalPoint);
		return otherPoint;
	}

	public float[] localToParent(ObjectModel model, float x, float y) {
		return localToOther(model, ParentComponent.get_parent(model), x, y);
	}

	public float[] localToParent(ObjectModel model, float[] xy) {
		return localToParent(model, xy[0], xy[1]);
	}

	public float[] parentToLocal(ObjectModel model, float x, float y) {
		return localToOther(ParentComponent.get_parent(model), model, x, y);
	}

	public float[] parentToLocal(ObjectModel model, float[] xy) {
		return parentToLocal(model, xy[0], xy[1]);
	}

	public float[] localToScene(ObjectModel model, float[] localPoint) {
		return localToScene(model, localPoint[0], localPoint[1]);
	}

	public float globalScaleX(ObjectModel model) {
		var scale = TransformComponent.get_scaleX(model);

		var parent = ParentComponent.get_parent(model);

		if (parent == null || !TransformComponent.is(parent)) {
			return _rendererContext.getScale() * scale;
		}

		return scale * globalScaleX(parent);
	}

	public float globalScaleY(ObjectModel model) {
		var scale = TransformComponent.get_scaleY(model);

		var parent = ParentComponent.get_parent(model);

		if (parent == null || !TransformComponent.is(parent)) {
			return _rendererContext.getScale() * scale;
		}

		return scale * globalScaleY(parent);
	}

	public float globalAngle(ObjectModel model) {
		if (!TransformComponent.is(model)) {
			return 0;
		}

		var angle = TransformComponent.get_angle(model);

		var parent = ParentComponent.get_parent(model);

		if (parent == null) {
			return angle;
		}

		return angle + globalAngle(parent);
	}

	public float[] sceneToLocal(ObjectModel model, float[] scenePoint) {
		return sceneToLocal(model, scenePoint[0], scenePoint[1]);
	}

	public float[] sceneToLocal(ObjectModel model, float sceneX, float sceneY) {
		var matrix = _modelMatrixMap.get(model);

		return sceneToLocal(matrix, sceneX, sceneY);
	}

	@SuppressWarnings("static-method")
	public float[] sceneToLocal(float[] matrix, float sceneX, float sceneY) {
		var tx = new Transform(Display.getDefault(), matrix);

		tx.invert();

		var point = new float[] { sceneX, sceneY };

		tx.transform(point);

		tx.dispose();

		return point;
	}

	public Transform getObjectTransform(ObjectModel model) {
		var matrix = _modelMatrixMap.get(model);

		var tx = new Transform(Display.getDefault(), matrix);

		return tx;
	}

	/**
	 * {m11, m12, m21, m22, dx, dy}
	 */
	public float[] getObjectMatrix(ObjectModel model) {
		return _modelMatrixMap.get(model);
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
