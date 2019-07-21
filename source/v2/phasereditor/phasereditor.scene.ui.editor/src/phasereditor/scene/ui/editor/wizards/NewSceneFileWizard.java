// The MIT License (MIT)
//
// Copyright (c) 2015 Arian Fornaris
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
package phasereditor.scene.ui.editor.wizards;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;
import org.eclipse.ui.ide.IDE;

import phasereditor.assetpack.core.SceneFileAssetModel;
import phasereditor.assetpack.core.ScriptAssetModel;
import phasereditor.assetpack.ui.editor.wizards.NewPage_AssetPackSection;
import phasereditor.project.core.ProjectCore;
import phasereditor.scene.core.SceneCore;

public class NewSceneFileWizard extends Wizard implements INewWizard {

	private IStructuredSelection _selection;
	private SceneFileWizardPage _filePage;
	private IWorkbenchPage _windowPage;
	private AssetPackSectionPage _assetPackPage;

	static class AssetPackSectionPage extends NewPage_AssetPackSection {

		private Button _asSceneFileButton;
		private Button _asScriptButton;
		private Button _dontAddToPackButton;

		public AssetPackSectionPage(WizardNewFileCreationPage filePage) {
			super(filePage);
		}

		@Override
		protected void createParametersControls(Composite container) {
			// super.createParametersControls(container);

			var listener = SelectionListener.widgetSelectedAdapter(e -> updatePageComplete());
			_asSceneFileButton = new Button(container, SWT.RADIO);
			_asSceneFileButton.setText("Add the compiled file to a pack as a 'sceneFile' key.");
			_asSceneFileButton
					.setToolTipText("Select this if you want automatically add the scene to the Scene Manager.");
			_asSceneFileButton.setSelection(true);
			_asSceneFileButton.addSelectionListener(listener);

			_asScriptButton = new Button(container, SWT.RADIO);
			_asScriptButton.setText("Add the compiled file to a pack as a 'script' key.");
			_asScriptButton.setToolTipText(
					"Select this if you want to manually add the scene the Scene Manager.\nOr create a different type of object.");
			_asScriptButton.addSelectionListener(listener);

			_dontAddToPackButton = new Button(container, SWT.RADIO);
			_dontAddToPackButton.setText("Do not add the file to a pack.");
			_dontAddToPackButton.addSelectionListener(listener);
		}

		@Override
		protected boolean isAddingToAssetPackSelected() {
			return !_dontAddToPackButton.getSelection();
		}

		public Button getAsSceneFileButton() {
			return _asSceneFileButton;
		}

	}

	public NewSceneFileWizard() {
		setWindowTitle("New Scene File");
	}

	@Override
	public void addPages() {
		_filePage = new SceneFileWizardPage(_selection);
		_assetPackPage = new AssetPackSectionPage(_filePage);

		addPage(_filePage);
		addPage(_assetPackPage);
	}

	public SceneFileWizardPage getFilePage() {
		return _filePage;
	}

	public NewPage_AssetPackSection getAssetPackPage() {
		return _assetPackPage;
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		_selection = selection;
		_windowPage = workbench.getActiveWorkbenchWindow().getActivePage();
	}

	@Override
	public boolean performFinish() {

		// no file extension specified so add default extension
		String fileName = _filePage.getFileName();
		if (fileName.lastIndexOf('.') == -1) {
			String newFileName = fileName + ".scene";
			_filePage.setFileName(newFileName);
		}

		// create a new empty file
		IFile file = _filePage.createNewFile();

		// if there was problem with creating file, it will be null, so make
		// sure to check
		if (file == null) {
			return false;
		}

		try {
			getContainer().run(false, false, new IRunnableWithProgress() {

				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

					try {

						var model = getFilePage().getInitialModel();
						SceneCore.compileScene(model, file, monitor);

						getAssetPackPage().performFinish(monitor, section -> {
							var jsFile = SceneCore.getSceneSourceCodeFile(model, file);

							var pack = section.getPack();

							if (_assetPackPage.getAsSceneFileButton().getSelection()) {
								var asset = new SceneFileAssetModel(pack.createKey(jsFile), section);
								asset.setUrl(ProjectCore.getAssetUrl(jsFile));
								section.addAsset(asset);
							} else {
								var asset = new ScriptAssetModel(pack.createKey(jsFile), section);
								asset.setUrl(ProjectCore.getAssetUrl(jsFile));
								section.addAsset(asset);
							}

						});

					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
			});
		} catch (InvocationTargetException | InterruptedException e1) {
			e1.printStackTrace();
			throw new RuntimeException(e1);
		}

		// open the file in editor
		try {
			IDE.openEditor(_windowPage, file);
		} catch (PartInitException e) {
			throw new RuntimeException(e);
		}

		return true;
	}

}
