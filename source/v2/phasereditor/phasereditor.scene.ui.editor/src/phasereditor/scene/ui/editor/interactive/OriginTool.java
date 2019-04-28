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

import static java.lang.System.out;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.GC;

import phasereditor.scene.core.GameObjectEditorComponent;
import phasereditor.scene.core.FlipComponent;
import phasereditor.scene.core.ObjectModel;
import phasereditor.scene.core.OriginComponent;
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
public class OriginTool extends InteractiveTool {

	private static final int BOX = 14;
	private static final int ARROW_LENGTH = 70;
	private int _globalX;
	private int _globalY;
	private boolean _changeX;
	private boolean _changeY;

	public OriginTool(SceneEditor editor, boolean changeX, boolean changeY) {
		super(editor);

		_changeX = changeX;
		_changeY = changeY;
	}

	@Override
	protected boolean canEdit(ObjectModel model) {
		return model instanceof OriginComponent;
	}

	@Override
	public void render(GC gc) {
		var renderer = getRenderer();

		var centerGlobalX = 0;
		var centerGlobalY = 0;
		var globalX = 0;
		var globalY = 0;
		var globalAngle = 0f;

		for (var model : getModels()) {

			float localX;
			float localY;

			{
				var originX = OriginComponent.get_originX(model);
				var originY = OriginComponent.get_originY(model);

				var size = renderer.getObjectSize(model);

				localX = originX * size[0];
				localY = originY * size[1];
			}

			var globalXY = renderer.localToScene(model, localX, localY);

			centerGlobalX += globalXY[0];
			centerGlobalY += globalXY[1];

			globalAngle += renderer.globalAngle(model);

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

				var xy = renderer.localToScene(model, localX + ARROW_LENGTH / scale * flipX, localY);

				globalX += (int) xy[0];
				globalY += (int) xy[1];

			} else {
				var scale = renderer.globalScaleY(model);

				var xy = renderer.localToScene(model, localX, localY + ARROW_LENGTH / scale * flipY);

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

		// paint

		if (_changeX && _changeY) {

			var color = SwtRM.getColor(SWT.COLOR_YELLOW);

			var darkColor = _hightlights ? color : SwtRM.getColor(SWT.COLOR_DARK_YELLOW);

			drawCircle(gc, globalX, globalY, BOX, color, darkColor);
		} else if (doPaint()) {

			var color = SwtRM.getColor(_changeX ? SWT.COLOR_RED : SWT.COLOR_GREEN);

			var darkColor = _hightlights ? color : SwtRM.getColor(_changeX ? SWT.COLOR_DARK_RED : SWT.COLOR_DARK_GREEN);

			drawLine(gc, centerGlobalX, centerGlobalY, globalX, globalY, color, darkColor);

			drawArrow(gc, globalX, globalY, globalAngle + (_changeY ? 90 : 0), BOX, color, darkColor);
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

			for (var model : getModels()) {

				out.println("---");

				var renderer = getEditor().getScene().getSceneRenderer();

				var size = renderer.getObjectSize(model);

				float originX;
				float originY;

				float newOriginX;
				float newOriginY;

				{
					var startCursorLocal = (float[]) model.get("start-cursor-local-xy");
					var cursorLocal = renderer.sceneToLocal(model, e.x, e.y);

					var dx = cursorLocal[0] - startCursorLocal[0];
					var dy = cursorLocal[1] - startCursorLocal[1];

					var originDX = dx / size[0];
					var originDY = dy / size[1];

					originX = (float) model.get("initial-originX");
					originY = (float) model.get("initial-originY");

					newOriginX = originX + (_changeX ? originDX : 0);
					newOriginY = originY + (_changeY ? originDY : 0);
				}

				out.println("new origin " + newOriginX + "," + newOriginY);

				var local1 = new float[] { originX * size[0], originY * size[1] };
				var local2 = new float[] { newOriginX * size[0], newOriginY * size[1] };

				var parent1 = renderer.localToParent(model, local1);
				var parent2 = renderer.localToParent(model, local2);

				var dx = parent2[0] - parent1[0];
				var dy = parent2[1] - parent1[1];

				out.println("delta position " + dx + "," + dy);

				OriginComponent.set_originX(model, newOriginX);
				OriginComponent.set_originY(model, newOriginY);

				TransformComponent.set_x(model, (float) model.get("initial-x") + dx);
				TransformComponent.set_y(model, (float) model.get("initial-y") + dy);

				GameObjectEditorComponent.set_gameObjectEditorDirty(model, true);

			}

			// getEditor().updatePropertyPagesContentWithSelection();
		}
	}

	@Override
	public void mouseDown(MouseEvent e) {
		if (contains(e.x, e.y)) {

			var renderer = getEditor().getScene().getSceneRenderer();

			_dragging = true;

			for (var model : getModels()) {
				model.put("initial-originX", OriginComponent.get_originX(model));
				model.put("initial-originY", OriginComponent.get_originY(model));

				model.put("initial-x", TransformComponent.get_x(model));
				model.put("initial-y", TransformComponent.get_y(model));

				var startCursorLocal = renderer.sceneToLocal(model, e.x, e.y);

				model.put("start-cursor-local-xy", startCursorLocal);
			}
		}
	}

	@Override
	public void mouseUp(MouseEvent e) {
		if (_dragging) {

			var editor = getEditor();

			getModels().forEach(model -> {

				model.put("final-originX", OriginComponent.get_originX(model));
				model.put("final-originY", OriginComponent.get_originY(model));

				model.put("final-x", TransformComponent.get_x(model));
				model.put("final-y", TransformComponent.get_y(model));

				OriginComponent.set_originX(model, (float) model.get("initial-originX"));
				OriginComponent.set_originY(model, (float) model.get("initial-originY"));

				TransformComponent.set_x(model, (float) model.get("initial-x"));
				TransformComponent.set_y(model, (float) model.get("initial-y"));

			});

			var before = SingleObjectSnapshotOperation.takeSnapshot(getModels());

			getModels().forEach(model -> {

				OriginComponent.set_originX(model, (float) model.get("final-originX"));
				OriginComponent.set_originY(model, (float) model.get("final-originY"));

				TransformComponent.set_x(model, (float) model.get("final-x"));
				TransformComponent.set_y(model, (float) model.get("final-y"));

			});

			var after = SingleObjectSnapshotOperation.takeSnapshot(getModels());

			editor.executeOperation(new SingleObjectSnapshotOperation(before, after, "Set origin.", true));

			editor.setDirty(true);

			if (editor.getOutline() != null) {
				editor.refreshOutline_basedOnId();
			}

			getScene().redraw();

		}
		_dragging = false;
	}

}
