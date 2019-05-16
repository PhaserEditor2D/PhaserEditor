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
package phasereditor.assetpack.ui.editor.wizards;

import java.util.List;
import java.util.function.Consumer;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;

import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.assetpack.core.AssetPackModel;
import phasereditor.assetpack.core.AssetSectionModel;
import phasereditor.assetpack.ui.AssetLabelProvider;
import phasereditor.assetpack.ui.editor.AssetPackEditor;
import phasereditor.assetpack.ui.editor.AssetPackUIEditor;
import phasereditor.project.core.ProjectCore;
import phasereditor.ui.PhaserEditorUI;
import phasereditor.ui.TreeArrayContentProvider;

/**
 * @author arian
 *
 */
public class NewPage_AssetPackSection extends WizardPage {
	private TreeViewer _treeViewer;
	private Button _btnAddTheSource;
	private AssetPackModel _selectedPack;
	private WizardNewFileCreationPage _filePage;
	private IProject _project;

	/**
	 * Create the wizard.
	 */
	public NewPage_AssetPackSection(WizardNewFileCreationPage filePage) {
		super("assetPackPage");
		_filePage = filePage;
		setTitle("Loading");
		setDescription("Add the file to an Asset Pack.");
	}

	/**
	 * Create contents of the wizard.
	 * 
	 * @param parent
	 */
	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);

		setControl(container);
		container.setLayout(new GridLayout(1, false));

		createParametersControls(container);

		_treeViewer = new TreeViewer(container);
		Tree tree = _treeViewer.getTree();
		tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		_treeViewer.setLabelProvider(AssetLabelProvider.GLOBAL_16);
		_treeViewer.setContentProvider(new TreeArrayContentProvider());

		afterCreateWidgets();

	}

	protected void createParametersControls(Composite container) {
		_btnAddTheSource = new Button(container, SWT.CHECK);
		_btnAddTheSource.setSelection(true);
		_btnAddTheSource.setText("Add the new file to a Pack file.");
		_btnAddTheSource.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> updatePageComplete()));
	}

	private void afterCreateWidgets() {
		_treeViewer.addSelectionChangedListener(e -> updatePageComplete());

		updatePageComplete();
	}

	protected void updatePageComplete() {
		_selectedPack = null;

		String error = null;

		if (isAddingToAssetPackSelected()) {
			Object elem = _treeViewer.getStructuredSelection().getFirstElement();

			if (elem == null) {
				error = "Please, select a Pack file.";
			} else {
				var pack = (AssetPackModel) elem;

				// find if there is an open and dirty editor

				List<AssetPackEditor> editors = AssetPackUIEditor.findOpenAssetPackEditors(pack.getFile());

				if (editors.stream().filter(editor -> editor.isDirty()).findFirst().isPresent()) {
					error = "The selected Pack file is open in a dirty editor.";
				}

				_selectedPack = pack;
			}
		}

		setErrorMessage(error);
		setPageComplete(error == null);
	}

	protected boolean isAddingToAssetPackSelected() {
		return _btnAddTheSource.getSelection();
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);

		_project = getProject();

		_treeViewer.setInput(AssetPackCore.getAssetPackModels(_project));
		_treeViewer.expandAll();

		updatePageComplete();
	}

	private IProject getProject() {
		return ProjectCore.getProjectFromPath(_filePage.getContainerFullPath());
	}

	public AssetPackModel getSelectedPack() {
		return _selectedPack;
	}

	public void performFinish(IProgressMonitor monitor, Consumer<AssetSectionModel> addElementsToSection) {
		if (_selectedPack != null) {

			PhaserEditorUI.swtRun(new Runnable() {

				@Override
				public void run() {
					List<AssetPackEditor> editors = AssetPackUIEditor.findOpenAssetPackEditors(_selectedPack.getFile());

					for (AssetPackEditor editor : editors) {
						AssetPackModel pack = editor.getModel();

						var section = pack.getSections().get(0);

						if (section == null) {
							section = new AssetSectionModel("section", pack);
							pack.addSection(section, true);
						}

						addElementsToSection.accept(section);

						editor.build();

						pack.setDirty(false);
					}
				}
			});

			for (AssetPackModel pack : AssetPackCore.getAssetPackModels(_project)) {

				if (pack.getSections().isEmpty()) {
					pack.addSection(new AssetSectionModel("section", pack), false);
				}
				
				AssetSectionModel section = pack.getSections().get(0);

				if (section == null) {
					section = new AssetSectionModel("section", pack);
					pack.addSection(section, true);
				}

				addElementsToSection.accept(section);

				pack.save(monitor);
			}

		}
	}

}
