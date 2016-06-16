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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import javafx.geometry.Point2D;
import javafx.scene.Node;
import phasereditor.canvas.core.BaseObjectModel;
import phasereditor.canvas.core.GroupModel;
import phasereditor.canvas.ui.shapes.BaseObjectControl;
import phasereditor.canvas.ui.shapes.GroupNode;
import phasereditor.canvas.ui.shapes.IObjectNode;

/**
 * @author arian
 *
 */
public class TrimNodeOperation extends AbstractNodeOperation {

	private Map<String, Point2D> _mapBefore;
	private Map<String, Point2D> _mapAfter;

	public TrimNodeOperation(String controlId) {
		super("TrimNodeOperation", controlId);
	}

	@Override
	public IStatus execute(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		_mapBefore = new LinkedHashMap<>();
		_mapAfter = new LinkedHashMap<>();

		// remove the empty space from the left and top.
		GroupNode group = (GroupNode) findControl(info).getNode();
		double minx = Double.MAX_VALUE;
		double miny = Double.MAX_VALUE;
		for (Node node : group.getChildren()) {
			BaseObjectModel model = ((IObjectNode) node).getModel();
			minx = Math.min(model.getX(), minx);
			miny = Math.min(model.getY(), miny);
		}

		GroupModel groupModel = group.getModel();
		_mapBefore.put(_nodeId, new Point2D(groupModel.getX(), groupModel.getY()));
		groupModel.setX(groupModel.getX() + minx);
		groupModel.setY(groupModel.getY() + miny);
		_mapAfter.put(_nodeId, new Point2D(groupModel.getX(), groupModel.getY()));

		for (Node node : group.getChildren()) {
			IObjectNode sprite = (IObjectNode) node;
			BaseObjectModel model = sprite.getModel();
			_mapBefore.put(model.getId(), new Point2D(model.getX(), model.getY()));
			model.setX(model.getX() - minx);
			model.setY(model.getY() - miny);
			_mapAfter.put(model.getId(), new Point2D(model.getX(), model.getY()));
			sprite.getControl().updateFromModel();
		}

		group.getControl().updateFromModel();
		getCanvas(info).getSelectionBehavior().updateSelectedNodes();

		return Status.OK_STATUS;
	}

	@Override
	public IStatus redo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		restorePositions(info, _mapAfter);
		return Status.OK_STATUS;
	}

	@Override
	public IStatus undo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		restorePositions(info, _mapBefore);
		return Status.OK_STATUS;
	}

	private void restorePositions(IAdaptable info, Map<String, Point2D> map) {
		for (Entry<String, Point2D> entry : map.entrySet()) {
			String id = entry.getKey();
			Point2D point = entry.getValue();
			BaseObjectControl<?> control = findControl(info, id);
			control.getModel().setX(point.getX());
			BaseObjectModel model = control.getModel();
			model.setY(point.getY());
			control.updateFromModel();
		}

		BaseObjectControl<?> control = findControl(info);
		control.updateFromModel();
		getCanvas(info).getSelectionBehavior().updateSelectedNodes();
	}

}
