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

import java.util.Arrays;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;

import phasereditor.scene.core.SceneCore;
import phasereditor.scene.core.SceneModel;
import phasereditor.scene.core.SceneModel.MethodContextType;
import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.properties.CheckListener;
import phasereditor.ui.properties.FormPropertyPage;
import phasereditor.ui.properties.TextListener;

/**
 * @author arian
 *
 */
public class CompilerSection extends BaseDesignSection {

	private Button _autoLoadAssetsButton;
	private Text _preloadNameText;
	private Text _createNameText;
	private Button _generateEventsButton;
	private Text _superClassNameText;
	private Button _onlyGenerateMethodsButton;

	public CompilerSection(FormPropertyPage page) {
		super("Compiler", page);
	}

	@Override
	public boolean canEdit(Object obj) {
		return obj instanceof SceneModel;
	}

	@SuppressWarnings({ "unused" })
	@Override
	public Control createContent(Composite parent) {

		var comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new GridLayout(2, false));

		{
			label(comp, "Generate Assets Loading",
					"*Generate a preload method that loads all the assets used in this scene.");
			_autoLoadAssetsButton = new Button(comp, SWT.CHECK);
			new CheckListener(_autoLoadAssetsButton) {

				@Override
				protected void accept(boolean value) {
					wrapOperation(() -> {
						getScene().getModel().setAutoLoadAssets(value);
					});
				}
			};
		}

		{
			label(comp, "Generate Method Events", "*Insert events at the start and the end of the methods.");
			_generateEventsButton = new Button(comp, SWT.CHECK);

			new CheckListener(_generateEventsButton) {

				@Override
				protected void accept(boolean value) {
					wrapOperation(() -> {
						getScene().getModel().setGenerateMethodEvents(value);
					});
				}
			};
		}

		{
			label(comp, "Only Generate Methods", "*Generate plain methods, without a containing class.");
			_onlyGenerateMethodsButton = new Button(comp, SWT.CHECK);
			new CheckListener(_onlyGenerateMethodsButton) {

				@Override
				protected void accept(boolean value) {
					wrapOperation(() -> {
						getScene().getModel().setOnlyGenerateMethods(value);
					});
				}
			};
		}

		{
			label(comp, "Scene Key",
					"*The unique key of this Scene. Must be unique within the entire Game instance.\nIf not set, no constructor with the scene key will be generated. ");
			var text = new Text(comp, SWT.BORDER);
			text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			new TextListener(text) {

				@Override
				protected void accept(String value) {
					wrapOperation(() -> {
						getScene().getModel().setSceneKey(value);
					});
				}
			};
			addUpdate(() -> {
				text.setText(getScene().getModel().getSceneKey());
			});

		}

		{
			label(comp, "Super Class", "*The name of the super class.");
			_superClassNameText = new Text(comp, SWT.BORDER);
			_superClassNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			new TextListener(_superClassNameText) {

				@Override
				protected void accept(String value) {
					wrapOperation(() -> {
						getScene().getModel().setSuperClassName(value);
					});
				}
			};
		}

		{
			label(comp, "Preload Method", "*The name of the preload method.");
			_preloadNameText = new Text(comp, SWT.BORDER);
			_preloadNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			new TextListener(_preloadNameText) {

				@Override
				protected void accept(String value) {
					wrapOperation(() -> {
						getScene().getModel().setPreloadMethodName(value);
					});
				}
			};
		}

		{
			label(comp, "Create Method", "*The name of the create method.");
			_createNameText = new Text(comp, SWT.BORDER);
			_createNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			new TextListener(_createNameText) {

				@Override
				protected void accept(String value) {
					wrapOperation(() -> {
						getScene().getModel().setCreateMethodName(value);
					});
				}

			};
		}

		{
			label(comp, "Methods Context Type", "*The context of the method.");
			var combo = new Combo(comp, SWT.READ_ONLY);
			combo.setItems(Arrays.stream(MethodContextType.values()).map(e -> e.name()).toArray(String[]::new));
			combo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			combo.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					wrapOperation(() -> {
						getScene().getModel()
								.setMethodContextType(MethodContextType.values()[combo.getSelectionIndex()]);
					});
				}
			});
			addUpdate(() -> {
				combo.select(getScene().getModel().getMethodContextType().ordinal());
			});
		}

		return comp;
	}

	void openSourceFile() {
		var file = SceneCore.getSceneSourceCodeFile(getEditor().getEditorInput().getFile());
		if (file.exists()) {
			try {
				IDE.openEditor(getEditor().getEditorSite().getWorkbenchWindow().getActivePage(), file);
			} catch (PartInitException e1) {
				e1.printStackTrace();
				throw new RuntimeException(e1);
			}
		}
	}

	@Override
	public void fillToolbar(ToolBarManager manager) {
		manager.add(new Action("Compile scene.", EditorSharedImages.getImageDescriptor(IMG_BUILD)) {
			@Override
			public void run() {
				getEditor().compile();
			}
		});

		manager.add(new Action("Open JavaScript Source File.", EditorSharedImages.getImageDescriptor(IMG_GOTO_SOURCE)) {
			@Override
			public void run() {
				openSourceFile();
			}
		});
	}

	@Override
	public void user_update_UI_from_Model() {
		var model = getEditor().getSceneModel();

		_autoLoadAssetsButton.setSelection(model.isAutoLoadAssets());
		_generateEventsButton.setSelection(model.isGenerateMethodEvents());
		_onlyGenerateMethodsButton.setSelection(model.isOnlyGenerateMethods());

		_superClassNameText.setText(model.getSuperClassName());
		_preloadNameText.setText(model.getPreloadMethodName());
		_createNameText.setText(model.getCreateMethodName());

	}

}
