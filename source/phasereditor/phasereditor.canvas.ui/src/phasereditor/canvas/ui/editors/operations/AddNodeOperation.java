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

import java.util.UUID;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.json.JSONObject;

import phasereditor.canvas.core.BaseObjectModel;
import phasereditor.canvas.core.CanvasModelFactory;
import phasereditor.canvas.core.WorldModel;
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

	public AddNodeOperation(JSONObject data, int index, double x, double y, String parentId) {
		super("CreateNodeOperation", UUID.randomUUID().toString());
		_data = data;
		_index = index;
		_x = x;
		_y = y;
		_parentId = parentId;
	}

	@Override
	public IStatus execute(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		ObjectCanvas canvas = info.getAdapter(CanvasEditor.class).getCanvas();
		GroupControl groupControl = (GroupControl) findControl(info, _parentId);
		BaseObjectModel model = CanvasModelFactory.createModel(groupControl.getModel(), _data);
		BaseObjectControl<?> control = CanvasObjectFactory.createObjectControl(canvas, model);
		groupControl.addChild(_index, control.getIObjectNode());
		canvas.getWorldModel().firePropertyChange(WorldModel.PROP_STRUCTURE);
		canvas.getSelectionBehavior().updateSelectedNodes();
		return null;
	}

	@Override
	public IStatus redo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		return null;
	}

	@Override
	public IStatus undo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		return null;
	}

}
