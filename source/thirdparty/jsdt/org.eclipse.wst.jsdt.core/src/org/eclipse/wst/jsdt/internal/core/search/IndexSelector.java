/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.core.search;

import org.eclipse.core.runtime.IPath;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptModel;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchScope;
import org.eclipse.wst.jsdt.core.search.SearchPattern;
import org.eclipse.wst.jsdt.internal.compiler.util.SimpleSet;
import org.eclipse.wst.jsdt.internal.core.JavaModelManager;
import org.eclipse.wst.jsdt.internal.core.JavaProject;
import org.eclipse.wst.jsdt.internal.core.LibraryFragmentRoot;
import org.eclipse.wst.jsdt.internal.core.PackageFragmentRoot;
import org.eclipse.wst.jsdt.internal.core.search.indexing.IndexManager;
import org.eclipse.wst.jsdt.internal.core.search.matching.MatchLocator;
import org.eclipse.wst.jsdt.internal.core.search.matching.MethodPattern;

/**
 * Selects the indexes that correspond to projects in a given search scope
 * and that are dependent on a given focus element.
 */
public class IndexSelector {
	IJavaScriptSearchScope searchScope;
	SearchPattern pattern;
	IPath[] indexLocations; // cache of the keys for looking index up

public IndexSelector(
		IJavaScriptSearchScope searchScope,
		SearchPattern pattern) {

	this.searchScope = searchScope;
	this.pattern = pattern;
}
/**
 * Returns whether elements of the given project or jar can see the given focus (an IJavaScriptProject or
 * a JarPackageFragmentRot) either because the focus is part of the project or the jar, or because it is
 * accessible throught the project's classpath
 */
public static boolean canSeeFocus(IJavaScriptElement focus, boolean isPolymorphicSearch, IPath projectOrJarPath) {
	try {
		IIncludePathEntry[] focusEntries = null;
		if (isPolymorphicSearch) {
			JavaProject focusProject = (JavaProject) focus;
			focusEntries = focusProject.getExpandedClasspath();
		}
		IJavaScriptModel model = focus.getJavaScriptModel();
		IJavaScriptProject project = getJavaProject(projectOrJarPath, model);
		if (project != null)
			return canSeeFocus(focus, (JavaProject) project, focusEntries);

		// projectOrJarPath is a jar
		// it can see the focus only if it is on the classpath of a project that can see the focus
		IJavaScriptProject[] allProjects = model.getJavaScriptProjects();
		for (int i = 0, length = allProjects.length; i < length; i++) {
			JavaProject otherProject = (JavaProject) allProjects[i];
			IIncludePathEntry entry = otherProject.getClasspathEntryFor(projectOrJarPath);
			if (entry != null
					&& entry.getEntryKind() == IIncludePathEntry.CPE_LIBRARY
					&& canSeeFocus(focus, otherProject, focusEntries))
				return true;
		}
		return false;
	} catch (JavaScriptModelException e) {
		return false;
	}
}
public static boolean canSeeFocus(IJavaScriptElement focus, JavaProject javaProject, IIncludePathEntry[] focusEntriesForPolymorphicSearch) {
	try {
		if (focus.equals(javaProject))
			return true;

		if (focusEntriesForPolymorphicSearch != null) {
			// look for refering project
			IPath projectPath = javaProject.getProject().getFullPath();
			for (int i = 0, length = focusEntriesForPolymorphicSearch.length; i < length; i++) {
				IIncludePathEntry entry = focusEntriesForPolymorphicSearch[i];
				if (entry.getEntryKind() == IIncludePathEntry.CPE_PROJECT && entry.getPath().equals(projectPath))
					return true;
			}
		}
		if (focus instanceof LibraryFragmentRoot || focus instanceof PackageFragmentRoot) {
			// focus is part of a jar
			IPath focusPath = focus.getPath();
			IIncludePathEntry[] entries = javaProject.getExpandedClasspath();
			for (int i = 0, length = entries.length; i < length; i++) {
				IIncludePathEntry entry = entries[i];
				if ((entry.getEntryKind() == IIncludePathEntry.CPE_LIBRARY || entry.getEntryKind() == IIncludePathEntry.CPE_SOURCE) && entry.getPath().equals(focusPath))
					return true;
			}
			if(focus instanceof LibraryFragmentRoot)
				return false;
		}
		// look for dependent projects
		IPath focusPath = null;
		if(focus instanceof PackageFragmentRoot)
			focusPath = ((JavaProject) focus.getParent()).getProject().getFullPath();
		else
			focusPath = ((JavaProject) focus).getProject().getFullPath();
		IIncludePathEntry[] entries = javaProject.getExpandedClasspath();
		for (int i = 0, length = entries.length; i < length; i++) {
			IIncludePathEntry entry = entries[i];
			if (entry.getEntryKind() == IIncludePathEntry.CPE_PROJECT && entry.getPath().equals(focusPath))
				return true;
		}
		return false;
	} catch (JavaScriptModelException e) {
		return false;
	}
}
/*
 *  Compute the list of paths which are keying index files.
 */
private void initializeIndexLocations() {
	IPath[] projectsAndJars = this.searchScope.enclosingProjectsAndJars();
	IndexManager manager = JavaModelManager.getJavaModelManager().getIndexManager();
	SimpleSet locations = new SimpleSet();
	IJavaScriptElement focus = MatchLocator.projectOrJarFocus(this.pattern);
	if (focus == null) {
		for (int i = 0; i < projectsAndJars.length; i++)
			locations.add(manager.computeIndexLocation(projectsAndJars[i]));
	} else {
		try {
			// find the projects from projectsAndJars that see the focus then walk those projects looking for the jars from projectsAndJars
			int length = projectsAndJars.length;
			JavaProject[] projectsCanSeeFocus = new JavaProject[length];
			SimpleSet visitedProjects = new SimpleSet(length);
			int projectIndex = 0;
			SimpleSet jarsToCheck = new SimpleSet(length);
			IIncludePathEntry[] focusEntries = null;
			if (this.pattern instanceof MethodPattern) { // should consider polymorphic search for method patterns
				JavaProject focusProject = focus instanceof LibraryFragmentRoot || focus instanceof PackageFragmentRoot ? (JavaProject) focus.getParent() : (JavaProject) focus;
				focusEntries = focusProject.getExpandedClasspath();
			}
			IJavaScriptModel model = JavaModelManager.getJavaModelManager().getJavaModel();
			for (int i = 0; i < length; i++) {
				IPath path = projectsAndJars[i];
				JavaProject project = (JavaProject) getJavaProject(path, model);
				if (project != null) {
					visitedProjects.add(project);
					if (canSeeFocus(focus, project, focusEntries)) {
						locations.add(manager.computeIndexLocation(path));
						projectsCanSeeFocus[projectIndex++] = project;
					}
				} else {
					jarsToCheck.add(path);
				}
			}
			for (int i = 0; i < projectIndex && jarsToCheck.elementSize > 0; i++) {
				IIncludePathEntry[] entries = projectsCanSeeFocus[i].getResolvedClasspath();
				for (int j = entries.length; --j >= 0;) {
					IIncludePathEntry entry = entries[j];
					if (entry.getEntryKind() == IIncludePathEntry.CPE_LIBRARY) {
						IPath path = entry.getPath();
						if (jarsToCheck.includes(path)) {
							locations.add(manager.computeIndexLocation(entry.getPath()));
							jarsToCheck.remove(path);
						}
					}
				}
			}
			// jar files can be included in the search scope without including one of the projects that references them, so scan all projects that have not been visited
			if (jarsToCheck.elementSize > 0) {
				IJavaScriptProject[] allProjects = model.getJavaScriptProjects();
				for (int i = 0, l = allProjects.length; i < l && jarsToCheck.elementSize > 0; i++) {
					JavaProject project = (JavaProject) allProjects[i];
					if (!visitedProjects.includes(project)) {
						IIncludePathEntry[] entries = project.getResolvedClasspath();
						for (int j = entries.length; --j >= 0;) {
							IIncludePathEntry entry = entries[j];
							if (entry.getEntryKind() == IIncludePathEntry.CPE_LIBRARY) {
								IPath path = entry.getPath();
								if (jarsToCheck.includes(path)) {
									locations.add(manager.computeIndexLocation(entry.getPath()));
									jarsToCheck.remove(path);
								}
							}
						}
					}
				}
			}
		} catch (JavaScriptModelException e) {
			// ignored
		}
	}

	this.indexLocations = new IPath[locations.elementSize];
	Object[] values = locations.values;
	int count = 0;
	for (int i = values.length; --i >= 0;)
		if (values[i] != null)
			this.indexLocations[count++] = (IPath) values[i];
}
public IPath[] getIndexLocations() {
	if (this.indexLocations == null) {
		this.initializeIndexLocations();
	}
	return this.indexLocations;
}

/**
 * Returns the java project that corresponds to the given path.
 * Returns null if the path doesn't correspond to a project.
 */
private static IJavaScriptProject getJavaProject(IPath path, IJavaScriptModel model) {
	IJavaScriptProject project = model.getJavaScriptProject(path.segment(0)); // First path segment could be a project name 
	if (project.exists()) {
		return project;
	}
	return null;
}
}
