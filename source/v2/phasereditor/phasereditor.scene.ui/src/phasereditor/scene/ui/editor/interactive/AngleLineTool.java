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

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.widgets.Display;
import org.eclipse.wb.swt.SWTResourceManager;

import phasereditor.scene.core.ObjectModel;
import phasereditor.scene.core.OriginComponent;
import phasereditor.scene.core.ParentComponent;
import phasereditor.scene.core.TransformComponent;
import phasereditor.scene.ui.editor.SceneEditor;
import phasereditor.ui.ColorUtil;

/**
 * @author arian
 *
 */
public class AngleLineTool extends InteractiveTool {

	private boolean _start;

	public AngleLineTool(SceneEditor editor, boolean start) {
		super(editor);

		_start = start;
	}

	@Override
	protected boolean canEdit(ObjectModel model) {
		return model instanceof TransformComponent;
	}

	@Override
	public void render(GC gc) {
		var renderer = getRenderer();

		var centerX = 0;
		var centerY = 0;

		var globalStartAngle = 0f;
		var globalEndAngle = 0f;

		for (var model : getModels()) {

			var objSize = renderer.getObjectSize(model);

			float modelX = 0;
			float modelY = 0;

			if (model instanceof OriginComponent) {
				modelX = objSize[0] * OriginComponent.get_originX(model);
				modelY = objSize[1] * OriginComponent.get_originY(model);
			}

			var globalXY = renderer.localToScene(model, modelX, modelY);

			centerX += globalXY[0];
			centerY += globalXY[1];

			float startAngle;
			float endAngle;

			var parentAngle = renderer.globalAngle(ParentComponent.get_parent(model));

			if (getScene().isTransformLocalCoords()) {
				startAngle = parentAngle;
				endAngle = parentAngle + TransformComponent.get_angle(model);
			} else {
				startAngle = 0;
				endAngle = parentAngle + TransformComponent.get_angle(model);
			}

			globalStartAngle += startAngle;
			globalEndAngle += endAngle;

		}

		var size = getModels().size();

		centerX = centerX / size;
		centerY = centerY / size;
		globalStartAngle = globalStartAngle / size;
		globalEndAngle = globalEndAngle / size;

		gc.setForeground(SWTResourceManager.getColor(ColorUtil.WHITESMOKE.rgb));

		var tx = new Transform(Display.getDefault());

		tx.translate(centerX, centerY);
		tx.rotate(_start ? globalStartAngle : globalEndAngle);

		var p0 = new float[] { 0, 0 };
		var p1 = new float[] { 150, 0 };

		tx.transform(p0);
		tx.transform(p1);
		
		tx.dispose();

		gc.drawLine((int) p0[0], (int) p0[1], (int) p1[0], (int) p1[1]);
		
	}

	@Override
	public boolean contains(int sceneX, int sceneY) {
		return false;
	}

}
