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
import org.eclipse.swt.graphics.RGB;
import org.eclipse.wb.swt.SWTResourceManager;

import phasereditor.scene.core.ObjectModel;
import phasereditor.scene.core.OriginComponent;
import phasereditor.scene.core.TransformComponent;
import phasereditor.scene.ui.editor.SceneEditor;
import phasereditor.scene.ui.editor.undo.SingleObjectSnapshotOperation;
import phasereditor.ui.ColorUtil;
import phasereditor.ui.PhaserEditorUI;

/**
 * @author arian
 *
 */
@SuppressWarnings("boxing")
public class AngleTool extends InteractiveTool {
	private static RGB[] COLORS = { null, ColorUtil.RED.rgb, ColorUtil.GREEN.rgb, ColorUtil.BLUE.rgb };

	private static final int BOX = 14;
	private int _centerX;
	private int _centerY;
	private int _order;
	private boolean _hightlights;

	private int _radius;

	private float[] _cursorStart;

	public AngleTool(SceneEditor editor, int order) {
		super(editor);
		_order = order;
		_radius = _order * 50;
	}

	@Override
	protected boolean canEdit(ObjectModel model) {
		return model instanceof TransformComponent;
	}

	@Override
	public void render(GC gc) {
		var renderer = getRenderer();

		var globalX = 0;
		var globalY = 0;

		for (var model : getModels()) {

			var objSize = renderer.getObjectSize(model);

			float modelX = 0;
			float modelY = 0;

			if (model instanceof OriginComponent) {
				modelX = objSize[0] * OriginComponent.get_originX(model);
				modelY = objSize[1] * OriginComponent.get_originY(model);
			}

			var globalXY = renderer.localToScene(model, modelX, modelY);

			globalX += globalXY[0];
			globalY += globalXY[1];

		}

		var size = getModels().size();

		globalX = globalX / size;
		globalY = globalY / size;

		_centerX = globalX;
		_centerY = globalY;

		// paint

		fillCircle(gc, globalX, globalY, BOX, SWTResourceManager.getColor(SWT.COLOR_BLUE));

		if (doPaint()) {

			gc.setForeground(SWTResourceManager.getColor(_hightlights ? ColorUtil.WHITE.rgb : COLORS[_order]));

			gc.drawOval(_centerX - _radius, _centerY - _radius, _radius * 2, _radius * 2);
		}
	}

	@Override
	public boolean contains(int sceneX, int sceneY) {

		if (_dragging) {
			return true;
		}

		var d = PhaserEditorUI.distance(sceneX, sceneY, _centerX, _centerY);

		var diff = Math.abs(d - _radius);

		return _hightlights = diff < 10;
	}

	@Override
	public void mouseMove(MouseEvent e) {
		if (_dragging && contains(e.x, e.y)) {

			var dx = _cursorStart[0] - e.x;
			var dy = _cursorStart[1] - e.y;

			if (Math.abs(dx) < 1 || Math.abs(dy) < 1) {
				return;
			}

			for (var model : getModels()) {
				var x = e.x;
				var y = e.y;
				var initX = _cursorStart[0];
				var initY = _cursorStart[1];

				var deltaRadians = angleBetweenTwoPointsWithFixedPoint(x, y, initX, initY, _centerX, _centerY);

				var deltaAngle = (float) Math.toDegrees(deltaRadians);

				TransformComponent.set_angle(model, (float) model.get("initial-angle") + deltaAngle);
			}
			
			getEditor().updatePropertyPagesContentWithSelection();
		}

	}

	public static float angleBetweenTwoPointsWithFixedPoint(float point1X, float point1Y, float point2X, float point2Y,
			float fixedX, float fixedY) {

		var angle1 = (float) Math.atan2(point1Y - fixedY, point1X - fixedX);
		var angle2 = (float) Math.atan2(point2Y - fixedY, point2X - fixedX);

		return angle1 - angle2;
	}

	@Override
	public void mouseDown(MouseEvent e) {
		if (contains(e.x, e.y)) {

			_dragging = true;

			for (var model : getModels()) {
				model.put("initial-angle", TransformComponent.get_angle(model));
			}

			_cursorStart = new float[] { e.x, e.y };
		}
	}

	@Override
	public void mouseUp(MouseEvent e) {
		if (_dragging) {

			var editor = getEditor();

			getModels().forEach(model -> {

				model.put("final-angle", TransformComponent.get_angle(model));

				TransformComponent.set_angle(model, (float) model.get("initial-angle"));

			});

			var before = SingleObjectSnapshotOperation.takeSnapshot(getModels());

			getModels().forEach(model -> {

				TransformComponent.set_angle(model, (float) model.get("final-angle"));

			});

			var after = SingleObjectSnapshotOperation.takeSnapshot(getModels());

			editor.executeOperation(new SingleObjectSnapshotOperation(before, after, "Set position.", true));

			editor.setDirty(true);

			editor.getScene().redraw();

		}

		_dragging = false;
	}

}
