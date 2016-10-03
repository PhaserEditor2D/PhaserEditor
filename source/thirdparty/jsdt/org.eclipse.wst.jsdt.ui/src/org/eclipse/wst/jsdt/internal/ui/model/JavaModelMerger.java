/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.model;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.runtime.Assert;
import org.eclipse.ltk.ui.refactoring.model.AbstractResourceMappingMerger;
import org.eclipse.wst.jsdt.core.IJavaScriptModel;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;

/**
 * Java-aware refactoring model merger.
 * 
 * 
 */
public final class JavaModelMerger extends AbstractResourceMappingMerger {

	/**
	 * Creates a new java model merger.
	 * 
	 * @param provider
	 *            the model provider
	 */
	public JavaModelMerger(final ModelProvider provider) {
		super(provider);
	}

	/**
	 * {@inheritDoc}
	 */
	protected IProject[] getDependencies(final IProject[] projects) {
		Assert.isNotNull(projects);
		final Set set= new HashSet();
		for (int index= 0; index < projects.length; index++)
			getDependentProjects(set, projects[index]);
		final IProject[] result= new IProject[set.size()];
		set.toArray(result);
		return result;
	}

	/**
	 * Returns the dependent projects of the specified project.
	 * 
	 * @param set
	 *            the project set
	 * @param project
	 *            the project to get its dependent projects
	 */
	private void getDependentProjects(final Set set, final IProject project) {
		Assert.isNotNull(set);
		Assert.isNotNull(project);
		final IJavaScriptModel model= JavaScriptCore.create(ResourcesPlugin.getWorkspace().getRoot());
		if (model != null) {
			try {
				final String name= project.getName();
				final IJavaScriptProject[] projects= model.getJavaScriptProjects();
				for (int index= 0; index < projects.length; index++) {
					final String[] names= projects[index].getRequiredProjectNames();
					for (int offset= 0; offset < names.length; offset++) {
						if (name.equals(names[offset]))
							set.add(projects[index].getProject());
					}
				}
			} catch (JavaScriptModelException exception) {
				JavaScriptPlugin.log(exception);
			}
		}
	}
}
