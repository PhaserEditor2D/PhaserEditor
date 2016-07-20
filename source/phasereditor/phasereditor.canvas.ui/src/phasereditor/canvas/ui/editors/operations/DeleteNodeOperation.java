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

import static java.lang.System.currentTimeMillis;
import static java.lang.System.out;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.json.JSONObject;

import phasereditor.canvas.core.BaseObjectModel;
import phasereditor.canvas.core.CanvasModelFactory;
import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.canvas.ui.editors.ObjectCanvas;
import phasereditor.canvas.ui.shapes.BaseObjectControl;
import phasereditor.canvas.ui.shapes.CanvasObjectFactory;
import phasereditor.canvas.ui.shapes.GroupControl;
import phasereditor.canvas.ui.shapes.GroupNode;

/**
 * @author arian
 *
 */
public class DeleteNodeOperation extends AbstractNodeOperation {

	private String _groupId;
	private int _index;
	private JSONObject _data;

	public DeleteNodeOperation(String nodeId) {
		super("DeleteNodeOperation", nodeId);
	}

	@Override
	public IStatus execute(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		BaseObjectControl<?> control = findControl(info);
		long t = currentTimeMillis();
		out.println("deleting " + control.getModel().getEditorName());
		GroupNode group = control.getIObjectNode().getGroup();

		_groupId = group.getControl().getId();
		_index = group.getChildren().indexOf(control.getNode());
		_data = new JSONObject();

		control.getModel().write(_data);

		remove(control);

		out.println("done " + (currentTimeMillis() - t) + "ms");
		
		return Status.OK_STATUS;
	}

	@Override
	public IStatus redo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		BaseObjectControl<?> control = findControl(info);

		remove(control);

		return Status.OK_STATUS;
	}

	@Override
	public IStatus undo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		GroupControl group = (GroupControl) findControl(info, _groupId);
		BaseObjectModel model = CanvasModelFactory.createModel(group.getModel(), _data);
		ObjectCanvas canvas = info.getAdapter(CanvasEditor.class).getCanvas();
		BaseObjectControl<?> control = CanvasObjectFactory.createObjectControl(canvas, model);
		group.addChild(_index, control.getIObjectNode());
		return Status.OK_STATUS;
	}

	private static void remove(BaseObjectControl<?> control) {
		ObjectCanvas canvas = control.getCanvas();

		control.removeme();

		canvas.getSelectionBehavior().removeNodeFromSelection(control.getNode());
	}

}
