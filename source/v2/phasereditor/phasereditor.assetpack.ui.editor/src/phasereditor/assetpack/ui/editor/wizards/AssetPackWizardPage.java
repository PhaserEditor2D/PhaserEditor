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
package phasereditor.assetpack.ui.editor.wizards;

import static java.lang.System.currentTimeMillis;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;
import org.json.JSONObject;
import org.json.JSONTokener;

import phasereditor.inspect.core.InspectCore;
import phasereditor.project.core.ProjectCore;

public class AssetPackWizardPage extends WizardNewFileCreationPage {

	public AssetPackWizardPage(IStructuredSelection selection) {
		super("newfile", selection);
		setTitle("New Asset Pack File");
		setDescription("Create a new Asset Pack file.");
	}

	@Override
	public String getFileExtension() {
		return "json";
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

			IPath webPath = ProjectCore.getWebContentPath(project);
			
			if (webPath != null && webPath.isPrefixOf(fullPath)) {
				return;
			}
			
			IPath assetsPath;

			if (webPath == null) {
				// this can happen in non Phaser projects.
				assetsPath = project.getFullPath();
			} else {
				assetsPath = webPath.append("assets");
			}

			if (!ResourcesPlugin.getWorkspace().getRoot().getFolder(assetsPath).exists()) {
				assetsPath = webPath;
			}

			setContainerFullPath(assetsPath);
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
			if (fileName.contains(".") && !fileName.endsWith(".json")) {
				setErrorMessage("File name must end in the .json extension.");
				return false;
			}
			// no file extension specified so check adding default
			// extension doesn't equal a file that already exists
			if (fileName.lastIndexOf('.') == -1) {
				String newFileName = fileName + ".json";
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
					setErrorMessage("Asset Pack name exists.");
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
		Path file = InspectCore.getBundleFile(InspectCore.RESOURCES_TEMPLATES_PLUGIN, "templates_newfile/empty-asset-pack.json");
		try (InputStream input = Files.newInputStream(file);) {
			JSONObject obj = new JSONObject(new JSONTokener(input));
			obj.getJSONObject("meta").put("generated", Long.toString(currentTimeMillis()));
			return new ByteArrayInputStream(obj.toString().getBytes());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
