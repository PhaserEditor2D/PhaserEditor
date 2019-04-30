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

import static phasereditor.ui.PhaserEditorUI.swtRun;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.DragDetectEvent;
import org.eclipse.swt.events.DragDetectListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.widgets.Composite;
import org.json.JSONObject;

import phasereditor.assetpack.core.AssetFinder;
import phasereditor.assetpack.core.BitmapFontAssetModel;
import phasereditor.assetpack.core.IAssetFrameModel;
import phasereditor.assetpack.core.ImageAssetModel;
import phasereditor.assetpack.core.SpritesheetAssetModel;
import phasereditor.assetpack.core.animations.AnimationFrameModel;
import phasereditor.assetpack.core.animations.AnimationModel;
import phasereditor.assetpack.ui.AssetPackUI;
import phasereditor.scene.core.AnimationsComponent;
import phasereditor.scene.core.BitmapTextComponent;
import phasereditor.scene.core.BitmapTextModel;
import phasereditor.scene.core.FlipComponent;
import phasereditor.scene.core.GameObjectEditorComponent;
import phasereditor.scene.core.ImageModel;
import phasereditor.scene.core.NameComputer;
import phasereditor.scene.core.ObjectModel;
import phasereditor.scene.core.ParentComponent;
import phasereditor.scene.core.SceneModel;
import phasereditor.scene.core.SpriteModel;
import phasereditor.scene.core.TextualComponent;
import phasereditor.scene.core.TextureComponent;
import phasereditor.scene.core.TransformComponent;
import phasereditor.scene.core.VariableComponent;
import phasereditor.scene.ui.ISceneObjectRendererContext;
import phasereditor.scene.ui.SceneObjectRenderer;
import phasereditor.scene.ui.editor.undo.WorldSnapshotOperation;
import phasereditor.ui.Colors;
import phasereditor.ui.PhaserEditorUI;
import phasereditor.ui.ZoomCanvas;

/**
 * @author arian
 */
@Deprecated
public class SceneCanvas extends ZoomCanvas
		implements MouseListener, MouseMoveListener, DragDetectListener, ISceneObjectRendererContext {

	
	public static final int X_LABELS_HEIGHT = 18;
	public static final int Y_LABEL_WIDTH = 18;
	private SceneEditor _editor;
	private SceneObjectRenderer _renderer;
	private float _renderModelSnapX;
	private float _renderModelSnapY;
	private DragObjectsEvents _dragObjectsEvents;
	private boolean _interactiveToolsHightlights;

	public SceneCanvas(Composite parent, int style) {
		super(parent, style | SWT.DOUBLE_BUFFERED | SWT.NO_REDRAW_RESIZE);

		addPaintListener(this);

		_dragObjectsEvents = new DragObjectsEvents(this);

		addDragDetectListener(this);
		addMouseListener(this);
		addMouseMoveListener(this);

		init_DND();

		setZoomWhenShiftPressed(false);

	}

	private void init_DND() {
		{
			int options = DND.DROP_MOVE | DND.DROP_DEFAULT;
			DropTarget target = new DropTarget(this, options);
			Transfer[] types = { LocalSelectionTransfer.getTransfer() };
			target.setTransfer(types);
			target.addDropListener(new DropTargetAdapter() {
				@Override
				public void drop(DropTargetEvent event) {
					var loc = toDisplay(0, 0);

					var x = event.x - loc.x;
					var y = event.y - loc.y;

					if (event.data instanceof Object[]) {
						selectionDropped(x, y, (Object[]) event.data);
					}
					if (event.data instanceof IStructuredSelection) {
						selectionDropped(x, y, ((IStructuredSelection) event.data).toArray());
					}
				}
			});
		}
	}

	public float[] viewToModel(int x, int y) {
		var calc = calc();

		var modelX = calc.viewToModelX(x) - Y_LABEL_WIDTH;
		var modelY = calc.viewToModelY(y) - X_LABELS_HEIGHT;

		return new float[] { modelX, modelY };
	}

	public float[] modelToView(int x, int y) {
		var calc = calc();

		var viewX = calc.modelToViewX(x) + Y_LABEL_WIDTH;
		var viewY = calc.modelToViewY(y) + X_LABELS_HEIGHT;

		return new float[] { viewX, viewY };
	}

	private void selectionDropped(int x, int y, Object[] data) {

		var nameComputer = new NameComputer(getModel().getDisplayList());

		var beforeSnapshot = WorldSnapshotOperation.takeSnapshot(_editor);

		var calc = calc();

		var modelX = calc.viewToModelX(x) - Y_LABEL_WIDTH;
		var modelY = calc.viewToModelY(y) - X_LABELS_HEIGHT;

		var sceneModel = getModel();
		modelX = sceneModel.snapValueX(modelX);
		modelY = sceneModel.snapValueY(modelY);

		var newModels = new ArrayList<ObjectModel>();

		for (var obj : data) {

			if (obj instanceof ImageAssetModel) {
				obj = ((ImageAssetModel) obj).getFrame();
			}

			if (obj instanceof AnimationModel) {
				var animFrames = ((AnimationModel) obj).getFrames();
				if (!animFrames.isEmpty()) {
					obj = animFrames.get(0);
				}
			}

			if (obj instanceof AnimationFrameModel) {
				var animFrame = (AnimationFrameModel) obj;
				var textureFrame = animFrame.getFrameName() == null ? null : animFrame.getFrameName() + "";
				var textureKey = animFrame.getTextureKey();

				var texture = getAssetFinder().findTexture(textureKey, textureFrame);

				if (texture != null) {
					var sprite = new SpriteModel();

					var name = nameComputer.newName(computeBaseName(texture));

					VariableComponent.set_variableName(sprite, name);

					TransformComponent.set_x(sprite, modelX);
					TransformComponent.set_y(sprite, modelY);

					TextureComponent.set_textureKey(sprite, textureKey);
					TextureComponent.set_textureFrame(sprite, textureFrame);

					AnimationsComponent.set_autoPlayAnimKey(sprite, animFrame.getAnimation().getKey());

					newModels.add(sprite);

				}
			} else if (obj instanceof IAssetFrameModel) {

				var frame = (IAssetFrameModel) obj;

				var sprite = new ImageModel();

				var name = nameComputer.newName(computeBaseName(frame));

				VariableComponent.set_variableName(sprite, name);

				TransformComponent.set_x(sprite, modelX);
				TransformComponent.set_y(sprite, modelY);

				TextureComponent.utils_setTexture(sprite, (IAssetFrameModel) obj);

				newModels.add(sprite);

			} else if (obj instanceof BitmapFontAssetModel) {

				var asset = (BitmapFontAssetModel) obj;

				var textModel = new BitmapTextModel();

				var name = nameComputer.newName(asset.getKey());

				VariableComponent.set_variableName(textModel, name);

				TransformComponent.set_x(textModel, modelX);
				TransformComponent.set_y(textModel, modelY);

				BitmapTextComponent.utils_setFont(textModel, asset);
				TextualComponent.set_text(textModel, "BitmapText");

				textModel.updateSizeFromBitmapFont(getAssetFinder());

				newModels.add(textModel);

			}
		}

		for (var model : newModels) {
			ParentComponent.utils_addChild(getModel().getDisplayList(), model);
		}

		var afterSnapshot = WorldSnapshotOperation.takeSnapshot(_editor);

		_editor.executeOperation(new WorldSnapshotOperation(beforeSnapshot, afterSnapshot, "Drop assets"));

		_editor.refreshOutline();

		_editor.setSelection(newModels);

		_editor.setDirty(true);

		_editor.getEditorSite().getPage().activate(_editor);
	}

	private static String computeBaseName(IAssetFrameModel texture) {
		if (texture instanceof SpritesheetAssetModel.FrameModel) {
			return texture.getAsset().getKey();
		}

		return texture.getKey();
	}

	public void init(SceneEditor editor) {
		_editor = editor;
	}

	@Override
	public AssetFinder getAssetFinder() {
		return _editor.getAssetFinder();
	}

	public SceneEditor getEditor() {
		return _editor;
	}

	public SceneObjectRenderer getSceneRenderer() {
		return _renderer;
	}

	public SceneModel getModel() {
		return _editor.getSceneModel();
	}

	@Override
	public void dispose() {
		super.dispose();

		_renderer.dispose();
	}

	private boolean _rendered;

	@Override
	protected void customPaintControl(PaintEvent e) {

		if (getEditor().isWaitingForProjectBuilders()) {

			renderBackground(e);

			var calc = calc();

			renderGrid(e, calc);

			var str = "Waiting for project builders...";
			var size = e.gc.stringExtent(str);

			e.gc.drawText(str, e.width / 2 - size.x / 2, e.height / 2 - size.y / 2);

			return;
		}

		if (_renderer == null) {
			_renderer = new SceneObjectRenderer(this);
		}

		_interactiveToolsHightlights = isInteractiveHightlights();

		// I dont know why the line width affects the transform in angles of 45.5.
		e.gc.setLineWidth(1);

		renderBackground(e);

		var calc = calc();

		renderGrid(e, calc);

		var tx = new Transform(e.gc.getDevice());
		tx.translate(Y_LABEL_WIDTH, X_LABELS_HEIGHT);

		_renderer.renderScene(e.gc, tx, _editor.getSceneModel());

		renderBorders(e.gc, calc);

		renderSelection(e.gc);

		renderInteractiveElements(e.gc);

		renderLabels(e, calc);

		tx.dispose();

		if (!_rendered) {
			swtRun(() -> {
				_editor.refreshOutline();
			});
		}

		_rendered = true;
	}

	public boolean isRendered() {
		return _rendered;
	}

	private void renderInteractiveElements(GC gc) {

	}

	private void renderBorders(GC gc, ZoomCalculator calc) {
		var view = calc.modelToView(getModel().getBorderX(), getModel().getBorderY(), getModel().getBorderWidth(),
				getModel().getBorderHeight());

		view.x += Y_LABEL_WIDTH;
		view.y += X_LABELS_HEIGHT;

		gc.setAlpha(150);

		gc.setForeground(Colors.color(Colors.BLACK));
		gc.drawRectangle(view.x + 1, view.y + 1, view.width, view.height);

		gc.setForeground(Colors.color(Colors.WHITE));
		gc.drawRectangle(view);

		gc.setAlpha(255);
	}

	private void renderSelection(GC gc) {

		var selectionColor = Colors.color(Colors.LIGHTGREEN);
		// var selectionColor = SWTResourceManager.getColor(ColorUtil.WHITE.rgb);

		for (var model : _editor.getSelectionList()) {

			if (!TransformComponent.is(model)) {
				continue;
			}

			gc.setForeground(selectionColor);

			var size = _renderer.getObjectSize(model);

			var p0 = _renderer.localToScene(model, 0, 0);
			var p1 = _renderer.localToScene(model, size[0], 0);
			var p2 = _renderer.localToScene(model, size[0], size[1]);
			var p3 = _renderer.localToScene(model, 0, size[1]);

			// gc.setForeground(SWTResourceManager.getColor(Colors.DARK_GREEN));
			// gc.setLineWidth(3);
			//
			// drawCorner(gc, p0, p1);
			// drawCorner(gc, p1, p2);
			// drawCorner(gc, p2, p3);
			// drawCorner(gc, p3, p0);
			//
			// gc.setForeground(selectionColor);
			// gc.setLineWidth(1);
			//
			// drawCorner(gc, p0, p1);
			// drawCorner(gc, p1, p2);
			// drawCorner(gc, p2, p3);
			// drawCorner(gc, p3, p0);

			var points = new int[] {

					(int) p0[0], (int) p0[1],

					(int) p1[0], (int) p1[1],

					(int) p2[0], (int) p2[1],

					(int) p3[0], (int) p3[1]

			};

			if (_interactiveToolsHightlights) {

				// draw thin border

				gc.setForeground(selectionColor);
				gc.drawPolygon(points);

			} else {

				// draw bold border
				{

					gc.setForeground(Colors.color(Colors.DARKGREEN));
					gc.setLineWidth(3);
					gc.drawPolygon(points);
					gc.setLineWidth(1);
					gc.setForeground(selectionColor);
					gc.drawPolygon(points);
				}

				// draw objects name
				{
					gc.setForeground(Colors.color(Colors.WHITE));

					var vector = PhaserEditorUI.vector(p3, p0);

					var angle = _renderer.globalAngle(model);

					var p = p0;

					var str = " " + VariableComponent.get_variableName(model);
					var strSize = gc.textExtent(str);

					var tx = new Transform(gc.getDevice());
					tx.translate(p[0], p[1]);
					tx.translate(vector[0] * 20, vector[1] * 20);
					tx.rotate(angle);

					if (FlipComponent.is(model)) {

						var flipX = FlipComponent.get_flipX(model);
						var flipY = FlipComponent.get_flipY(model);

						if (flipX) {
							tx.translate(-strSize.x, 0);
						}

						if (flipY) {
							tx.translate(0, -strSize.y);
						}

					}

					gc.setTransform(tx);
					gc.setForeground(Colors.color(Colors.BLACK));
					gc.drawText(str, 1, 1, true);

					gc.setForeground(Colors.color(Colors.WHITE));
					gc.drawText(str, 0, 0, true);

					gc.setTransform(null);

					tx.dispose();
				}
			}
		}
	}

	@SuppressWarnings("unused")
	private static float[] drawCorner(GC gc, float[] p1, float[] p2) {
		var vector = new float[] { p2[0] - p1[0], p2[1] - p1[1] };

		var d = PhaserEditorUI.distance(0, 0, vector[0], vector[1]);
		vector[0] /= d;
		vector[1] /= d;
		var len = d * 0.2;

		gc.drawLine((int) p1[0], (int) p1[1], (int) (p1[0] + vector[0] * len), (int) (p1[1] + vector[1] * len));
		gc.drawLine((int) p2[0], (int) p2[1], (int) (p2[0] - vector[0] * len), (int) (p2[1] - vector[1] * len));

		return vector;
	}

	private void renderBackground(PaintEvent e) {
		var gc = e.gc;

		gc.setBackground(getBackgroundColor());
		gc.setForeground(getGridColor());

		gc.fillRectangle(0, 0, e.width, e.height);
	}

	private Color getGridColor() {
		return Colors.color(getModel().getForegroundColor());
	}

	private Color getBackgroundColor() {
		return Colors.color(getModel().getBackgroundColor());
	}

	private void renderGrid(PaintEvent e, ZoomCalculator calc) {
		var gc = e.gc;

		gc.setForeground(getGridColor());

		// paint labels

		var initialModelSnapX = 5f;
		var initialModelSnapY = 5f;

		if (getModel().isSnapEnabled()) {
			initialModelSnapX = getModel().getSnapWidth();
			initialModelSnapY = getModel().getSnapHeight();
		}

		var modelSnapX = 10f;
		var modelSnapY = 10f;

		var viewSnapX = 0f;
		var viewSnapY = 0f;

		int i = 1;
		while (viewSnapX < 30) {
			modelSnapX = initialModelSnapX * i;
			viewSnapX = calc.modelToViewWidth(modelSnapX);
			i++;
		}

		i = 1;
		while (viewSnapY < 30) {
			modelSnapY = initialModelSnapY * i;
			viewSnapY = calc.modelToViewHeight(modelSnapY);
			i++;
		}

		_renderModelSnapX = modelSnapX;
		_renderModelSnapY = modelSnapY;

		var modelNextSnapX = modelSnapX * 4;
		var modelNextNextSnapX = modelSnapX * 8;

		var modelNextSnapY = modelSnapY * 4;
		var modelNextNextSnapY = modelSnapY * 8;

		var modelStartX = calc.viewToModelX(0);
		var modelStartY = calc.viewToModelY(0);

		var modelRight = calc.viewToModelX(e.width);
		var modelBottom = calc.viewToModelY(e.height);

		modelStartX = (int) (modelStartX / modelSnapX) * modelSnapX;
		modelStartY = (int) (modelStartY / modelSnapY) * modelSnapY;

		i = 0;
		while (true) {

			var modelX = modelStartX + i * modelSnapX;

			if (modelX > modelRight) {
				break;
			}

			if (modelX % modelNextNextSnapX == 0) {
				gc.setAlpha(255);
			} else if (modelX % modelNextSnapX == 0) {
				gc.setAlpha(150);
			} else {
				gc.setAlpha(100);
			}

			var viewX = calc.modelToViewX(modelX) + Y_LABEL_WIDTH;

			gc.drawLine((int) viewX, X_LABELS_HEIGHT, (int) viewX, e.height);

			i++;
		}

		gc.setAlpha(255);

		i = 0;
		while (true) {

			var modelY = modelStartY + i * modelSnapY;

			if (modelY > modelBottom) {
				break;
			}

			var viewY = calc.modelToViewY(modelY) + X_LABELS_HEIGHT;

			if (modelY % modelNextNextSnapY == 0) {
				gc.setAlpha(255);
			} else if (modelY % modelNextSnapY == 0) {
				gc.setAlpha(150);
			} else {
				gc.setAlpha(100);
			}

			gc.drawLine(X_LABELS_HEIGHT, (int) viewY, e.width, (int) viewY);

			i++;
		}

		gc.setAlpha(255);
	}

	private void renderLabels(PaintEvent e, ZoomCalculator calc) {
		var gc = e.gc;

		gc.setForeground(getGridColor());
		gc.setBackground(getBackgroundColor());

		gc.setAlpha(220);
		gc.fillRectangle(0, 0, e.width, X_LABELS_HEIGHT);
		gc.fillRectangle(0, 0, Y_LABEL_WIDTH, e.height);
		gc.setAlpha(255);

		// paint labels

		var modelSnapX = _renderModelSnapX;
		var modelSnapY = _renderModelSnapY;

		var modelStartX = calc.viewToModelX(0);
		var modelStartY = calc.viewToModelY(0);

		var modelRight = calc.viewToModelX(e.width);
		var modelBottom = calc.viewToModelY(e.height);

		int i;

		i = 2;
		while (true) {
			float viewSnapX = calc.modelToViewWidth(modelSnapX);
			if (viewSnapX > 80) {
				break;
			}
			modelSnapX = _renderModelSnapX * i;
			i++;
		}

		i = 2;
		while (true) {
			float viewSnapY = calc.modelToViewWidth(modelSnapY);
			if (viewSnapY > 80) {
				break;
			}
			modelSnapY = _renderModelSnapY * i;
			i++;
		}

		modelStartX = (int) (modelStartX / modelSnapX) * modelSnapX;
		modelStartY = (int) (modelStartY / modelSnapY) * modelSnapY;

		i = 0;
		while (true) {

			var modelX = modelStartX + i * modelSnapX;

			if (modelX > modelRight) {
				break;
			}

			var viewX = calc.modelToViewX(modelX) + Y_LABEL_WIDTH;

			if (viewX >= Y_LABEL_WIDTH && viewX <= e.width - Y_LABEL_WIDTH) {
				String label = Integer.toString((int) modelX);

				gc.drawString(label, (int) viewX + 5, 0, true);
				gc.drawLine((int) viewX, 0, (int) viewX, X_LABELS_HEIGHT);
			}

			i++;
		}

		gc.drawLine(Y_LABEL_WIDTH, X_LABELS_HEIGHT, e.width, X_LABELS_HEIGHT);

		i = 0;
		while (true) {

			var modelY = modelStartY + i * modelSnapY;

			if (modelY > modelBottom) {
				break;
			}

			var viewY = calc.modelToViewY(modelY) + X_LABELS_HEIGHT;

			if (viewY >= X_LABELS_HEIGHT && viewY <= e.height - X_LABELS_HEIGHT) {

				String label = Integer.toString((int) modelY);
				var labelExtent = gc.stringExtent(label);

				var tx = new Transform(getDevice());

				tx.translate(0, viewY + 5 + labelExtent.x);
				tx.rotate(-90);

				gc.setTransform(tx);

				gc.drawString(label, 0, 0, true);

				gc.setTransform(null);
				tx.dispose();

				gc.drawLine(0, (int) viewY, Y_LABEL_WIDTH, (int) viewY);
			}

			i++;
		}

		gc.drawLine(Y_LABEL_WIDTH, X_LABELS_HEIGHT, Y_LABEL_WIDTH, e.height);

		gc.setAlpha(255);
	}

	@Override
	protected Point getImageSize() {
		return new Point(1, 1);
	}

	ObjectModel pickObject(int x, int y) {
		return pickObject(getModel().getDisplayList(), x, y);
	}

	private ObjectModel pickObject(ObjectModel model, int x, int y) {

		if (model instanceof GameObjectEditorComponent) {
			if (GameObjectEditorComponent.get_gameObjectEditorTransparency(model) == 0) {
				return null;
			}
		}

		if (model instanceof ParentComponent) {
			if (model instanceof GameObjectEditorComponent && GameObjectEditorComponent
					.get_gameObjectEditorClosed(model) /* || groupModel.isPrefabInstance() */) {

				var polygon = _renderer.getObjectChildrenBounds(model);

				if (hitsPolygon(x, y, polygon)) {

					if (hitsImage(x, y, model)) {
						return model;
					}
				}

			} else {

				var children = ParentComponent.get_children(model);

				for (int i = children.size() - 1; i >= 0; i--) {

					var model2 = children.get(i);

					var pick = pickObject(model2, x, y);

					if (pick != null) {
						return pick;
					}
				}
			}
		}

		var polygon = _renderer.getObjectBounds(model);

		if (hitsPolygon(x, y, polygon)) {

			if (hitsImage(x, y, model)) {
				return model;
			}
		}

		return null;
	}

	private boolean hitsImage(int x, int y, ObjectModel model) {
		var renderer = getSceneRenderer();

		var scaledImage = renderer.getModelImageFromCache(model);

		var xy = renderer.sceneToLocal(model, x, y);

		var imgX = (int) xy[0];
		var imgY = (int) xy[1];

		if (scaledImage == null) {
			if (model instanceof ImageModel || model instanceof SpriteModel) {
				var frame = TextureComponent.utils_getTexture(model, getAssetFinder());

				if (frame == null) {
					return false;
				}

				return hitsImage(imgX, imgY, frame);

			}
		}

		if (scaledImage == null) {
			return false;
		}

		return scaledImage.hits(imgX, imgY);
	}

	private static boolean hitsImage(int x, int y, IAssetFrameModel frame) {
		var proxy = AssetPackUI.getImageProxy(frame);

		if (proxy == null) {
			return false;
		}

		return proxy.hits(x, y);
	}

	private static boolean hitsPolygon(int x, int y, float[] polygon) {
		if (polygon == null) {
			return false;
		}

		int npoints = polygon.length / 2;

		if (npoints <= 2) {
			return false;
		}

		var xpoints = new int[npoints];
		var ypoints = new int[npoints];

		for (int i = 0; i < npoints; i++) {
			xpoints[i] = (int) polygon[i * 2];
			ypoints[i] = (int) polygon[i * 2 + 1];
		}

		int hits = 0;

		int lastx = xpoints[npoints - 1];
		int lasty = ypoints[npoints - 1];
		int curx, cury;

		// Walk the edges of the polygon
		for (int i = 0; i < npoints; lastx = curx, lasty = cury, i++) {
			curx = xpoints[i];
			cury = ypoints[i];

			if (cury == lasty) {
				continue;
			}

			int leftx;
			if (curx < lastx) {
				if (x >= lastx) {
					continue;
				}
				leftx = curx;
			} else {
				if (x >= curx) {
					continue;
				}
				leftx = lastx;
			}

			double test1, test2;
			if (cury < lasty) {
				if (y < cury || y >= lasty) {
					continue;
				}
				if (x < leftx) {
					hits++;
					continue;
				}
				test1 = x - curx;
				test2 = y - cury;
			} else {
				if (y < lasty || y >= cury) {
					continue;
				}
				if (x < leftx) {
					hits++;
					continue;
				}
				test1 = x - lastx;
				test2 = y - lasty;
			}

			if (test1 < (test2 / (lasty - cury) * (lastx - curx))) {
				hits++;
			}
		}

		return ((hits & 1) != 0);
	}

	public void reveal(ObjectModel model) {
		var objBounds = _renderer.getObjectBounds(model);

		if (objBounds == null) {
			return;
		}

		var x1 = objBounds[0];
		var y1 = objBounds[1];
		var x2 = objBounds[4];
		var y2 = objBounds[5];

		var w = x2 - x1;
		var h = y2 - y1;

		var canvasBounds = getBounds();

		setOffsetX((int) (getOffsetX() - x1 + Y_LABEL_WIDTH + canvasBounds.width / 2 - w / 2));
		setOffsetY((int) (getOffsetY() - y1 + X_LABELS_HEIGHT + canvasBounds.height / 2 - h / 2));

		redraw();
	}

	public void selectAll() {
		var list = new ArrayList<ObjectModel>();

		var root = getEditor().getSceneModel().getDisplayList();

		root.visitChildren(model -> list.add(model));

		_editor.setSelection(list);
	}

	public void delete() {
		_editor.delete();
	}


	

	public void paste(ObjectModel parent, boolean placeAtCursorPosition) {
		
		
	}

	
	@Override
	public void mouseDoubleClick(MouseEvent e) {
		//
	}

	@Override
	public void mouseDown(MouseEvent e) {
		if (e.button != 1 || isHandModeActivated()) {
			return;
		}

	}

	@Override
	public void mouseUp(MouseEvent e) {

		if (e.button != 1 || isHandModeActivated()) {
			return;
		}

		boolean contains = false;

		if (_dragDetected) {
			_dragDetected = false;

			if (_dragObjectsEvents.isDragging()) {
				_dragObjectsEvents.done();
			}

			return;
		}

	}

	@Override
	public void mouseMove(MouseEvent e) {

		if (isHandModeActivated()) {
			return;
		}

		if (_dragObjectsEvents.isDragging()) {
			_dragObjectsEvents.update(e);
		}

	}

	private boolean _dragDetected;

	@Override
	public void dragDetected(DragDetectEvent e) {
		if (isHandModeActivated()) {
			return;
		}

		_dragDetected = true;

		var obj = pickObject(e.x, e.y);

		if (obj != null) {
			if (_editor.getSelectionList().contains(obj)) {
				_dragObjectsEvents.start(e);
			}
		}
	}

	public boolean isInteractiveDragging() {

		return false;
	}

	public boolean isInteractiveHightlights() {

		return false;
	}

	@Override
	protected boolean hasImage() {
		return true;
	}

	@Override
	public Device getDevice() {
		return getDisplay();
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if (e.character == SWT.SPACE) {
			if (_dragDetected) {
				// don't activate the hand tool if we are dragging objects
				return;
			}
		}

		super.keyPressed(e);
	}

	@Override
	public void keyReleased(KeyEvent e) {
		super.keyReleased(e);

		switch (e.character) {
		case SWT.ESC:
			getEditor().setSelection(List.of());
			break;
		default:
			break;
		}
	}
}
