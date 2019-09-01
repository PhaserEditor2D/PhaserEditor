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

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;

import phasereditor.project.core.ProjectCore;
import phasereditor.project.core.codegen.JSClassGenerator;

/**
 * @author arian
 *
 */
public class NewJSClassFileWizardPage extends WizardNewFileCreationPage {
	public NewJSClassFileWizardPage(IStructuredSelection selection) {
		super("newfile", selection);
		setTitle("New Class File");
		setDescription("Create a new class file.");
	}

	@Override
	public String getFileExtension() {
		return "js";
	}

	/**
	 * This method is overriden to set additional validation specific to asset
	 * pack files.
	 */
	@Override
	protected boolean validatePage() {
		setMessage(null);
		setErrorMessage(null);

		if (!super.validatePage()) {
			return false;
		}

		String fileName = getFileName();
		IPath fullPath = getContainerFullPath();
		if ((fullPath != null) && (fullPath.isEmpty() == false) && (fileName != null)) {

			// check that filename does not contain invalid extension
			String ext = "." + getFileExtension();
			if (fileName.contains(".") && !fileName.endsWith(ext)) {
				setErrorMessage("File name must end in the " + ext + " extension.");
				return false;
			}
			// no file extension specified so check adding default
			// extension doesn't equal a file that already exists
			if (fileName.lastIndexOf('.') == -1) {
				String newFileName = fileName + ext;
				IPath resourcePath = fullPath.append(newFileName);

				IWorkspace workspace = ResourcesPlugin.getWorkspace();
				IStatus result = workspace.validatePath(resourcePath.toString(), IResource.FOLDER);
				if (!result.isOK()) {
					// path invalid
					setErrorMessage(result.getMessage());
					return false;
				}

				if ((workspace.getRoot().getFolder(resourcePath).exists()
						|| workspace.getRoot().getFile(resourcePath).exists())) {
					setErrorMessage("File name exists.");
					return false;
				}
			}

			// get the IProject for the selection path
			IProject project = ProjectCore.getProjectFromPath(fullPath);
			// if inside web project, check if inside webContent folder
			if (project != null && ProjectCore.isPhaserProject(project)) {
				// check that the path is inside the webContent folder
				IPath sourcePath = ProjectCore.getWebContentPath(project);
				if (!sourcePath.isPrefixOf(fullPath)) {
					setMessage("The file must be inside the WebContent folder.", WARNING);
				}
			}
		}

		return true;
	}

	@Override
	protected InputStream getInitialContents() {
		NewJSClassFileWizard wizard = (NewJSClassFileWizard) getWizard();
		String baseclass = wizard.getSettingsPage().getSuperclass().trim();
		String clsname = getFileName();
		clsname = getFileName().substring(0, clsname.length() - getFileExtension().length() - 1);
		JSClassGenerator gen = new JSClassGenerator(clsname, baseclass);
		String str = gen.generate("");
		return new ByteArrayInputStream(str.getBytes());
	}

}
