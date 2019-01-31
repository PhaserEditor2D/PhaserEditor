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

import static java.util.stream.Collectors.toSet;

import java.util.HashSet;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.json.JSONObject;

import phasereditor.scene.core.GameObjectComponent;
import phasereditor.scene.core.VisibleComponent;
import phasereditor.ui.EditorSharedImages;

/**
 * @author arian
 *
 */
public class GameObjectSection extends ScenePropertySection {

	private Composite _dataComp;
	private Action _addDataAction;
	private String _focusOnDataKey;

	public GameObjectSection(ScenePropertyPage page) {
		super("Game Object", page);
	}

	@Override
	public boolean canEdit(Object obj) {
		return obj instanceof VisibleComponent && obj instanceof GameObjectComponent;
	}

	@SuppressWarnings("boxing")
	@Override
	public void fillToolbar(ToolBarManager manager) {
		super.fillToolbar(manager);

		{
			// active

			var action = new Action("", IAction.AS_CHECK_BOX) {

				{
					setImageDescriptor(EditorSharedImages.getImageDescriptor(IMG_LIGHTNING));
					setToolTipText(getHelp("Phaser.GameObjects.GameObject.active"));
				}

				@Override
				public void run() {
					wrapOperation(() -> {
						getModels().forEach(model -> GameObjectComponent.set_active(model, isChecked()));
					});

					getEditor().setDirty(true);

					update_UI_from_Model();
				}
			};

			manager.add(action);

			addUpdate(() -> {
				action.setChecked(flatValues_to_Boolean(
						getModels().stream().map(model -> GameObjectComponent.get_active(model))));
			});
		}

		{
			// visible

			var action = new Action("", IAction.AS_CHECK_BOX) {
				{
					setToolTipText(getHelp("Phaser.GameObjects.Components.Visible.visible"));
				}

				@Override
				public void run() {
					wrapOperation(() -> {
						getModels().forEach(model -> VisibleComponent.set_visible(model, isChecked()));
					});

					getEditor().setDirty(true);

					update_UI_from_Model();
				}
			};

			manager.add(action);

			addUpdate(() -> {
				action.setChecked(
						flatValues_to_Boolean(getModels().stream().map(model -> VisibleComponent.get_visible(model))));

				action.setImageDescriptor(

						EditorSharedImages.getImageDescriptor(action.isChecked() ?

								IMG_EYE_OPEN

								: IMG_EYE_CLOSE));
			});
		}

	}

	class AddDataPropertyAction extends Action {

		public AddDataPropertyAction() {
			super("Add Property", EditorSharedImages.getImageDescriptor(IMG_ADD));
		}

		@Override
		public void run() {

			// collect all keys in this scene
			var sceneKeys = new HashSet<String>();

			getEditor().getSceneModel().getDisplayList().visit(model -> {
				if (GameObjectComponent.is(model)) {
					var json = GameObjectComponent.get_data(model);
					if (json != null) {
						sceneKeys.addAll(json.keySet());
					}
				}
			});

			var selectionKeys = getModels().stream().filter(model -> GameObjectComponent.get_data(model) != null)
					.flatMap(model -> GameObjectComponent.get_data(model).keySet().stream()).collect(toSet());

			var keys = new HashSet<>(sceneKeys);
			keys.removeAll(selectionKeys);

			if (keys.isEmpty()) {
				openNewPropertyDialog();
				return;
			}

			// there are unused keys, let's show the menu

			{
				var manager = new MenuManager();

				for (var key : keys) {
					manager.add(new Action("Add '" + key + "'") {
						@Override
						public void run() {
							addProperty(key);
						}
					});
				}

				manager.add(new Separator());
				manager.add(new Action("Add New Property", EditorSharedImages.getImageDescriptor(IMG_ADD)) {
					@Override
					public void run() {
						openNewPropertyDialog();
					}
				});

				var menu = manager.createContextMenu(_dataComp);
				menu.setVisible(true);
			}
		}

		private void openNewPropertyDialog() {
			var dlg = new InputDialog(getEditor().getEditorSite().getShell(), "Add Property", "Enter the property name",
					"", str -> {
						for (var model : getModels()) {
							var json = GameObjectComponent.get_data(model);
							if (json != null) {
								if (json.has(str)) {
									return "That property name exists.";
								}
							}
						}
						return null;
					});

			if (dlg.open() == Window.OK) {

				var key = dlg.getValue();

				addProperty(key);

			}
		}

		private void addProperty(String key) {
			wrapOperation(() -> {

				for (var model : getModels()) {
					var json = GameObjectComponent.get_data(model);

					if (json == null) {
						json = new JSONObject();
						GameObjectComponent.set_data(model, json);
					}

					json.put(key, "0");
				}
			});

			_focusOnDataKey = key;

			getEditor().setDirty(true);
			updateDataRows();
		}
	}

	private void createActions() {
		{
			// data
			_addDataAction = new AddDataPropertyAction();
		}
	}

	@Override
	public Control createContent(Composite parent) {
		createActions();

		var comp = new Composite(parent, 0);
		comp.setLayout(new GridLayout(2, false));

		{

			label(comp, "Data", "");
			var manager = new ToolBarManager();
			manager.add(_addDataAction);
			manager.createControl(comp);

			_dataComp = new Composite(comp, 0);
			_dataComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
			_dataComp.setLayout(new GridLayout(3, false));

			addUpdate(this::updateDataRows);

		}

		return comp;
	}

	private void updateDataRows() {

		for (var comp : _dataComp.getChildren()) {
			comp.dispose();
		}

		var keys = getModels().stream()

				.map(model -> GameObjectComponent.get_data(model))

				.filter(json -> json != null)

				.flatMap(json -> json.keySet().stream())

				.collect(toSet());

		var finalKeys = new HashSet<>(keys);

		for (var key : keys) {
			for (var model : getModels()) {
				var json = GameObjectComponent.get_data(model);
				if (!json.has(key)) {
					finalKeys.remove(key);
				}
			}
		}

		finalKeys.stream().sorted().forEach(key -> {

			var row = new DataRowHandler(_dataComp, key);

			var str = flatValues_to_String(

					getModels().stream()

							.map(model -> GameObjectComponent.get_data(model))

							.filter(json -> json.has(key))

							.map(json -> json.getString(key))

			);

			row.setValue(str);
		});

	}

	private class DataRowHandler {

		private String _key;
		private Text _valueText;

		@SuppressWarnings("unused")
		public DataRowHandler(Composite parent, String key) {

			_key = key;

			{
				var label = new Label(parent, 0);
				label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
				label.setText(_key);
			}

			{
				_valueText = new Text(parent, SWT.BORDER);
				_valueText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
				new SceneText(_valueText) {

					@Override
					protected void accept2(String value) {
						getModels().forEach(obj -> GameObjectComponent.get_data(obj).put(_key, value));
						getEditor().setDirty(true);
					}
				};

			}

			{
				var manager = new ToolBarManager();
				manager.add(new Action("Property Menu", EditorSharedImages.getImageDescriptor(IMG_BULLET_MENU)) {
					@Override
					public void run() {

						var menuManager = new MenuManager();
						menuManager.add(new Action("Delete") {

							@Override
							public void run() {

								wrapOperation(() -> {

									getModels().stream().map(obj -> GameObjectComponent.get_data(obj))

											.filter(json -> json != null)

											.forEach(json -> json.remove(_key));

								});

								updateDataRows();

								getEditor().setDirty(true);
							}
						});
						
						var menu = menuManager.createContextMenu(parent);
						menu.setVisible(true);

					}
				});
				var toolbar = manager.createControl(parent);
				toolbar.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
			}

			if (_key.equals(_focusOnDataKey)) {
				_valueText.setFocus();
				_focusOnDataKey = null;
			}

		}

		public void setValue(String value) {
			_valueText.setText(value);
		}
	}
}
