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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.json.JSONObject;

import phasereditor.scene.core.GameObjectComponent;
import phasereditor.scene.core.ObjectModel;
import phasereditor.scene.core.VariableComponent;
import phasereditor.scene.core.VisibleComponent;
import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.properties.CheckListener;

/**
 * @author arian
 *
 */
@SuppressWarnings("synthetic-access")
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

						flatValues_to_Boolean(

								getModels().stream().map(model ->

								VisibleComponent.get_visible(model)

								)));

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
					manager.add(new Action("Add Property '" + key + "'") {
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

	@SuppressWarnings({ "unused", "boxing" })
	@Override
	public Control createContent(Composite parent) {
		createActions();

		var comp = new Composite(parent, 0);
		comp.setLayout(new GridLayout(3, false));

		{
			label(comp, "Name", "Phaser.GameObjects.GameObject.name");
			var btn = new Button(comp, SWT.CHECK);
			btn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
			new CheckListener(btn) {

				@Override
				protected void accept(boolean value) {
					wrapOperation(() -> {
						getModels().forEach(model -> GameObjectComponent.set_useName(model, value));
					});

					getEditor().setDirty(true);
				}
			};

			addUpdate(() -> {
				btn.setText(flatValues_to_String(getModels().stream().map(VariableComponent::get_variableName)));
				btn.setSelection(flatValues_to_Boolean(getModels().stream().map(GameObjectComponent::get_useName)));
			});
		}

		{
			label(comp, "Factory",
					"*(Editor) Custom GameObjectFactory method. Leave it blank to use the default method.");

			var text = new Text(comp, SWT.BORDER);
			text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			new SceneText(text) {

				@Override
				protected void accept2(String value) {
					getModels().stream().forEach(model -> GameObjectComponent.set_objectFactory(model, value));
					getEditor().setDirty(true);
					getEditor().updatePropertyPagesContentWithSelection();
				}
			};

			addUpdate(() -> {
				text.setText(flatValues_to_String(
						getModels().stream().map(model -> GameObjectComponent.get_objectFactory(model))));
			});

			var manager = new ToolBarManager();
			manager.add(new Action("Factory menu.", EditorSharedImages.getImageDescriptor(IMG_BULLET_MENU)) {

				@Override
				public void run() {
					var menuManager = new MenuManager();
					var factory = flatValues_to_String(
							getModels().stream().map(model -> GameObjectComponent.get_objectFactory(model)))

									.trim();

					var set = new HashSet<String>();

					getSceneModel().getDisplayList().visit(model -> {
						if (GameObjectComponent.is(model)) {
							var factory2 = GameObjectComponent.get_objectFactory(model);
							if (!factory2.equals(factory)) {
								set.add(factory2);
							}
						}
					});

					set.remove(GameObjectComponent.objectFactory_default);

					for (var factory2 : set) {
						menuManager.add(new Action("Set User Factory '" + factory2 + "'") {
							@Override
							public void run() {
								setObjectFactory(factory2);
							}
						});
					}

					menuManager.add(new Separator());

					if (!factory.equals(GameObjectComponent.objectFactory_default)) {
						menuManager.add(new Action("Clear User Factory") {
							@Override
							public void run() {
								setObjectFactory(GameObjectComponent.objectFactory_default);
							}
						});
					}

					if (factory.length() > 0) {
						menuManager.add(new Action("Select All Objects With Factory '" + factory + "'") {

							@Override
							public void run() {
								var list = new ArrayList<ObjectModel>();

								getSceneModel().getDisplayList().visit(model -> {
									if (GameObjectComponent.is(model)
											&& GameObjectComponent.get_objectFactory(model).equals(factory)) {
										list.add(model);
									}
								});

								setSelection(list);
							}
						});
					}

					var menu = menuManager.createContextMenu(comp);
					menu.setVisible(true);
				}
			});
			manager.createControl(comp);
		}

		{
			label(comp, "Factory R. Type",
					"*(Editor) If the Factory is set, then add jsdoc to set the return type. You can leave it blank.");

			var text = new Text(comp, SWT.BORDER);
			text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			new SceneText(text) {

				@Override
				protected void accept2(String value) {
					getModels().stream().forEach(model -> GameObjectComponent.set_objectFactoryType(model, value));
					getEditor().setDirty(true);
					getEditor().updatePropertyPagesContentWithSelection();
				}
			};

			addUpdate(() -> {
				text.setText(flatValues_to_String(getModels().stream()

						.map(model -> GameObjectComponent.get_objectFactoryType(model))));

				text.setEnabled(getModels().stream()

						.map(model -> GameObjectComponent.get_objectFactory(model))

						.filter(s -> s.trim().length() == 0)

						.count() == 0

				);
			});

			createMenuIconToolbar(comp, menu -> {
				var currentFactory = flatValues_to_String(getModels()

						.stream()

						.map(model -> GameObjectComponent.get_objectFactory(model)))

								.trim();

				var currentType = text.getText();

				var set = new HashSet<String>();

				getSceneModel().getDisplayList().visit(model -> {
					if (GameObjectComponent.is(model)) {
						var factory2 = GameObjectComponent.get_objectFactory(model);
						if (factory2.equals(currentFactory)) {
							var type = GameObjectComponent.get_objectFactoryType(model);
							set.add(type);
						}
					}
				});

				set.remove(GameObjectComponent.objectFactoryType_default);
				set.remove(currentType);

				for (var type : set) {
					menu.add(new Action("Set Factory Type '" + type + "'") {
						@Override
						public void run() {
							setObjectFactoryType(type);
						}
					});
				}

				menu.add(new Separator());

				if (!currentType.equals(GameObjectComponent.objectFactoryType_default)) {
					menu.add(new Action("Clear Factory Type") {
						@Override
						public void run() {
							setObjectFactoryType(GameObjectComponent.objectFactoryType_default);
						}
					});
				}
			});
		}

		{
			new Label(comp, 0);
			var btn = new Button(comp, SWT.CHECK);
			btn.setText("Build Object");
			btn.setToolTipText("(Phaser Editor) If checked, a build() method will be applied to this object.");
			btn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
			new CheckListener(btn) {

				@Override
				protected void accept(boolean value) {
					wrapOperation(() -> {
						getModels().forEach(model -> GameObjectComponent.set_objectBuild(model, value));
					});
					getEditor().setDirty(true);
				}
			};

			addUpdate(() -> {
				long count = getModels().stream()
						.filter(model -> GameObjectComponent.get_objectFactory(model).trim().length() > 0).count();
				btn.setEnabled(count == getModels().size());
				btn.setSelection(flatValues_to_Boolean(getModels().stream().map(GameObjectComponent::get_objectBuild)));
			});
		}

		{

			label(comp, "Data", "Phaser.GameObjects.GameObject.data");
			var manager = new ToolBarManager();
			manager.add(_addDataAction);
			manager.createControl(comp).setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));

			new Label(comp, 0);

			_dataComp = new Composite(comp, 0);
			_dataComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
			_dataComp.setLayout(new GridLayout(3, false));

			addUpdate(this::updateDataRows);

		}

		return comp;
	}

	private void setObjectFactory(String factory) {
		wrapOperation(() -> {
			Set<String> types;

			if (factory.equals(GameObjectComponent.objectFactory_default)) {
				types = Set.of(GameObjectComponent.objectFactoryType_default);
			} else {
				types = getSceneModel().getDisplayList().stream()

						.filter(GameObjectComponent::is)

						.filter(model -> GameObjectComponent.get_objectFactory(model).equals(factory))

						.map(GameObjectComponent::get_objectFactoryType)

						.collect(toSet());
			}

			if (types.size() == 1) {
				var type = types.iterator().next();
				getModels().forEach(model -> GameObjectComponent.set_objectFactoryType(model, type));
			}

			getModels().forEach(model -> GameObjectComponent.set_objectFactory(model, factory));
		});

		getEditor().setDirty(true);
		getEditor().updatePropertyPagesContentWithSelection();
	}

	private void setObjectFactoryType(String factory) {
		wrapOperation(() -> {
			getModels().forEach(model -> GameObjectComponent.set_objectFactoryType(model, factory));
		});

		getEditor().setDirty(true);
		getEditor().updatePropertyPagesContentWithSelection();
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
				if (json != null && !json.has(key)) {
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

	private void setSelection(ArrayList<ObjectModel> list) {
		getEditor().setSelection(list);
		getEditor().updatePropertyPagesContentWithSelection();
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
						menuManager.add(new Action("Select All Objects With Property '" + _key + "'") {
							@Override
							public void run() {
								selectObjectsWithSameProperty(null);
							}

						});

						menuManager.add(new Action(
								"Select All Objects With Property '" + _key + "=" + _valueText.getText() + "'") {
							@Override
							public void run() {
								selectObjectsWithSameProperty(_valueText.getText());
							}

						});

						menuManager.add(new Separator());

						menuManager.add(new Action("Delete Property '" + _key + "'",
								EditorSharedImages.getImageDescriptor(IMG_DELETE)) {

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

		private void selectObjectsWithSameProperty(String value) {
			var list = new ArrayList<ObjectModel>();

			getEditor().getSceneModel().getDisplayList().visit(model -> {
				if (GameObjectComponent.is(model)) {
					var json = GameObjectComponent.get_data(model);
					if (json != null) {

						if (json.has(_key)) {

							if (value == null) {
								list.add(model);
							} else {
								var value2 = json.getString(_key);
								if (value.equals(value2)) {
									list.add(model);
								}
							}
						}
					}

				}
			});

			setSelection(list);
		}

		public void setValue(String value) {
			_valueText.setText(value);
		}
	}
}
