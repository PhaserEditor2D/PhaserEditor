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
package phasereditor.canvas.ui.handlers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.json.JSONObject;

import javafx.scene.Node;
import phasereditor.canvas.core.BaseObjectModel;
import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.canvas.ui.editors.operations.AddNodeOperation;
import phasereditor.canvas.ui.editors.operations.CompositeOperation;
import phasereditor.canvas.ui.editors.operations.DeleteNodeOperation;
import phasereditor.canvas.ui.editors.operations.SelectOperation;
import phasereditor.canvas.ui.shapes.GroupNode;
import phasereditor.canvas.ui.shapes.IObjectNode;

/**
 * @author arian
 *
 */
public class BreakGroupHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IStructuredSelection sel = (IStructuredSelection) HandlerUtil.getCurrentSelection(event);
		// List<Object> newselection = new ArrayList<>();

		CompositeOperation operations = new CompositeOperation();

		List<String> parentSelection = new ArrayList<>();

		// select the group

		for (Object obj : sel.toArray()) {
			GroupNode group = (GroupNode) obj;
			if (group.getModel().isPrefabInstance()) {
				continue;
			}
			parentSelection.add(group.getModel().getId());
		}

		if (parentSelection.isEmpty()) {
			// all the selected groups are prefabs so its better to stop
			// here
			return null;
		}

		operations.add(new SelectOperation(parentSelection));

		List<String> childrenSelection = new ArrayList<>();

		// un-group

		for (Object obj : sel.toArray()) {
			GroupNode group = (GroupNode) obj;

			// delete the group
			operations.add(new DeleteNodeOperation(group.getModel().getId()));

			GroupNode parent = group.getGroup();
			String parentId = parent.getModel().getId();

			parentSelection.add(parentId);

			double groupx = group.getModel().getX();
			double groupy = group.getModel().getY();

			int i = parent.getChildren().size() - 1;

			for (Node node : group.getChildren()) {
				// add node to new parent
				IObjectNode inode = (IObjectNode) node;
				JSONObject data = new JSONObject();
				BaseObjectModel model = inode.getModel();
				model.write(data, true);
				double x = groupx + model.getX();
				double y = groupy + model.getY();
				operations.add(new AddNodeOperation(data, i, x, y, parentId));
				childrenSelection.add(inode.getModel().getId());
				i++;
			}
		}
		operations.add(new SelectOperation(childrenSelection));
		CanvasEditor editor = (CanvasEditor) HandlerUtil.getActiveEditor(event);
		editor.getCanvas().getUpdateBehavior().executeOperations(operations);

		return null;
	}
}
