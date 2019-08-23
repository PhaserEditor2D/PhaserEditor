// The MIT License (MIT)
//
// Copyright (c) 2015, 2019 Arian Fornaris
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
package phasereditor.scene.ui.editor;

import static phasereditor.ui.PhaserEditorUI.swtRun;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Event;

import phasereditor.scene.core.GroupModel;
import phasereditor.scene.core.NameComputer;
import phasereditor.scene.core.ParentComponent;
import phasereditor.scene.core.VariableComponent;
import phasereditor.scene.ui.editor.undo.GroupListSnapshotOperation;

/**
 * @author arian
 *
 */
public class AddGroupAction extends Action {

	private SceneEditor _editor;

	public AddGroupAction(SceneEditor editor) {
		super("Group");
		_editor = editor;
	}

	@Override
	public void runWithEvent(Event event) {
		var sceneModel = _editor.getSceneModel();

		var groups = sceneModel.getGroupsModel();

		var nameComputer = new NameComputer(groups);
		var initialName = nameComputer.newName("group");

		var dlg = new InputDialog(_editor.getSite().getShell(), "Create Group", "Enter the name of the new Group:",
				initialName, new IInputValidator() {

					@Override
					public String isValid(String newText) {

						for (var group : ParentComponent.get_children(groups)) {
							if (VariableComponent.get_variableName(group).equals(newText)) {
								return "That name is used.";
							}
						}

						return null;
					}
				});

		if (dlg.open() == Window.OK) {
			var value = dlg.getValue();

			var group = new GroupModel(groups);

			VariableComponent.set_variableName(group, value);

			var before = GroupListSnapshotOperation.takeSnapshot(_editor);

			ParentComponent.get_children(groups).add(group);

			addObjectsToNewGroup(group);

			var after = GroupListSnapshotOperation.takeSnapshot(_editor);

			_editor.executeOperation(new GroupListSnapshotOperation(before, after, "Add group."));

			// TODO: just for now, we should fix the bug of clicking on a TreeCanvas action.
			swtRun(() -> {
				_editor.setSelection(List.of(group));

				_editor.updatePropertyPagesContentWithSelection();
			});

			_editor.refreshOutline();

			_editor.setDirty(true);
		}
	}

	@SuppressWarnings("unused")
	protected void addObjectsToNewGroup(GroupModel group) {
		// to be implemented by derived types.
	}

}
