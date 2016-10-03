/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Matt Chapman, mpchapman@gmail.com - 89977 Make JDT .java agnostic
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.corext.buildpath;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptModelStatus;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.JavaScriptConventions;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.wst.jsdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths.CPListElement;
/**
*
* Provisional API: This class/interface is part of an interim API that is still under development and expected to
* change significantly before reaching stability. It is being made available at this early stage to solicit feedback
* from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
* (repeatedly) as the API evolves.
*/
public class ClasspathModifier {

	private ClasspathModifier() {}
    
    public static BuildpathDelta removeFromBuildpath(CPListElement[] toRemove, CPJavaProject cpProject) {
    	
        IJavaScriptProject javaScriptProject= cpProject.getJavaProject();
		IPath projectPath= javaScriptProject.getPath();
        IWorkspaceRoot workspaceRoot= javaScriptProject.getProject().getWorkspace().getRoot();
        
    	List existingEntries= cpProject.getCPListElements();
		BuildpathDelta result= new BuildpathDelta(NewWizardMessages.NewSourceContainerWorkbookPage_ToolBar_RemoveFromCP_tooltip);
		
		for (int i= 0; i < toRemove.length; i++) {
	        CPListElement element= toRemove[i];
	        existingEntries.remove(element);
	        result.removeEntry(element);
	        IPath path= element.getPath();
			removeFilters(path, javaScriptProject, existingEntries);
			if (!path.equals(projectPath)) {
	            IResource member= workspaceRoot.findMember(path);
	            if (member != null)
	            	result.addDeletedResource(member);
            }
        }
		
    	result.setNewEntries((CPListElement[])existingEntries.toArray(new CPListElement[existingEntries.size()]));

	    return result;
    }

	/**
	 * Get the <code>IIncludePathEntry</code> from the project and 
	 * convert it into a list of <code>CPListElement</code>s.
	 * 
	 * @param project the JavaScript project to get it's include path entries from
	 * @return a list of <code>CPListElement</code>s corresponding to the 
	 * include path entries of the project
	 * @throws JavaScriptModelException
	 */
	public static List getExistingEntries(IJavaScriptProject project) throws JavaScriptModelException {
		IIncludePathEntry[] classpathEntries= project.getRawIncludepath();
		ArrayList newClassPath= new ArrayList();
		for (int i= 0; i < classpathEntries.length; i++) {
			IIncludePathEntry curr= classpathEntries[i];
			newClassPath.add(CPListElement.createFromExisting(curr, project));
		}
		return newClassPath;
	}

	/**
	 * Try to find the corresponding and modified <code>CPListElement</code> for the root 
	 * in the list of elements and return it.
	 * If no one can be found, the roots <code>ClasspathEntry</code> is converted to a 
	 * <code>CPListElement</code> and returned.
	 * 
	 * @param elements a list of <code>CPListElements</code>
	 * @param root the root to find the <code>ClasspathEntry</code> for represented by 
	 * a <code>CPListElement</code>
	 * @return the <code>CPListElement</code> found in the list (matching by using the path) or 
	 * the roots own <code>IIncludePathEntry</code> converted to a <code>CPListElement</code>.
	 * @throws JavaScriptModelException
	 */
	public static CPListElement getClasspathEntry(List elements, IPackageFragmentRoot root) throws JavaScriptModelException {
		IIncludePathEntry entry= root.getRawIncludepathEntry();
		for (int i= 0; i < elements.size(); i++) {
			CPListElement element= (CPListElement) elements.get(i);
			if (element.getPath().equals(root.getPath()) && element.getEntryKind() == entry.getEntryKind())
				return (CPListElement) elements.get(i);
		}
		CPListElement newElement= CPListElement.createFromExisting(entry, root.getJavaScriptProject());
		elements.add(newElement);
		return newElement;
	}

	/**
	 * Get the source folder of a given <code>IResource</code> element,
	 * starting with the resource's parent.
	 * 
	 * @param resource the resource to get the fragment root from
	 * @param project the JavaScript project
	 * @param monitor progress monitor, can be <code>null</code>
	 * @return resolved fragment root, or <code>null</code> the resource is not (in) a source folder
	 * @throws JavaScriptModelException
	 */
	public static IPackageFragmentRoot getFragmentRoot(IResource resource, IJavaScriptProject project, IProgressMonitor monitor) throws JavaScriptModelException {
		if (monitor == null)
			monitor= new NullProgressMonitor();
		IJavaScriptElement javaElem= null;
		if (resource.getFullPath().equals(project.getPath()))
			return project.getPackageFragmentRoot(resource);
		IContainer container= resource.getParent();
		do {
			if (container instanceof IFolder)
				javaElem= JavaScriptCore.create((IFolder) container);
			if (container.getFullPath().equals(project.getPath())) {
				javaElem= project;
				break;
			}
			container= container.getParent();
			if (container == null)
				return null;
		} while (javaElem == null || !(javaElem instanceof IPackageFragmentRoot));
		if (javaElem instanceof IJavaScriptProject) {
			if (!isSourceFolder((IJavaScriptProject)javaElem))
				return null;
			javaElem= project.getPackageFragmentRoot(project.getResource());
		}
		return (IPackageFragmentRoot) javaElem;
	}

	/**
	 * Get the <code>IIncludePathEntry</code> for the
	 * given path by looking up all
	 * build path entries on the project
	 * 
	 * @param path the path to find a build path entry for
	 * @param project the JavaScript project
	 * @return the <code>IIncludePathEntry</code> corresponding
	 * to the <code>path</code> or <code>null</code> if there
	 * is no such entry
	 * @throws JavaScriptModelException
	 */
	public static IIncludePathEntry getClasspathEntryFor(IPath path, IJavaScriptProject project, int entryKind) throws JavaScriptModelException {
		IIncludePathEntry[] entries= project.getRawIncludepath();
		for (int i= 0; i < entries.length; i++) {
			IIncludePathEntry entry= entries[i];
			if (entry.getPath().equals(path) && equalEntryKind(entry, entryKind))
				return entry;
		}
		return null;
	}

	/**
	 * Determines whether the current selection (of type
	 * <code>IJavaScriptUnit</code> or <code>IPackageFragment</code>)
	 * is on the inclusion filter of it's parent source folder.
	 * 
	 * @param selection the current Java element
	 * @param project the Java project
	 * @param monitor progress monitor, can be <code>null</code>
	 * @return <code>true</code> if the current selection is included,
	 * <code>false</code> otherwise.
	 * @throws JavaScriptModelException 
	 */
	public static boolean isIncluded(IJavaScriptElement selection, IJavaScriptProject project, IProgressMonitor monitor) throws JavaScriptModelException {
		if (monitor == null)
			monitor= new NullProgressMonitor();
		try {
			monitor.beginTask(NewWizardMessages.ClasspathModifier_Monitor_ContainsPath, 4); 
			IPackageFragmentRoot root= (IPackageFragmentRoot) selection.getAncestor(IJavaScriptElement.PACKAGE_FRAGMENT_ROOT);
			IIncludePathEntry entry= root.getRawIncludepathEntry();
			if (entry == null)
				return false;
			return contains(selection.getPath().removeFirstSegments(root.getPath().segmentCount()), entry.getInclusionPatterns(), new SubProgressMonitor(monitor, 2));
		} finally {
			monitor.done();
		}
	}

	/**
	 * Find out whether the <code>IResource</code> excluded or not.
	 * 
	 * @param resource the resource to be checked
	 * @param project the Java project
	 * @return <code>true</code> if the resource is excluded, <code>
	 * false</code> otherwise
	 * @throws JavaScriptModelException
	 */
	public static boolean isExcluded(IResource resource, IJavaScriptProject project) throws JavaScriptModelException {
		IPackageFragmentRoot root= getFragmentRoot(resource, project, null);
		if (root == null)
			return false;
		String fragmentName= getName(resource.getFullPath(), root.getPath());
		fragmentName= completeName(fragmentName);
		IIncludePathEntry entry= root.getRawIncludepathEntry();
		return entry != null && contains(new Path(fragmentName), entry.getExclusionPatterns(), null);
	}

	/**
	 * Find out whether one of the <code>IResource</code>'s parents
	 * is excluded.
	 * 
	 * @param resource check the resources parents whether they are
	 * excluded or not
	 * @param project the Java project
	 * @return <code>true</code> if there is an excluded parent, 
	 * <code>false</code> otherwise
	 * @throws JavaScriptModelException
	 */
	public static boolean parentExcluded(IResource resource, IJavaScriptProject project) throws JavaScriptModelException {
		if (resource.getFullPath().equals(project.getPath()))
			return false;
		IPackageFragmentRoot root= getFragmentRoot(resource, project, null);
		if (root == null) {
			return true;
		}
		IPath path= resource.getFullPath().removeFirstSegments(root.getPath().segmentCount());
		IIncludePathEntry entry= root.getRawIncludepathEntry();
		if (entry == null)
			return true; // there is no build path entry, this is equal to the fact that the parent is excluded
		while (path.segmentCount() > 0) {
			if (contains(path, entry.getExclusionPatterns(), null))
				return true;
			path= path.removeLastSegments(1);
		}
		return false;
	}
	
	public static String escapeSpecialChars(String value) {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);

			switch (c) {
			case '&':
				buf.append("&amp;"); //$NON-NLS-1$
				break;
			case '<':
				buf.append("&lt;"); //$NON-NLS-1$
				break;
			case '>':
				buf.append("&gt;"); //$NON-NLS-1$
				break;
			case '\'':
				buf.append("&apos;"); //$NON-NLS-1$
				break;
			case '\"':
				buf.append("&quot;"); //$NON-NLS-1$
				break;
			case 160:
				buf.append(" "); //$NON-NLS-1$
				break;
			default:
				buf.append(c);
				break;
			}
		}
		return buf.toString();
	}
	

	/**
	 * Check whether the <code>IJavaScriptProject</code>
	 * is a source folder 
	 * 
	 * @param project the project to test
	 * @return <code>true</code> if <code>project</code> is a source folder
	 * <code>false</code> otherwise.
	 */
	public static boolean isSourceFolder(IJavaScriptProject project) throws JavaScriptModelException {
		return ClasspathModifier.getClasspathEntryFor(project.getPath(), project, IIncludePathEntry.CPE_SOURCE) != null;
	}
	
	/**
	 * Check whether the <code>IPackageFragment</code>
	 * corresponds to the project's default fragment.
	 * 
	 * @param fragment the package fragment to be checked
	 * @return <code>true</code> if is the default package fragment,
	 * <code>false</code> otherwise.
	 */
	public static boolean isDefaultFragment(IPackageFragment fragment) {
		return fragment.getElementName().length() == 0;
	}

	/**
	 * Determines whether the inclusion filter of the element's source folder is empty
	 * or not
	 * @return <code>true</code> if the inclusion filter is empty,
	 * <code>false</code> otherwise.
	 * @throws JavaScriptModelException 
	 */
	public static boolean includeFiltersEmpty(IResource resource, IJavaScriptProject project, IProgressMonitor monitor) throws JavaScriptModelException {
		if (monitor == null)
			monitor= new NullProgressMonitor();
		try {
			monitor.beginTask(NewWizardMessages.ClasspathModifier_Monitor_ExamineInputFilters, 4); 
			IPackageFragmentRoot root= getFragmentRoot(resource, project, new SubProgressMonitor(monitor, 4));
			if (root != null) {
				IIncludePathEntry entry= root.getRawIncludepathEntry();
				return entry.getInclusionPatterns().length == 0;
			}
			return true;
		} finally {
			monitor.done();
		}
	}

	/**
	 * Check whether the input paramenter of type <code>
	 * IPackageFragmentRoot</code> has either it's inclusion or
	 * exclusion filter or both set (that means they are
	 * not empty).
	 * 
	 * @param root the fragment root to be inspected
	 * @return <code>true</code> inclusion or exclusion filter set,
	 * <code>false</code> otherwise.
	 */
	public static boolean filtersSet(IPackageFragmentRoot root) throws JavaScriptModelException {
		if (root == null)
			return false;
		IIncludePathEntry entry= root.getRawIncludepathEntry();
		IPath[] inclusions= entry.getInclusionPatterns();
		IPath[] exclusions= entry.getExclusionPatterns();
		if (inclusions != null && inclusions.length > 0)
			return true;
		if (exclusions != null && exclusions.length > 0)
			return true;
		return false;
	}

	/**
	 * Add a resource to the build path.
	 * 
	 * @param resource the resource to be added to the build path
	 * @param project the Java project
	 * @param monitor progress monitor, can be <code>null</code> 
	 * @return returns the new element of type <code>IPackageFragmentRoot</code> that has been added to the build path
	 * @throws CoreException 
	 * @throws OperationCanceledException 
	 */
	public static CPListElement addToClasspath(IResource resource, List existingEntries, List newEntries, IJavaScriptProject project, IProgressMonitor monitor) throws OperationCanceledException, CoreException {
		if (monitor == null)
			monitor= new NullProgressMonitor();
		try {
			monitor.beginTask(NewWizardMessages.ClasspathModifier_Monitor_AddToBuildpath, 2); 
			exclude(resource.getFullPath(), existingEntries, newEntries, project, new SubProgressMonitor(monitor, 1));
			CPListElement entry= new CPListElement(project, IIncludePathEntry.CPE_SOURCE, resource.getFullPath(), resource);
			return entry;
		} finally {
			monitor.done();
		}
	}

	/**
	 * Add a Java element to the build path.
	 * 
	 * @param javaElement element to be added to the build path
	 * @param project the Java project
	 * @param monitor progress monitor, can be <code>null</code> 
	 * @return returns the new element of type <code>IPackageFragmentRoot</code> that has been added to the build path
	 * @throws CoreException 
	 * @throws OperationCanceledException 
	 */
	public static CPListElement addToClasspath(IJavaScriptElement javaElement, List existingEntries, List newEntries, IJavaScriptProject project, IProgressMonitor monitor) throws OperationCanceledException, CoreException {
		if (monitor == null)
			monitor= new NullProgressMonitor();
		try {
			monitor.beginTask(NewWizardMessages.ClasspathModifier_Monitor_AddToBuildpath, 10); 
			CPListElement entry= new CPListElement(project, IIncludePathEntry.CPE_SOURCE, javaElement.getPath(), javaElement.getResource());
			return entry;
		} finally {
			monitor.done();
		}
	}

	/**
	 * Remove the Java project from the build path
	 * 
	 * @param project the project to be removed
	 * @param existingEntries a list of existing <code>CPListElement</code>. This list 
	 * will be traversed and the entry for the project will be removed.
	 * @param monitor progress monitor, can be <code>null</code>
	 * @return returns the Java project
	 */
	public static IJavaScriptProject removeFromClasspath(IJavaScriptProject project, List existingEntries, IProgressMonitor monitor) {
		CPListElement elem= getListElement(project.getPath(), existingEntries);
		if (elem != null) {
			existingEntries.remove(elem);
		}
		return project;
	}
	
	/**
	 * Remove <code>path</code> from inclusion/exlusion filters in all <code>existingEntries</code>
	 * 
	 * @param path the path to remove
	 * @param project the Java project
	 * @param existingEntries a list of <code>CPListElement</code> representing the build path
	 * entries of the project.
	 * @return returns a <code>List</code> of <code>CPListElement</code> of modified elements, not null.
	 */
	public static List removeFilters(IPath path, IJavaScriptProject project, List existingEntries) {
		if (path == null)
			return Collections.EMPTY_LIST;
		
		IPath projPath= project.getPath();
		if (projPath.isPrefixOf(path)) {
			path= path.removeFirstSegments(projPath.segmentCount()).addTrailingSeparator();
		}
		
		List result= new ArrayList();
		for (Iterator iter= existingEntries.iterator(); iter.hasNext();) {
			CPListElement element= (CPListElement)iter.next();
			boolean hasChange= false;
			IPath[] exlusions= (IPath[])element.getAttribute(CPListElement.EXCLUSION);
			if (exlusions != null) {
				List exlusionList= new ArrayList(exlusions.length);
				for (int i= 0; i < exlusions.length; i++) {
					if (!exlusions[i].equals(path)) {
						exlusionList.add(exlusions[i]);
					} else {
						hasChange= true;
					}
				}
				element.setAttribute(CPListElement.EXCLUSION, exlusionList.toArray(new IPath[exlusionList.size()]));
			}
			
			IPath[] inclusion= (IPath[])element.getAttribute(CPListElement.INCLUSION);
			if (inclusion != null) {
				List inclusionList= new ArrayList(inclusion.length);
				for (int i= 0; i < inclusion.length; i++) {
					if (!inclusion[i].equals(path)) {
						inclusionList.add(inclusion[i]);
					} else {
						hasChange= true;
					}
				}
				element.setAttribute(CPListElement.INCLUSION, inclusionList.toArray(new IPath[inclusionList.size()]));
			}
			if (hasChange) {
				result.add(element);
			}
		}
		return result;
	}

	/**
	 * Exclude an element with a given name and absolute path
	 * from the build path.
	 * 
	 * @param name the name of the element to be excluded
	 * @param fullPath the absolute path of the element
	 * @param entry the build path entry to be modified
	 * @param project the Java project
	 * @param monitor progress monitor, can be <code>null</code>
	 * @return a <code>IResource</code> corresponding to the excluded element
	 * @throws JavaScriptModelException 
	 */
	private static IResource exclude(String name, IPath fullPath, CPListElement entry, IJavaScriptProject project, IProgressMonitor monitor) throws JavaScriptModelException {
		if (monitor == null)
			monitor= new NullProgressMonitor();
		IResource result;
		try {
			monitor.beginTask(NewWizardMessages.ClasspathModifier_Monitor_Excluding, 6); 
			IPath[] excludedPath= (IPath[]) entry.getAttribute(CPListElement.EXCLUSION);
			IPath[] newExcludedPath= new IPath[excludedPath.length + 1];
			name= completeName(name);
			IPath path= new Path(name);
			if (!contains(path, excludedPath, new SubProgressMonitor(monitor, 2))) {
				System.arraycopy(excludedPath, 0, newExcludedPath, 0, excludedPath.length);
				newExcludedPath[excludedPath.length]= path;
				entry.setAttribute(CPListElement.EXCLUSION, newExcludedPath);
				entry.setAttribute(CPListElement.INCLUSION, remove(path, (IPath[]) entry.getAttribute(CPListElement.INCLUSION), new SubProgressMonitor(monitor, 4)));
			}
			result= fullPath == null ? null : getResource(fullPath, project);
		} finally {
			monitor.done();
		}
		return result;
	}

	/**
	 * Exclude an object at a given path.
	 * This means that the exclusion filter for the
	 * corresponding <code>IPackageFragmentRoot</code> needs to be modified.
	 * 
	 * First, the fragment root needs to be found. To do so, the new entries 
	 * are and the existing entries are traversed for a match and the entry 
	 * with the path is removed from one of those lists.
	 * 
	 * Note: the <code>IJavaScriptElement</code>'s fragment (if there is one)
	 * is not allowed to be excluded! However, inclusion (or simply no
	 * filter) on the parent fragment is allowed.
	 * 
	 * @param path absolute path of an object to be excluded
	 * @param existingEntries a list of existing build path entries
	 * @param newEntries a list of new build path entries
	 * @param project the Java project
	 * @param monitor progress monitor, can be <code>null</code>
	 */
	public static void exclude(IPath path, List existingEntries, List newEntries, IJavaScriptProject project, IProgressMonitor monitor) throws JavaScriptModelException {
		if (monitor == null)
			monitor= new NullProgressMonitor();
		try {
			monitor.beginTask(NewWizardMessages.ClasspathModifier_Monitor_Excluding, 1); 
			CPListElement elem= null;
			CPListElement existingElem= null;
			int i= 0;
			do {
				i++;
				IPath rootPath= path.removeLastSegments(i);

				if (rootPath.segmentCount() == 0)
					return;

				elem= getListElement(rootPath, newEntries);
				existingElem= getListElement(rootPath, existingEntries);
			} while (existingElem == null && elem == null);
			if (elem == null) {
				elem= existingElem;
			}
			exclude(path.removeFirstSegments(path.segmentCount() - i).toString(), null, elem, project, new SubProgressMonitor(monitor, 1)); 
		} finally {
			monitor.done();
		}
	}

	/**
	 * Exclude a <code>IJavaScriptElement</code>. This means that the exclusion filter for the
	 * corresponding <code>IPackageFragmentRoot</code>s need to be modified.
	 * 
	 * Note: the <code>IJavaScriptElement</code>'s fragment (if there is one)
	 * is not allowed to be excluded! However, inclusion (or simply no
	 * filter) on the parent fragment is allowed.
	 * 
	 * @param javaElement the Java element to be excluded
	 * @param entry the <code>CPListElement</code> representing the 
	 * <code>IIncludePathEntry</code> of the Java element's root.
	 * @param project the Java project
	 * @param monitor progress monitor, can be <code>null</code>
	 * 
	 * @return the resulting <code>IResource<code>
	 * @throws JavaScriptModelException
	 */
	public static IResource exclude(IJavaScriptElement javaElement, CPListElement entry, IJavaScriptProject project, IProgressMonitor monitor) throws JavaScriptModelException {
		if (monitor == null)
			monitor= new NullProgressMonitor();
		try {
			String name= getName(javaElement.getPath(), entry.getPath());
			return exclude(name, javaElement.getPath(), entry, project, new SubProgressMonitor(monitor, 1));
		} finally {
			monitor.done();
		}
	}

	/**
	 * Inverse operation to <code>exclude</code>.
	 * The resource removed from it's fragment roots exlusion filter.
	 * 
	 * Note: the <code>IJavaScriptElement</code>'s fragment (if there is one)
	 * is not allowed to be excluded! However, inclusion (or simply no
	 * filter) on the parent fragment is allowed.
	 * 
	 * @param resource the resource to be unexcluded
	 * @param entry the <code>CPListElement</code> representing the 
	 * <code>IIncludePathEntry</code> of the resource's root.
	 * @param project the Java project
	 * @param monitor progress monitor, can be <code>null</code>
	 * @throws JavaScriptModelException
	 * 
	 */
	public static void unExclude(IResource resource, CPListElement entry, IJavaScriptProject project, IProgressMonitor monitor) throws JavaScriptModelException {
		if (monitor == null)
			monitor= new NullProgressMonitor();
		try {
			monitor.beginTask(NewWizardMessages.ClasspathModifier_Monitor_RemoveExclusion, 10); 
			String name= getName(resource.getFullPath(), entry.getPath());
			IPath[] excludedPath= (IPath[]) entry.getAttribute(CPListElement.EXCLUSION);
			IPath[] newExcludedPath= remove(new Path(completeName(name)), excludedPath, new SubProgressMonitor(monitor, 3));
			entry.setAttribute(CPListElement.EXCLUSION, newExcludedPath);
		} finally {
			monitor.done();
		}
	}

	/**
	 * Resets inclusion and exclusion filters for the given
	 * <code>IJavaScriptElement</code>
	 * 
	 * @param element element to reset it's filters
	 * @param entry the <code>CPListElement</code> to reset its filters for
	 * @param project the Java project
	 * @param monitor progress monitor, can be <code>null</code>
	 * @throws JavaScriptModelException
	 */
	public static void resetFilters(IJavaScriptElement element, CPListElement entry, IJavaScriptProject project, IProgressMonitor monitor) throws JavaScriptModelException {
		if (monitor == null)
			monitor= new NullProgressMonitor();
		try {
			monitor.beginTask(NewWizardMessages.ClasspathModifier_Monitor_ResetFilters, 3); 

			List exclusionList= getFoldersOnCP(element.getPath(), project, new SubProgressMonitor(monitor, 2));
			
			IPath[] exclusions= (IPath[]) exclusionList.toArray(new IPath[exclusionList.size()]);

			entry.setAttribute(CPListElement.INCLUSION, new IPath[0]);
			entry.setAttribute(CPListElement.EXCLUSION, exclusions);
		} finally {
			monitor.done();
		}
	}

	/**
	 * Try to find the corresponding and modified <code>CPListElement</code> for the provided 
	 * <code>CPListElement</code> in the list of elements and return it.
	 * If no one can be found, the provided <code>CPListElement</code> is returned.
	 * 
	 * @param elements a list of <code>CPListElements</code>
	 * @param cpElement the <code>CPListElement</code> to find the corresponding entry in 
	 * the list
	 * @return the <code>CPListElement</code> found in the list (matching by using the path) or 
	 * the second <code>CPListElement</code> parameter itself if there is no match.
	 */
	public static CPListElement getClasspathEntry(List elements, CPListElement cpElement) {
		for (int i= 0; i < elements.size(); i++) {
			if (((CPListElement) elements.get(i)).getPath().equals(cpElement.getPath()))
				return (CPListElement) elements.get(i);
		}
		elements.add(cpElement);
		return cpElement;
	}

	/**
	 * For a given path, find the corresponding element in the list.
	 * 
	 * @param path the path to found an entry for
	 * @param elements a list of <code>CPListElement</code>s
	 * @return the mathed <code>CPListElement</code> or <code>null</code> if 
	 * no match could be found
	 */
	public static CPListElement getListElement(IPath path, List elements) {
		for (int i= 0; i < elements.size(); i++) {
			CPListElement element= (CPListElement) elements.get(i);
			if (element.getEntryKind() == IIncludePathEntry.CPE_SOURCE && element.getPath().equals(path)) {
				return element;
			}
		}
		return null;
	}
	
	public static void commitClassPath(List newEntries, IJavaScriptProject project, IProgressMonitor monitor) throws JavaScriptModelException {
		if (monitor == null)
			monitor= new NullProgressMonitor();
		
		monitor.beginTask("", 2); //$NON-NLS-1$
		
		try {
			IIncludePathEntry[] entries= convert(newEntries);

			IJavaScriptModelStatus status= JavaScriptConventions.validateClasspath(project, entries);
			if (!status.isOK())
				throw new JavaScriptModelException(status);

			project.setRawIncludepath(entries, new SubProgressMonitor(monitor, 2));
		} finally {
			monitor.done();
		}
	}
	
    public static void commitClassPath(CPJavaProject cpProject, IProgressMonitor monitor) throws JavaScriptModelException {
		if (monitor == null)
			monitor= new NullProgressMonitor();
		
		monitor.beginTask("", 2); //$NON-NLS-1$
		
		try {
			IIncludePathEntry[] entries= convert(cpProject.getCPListElements());

			IJavaScriptModelStatus status= JavaScriptConventions.validateClasspath(cpProject.getJavaProject(), entries);
			if (!status.isOK())
				throw new JavaScriptModelException(status);

			cpProject.getJavaProject().setRawIncludepath(entries, new SubProgressMonitor(monitor, 2));
		} finally {
			monitor.done();
		}
    }

	/**
	 * For a given list of entries, find out what representation they 
	 * will have in the project and return a list with corresponding 
	 * elements.
	 * 
	 * @param entries a list of entries to find an appropriate representation 
	 * for. The list can contain elements of two types: 
	 * <li><code>IResource</code></li>
	 * <li><code>IJavaScriptElement</code></li>
	 * @param project the Java project
	 * @return a list of elements corresponding to the passed entries.
	 */
	public static List getCorrespondingElements(List entries, IJavaScriptProject project) {
		List result= new ArrayList();
		for (int i= 0; i < entries.size(); i++) {
			Object element= entries.get(i);
			IPath path;
			if (element instanceof IResource)
				path= ((IResource) element).getFullPath();
			else
				path= ((IJavaScriptElement) element).getPath();
			IResource resource= getResource(path, project);
			if (resource != null) {
				IJavaScriptElement elem= JavaScriptCore.create(resource);
				if (elem != null && project.isOnIncludepath(elem))
					result.add(elem);
				else
					result.add(resource);
			}

		}
		return result;
	}

	/**
	 * Returns for the given absolute path the corresponding
	 * resource, this is either element of type <code>IFile</code>
	 * or <code>IFolder</code>.
	 *  
	 * @param path an absolute path to a resource
	 * @param project the Java project
	 * @return the resource matching to the path. Can be
	 * either an <code>IFile</code> or an <code>IFolder</code>.
	 */
	private static IResource getResource(IPath path, IJavaScriptProject project) {
		return project.getProject().getWorkspace().getRoot().findMember(path);
	}

	/**
	 * Find out whether the provided path equals to one
	 * in the array.
	 * 
	 * @param path path to find an equivalent for
	 * @param paths set of paths to compare with
	 * @param monitor progress monitor, can be <code>null</code>
	 * @return <code>true</code> if there is an occurrence, <code>
	 * false</code> otherwise
	 */
	private static boolean contains(IPath path, IPath[] paths, IProgressMonitor monitor) {
		if (monitor == null)
			monitor= new NullProgressMonitor();
		if (path == null)
			return false;
		try {
			monitor.beginTask(NewWizardMessages.ClasspathModifier_Monitor_ComparePaths, paths.length); 
			if (path.getFileExtension() == null)
				path= new Path(completeName(path.toString())); 
			for (int i= 0; i < paths.length; i++) {
				if (paths[i].equals(path))
					return true;
				monitor.worked(1);
			}
		} finally {
			monitor.done();
		}
		return false;
	}

	/**
	 * Add a '/' at the end of the name if
	 * it does not end with '.java', or other Java-like extension.
	 * 
	 * @param name append '/' at the end if
	 * necessary
	 * @return modified string
	 */
	private static String completeName(String name) {
		if (!JavaScriptCore.isJavaScriptLikeFileName(name)) {
			name= name + "/"; //$NON-NLS-1$
			name= name.replace('.', '/');
			return name;
		}
		return name;
	}

	/**
	 * Removes <code>path</code> out of the set of given <code>
	 * paths</code>. If the path is not contained, then the 
	 * initially provided array of paths is returned.
	 * 
	 * Only the first occurrence will be removed.
	 * 
	 * @param path path to be removed
	 * @param paths array of path to apply the removal on
	 * @param monitor progress monitor, can be <code>null</code>
	 * @return array which does not contain <code>path</code>
	 */
	private static IPath[] remove(IPath path, IPath[] paths, IProgressMonitor monitor) {
		if (monitor == null)
			monitor= new NullProgressMonitor();
		try {
			monitor.beginTask(NewWizardMessages.ClasspathModifier_Monitor_RemovePath, paths.length + 5); 
			if (!contains(path, paths, new SubProgressMonitor(monitor, 5)))
				return paths;

			ArrayList newPaths= new ArrayList();
			for (int i= 0; i < paths.length; i++) {
				monitor.worked(1);
				if (!paths[i].equals(path))
					newPaths.add(paths[i]);
			}
			
			return (IPath[]) newPaths.toArray(new IPath[newPaths.size()]);
		} finally {
			monitor.done();
		}

	}

	/**
	 * Find all folders that are on the build path and
	 * <code>path</code> is a prefix of those folders
	 * path entry, that is, all folders which are a
	 * subfolder of <code>path</code>.
	 * 
	 * For example, if <code>path</code>=/MyProject/src 
	 * then all folders having a path like /MyProject/src/*,
	 * where * can be any valid string are returned if
	 * they are also on the project's build path.
	 * 
	 * @param path absolute path
	 * @param project the Java project
	 * @param monitor progress monitor, can be <code>null</code>
	 * @return an array of paths which belong to subfolders
	 * of <code>path</code> and which are on the build path
	 * @throws JavaScriptModelException
	 */
	private static List getFoldersOnCP(IPath path, IJavaScriptProject project, IProgressMonitor monitor) throws JavaScriptModelException {
		if (monitor == null)
			monitor= new NullProgressMonitor();
		List srcFolders= new ArrayList();
		IIncludePathEntry[] cpEntries= project.getRawIncludepath();
		for (int i= 0; i < cpEntries.length; i++) {
			IPath cpPath= cpEntries[i].getPath();
			if (path.isPrefixOf(cpPath) && path.segmentCount() + 1 == cpPath.segmentCount())
				srcFolders.add(new Path(completeName(cpPath.lastSegment())));
		}
		return srcFolders;
	}

	/**
	 * Returns a string corresponding to the <code>path</code>
	 * with the <code>rootPath<code>'s number of segments
	 * removed
	 * 
	 * @param path path to remove segments
	 * @param rootPath provides the number of segments to
	 * be removed
	 * @return a string corresponding to the mentioned
	 * action
	 */
	private static String getName(IPath path, IPath rootPath) {
		return path.removeFirstSegments(rootPath.segmentCount()).toString();
	}

	/**
	 * Sets and validates the new entries. Note that the elments of 
	 * the list containing the new entries will be added to the list of 
	 * existing entries (therefore, there is no return list for this method).
	 * 
	 * @param existingEntries a list of existing classpath entries
	 * @param newEntries a list of entries to be added to the existing ones
	 * @param project the Java project
	 * @param monitor a progress monitor, can be <code>null</code>
	 * @throws CoreException in case that validation on one of the new entries fails
	 */
	public static void setNewEntry(List existingEntries, List newEntries, IJavaScriptProject project, IProgressMonitor monitor) throws CoreException {
		try {
			monitor.beginTask(NewWizardMessages.ClasspathModifier_Monitor_SetNewEntry, existingEntries.size()); 
			for (int i= 0; i < newEntries.size(); i++) {
				CPListElement entry= (CPListElement) newEntries.get(i);
				validateAndAddEntry(entry, existingEntries, project);
				monitor.worked(1);
			}
		} finally {
			monitor.done();
		}
	}

	/**
	 * Convert a list of <code>CPListElement</code>s to 
	 * an array of <code>IIncludePathEntry</code>.
	 * 
	 * @param list the list to be converted
	 * @return an array containing build path entries 
	 * corresponding to the list
	 */
	private static IIncludePathEntry[] convert(List list) {
		IIncludePathEntry[] entries= new IIncludePathEntry[list.size()];
		for (int i= 0; i < list.size(); i++) {
			CPListElement element= (CPListElement) list.get(i);
			entries[i]= element.getClasspathEntry();
		}
		return entries;
	}

	/**
	 * Validate the new entry in the context of the existing entries. Furthermore, 
	 * check if exclusion filters need to be applied and do so if necessary.
	 * 
	 * If validation was successfull, add the new entry to the list of existing entries.
	 * 
	 * @param entry the entry to be validated and added to the list of existing entries.
	 * @param existingEntries a list of existing entries representing the build path
	 * @param project the Java project
	 * @throws CoreException in case that validation fails
	 */
	private static void validateAndAddEntry(CPListElement entry, List existingEntries, IJavaScriptProject project) throws CoreException {
		IPath path= entry.getPath();
		IWorkspaceRoot workspaceRoot= ResourcesPlugin.getWorkspace().getRoot();
		IStatus validate= workspaceRoot.getWorkspace().validatePath(path.toString(), IResource.FOLDER);
		StatusInfo rootStatus= new StatusInfo();
		rootStatus.setOK();
		boolean isExternal= isExternalArchiveOrLibrary(entry, project);
		if (!isExternal && validate.matches(IStatus.ERROR) && !project.getPath().equals(path)) {
			rootStatus.setError(Messages.format(NewWizardMessages.NewSourceFolderWizardPage_error_InvalidRootName, validate.getMessage())); 
			throw new CoreException(rootStatus);
		} else {
			if (!isExternal && !project.getPath().equals(path)) {
				IResource res= workspaceRoot.findMember(path);
				if (res != null) {
					if (res.getType() != IResource.FOLDER && res.getType() != IResource.FILE) {
						rootStatus.setError(NewWizardMessages.NewSourceFolderWizardPage_error_NotAFolder); 
						throw new CoreException(rootStatus);
					}
				} else {
					URI projLocation= project.getProject().getLocationURI();
					if (projLocation != null) {
						IFileStore store= EFS.getStore(projLocation).getChild(path);
						if (store.fetchInfo().exists()) {
							rootStatus.setError(NewWizardMessages.NewSourceFolderWizardPage_error_AlreadyExistingDifferentCase); 
							throw new CoreException(rootStatus);
						}
					}
				}
			}

			for (int i= 0; i < existingEntries.size(); i++) {
				CPListElement curr= (CPListElement) existingEntries.get(i);
				if (curr.getEntryKind() == IIncludePathEntry.CPE_SOURCE) {
					if (path.equals(curr.getPath()) && !project.getPath().equals(path)) {
						rootStatus.setError(NewWizardMessages.NewSourceFolderWizardPage_error_AlreadyExisting); 
						throw new CoreException(rootStatus);
					}
				}
			}

			if (!isExternal && !entry.getPath().equals(project.getPath()))
				exclude(entry.getPath(), existingEntries, new ArrayList(), project, null);

			insertAtEndOfCategory(entry, existingEntries);

			IIncludePathEntry[] entries= convert(existingEntries);

			IJavaScriptModelStatus status= JavaScriptConventions.validateClasspath(project, entries);
			if (!status.isOK()) {
				rootStatus.setError(status.getMessage());
				throw new CoreException(rootStatus);
			}

			if (isSourceFolder(project) || project.getPath().equals(path)) {
				rootStatus.setWarning(NewWizardMessages.NewSourceFolderWizardPage_warning_ReplaceSF); 
				return;
			}

			rootStatus.setOK();
			return;
		}
	}

	private static void insertAtEndOfCategory(CPListElement entry, List existingEntries) {
		int length= existingEntries.size();
		CPListElement[] elements= (CPListElement[])existingEntries.toArray(new CPListElement[length]);
		int i= 0;
		while (i < length && elements[i].getClasspathEntry().getEntryKind() != entry.getClasspathEntry().getEntryKind()) {
			i++;
		}
		if (i < length) {
			i++;
			while (i < length && elements[i].getClasspathEntry().getEntryKind() == entry.getClasspathEntry().getEntryKind()) {
				i++;
			}
			existingEntries.add(i, entry);
			return;
		}
		
		switch (entry.getClasspathEntry().getEntryKind()) {
		case IIncludePathEntry.CPE_SOURCE:
			existingEntries.add(0, entry);
			break;
		case IIncludePathEntry.CPE_CONTAINER:
		case IIncludePathEntry.CPE_LIBRARY:
		case IIncludePathEntry.CPE_PROJECT:
		default:
			existingEntries.add(entry);
			break;
		}
	}

	private static boolean isExternalArchiveOrLibrary(CPListElement entry, IJavaScriptProject project) {
		if (entry.getEntryKind() == IIncludePathEntry.CPE_LIBRARY || entry.getEntryKind() == IIncludePathEntry.CPE_CONTAINER) {
			if (entry.getResource() instanceof IFolder) {
				return false;
			}
			return true;
		}
		return false;
	}

	/**
	 * Test if the provided kind is of type
	 * <code>IIncludePathEntry.CPE_SOURCE</code>
	 * 
	 * @param entry the classpath entry to be compared with the provided type
	 * @param kind the kind to be checked
	 * @return <code>true</code> if kind equals
	 * <code>IIncludePathEntry.CPE_SOURCE</code>, 
	 * <code>false</code> otherwise
	 */
	private static boolean equalEntryKind(IIncludePathEntry entry, int kind) {
		return entry.getEntryKind() == kind;
	}
}
