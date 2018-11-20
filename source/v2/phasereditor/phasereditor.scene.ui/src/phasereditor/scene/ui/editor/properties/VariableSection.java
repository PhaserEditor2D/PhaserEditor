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

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolItem;

import phasereditor.scene.core.GameObjectEditorComponent;
import phasereditor.scene.core.GroupComponent;
import phasereditor.scene.core.GroupModel;
import phasereditor.scene.core.ObjectModel;
import phasereditor.scene.core.ParentComponent;
import phasereditor.scene.core.VariableComponent;
import phasereditor.scene.ui.SceneUI;
import phasereditor.scene.ui.editor.SceneEditor;
import phasereditor.scene.ui.editor.undo.GroupListSnapshotOperation;
import phasereditor.scene.ui.editor.undo.SingleObjectSnapshotOperation;
import phasereditor.ui.EditorSharedImages;

/**
 * @author arian
 *
 */
public class VariableSection extends ScenePropertySection {

	private Text _editorNameText;
	private Action _fieldAction;

	public VariableSection(ScenePropertyPage page) {
		super("Variable", page);
	}

	@Override
	public boolean canEdit(Object obj) {
		return obj instanceof GameObjectEditorComponent;
	}

	@Override
	public Control createContent(Composite parent) {

		createActions();

		Composite comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new GridLayout(2, false));

		{
			label(comp, "Var Name", "*(Editor) The name of the variable used in the generated code.");

			var row = new Composite(comp, 0);
			row.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			var gl = new GridLayout(2, false);
			gl.marginWidth = gl.marginHeight = 0;
			row.setLayout(gl);

			_editorNameText = new Text(row, SWT.BORDER);
			_editorNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			var toolbar = new ToolBarManager();

			toolbar.add(_fieldAction);

			toolbar.createControl(row);
		}

		return comp;
	}

	@Override
	public void fillToolbar(ToolBarManager manager) {
		manager.add(_fieldAction);
	}

	abstract class GroupMenuAction extends Action {

		public GroupMenuAction(String text, String icon) {
			super(text);

			setImageDescriptor(EditorSharedImages.getImageDescriptor(icon));
		}

		@SuppressWarnings({ "cast", "rawtypes", "unchecked" })
		@Override
		public void runWithEvent(Event event) {

			var manager = new MenuManager();

			var editor = getEditor();

			var groups = ParentComponent.get_children(editor.getSceneModel().getGroupsModel());

			fillMenu(manager, editor, (List<GroupModel>) (List) groups);

			var menu = manager.createContextMenu(((ToolItem) event.widget).getParent());
			menu.setVisible(true);

		}

		protected abstract void fillMenu(MenuManager manager, SceneEditor editor, List<GroupModel> groups);
	}

	abstract class GroupAction extends Action {

		private GroupModel _group;

		public GroupAction(GroupModel group, String icon) {

			super(GroupComponent.get_name(group), EditorSharedImages.getImageDescriptor(icon));

			_group = group;
		}

		@Override
		public void runWithEvent(Event event) {
			var editor = getEditor();

			var before = GroupListSnapshotOperation.takeSnapshot(editor);

			var groupChildren = ParentComponent.get_children(_group);

			performGroupOperation(groupChildren);

			var after = GroupListSnapshotOperation.takeSnapshot(editor);

			editor.executeOperation(new GroupListSnapshotOperation(before, after, getText()));

			editor.setDirty(true);
			editor.getScene().redraw();
			editor.refreshOutline();

			editor.updatePropertyPagesContentWithSelection();

		}

		protected abstract void performGroupOperation(List<ObjectModel> groupChildren);
	}

	class AddToGroupMenuAction extends GroupMenuAction {

		public AddToGroupMenuAction() {
			super("Add selected objects to a group.", IMG_ADD_TO_GROUP);
		}

		@Override
		protected void fillMenu(MenuManager manager, SceneEditor editor, List<GroupModel> groups) {

			groups.stream().filter(group -> {

				// do not include groups that contains one of the selected models

				var children = ParentComponent.get_children(group);
				for (var model : getModels()) {
					if (children.contains(model)) {
						return false;
					}
				}
				return true;
			}).forEach(group -> {
				manager.add(new GroupAction(group, IMG_ADD) {

					@Override
					protected void performGroupOperation(List<ObjectModel> groupChildren) {
						groupChildren.addAll(getModels());
					}

				});
			});

		}
	}

	class RemoveFromGroupMenuAction extends GroupMenuAction {

		public RemoveFromGroupMenuAction() {
			super("Remove selcted objects from a group.", IMG_REMOVE_FROM_GROUP);
		}

		@Override
		protected void fillMenu(MenuManager manager, SceneEditor editor, List<GroupModel> groups) {
			groups.stream().filter(group -> {

				// just accepts the groups that contains all the selected objects.

				return ParentComponent.get_children(group).containsAll(getModels());
			}).forEach(group -> {
				manager.add(new GroupAction(group, IMG_DELETE) {

					@Override
					protected void performGroupOperation(List<ObjectModel> groupChildren) {
						groupChildren.removeAll(getModels());
					}

				});
			});
		}

	}

	private void createActions() {
		_fieldAction = new Action("Assign to a property.", IAction.AS_CHECK_BOX) {
			{
				setImageDescriptor(EditorSharedImages.getImageDescriptor(IMG_PROPERTY));
			}

			@Override
			public void run() {
				update_editorField();
			}
		};
	}

	protected void update_editorField() {
		getModels().forEach(model -> {
			SceneEditor editor = getEditor();

			var before = SingleObjectSnapshotOperation.takeSnapshot(getModels());

			VariableComponent.set_editorField(model, _fieldAction.isChecked());

			editor.setDirty(true);

			var after = SingleObjectSnapshotOperation.takeSnapshot(getModels());

			editor.executeOperation(new SingleObjectSnapshotOperation(before, after, "Set variables field flag."));
		});
	}

	class MorphAction extends Action {
		private String _toType;

		public MorphAction(String toType) {
			super("Morph To " + toType);
			_toType = toType;
		}

		@Override
		public void run() {
			SceneUI.action_MorphObjectsToNewType(getEditor(), getModels(), _toType);
		}

	}

	@SuppressWarnings("boxing")
	@Override
	public void update_UI_from_Model() {
		var models = getModels();

		_editorNameText.setText(
				flatValues_to_String(models.stream().map(model -> VariableComponent.get_gameObjectEditorName(model))));

		_fieldAction.setChecked(flatValues_to_boolean(
				models.stream().map(model -> VariableComponent.get_gameObjectEditorField(model))));

		listen(_editorNameText, value -> {
			models.stream().forEach(model -> VariableComponent.set_gameObjectEditorName(model, value));

			getEditor().setDirty(true);
			getEditor().refreshOutline();

		}, models);
	}

}
