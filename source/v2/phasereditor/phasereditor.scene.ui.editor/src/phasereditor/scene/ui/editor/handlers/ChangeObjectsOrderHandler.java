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
package phasereditor.scene.ui.editor.handlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.scene.core.ObjectModel;
import phasereditor.scene.core.ParentComponent;
import phasereditor.scene.ui.editor.SceneEditor;
import phasereditor.scene.ui.editor.messages.ResetSceneMessage;
import phasereditor.scene.ui.editor.messages.SelectObjectsMessage;
import phasereditor.scene.ui.editor.undo.WorldSnapshotOperation;

/**
 * @author arian
 *
 */
public class ChangeObjectsOrderHandler extends AbstractHandler {

	@SuppressWarnings("boxing")
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		var cmd = event.getCommand().getId();

		var nameindex = cmd.indexOf("_");

		final var ordername = cmd.substring(nameindex + 1);

		var editor = (SceneEditor) HandlerUtil.getActiveEditor(event);

		var models = editor.getSelectionList();

		// first, check all models are from the same parent

		ObjectModel parent = null;

		for (var model : models) {
			var parent2 = ParentComponent.get_parent(model);
			if (parent == null) {
				parent = parent2;
			} else {
				if (parent2 != parent) {

					MessageDialog.openInformation(editor.getEditorSite().getShell(), "Order Action",
							"Cannot change the order of objects with different parents.");
					return null;
				}
			}
		}

		var children = ParentComponent.get_children(parent);

		var canMove = true;

		// compute if all the objects can be moved

		for (var model : models) {

			var size = children.size();
			var index = children.indexOf(model);

			switch (ordername) {
			case "UP":
			case "TOP":
				if (index + 1 == size) {
					canMove = false;
				}
				break;
			case "DOWN":
			case "BOTTOM":
				if (index - 1 < 0) {
					canMove = false;
				}
				break;
			default:
				break;
			}
		}

		// just move the objects if all the objects can be moved

		if (!canMove) {
			return null;
		}

		var modelIndexMap = new HashMap<ObjectModel, Integer>();
		var modelSet = new HashSet<>(models);

		var top = children.size() - 1;
		var bottom = 0;

		for (int i = 0; i < children.size(); i++) {

			var model = children.get(ordername.equals("TOP") ? children.size() - i - 1 : i);

			if (!modelSet.contains(model)) {
				continue;
			}

			var index = children.indexOf(model);
			var next = index + 1;
			var prev = index - 1;

			switch (ordername) {
			case "UP":
				modelIndexMap.put(model, next);
				break;
			case "DOWN":
				modelIndexMap.put(model, prev);
				break;
			case "TOP":
				modelIndexMap.put(model, top);
				top--;
				break;
			case "BOTTOM":
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

		var beforeData = WorldSnapshotOperation.takeSnapshot(editor);

		ParentComponent.set_children(parent, newChildren);

		var afterData = WorldSnapshotOperation.takeSnapshot(editor);

		var operation = new WorldSnapshotOperation(beforeData, afterData, "Change objects order.");

		editor.executeOperation(operation);

		editor.setDirty(true);
		editor.refreshOutline();

		editor.getBroker().sendAllBatch(

				new ResetSceneMessage(editor),

				new SelectObjectsMessage(editor)

		);

		return null;
	}

}
