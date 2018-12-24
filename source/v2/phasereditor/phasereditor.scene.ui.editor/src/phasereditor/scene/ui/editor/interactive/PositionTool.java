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
package phasereditor.scene.ui.editor.interactive;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.widgets.Display;

import phasereditor.scene.core.GameObjectEditorComponent;
import phasereditor.scene.core.FlipComponent;
import phasereditor.scene.core.ObjectModel;
import phasereditor.scene.core.OriginComponent;
import phasereditor.scene.core.ParentComponent;
import phasereditor.scene.core.TransformComponent;
import phasereditor.scene.ui.editor.SceneEditor;
import phasereditor.scene.ui.editor.undo.SingleObjectSnapshotOperation;
import phasereditor.ui.PhaserEditorUI;
import phasereditor.ui.SwtRM;

/**
 * @author arian
 *
 */
@SuppressWarnings("boxing")
public class PositionTool extends InteractiveTool {

	private static final int BOX = 14;
	private static final int ARROW_LENGTH = 80;
	private int _globalX;
	private int _globalY;
	private boolean _changeX;
	private boolean _changeY;
	private float[] _arrowPoint;
	private float[] _startDragCursorPoint;
	private float[] _centerPoint;
	private float[] _startVector;

	public PositionTool(SceneEditor editor, boolean changeX, boolean changeY) {
		super(editor);

		_changeX = changeX;
		_changeY = changeY;
	}

	@Override
	protected boolean canEdit(ObjectModel model) {
		return model instanceof TransformComponent;
	}

	@Override
	public void render(GC gc) {
		var renderer = getRenderer();

		var centerGlobalX = 0;
		var centerGlobalY = 0;
		var globalX = 0;
		var globalY = 0;
		var globalAngle = 0f;

		var localCoords = getScene().isTransformLocalCoords();

		for (var model : getModels()) {

			var objSize = renderer.getObjectSize(model);

			float modelX = 0;
			float modelY = 0;

			if (model instanceof OriginComponent) {
				modelX = objSize[0] * OriginComponent.get_originX(model);
				modelY = objSize[1] * OriginComponent.get_originY(model);
			}

			var globalXY = renderer.localToScene(model, modelX, modelY);

			centerGlobalX += globalXY[0];
			centerGlobalY += globalXY[1];

			if (localCoords) {
				globalAngle += renderer.globalAngle(model);
			}

			var flipX = 1;
			var flipY = 1;

			if (model instanceof FlipComponent) {
				flipX = FlipComponent.get_flipX(model) ? -1 : 1;
				flipY = FlipComponent.get_flipY(model) ? -1 : 1;
			}

			if (_changeX && _changeY) {

				globalX = centerGlobalX;
				globalY = centerGlobalY;

			} else if (_changeX) {
				var scale = renderer.globalScaleX(model);

				float[] xy;

				if (localCoords) {
					xy = renderer.localToScene(model, modelX + ARROW_LENGTH / scale * flipX, modelY);
				} else {
					xy = renderer.localToScene(model, modelX, modelY);
					xy[0] += ARROW_LENGTH;
				}

				globalX += (int) xy[0];
				globalY += (int) xy[1];

			} else {
				var scale = renderer.globalScaleY(model);

				float[] xy;

				if (localCoords) {
					xy = renderer.localToScene(model, modelX, modelY + ARROW_LENGTH / scale * flipY);
				} else {
					xy = renderer.localToScene(model, modelX, modelY);
					xy[1] += ARROW_LENGTH;
				}

				globalX += (int) xy[0];
				globalY += (int) xy[1];

			}

		}

		var size = getModels().size();

		globalX = globalX / size;
		globalY = globalY / size;

		centerGlobalX = centerGlobalX / size;
		centerGlobalY = centerGlobalY / size;

		globalAngle = globalAngle / size;

		_globalX = globalX;
		_globalY = globalY;

		_arrowPoint = new float[] { globalX, globalY };
		_centerPoint = new float[] { centerGlobalX, centerGlobalY };

		// paint

		if (doPaint()) {
			if (_changeX && _changeY) {
				var color = SwtRM.getColor(SWT.COLOR_YELLOW);
				var darkColor = _hightlights ? color : SwtRM.getColor(SWT.COLOR_DARK_YELLOW);

				drawRect(gc, globalX, globalY, globalAngle, BOX, color, darkColor);
			} else {
				var color = SwtRM.getColor(_changeX ? SWT.COLOR_RED : SWT.COLOR_GREEN);
				var darkColor = _hightlights ? color
						: SwtRM.getColor(_changeX ? SWT.COLOR_DARK_RED : SWT.COLOR_DARK_GREEN);

				drawLine(gc, centerGlobalX, centerGlobalY, globalX, globalY, color, darkColor);

				drawArrow(gc, globalX, globalY, globalAngle + (_changeY ? 90 : 0), BOX, color, darkColor);
			}
		}
	}

	@Override
	public boolean contains(int sceneX, int sceneY) {

		if (_dragging) {
			return true;
		}

		var contains = PhaserEditorUI.distance(sceneX, sceneY, _globalX, _globalY) <= BOX;

		_hightlights = contains;

		return contains;
	}

	@Override
	public void mouseMove(MouseEvent e) {
		if (_dragging && contains(e.x, e.y)) {

			var renderer = getRenderer();

			var localCoords = getScene().isTransformLocalCoords();

			var changeXY = _changeX && _changeY;

			if (localCoords || changeXY) {

				if (changeXY) {
					for (var model : getModels()) {

						var parent = ParentComponent.get_parent(model);

						var p0 = renderer.sceneToLocal(parent, _startDragCursorPoint);
						var p1 = renderer.sceneToLocal(parent, e.x, e.y);

						var dx = p1[0] - p0[0];
						var dy = p1[1] - p0[1];

						var initialModelX = (float) model.get("initial-model-x");
						var initialModelY = (float) model.get("initial-model-y");

						var modelX = initialModelX + dx;
						var modelY = initialModelY + dy;

						{
							// snap
							var sceneModel = getEditor().getSceneModel();
							modelX = sceneModel.snapValueX(modelX);
							modelY = sceneModel.snapValueY(modelY);
						}

						TransformComponent.set_x(model, modelX);
						TransformComponent.set_y(model, modelY);

						GameObjectEditorComponent.set_gameObjectEditorDirty(model, true);

					}

				} else {

					for (var model : getModels()) {

						var parent = ParentComponent.get_parent(model);

						var vector = new float[] { _changeX ? 1 : 0, _changeY ? 1 : 0 };
						var tx = new Transform(Display.getDefault());
						tx.rotate(TransformComponent.get_angle(model));
						tx.transform(vector);
						tx.dispose();

						var p0 = renderer.sceneToLocal(parent, _startDragCursorPoint);
						var p1 = renderer.sceneToLocal(parent, e.x, e.y);

						var d = PhaserEditorUI.distance(p0, p1);

						var moveVector = new float[] { e.x - _startDragCursorPoint[0], e.y - _startDragCursorPoint[1] };
						var ang = PhaserEditorUI.angle(_startVector, moveVector);

						if (ang > 90) {
							d = -d;
						}

						vector[0] *= d;
						vector[1] *= d;

						var initialModelX = (float) model.get("initial-model-x");
						var initialModelY = (float) model.get("initial-model-y");

						var modelX = initialModelX + vector[0];
						var modelY = initialModelY + vector[1];

						{
							// snap
							var sceneModel = getEditor().getSceneModel();
							modelX = sceneModel.snapValueX(modelX);
							modelY = sceneModel.snapValueY(modelY);
						}

						TransformComponent.set_x(model, modelX);
						TransformComponent.set_y(model, modelY);

						GameObjectEditorComponent.set_gameObjectEditorDirty(model, true);
					}

				}
			} else {

				// global coords

				var dx = e.x - _startDragCursorPoint[0];
				var dy = e.y - _startDragCursorPoint[1];

				for (var model : getModels()) {

					var initXY = (float[]) model.get("initial-scene-xy");

					var initY = initXY[1];
					var initX = initXY[0];

					float[] point;

					if (changeXY) {
						point = new float[] { initX + dx, initY + dy };
					} else {
						if (_changeX) {
							point = new float[] { initX + dx, initY };
						} else {
							point = new float[] { initX, initY + dy };
						}

						var parent = ParentComponent.get_parent(model);

						point = renderer.sceneToLocal(parent, point);

						var modelX = point[0];
						var modelY = point[1];

						{
							// snap
							var sceneModel = getEditor().getSceneModel();
							point[0] = sceneModel.snapValueX(modelX);
							point[1] = sceneModel.snapValueY(modelY);
						}

						TransformComponent.set_x(model, modelX);
						TransformComponent.set_y(model, modelY);

						GameObjectEditorComponent.set_gameObjectEditorDirty(model, true);
					}
				}
			}

			getEditor().updatePropertyPagesContentWithSelection();
		}
	}

	@Override
	public void mouseDown(MouseEvent e) {
		if (contains(e.x, e.y)) {

			_dragging = true;

			_startDragCursorPoint = new float[] { e.x, e.y };

			_startVector = new float[] { _arrowPoint[0] - _centerPoint[0], _arrowPoint[1] - _centerPoint[1] };

			var renderer = getRenderer();

			for (var model : getModels()) {
				float x = TransformComponent.get_x(model);
				float y = TransformComponent.get_y(model);

				model.put("initial-model-x", x);
				model.put("initial-model-y", y);

				var parent = ParentComponent.get_parent(model);

				model.put("initial-scene-xy", renderer.localToScene(parent, x, y));
			}
		}
	}

	@Override
	public void mouseUp(MouseEvent e) {
		if (_dragging) {

			var editor = getEditor();

			getModels().forEach(model -> {

				model.put("final-model-x", TransformComponent.get_x(model));
				model.put("final-model-y", TransformComponent.get_y(model));

				TransformComponent.set_x(model, (float) model.get("initial-model-x"));
				TransformComponent.set_y(model, (float) model.get("initial-model-y"));

			});

			var before = SingleObjectSnapshotOperation.takeSnapshot(getModels());

			getModels().forEach(model -> {

				TransformComponent.set_x(model, (float) model.get("final-model-x"));
				TransformComponent.set_y(model, (float) model.get("final-model-y"));

			});

			var after = SingleObjectSnapshotOperation.takeSnapshot(getModels());

			editor.executeOperation(new SingleObjectSnapshotOperation(before, after, "Set position.", true));

			editor.setDirty(true);

			editor.getScene().redraw();

		}

		_dragging = false;
	}

}
