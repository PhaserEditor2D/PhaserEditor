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

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;
import org.json.JSONObject;

import phasereditor.inspect.core.InspectCore;
import phasereditor.inspect.core.templates.TemplateModel;
import phasereditor.project.core.ProjectCore;

/**
 * @author arian
 *
 */
public class NewPhaserProjectWizard extends Wizard implements INewWizard {
	protected WizardNewProjectCreationPage _projectPage;
	protected NewPhaserProjectSettingsWizardPage _settingsPage;

	public NewPhaserProjectWizard() {
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		// nothing
	}

	@Override
	public void addPages() {
		_projectPage = new WizardNewProjectCreationPage("project");
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
		IProject project = _projectPage.getProjectHandle();

		String width = _settingsPage.getWidthText().getText();
		String height = _settingsPage.getHeightText().getText();
		String renderer = _settingsPage.getRendererCombo().getText();
		boolean transparent = _settingsPage.getTransparentBtn().getSelection();
		boolean antialias = _settingsPage.getAntialiasBtn().getSelection();
		JSONObject physicsConfig = new JSONObject();

		if (_settingsPage.getArcadeBtn().getSelection()) {
			physicsConfig.put("arcade", true);
		}

		if (_settingsPage.getBox2dBtn().getSelection()) {
			physicsConfig.put("box2d", true);
		}

		if (_settingsPage.getMatterBtn().getSelection()) {
			physicsConfig.put("matter", true);
		}

		if (_settingsPage.getP2Btn().getSelection()) {
			physicsConfig.put("p2", true);
		}

		if (_settingsPage.getNinjaBtn().getSelection()) {
			physicsConfig.put("ninja", true);
		}

		boolean simplestProject = _settingsPage.getSimplestBtn().getSelection();
		boolean singleState = _settingsPage.getSingleStateBtn().getSelection();
		boolean hasAssets = _settingsPage.getIncludeAssets().getSelection();

		try {
			getContainer().run(true, false, new IRunnableWithProgress() {

				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						monitor.beginTask("Creating project", 3);
						project.create(monitor);
						monitor.worked(1);

						project.open(monitor);
						monitor.worked(2);

						TemplateModel template;
						Map<String, String> values = new HashMap<>();

						values.put("title", project.getName());
						values.put("game.width", width);
						values.put("game.height", height);
						values.put("game.renderer", renderer);

						boolean hasExtraParams = transparent || !antialias || !physicsConfig.isEmpty();

						StringBuilder gameParams = new StringBuilder();

						String templId = "";

						if (simplestProject) {
							templId = "phasereditor.project.simplest";
						} else {
							
							if (hasExtraParams) {
								gameParams.append(", '', null");
							}
							
							if (singleState) {
								templId = "phasereditor.project.singleState";
							} else {
								// multiple states
								templId = "phasereditor.project.multipleStates";
							}
						}

						if (hasExtraParams) {
							String p1 = ", " + Boolean.toString(transparent);
							String p2 = ", " + Boolean.toString(antialias);
							String p3 = ", " + physicsConfig.toString();

							if (!physicsConfig.isEmpty()) {
								gameParams.append(p1 + p2 + p3);
							} else if (!antialias) {
								gameParams.append(p1 + p2);
							} else if (transparent) {
								gameParams.append(p1);
							}
						}

						values.put("game.extra", gameParams.toString());

						if (hasAssets) {
							templId += ".assets";
						}

						template = InspectCore.getProjectTemplates().findById(templId);

						ProjectCore.configureNewPhaserProject(project, template, values);
						monitor.worked(3);

					} catch (CoreException e) {
						throw new RuntimeException(e);
					}
				}
			});
		} catch (InvocationTargetException | InterruptedException e) {
			throw new RuntimeException(e);
		}

		return true;
	}

	@Override
	public boolean needsProgressMonitor() {
		return true;
	}
}
