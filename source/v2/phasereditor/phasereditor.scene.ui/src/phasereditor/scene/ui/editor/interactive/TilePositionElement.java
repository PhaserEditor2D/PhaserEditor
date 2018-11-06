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

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.wb.swt.SWTResourceManager;

import phasereditor.scene.core.ObjectModel;
import phasereditor.scene.core.TileSpriteComponent;
import phasereditor.scene.ui.editor.SceneEditor;

/**
 * @author arian
 *
 */
@SuppressWarnings("boxing")
public class TilePositionElement extends RenderInteractiveElement {

	private static final int ARROW_LENGTH = 80;
	private int _globalX;
	private int _globalY;
	private boolean _dragging;
	private int _initialGlobalY;
	private int _initialGlobalX;
	private boolean _changeX;
	private boolean _changeY;

	public TilePositionElement(SceneEditor editor, List<ObjectModel> models, boolean changeX, boolean changeY) {
		super(editor, models);

		_changeX = changeX;
		_changeY = changeY;
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

			if (_changeX && _changeY) {

				globalX = centerGlobalX;
				globalY = centerGlobalY;

			} else if (_changeX) {

				var scale = renderer.globalScaleX(model);

				var xy = renderer.localToScene(model, modelX + ARROW_LENGTH / scale, modelY);

				globalX += (int) xy[0];
				globalY += (int) xy[1];

			} else {
				var scale = renderer.globalScaleY(model);

				var xy = renderer.localToScene(model, modelX, modelY + ARROW_LENGTH / scale);

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

		if (_changeX && _changeY) {
			gc.setBackground(SWTResourceManager.getColor(SWT.COLOR_BLACK));

			fillRect(gc, globalX, _globalY, globalAngle, 12);

			gc.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
			fillRect(gc, globalX, _globalY, globalAngle, 10);

		} else {
			gc.setBackground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
			gc.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));

			fillRect(gc, globalX, _globalY, globalAngle, 12);

			gc.setLineWidth(3);
			gc.drawLine(centerGlobalX, centerGlobalY, globalX, globalY);
			gc.setLineWidth(1);

			var color = SWTResourceManager.getColor(_changeX ? SWT.COLOR_RED : SWT.COLOR_GREEN);

			gc.setBackground(color);
			gc.setForeground(color);

			gc.drawLine(centerGlobalX, centerGlobalY, globalX, globalY);

			fillRect(gc, globalX, globalY, globalAngle, 10);
		}

		_globalX = globalX;
		_globalY = globalY;

	}

	private static void fillRect(GC gc, int globalX, int globalY, float globalAngle, int size) {
		var tx = new Transform(gc.getDevice());

		tx.translate(globalX, globalY);
		tx.rotate(globalAngle);
		gc.setTransform(tx);

		gc.fillRectangle(-size / 2, -size / 2, size, size);

		gc.setTransform(null);

		tx.dispose();
	}

	@Override
	public boolean contains(int sceneX, int sceneY) {

		if (_dragging) {
			return true;
		}

		boolean b = _globalX - 5 <= sceneX

				&& _globalX + 5 >= sceneX

				&& _globalY - 5 <= sceneY

				&& _globalY + 5 >= sceneY;

		return b;
	}

	@Override
	public void mouseMove(MouseEvent e) {
		if (_dragging && contains(e.x, e.y)) {

			for (var model : getModels()) {

				var initialXY = (float[]) model.get("initial-local-xy");
				var localXY = getRenderer().sceneToLocal(model, e.x, e.y);

				var dx = localXY[0] - initialXY[0];
				var dy = localXY[1] - initialXY[1];

				var initialTilePositionX = (float) model.get("initial-tilePositionX");
				var initialTilePositionY = (float) model.get("initial-tilePositionY");

				var tilePositionX = initialTilePositionX + dx;
				var tilePositionY = initialTilePositionY + dy;

				if (_changeX) {
					TileSpriteComponent.set_tilePositionX(model, tilePositionX);
				}

				if (_changeY) {
					TileSpriteComponent.set_tilePositionY(model, tilePositionY);
				}

				model.setDirty(true);

				getEditor().updatePropertyPagesContentWithSelection();
			}
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
		_dragging = false;
	}

}
