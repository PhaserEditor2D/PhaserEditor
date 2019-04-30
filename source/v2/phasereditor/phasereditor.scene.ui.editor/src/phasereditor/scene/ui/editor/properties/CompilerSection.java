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
import java.util.function.Consumer;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

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
		comp.setLayout(new GridLayout(3, false));

		// {
		// label(comp, "Generate Method Events", "*Insert events at the start and the
		// end of the methods.");
		// _generateEventsButton = new Button(comp, SWT.CHECK);
		//
		// new CheckListener(_generateEventsButton) {
		//
		// @Override
		// protected void accept(boolean value) {
		// wrapOperation(() -> {
		// getSceneModel().setGenerateMethodEvents(value);
		// });
		// }
		// };
		// }

		{
			label(comp, "Only Generate Methods", "*Generate plain methods, without a containing class.");
			var btn = new Button(comp, SWT.CHECK);
			btn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
			new CheckListener(btn) {

				@Override
				protected void accept(boolean value) {
					wrapOperation(() -> {
						getSceneModel().setOnlyGenerateMethods(value);
					});
				}
			};
			addUpdate(() -> {
				btn.setSelection(getSceneModel().isOnlyGenerateMethods());
			});
		}

		{
			Consumer<String> setValue = value -> {
				wrapOperation(() -> {
					getSceneModel().setSceneKey(value);
				});
			};

			label(comp, "Scene Key",
					"*The unique key of this Scene. Must be unique within the entire Game instance.\nIf not set, no constructor with the scene key will be generated. ");
			var text = new Text(comp, SWT.BORDER);
			text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			new TextListener(text) {

				@Override
				protected void accept(String value) {
					setValue.accept(value);
				}
			};
			createMenuIconToolbar(comp, menu -> {
				var name = getEditor().getEditorInput().getFile().getFullPath().removeFileExtension().lastSegment();
				menu.add(new Action(name) {
					@Override
					public void run() {
						text.setText(name);
						setValue.accept(name);
					}
				});

				menu.add(new Separator());

				menu.add(new Action("(None)") {
					@Override
					public void run() {
						text.setText("");
						setValue.accept("");
					}
				});

			});

			addUpdate(() -> {
				text.setText(getSceneModel().getSceneKey());
			});

		}

		{
			Consumer<String> setValue = value -> {
				wrapOperation(() -> {
					getSceneModel().setSuperClassName(value);
				});
			};

			label(comp, "Super Class", "*The name of the super class.");
			var text = new Text(comp, SWT.BORDER);
			text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			new TextListener(text) {

				@Override
				protected void accept(String value) {
					setValue.accept(value);
				}
			};
			addUpdate(() -> {
				text.setText(getSceneModel().getSuperClassName());
			});

			createMenuIconToolbar(comp, menu -> {
				menu.add(new Action("Phaser.Scene") {
					@Override
					public void run() {
						text.setText(getText());
						setValue.accept(getText());
					}
				});

				menu.add(new Separator());

				menu.add(new Action("(None)") {
					@Override
					public void run() {
						text.setText("");
						setValue.accept("");
					}
				});
			});
		}

		{
			Consumer<String> setValue = value -> {
				wrapOperation(() -> {
					getSceneModel().setPreloadMethodName(value);
				});
			};
			label(comp, "Preload Method",
					"*The name of the preload method.\nLeave it empty if you don't want to generate this method.");
			var text = new Text(comp, SWT.BORDER);
			text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			new TextListener(text) {

				@Override
				protected void accept(String value) {
					setValue.accept(value);
				}
			};
			addUpdate(() -> {
				text.setText(getSceneModel().getPreloadMethodName());
			});

			createMenuIconToolbar(comp, menu -> {
				for (var name : new String[] { "preload", "_preload" }) {
					menu.add(new Action(name) {
						@Override
						public void run() {
							text.setText(getText());
							setValue.accept(getText());
						}
					});
				}

				menu.add(new Separator());

				menu.add(new Action("(None)") {
					@Override
					public void run() {
						text.setText("");
						setValue.accept("");
					}
				});
			});
		}

		{
			Consumer<String> setValue = value -> {
				wrapOperation(() -> {
					getSceneModel().setCreateMethodName(value);
				});
			};

			label(comp, "Create Method", "*The name of the create method.");
			var text = new Text(comp, SWT.BORDER);
			text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			new TextListener(text) {

				@Override
				protected void accept(String value) {
					setValue.accept(value);
				}

			};
			addUpdate(() -> {
				text.setText(getSceneModel().getCreateMethodName());
			});

			createMenuIconToolbar(comp, menu -> {
				for (var name : new String[] { "create", "_create" }) {
					menu.add(new Action(name) {
						@Override
						public void run() {
							text.setText(getText());
							setValue.accept(getText());
						}
					});
				}
			});
		}

		{
			label(comp, "Methods Context Type", "*The context of the method.");
			var combo = new Combo(comp, SWT.READ_ONLY);
			combo.setItems(Arrays.stream(MethodContextType.values()).map(e -> e.name()).toArray(String[]::new));
			combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
			combo.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					wrapOperation(() -> {
						getSceneModel()
								.setMethodContextType(MethodContextType.values()[combo.getSelectionIndex()]);
					});
				}
			});
			addUpdate(() -> {
				combo.select(getSceneModel().getMethodContextType().ordinal());
			});
		}

		{
			var btn = new Button(comp, 0);
			btn.setAlignment(SWT.LEFT);
			btn.setText("Compile Scene");
			btn.setImage(EditorSharedImages.getImage(IMG_BUILD));
			btn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
			btn.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> getEditor().compile()));
		}

		{
			var btn = new Button(comp, 0);
			btn.setAlignment(SWT.LEFT);
			btn.setText("Go To Code");
			btn.setImage(EditorSharedImages.getImage(IMG_GOTO_SOURCE));
			btn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
			btn.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> getEditor().openSourceFile()));
		}

		return comp;
	}

	@Override
	public void fillToolbar(ToolBarManager manager) {
		manager.add(new Action("Compile Scene.", EditorSharedImages.getImageDescriptor(IMG_BUILD)) {
			@Override
			public void run() {
				getEditor().compile();
			}
		});

		manager.add(new Action("Open JavaScript Source File.", EditorSharedImages.getImageDescriptor(IMG_GOTO_SOURCE)) {
			@Override
			public void run() {
				getEditor().openSourceFile();
			}
		});
	}
}
