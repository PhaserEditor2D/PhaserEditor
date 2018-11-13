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
import org.eclipse.wb.swt.SWTResourceManager;

import phasereditor.scene.core.ObjectModel;
import phasereditor.scene.core.OriginComponent;
import phasereditor.scene.core.TransformComponent;
import phasereditor.scene.ui.editor.SceneEditor;
import phasereditor.scene.ui.editor.undo.SingleObjectSnapshotOperation;
import phasereditor.ui.PhaserEditorUI;

/**
 * @author arian
 *
 */
@SuppressWarnings("boxing")
public class ScaleTool extends InteractiveTool {

	private static final int BOX = 14;
	private int _globalX;
	private int _globalY;
	private boolean _changeX;
	private boolean _changeY;
	private boolean _hightlights;

	public ScaleTool(SceneEditor editor, boolean changeX, boolean changeY) {
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

		for (var model : getModels()) {
			var size = getRenderer().getObjectSize(model);

			var modelX = size[0];
			var modelY = size[1];

			var globalXY = renderer.localToScene(model, modelX, modelY);

			centerGlobalX += globalXY[0];
			centerGlobalY += globalXY[1];

			globalAngle += renderer.globalAngle(model);

			if (_changeX && _changeY) {

				globalX = centerGlobalX;
				globalY = centerGlobalY;

			} else {
				var xy = renderer.localToScene(model, _changeX ? size[0] : size[0] / 2,
						_changeY ? size[1] : size[1] / 2);
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

		if (doPaint()) {
			if (_changeX && _changeY) {
				fillRect(gc, globalX, globalY, globalAngle, BOX,
						SWTResourceManager.getColor(_hightlights ? SWT.COLOR_WHITE : SWT.COLOR_YELLOW));
			} else {
				gc.setBackground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
				gc.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));

				var color = SWTResourceManager
						.getColor(_hightlights ? SWT.COLOR_WHITE : (_changeX ? SWT.COLOR_RED : SWT.COLOR_GREEN));

				gc.setBackground(color);
				gc.setForeground(color);

				fillRect(gc, globalX, globalY, globalAngle + (_changeY ? 90 : 0), BOX, color);
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

			out.println("----");

			var renderer = getRenderer();

			for (var model : getModels()) {

				var matrix = (float[]) model.get("initial-matrix");
				var localCursor = renderer.sceneToLocal(matrix, e.x, e.y);
				var initialLocalCursor = (float[]) model.get("initial-cursor-local-xy");

				var localDX = localCursor[0] - initialLocalCursor[0];
				var localDY = localCursor[1] - initialLocalCursor[1];

				out.println("local delta " + localDX);

				var size = getRenderer().getObjectSize(model);

				var scaleX = (float) model.get("initial-scaleX");
				var scaleY = (float) model.get("initial-scaleY");

				var scaleDX = localDX / size[0] * scaleX;
				var scaleDY = localDY / size[1] * scaleY;

				out.println("scale delta " + scaleDX);

				var newScaleX = scaleX + scaleDX;
				var newScaleY = scaleY + scaleDY;

				var x = (float) model.get("initial-x") + localDX * scaleX * OriginComponent.get_originX(model);
				var y = (float) model.get("initial-y") + localDY * scaleY * OriginComponent.get_originY(model);

				if (_changeX) {
					TransformComponent.set_scaleX(model, newScaleX);
					TransformComponent.set_x(model, x);
				}

				if (_changeY) {
					TransformComponent.set_scaleY(model, newScaleY);
					TransformComponent.set_y(model, y);
				}

				model.setDirty(true);
			}

			getEditor().updatePropertyPagesContentWithSelection();
		}
	}

	@Override
	public void mouseDown(MouseEvent e) {
		if (contains(e.x, e.y)) {

			_dragging = true;

			var renderer = getRenderer();

			for (var model : getModels()) {

				var scaleX = TransformComponent.get_scaleX(model);
				var scaleY = TransformComponent.get_scaleY(model);

				model.put("initial-scaleX", scaleX);
				model.put("initial-scaleY", scaleY);

				model.put("initial-x", TransformComponent.get_x(model));
				model.put("initial-y", TransformComponent.get_y(model));

				model.put("initial-cursor-local-xy", renderer.sceneToLocal(model, e.x, e.y));

				model.put("initial-matrix", renderer.getObjectMatrix(model));
			}

		}
	}

	@Override
	public void mouseUp(MouseEvent e) {
		if (_dragging) {

			var editor = getEditor();

			getModels().forEach(model -> {

				model.put("final-scaleX", TransformComponent.get_scaleX(model));
				model.put("final-scaleY", TransformComponent.get_scaleY(model));
				
				model.put("final-x", TransformComponent.get_x(model));
				model.put("final-y", TransformComponent.get_y(model));

				TransformComponent.set_scaleX(model, (float) model.get("initial-scaleX"));
				TransformComponent.set_scaleY(model, (float) model.get("initial-scaleY"));
				
				TransformComponent.set_x(model, (float) model.get("initial-x"));
				TransformComponent.set_y(model, (float) model.get("initial-y"));

			});

			var before = SingleObjectSnapshotOperation.takeSnapshot(getModels());

			getModels().forEach(model -> {

				TransformComponent.set_scaleX(model, (float) model.get("final-scaleX"));
				TransformComponent.set_scaleY(model, (float) model.get("final-scaleY"));
				
				TransformComponent.set_x(model, (float) model.get("final-x"));
				TransformComponent.set_y(model, (float) model.get("final-y"));

			});

			var after = SingleObjectSnapshotOperation.takeSnapshot(getModels());

			editor.executeOperation(new SingleObjectSnapshotOperation(before, after, "Set scale.", true));

			editor.setDirty(true);

			if (editor.getOutline() != null) {
				editor.refreshOutline_basedOnId();
			}

			getScene().redraw();

		}

		_dragging = false;
	}

}
