// The MIT License (MIT)
//
// Copyright (c) 2015, 2016 Arian Fornaris
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
package phasereditor.canvas.ui.editors.operations;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.json.JSONObject;

import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.canvas.ui.editors.ObjectCanvas;
import phasereditor.canvas.ui.editors.SceneSettings;
import phasereditor.canvas.ui.editors.behaviors.UpdateBehavior;

/**
 * @author arian
 *
 */
public class ChangeSettingsOperation extends AbstractOperation {

	private JSONObject _settings;
	private JSONObject _beforeSettings;

	public ChangeSettingsOperation(JSONObject settings) {
		super("ChangeSettingsOperation");
		addContext(CanvasEditor.UNDO_CONTEXT);
		_settings = settings;
	}

	@Override
	public IStatus execute(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		_beforeSettings = new JSONObject();
		CanvasEditor editor = info.getAdapter(CanvasEditor.class);
		ObjectCanvas canvas = editor.getCanvas();
		SceneSettings model = canvas.getSettingsModel();
		model.write(_beforeSettings);
		model.read(_settings);
		UpdateBehavior update = canvas.getUpdateBehavior();
		update.updateFromSettings();
		return Status.OK_STATUS;
	}

	@Override
	public IStatus redo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		CanvasEditor editor = info.getAdapter(CanvasEditor.class);
		SceneSettings model = editor.getCanvas().getSettingsModel();
		model.read(_settings);
		editor.getCanvas().getUpdateBehavior().updateFromSettings();
		return Status.OK_STATUS;
	}

	@Override
	public IStatus undo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		CanvasEditor editor = info.getAdapter(CanvasEditor.class);
		SceneSettings model = editor.getCanvas().getSettingsModel();
		model.read(_beforeSettings);
		editor.getCanvas().getUpdateBehavior().updateFromSettings();
		return Status.OK_STATUS;
	}

}
