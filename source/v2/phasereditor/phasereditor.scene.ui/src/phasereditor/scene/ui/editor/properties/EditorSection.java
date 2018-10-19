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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.ToolBarManager;
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
import org.json.JSONObject;

import phasereditor.scene.core.BitmapTextModel;
import phasereditor.scene.core.DynamicBitmapTextModel;
import phasereditor.scene.core.EditorComponent;
import phasereditor.scene.core.ImageModel;
import phasereditor.scene.core.ObjectModel;
import phasereditor.scene.core.ParentComponent;
import phasereditor.scene.core.SceneModel;
import phasereditor.scene.core.SpriteModel;
import phasereditor.scene.core.TileSpriteModel;
import phasereditor.scene.ui.editor.SceneEditor;
import phasereditor.scene.ui.editor.undo.SceneSnapshotOperation;
import phasereditor.scene.ui.editor.undo.WorldSnapshotOperation;
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

	public EditorSection(ScenePropertyPage page) {
		super("Editor", page);
	}

	@Override
	public boolean canEdit(Object obj) {
		return obj instanceof EditorComponent;
	}

	@Override
	public Control createContent(Composite parent) {

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

			toolbar.add(_fieldAction = new Action("Assign to a property.", IAction.AS_CHECK_BOX) {
				{
					setImageDescriptor(EditorSharedImages.getImageDescriptor(IMG_PROPERTY));
				}

				@Override
				public void run() {
					update_editorField();
				}
			});

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

		return comp;
	}

	protected void update_editorField() {
		getModels().forEach(model -> {
			SceneEditor editor = getEditor();

			var before = WorldSnapshotOperation.takeSnapshot(editor);

			EditorComponent.set_editorField(model, _fieldAction.isChecked());

			editor.setDirty(true);

			var after = WorldSnapshotOperation.takeSnapshot(editor);

			editor.executeOperation(new SceneSnapshotOperation(before, after, "Set variables field flag."));
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

			var before = WorldSnapshotOperation.takeSnapshot(getEditor());

			int i = 0;

			var project = getEditor().getEditorInput().getFile().getProject();

			for (var obj : getModels()) {
				var model = obj;

				if (model.getType().equals(_toType)) {
					continue;
				}

				i++;

				var data = new JSONObject();
				model.write(data);

				ObjectModel newModel = null;

				switch (_toType) {
				case SpriteModel.TYPE:
					newModel = new SpriteModel();
					newModel.read(data, project);
					break;
				case ImageModel.TYPE:
					newModel = new ImageModel();
					newModel.read(data, project);
					break;
				case TileSpriteModel.TYPE:
					var tileModel = new TileSpriteModel();
					tileModel.read(data, project);
					tileModel.setSizeToFrame();
					newModel = tileModel;
					break;
				case BitmapTextModel.TYPE:
					newModel = new BitmapTextModel();
					newModel.read(data, project);
					break;
				case DynamicBitmapTextModel.TYPE:
					newModel = new DynamicBitmapTextModel();
					newModel.read(data, project);
					break;

				default:
					break;
				}

				if (newModel != null) {

					var parent = ParentComponent.get_parent(model);
					var siblings = ParentComponent.get_children(parent);
					var index = siblings.indexOf(model);

					ParentComponent.removeFromParent(model);
					ParentComponent.addChild(parent, index, newModel);

					getEditor().refreshOutline_basedOnId();

					getEditor().updatePropertyPagesContentWithSelection_basedOnId();

					getEditor().setDirty(true);
				}

			}

			if (i > 0) {
				var after = WorldSnapshotOperation.takeSnapshot(getEditor());
				getEditor().executeOperation(new WorldSnapshotOperation(before, after, "Morph to " + _toType));
			}

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
