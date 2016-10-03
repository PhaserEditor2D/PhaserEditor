/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.wizards;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;
import org.eclipse.wst.jsdt.core.IBuffer;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.ui.CodeGeneration;

class NewJSFileWizardPage extends WizardNewFileCreationPage {
	
	private IContentType	fContentType;
	private List			fValidExtensions = null;
	
	public NewJSFileWizardPage(String pageName, IStructuredSelection selection) {
        super(pageName, selection);
    }
	
	/**
	 * This method is overridden to set the selected folder to source 
	 * folder if the current selection is outside the source folder. 
	 */
	protected void initialPopulateContainerNameField() {
		super.initialPopulateContainerNameField();
		
		IPath fullPath = getContainerFullPath();
		if (fullPath != null && fullPath.segmentCount() > 0) {
			IProject project = getProjectFromPath(fullPath);
			IPath sourcePath = getSourcePath(project);
			IPath projectPath = project.getFullPath();
			if (projectPath.equals(fullPath))
				setContainerFullPath(sourcePath);
			else
				setContainerFullPath(fullPath);
		}
		//if (webContentPath != null && !webContentPath.isPrefixOf(fullPath)) {
			//setContainerFullPath(webContentPath);
	//	}else{
			//setContainerFullPath(new Path(""));
	//	}
			
	}
	
	/**
	 * This method is overriden to set additional validation specific to 
	 * javascript files. 
	 */
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
			if (!extensionValidForContentType(fileName)) {
				setErrorMessage(NLS.bind(NewWizardMessages.Javascript_Error_Filename_Must_End_JS, getValidExtensions().toString()));
				return false;
			}
			// no file extension specified so check adding default
			// extension doesn't equal a file that already exists
			if (fileName.lastIndexOf('.') == -1) {
				String newFileName = addDefaultExtension(fileName);
				IPath resourcePath = fullPath.append(newFileName);

				IWorkspace workspace = ResourcesPlugin.getWorkspace();
				IStatus result = workspace.validatePath(resourcePath.toString(), IResource.FOLDER);
				if (!result.isOK()) {
					// path invalid
					setErrorMessage(result.getMessage());
					return false;
				}

				if ((workspace.getRoot().getFolder(resourcePath).exists() || workspace.getRoot().getFile(resourcePath).exists())) {
					setErrorMessage(NewWizardMessages.Javascript_Resource_Group_Name_Exists);
					return false;
				}
			}
			
			// get the IProject for the selection path
			IProject project = getProjectFromPath(fullPath);
			// if inside web project, check if inside webContent folder
			if (project != null && isJSProject(project)) {
				// check that the path is inside the webContent folder
				IPath sourcePath = getSourcePath(project);
				if (sourcePath == null) {
					setErrorMessage(NLS.bind(NewWizardMessages.Javascript_Error_Source_Folder_Is_Not_Configured, project.getName()));
					return false;
				}
				if (!sourcePath.isPrefixOf(fullPath)) {
					setMessage(NewWizardMessages.Javascript_Warning_Folder_Must_Be_Inside_Web_Content, WARNING);
				}
			}
		}

		return true;
	}
	
	/**
	 * Get content type associated with this new file wizard
	 * 
	 * @return IContentType
	 */
	private IContentType getContentType() {
		if (fContentType == null)
//			fContentType = Platform.getContentTypeManager().getContentType(ContentTypeIdForJavaScript.ContentTypeID_JAVASCRIPT);
			fContentType = Platform.getContentTypeManager().getContentType("org.eclipse.wst.jsdt.core.jsSource"); //$NON-NLS-1$
		return fContentType;
	}

	/**
	 * Get list of valid extensions for JavaScript Content type
	 * 
	 * @return
	 */
	private List getValidExtensions() {
		if (fValidExtensions == null) {
			IContentType type = getContentType();
			fValidExtensions = new ArrayList(Arrays.asList(type.getFileSpecs(IContentType.FILE_EXTENSION_SPEC)));
		}
		return fValidExtensions;
	}
	
	/**
	 * Verifies if fileName is valid name for content type. Takes base content
	 * type into consideration.
	 * 
	 * @param fileName
	 * @return true if extension is valid for this content type
	 */
	private boolean extensionValidForContentType(String fileName) {
		boolean valid = false;

		IContentType type = getContentType();
		// there is currently an extension
		if (fileName.lastIndexOf('.') != -1) {
			// check what content types are associated with current extension
			IContentType[] types = Platform.getContentTypeManager().findContentTypesFor(fileName);
			int i = 0;
			while (i < types.length && !valid) {
				valid = types[i].isKindOf(type);
				++i;
			}
		}
		else
			valid = true; // no extension so valid
		return valid;
	}

	/**
	 * Adds default extension to the filename
	 * 
	 * @param filename
	 * @return
	 */
	String addDefaultExtension(String filename) {
		StringBuffer newFileName = new StringBuffer(filename);

//		Preferences preference = JavaScriptCorePlugin.getDefault().getPluginPreferences();
//		String ext = preference.getString(JavaScriptCorePreferenceNames.DEFAULT_EXTENSION);

		newFileName.append("."); //$NON-NLS-1$
//		newFileName.append(ext);
newFileName.append("js"); //$NON-NLS-1$
		return newFileName.toString();
	}
	
	/**
	 * Returns the project that contains the specified path
	 * 
	 * @param path the path which project is needed
	 * @return IProject object. If path is <code>null</code> the return value 
	 * 		   is also <code>null</code>. 
	 */
	private IProject getProjectFromPath(IPath path) {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IProject project = null;
		
		if (path != null) {
			if (workspace.validatePath(path.toString(), IResource.PROJECT).isOK()) {
				project = workspace.getRoot().getProject(path.toString());
			} else {
				project = workspace.getRoot().getFile(path).getProject();
			}
		}
		
		return project;
	}
	
	/**
	 * Checks if the specified project is a web project. 
	 * 
	 * @param project project to be checked
	 * @return true if the project is web project, otherwise false
	 */
	private boolean isJSProject(IProject project) {
		try {
			return project.hasNature(JavaScriptCore.NATURE_ID);
		}
		catch (CoreException e) {
			return false;
		}
	}
	
	/**
	 * Returns the source folder of the specified project
	 * 
	 * @param project the project which source path is needed
	 * @return IPath of the source folder
	 */
	private IPath getSourcePath(IProject project) {
		IPath path = null;

		if (project != null && isJSProject(project)) {
			IJavaScriptProject p = JavaScriptCore.create(project);
			try {
				IIncludePathEntry[] includepath = p.getResolvedIncludepath(true);
				for (int i = 0; i < includepath.length; i++) {
					if (includepath[i].getEntryKind() == IIncludePathEntry.CPE_SOURCE)
						return includepath[i].getPath();
				}
			}
			catch (JavaScriptModelException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return path;
	}

	public void addFileComment(IFile file) {
		addFileComment(file, true);
	}	
	public void addFileComment(IFile file, boolean overwrite) {
		IJavaScriptUnit cu= JavaScriptCore.createCompilationUnitFrom(file);
		try {
			cu.becomeWorkingCopy(new NullProgressMonitor());
			IBuffer buffer = cu.getBuffer();
			if (overwrite || buffer.getLength() == 0) {
				buffer.setContents(CodeGeneration.getFileComment(cu, StubUtility.getLineDelimiterUsed(cu)));
				cu.commitWorkingCopy(true, new NullProgressMonitor());
			}
			cu.discardWorkingCopy();
		} catch (CoreException e) {
			JavaScriptPlugin.log(e);
		}
	}	
}
