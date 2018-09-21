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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Transform;

import phasereditor.assetpack.core.IAssetFrameModel;
import phasereditor.scene.core.EditorComponent;
import phasereditor.scene.core.FlipComponent;
import phasereditor.scene.core.ObjectModel;
import phasereditor.scene.core.OriginComponent;
import phasereditor.scene.core.ParentComponent;
import phasereditor.scene.core.SceneModel;
import phasereditor.scene.core.SpriteModel;
import phasereditor.scene.core.TextureComponent;
import phasereditor.scene.core.TransformComponent;

/**
 * @author arian
 *
 */
public class SceneObjectRenderer {
	private SceneCanvas _canvas;
	private Map<ObjectModel, float[]> _modelTransformMap;
	private Map<ObjectModel, float[]> _modelBoundsMap;
	private Map<ObjectModel, float[]> _modelChildrenBoundsMap;
	private boolean _debug;

	public SceneObjectRenderer(SceneCanvas canvas) {
		super();

		_canvas = canvas;

		_modelTransformMap = new HashMap<>();
		_modelChildrenBoundsMap = new HashMap<>();
		_modelBoundsMap = new HashMap<>();
	}

	public void renderScene(GC gc, Transform tx, SceneModel sceneModel) {

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

			for (var objModel : sceneModel.getObjects()) {

				var tx3 = newTx(gc, tx2);

				renderObject(gc, tx3, objModel);

				tx3.dispose();
			}

		} finally {
			tx2.dispose();
			gc.setTransform(null);
		}

		if (_debug) {
			startDebug(gc, sceneModel);
		}
	}

	private void startDebug(GC gc, SceneModel model) {
		var oldTx = new Transform(gc.getDevice());
		gc.getTransform(oldTx);

		var newTx = new Transform(gc.getDevice());
		gc.setTransform(newTx);

		for (var objModel : model.getObjects()) {
			debugObject(gc, objModel);
		}

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

	private void renderObject(GC gc, Transform tx, ObjectModel objModel) {

		if (!EditorComponent.get_editorShow(objModel)) {
			return;
		}

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

		var tx2 = tx;
		{
			// flip

			if (objModel instanceof FlipComponent) {
				tx2 = newTx(gc, tx);
				tx2.scale(FlipComponent.get_flipX(objModel) ? -1 : 1, FlipComponent.get_flipY(objModel) ? -1 : 1);
				gc.setTransform(tx2);
			}
		}

		if (objModel instanceof SpriteModel) {

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

	private void renderSprite(GC gc, Transform tx, SpriteModel model) {
		{
			// anchor
			var anchorX = OriginComponent.get_originX(model);
			var anchorY = OriginComponent.get_originY(model);

			var size = getTextureSize(model);

			double x = -size.x * anchorX;
			double y = -size.y * anchorY;
			tx.translate((float) x, (float) y);
		}

		setObjectTransform(gc, tx, model);

		var frame = TextureComponent.get_frame(model);

		renderTexture(gc, model, frame);
	}

	private static Transform newTx(GC gc, Transform tx) {
		var txElements = new float[6];
		tx.getElements(txElements);
		return new Transform(gc.getDevice(), txElements);
	}

	private Image loadImage(IFile file) {
		return _canvas.loadImage(file);
	}

	private static Point getTextureSize(ObjectModel model) {

		// TODO: implement the rest of the models

		if (model instanceof TextureComponent) {

			var frame = TextureComponent.get_frame(model);

			var fd = frame.getFrameData();

			return fd.srcSize;
		}

		return new Point(0, 0);
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

		var txElems = _modelTransformMap.get(model);

		if (txElems == null) {
			txElems = new float[6];
			_modelTransformMap.put(model, txElems);
		}

		tx.getElements(txElems);
	}

	private void setObjectBounds(GC gc, ObjectModel model, float x, float y, float width, float height) {
		var txElems = _modelTransformMap.get(model);
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
}
