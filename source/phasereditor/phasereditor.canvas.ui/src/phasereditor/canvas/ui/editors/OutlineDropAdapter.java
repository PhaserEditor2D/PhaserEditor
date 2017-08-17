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
package phasereditor.canvas.ui.editors;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TransferData;
import org.json.JSONObject;

import phasereditor.canvas.core.CanvasType;
import phasereditor.canvas.ui.editors.operations.AddNodeOperation;
import phasereditor.canvas.ui.editors.operations.CompositeOperation;
import phasereditor.canvas.ui.editors.operations.DeleteNodeOperation;
import phasereditor.canvas.ui.editors.operations.ExpandOutlineOperation;
import phasereditor.canvas.ui.editors.operations.SelectOperation;
import phasereditor.canvas.ui.shapes.GroupNode;
import phasereditor.canvas.ui.shapes.IObjectNode;

/**
 * @author arian
 *
 */
public class OutlineDropAdapter extends ViewerDropAdapter {
	private int _location;
	private Object _target;
	private CanvasEditor _editor;

	public OutlineDropAdapter(CanvasEditor editor) {
		super(editor.getOutline());
		_editor = editor;
	}

	@Override
	public boolean validateDrop(Object target, int operation, TransferData transferType) {

		if (target instanceof IObjectNode) {
			IObjectNode node = (IObjectNode) target;

			// do not allow to drop in the world node if this is a group prefab
			if (node.getControl().getCanvas().getEditor().getModel().getType() == CanvasType.GROUP) {
				if (node.getModel() == node.getModel().getWorld().findGroupPrefabRoot()) {
					switch (_location) {
					case LOCATION_BEFORE:
						return false;
					case LOCATION_AFTER:
						return false;
					case LOCATION_ON:
					default:
						return true;
					}
				}
			}
		}
		return true;
	}

	@Override
	public void dragOver(DropTargetEvent event) {
		_location = determineLocation(event);
		_target = determineTarget(event);
		super.dragOver(event);
	}

	@SuppressWarnings("unlikely-arg-type")
	@Override
	public boolean performDrop(Object data) {
		if (_target == null) {
			return false;
		}

		IObjectNode target = (IObjectNode) _target;

		List<IObjectNode> nodes = new ArrayList<>();

		for (Object obj : ((IStructuredSelection) data).toArray()) {

			if (obj == target) {
				return false;
			}

			if (target.getAncestors().contains(obj)) {
				return false;
			}

			if (obj instanceof IObjectNode) {
				nodes.add((IObjectNode) obj);
			}
		}

		Set<IObjectNode> set = new HashSet<>(nodes);

		for (IObjectNode node : set) {
			for (IObjectNode ancestor : node.getAncestors()) {
				if (set.contains(ancestor)) {
					nodes.remove(node);
				}
			}
		}

		CompositeOperation operations = new CompositeOperation();

		List<String> selection = new ArrayList<>();
		List<String> expandList = new ArrayList<>();

		for (IObjectNode node : nodes) {
			GroupNode group = target.getGroup();

			if (_location != LOCATION_NONE) {
				// node.getControl().removeme();
				operations.add(new DeleteNodeOperation(node.getControl().getId()));
			}

			// If the target is on the same group of the node, then the index
			// could be wrong because the node will be deleted.
			// To fix this we are going to simulate a deletion and compute the
			// index
			int i;
			if (target.getGroup() == node.getGroup()) {
				List<?> list = new ArrayList<>(node.getGroup().getChildren());
				list.remove(node);
				i = list.indexOf(target);
			} else {
				i = group.getChildren().indexOf(target);
			}
			if (i < 0) {
				i = group.getChildren().size();
			}

			JSONObject jsonData = new JSONObject();
			node.getModel().write(jsonData, true);

			selection.add(node.getModel().getId());

			double x = node.getModel().getX();
			double y = node.getModel().getY();

			String groupId = group.getControl().getId();
			switch (_location) {
			case LOCATION_BEFORE:
				operations.add(new AddNodeOperation(jsonData, i + 1, x, y, groupId, false));
				expandList.add(groupId);
				break;
			case LOCATION_AFTER:
				operations.add(new AddNodeOperation(jsonData, i, x, y, groupId, false));
				expandList.add(groupId);
				break;
			case LOCATION_ON:
				if (target instanceof GroupNode) {
					GroupNode asGroup = (GroupNode) target;
					// add to the end (-1 because the node will be removed)
					int i2 = asGroup.getChildren().size() - 1;
					String targetId = asGroup.getControl().getId();
					operations.add(new AddNodeOperation(jsonData, i2, x, y, targetId, false));
					expandList.add(targetId);
				} else {
					operations.add(new AddNodeOperation(jsonData, i, x, y, groupId, false));
				}
				break;
			default:
				break;
			}
		}

		operations.add(new ExpandOutlineOperation(expandList));
		operations.add(new SelectOperation(selection));

		_editor.getCanvas().getUpdateBehavior().executeOperations(operations);

		return true;
	}
}