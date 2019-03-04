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
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

import phasereditor.scene.core.CodeDomComponent;
import phasereditor.scene.core.SceneCompiler;
import phasereditor.scene.core.VariableComponent;
import phasereditor.scene.ui.editor.SceneEditor;
import phasereditor.scene.ui.editor.SceneUIEditor;
import phasereditor.scene.ui.editor.undo.SingleObjectSnapshotOperation;
import phasereditor.ui.EditorSharedImages;

/**
 * @author arian
 *
 */
public class VariableSection extends ScenePropertySection {

	private Action _fieldAction;
	private Action _goToCodeAction;

	public VariableSection(ScenePropertyPage page) {
		super("Variable", page);
	}

	@Override
	public boolean canEdit(Object obj) {
		return obj instanceof VariableComponent;
	}

	@SuppressWarnings("unused")
	@Override
	public Control createContent(Composite parent) {

		createActions();

		Composite comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new GridLayout(3, false));

		{
			label(comp, "Var Name", "*(Editor) The name of the variable used in the generated code.");

			var text = new Text(comp, SWT.BORDER);
			text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
			new SceneText(text) {

				@Override
				protected void accept2(String value) {
					
					// just don't allow empty names
					if (value.length() == 0) {
						return;
					}
					
					int len = getModels().size();

					if (len > 1) {
						int i = 1;
						for (var model : getModels()) {
							VariableComponent.set_variableName(model, value + "_" + i);
							i++;
						}
					} else {
						getModels().stream().forEach(model -> VariableComponent.set_variableName(model, value));
					}

					getEditor().setDirty(true);
					getEditor().refreshOutline();

					// let's do this to update other fields like GameObject.name
					getEditor().updatePropertyPagesContentWithSelection();
				}
			};
			addUpdate(() -> {
				text.setText(flatValues_to_String(
						getModels().stream().map(model -> VariableComponent.get_variableName(model))));
			});
		}

		{
			var btn = new Button(comp, SWT.BORDER);
			btn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
			btn.setText("Go To Code");
			btn.setImage(EditorSharedImages.getImage(IMG_GOTO_SOURCE));
			btn.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> _goToCodeAction.run()));

		}

		return comp;
	}

	@Override
	public void fillToolbar(ToolBarManager manager) {
		manager.add(_fieldAction);
		manager.add(_goToCodeAction);
	}

	@SuppressWarnings("boxing")
	private void createActions() {
		_fieldAction = new Action("Assign to a property.", IAction.AS_CHECK_BOX) {
			{
				setImageDescriptor(EditorSharedImages.getImageDescriptor(IMG_PROPERTY));
			}

			@Override
			public void run() {
				getModels().forEach(model -> {
					SceneEditor editor = getEditor();

					var before = SingleObjectSnapshotOperation.takeSnapshot(getModels());

					VariableComponent.set_variableField(model, _fieldAction.isChecked());

					editor.setDirty(true);

					var after = SingleObjectSnapshotOperation.takeSnapshot(getModels());

					editor.executeOperation(
							new SingleObjectSnapshotOperation(before, after, "Set variables field flag."));
				});
			}
		};

		addUpdate(() -> {
			_fieldAction.setChecked(flatValues_to_boolean(
					getModels().stream().map(model -> VariableComponent.get_variableField(model))));
		});

		_goToCodeAction = new Action("Go To Code", EditorSharedImages.getImageDescriptor(IMG_GOTO_SOURCE)) {
			@Override
			public void run() {
				try {
					var codeDom = CodeDomComponent.get_codeDom(getModels().get(0));

					if (codeDom == null) {
						var compiler = new SceneCompiler(getEditor().getEditorInput().getFile(), getSceneModel());
						compiler.compile();
						codeDom = CodeDomComponent.get_codeDom(getModels().get(0));
					}

					getEditor().openSourceFile(codeDom.getOffset());

				} catch (Exception e) {
					SceneUIEditor.logError(e);
				}
			}
		};
	}
}
