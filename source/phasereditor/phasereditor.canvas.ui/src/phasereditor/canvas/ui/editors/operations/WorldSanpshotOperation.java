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

import phasereditor.canvas.core.WorldModel;
import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.canvas.ui.editors.ObjectCanvas;

/**
 * @author arian
 *
 */
@Deprecated
public class WorldSanpshotOperation extends AbstractOperation {

	private JSONObject _beforeState;
	private JSONObject _afterState;
	private Runnable _action;

	public WorldSanpshotOperation(String label, Runnable action) {
		super(label);
		_action = action;
		addContext(CanvasEditor.UNDO_CONTEXT);
	}

	@Override
	public IStatus execute(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		CanvasEditor editor = info.getAdapter(CanvasEditor.class);
		WorldModel model = editor.getCanvas().getWorldModel();

		_beforeState = new JSONObject();
		_afterState = new JSONObject();

		model.write(_beforeState);

		_action.run();

		model.write(_afterState);

		return Status.OK_STATUS;
	}

	@Override
	public IStatus redo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		return loadState(info, _afterState);
	}

	@Override
	public IStatus undo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		return loadState(info, _beforeState);
	}

	private static IStatus loadState(IAdaptable info, JSONObject state) {
		CanvasEditor editor = info.getAdapter(CanvasEditor.class);
		ObjectCanvas canvas = editor.getCanvas();
		WorldModel model = canvas.getWorldModel();
		

		model.read(state);

		canvas.getWorldNode().getControl().updateStructureFromModel();
		canvas.getWorldModel().firePropertyChange(WorldModel.PROP_STRUCTURE);

		return Status.OK_STATUS;
	}

}
