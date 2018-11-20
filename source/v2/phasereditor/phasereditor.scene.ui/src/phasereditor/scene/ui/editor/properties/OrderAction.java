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
package phasereditor.scene.ui.editor.properties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;

import phasereditor.scene.core.ObjectModel;
import phasereditor.scene.core.ParentComponent;
import phasereditor.scene.ui.editor.SceneEditor;
import phasereditor.scene.ui.editor.undo.SingleObjectSnapshotOperation;

public class OrderAction {
	private OrderActionValue _order;
	private SceneEditor _editor;

	public enum OrderActionValue {
		UP, DOWN, TOP, BOTTOM
	}

	public OrderAction(SceneEditor editor, OrderActionValue order) {
		_order = order;
		this._editor = editor;
	}

	@SuppressWarnings({ "incomplete-switch", "boxing" })
	public void run() {
		var models = _editor.getSelectionList();
		
		// first, check all models are from the same parent

		ObjectModel parent = null;

		for (var model : models) {
			var parent2 = ParentComponent.get_parent(model);
			if (parent == null) {
				parent = parent2;
			} else {
				if (parent2 != parent) {

					MessageDialog.openInformation(_editor.getEditorSite().getShell(), "Order Action",
							"Cannot change the order of objects with different parents.");
					return;
				}
			}
		}

		var children = ParentComponent.get_children(parent);

		var canMove = true;

		// compute if all the objects can be moved

		for (var model : models) {

			var size = children.size();
			var index = children.indexOf(model);

			switch (_order) {
			case UP:
			case TOP:
				if (index + 1 == size) {
					canMove = false;
				}
				break;
			case DOWN:
			case BOTTOM:
				if (index - 1 < 0) {
					canMove = false;
				}
				break;
			}
		}

		// just move the objects if all the objects can be moved

		if (!canMove) {
			return;
		}

		var modelIndexMap = new HashMap<ObjectModel, Integer>();
		var modelSet = new HashSet<>(models);

		var top = children.size() - 1;
		var bottom = 0;

		for (int i = 0; i < children.size(); i++) {

			var model = children.get(_order == OrderActionValue.TOP ? children.size() - i - 1 : i);

			if (!modelSet.contains(model)) {
				continue;
			}

			var index = children.indexOf(model);
			var next = index + 1;
			var prev = index - 1;

			switch (_order) {
			case UP:
				modelIndexMap.put(model, next);
				break;
			case DOWN:
				modelIndexMap.put(model, prev);
				break;
			case TOP:
				modelIndexMap.put(model, top);
				top--;
				break;
			case BOTTOM:
				modelIndexMap.put(model, bottom);
				bottom++;
				break;
			default:
				break;
			}
		}

		var newChildren = new ArrayList<ObjectModel>();

		for (var i = 0; i < children.size(); i++) {
			newChildren.add(null);
		}

		for (var model : models) {
			var index = modelIndexMap.get(model);
			newChildren.set(index, model);
		}

		var newIndex = 0;

		for (var model : children) {

			if (modelSet.contains(model)) {
				continue;
			}

			while (newChildren.get(newIndex) != null) {
				newIndex++;
			}
			newChildren.set(newIndex, model);
		}

		var parentList = List.of(parent);

		var beforeData = SingleObjectSnapshotOperation.takeSnapshot(parentList);

		ParentComponent.set_children(parent, newChildren);

		var afterData = SingleObjectSnapshotOperation.takeSnapshot(parentList);

		var operation = new SingleObjectSnapshotOperation(beforeData, afterData, "Change objects order.");

		_editor.executeOperation(operation);

		_editor.setDirty(true);
		_editor.getScene().redraw();
		_editor.refreshOutline();

	}
}