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
import org.eclipse.wb.swt.SWTResourceManager;

import phasereditor.scene.core.FlipComponent;
import phasereditor.scene.core.ObjectModel;
import phasereditor.scene.core.TileSpriteComponent;
import phasereditor.scene.core.TileSpriteModel;
import phasereditor.scene.ui.editor.SceneEditor;
import phasereditor.scene.ui.editor.undo.SingleObjectSnapshotOperation;
import phasereditor.ui.PhaserEditorUI;

/**
 * @author arian
 *
 */
@SuppressWarnings("boxing")
public class TilePositionTool extends InteractiveTool {

	private static final int BOX = 14;
	private static final int ARROW_LENGTH = 80;
	private int _globalX;
	private int _globalY;
	private int _initialGlobalY;
	private int _initialGlobalX;
	private boolean _changeX;
	private boolean _changeY;
	private boolean _hightlights;

	public TilePositionTool(SceneEditor editor, boolean changeX, boolean changeY) {
		super(editor);

		_changeX = changeX;
		_changeY = changeY;
	}

	@Override
	protected boolean canEdit(ObjectModel model) {
		return model instanceof TileSpriteModel;
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
			var modelX = TileSpriteComponent.get_tilePositionX(model);
			var modelY = TileSpriteComponent.get_tilePositionY(model);

			var globalXY = renderer.localToScene(model, modelX, modelY);

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

				var xy = renderer.localToScene(model, modelX + ARROW_LENGTH / scale * flipX, modelY);

				globalX += (int) xy[0];
				globalY += (int) xy[1];

			} else {
				var scale = renderer.globalScaleY(model);

				var xy = renderer.localToScene(model, modelX, modelY + ARROW_LENGTH / scale * flipY);

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
			var color = SWTResourceManager.getColor(_changeX ? SWT.COLOR_RED : SWT.COLOR_GREEN);
			var darkColor = _hightlights ? color
					: SWTResourceManager.getColor(_changeX ? SWT.COLOR_DARK_RED : SWT.COLOR_DARK_GREEN);

			drawRect(gc, globalX, _globalY, globalAngle, BOX, color, darkColor);
		} else if (doPaint()) {
			gc.setBackground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
			gc.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));

			var color = SWTResourceManager.getColor(_changeX ? SWT.COLOR_RED : SWT.COLOR_GREEN);
			var darkColor = _hightlights ? color
					: SWTResourceManager.getColor(_changeX ? SWT.COLOR_DARK_RED : SWT.COLOR_DARK_GREEN);

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

				var initialLocalXY = (float[]) model.get("initial-local-xy");
				var localXY = getRenderer().sceneToLocal(model, e.x, e.y);

				var dx = localXY[0] - initialLocalXY[0];
				var dy = localXY[1] - initialLocalXY[1];

				var initialTilePositionX = (float) model.get("initial-tilePositionX");
				var initialTilePositionY = (float) model.get("initial-tilePositionY");

				var tilePositionX = initialTilePositionX + dx;
				var tilePositionY = initialTilePositionY + dy;

				{
					// snap
					var sceneModel = getEditor().getSceneModel();
					tilePositionX = sceneModel.snapValueX(tilePositionX);
					tilePositionY = sceneModel.snapValueY(tilePositionY);
				}

				if (_changeX) {
					TileSpriteComponent.set_tilePositionX(model, tilePositionX);
				}

				if (_changeY) {
					TileSpriteComponent.set_tilePositionY(model, tilePositionY);
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

			_initialGlobalX = _globalX;
			_initialGlobalY = _globalY;

			for (var model : getModels()) {
				model.put("initial-tilePositionX", TileSpriteComponent.get_tilePositionX(model));
				model.put("initial-tilePositionY", TileSpriteComponent.get_tilePositionY(model));

				var xy = getRenderer().sceneToLocal(model, _initialGlobalX, _initialGlobalY);
				model.put("initial-local-xy", xy);

			}
		}
	}

	@Override
	public void mouseUp(MouseEvent e) {
		if (_dragging) {

			var editor = getEditor();

			getModels().forEach(model -> {

				model.put("final-tilePositionX", TileSpriteComponent.get_tilePositionX(model));
				model.put("final-tilePositionY", TileSpriteComponent.get_tilePositionY(model));

				TileSpriteComponent.set_tilePositionX(model, (float) model.get("initial-tilePositionX"));
				TileSpriteComponent.set_tilePositionY(model, (float) model.get("initial-tilePositionY"));

			});

			var before = SingleObjectSnapshotOperation.takeSnapshot(getModels());

			getModels().forEach(model -> {

				TileSpriteComponent.set_tilePositionX(model, (float) model.get("final-tilePositionX"));
				TileSpriteComponent.set_tilePositionY(model, (float) model.get("final-tilePositionY"));

			});

			var after = SingleObjectSnapshotOperation.takeSnapshot(getModels());

			editor.executeOperation(new SingleObjectSnapshotOperation(before, after, "Set tile position.", true));

			editor.setDirty(true);

			if (editor.getOutline() != null) {
				editor.refreshOutline_basedOnId();
			}

			getScene().redraw();

		}
		_dragging = false;
	}

}
