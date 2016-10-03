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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.internal.core.util.Util;

public class ProjectReferenceChange {

	private JavaProject project;
	private IIncludePathEntry[] oldResolvedClasspath;

	public ProjectReferenceChange(JavaProject project, IIncludePathEntry[] oldResolvedClasspath) {
		this.project = project;
		this.oldResolvedClasspath = oldResolvedClasspath;
	}

	/*
	 * Update projects references so that the build order is consistent with the classpath
	 */
	public void updateProjectReferencesIfNecessary() throws JavaScriptModelException {

		String[] oldRequired = this.oldResolvedClasspath == null ? CharOperation.NO_STRINGS : this.project.projectPrerequisites(this.oldResolvedClasspath);
		IIncludePathEntry[] newResolvedClasspath = this.project.getResolvedClasspath();
		String[] newRequired = this.project.projectPrerequisites(newResolvedClasspath);
		try {
			IProject projectResource = this.project.getProject();
			IProjectDescription description = projectResource.getDescription();

			IProject[] projectReferences = description.getDynamicReferences();

			HashSet oldReferences = new HashSet(projectReferences.length);
			for (int i = 0; i < projectReferences.length; i++){
				String projectName = projectReferences[i].getName();
				oldReferences.add(projectName);
			}
			HashSet newReferences = (HashSet)oldReferences.clone();

			for (int i = 0; i < oldRequired.length; i++){
				String projectName = oldRequired[i];
				newReferences.remove(projectName);
			}
			for (int i = 0; i < newRequired.length; i++){
				String projectName = newRequired[i];
				newReferences.add(projectName);
			}

			Iterator iter;
			int newSize = newReferences.size();

			checkIdentity: {
				if (oldReferences.size() == newSize){
					iter = newReferences.iterator();
					while (iter.hasNext()){
						if (!oldReferences.contains(iter.next())){
							break checkIdentity;
						}
					}
					return;
				}
			}
			String[] requiredProjectNames = new String[newSize];
			int index = 0;
			iter = newReferences.iterator();
			while (iter.hasNext()){
				requiredProjectNames[index++] = (String)iter.next();
			}
			Util.sort(requiredProjectNames); // ensure that if changed, the order is consistent

			IProject[] requiredProjectArray = new IProject[newSize];
			IWorkspaceRoot wksRoot = projectResource.getWorkspace().getRoot();
			for (int i = 0; i < newSize; i++){
				requiredProjectArray[i] = wksRoot.getProject(requiredProjectNames[i]);
			}
			description.setDynamicReferences(requiredProjectArray);
			projectResource.setDescription(description, null);

		} catch(CoreException e){
			if (!ExternalJavaProject.EXTERNAL_PROJECT_NAME.equals(this.project.getElementName()))
				throw new JavaScriptModelException(e);
		}
	}
	public String toString() {
		return "ProjectRefenceChange: " + this.project.getElementName(); //$NON-NLS-1$
	}
}
