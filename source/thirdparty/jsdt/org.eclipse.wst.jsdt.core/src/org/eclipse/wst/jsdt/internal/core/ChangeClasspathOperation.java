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
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;

/*
 * Abstract class for operations that change the classpath
 */
public abstract class ChangeClasspathOperation extends JavaModelOperation {

	protected boolean canChangeResources;

	public ChangeClasspathOperation(IJavaScriptElement[] elements, boolean canChangeResources) {
		super(elements);
		this.canChangeResources = canChangeResources;
	}

	protected boolean canModifyRoots() {
		// changing the classpath can modify roots
		return true;
	}

	/*
	 * The resolved classpath of the given project may have changed:
	 * - generate a delta
	 * - trigger indexing
	 * - update project references
	 * - create resolved classpath markers
	 */
	protected void classpathChanged(JavaProject project) throws JavaScriptModelException {
		DeltaProcessingState state = JavaModelManager.getJavaModelManager().deltaState;
		DeltaProcessor deltaProcessor = state.getDeltaProcessor();
		ClasspathChange change = (ClasspathChange) deltaProcessor.classpathChanges.get(project.getProject());
		if (this.canChangeResources) {
			// workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=177922
			if (isTopLevelOperation() && !ResourcesPlugin.getWorkspace().isTreeLocked()) {
				new ClasspathValidation(project).validate();
			}

			// delta, indexing and classpath markers are going to be created by the delta processor
			// while handling the .classpath file change

			// however ensure project references are updated
			// since some clients rely on the project references when run inside an IWorkspaceRunnable
			new ProjectReferenceChange(project, change.oldResolvedClasspath).updateProjectReferencesIfNecessary();
		} else {
			JavaElementDelta delta = new JavaElementDelta(getJavaModel());
			int result = change.generateDelta(delta);
			if ((result & ClasspathChange.HAS_DELTA) != 0) {
				// create delta
				addDelta(delta);

				// ensure indexes are updated
				change.requestIndexing();

				// ensure classpath is validated on next build
				state.addClasspathValidation(project);
			}
			if ((result & ClasspathChange.HAS_PROJECT_CHANGE) != 0) {
				// ensure project references are updated on next build
				state.addProjectReferenceChange(project, change.oldResolvedClasspath);
			}
		}
	}

	protected ISchedulingRule getSchedulingRule() {
		return null; // no lock taken while changing classpath
	}

	public boolean isReadOnly() {
		return !this.canChangeResources;
	}

}
