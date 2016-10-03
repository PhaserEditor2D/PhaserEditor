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

import java.util.HashSet;
import java.util.Iterator;

import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptElementDelta;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;

/**
 * This class is used by <code>JavaModelManager</code> to update the JavaModel
 * based on some <code>IJavaScriptElementDelta</code>s.
 */
public class ModelUpdater {

	HashSet projectsToUpdate = new HashSet();

	/**
	 * Adds the given child handle to its parent's cache of children.
	 */
	protected void addToParentInfo(Openable child) {

		Openable parent = (Openable) child.getParent();
		if (parent != null && parent.isOpen()) {
			try {
				JavaElementInfo info = (JavaElementInfo)parent.getElementInfo();
				info.addChild(child);
			} catch (JavaScriptModelException e) {
				// do nothing - we already checked if open
			}
		}
	}

	/**
	 * Closes the given element, which removes it from the cache of open elements.
	 */
	protected static void close(Openable element) {

		try {
			element.close();
		} catch (JavaScriptModelException e) {
			// do nothing
		}
	}

	/**
	 * Processing for an element that has been added:<ul>
	 * <li>If the element is a project, do nothing, and do not process
	 * children, as when a project is created it does not yet have any
	 * natures - specifically a java nature.
	 * <li>If the elemet is not a project, process it as added (see
	 * <code>basicElementAdded</code>.
	 * </ul>
	 */
	protected void elementAdded(Openable element) {

		int elementType = element.getElementType();
		if (elementType == IJavaScriptElement.JAVASCRIPT_PROJECT) {
			// project add is handled by JavaProject.configure() because
			// when a project is created, it does not yet have a java nature
			addToParentInfo(element);
			this.projectsToUpdate.add(element);
		} else {
			addToParentInfo(element);

			// Force the element to be closed as it might have been opened
			// before the resource modification came in and it might have a new child
			// For example, in an IWorkspaceRunnable:
			// 1. create a package fragment p using a java model operation
			// 2. open package p
			// 3. add file X.js in folder p
			// When the resource delta comes in, only the addition of p is notified,
			// but the package p is already opened, thus its children are not recomputed
			// and it appears empty.
			close(element);
		}

		switch (elementType) {
			case IJavaScriptElement.PACKAGE_FRAGMENT_ROOT :
				// when a root is added, and is on the classpath, the project must be updated
				this.projectsToUpdate.add(element.getJavaScriptProject());
				break;
			case IJavaScriptElement.PACKAGE_FRAGMENT :
				// get rid of package fragment cache
				JavaProject project = (JavaProject) element.getJavaScriptProject();
				project.resetCaches();
				break;
		}
	}

	/**
	 * Generic processing for elements with changed contents:<ul>
	 * <li>The element is closed such that any subsequent accesses will re-open
	 * the element reflecting its new structure.
	 * </ul>
	 */
	protected void elementChanged(Openable element) {

		close(element);
	}

	/**
	 * Generic processing for a removed element:<ul>
	 * <li>Close the element, removing its structure from the cache
	 * <li>Remove the element from its parent's cache of children
	 * <li>Add a REMOVED entry in the delta
	 * </ul>
	 */
	protected void elementRemoved(Openable element) {

		if (element.isOpen()) {
			close(element);
		}
		removeFromParentInfo(element);
		int elementType = element.getElementType();

		switch (elementType) {
			case IJavaScriptElement.JAVASCRIPT_MODEL :
				JavaModelManager.getJavaModelManager().getIndexManager().reset();
				break;
			case IJavaScriptElement.JAVASCRIPT_PROJECT :
				JavaModelManager manager = JavaModelManager.getJavaModelManager();
				JavaProject javaProject = (JavaProject) element;
				manager.removePerProjectInfo(javaProject);
				manager.containerRemove(javaProject);
				break;
			case IJavaScriptElement.PACKAGE_FRAGMENT_ROOT :
				this.projectsToUpdate.add(element.getJavaScriptProject());
				break;
			case IJavaScriptElement.PACKAGE_FRAGMENT :
				// get rid of package fragment cache
				JavaProject project = (JavaProject) element.getJavaScriptProject();
				project.resetCaches();
				break;
		}
	}

	/**
	 * Converts a <code>IResourceDelta</code> rooted in a <code>Workspace</code> into
	 * the corresponding set of <code>IJavaScriptElementDelta</code>, rooted in the
	 * relevant <code>JavaModel</code>s.
	 */
	public void processJavaDelta(IJavaScriptElementDelta delta) {

//		if (DeltaProcessor.VERBOSE){
//			System.out.println("UPDATING Model with Delta: ["+Thread.currentThread()+":" + delta + "]:");
//		}

		try {
			this.traverseDelta(delta, null, null); // traverse delta

			// update package fragment roots of projects that were affected
			Iterator iterator = this.projectsToUpdate.iterator();
			while (iterator.hasNext()) {
				JavaProject project = (JavaProject) iterator.next();
				project.updatePackageFragmentRoots();
			}
		} finally {
			this.projectsToUpdate = new HashSet();
		}
	}

	/**
	 * Removes the given element from its parents cache of children. If the
	 * element does not have a parent, or the parent is not currently open,
	 * this has no effect.
	 */
	protected void removeFromParentInfo(Openable child) {

		Openable parent = (Openable) child.getParent();
		if (parent != null && parent.isOpen()) {
			try {
				JavaElementInfo info = (JavaElementInfo)parent.getElementInfo();
				info.removeChild(child);
			} catch (JavaScriptModelException e) {
				// do nothing - we already checked if open
			}
		}
	}

	/**
	 * Converts an <code>IResourceDelta</code> and its children into
	 * the corresponding <code>IJavaScriptElementDelta</code>s.
	 * Return whether the delta corresponds to a resource on the classpath.
	 * If it is not a resource on the classpath, it will be added as a non-java
	 * resource by the sender of this method.
	 */
	protected void traverseDelta(
		IJavaScriptElementDelta delta,
		IPackageFragmentRoot root,
		IJavaScriptProject project) {

		boolean processChildren = true;

		Openable element = (Openable) delta.getElement();
		switch (element.getElementType()) {
			case IJavaScriptElement.JAVASCRIPT_PROJECT :
				project = (IJavaScriptProject) element;
				break;
			case IJavaScriptElement.PACKAGE_FRAGMENT_ROOT :
				root = (IPackageFragmentRoot) element;
				break;
			case IJavaScriptElement.JAVASCRIPT_UNIT :
				// filter out working copies that are not primary (we don't want to add/remove them to/from the package fragment
				CompilationUnit cu = (CompilationUnit)element;
				if (cu.isWorkingCopy() && !cu.isPrimary()) {
					return;
				}
			case IJavaScriptElement.CLASS_FILE :
				processChildren = false;
				break;
		}

		switch (delta.getKind()) {
			case IJavaScriptElementDelta.ADDED :
				elementAdded(element);
				break;
			case IJavaScriptElementDelta.REMOVED :
				elementRemoved(element);
				break;
			case IJavaScriptElementDelta.CHANGED :
				if ((delta.getFlags() & IJavaScriptElementDelta.F_CONTENT) != 0){
					elementChanged(element);
				}
				break;
		}
		if (processChildren) {
			IJavaScriptElementDelta[] children = delta.getAffectedChildren();
			for (int i = 0; i < children.length; i++) {
				IJavaScriptElementDelta childDelta = children[i];
				this.traverseDelta(childDelta, root, project);
			}
		}
	}
}
