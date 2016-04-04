// The MIT License (MIT)
//
// Copyright (c) 2016 Arian Fornaris
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
import java.io.InputStream;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;
import org.json.JSONObject;

import phasereditor.canvas.core.WorldModel;
import phasereditor.project.core.ProjectCore;

public class CanvasNewFilePage extends WizardNewFileCreationPage {

	public CanvasNewFilePage(IStructuredSelection selection) {
		super("newfile", selection);
		setTitle("New Canvas File");
		setDescription("Create a new Canvas file.");
	}

	@Override
	public String getFileExtension() {
		return "canvas";
	}

	/**
	 * This method is overridden to set the selected folder to WebContent folder
	 * if the current selection is outside the WebContent folder.
	 */
	@Override
	protected void initialPopulateContainerNameField() {
		super.initialPopulateContainerNameField();

		IPath fullPath = getContainerFullPath();
		if (fullPath != null && fullPath.segmentCount() > 0) {
			IProject project = ProjectCore.getProjectFromPath(fullPath);
			IPath designPath = ProjectCore.getDesignPath(project);
			setContainerFullPath(designPath);
		}
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
			String fileExt = getFileExtension();
			if (fileName.contains(".") && !fileName.endsWith("." + fileExt)) {
				setErrorMessage("File name must end in the ." + fileExt + " extension.");
				return false;
			}
			// no file extension specified so check adding default
			// extension doesn't equal a file that already exists
			if (fileName.lastIndexOf('.') == -1) {
				String newFileName = fileName + ".canvas";
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
					setErrorMessage("Canvas name exists.");
					return false;
				}
			}
		}

		return true;
	}

	@Override
	protected InputStream getInitialContents() {
		JSONObject json = new JSONObject();
		new WorldModel().write(json);
		String content = json.toString(4);
		return new ByteArrayInputStream(content.getBytes());
	}
}
