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
import phasereditor.assetpack.ui.AssetSectionsContentProvider;
import phasereditor.assetpack.ui.editor.AssetPackEditor;
import phasereditor.assetpack.ui.editor.AssetPackUIEditor;
import phasereditor.project.core.ProjectCore;
import phasereditor.ui.PhaserEditorUI;

/**
 * @author arian
 *
 */
public class NewPage_AssetPackSection extends WizardPage {
	private TreeViewer _treeViewer;
	private Button _btnAddTheSource;
	private AssetSectionModel _selectedSection;
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

		_btnAddTheSource = new Button(container, SWT.CHECK);
		_btnAddTheSource.setSelection(true);
		_btnAddTheSource.setText("Add the file to a pack section.");

		_treeViewer = new TreeViewer(container);
		Tree tree = _treeViewer.getTree();
		tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		_treeViewer.setLabelProvider(AssetLabelProvider.GLOBAL_16);
		_treeViewer.setContentProvider(new AssetSectionsContentProvider());

		afterCreateWidgets();

	}

	private void afterCreateWidgets() {
		_treeViewer.addSelectionChangedListener(e -> updatePageComplete());
		_btnAddTheSource.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> updatePageComplete()));
		updatePageComplete();
	}

	private void updatePageComplete() {
		_selectedSection = null;

		String error = null;

		if (_btnAddTheSource.getSelection()) {
			Object elem = _treeViewer.getStructuredSelection().getFirstElement();

			if (elem == null) {
				error = "Please, select an asset section.";
			} else {
				if (elem instanceof AssetSectionModel) {
					AssetSectionModel section = (AssetSectionModel) elem;

					// find if there is an open and dirty editor

					List<AssetPackEditor> editors = AssetPackUIEditor.findOpenAssetPackEditors(section.getPack().getFile());

					if (editors.stream().filter(editor -> editor.isDirty()).findFirst().isPresent()) {
						error = "The selected section is open in a dirty editor.";
					}

					_selectedSection = section;
				} else {
					error = "Please, select an asset section.";
				}
			}
		}

		setErrorMessage(error);
		setPageComplete(error == null);
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);

		_project = getProject();

		_treeViewer.setInput(_project);
		_treeViewer.expandAll();

		updatePageComplete();
	}

	private IProject getProject() {
		return ProjectCore.getProjectFromPath(_filePage.getContainerFullPath());
	}

	public AssetSectionModel getSelectedSection() {
		return _selectedSection;
	}

	public void performFinish(IProgressMonitor monitor, Consumer<AssetSectionModel> addElementsToSection) {
		AssetSectionModel addToSection = getSelectedSection();
		if (addToSection != null) {

			var sectionKey = addToSection.getKey();

			PhaserEditorUI.swtRun(new Runnable() {

				@Override
				public void run() {
					List<AssetPackEditor> editors = AssetPackUIEditor
							.findOpenAssetPackEditors(addToSection.getPack().getFile());

					for (AssetPackEditor editor : editors) {
						AssetPackModel pack = editor.getModel();

						var section = pack.findSection(sectionKey);

						if (section != null) {

							addElementsToSection.accept(section);

							editor.refresh();

							pack.setDirty(false);
						}
					}
				}
			});

			for (AssetPackModel pack : AssetPackCore.getAssetPackModels(_project)) {

				AssetSectionModel section = pack.findSection(sectionKey);

				if (section != null) {

					addElementsToSection.accept(section);

					pack.save(monitor);
				}
			}

		}
	}

}
