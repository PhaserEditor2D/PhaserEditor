/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.core;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.AssertionFailedException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptModelStatus;
import org.eclipse.wst.jsdt.core.IJavaScriptModelStatusConstants;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.core.util.Messages;

public class CopyPackageFragmentRootOperation extends JavaModelOperation {
	IPath destination;
	int updateResourceFlags;
	int updateModelFlags;
	IIncludePathEntry sibling;

	public CopyPackageFragmentRootOperation(
		IPackageFragmentRoot root,
		IPath destination,
		int updateResourceFlags,
		int updateModelFlags,
		IIncludePathEntry sibling) {

		super(root);
		this.destination = destination;
		this.updateResourceFlags = updateResourceFlags;
		this.updateModelFlags = updateModelFlags;
		this.sibling = sibling;
	}
	protected void executeOperation() throws JavaScriptModelException {

		IPackageFragmentRoot root = (IPackageFragmentRoot)this.getElementToProcess();
		IIncludePathEntry rootEntry = root.getRawIncludepathEntry();
		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();

		// copy resource
		if (!root.isExternal() && (this.updateModelFlags & IPackageFragmentRoot.NO_RESOURCE_MODIFICATION) == 0) {
			copyResource(root, rootEntry, workspaceRoot);
		}

		// update classpath if needed
		if ((this.updateModelFlags & IPackageFragmentRoot.DESTINATION_PROJECT_INCLUDEPATH) != 0) {
			addEntryToClasspath(rootEntry, workspaceRoot);
		}
	}
	protected void copyResource(
		IPackageFragmentRoot root,
		IIncludePathEntry rootEntry,
		final IWorkspaceRoot workspaceRoot)
		throws JavaScriptModelException {
		final char[][] exclusionPatterns = ((ClasspathEntry)rootEntry).fullExclusionPatternChars();
		IResource rootResource = root.getResource();
		if (root.getKind() == IPackageFragmentRoot.K_BINARY || exclusionPatterns == null) {
			try {
				IResource destRes;
				if ((this.updateModelFlags & IPackageFragmentRoot.REPLACE) != 0) {
					if (rootEntry.getPath().equals(this.destination)) return;
					if ((destRes = workspaceRoot.findMember(this.destination)) != null) {
						destRes.delete(this.updateResourceFlags, progressMonitor);
					}
				}
				rootResource.copy(this.destination, this.updateResourceFlags, progressMonitor);
			} catch (CoreException e) {
				throw new JavaScriptModelException(e);
			}
		} else {
			final int sourceSegmentCount = rootEntry.getPath().segmentCount();
			final IFolder destFolder = workspaceRoot.getFolder(this.destination);
			final IPath[] nestedFolders = getNestedFolders(root);
			IResourceProxyVisitor visitor = new IResourceProxyVisitor() {
				public boolean visit(IResourceProxy proxy) throws CoreException {
					if (proxy.getType() == IResource.FOLDER) {
						IPath path = proxy.requestFullPath();
						if (prefixesOneOf(path, nestedFolders)) {
							if (equalsOneOf(path, nestedFolders)) {
								// nested source folder
								return false;
							} else {
								// folder containing nested source folder
								IFolder folder = destFolder.getFolder(path.removeFirstSegments(sourceSegmentCount));
								if ((updateModelFlags & IPackageFragmentRoot.REPLACE) != 0
										&& folder.exists()) {
									return true;
								}
								folder.create(updateResourceFlags, true, progressMonitor);
								return true;
							}
						} else {
							// subtree doesn't contain any nested source folders
							IPath destPath = destination.append(path.removeFirstSegments(sourceSegmentCount));
							IResource destRes;
							if ((updateModelFlags & IPackageFragmentRoot.REPLACE) != 0
									&& (destRes = workspaceRoot.findMember(destPath)) != null) {
								destRes.delete(updateResourceFlags, progressMonitor);
							}
							proxy.requestResource().copy(destPath, updateResourceFlags, progressMonitor);
							return false;
						}
					} else {
						IPath path = proxy.requestFullPath();
						IPath destPath = destination.append(path.removeFirstSegments(sourceSegmentCount));
						IResource destRes;
						if ((updateModelFlags & IPackageFragmentRoot.REPLACE) != 0
								&& (destRes = workspaceRoot.findMember(destPath)) != null) {
							destRes.delete(updateResourceFlags, progressMonitor);
						}
						proxy.requestResource().copy(destPath, updateResourceFlags, progressMonitor);
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
	protected void addEntryToClasspath(IIncludePathEntry rootEntry, IWorkspaceRoot workspaceRoot) throws JavaScriptModelException {

		IProject destProject = workspaceRoot.getProject(this.destination.segment(0));
		IJavaScriptProject jProject = JavaScriptCore.create(destProject);
		IIncludePathEntry[] classpath = jProject.getRawIncludepath();
		int length = classpath.length;
		IIncludePathEntry[] newClasspath;

		// case of existing entry and REPLACE was specified
		if ((this.updateModelFlags & IPackageFragmentRoot.REPLACE) != 0) {
			// find existing entry
			for (int i = 0; i < length; i++) {
				if (this.destination.equals(classpath[i].getPath())) {
					newClasspath = new IIncludePathEntry[length];
					System.arraycopy(classpath, 0, newClasspath, 0, length);
					newClasspath[i] = copy(rootEntry);
					jProject.setRawIncludepath(newClasspath, progressMonitor);
					return;
				}
			}
		}

		// other cases
		int position;
		if (this.sibling == null) {
			// insert at the end
			position = length;
		} else {
			// insert before sibling
			position = -1;
			for (int i = 0; i < length; i++) {
				if (this.sibling.equals(classpath[i])) {
					position = i;
					break;
				}
			}
		}
		if (position == -1) {
			throw new JavaScriptModelException(new JavaModelStatus(IJavaScriptModelStatusConstants.INVALID_SIBLING, this.sibling.toString()));
		}
		newClasspath = new IIncludePathEntry[length+1];
		if (position != 0) {
			System.arraycopy(classpath, 0, newClasspath, 0, position);
		}
		if (position != length) {
			System.arraycopy(classpath, position, newClasspath, position+1, length-position);
		}
		IIncludePathEntry newEntry = copy(rootEntry);
		newClasspath[position] = newEntry;
		jProject.setRawIncludepath(newClasspath, progressMonitor);
	}
	/*
	 * Copies the given classpath entry replacing its path with the destination path
	 * if it is a source folder or a library.
	 */
	protected IIncludePathEntry copy(IIncludePathEntry entry) throws JavaScriptModelException {
		switch (entry.getEntryKind()) {
			case IIncludePathEntry.CPE_CONTAINER:
				return JavaScriptCore.newContainerEntry(entry.getPath(), entry.getAccessRules(), entry.getExtraAttributes(), entry.isExported());
			case IIncludePathEntry.CPE_LIBRARY:
				try {
					return JavaScriptCore.newLibraryEntry(this.destination, entry.getSourceAttachmentPath(), entry.getSourceAttachmentRootPath(), entry.getAccessRules(), entry.getExtraAttributes(), entry.isExported());
				} catch (AssertionFailedException e) {
					IJavaScriptModelStatus status = new JavaModelStatus(IJavaScriptModelStatusConstants.INVALID_PATH, e.getMessage());
					throw new JavaScriptModelException(status);
				}
			case IIncludePathEntry.CPE_PROJECT:
				return JavaScriptCore.newProjectEntry(entry.getPath(), entry.getAccessRules(), entry.combineAccessRules(), entry.getExtraAttributes(), entry.isExported());
			case IIncludePathEntry.CPE_SOURCE:
				return JavaScriptCore.newSourceEntry(this.destination, entry.getInclusionPatterns(), entry.getExclusionPatterns(), null, entry.getExtraAttributes());
			case IIncludePathEntry.CPE_VARIABLE:
				try {
					return JavaScriptCore.newVariableEntry(entry.getPath(), entry.getSourceAttachmentPath(), entry.getSourceAttachmentRootPath(), entry.getAccessRules(), entry.getExtraAttributes(), entry.isExported());
				} catch (AssertionFailedException e) {
					IJavaScriptModelStatus status = new JavaModelStatus(IJavaScriptModelStatusConstants.INVALID_PATH, e.getMessage());
					throw new JavaScriptModelException(status);
				}
			default:
				throw new JavaScriptModelException(new JavaModelStatus(IJavaScriptModelStatusConstants.ELEMENT_DOES_NOT_EXIST, this.getElementToProcess()));
		}
	}
	public IJavaScriptModelStatus verify() {
		IJavaScriptModelStatus status = super.verify();
		if (!status.isOK()) {
			return status;
		}
		IPackageFragmentRoot root = (IPackageFragmentRoot)getElementToProcess();
		if (root == null || !root.exists()) {
			return new JavaModelStatus(IJavaScriptModelStatusConstants.ELEMENT_DOES_NOT_EXIST, root);
		}

		IResource resource = root.getResource();
		if (resource instanceof IFolder) {
			if (resource.isLinked()) {
				return new JavaModelStatus(IJavaScriptModelStatusConstants.INVALID_RESOURCE, root);
			}
		}

		if ((this.updateModelFlags & IPackageFragmentRoot.DESTINATION_PROJECT_INCLUDEPATH) != 0) {
			String destProjectName = this.destination.segment(0);
			IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(destProjectName);
			if (JavaProject.hasJavaNature(project)) {
				try {
					IJavaScriptProject destProject = JavaScriptCore.create(project);
					IIncludePathEntry[] destClasspath = destProject.getRawIncludepath();
					boolean foundSibling = false;
					boolean foundExistingEntry = false;
					for (int i = 0, length = destClasspath.length; i < length; i++) {
						IIncludePathEntry entry = destClasspath[i];
						if (entry.equals(this.sibling)) {
							foundSibling = true;
							break;
						}
						if (entry.getPath().equals(this.destination)) {
							foundExistingEntry = true;
						}
					}
					if (this.sibling != null && !foundSibling) {
						return new JavaModelStatus(IJavaScriptModelStatusConstants.INVALID_SIBLING, this.sibling.toString());
					}
					if (foundExistingEntry && (this.updateModelFlags & IPackageFragmentRoot.REPLACE) == 0) {
						return new JavaModelStatus(
							IJavaScriptModelStatusConstants.NAME_COLLISION,
							Messages.bind(Messages.status_nameCollision, new String[] {this.destination.toString()}));
					}
				} catch (JavaScriptModelException e) {
					return e.getJavaScriptModelStatus();
				}
			}
		}

		return JavaModelStatus.VERIFIED_OK;
	}
}
