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
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptModelStatusConstants;
import org.eclipse.wst.jsdt.core.IOpenable;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.core.util.Messages;

/**
 * This operation deletes a collection of resources and all of their children.
 * It does not delete resources which do not belong to the Java Model
 * (eg GIF files).
 */
public class DeleteResourceElementsOperation extends MultiOperation {
/**
 * When executed, this operation will delete the given elements. The elements
 * to delete cannot be <code>null</code> or empty, and must have a corresponding
 * resource.
 */
protected DeleteResourceElementsOperation(IJavaScriptElement[] elementsToProcess, boolean force) {
	super(elementsToProcess, force);
}
/**
 * Deletes the direct children of <code>frag</code> corresponding to its kind
 * (K_SOURCE or K_BINARY), and deletes the corresponding folder if it is then
 * empty.
 */
private void deletePackageFragment(IPackageFragment frag)
	throws JavaScriptModelException {
	IResource res = frag.getResource();
	if (res != null) {
		// collect the children to remove
		IJavaScriptElement[] childrenOfInterest = frag.getChildren();
		if (childrenOfInterest.length > 0) {
			IResource[] resources = new IResource[childrenOfInterest.length];
			// remove the children
			for (int i = 0; i < childrenOfInterest.length; i++) {
				resources[i] = childrenOfInterest[i].getCorrespondingResource();
			}
			deleteResources(resources, force);
		}

		// Discard non-java resources
		Object[] nonJavaResources = frag.getNonJavaScriptResources();
		int actualResourceCount = 0;
		for (int i = 0, max = nonJavaResources.length; i < max; i++){
			if (nonJavaResources[i] instanceof IResource) actualResourceCount++;
		}
		IResource[] actualNonJavaResources = new IResource[actualResourceCount];
		for (int i = 0, max = nonJavaResources.length, index = 0; i < max; i++){
			if (nonJavaResources[i] instanceof IResource) actualNonJavaResources[index++] = (IResource)nonJavaResources[i];
		}
		deleteResources(actualNonJavaResources, force);

		// delete remaining files in this package (.class file in the case where Proj=src=bin)
		IResource[] remainingFiles;
		try {
			remainingFiles = ((IContainer) res).members();
		} catch (CoreException ce) {
			throw new JavaScriptModelException(ce);
		}
		boolean isEmpty = true;
		for (int i = 0, length = remainingFiles.length; i < length; i++) {
			IResource file = remainingFiles[i];
			if (file instanceof IFile && org.eclipse.wst.jsdt.internal.compiler.util.Util.isClassFileName(file.getName())) {
				this.deleteResource(file, IResource.FORCE | IResource.KEEP_HISTORY);
			} else {
				isEmpty = false;
			}
		}
		if (isEmpty && !frag.isDefaultPackage()/*don't delete default package's folder: see https://bugs.eclipse.org/bugs/show_bug.cgi?id=38450*/) {
			// delete recursively empty folders
			IResource fragResource =  frag.getResource();
			if (fragResource != null) {
				deleteEmptyPackageFragment(frag, false, fragResource.getParent());
			}
		}
	}
}
/**
 * @see MultiOperation
 */
protected String getMainTaskName() {
	return Messages.operation_deleteResourceProgress;
}
/**
 * @see MultiOperation This method delegate to <code>deleteResource</code> or
 * <code>deletePackageFragment</code> depending on the type of <code>element</code>.
 */
protected void processElement(IJavaScriptElement element) throws JavaScriptModelException {
	switch (element.getElementType()) {
		case IJavaScriptElement.CLASS_FILE :
		case IJavaScriptElement.JAVASCRIPT_UNIT :
			deleteResource(element.getResource(), force ? IResource.FORCE | IResource.KEEP_HISTORY : IResource.KEEP_HISTORY);
			break;
		case IJavaScriptElement.PACKAGE_FRAGMENT :
			deletePackageFragment((IPackageFragment) element);
			break;
		default :
			throw new JavaScriptModelException(new JavaModelStatus(IJavaScriptModelStatusConstants.INVALID_ELEMENT_TYPES, element));
	}
	// ensure the element is closed
	if (element instanceof IOpenable) {
		((IOpenable)element).close();
	}
}
/**
 * @see MultiOperation
 */
protected void verify(IJavaScriptElement element) throws JavaScriptModelException {
	if (element == null || !element.exists())
		error(IJavaScriptModelStatusConstants.ELEMENT_DOES_NOT_EXIST, element);

	int type = element.getElementType();
	if (type <= IJavaScriptElement.PACKAGE_FRAGMENT_ROOT || type > IJavaScriptElement.JAVASCRIPT_UNIT)
		error(IJavaScriptModelStatusConstants.INVALID_ELEMENT_TYPES, element);
//	else if (type == IJavaScriptElement.PACKAGE_FRAGMENT && element instanceof JarPackageFragment)
//		error(IJavaScriptModelStatusConstants.INVALID_ELEMENT_TYPES, element);
	IResource resource = element.getResource();
	if (resource instanceof IFolder) {
		if (resource.isLinked()) {
			error(IJavaScriptModelStatusConstants.INVALID_RESOURCE, element);
		}
	}
}
}
