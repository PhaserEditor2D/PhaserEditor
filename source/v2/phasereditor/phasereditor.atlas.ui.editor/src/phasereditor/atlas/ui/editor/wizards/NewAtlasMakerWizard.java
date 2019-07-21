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
package phasereditor.atlas.ui.editor.wizards;

import static phasereditor.ui.PhaserEditorUI.swtRun;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;

import phasereditor.assetpack.core.MultiAtlasAssetModel;
import phasereditor.assetpack.ui.editor.wizards.NewPage_AssetPackSection;
import phasereditor.project.core.ProjectCore;

public class NewAtlasMakerWizard extends Wizard implements INewWizard {
	private IStructuredSelection _selection;
	private AtlasMakerWizardPage _filePage;
	private IWorkbenchPage _windowPage;
	private NewPage_AssetPackSection _assetPackPage;

	public NewAtlasMakerWizard() {
		setWindowTitle("New Atlas Generator File");
	}

	@Override
	public void addPages() {
		_filePage = new AtlasMakerWizardPage(_selection, "New Atlas Generator File",
				"Create a new Atlas generator file.");
		_assetPackPage = new NewPage_AssetPackSection(_filePage);

		addPage(_filePage);
		addPage(_assetPackPage);
	}

	public AtlasMakerWizardPage getFilePage() {
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

	public IWorkbenchPage getWindowPage() {
		return _windowPage;
	}

	@Override
	public boolean performFinish() {

		// no file extension specified so add default extension
		String fileName = _filePage.getFileName();
		if (fileName.lastIndexOf('.') == -1) {
			String newFileName = fileName + ".atlas";
			_filePage.setFileName(newFileName);
		}

		IFile file = getFilePage().createNewFile();

		if (file == null) {
			return false;
		}

		var job = new WorkspaceJob("Creating Texture Packer file.") {

			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {

				// create a new empty file

				// if there was problem with creating file, it will be null, so make
				// sure to check

				getAssetPackPage().performFinish(monitor, section -> {
					var pack = section.getPack();

					var asset = new MultiAtlasAssetModel(pack.createKey(file), section);

					var jsonFile = file.getProject()
							.getFile(file.getProjectRelativePath().removeFileExtension().addFileExtension("json"));

					asset.setKey(pack.createKey(jsonFile));
					asset.setUrl(ProjectCore.getAssetUrl(jsonFile));
					asset.setPath(ProjectCore.getAssetUrl(jsonFile.getProject(), file.getParent().getFullPath()));

					section.addAsset(asset);
				});

				swtRun(() -> {
					// open the file in editor
					try {
						IDE.openEditor(getWindowPage(), file);
					} catch (PartInitException e) {
						throw new RuntimeException(e);
					}
				});

				return Status.OK_STATUS;
			}

		};

		job.schedule();

		return true;
	}
}
