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

import java.util.List;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import phasereditor.canvas.core.BaseObjectModel;
import phasereditor.canvas.core.WorldModel.ZOperation;
import phasereditor.canvas.ui.shapes.BaseObjectControl;

/**
 * @author arian
 */
public class ChangeZOperation extends AbstractNodeOperation {

	private ZOperation _zop;
	private int _beforeIndex;
	private int _afterIndex;

	public ChangeZOperation(String controlId, ZOperation zop) {
		super("ChangeZOperation", controlId);
		_zop = zop;
	}

	@Override
	public IStatus execute(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		BaseObjectControl<?> control = findControl(info);
		ObservableList<Node> children = control.getGroup().getChildren();
		_beforeIndex = children.indexOf(control.getNode());
		control.applyZOperation(_zop);
		_afterIndex = control.getGroup().getChildren().indexOf(control.getNode());
		return null;
	}

	@Override
	public IStatus redo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		BaseObjectControl<?> control = findControl(info);
		control.applyZOperation(_zop);
		return Status.OK_STATUS;
	}

	@Override
	public IStatus undo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		BaseObjectControl<?> control = findControl(info);
		ObservableList<Node> nodes = control.getGroup().getChildren();
		nodes.remove(_afterIndex);
		nodes.add(_beforeIndex, control.getNode());

		List<BaseObjectModel> models = control.getGroup().getModel().getChildren();
		models.remove(_afterIndex);
		models.add(_beforeIndex, control.getModel());

		return Status.OK_STATUS;
	}

}
