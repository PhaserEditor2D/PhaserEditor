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
package phasereditor.project.ui.wizards;

import java.lang.reflect.InvocationTargetException;

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

import phasereditor.project.core.ProjectCore;
import phasereditor.project.core.codegen.SourceLang;

public class NewPhaserExampleProjectWizard extends Wizard implements INewWizard {

	protected WizardNewProjectCreationPage _projectPage;
	protected PhaserTemplateWizardPage _templPage;

	public NewPhaserExampleProjectWizard() {
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

		_templPage = new PhaserTemplateWizardPage();
		addPage(_templPage);
		addPage(_projectPage);
	}

	@Override
	public void createPageControls(Composite pageContainer) {
		super.createPageControls(pageContainer);
		_templPage.setFocus();
	}

	@Override
	public boolean performFinish() {
		IProject project = _projectPage.getProjectHandle();
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

						ProjectCore.configureNewPhaserProject(project, _templPage.getTemplate(), null, SourceLang.JAVA_SCRIPT);
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
