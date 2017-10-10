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
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.json.JSONObject;

import phasereditor.canvas.core.BaseObjectModel;
import phasereditor.canvas.core.CanvasModelFactory;
import phasereditor.canvas.core.GroupModel;
import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.canvas.ui.editors.ObjectCanvas;
import phasereditor.canvas.ui.shapes.BaseObjectControl;
import phasereditor.canvas.ui.shapes.CanvasObjectFactory;
import phasereditor.canvas.ui.shapes.GroupControl;

/**
 * @author arian
 *
 */
public class AddNodeOperation extends AbstractNodeOperation {

	private JSONObject _data;
	private double _x;
	private double _y;
	private String _parentId;
	private int _index;
	private boolean _createUniqueName;

	public AddNodeOperation(JSONObject data, int index, double x, double y, String parentId) {
		this(data, index, x, y, parentId, true);
	}
	
	public AddNodeOperation(JSONObject data, int index, double x, double y, String parentId, boolean createUniqueName) {
		super("CreateNodeOperation", null);
		_data = data;
		_index = index;

		// _x = x;
		// _y = y;

		// TODO: round position to integer
		_x = Math.round(x);
		_y = Math.round(y);

		_parentId = parentId;

		_createUniqueName = createUniqueName;
	}

	@Override
	public IStatus execute(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		return addNode(info);
	}

	@Override
	public IStatus redo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		return addNode(info);
	}

	@Override
	public IStatus undo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		BaseObjectControl<?> control = findControl(info, _controlId);
		ObjectCanvas canvas = control.getCanvas();

		control.removeme();

		canvas.getSelectionBehavior().removeNodeFromSelection(control.getNode());

		return Status.OK_STATUS;
	}

	private IStatus addNode(IAdaptable info) {
		ObjectCanvas canvas = info.getAdapter(CanvasEditor.class).getCanvas();
		GroupControl groupControl = (GroupControl) findControl(info, _parentId);
		BaseObjectModel model = CanvasModelFactory.createModel(groupControl.getModel(), _data);
		_controlId = model.getId();

		if (_createUniqueName) {
			changeName(canvas, model);
		}

		model.setX(_x);
		model.setY(_y);
		BaseObjectControl<?> control = CanvasObjectFactory.createObjectControl(canvas, model);
		groupControl.addChild(_index, control.getIObjectNode());

		return Status.OK_STATUS;
	}

	private static void changeName(ObjectCanvas canvas, BaseObjectModel model) {
		model.setEditorName(canvas.getWorldModel().createName(model.getEditorName()));

		if (model instanceof GroupModel) {
			GroupModel group = (GroupModel) model;
			for (BaseObjectModel child : group.getChildren()) {
				changeName(canvas, child);
			}
		}
	}

}
