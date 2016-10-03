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

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptModel;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.WorkingCopyOwner;
import org.eclipse.wst.jsdt.internal.core.util.MementoTokenizer;
import org.eclipse.wst.jsdt.internal.core.util.Messages;

/**
 * Implementation of <code>IJavaScriptModel<code>. The Java Model maintains a cache of
 * active <code>IJavaScriptProject</code>s in a workspace. A Java Model is specific to a
 * workspace. To retrieve a workspace's model, use the
 * <code>#getJavaModel(IWorkspace)</code> method.
 *
 * @see IJavaScriptModel
 */
public class JavaModel extends Openable implements IJavaScriptModel {

	/**
	 * A set of java.io.Files used as a cache of external jars that
	 * are known to be existing.
	 * Note this cache is kept for the whole session.
	 */
	public static HashSet existingExternalFiles = new HashSet();

	/**
	 * A set of external files ({@link #existingExternalFiles}) which have
	 * been confirmed as file (ie. which returns true to {@link java.io.File#isFile()}.
	 * Note this cache is kept for the whole session.
	 */
	public static HashSet existingExternalConfirmedFiles = new HashSet();

/**
 * Constructs a new Java Model on the given workspace.
 * Note that only one instance of JavaModel handle should ever be created.
 * One should only indirect through JavaModelManager#getJavaModel() to get
 * access to it.
 *
 * @exception Error if called more than once
 */
protected JavaModel() throws Error {
	super(null);
}
protected boolean buildStructure(OpenableElementInfo info, IProgressMonitor pm, Map newElements, IResource underlyingResource)	/*throws JavaScriptModelException*/ {

	// determine my children
	IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
	int length = projects.length;
	IJavaScriptElement[] children = new IJavaScriptElement[length];
	int index = 0;
	for (int i = 0; i < length; i++) {
		IProject project = projects[i];
		if (JavaProject.hasJavaNature(project)) {
			children[index++] = getJavaProject(project);
		}
	}
	if (index < length)
		System.arraycopy(children, 0, children = new IJavaScriptElement[index], 0, index);
	info.setChildren(children);

	newElements.put(this, info);

	return true;
}
/*
 * @see IJavaScriptModel
 */
public boolean contains(IResource resource) {
	switch (resource.getType()) {
		case IResource.ROOT:
		case IResource.PROJECT:
			return true;
	}
	// file or folder
	IJavaScriptProject[] projects;
	try {
		projects = this.getJavaScriptProjects();
	} catch (JavaScriptModelException e) {
		return false;
	}
	for (int i = 0, length = projects.length; i < length; i++) {
		JavaProject project = (JavaProject)projects[i];
		if (!project.contains(resource)) {
			return false;
		}
	}
	return true;
}
/**
 * @see IJavaScriptModel
 */
public void copy(IJavaScriptElement[] elements, IJavaScriptElement[] containers, IJavaScriptElement[] siblings, String[] renamings, boolean force, IProgressMonitor monitor) throws JavaScriptModelException {
	if (elements != null && elements.length > 0 && elements[0] != null && elements[0].getElementType() < IJavaScriptElement.TYPE) {
		runOperation(new CopyResourceElementsOperation(elements, containers, force), elements, siblings, renamings, monitor);
	} else {
		runOperation(new CopyElementsOperation(elements, containers, force), elements, siblings, renamings, monitor);
	}
}
/**
 * Returns a new element info for this element.
 */
protected Object createElementInfo() {
	return new JavaModelInfo();
}

/**
 * @see IJavaScriptModel
 */
public void delete(IJavaScriptElement[] elements, boolean force, IProgressMonitor monitor) throws JavaScriptModelException {
	if (elements != null && elements.length > 0 && elements[0] != null && elements[0].getElementType() < IJavaScriptElement.TYPE) {
		new DeleteResourceElementsOperation(elements, force).runOperation(monitor);
	} else {
		new DeleteElementsOperation(elements, force).runOperation(monitor);
	}
}
public boolean equals(Object o) {
	if (!(o instanceof JavaModel)) return false;
	return super.equals(o);
}
/**
 * @see IJavaScriptElement
 */
public int getElementType() {
	return JAVASCRIPT_MODEL;
}
/**
 * Flushes the cache of external files known to be existing.
 */
public static void flushExternalFileCache() {
	existingExternalFiles = new HashSet();
	existingExternalConfirmedFiles = new HashSet();
}

/*
 * @see JavaElement
 */
public IJavaScriptElement getHandleFromMemento(String token, MementoTokenizer memento, WorkingCopyOwner owner) {
	switch (token.charAt(0)) {
		case JEM_JAVAPROJECT:
			if (!memento.hasMoreTokens()) return this;
			String projectName = memento.nextToken();
			JavaElement project = (JavaElement)getJavaScriptProject(projectName);
			return project.getHandleFromMemento(memento, owner);
	}
	return null;
}
/**
 * @see JavaElement#getHandleMemento(StringBuffer)
 */
protected void getHandleMemento(StringBuffer buff) {
	buff.append(getElementName());
}
/**
 * Returns the <code>char</code> that marks the start of this handles
 * contribution to a memento.
 */
protected char getHandleMementoDelimiter(){
	Assert.isTrue(false, "Should not be called"); //$NON-NLS-1$
	return 0;
}
/**
 * @see IJavaScriptModel
 */
public IJavaScriptProject getJavaScriptProject(String projectName) {
	return new JavaProject(ResourcesPlugin.getWorkspace().getRoot().getProject(projectName), this);
}
/**
 * Returns the active Java project associated with the specified
 * resource, or <code>null</code> if no Java project yet exists
 * for the resource.
 *
 * @exception IllegalArgumentException if the given resource
 * is not one of an IProject, IFolder, or IFile.
 */
public IJavaScriptProject getJavaProject(IResource resource) {
	switch(resource.getType()){
		case IResource.FOLDER:
			return new JavaProject(((IFolder)resource).getProject(), this);
		case IResource.FILE:
			return new JavaProject(((IFile)resource).getProject(), this);
		case IResource.PROJECT:
			return new JavaProject((IProject)resource, this);
		default:
			throw new IllegalArgumentException(Messages.element_invalidResourceForProject);
	}
}
/**
 * @see IJavaScriptModel
 */
public IJavaScriptProject[] getJavaScriptProjects() throws JavaScriptModelException {
	ArrayList list = getChildrenOfType(JAVASCRIPT_PROJECT);
	IJavaScriptProject[] array= new IJavaScriptProject[list.size()];
	list.toArray(array);
	return array;

}
/**
 * @see IJavaScriptModel
 */
public Object[] getNonJavaScriptResources() throws JavaScriptModelException {
		return ((JavaModelInfo) getElementInfo()).getNonJavaResources();
}

/*
 * @see IJavaScriptElement
 */
public IPath getPath() {
	return Path.ROOT;
}
/*
 * @see IJavaScriptElement
 */
public IResource getResource() {
	return ResourcesPlugin.getWorkspace().getRoot();
}
/**
 * @see org.eclipse.wst.jsdt.core.IOpenable
 */
public IResource getUnderlyingResource() {
	return null;
}
/**
 * Returns the workbench associated with this object.
 */
public IWorkspace getWorkspace() {
	return ResourcesPlugin.getWorkspace();
}

/**
 * @see IJavaScriptModel
 */
public void move(IJavaScriptElement[] elements, IJavaScriptElement[] containers, IJavaScriptElement[] siblings, String[] renamings, boolean force, IProgressMonitor monitor) throws JavaScriptModelException {
	if (elements != null && elements.length > 0 && elements[0] != null && elements[0].getElementType() < IJavaScriptElement.TYPE) {
		runOperation(new MoveResourceElementsOperation(elements, containers, force), elements, siblings, renamings, monitor);
	} else {
		runOperation(new MoveElementsOperation(elements, containers, force), elements, siblings, renamings, monitor);
	}
}

/**
 * @see IJavaScriptModel#refreshExternalArchives(IJavaScriptElement[], IProgressMonitor)
 */
public void refreshExternalArchives(IJavaScriptElement[] elementsScope, IProgressMonitor monitor) throws JavaScriptModelException {
	if (elementsScope == null){
		elementsScope = new IJavaScriptElement[] { this };
	}
	JavaModelManager.getJavaModelManager().getDeltaProcessor().checkExternalArchiveChanges(elementsScope, monitor);
}

/**
 * @see IJavaScriptModel
 */
public void rename(IJavaScriptElement[] elements, IJavaScriptElement[] destinations, String[] renamings, boolean force, IProgressMonitor monitor) throws JavaScriptModelException {
	MultiOperation op;
	if (elements != null && elements.length > 0 && elements[0] != null && elements[0].getElementType() < IJavaScriptElement.TYPE) {
		op = new RenameResourceElementsOperation(elements, destinations, renamings, force);
	} else {
		op = new RenameElementsOperation(elements, destinations, renamings, force);
	}

	op.runOperation(monitor);
}
/**
 * Configures and runs the <code>MultiOperation</code>.
 */
protected void runOperation(MultiOperation op, IJavaScriptElement[] elements, IJavaScriptElement[] siblings, String[] renamings, IProgressMonitor monitor) throws JavaScriptModelException {
	op.setRenamings(renamings);
	if (siblings != null) {
		for (int i = 0; i < elements.length; i++) {
			op.setInsertBefore(elements[i], siblings[i]);
		}
	}
	op.runOperation(monitor);
}
/**
 * @private Debugging purposes
 */
protected void toStringInfo(int tab, StringBuffer buffer, Object info, boolean showResolvedInfo) {
	buffer.append(this.tabString(tab));
	buffer.append("Java Model"); //$NON-NLS-1$
	if (info == null) {
		buffer.append(" (not open)"); //$NON-NLS-1$
	}
}

/**
 * Helper method - returns the targeted item (IResource if internal or java.io.File if external),
 * or null if unbound
 * Internal items must be referred to using container relative paths.
 */
public static Object getTarget(IContainer container, IPath path, boolean checkResourceExistence) {

	if (path == null) return null;

	// lookup - inside the container
	if (path.getDevice() == null) { // container relative paths should not contain a device
												// (see http://dev.eclipse.org/bugs/show_bug.cgi?id=18684)
												// (case of a workspace rooted at d:\ )
		IResource resource = container.findMember(path);
		if (resource != null){
			if (!checkResourceExistence ||resource.exists()) return resource;
			return null;
		}
	}

	// if path is relative, it cannot be an external path
	// (see http://dev.eclipse.org/bugs/show_bug.cgi?id=22517)
	if (!path.isAbsolute()) return null;

	// lookup - outside the container
	return getTargetAsExternalFile(path, checkResourceExistence);
}
private synchronized static Object getTargetAsExternalFile(IPath path, boolean checkResourceExistence) {
	File externalFile = new File(path.toOSString());
	if (!checkResourceExistence) {
		return externalFile;
	} else if (existingExternalFiles.contains(externalFile)) {
		return externalFile;
	} else {
		if (JavaModelManager.ZIP_ACCESS_VERBOSE) {
			System.out.println("(" + Thread.currentThread() + ") [JavaModel.getTarget(...)] Checking existence of " + path.toString()); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (externalFile.exists()) {
			// cache external file
			existingExternalFiles.add(externalFile);
			return externalFile;
		}
	}
	return null;
}

/**
 * Helper method - returns whether an object is a file (ie. which returns true to {@link java.io.File#isFile()}.
 */
public static boolean isFile(Object target) {
	File f = getFile(target);
	return f != null && f.isFile();
}

/**
 * Helper method - returns the file item (ie. which returns true to {@link java.io.File#isFile()},
 * or null if unbound
 */
public static synchronized File getFile(Object target) {
	if (existingExternalConfirmedFiles.contains(target))
		return (File) target;
	if (target instanceof File) {
		File f = (File) target;
//		if (f.isFile()) {
		if (f.exists()) {
			existingExternalConfirmedFiles.add(f);
			return f;
		}
	}

	return null;
}
}
