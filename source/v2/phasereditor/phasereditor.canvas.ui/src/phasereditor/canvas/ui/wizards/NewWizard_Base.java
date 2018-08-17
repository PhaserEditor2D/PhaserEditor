// The MIT License (MIT)
//
// Copyright (c) 2015, 2017 Arian Fornaris
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
package phasereditor.canvas.ui.wizards;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.json.JSONException;
import org.json.JSONObject;

import phasereditor.assetpack.core.AssetSectionModel;
import phasereditor.assetpack.core.ScriptAssetModel;
import phasereditor.assetpack.ui.wizards.NewPage_AssetPackSection;
import phasereditor.canvas.core.CanvasCore;
import phasereditor.canvas.core.CanvasModel;
import phasereditor.canvas.core.CanvasType;
import phasereditor.canvas.core.EditorSettings;
import phasereditor.canvas.core.codegen.CanvasCodeGeneratorProvider;
import phasereditor.canvas.ui.CanvasUI;
import phasereditor.canvas.ui.prefs.CodeGeneratorPreferencesPage;
import phasereditor.lic.LicCore;
import phasereditor.project.core.ProjectCore;
import phasereditor.project.core.codegen.ICodeGenerator;
import phasereditor.project.core.codegen.SourceLang;
import phasereditor.ui.PhaserEditorUI;

/**
 * @author arian
 *
 */
public abstract class NewWizard_Base extends Wizard implements INewWizard {

	private IStructuredSelection _selection;
	private CanvasModel _model;
	private NewPage_File _filePage;
	private IWorkbenchPage _windowPage;
	private CanvasType _canvasType;
	NewPage_AssetPackSection _assetPackPage;
	IFile _createdCanvasFile;

	public NewWizard_Base(CanvasType canvasType) {
		super();
		_canvasType = canvasType;
	}

	@Override
	public void setContainer(IWizardContainer container) {
		super.setContainer(container);
		if (container != null) {
			WizardDialog dlg = (WizardDialog) container;
			dlg.addPageChangedListener(new IPageChangedListener() {

				@Override
				public void pageChanged(PageChangedEvent event) {
					if (event.getSelectedPage() != getFilePage()) {
						setLangFromProjectType();
					}
				}
			});
		}
	}

	public CanvasType getCanvasType() {
		return _canvasType;
	}

	public NewPage_File getFilePage() {
		return _filePage;
	}

	public CanvasModel getModel() {
		return _model;
	}

	public IStructuredSelection getSelection() {
		return _selection;
	}

	public IWorkbenchPage getWindowPage() {
		return _windowPage;
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		_selection = selection;
		_windowPage = workbench.getActiveWorkbenchWindow().getActivePage();
	}

	@Override
	public final void addPages() {
		_model = createInitialModel();
		_model.setType(_canvasType);

		_filePage = createNewFilePage();
		_filePage.setModel(_model);

		addPage(_filePage);

		_assetPackPage = new NewPage_AssetPackSection(_filePage);

		addPage(_assetPackPage);

		addExtraPages();
	}

	protected abstract void addExtraPages();

	public NewPage_AssetPackSection getAssetPackPage() {
		return _assetPackPage;
	}

	protected CanvasModel createInitialModel() {
		CanvasModel model = new CanvasModel(null);
		model.setType(getCanvasType());
		model.getSettings().setBaseClass(CanvasCodeGeneratorProvider.getDefaultBaseClassFor(model.getType()));

		CodeGeneratorPreferencesPage.update_Settings_from_Store(CanvasUI.getPreferenceStore(),
				model.getSettings().getUserCode(), getCanvasType());

		return model;
	}

	protected NewPage_File createNewFilePage() {
		return new NewPage_File(_selection, "Create New File", "Create a new file.") {
			@Override
			public String getFileExtension() {
				return isCanvasFileDesired() ? "canvas" : (getModel().getSettings().getLang().getExtension());
			}
		};
	}

	@SuppressWarnings("static-method")
	protected boolean isCanvasFileDesired() {
		return true;
	}

	@Override
	public boolean performFinish() {

		// no file extension specified so add default extension
		String fileName = _filePage.getFileName();
		if (fileName.lastIndexOf('.') == -1) {
			String newFileName = fileName + "." + _filePage.getFileExtension();
			_filePage.setFileName(newFileName);
		}

		// create a new empty file
		_createdCanvasFile = _filePage.createNewFile();

		// if there was problem with creating file, it will be null, so make
		// sure to check
		if (_createdCanvasFile != null) {

			// check for free version

			IProject project = _createdCanvasFile.getProject();
			if (LicCore.isEvaluationProduct()) {
				String rule = CanvasCore.isFreeVersionAllowed(project);
				if (rule != null) {
					LicCore.launchGoPremiumDialogs(rule);
					return false;
				}
			}

			// --

			WorkspaceJob job = new WorkspaceJob("Creating the new Canvas file") {

				@Override
				public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {

					setLangFromProjectType();

					if (isCanvasFileDesired()) {
						createCanvasFile(_createdCanvasFile, monitor);
					} else {
						createFinalModelJSON(_createdCanvasFile);
					}

					{
						createSourceFile(_createdCanvasFile.getParent(), monitor);

						_assetPackPage.performFinish(monitor, NewWizard_Base.this::addScriptAssetToPack);
					}

					PhaserEditorUI.swtRun(new Runnable() {

						@Override
						public void run() {
							// open the file in editor
							try {
								IDE.openEditor(getWindowPage(), _createdCanvasFile);
							} catch (PartInitException e) {
								throw new RuntimeException(e);
							}
						}
					});

					return Status.OK_STATUS;
				}
			};
			job.schedule();
		}

		return true;
	}

	void addScriptAssetToPack(AssetSectionModel section) {
		IProject project = _createdCanvasFile.getProject();

		var pack = section.getPack();

		ScriptAssetModel asset = new ScriptAssetModel(pack.createKey(_createdCanvasFile), section);

		IPath jsFilePath = _createdCanvasFile.getFullPath().removeFileExtension().addFileExtension("js");
		String url = ProjectCore.getAssetUrl(project, jsFilePath);
		asset.setUrl(url);

		section.addAsset(asset, false);
	}

	void createCanvasFile(IFile file, IProgressMonitor monitor) {
		JSONObject obj = createFinalModelJSON(file);

		try {
			file.setContents(new ByteArrayInputStream(obj.toString(2).getBytes()), false, false, monitor);
		} catch (JSONException | CoreException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	protected JSONObject createFinalModelJSON(IFile file) {
		// set default content
		_model.setFile(file);
		_model.getSettings().setClassName(CanvasCore.getDefaultClassName(file));
		_model.getWorld().setEditorName(_model.getSettings().getClassName());
		JSONObject obj = new JSONObject();
		_model.write(obj, true);
		return obj;
	}

	IFile createSourceFile(IContainer parent, IProgressMonitor monitor) {
		try {

			EditorSettings settings = _model.getSettings();
			String fname = settings.getClassName() + "." + settings.getLang().getExtension();
			IFile srcFile = parent.getFile(new Path(fname));
			String replace = null;

			if (srcFile.exists()) {
				byte[] bytes = Files.readAllBytes(srcFile.getLocation().makeAbsolute().toFile().toPath());
				replace = new String(bytes);
			}

			ICodeGenerator generator = new CanvasCodeGeneratorProvider().getCodeGenerator(_model);
			String content = generator.generate(replace);

			ByteArrayInputStream stream = new ByteArrayInputStream(content.getBytes());
			if (srcFile.exists()) {
				srcFile.setContents(stream, IResource.NONE, monitor);
			} else {
				srcFile.create(stream, false, monitor);
			}
			srcFile.refreshLocal(1, monitor);

			return srcFile;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private boolean _setLangFromProjectType_set = false;

	void setLangFromProjectType() {
		if (_setLangFromProjectType_set) {
			return;
		}

		_setLangFromProjectType_set = true;

		IProject project = getProject();

		if (ProjectCore.isTypeScriptProject(project)) {
			_model.getSettings().setLang(SourceLang.TYPE_SCRIPT);
		}
	}

	public IProject getProject() {
		IPath path = _filePage.getContainerFullPath();
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IContainer folder;
		if (path.segmentCount() == 1) {
			folder = root.getProject(path.lastSegment());
		} else {
			folder = root.getFolder(path);
		}

		IProject project = folder.getProject();
		return project;
	}
}
