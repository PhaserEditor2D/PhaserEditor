/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.core;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptModel;
import org.eclipse.wst.jsdt.core.IJavaScriptModelStatus;
import org.eclipse.wst.jsdt.core.IJavaScriptModelStatusConstants;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;

public class DeletePackageFragmentRootOperation extends JavaModelOperation {

	int updateResourceFlags;
	int updateModelFlags;

	public DeletePackageFragmentRootOperation(
		IPackageFragmentRoot root,
		int updateResourceFlags,
		int updateModelFlags) {

		super(root);
		this.updateResourceFlags = updateResourceFlags;
		this.updateModelFlags = updateModelFlags;
	}

	protected void executeOperation() throws JavaScriptModelException {

		IPackageFragmentRoot root = (IPackageFragmentRoot)this.getElementToProcess();
		IIncludePathEntry rootEntry = root.getRawIncludepathEntry();

		// remember olds roots
		DeltaProcessor deltaProcessor = JavaModelManager.getJavaModelManager().getDeltaProcessor();
		if (deltaProcessor.oldRoots == null)
			deltaProcessor.oldRoots = new HashMap();

		// update classpath if needed
		if ((updateModelFlags & IPackageFragmentRoot.ORIGINATING_PROJECT_INCLUDEPATH) != 0) {
			updateProjectClasspath(rootEntry.getPath(), root.getJavaScriptProject(), deltaProcessor.oldRoots);
		}
		if ((updateModelFlags & IPackageFragmentRoot.OTHER_REFERRING_PROJECTS_INCLUDEPATH) != 0) {
			updateReferringProjectClasspaths(rootEntry.getPath(), root.getJavaScriptProject(), deltaProcessor.oldRoots);
		}

		// delete resource
		if (!root.isExternal() && (this.updateModelFlags & IPackageFragmentRoot.NO_RESOURCE_MODIFICATION) == 0) {
			deleteResource(root, rootEntry);
		}
	}

	protected void deleteResource(
		IPackageFragmentRoot root,
		IIncludePathEntry rootEntry)
		throws JavaScriptModelException {
		final char[][] exclusionPatterns = ((ClasspathEntry)rootEntry).fullExclusionPatternChars();
		IResource rootResource = root.getResource();
		if (rootEntry.getEntryKind() != IIncludePathEntry.CPE_SOURCE || exclusionPatterns == null) {
			try {
				rootResource.delete(this.updateResourceFlags, progressMonitor);
			} catch (CoreException e) {
				throw new JavaScriptModelException(e);
			}
		} else {
			final IPath[] nestedFolders = getNestedFolders(root);
			IResourceProxyVisitor visitor = new IResourceProxyVisitor() {
				public boolean visit(IResourceProxy proxy) throws CoreException {
					if (proxy.getType() == IResource.FOLDER) {
						IPath path = proxy.requestFullPath();
						if (prefixesOneOf(path, nestedFolders)) {
							// equals if nested source folder
							return !equalsOneOf(path, nestedFolders);
						} else {
							// subtree doesn't contain any nested source folders
							proxy.requestResource().delete(updateResourceFlags, progressMonitor);
							return false;
						}
					} else {
						proxy.requestResource().delete(updateResourceFlags, progressMonitor);
						return false;
					}
				}
			};
			try {
				rootResource.accept(visitor, IResource.NONE);
			} catch (CoreException e) {
				throw new JavaScriptModelException(e);
			}
		}
		setAttribute(HAS_MODIFIED_RESOURCE_ATTR, TRUE);
	}


	/*
	 * Deletes the classpath entries equals to the given rootPath from all Java projects.
	 */
	protected void updateReferringProjectClasspaths(IPath rootPath, IJavaScriptProject projectOfRoot, Map oldRoots) throws JavaScriptModelException {
		IJavaScriptModel model = this.getJavaModel();
		IJavaScriptProject[] projects = model.getJavaScriptProjects();
		for (int i = 0, length = projects.length; i < length; i++) {
			IJavaScriptProject project = projects[i];
			if (project.equals(projectOfRoot)) continue;
			updateProjectClasspath(rootPath, project, oldRoots);
		}
	}

	/*
	 * Deletes the classpath entries equals to the given rootPath from the given project.
	 */
	protected void updateProjectClasspath(IPath rootPath, IJavaScriptProject project, Map oldRoots) throws JavaScriptModelException {
		// remember old roots
		oldRoots.put(project, project.getPackageFragmentRoots());

		IIncludePathEntry[] classpath = project.getRawIncludepath();
		IIncludePathEntry[] newClasspath = null;
		int cpLength = classpath.length;
		int newCPIndex = -1;
		for (int j = 0; j < cpLength; j++) {
			IIncludePathEntry entry = classpath[j];
			if (rootPath.equals(entry.getPath())) {
				if (newClasspath == null) {
					newClasspath = new IIncludePathEntry[cpLength-1];
					System.arraycopy(classpath, 0, newClasspath, 0, j);
					newCPIndex = j;
				}
			} else if (newClasspath != null) {
				newClasspath[newCPIndex++] = entry;
			}
		}
		if (newClasspath != null) {
			if (newCPIndex < newClasspath.length) {
				System.arraycopy(newClasspath, 0, newClasspath = new IIncludePathEntry[newCPIndex], 0, newCPIndex);
			}
			project.setRawIncludepath(newClasspath, progressMonitor);
		}
	}
	protected IJavaScriptModelStatus verify() {
		IJavaScriptModelStatus status = super.verify();
		if (!status.isOK()) {
			return status;
		}
		IPackageFragmentRoot root = (IPackageFragmentRoot) this.getElementToProcess();
		if (root == null || !root.exists()) {
			return new JavaModelStatus(IJavaScriptModelStatusConstants.ELEMENT_DOES_NOT_EXIST, root);
		}

		IResource resource = root.getResource();
		if (resource instanceof IFolder) {
			if (resource.isLinked()) {
				return new JavaModelStatus(IJavaScriptModelStatusConstants.INVALID_RESOURCE, root);
			}
		}
		return JavaModelStatus.VERIFIED_OK;
	}

}
