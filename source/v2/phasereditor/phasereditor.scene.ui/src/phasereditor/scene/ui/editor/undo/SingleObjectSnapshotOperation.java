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
package phasereditor.scene.ui.editor.undo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.json.JSONObject;

import phasereditor.scene.core.ObjectModel;
import phasereditor.scene.ui.editor.SceneEditor;

/**
 * @author arian
 *
 */
public class SingleObjectSnapshotOperation extends AbstractOperation {

	public static List<JSONObject> takeSnapshot(List<ObjectModel> models) {
		var list = new ArrayList<JSONObject>();

		for (var model : models) {
			var data = new JSONObject();
			model.write(data);
			list.add(data);
		}

		return list;
	}

	private List<JSONObject> _beforeData;
	private List<JSONObject> _afterData;
	private boolean _dirtyModels;
	private Function<ObjectModel, Boolean> _filterDirtyModels;

	public SingleObjectSnapshotOperation(List<JSONObject> beforeData, List<JSONObject> afterData, String label,
			boolean dirtyModels) {
		this(beforeData, afterData, label, dirtyModels, null);
	}

	public SingleObjectSnapshotOperation(List<JSONObject> beforeData, List<JSONObject> afterData, String label,
			boolean dirtyModels, Function<ObjectModel, Boolean> filterDirtyModels) {
		super(label);

		_beforeData = beforeData;
		_afterData = afterData;

		_dirtyModels = dirtyModels;
		_filterDirtyModels = filterDirtyModels;
	}

	public SingleObjectSnapshotOperation(List<JSONObject> beforeData, List<JSONObject> afterData, String label) {
		this(beforeData, afterData, label, false);
	}

	@Override
	public IStatus execute(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		// nothing to do, the change was made
		return Status.OK_STATUS;
	}

	@Override
	public IStatus redo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {

		loadSnapshot(info, _afterData);

		return Status.OK_STATUS;
	}

	@Override
	public IStatus undo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {

		loadSnapshot(info, _beforeData);

		return Status.OK_STATUS;
	}

	private void loadSnapshot(IAdaptable info, List<JSONObject> list) {
		var editor = info.getAdapter(SceneEditor.class);
		var sceneModel = editor.getSceneModel();
		var project = editor.getEditorInput().getFile().getProject();
		var canvas = editor.getScene();

		for (var data : list) {
			var id = data.getString("-id");
			var model = sceneModel.getRootObject().findById(id);

			if (model != null) {
				model.read(data, project);

				if (_dirtyModels) {
					if (_filterDirtyModels == null || _filterDirtyModels.apply(model).booleanValue()) {
						model.setDirty(true);
					}

				}
			}
		}

		editor.refreshSelectionBaseOnId();

		canvas.redraw();

		editor.setDirty(true);
	}

}
