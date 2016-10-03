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

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.wst.jsdt.core.IJsGlobalScopeContainer;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.core.util.Util;

public class SetContainerOperation extends ChangeClasspathOperation {

	IPath containerPath;
	IJavaScriptProject[] affectedProjects;
	IJsGlobalScopeContainer[] respectiveContainers;

	/*
	 * Creates a new SetContainerOperation.
	 */
	public SetContainerOperation(IPath containerPath, IJavaScriptProject[] affectedProjects, IJsGlobalScopeContainer[] respectiveContainers) {
		super(new IJavaScriptElement[] {JavaModelManager.getJavaModelManager().getJavaModel()}, !ResourcesPlugin.getWorkspace().isTreeLocked());
		this.containerPath = containerPath;
		this.affectedProjects = affectedProjects;
		this.respectiveContainers = respectiveContainers;
	}

	protected void executeOperation() throws JavaScriptModelException {
		execute();
	}
		
	public void execute() throws JavaScriptModelException {

		checkCanceled();
		try {
			beginTask("", 1); //$NON-NLS-1$
			if (JavaModelManager.CP_RESOLVE_VERBOSE)
				verbose_set_container();
			if (JavaModelManager.CP_RESOLVE_VERBOSE_ADVANCED)
				verbose_set_container_invocation_trace();

			JavaModelManager manager = JavaModelManager.getJavaModelManager();
			if (manager.containerPutIfInitializingWithSameEntries(this.containerPath, this.affectedProjects, this.respectiveContainers))
				return;

			final int projectLength = this.affectedProjects.length;
			final IJavaScriptProject[] modifiedProjects;
			System.arraycopy(this.affectedProjects, 0, modifiedProjects = new IJavaScriptProject[projectLength], 0, projectLength);
			final IIncludePathEntry[][] oldResolvedPaths = new IIncludePathEntry[projectLength][];

			// filter out unmodified project containers
			int remaining = 0;
			for (int i = 0; i < projectLength; i++){
				if (isCanceled())
					return;
				JavaProject affectedProject = (JavaProject) this.affectedProjects[i];
				IJsGlobalScopeContainer newContainer = this.respectiveContainers[i];
				if (newContainer == null) newContainer = JavaModelManager.CONTAINER_INITIALIZATION_IN_PROGRESS; // 30920 - prevent infinite loop
				boolean found = false;
				if (JavaProject.hasJavaNature(affectedProject.getProject())){
					IIncludePathEntry[] rawClasspath = affectedProject.getRawIncludepath();
					for (int j = 0, cpLength = rawClasspath.length; j <cpLength; j++) {
						IIncludePathEntry entry = rawClasspath[j];
						if (entry.getEntryKind() == IIncludePathEntry.CPE_CONTAINER && entry.getPath().equals(this.containerPath)){
							found = true;
							break;
						}
					}
				}
				if (!found) {
					modifiedProjects[i] = null; // filter out this project - does not reference the container path, or isnt't yet Java project
					manager.containerPut(affectedProject, this.containerPath, newContainer);
					continue;
				}
				IJsGlobalScopeContainer oldContainer = manager.containerGet(affectedProject, this.containerPath);
				if (oldContainer == JavaModelManager.CONTAINER_INITIALIZATION_IN_PROGRESS) {
					oldContainer = null;
				}
				if ((oldContainer != null && oldContainer.equals(this.respectiveContainers[i]))
						|| (oldContainer == this.respectiveContainers[i])/*handle case where old and new containers are null (see bug 149043*/) {
					modifiedProjects[i] = null; // filter out this project - container did not change
					continue;
				}
				remaining++;
				oldResolvedPaths[i] = affectedProject.getResolvedClasspath();
				manager.containerPut(affectedProject, this.containerPath, newContainer);
			}

			if (remaining == 0) return;

			// trigger model refresh
			try {
				for(int i = 0; i < projectLength; i++){
					if (isCanceled())
						return;

					JavaProject affectedProject = (JavaProject)modifiedProjects[i];
					if (affectedProject == null) continue; // was filtered out
					if (JavaModelManager.CP_RESOLVE_VERBOSE_ADVANCED)
						verbose_update_project(affectedProject);

					// force resolved classpath to be recomputed
					affectedProject.getPerProjectInfo().resetResolvedClasspath();

					// if needed, generate delta, update project ref, create markers, ...
					classpathChanged(affectedProject);

					if (this.canChangeResources) {
						// touch project to force a build if needed
						try {
							affectedProject.getProject().touch(this.progressMonitor);
						} catch (CoreException e) {
							// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=148970
							if (!ExternalJavaProject.EXTERNAL_PROJECT_NAME.equals(affectedProject.getElementName()))
								throw e;
						}
					}
				}
			} catch(CoreException e) {
				if (JavaModelManager.CP_RESOLVE_VERBOSE)
					verbose_failure(e);
				if (e instanceof JavaScriptModelException) {
					throw (JavaScriptModelException)e;
				} else {
					throw new JavaScriptModelException(e);
				}
			} finally {
				for (int i = 0; i < projectLength; i++) {
					if (this.respectiveContainers[i] == null) {
						manager.containerPut(this.affectedProjects[i], this.containerPath, null); // reset init in progress marker
					}
				}
			}
		} finally {
			done();
		}
	}

	private void verbose_failure(CoreException e) {
		Util.verbose(
			"CPContainer SET  - FAILED DUE TO EXCEPTION\n" + //$NON-NLS-1$
			"	container path: " + this.containerPath, //$NON-NLS-1$
			System.err);
		e.printStackTrace();
	}

	private void verbose_update_project(JavaProject affectedProject) {
		Util.verbose(
			"CPContainer SET  - updating affected project due to setting container\n" + //$NON-NLS-1$
			"	project: " + affectedProject.getElementName() + '\n' + //$NON-NLS-1$
			"	container path: " + this.containerPath); //$NON-NLS-1$
	}

	private void verbose_set_container() {
		Util.verbose(
			"CPContainer SET  - setting container\n" + //$NON-NLS-1$
			"	container path: " + this.containerPath + '\n' + //$NON-NLS-1$
			"	projects: {" +//$NON-NLS-1$
			org.eclipse.wst.jsdt.internal.compiler.util.Util.toString(
				this.affectedProjects,
				new org.eclipse.wst.jsdt.internal.compiler.util.Util.Displayable(){
					public String displayString(Object o) { return ((IJavaScriptProject) o).getElementName(); }
				}) +
			"}\n	values: {\n"  +//$NON-NLS-1$
			org.eclipse.wst.jsdt.internal.compiler.util.Util.toString(
				this.respectiveContainers,
				new org.eclipse.wst.jsdt.internal.compiler.util.Util.Displayable(){
					public String displayString(Object o) {
						StringBuffer buffer = new StringBuffer("		"); //$NON-NLS-1$
						if (o == null) {
							buffer.append("<null>"); //$NON-NLS-1$
							return buffer.toString();
						}
						IJsGlobalScopeContainer container = (IJsGlobalScopeContainer) o;
						buffer.append(container.getDescription());
						buffer.append(" {\n"); //$NON-NLS-1$
						IIncludePathEntry[] entries = container.getIncludepathEntries();
						if (entries != null){
							for (int i = 0; i < entries.length; i++){
								buffer.append(" 			"); //$NON-NLS-1$
								buffer.append(entries[i]);
								buffer.append('\n');
							}
						}
						buffer.append(" 		}"); //$NON-NLS-1$
						return buffer.toString();
					}
				}) +
			"\n	}");//$NON-NLS-1$
	}

	private void verbose_set_container_invocation_trace() {
		Util.verbose(
			"CPContainer SET  - setting container\n" + //$NON-NLS-1$
			"	invocation stack trace:"); //$NON-NLS-1$
			new Exception("<Fake exception>").printStackTrace(System.out); //$NON-NLS-1$
	}

}
