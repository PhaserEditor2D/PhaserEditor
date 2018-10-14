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

import static java.util.stream.Collectors.toList;

import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.events.MouseEvent;
import org.json.JSONObject;

import phasereditor.scene.core.ObjectModel;
import phasereditor.scene.core.ParentComponent;
import phasereditor.scene.core.TransformComponent;
import phasereditor.scene.ui.editor.undo.WorldSnapshotOperation;

/**
 * @author arian
 *
 */
@SuppressWarnings("boxing")
public class DragObjectsEvents {

	private static final String START_DRAG_Y = "-start-drag-y";
	private static final String START_DRAG_X = "-start-drag-x";
	private SceneCanvas _scene;
	private int _startX;
	private int _startY;
	private List<ObjectModel> _objects;
	private JSONObject _beforeData;

	public DragObjectsEvents(SceneCanvas scene) {
		_scene = scene;
	}

	public void done() {

		_scene.getEditor().updatePropertyPagesContentWithSelection();

		_objects = null;

		var afterData = WorldSnapshotOperation.takeSnapshot(_scene.getEditor());

		_scene.getEditor().executeOperation(new WorldSnapshotOperation(_beforeData, afterData, "Move objects"));
	}

	public void update(MouseEvent e) {

		_scene.getEditor().setDirty(true);

		var cursorX = e.x;
		var cursorY = e.y;

		var deltaSceneX = cursorX - _startX;
		var deltaSceneY = cursorY - _startY;

		var renderer = _scene.getSceneRenderer();

		for (var model : _objects) {
			if (model instanceof TransformComponent) {
				var parent = ParentComponent.get_parent(model);

				var localX = (float) model.get(START_DRAG_X);
				var localY = (float) model.get(START_DRAG_Y);

				var scenePoint = renderer.localToScene(parent, localX, localY);

				var sceneX = scenePoint[0] + deltaSceneX;
				var sceneY = scenePoint[1] + deltaSceneY;

				var localPoint = renderer.sceneToLocal(parent, sceneX, sceneY);

				TransformComponent.set_x(model, localPoint[0]);
				TransformComponent.set_y(model, localPoint[1]);
			}
		}

		_scene.redraw();
	}

	public boolean isDragging() {
		return _objects != null;
	}

	public void start(MouseEvent e) {

		_beforeData = WorldSnapshotOperation.takeSnapshot(_scene.getEditor());

		_startX = e.x;
		_startY = e.y;

		var selection = (IStructuredSelection) _scene.getEditor().getEditorSite().getSelectionProvider().getSelection();

		_objects = SceneCanvas.filterChidlren(

				Arrays.stream(selection.toArray()).map(obj -> (ObjectModel) obj).collect(toList())

		);

		for (var model : _objects) {
			if (model instanceof TransformComponent) {
				var localX = TransformComponent.get_x(model);
				var localY = TransformComponent.get_y(model);

				model.put(START_DRAG_X, localX);
				model.put(START_DRAG_Y, localY);

			}
		}
	}

}
