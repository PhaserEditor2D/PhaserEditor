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
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.json.JSONObject;

import phasereditor.scene.core.EditorComponent;
import phasereditor.scene.core.ObjectModel;
import phasereditor.scene.core.ParentComponent;
import phasereditor.scene.core.SpriteModel;
import phasereditor.scene.core.TileSpriteModel;
import phasereditor.scene.ui.editor.undo.SceneSnapshotOperation;

/**
 * @author arian
 *
 */
public class EditorSection extends ScenePropertySection {

	private Label _editorNameLabel;
	private Text _editorNameText;
	private Button _typeBtn;

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

		_editorNameLabel = new Label(comp, SWT.NONE);
		_editorNameLabel.setText("Var Name");

		_editorNameText = new Text(comp, SWT.BORDER);
		_editorNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		{
			var label = new Label(comp, SWT.NONE);
			label.setText("Type");

			_typeBtn = new Button(comp, SWT.NONE);
			_typeBtn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			_typeBtn.addSelectionListener(SelectionListener.widgetSelectedAdapter(this::populateTypeList));
		}

		update_UI_from_Model();

		return comp;
	}

	class MorphAction extends Action {
		private String _toType;

		public MorphAction(String toType) {
			super("Morph To " + toType);
			_toType = toType;
		}

		@Override
		public void run() {

			var before = SceneSnapshotOperation.takeSnapshot(getEditor());

			int i = 0;

			var project = getEditor().getEditorInput().getFile().getProject();

			for (var obj : getModels()) {
				var model = (ObjectModel) obj;

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
				case TileSpriteModel.TYPE:
					newModel = new TileSpriteModel();
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
				var after = SceneSnapshotOperation.takeSnapshot(getEditor());
				getEditor().executeOperation(new SceneSnapshotOperation(before, after, "Morph to " + _toType));
			}

		}
	}

	@SuppressWarnings("unused")
	private void populateTypeList(SelectionEvent e) {
		var manager = new MenuManager();

		for (var type : new String[] { SpriteModel.TYPE, TileSpriteModel.TYPE }) {
			manager.add(new MorphAction(type));
		}

		var menu = manager.createContextMenu(_typeBtn);
		menu.setVisible(true);

	}

	@Override
	public void update_UI_from_Model() {

		var models = List.of(getModels());

		_editorNameText.setText(flatValues_to_String(
				models.stream().map(model -> EditorComponent.get_editorName((ObjectModel) model))));

		_typeBtn.setText(flatValues_to_String(models.stream().map(model -> ((ObjectModel) model).getType())));

		listen(_editorNameText, value -> {
			models.stream().forEach(model -> EditorComponent.set_editorName((ObjectModel) model, value));

			getEditor().setDirty(true);
			getEditor().refreshOutline();

		}, models);
	}

}
