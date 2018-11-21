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
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

import phasereditor.scene.core.VariableComponent;
import phasereditor.scene.ui.editor.SceneEditor;
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
		return obj instanceof VariableComponent;
	}

	@Override
	public Control createContent(Composite parent) {

		createActions();

		Composite comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new GridLayout(2, false));

		{
			label(comp, "Var Name", "*(Editor) The name of the variable used in the generated code.");

			_editorNameText = new Text(comp, SWT.BORDER);
			_editorNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		}

		return comp;
	}

	@Override
	public void fillToolbar(ToolBarManager manager) {
		manager.add(_fieldAction);
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

			VariableComponent.set_variableField(model, _fieldAction.isChecked());

			editor.setDirty(true);

			var after = SingleObjectSnapshotOperation.takeSnapshot(getModels());

			editor.executeOperation(new SingleObjectSnapshotOperation(before, after, "Set variables field flag."));
		});
	}

	@SuppressWarnings("boxing")
	@Override
	public void update_UI_from_Model() {
		var models = getModels();

		_editorNameText
				.setText(flatValues_to_String(models.stream().map(model -> VariableComponent.get_variableName(model))));

		_fieldAction.setChecked(
				flatValues_to_boolean(models.stream().map(model -> VariableComponent.get_variableField(model))));

		listen(_editorNameText, value -> {
			
			models.stream().forEach(model -> VariableComponent.set_variableName(model, value));

			getEditor().setDirty(true);
			getEditor().refreshOutline();

		}, models);
	}

}
