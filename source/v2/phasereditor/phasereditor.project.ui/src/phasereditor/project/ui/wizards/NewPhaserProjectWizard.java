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
package phasereditor.project.ui.wizards;

import java.util.HashMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;
import org.json.JSONObject;

import phasereditor.inspect.core.InspectCore;
import phasereditor.inspect.core.templates.TemplateModel;
import phasereditor.project.core.ProjectCore;
import phasereditor.project.core.codegen.SourceLang;

/**
 * @author arian
 *
 */
public class NewPhaserProjectWizard extends Wizard implements INewWizard {
	protected WizardNewProjectCreationPage _projectPage;
	protected NewPhaserProjectSettingsWizardPage _settingsPage;
	private IStructuredSelection _selection;
	private IWorkbench _workbench;

	public NewPhaserProjectWizard() {
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		_workbench = workbench;
		_selection = selection;
	}

	public IStructuredSelection getSelection() {
		return _selection;
	}

	public IWorkbench getWorkbench() {
		return _workbench;
	}

	@Override
	public void addPages() {
		_projectPage = new WizardNewProjectCreationPage("project") {
			@Override
			public void createControl(Composite parent) {
				super.createControl(parent);

				createWorkingSetGroup((Composite) getControl(),

						getSelection(),

						new String[] { "org.eclipse.ui.resourceWorkingSetPage" });

				Dialog.applyDialogFont(getControl());
			}
		};
		_projectPage.setTitle("New Phaser Project");
		_projectPage.setDescription("Set the project name.");

		{ // set initial name
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			int i = 1;
			while (root.getProject("Game" + i).exists()) {
				i++;
			}
			_projectPage.setInitialProjectName("Game" + i);
		}

		_settingsPage = new NewPhaserProjectSettingsWizardPage();
		addPage(_projectPage);
		addPage(_settingsPage);
	}

	@Override
	public void createPageControls(Composite pageContainer) {
		super.createPageControls(pageContainer);
		_settingsPage.setFocus();
	}

	@Override
	public boolean performFinish() {
		var params = new HashMap<String, String>();

		IProject project = _projectPage.getProjectHandle();

		int width = Integer.parseInt(_settingsPage.getWidthText().getText());
		int height = Integer.parseInt(_settingsPage.getHeightText().getText());

		params.put("title", project.getName());

		var config = new JSONObject();

		config.put("title", project.getName());
		config.put("width", width);
		config.put("height", height);
		config.put("type", "#!@-" + _settingsPage.getTypeCombo().getText() + "#!@-");
		config.put("backgroundColor", "#88F");
		config.put("parent", "game-container");

		boolean pixelArt = _settingsPage.getPixelArtBtn().getSelection();
		if (pixelArt) {
			var renderConfig = new JSONObject();
			renderConfig.put("pixelArt", pixelArt);

			config.put("render", renderConfig);
		}

		{
			var physicsConfig = new JSONObject();
			var physics = _settingsPage.getPhysics();

			if (physics != null) {
				physicsConfig.put("default", physics);
				config.put("physics", physicsConfig);
			}
		}

		{
			var scaleConfig = new JSONObject();

			var scaleMode = _settingsPage.getScaleMode();
			if (scaleMode != null) {
				scaleConfig.put("mode", "#!@-" + scaleMode + "#!@-");
			}

			var scaleAutoCenter = _settingsPage.getScaleAutoCenter();
			if (scaleAutoCenter != null) {
				scaleConfig.put("autoCenter", "#!@-" + scaleAutoCenter + "#!@-");
			}

			if (!scaleConfig.isEmpty()) {
				config.put("scale", scaleConfig);
			}
		}

		{
			String str = config.toString(4);
			str = str.replace("\"#!@-", "").replace("#!@-\"", "");
			str = str.substring(0, str.length() - 2) + "\n\t}";
			params.put("config", str);
		}

		SourceLang lang = _settingsPage.getSourceLang();

		IWorkingSet[] workingSets = _projectPage.getSelectedWorkingSets();

		new WorkspaceJob("Creating Phaser Project") {

			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {

				monitor.beginTask("Creating project", 4);
				project.create(monitor);
				monitor.worked(1);

				project.open(monitor);
				ProjectCore.setProjectSceneSize(project, width, height);
				monitor.worked(1);

				TemplateModel template;

				var templId = "phasereditor.project.simplest";

				if (lang == SourceLang.JAVA_SCRIPT_6) {
					templId += ".js6";
				} else if (lang == SourceLang.TYPE_SCRIPT) {
					templId += ".typescript";
				}

				var workbench = getWorkbench();

				template = InspectCore.getProjectTemplates().findById(templId);

				ProjectCore.configureNewPhaserProject(project, template, params, lang, monitor);
				monitor.worked(1);

				workbench.getWorkingSetManager().addToWorkingSets(project, workingSets);
				monitor.worked(1);

				return Status.OK_STATUS;
			}
		}.schedule();

		return true;
	}

	@Override
	public boolean needsProgressMonitor() {
		return true;
	}
}
