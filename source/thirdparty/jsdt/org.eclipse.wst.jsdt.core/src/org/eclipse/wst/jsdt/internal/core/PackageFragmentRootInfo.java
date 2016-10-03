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

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.core.util.Util;

/**
 * The element info for <code>PackageFragmentRoot</code>s.
 */
class PackageFragmentRootInfo extends OpenableElementInfo {

	/**
	 * The SourceMapper for this JAR (or <code>null</code> if
	 * this JAR does not have source attached).
	 */
	protected SourceMapper sourceMapper = null;

	/**
	 * The kind of the root associated with this info.
	 * Valid kinds are: <ul>
	 * <li><code>IPackageFragmentRoot.K_SOURCE</code>
	 * <li><code>IPackageFragmentRoot.K_BINARY</code></ul>
	 */
	protected int fRootKind= IPackageFragmentRoot.K_SOURCE;

	/**
	 * A array with all the non-java resources contained by this PackageFragment
	 */
	protected Object[] fNonJavaResources;
/**
 * Create and initialize a new instance of the receiver
 */
public PackageFragmentRootInfo() {
	this.fNonJavaResources = null;
}
/**
 * Starting at this folder, create non-java resources for this package fragment root
 * and add them to the non-java resources collection.
 *
 * @exception JavaScriptModelException  The resource associated with this package fragment does not exist
 */
static Object[] computeFolderNonJavaResources(JavaProject project, IContainer folder, char[][] inclusionPatterns, char[][] exclusionPatterns) throws JavaScriptModelException {
	Object[] nonJavaResources = new IResource[5];
	int nonJavaResourcesCounter = 0;
	try {
		IIncludePathEntry[] classpath = project.getResolvedClasspath();
		IResource[] members = folder.members();
		int length = members.length;
		if (length > 0) {
			String sourceLevel = project.getOption(JavaScriptCore.COMPILER_SOURCE, true);
			String complianceLevel = project.getOption(JavaScriptCore.COMPILER_COMPLIANCE, true);
			nextResource: for (int i = 0; i < length; i++) {
				IResource member = members[i];
				switch (member.getType()) {
					case IResource.FILE :
						String fileName = member.getName();

						// ignore .js files that are not excluded
						if (Util.isValidCompilationUnitName(fileName, sourceLevel, complianceLevel) && !Util.isExcluded(member, inclusionPatterns, exclusionPatterns))
							continue nextResource;
						// ignore .class files
						if (Util.isValidClassFileName(fileName, sourceLevel, complianceLevel))
							continue nextResource;
						// ignore .zip or .jar file on classpath
						if (org.eclipse.wst.jsdt.internal.compiler.util.Util.isArchiveFileName(fileName) && isClasspathEntry(member.getFullPath(), classpath))
							continue nextResource;
						break;

					case IResource.FOLDER :
						// ignore valid packages or excluded folders that correspond to a nested pkg fragment root
						if (Util.isValidFolderNameForPackage(member.getName(), sourceLevel, complianceLevel)
								&& (!Util.isExcluded(member, inclusionPatterns, exclusionPatterns)
										|| isClasspathEntry(member.getFullPath(), classpath)))
							continue nextResource;
						break;
				}
				if (nonJavaResources.length == nonJavaResourcesCounter) {
					// resize
					System.arraycopy(nonJavaResources, 0, (nonJavaResources = new IResource[nonJavaResourcesCounter * 2]), 0, nonJavaResourcesCounter);
				}
				nonJavaResources[nonJavaResourcesCounter++] = member;
			}
		}
		if (nonJavaResources.length != nonJavaResourcesCounter) {
			System.arraycopy(nonJavaResources, 0, (nonJavaResources = new IResource[nonJavaResourcesCounter]), 0, nonJavaResourcesCounter);
		}
		return nonJavaResources;
	} catch (CoreException e) {
		throw new JavaScriptModelException(e);
	}
}
/**
 * Compute the non-package resources of this package fragment root.
 */
private Object[] computeNonJavaResources(IJavaScriptProject project, IResource underlyingResource, PackageFragmentRoot handle) {
	Object[] nonJavaResources = NO_NON_JAVA_RESOURCES;
	try {
		// the underlying resource may be a folder or a project (in the case that the project folder
		// is actually the package fragment root)
		if (underlyingResource!=null && (underlyingResource.getType() == IResource.FOLDER || underlyingResource.getType() == IResource.PROJECT)) {
			nonJavaResources =
				computeFolderNonJavaResources(
					(JavaProject)project,
					(IContainer) underlyingResource,
					handle.fullInclusionPatternChars(),
					handle.fullExclusionPatternChars());
		}
	} catch (JavaScriptModelException e) {
		// ignore
	}
	return nonJavaResources;
}
/**
 * Returns an array of non-java resources contained in the receiver.
 */
synchronized Object[] getNonJavaResources(IJavaScriptProject project, IResource underlyingResource, PackageFragmentRoot handle) {
	Object[] nonJavaResources = this.fNonJavaResources;
	if (nonJavaResources == null) {
		nonJavaResources = this.computeNonJavaResources(project, underlyingResource, handle);
		this.fNonJavaResources = nonJavaResources;
	}
	return nonJavaResources;
}
/**
 * Returns the kind of this root.
 */
public int getRootKind() {
	return this.fRootKind;
}
/**
 * Retuns the SourceMapper for this root, or <code>null</code>
 * if this root does not have attached source.
 */
protected SourceMapper getSourceMapper() {
	return this.sourceMapper;
}
private static boolean isClasspathEntry(IPath path, IIncludePathEntry[] resolvedClasspath) {
	for (int i = 0, length = resolvedClasspath.length; i < length; i++) {
		IIncludePathEntry entry = resolvedClasspath[i];
		if (entry.getPath().equals(path)) {
			return true;
		}
	}
	return false;
}
/**
 * Set the fNonJavaResources to res value
 */
void setNonJavaResources(Object[] resources) {
	this.fNonJavaResources = resources;
}
/**
 * Sets the kind of this root.
 */
protected void setRootKind(int newRootKind) {
	this.fRootKind = newRootKind;
}
/**
 * Sets the SourceMapper for this root.
 */
protected void setSourceMapper(SourceMapper mapper) {
	this.sourceMapper= mapper;
}
}
