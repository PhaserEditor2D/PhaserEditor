/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.corext.refactoring;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchScope;
import org.eclipse.wst.jsdt.core.search.SearchEngine;
import org.eclipse.wst.jsdt.internal.corext.util.JdtFlags;

public class RefactoringScopeFactory {

	/*
	 * Adds to <code> projects </code> IJavaScriptProject objects for all projects directly or indirectly referencing focus. @param projects IJavaProjects will be added to this set
	 */
	private static void addReferencingProjects(IJavaScriptProject focus, Set projects) throws JavaScriptModelException {
		IProject[] referencingProjects= focus.getProject().getReferencingProjects();
		for (int i= 0; i < referencingProjects.length; i++) {
			IJavaScriptProject candidate= JavaScriptCore.create(referencingProjects[i]);
			if (candidate == null || projects.contains(candidate) || !candidate.exists())
				continue; // break cycle
			IIncludePathEntry entry= getReferencingClassPathEntry(candidate, focus);
			if (entry != null) {
				projects.add(candidate);
				if (entry.isExported())
					addReferencingProjects(candidate, projects);
			}
		}
	}

	private static void addRelatedReferencing(IJavaScriptProject focus, Set projects) throws CoreException {
		IProject[] referencingProjects= focus.getProject().getReferencingProjects();
		for (int i= 0; i < referencingProjects.length; i++) {
			IJavaScriptProject candidate= JavaScriptCore.create(referencingProjects[i]);
			if (candidate == null || projects.contains(candidate) || !candidate.exists())
				continue; // break cycle
			IIncludePathEntry entry= getReferencingClassPathEntry(candidate, focus);
			if (entry != null) {
				projects.add(candidate);
				if (entry.isExported()) {
					addRelatedReferencing(candidate, projects);
					addRelatedReferenced(candidate, projects);
				}
			}
		}
	}

	private static void addRelatedReferenced(IJavaScriptProject focus, Set projects) throws CoreException {
		IProject[] referencedProjects= focus.getProject().getReferencedProjects();
		for (int i= 0; i < referencedProjects.length; i++) {
			IJavaScriptProject candidate= JavaScriptCore.create(referencedProjects[i]);
			if (candidate == null || projects.contains(candidate) || !candidate.exists())
				continue; // break cycle
			IIncludePathEntry entry= getReferencingClassPathEntry(focus, candidate);
			if (entry != null) {
				projects.add(candidate);
				if (entry.isExported()) {
					addRelatedReferenced(candidate, projects);
					addRelatedReferencing(candidate, projects);
				}
			}
		}
	}

	/**
	 * Creates a new search scope with all compilation units possibly referencing <code>javaElement</code>,
	 * considering the visibility of the element.
	 * 
	 * @param javaElement the java element
	 * @return the search scope
	 * @throws JavaScriptModelException if an error occurs
	 */
	public static IJavaScriptSearchScope create(IJavaScriptElement javaElement) throws JavaScriptModelException {
		return RefactoringScopeFactory.create(javaElement, true);
	}
	
	/**
	 * Creates a new search scope with all compilation units possibly referencing <code>javaElement</code>.
	 * 
	 * @param javaElement the java element
	 * @param considerVisibility consider visibility of javaElement iff <code>true</code>
	 * @return the search scope
	 * @throws JavaScriptModelException if an error occurs
	 */
	public static IJavaScriptSearchScope create(IJavaScriptElement javaElement, boolean considerVisibility) throws JavaScriptModelException {
		if (considerVisibility & javaElement instanceof IMember) {
			IMember member= (IMember) javaElement;
			if (JdtFlags.isPrivate(member)) {
				if (member.getJavaScriptUnit() != null)
					return SearchEngine.createJavaSearchScope(new IJavaScriptElement[] { member.getJavaScriptUnit()});
				else
					return SearchEngine.createJavaSearchScope(new IJavaScriptElement[] { member});
			}
			// Removed code that does some optimizations regarding package visible members. The problem is that
			// there can be a package fragment with the same name in a different source folder or project. So we
			// have to treat package visible members like public or protected members.
		}
		return create(javaElement.getJavaScriptProject());
	}

	private static IJavaScriptSearchScope create(IJavaScriptProject javaProject) throws JavaScriptModelException {
		return SearchEngine.createJavaSearchScope(getAllScopeElements(javaProject), false);
	}

	/**
	 * Creates a new search scope comprising <code>members</code>.
	 * 
	 * @param members the members
	 * @return the search scope
	 * @throws JavaScriptModelException if an error occurs
	 */
	public static IJavaScriptSearchScope create(IMember[] members) throws JavaScriptModelException {
		Assert.isTrue(members != null && members.length > 0);
		IMember candidate= members[0];
		int visibility= getVisibility(candidate);
		for (int i= 1; i < members.length; i++) {
			int mv= getVisibility(members[i]);
			if (mv > visibility) {
				visibility= mv;
				candidate= members[i];
			}
		}
		return create(candidate);
	}

	/**
	 * Creates a new search scope with all projects possibly referenced
	 * from the given <code>javaElements</code>.
	 * 
	 * @param javaElements the java elements
	 * @return the search scope
	 */
	public static IJavaScriptSearchScope createReferencedScope(IJavaScriptElement[] javaElements) {
		Set projects= new HashSet();
		for (int i= 0; i < javaElements.length; i++) {
			projects.add(javaElements[i].getJavaScriptProject());
		}
		IJavaScriptProject[] prj= (IJavaScriptProject[]) projects.toArray(new IJavaScriptProject[projects.size()]);
		return SearchEngine.createJavaSearchScope(prj, true);
	}

	/**
	 * Creates a new search scope with all projects possibly referenced
	 * from the given <code>javaElements</code>.
	 * 
	 * @param javaElements the java elements
	 * @param includeMask the include mask
	 * @return the search scope
	 */
	public static IJavaScriptSearchScope createReferencedScope(IJavaScriptElement[] javaElements, int includeMask) {
		Set projects= new HashSet();
		for (int i= 0; i < javaElements.length; i++) {
			projects.add(javaElements[i].getJavaScriptProject());
		}
		IJavaScriptProject[] prj= (IJavaScriptProject[]) projects.toArray(new IJavaScriptProject[projects.size()]);
		return SearchEngine.createJavaSearchScope(prj, includeMask);
	}

	/**
	 * Creates a new search scope containing all projects which reference or are referenced by the specified project.
	 * 
	 * @param project the project
	 * @param includeMask the include mask
	 * @return the search scope
	 * @throws CoreException if a referenced project could not be determined
	 */
	public static IJavaScriptSearchScope createRelatedProjectsScope(IJavaScriptProject project, int includeMask) throws CoreException {
		IJavaScriptProject[] projects= getRelatedProjects(project);
		return SearchEngine.createJavaSearchScope(projects, includeMask);
	}

	private static IJavaScriptElement[] getAllScopeElements(IJavaScriptProject project) throws JavaScriptModelException {
		Collection sourceRoots= getAllSourceRootsInProjects(getReferencingProjects(project));
		return (IPackageFragmentRoot[]) sourceRoots.toArray(new IPackageFragmentRoot[sourceRoots.size()]);
	}

	/*
	 * @param projects a collection of IJavaScriptProject @return Collection a collection of IPackageFragmentRoot, one element for each packageFragmentRoot which lies within a project in <code> projects </code> .
	 */
	private static Collection getAllSourceRootsInProjects(Collection projects) throws JavaScriptModelException {
		List result= new ArrayList();
		for (Iterator it= projects.iterator(); it.hasNext();)
			result.addAll(getSourceRoots((IJavaScriptProject) it.next()));
		return result;
	}

	/*
	 * Finds, if possible, a classpathEntry in one given project such that this classpath entry references another given project. If more than one entry exists for the referenced project and at least one is exported, then an exported entry will be returned.
	 */
	private static IIncludePathEntry getReferencingClassPathEntry(IJavaScriptProject referencingProject, IJavaScriptProject referencedProject) throws JavaScriptModelException {
		IIncludePathEntry result= null;
		IPath path= referencedProject.getProject().getFullPath();
		IIncludePathEntry[] classpath= referencingProject.getResolvedIncludepath(true);
		for (int i= 0; i < classpath.length; i++) {
			IIncludePathEntry entry= classpath[i];
			if (entry.getEntryKind() == IIncludePathEntry.CPE_PROJECT && path.equals(entry.getPath())) {
				if (entry.isExported())
					return entry;
				// Consider it as a candidate. May be there is another entry that is
				// exported.
				result= entry;
			}
		}
		return result;
	}

	private static IJavaScriptProject[] getRelatedProjects(IJavaScriptProject focus) throws CoreException {
		final Set projects= new HashSet();

		addRelatedReferencing(focus, projects);
		addRelatedReferenced(focus, projects);

		projects.add(focus);
		return (IJavaScriptProject[]) projects.toArray(new IJavaScriptProject[projects.size()]);
	}

	private static Collection getReferencingProjects(IJavaScriptProject focus) throws JavaScriptModelException {
		Set projects= new HashSet();

		addReferencingProjects(focus, projects);
		projects.add(focus);
		return projects;
	}

	private static List getSourceRoots(IJavaScriptProject javaProject) throws JavaScriptModelException {
		List elements= new ArrayList();
		IPackageFragmentRoot[] roots= javaProject.getPackageFragmentRoots();
		// Add all package fragment roots except archives
		for (int i= 0; i < roots.length; i++) {
			IPackageFragmentRoot root= roots[i];
			if (!root.isArchive())
				elements.add(root);
		}
		return elements;
	}

	private static int getVisibility(IMember member) throws JavaScriptModelException {
		if (JdtFlags.isPrivate(member))
			return 0;
		if (JdtFlags.isPackageVisible(member))
			return 1;
		return 4;
	}

	private RefactoringScopeFactory() {
		// no instances
	}
}
