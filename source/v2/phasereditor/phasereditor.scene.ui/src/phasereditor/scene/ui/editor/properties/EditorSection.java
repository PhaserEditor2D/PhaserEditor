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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Text;

import phasereditor.scene.core.EditorComponent;
import phasereditor.scene.core.ObjectModel;
import phasereditor.scene.core.ParentComponent;
import phasereditor.scene.core.SceneModel;
import phasereditor.scene.ui.SceneUI;
import phasereditor.scene.ui.editor.SceneEditor;
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
	private List<OrderAction> _orderActions;

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
		_orderActions = new ArrayList<>();
		_orderActions.add(new OrderAction(IMG_DEPTH_UP));
		_orderActions.add(new OrderAction(IMG_DEPTH_DOWN));
		_orderActions.add(new OrderAction(IMG_DEPTH_TOP));
		_orderActions.add(new OrderAction(IMG_DEPTH_BOTTOM));

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

	class OrderAction extends Action {
		private String _icon;

		public OrderAction(String icon) {
			setImageDescriptor(EditorSharedImages.getImageDescriptor(icon));
			_icon = icon;
		}

		@SuppressWarnings({ "incomplete-switch", "boxing" })
		@Override
		public void run() {
			// first, check all models are from the same parent

			ObjectModel parent = null;

			var models = getModels();

			for (var model : models) {
				var parent2 = ParentComponent.get_parent(model);
				if (parent == null) {
					parent = parent2;
				} else {
					if (parent2 != parent) {

						MessageDialog.openInformation(getEditor().getEditorSite().getShell(), "Order Action",
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

				switch (_icon) {
				case IMG_DEPTH_UP:
				case IMG_DEPTH_TOP:
					if (index + 1 == size) {
						canMove = false;
					}
					break;
				case IMG_DEPTH_DOWN:
				case IMG_DEPTH_BOTTOM:
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

				var model = children.get(_icon == IMG_DEPTH_TOP ? children.size() - i - 1 : i);

				if (!modelSet.contains(model)) {
					continue;
				}

				var index = children.indexOf(model);
				var next = index + 1;
				var prev = index - 1;

				switch (_icon) {
				case IMG_DEPTH_UP:
					modelIndexMap.put(model, next);
					break;
				case IMG_DEPTH_DOWN:
					modelIndexMap.put(model, prev);
					break;
				case IMG_DEPTH_TOP:
					modelIndexMap.put(model, top);
					top--;
					break;
				case IMG_DEPTH_BOTTOM:
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
			
			var fparent = parent;
			
			wrapOperation(() -> {
				ParentComponent.set_children(fparent, newChildren);
			}, List.of(parent));

			getEditor().setDirty(true);
			getEditor().refreshOutline();

		}
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
			SceneUI.morphObjectsToNewType(getEditor(), getModels(), _toType);
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
