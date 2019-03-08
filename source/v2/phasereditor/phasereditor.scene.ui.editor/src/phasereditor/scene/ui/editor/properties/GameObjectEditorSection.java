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
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.internal.actions.CommandAction;

import phasereditor.scene.core.ContainerModel;
import phasereditor.scene.core.GameObjectEditorComponent;
import phasereditor.scene.core.GroupModel;
import phasereditor.scene.core.NameComputer;
import phasereditor.scene.core.ObjectModel;
import phasereditor.scene.core.ParentComponent;
import phasereditor.scene.core.SceneModel;
import phasereditor.scene.core.VariableComponent;
import phasereditor.scene.ui.editor.SceneEditor;
import phasereditor.scene.ui.editor.SceneUIEditor;
import phasereditor.scene.ui.editor.undo.GroupListSnapshotOperation;
import phasereditor.scene.ui.editor.undo.ScenePropertiesSnapshotOperation;
import phasereditor.ui.EditorSharedImages;

/**
 * @author arian
 *
 */
public class GameObjectEditorSection extends ScenePropertySection {

	private Button _typeButton;
	private Scale _transpScale;
	private List<Action> _orderActions;
	private IAction _addToGroupAction;
	private IAction _removeFromGroupAction;
	private Label _groupsLabel;
	private SelectGroupMenuAction _selectGroupAction;
	// private Action _showBonesAction;
	private Button _parentButton;
	private SelectContainerAction _selectContainerAction;
	private CreateContainerAction _createContainerAction;
	private RemoveFromParentAction _removeFromParentAction;
	private Button _snapButton;
	private CommandAction _duplicateAction;

	public GameObjectEditorSection(ScenePropertyPage page) {
		super("Editor", page);
		setStartCollapsed(true);
	}

	@Override
	public boolean canEdit(Object obj) {
		return obj instanceof GameObjectEditorComponent;
	}

	@SuppressWarnings({ "unused" })
	@Override
	public Control createContent(Composite parent) {

		createActions();

		Composite comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new GridLayout(2, false));

		{
			label(comp, "Type", "*(Editor) The Phaser type of this object." +

					"\n\nClick on the next button to morhp to other type.");

			_typeButton = new Button(comp, SWT.NONE);
			_typeButton.setToolTipText("Click to morph to other type.");
			_typeButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			_typeButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(this::populateTypeList));
		}

		{
			label(comp, "Transparency", "*(Editor) Transparency of the object when is renderer in the editor.");

			_transpScale = new Scale(comp, 0);
			_transpScale.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			_transpScale.setMinimum(0);
			_transpScale.setMinimum(100);
			new SceneScaleListener(_transpScale) {

				@Override
				protected void accept2(float value) {
					getModels().stream()
							.forEach(model -> GameObjectEditorComponent.set_gameObjectEditorTransparency(model, value));

					getEditor().setDirty(true);
				}
			};

		}

		// TODO: No containers for now!
		// {
		// label(comp, "Parent", "*(Editor) The object parent.");
		// _parentButton = new Button(comp, 0);
		// _parentButton.setText("<Display List>");
		// _parentButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		// _parentButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(this::populateParentMenu));
		// }

		{
			label(comp, "Order", "*(Editor) The display depth order.");

			var manager = new ToolBarManager();

			for (var action : _orderActions) {
				manager.add(action);
			}

			var toolbar = manager.createControl(comp);
			toolbar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		}

		{
			label(comp, "Groups", "*(Editor) The object's groups.");

			var rightComp = new Composite(comp, 0);
			rightComp.setLayout(new GridLayout(2, false));
			rightComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			_groupsLabel = new Label(rightComp, 0);
			_groupsLabel.setText("[group1, group2]");
			_groupsLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			var manager = new ToolBarManager();

			manager.add(_addToGroupAction);
			manager.add(_removeFromGroupAction);
			manager.add(_selectGroupAction);

			manager.createControl(rightComp);

		}

		{
			// snap
			label(comp, "Snapping", "*Set the size of the selected objects as snapping values.");
			_snapButton = new Button(comp, 0);
			_snapButton.setText("");
			_snapButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			_snapButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {

				var scene = getScene();

				var sceneModel = scene.getModel();

				var snap = computeSelectionSnap();

				if (snap != null) {

					var before = ScenePropertiesSnapshotOperation.takeSnapshot(getEditor());

					sceneModel.setSnapEnabled(true);
					sceneModel.setSnapWidth(snap[0]);
					sceneModel.setSnapHeight(snap[1]);

					var after = ScenePropertiesSnapshotOperation.takeSnapshot(getEditor());

					getEditor().executeOperation(
							new ScenePropertiesSnapshotOperation(before, after, "Change snapping with selection."));

					user_update_UI_from_Model();

				}

				scene.redraw();

			}));
		}

		return comp;
	}

	@Override
	public void fillToolbar(ToolBarManager manager) {
		for (var action : _orderActions) {
			manager.add(action);
		}

		manager.add(new Separator());

		// manager.add(_showBonesAction);
		manager.add(_duplicateAction);

		manager.add(new Separator());

		manager.add(_addToGroupAction);
		manager.add(_removeFromGroupAction);
		manager.add(_selectGroupAction);

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

		public GroupAction(GroupModel group, String prefix, String icon) {

			super(prefix + " \"" + VariableComponent.get_variableName(group) + "\" (" + group.getChildren().size()
					+ ")", EditorSharedImages.getImageDescriptor(icon));

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
				manager.add(new GroupAction(group, "Add To ", IMG_ADD) {

					@Override
					protected void performGroupOperation(List<ObjectModel> groupChildren) {
						groupChildren.addAll(getModels());
					}

				});
			});

			manager.add(new Separator());
			manager.add(new Action("Add To New Group", EditorSharedImages.getImageDescriptor(IMG_ADD)) {
				@Override
				public void run() {

					var sceneModel = editor.getSceneModel();

					var groupsModel = sceneModel.getGroupsModel();

					var nameComputer = new NameComputer(groupsModel);

					var initialName = nameComputer.newName("group");

					var dlg = new InputDialog(editor.getSite().getShell(), "Create Group",
							"Enter the name of the new Group:", initialName, new IInputValidator() {

								@Override
								public String isValid(String newText) {

									for (var group : ParentComponent.get_children(groupsModel)) {
										if (VariableComponent.get_variableName(group).equals(newText)) {
											return "That name is used.";
										}
									}

									return null;
								}
							});

					if (dlg.open() == Window.OK) {
						var value = dlg.getValue();

						var group = new GroupModel(groupsModel);

						VariableComponent.set_variableName(group, value);

						var before = GroupListSnapshotOperation.takeSnapshot(editor);

						groupsModel.getChildren().add(group);
						group.getChildren().addAll(getModels());

						var after = GroupListSnapshotOperation.takeSnapshot(editor);

						editor.executeOperation(new GroupListSnapshotOperation(before, after, "Add group."));

						editor.refreshOutline();

						editor.updatePropertyPagesContentWithSelection();

						editor.setDirty(true);

						user_update_UI_from_Model();

					}

				}
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
				manager.add(new GroupAction(group, "Remove From ", IMG_DELETE) {

					@Override
					protected void performGroupOperation(List<ObjectModel> groupChildren) {
						groupChildren.removeAll(getModels());
					}

				});
			});

			manager.add(new Separator());
			manager.add(
					new Action("Remove From All Groups", EditorSharedImages.getImageDescriptor(IMG_REMOVE_FROM_GROUP)) {
						@Override
						public void run() {
							var before = GroupListSnapshotOperation.takeSnapshot(editor);

							for (var group : groups) {
								group.getChildren().removeAll(getModels());
							}

							var after = GroupListSnapshotOperation.takeSnapshot(editor);

							editor.executeOperation(new GroupListSnapshotOperation(before, after, getText()));

							editor.setDirty(true);
							editor.getScene().redraw();
							editor.refreshOutline();

							editor.updatePropertyPagesContentWithSelection();
						}
					});
		}

	}

	class SelectGroupMenuAction extends GroupMenuAction {

		public SelectGroupMenuAction() {
			super("Select Group", IMG_SELECT_GROUP);
		}

		@Override
		protected void fillMenu(MenuManager manager, SceneEditor editor, List<GroupModel> groups) {
			for (var group : groups) {
				manager.add(new Action("Select '" + VariableComponent.get_variableName(group) + "'",
						EditorSharedImages.getImageDescriptor(IMG_SELECT_GROUP)) {
					@Override
					public void run() {
						getEditor().setSelection(List.of(group));
						getEditor().updatePropertyPagesContentWithSelection();
					}
				});
			}

			manager.add(new Separator());

			manager.add(new Action("Select All Groups", EditorSharedImages.getImageDescriptor(IMG_SELECT_GROUP)) {

				@SuppressWarnings("all")
				@Override
				public void run() {
					getEditor().setSelection((List) groups);
					getEditor().updatePropertyPagesContentWithSelection();
				}
			});
		}

	}

	class ShowBonesAction extends Action {

		public ShowBonesAction() {
			super("Show/Hide Bones", IAction.AS_CHECK_BOX);
			setImageDescriptor(EditorSharedImages.getImageDescriptor(IMG_BONE));
		}

		@Override
		public void run() {

			var value = isChecked();

			wrapOperation(() -> {
				getModels().forEach(model -> GameObjectEditorComponent.set_gameObjectEditorShowBones(model, value));

				user_update_UI_from_Model();

				getEditor().setDirty(true);
			});
		}
	}

	class SelectContainerAction extends Action {
		public SelectContainerAction() {
			super("Select Container", EditorSharedImages.getImageDescriptor(IMG_BULLET_GO));
		}
	}

	class CreateContainerAction extends Action {
		public CreateContainerAction() {
			super("Create Container", EditorSharedImages.getImageDescriptor(IMG_ADD));
		}

		@Override
		public void runWithEvent(Event event) {
			var editor = getEditor();

			var displayList = editor.getSceneModel().getDisplayList();

			var computer = new NameComputer(displayList);

			var initialName = computer.newName("container");

			var shell = event.display.getActiveShell();

			var dlg = new InputDialog(shell, "Create Container", "Enter the name of the new Container:", initialName,
					new IInputValidator() {
						@Override
						public String isValid(String newText) {
							return null;
						}
					});

			if (dlg.open() == Window.OK) {
				var container = new ContainerModel();

				var name = dlg.getValue();

				VariableComponent.set_variableName(container, name);

				for (var child : getModels()) {
					ParentComponent.utils_moveChild(container, child);
				}

				ParentComponent.utils_addChild(displayList, container);

				getScene().redraw();

				editor.setDirty(true);

				editor.refreshOutline();

				if (editor.getOutline() != null) {
					editor.getOutline().getViewer().getTree().reveal(getModels().toArray());
				}

				user_update_UI_from_Model();
			}
		}
	}

	class RemoveFromParentAction extends Action {
		public RemoveFromParentAction() {
			super("Remove From Parent", EditorSharedImages.getImageDescriptor(IMG_DELETE));
		}
	}

	private void createActions() {
		_orderActions = new ArrayList<>();

		// _orderActions.add(new JFaceOrderAction(editor, OrderActionValue.UP));
		// _orderActions.add(new JFaceOrderAction(editor, OrderActionValue.DOWN));
		// _orderActions.add(new JFaceOrderAction(editor, OrderActionValue.TOP));
		// _orderActions.add(new JFaceOrderAction(editor, OrderActionValue.BOTTOM));

		_orderActions.add(new CommandSectionAction(this, "phasereditor.scene.ui.editor.order_UP"));
		_orderActions.add(new CommandSectionAction(this, "phasereditor.scene.ui.editor.order_DOWN"));
		_orderActions.add(new CommandSectionAction(this, "phasereditor.scene.ui.editor.order_TOP"));
		_orderActions.add(new CommandSectionAction(this, "phasereditor.scene.ui.editor.order_BOTTOM"));

		_addToGroupAction = new AddToGroupMenuAction();
		_removeFromGroupAction = new RemoveFromGroupMenuAction();
		_selectGroupAction = new SelectGroupMenuAction();

		_duplicateAction = new CommandSectionAction(this, SceneUIEditor.COMMAND_ID_DUPLICATE_OBJECTS);

		// _showBonesAction = new ShowBonesAction();

		_selectContainerAction = new SelectContainerAction();
		_createContainerAction = new CreateContainerAction();
		_removeFromParentAction = new RemoveFromParentAction();
	}

	class MorphAction extends Action {
		private String _toType;

		public MorphAction(String toType) {
			super("Morph To " + toType);
			_toType = toType;
		}

		@Override
		public void run() {
			SceneUIEditor.action_MorphObjectsToNewType(getEditor(), getModels(), _toType);
		}

	}

	@SuppressWarnings("unused")
	private void populateParentMenu(SelectionEvent e) {
		var manager = new MenuManager();
		manager.add(_selectContainerAction);
		manager.add(_createContainerAction);
		manager.add(new Separator());
		manager.add(_removeFromParentAction);
		manager.createContextMenu(_parentButton).setVisible(true);
	}

	@SuppressWarnings({ "unused", "boxing" })
	private void populateTypeList(SelectionEvent e) {
		var models = getModels();

		var manager = new MenuManager();

		for (var type : SceneModel.GAME_OBJECT_TYPES) {

			var allow = models.stream()

					.map(m -> m.allowMorphTo(type))

					.reduce(true, (a, b) -> a && b);

			if (allow) {
				manager.add(new MorphAction(type));
			}
		}

		if (manager.getSize() > 0) {
			var menu = manager.createContextMenu(_typeButton);
			menu.setVisible(true);
		}
	}

	@SuppressWarnings("boxing")
	@Override
	public void user_update_UI_from_Model() {
		var models = getModels();

		_typeButton.setText(flatValues_to_String(models.stream().map(model -> model.getType())));

		_transpScale.setSelection(flatValues_to_int(
				models.stream()
						.map(model -> (int) (GameObjectEditorComponent.get_gameObjectEditorTransparency(model) * 100)),
				100));
		{

			var groups = ParentComponent.get_children(getEditor().getSceneModel().getGroupsModel());

			var str = groups.stream().filter(group -> ParentComponent.get_children(group).containsAll(models))
					.map(group -> VariableComponent.get_variableName(group)).collect(Collectors.joining(","));

			_groupsLabel.setText("[" + str + "]");

			_removeFromGroupAction.setEnabled(str.length() > 2);

			_selectGroupAction.setEnabled(_removeFromGroupAction.isEnabled());

		}

		// TODO: Well, no containers for now
		// {
		// _parentButton.setText(flatValues_to_String(getModels().stream()
		// .map(model ->
		// VariableComponent.get_variableName(ParentComponent.get_parent(model)))));
		// }

		// {
		// boolean b = flatValues_to_boolean(
		// getModels().stream().map(model ->
		// GameObjectEditorComponent.get_gameObjectEditorShowBones(model)));
		// _showBonesAction.setChecked(b);
		// }

		{
			var snap = computeSelectionSnap();
			if (snap != null) {
				_snapButton.setText("Set " + snap[0] + " x " + snap[1]);
			}
		}

	}

	private int[] computeSelectionSnap() {
		var renderer = getScene().getSceneRenderer();

		var w = Integer.MAX_VALUE;
		var h = Integer.MAX_VALUE;
		var set = false;

		for (var model : getModels()) {
			var size = renderer.getObjectSize(model);
			if (size != null) {
				if (size[0] > 0 && size[1] > 0) {

					w = (int) Math.min(w, size[0]);
					h = (int) Math.min(h, size[1]);

					if (w != 0 && h != 0) {
						set = true;
					}
				}
			}
		}

		return set ? new int[] { w, h } : null;
	}

}
