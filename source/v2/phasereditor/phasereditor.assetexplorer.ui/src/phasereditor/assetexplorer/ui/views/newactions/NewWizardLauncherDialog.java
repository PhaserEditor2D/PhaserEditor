// The MIT License (MIT)
//
// Copyright (c) 2015, 2019 Arian Fornaris
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
package phasereditor.assetexplorer.ui.views.newactions;

import static phasereditor.ui.PhaserEditorUI.swtRun;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;

import phasereditor.project.core.ProjectCore;
import phasereditor.ui.TreeArrayContentProvider;

/**
 * @author arian
 *
 */
public class NewWizardLauncherDialog extends Dialog {

	private FilteredTree _filteredTree;
	private TreeViewer _viewer;

	public NewWizardLauncherDialog(Shell parent) {
		super(parent);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("New");
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		_filteredTree = new FilteredTree((Composite) super.createDialogArea(parent), SWT.SINGLE | SWT.BORDER,
				new PatternFilter(), true);
		_viewer = _filteredTree.getViewer();
		_viewer.setContentProvider(new TreeArrayContentProvider());
		_viewer.setInput(new Object[] {

				new NewAtlasWizardLauncher(),

				new NewAssetPackWizardLauncher(),

				new NewAnimationWizardLauncher(),

				new NewSceneWizardLauncher(),

				new NewJavaScriptClassWizardLauncher(),

				new NewFactoryJSClassWizardLauncher(),

				new NewProjectWizardLauncher(),

				new NewExampleProjectWizardLauncher()

		});

		_viewer.setLabelProvider(new LabelProvider() {
			@Override
			public Image getImage(Object element) {
				var launcher = (NewWizardLancher) element;
				return launcher.getImage();
			}

			@Override
			public String getText(Object element) {
				var launcher = (NewWizardLancher) element;
				return launcher.getName();
			}
		});

		_viewer.addDoubleClickListener(e -> {
			this.okPressed();
		});

		return _filteredTree;
	}

	@Override
	protected void okPressed() {
		var selection = _viewer.getSelection();
		if (!selection.isEmpty()) {
			var elem = (NewWizardLancher) _viewer.getStructuredSelection().getFirstElement();
			swtRun(() -> {
				runLauncher(elem);
			});
		}
		super.okPressed();
	}

	private static void runLauncher(NewWizardLancher launcher) {
		launcher.openWizard(ProjectCore.getActiveProject());
	}

}
