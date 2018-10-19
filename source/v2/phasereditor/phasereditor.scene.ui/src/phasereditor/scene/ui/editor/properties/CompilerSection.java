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
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;

import phasereditor.scene.core.SceneCore;
import phasereditor.scene.core.SceneModel;
import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.properties.FormPropertyPage;

/**
 * @author arian
 *
 */
public class CompilerSection extends BaseDesignSection {

	private UserCodeBeforeAfterCodeComp _createComp;
	private UserCodeBeforeAfterCodeComp _preloadComp;

	public CompilerSection(FormPropertyPage page) {
		super("Compiler", page);
	}

	@Override
	public boolean canEdit(Object obj) {
		return obj instanceof SceneModel;
	}

	@Override
	public Control createContent(Composite parent) {

		var comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new GridLayout(1, false));

		{
			// user code

			label(comp, "User Code:", "*The user code to be inserted in the JavaScript file.");

			var tabFolder = new TabFolder(comp, SWT.NONE);
			var gd = new GridData(GridData.FILL_BOTH);
			gd.heightHint = 300;
			tabFolder.setLayoutData(gd);
			tabFolder.setBackgroundMode(SWT.INHERIT_FORCE);

			{

				var item = new TabItem(tabFolder, SWT.NONE);
				var codeComp = new UserCodeBeforeAfterCodeComp(tabFolder, SWT.NONE, "create");
				item.setControl(codeComp);
				item.setText("Create Method");
				_createComp = codeComp;
			}

			{

				var item = new TabItem(tabFolder, SWT.NONE);
				var codeComp = new UserCodeBeforeAfterCodeComp(tabFolder, SWT.NONE, "preload");
				item.setControl(codeComp);
				item.setText("Preload Method");
				_preloadComp = codeComp;
			}

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
	public void update_UI_from_Model() {
		var model = getEditor().getSceneModel();

		var preload = model.getPreloadUserCode();
		var create = model.getCreateUserCode();

		_preloadComp.setBeforeText(preload.getBeforeCode());
		_preloadComp.setAfterText(preload.getAfterCode());

		_createComp.setBeforeText(create.getBeforeCode());
		_createComp.setAfterText(create.getAfterCode());

		listen(_preloadComp.getBeforeTextViewer(), value -> {
			wrapOperation(() -> {
				preload.setBeforeCode(value);
			});
		});

		listen(_preloadComp.getAfterTextViewer(), value -> {
			wrapOperation(() -> {
				preload.setAfterCode(value);
			});
		});

		listen(_createComp.getBeforeTextViewer(), value -> {
			wrapOperation(() -> {
				create.setBeforeCode(value);
			});
		});

		listen(_createComp.getAfterTextViewer(), value -> {
			wrapOperation(() -> {
				create.setAfterCode(value);
			});
		});

	}

}
