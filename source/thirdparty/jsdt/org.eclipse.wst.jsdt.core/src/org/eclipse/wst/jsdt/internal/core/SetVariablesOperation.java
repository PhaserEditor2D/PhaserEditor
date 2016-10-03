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
import java.util.Iterator;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptModel;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.core.util.Util;

public class SetVariablesOperation extends ChangeClasspathOperation {

	String[] variableNames;
	IPath[] variablePaths;
	boolean updatePreferences;

	/*
	 * Creates a new SetVariablesOperation for the given variable values (null path meaning removal), allowing to change multiple variable values at once.
	 */
	public SetVariablesOperation(String[] variableNames, IPath[] variablePaths, boolean updatePreferences) {
		super(new IJavaScriptElement[] {JavaModelManager.getJavaModelManager().getJavaModel()}, !ResourcesPlugin.getWorkspace().isTreeLocked());
		this.variableNames = variableNames;
		this.variablePaths = variablePaths;
		this.updatePreferences = updatePreferences;
	}

	protected void executeOperation() throws JavaScriptModelException {
		checkCanceled();
		try {
			beginTask("", 1); //$NON-NLS-1$
			if (JavaModelManager.CP_RESOLVE_VERBOSE)
				verbose_set_variables();

			JavaModelManager manager = JavaModelManager.getJavaModelManager();
			if (manager.variablePutIfInitializingWithSameValue(this.variableNames, this.variablePaths))
				return;

			int varLength = this.variableNames.length;

			// gather classpath information for updating
			final HashMap affectedProjectClasspaths = new HashMap(5);
			IJavaScriptModel model = getJavaModel();

			// filter out unmodified variables
			int discardCount = 0;
			for (int i = 0; i < varLength; i++){
				String variableName = this.variableNames[i];
				IPath oldPath = manager.variableGet(variableName); // if reentering will provide previous session value
				if (oldPath == JavaModelManager.VARIABLE_INITIALIZATION_IN_PROGRESS) {
					oldPath = null;  //33695 - cannot filter out restored variable, must update affected project to reset cached CP
				}
				if (oldPath != null && oldPath.equals(this.variablePaths[i])){
					this.variableNames[i] = null;
					discardCount++;
				}
			}
			if (discardCount > 0){
				if (discardCount == varLength) return;
				int changedLength = varLength - discardCount;
				String[] changedVariableNames = new String[changedLength];
				IPath[] changedVariablePaths = new IPath[changedLength];
				for (int i = 0, index = 0; i < varLength; i++){
					if (this.variableNames[i] != null){
						changedVariableNames[index] = this.variableNames[i];
						changedVariablePaths[index] = this.variablePaths[i];
						index++;
					}
				}
				this.variableNames = changedVariableNames;
				this.variablePaths = changedVariablePaths;
				varLength = changedLength;
			}

			if (isCanceled())
				return;

			IJavaScriptProject[] projects = model.getJavaScriptProjects();
			nextProject : for (int i = 0, projectLength = projects.length; i < projectLength; i++){
				JavaProject project = (JavaProject) projects[i];

				// check to see if any of the modified variables is present on the classpath
				IIncludePathEntry[] classpath = project.getRawIncludepath();
				for (int j = 0, cpLength = classpath.length; j < cpLength; j++){

					IIncludePathEntry entry = classpath[j];
					for (int k = 0; k < varLength; k++){

						String variableName = this.variableNames[k];
						if (entry.getEntryKind() ==  IIncludePathEntry.CPE_VARIABLE){

							if (variableName.equals(entry.getPath().segment(0))){
								affectedProjectClasspaths.put(project, project.getResolvedClasspath());
								continue nextProject;
							}
							IPath sourcePath, sourceRootPath;
							if (((sourcePath = entry.getSourceAttachmentPath()) != null	&& variableName.equals(sourcePath.segment(0)))
								|| ((sourceRootPath = entry.getSourceAttachmentRootPath()) != null	&& variableName.equals(sourceRootPath.segment(0)))) {

								affectedProjectClasspaths.put(project, project.getResolvedClasspath());
								continue nextProject;
							}
						}
					}
				}
			}

			// update variables
			for (int i = 0; i < varLength; i++){
				manager.variablePut(this.variableNames[i], this.variablePaths[i]);
				if (this.updatePreferences)
					manager.variablePreferencesPut(this.variableNames[i], this.variablePaths[i]);
			}

			// update affected project classpaths
			if (!affectedProjectClasspaths.isEmpty()) {
				String[] dbgVariableNames = this.variableNames;
				try {
					// propagate classpath change
					Iterator projectsToUpdate = affectedProjectClasspaths.keySet().iterator();
					while (projectsToUpdate.hasNext()) {

						if (this.progressMonitor != null && this.progressMonitor.isCanceled()) return;

						JavaProject affectedProject = (JavaProject) projectsToUpdate.next();

						// force resolved classpath to be recomputed
						if (JavaModelManager.CP_RESOLVE_VERBOSE_ADVANCED)
							verbose_update_project(dbgVariableNames, affectedProject);
						affectedProject.getPerProjectInfo().resetResolvedClasspath();

						// if needed, generate delta, update project ref, create markers, ...
						classpathChanged(affectedProject);

						if (this.canChangeResources) {
							// touch project to force a build if needed
							affectedProject.getProject().touch(this.progressMonitor);
						}
					}
				} catch (CoreException e) {
					if (JavaModelManager.CP_RESOLVE_VERBOSE){
						verbose_failure(dbgVariableNames);
						e.printStackTrace();
					}
					if (e instanceof JavaScriptModelException) {
						throw (JavaScriptModelException)e;
					} else {
						throw new JavaScriptModelException(e);
					}
				}
			}
		} finally {
			done();
		}
	}

	private void verbose_failure(String[] dbgVariableNames) {
		Util.verbose(
			"CPVariable SET  - FAILED DUE TO EXCEPTION\n" + //$NON-NLS-1$
			"	variables: " + org.eclipse.wst.jsdt.internal.compiler.util.Util.toString(dbgVariableNames), //$NON-NLS-1$
			System.err);
	}

	private void verbose_update_project(String[] dbgVariableNames,
			JavaProject affectedProject) {
		Util.verbose(
			"CPVariable SET  - updating affected project due to setting variables\n" + //$NON-NLS-1$
			"	project: " + affectedProject.getElementName() + '\n' + //$NON-NLS-1$
			"	variables: " + org.eclipse.wst.jsdt.internal.compiler.util.Util.toString(dbgVariableNames)); //$NON-NLS-1$
	}

	private void verbose_set_variables() {
		Util.verbose(
			"CPVariable SET  - setting variables\n" + //$NON-NLS-1$
			"	variables: " + org.eclipse.wst.jsdt.internal.compiler.util.Util.toString(this.variableNames) + '\n' +//$NON-NLS-1$
			"	values: " + org.eclipse.wst.jsdt.internal.compiler.util.Util.toString(this.variablePaths)); //$NON-NLS-1$
	}

}
