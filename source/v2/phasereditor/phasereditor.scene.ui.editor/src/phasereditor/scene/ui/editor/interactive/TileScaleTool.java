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

import phasereditor.scene.core.GameObjectEditorComponent;
import phasereditor.scene.core.ObjectModel;
import phasereditor.scene.core.TextureComponent;
import phasereditor.scene.core.TileSpriteComponent;
import phasereditor.scene.core.TileSpriteModel;
import phasereditor.scene.ui.editor.SceneEditor;
import phasereditor.scene.ui.editor.undo.SingleObjectSnapshotOperation;
import phasereditor.ui.PhaserEditorUI;
import phasereditor.ui.SwtRM;

/**
 * @author arian
 *
 */
@SuppressWarnings("boxing")
public class TileScaleTool extends InteractiveTool {

	private static final int BOX = 14;
	private int _globalX;
	private int _globalY;
	private int _initialGlobalY;
	private int _initialGlobalX;
	private boolean _changeX;
	private boolean _changeY;

	public TileScaleTool(SceneEditor editor, boolean changeX, boolean changeY) {
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
			var frame = TextureComponent.utils_getTexture(model, getAssetFinder());
			var textureWidth = frame.getFrameData().srcSize.x;
			var textureHeight = frame.getFrameData().srcSize.y;

			var modelX = TileSpriteComponent.get_tilePositionX(model);
			var modelY = TileSpriteComponent.get_tilePositionY(model);

			var globalXY = renderer.localToScene(model, modelX, modelY);

			centerGlobalX += globalXY[0];
			centerGlobalY += globalXY[1];

			globalAngle += renderer.globalAngle(model);

			if (_changeX && _changeY) {

				globalX = centerGlobalX;
				globalY = centerGlobalY;

			} else if (_changeX) {

				var tileScale = TileSpriteComponent.get_tileScaleX(model);
				var len = textureWidth * tileScale;

				var xy = renderer.localToScene(model, modelX + len, modelY);

				globalX += (int) xy[0];
				globalY += (int) xy[1];

			} else {

				var tileScale = TileSpriteComponent.get_tileScaleY(model);
				var len = textureHeight * tileScale;

				var xy = renderer.localToScene(model, modelX, modelY + len);

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
			var color = SwtRM.getColor(_changeX ? SWT.COLOR_RED : SWT.COLOR_GREEN);
			var darkColor = _hightlights ? color
					: SwtRM.getColor(_changeX ? SWT.COLOR_DARK_RED : SWT.COLOR_DARK_GREEN);

			drawRect(gc, globalX, globalY, globalAngle, BOX, color, darkColor);
		} else if (doPaint()) {
			gc.setBackground(SwtRM.getColor(SWT.COLOR_BLACK));
			gc.setForeground(SwtRM.getColor(SWT.COLOR_BLACK));

			var color = SwtRM.getColor(_changeX ? SWT.COLOR_RED : SWT.COLOR_GREEN);
			var darkColor = _hightlights ? color
					: SwtRM.getColor(_changeX ? SWT.COLOR_DARK_RED : SWT.COLOR_DARK_GREEN);

			drawLine(gc, centerGlobalX, centerGlobalY, globalX, globalY, color, darkColor);

			drawRect(gc, globalX, globalY, globalAngle + (_changeY ? 90 : 0), BOX, color, darkColor);
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
				var frame = TextureComponent.utils_getTexture(model, getAssetFinder());
				var textureWidth = frame.getFrameData().srcSize.x;
				var textureHeight = frame.getFrameData().srcSize.y;

				var initialLocalXY = (float[]) model.get("initial-local-xy");
				var localXY = getRenderer().sceneToLocal(model, e.x, e.y);

				var dx = localXY[0] - initialLocalXY[0];
				var dy = localXY[1] - initialLocalXY[1];

				var initialTileScaleX = (float) model.get("initial-tileScaleX");
				var initialTileScaleY = (float) model.get("initial-tileScaleY");

				var tileScaleX = initialTileScaleX + dx / textureWidth;
				var tileScaleY = initialTileScaleY + dy / textureHeight;

				if (_changeX) {
					TileSpriteComponent.set_tileScaleX(model, tileScaleX);
				}

				if (_changeY) {
					TileSpriteComponent.set_tileScaleY(model, tileScaleY);
				}

				GameObjectEditorComponent.set_gameObjectEditorDirty(model, true);
			}

//			getEditor().updatePropertyPagesContentWithSelection();
		}
	}

	@Override
	public void mouseDown(MouseEvent e) {
		if (contains(e.x, e.y)) {

			_dragging = true;

			_initialGlobalX = _globalX;
			_initialGlobalY = _globalY;

			for (var model : getModels()) {
				model.put("initial-tileScaleX", TileSpriteComponent.get_tileScaleX(model));
				model.put("initial-tileScaleY", TileSpriteComponent.get_tileScaleY(model));

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

				model.put("final-tileScaleX", TileSpriteComponent.get_tileScaleX(model));
				model.put("final-tileScaleY", TileSpriteComponent.get_tileScaleY(model));

				TileSpriteComponent.set_tileScaleX(model, (float) model.get("initial-tileScaleX"));
				TileSpriteComponent.set_tileScaleY(model, (float) model.get("initial-tileScaleY"));

			});

			var before = SingleObjectSnapshotOperation.takeSnapshot(getModels());

			getModels().forEach(model -> {

				TileSpriteComponent.set_tileScaleX(model, (float) model.get("final-tileScaleX"));
				TileSpriteComponent.set_tileScaleY(model, (float) model.get("final-tileScaleY"));

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
