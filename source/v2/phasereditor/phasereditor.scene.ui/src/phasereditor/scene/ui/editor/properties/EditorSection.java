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
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolItem;

import phasereditor.scene.core.EditorComponent;
import phasereditor.scene.core.GroupComponent;
import phasereditor.scene.core.ObjectModel;
import phasereditor.scene.core.ParentComponent;
import phasereditor.scene.core.SceneModel;
import phasereditor.scene.ui.SceneUI;
import phasereditor.scene.ui.editor.SceneEditor;
import phasereditor.scene.ui.editor.properties.OrderAction.OrderActionValue;
import phasereditor.scene.ui.editor.undo.SingleObjectSnapshotOperation;
import phasereditor.ui.EditorSharedImages;

/**
 * @author arian
 *
 */
public class EditorSection extends ScenePropertySection {

	private Text _editorNameText;
	private Button _typeBtn;
	private Action _fieldAction;
	private Scale _transpScale;
	private List<JFaceOrderAction> _orderActions;
	private IAction _addToGroupAction;
	private IAction _removeFromGroupAction;
	private Label _groupsLabel;

	public EditorSection(ScenePropertyPage page) {
		super("Editor", page);
	}

	@Override
	public boolean canEdit(Object obj) {
		return obj instanceof EditorComponent;
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

		{
			label(comp, "Type", "*(Editor) The Phaser type of this object." +

					"\n\nClick on the next button to morhp to other type.");

			_typeBtn = new Button(comp, SWT.NONE);
			_typeBtn.setToolTipText("Click to morph to other type.");
			_typeBtn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			_typeBtn.addSelectionListener(SelectionListener.widgetSelectedAdapter(this::populateTypeList));
		}

		{
			label(comp, "Transparency", "*(Editor) Transparency of the object when is renderer in the editor.");

			_transpScale = new Scale(comp, 0);
			_transpScale.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			_transpScale.setMinimum(0);
			_transpScale.setMinimum(100);
		}

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

			manager.createControl(rightComp);

		}

		return comp;
	}

	@Override
	public void fillToolbar(ToolBarManager manager) {
		for (var action : _orderActions) {
			manager.add(action);
		}

		manager.add(new Separator());

		manager.add(_fieldAction);
	}

	private void createActions() {
		var editor = getEditor();

		_orderActions = new ArrayList<>();
		_orderActions.add(new JFaceOrderAction(editor, OrderActionValue.UP));
		_orderActions.add(new JFaceOrderAction(editor, OrderActionValue.DOWN));
		_orderActions.add(new JFaceOrderAction(editor, OrderActionValue.TOP));
		_orderActions.add(new JFaceOrderAction(editor, OrderActionValue.BOTTOM));

		_fieldAction = new Action("Assign to a property.", IAction.AS_CHECK_BOX) {
			{
				setImageDescriptor(EditorSharedImages.getImageDescriptor(IMG_PROPERTY));
			}

			@Override
			public void run() {
				update_editorField();
			}
		};

		_addToGroupAction = new Action("Add this object to a group.",
				EditorSharedImages.getImageDescriptor(IMG_ADD_TO_GROUP)) {
			@Override
			public void runWithEvent(Event event) {
				var manager = new MenuManager();

				var groups = ParentComponent.get_children(editor.getSceneModel().getGroupsModel());
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
					manager.add(
							new Action(GroupComponent.get_name(group), EditorSharedImages.getImageDescriptor(IMG_ADD)) {

								@Override
								public void run() {

									addObjectsToGroup(group);

								}

							});
				});

				var menu = manager.createContextMenu(((ToolItem) event.widget).getParent());
				menu.setVisible(true);
			}

		};

		_removeFromGroupAction = new Action("Remove this object from a group.",
				EditorSharedImages.getImageDescriptor(IMG_REMOVE_FROM_GROUP)) {

			@Override
			public void runWithEvent(Event event) {
				var manager = new MenuManager();

				var groups = ParentComponent.get_children(editor.getSceneModel().getGroupsModel());

				groups.stream().filter(group -> {

					// just accepts the groups that contains all the selected objects.

					return ParentComponent.get_children(group).containsAll(getModels());
				}).forEach(group -> {
					manager.add(new Action(GroupComponent.get_name(group),
							EditorSharedImages.getImageDescriptor(IMG_DELETE)) {

						@Override
						public void run() {

							removeObjectsFromGroup(group);

						}
					});
				});

				var menu = manager.createContextMenu(((ToolItem) event.widget).getParent());
				menu.setVisible(true);
			}

		};
	}

	protected void addObjectsToGroup(ObjectModel group) {
		ParentComponent.get_children(group).addAll(getModels());

		var editor = getEditor();

		editor.setDirty(true);
		editor.getScene().redraw();
		editor.refreshOutline();
		editor.updatePropertyPagesContentWithSelection();
	}

	protected void removeObjectsFromGroup(ObjectModel group) {
		ParentComponent.get_children(group).removeAll(getModels());

		var editor = getEditor();

		editor.setDirty(true);
		editor.getScene().redraw();
		editor.refreshOutline();
		editor.updatePropertyPagesContentWithSelection();
	}

	protected void update_editorField() {
		getModels().forEach(model -> {
			SceneEditor editor = getEditor();

			var before = SingleObjectSnapshotOperation.takeSnapshot(getModels());

			EditorComponent.set_editorField(model, _fieldAction.isChecked());

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
			var menu = manager.createContextMenu(_typeBtn);
			menu.setVisible(true);
		}
	}

	@SuppressWarnings("boxing")
	@Override
	public void update_UI_from_Model() {
		var models = getModels();

		_editorNameText
				.setText(flatValues_to_String(models.stream().map(model -> EditorComponent.get_editorName(model))));

		_fieldAction.setChecked(
				flatValues_to_boolean(models.stream().map(model -> EditorComponent.get_editorField(model))));

		_typeBtn.setText(flatValues_to_String(models.stream().map(model -> model.getType())));

		_transpScale.setSelection(flatValues_to_int(
				models.stream().map(model -> (int) (EditorComponent.get_editorTransparency(model) * 100)), 100));
		{

			var str = ParentComponent.get_children(getEditor().getSceneModel().getGroupsModel()).stream()
					.filter(group -> ParentComponent.get_children(group).containsAll(models))
					.map(group -> GroupComponent.get_name(group)).collect(Collectors.joining(","));

			_groupsLabel.setText("[" + str + "]");

		}

		listen(_editorNameText, value -> {
			models.stream().forEach(model -> EditorComponent.set_editorName(model, value));

			getEditor().setDirty(true);
			getEditor().refreshOutline();

		}, models);

		listenFloat(_transpScale, value -> {

			models.stream().forEach(model -> EditorComponent.set_editorTransparency(model, value));

			getEditor().setDirty(true);
		}, models);
	}

}
