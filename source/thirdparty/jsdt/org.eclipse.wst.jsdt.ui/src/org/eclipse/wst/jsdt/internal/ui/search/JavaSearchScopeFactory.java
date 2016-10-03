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
package org.eclipse.wst.jsdt.internal.ui.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.IWorkingSetSelectionDialog;
import org.eclipse.wst.jsdt.core.IJsGlobalScopeContainer;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchScope;
import org.eclipse.wst.jsdt.core.search.SearchEngine;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.browsing.LogicalPackage;
import org.eclipse.wst.jsdt.ui.JavaScriptUI;

public class JavaSearchScopeFactory {
	
	public static final int JRE= IJavaScriptSearchScope.SYSTEM_LIBRARIES;
	public static final int LIBS= IJavaScriptSearchScope.APPLICATION_LIBRARIES;
	public static final int PROJECTS= IJavaScriptSearchScope.REFERENCED_PROJECTS;
	public static final int SOURCES= IJavaScriptSearchScope.SOURCES;
	
	public static final int ALL= JRE | LIBS | PROJECTS | SOURCES;
	public static final int NO_PROJ= JRE | LIBS | SOURCES;
	public static final int NO_JRE= LIBS | PROJECTS | SOURCES;
	public static final int NO_JRE_NO_PROJ= LIBS | PROJECTS | SOURCES;

	private static JavaSearchScopeFactory fgInstance;
	private final IJavaScriptSearchScope EMPTY_SCOPE= SearchEngine.createJavaSearchScope(new IJavaScriptElement[] {});
	
	private JavaSearchScopeFactory() {
	}

	public static JavaSearchScopeFactory getInstance() {
		if (fgInstance == null)
			fgInstance= new JavaSearchScopeFactory();
		return fgInstance;
	}

	public IWorkingSet[] queryWorkingSets() throws JavaScriptModelException, InterruptedException {
		Shell shell= JavaScriptPlugin.getActiveWorkbenchShell();
		if (shell == null)
			return null;
		IWorkingSetSelectionDialog dialog= PlatformUI.getWorkbench().getWorkingSetManager().createWorkingSetSelectionDialog(shell, true);
		if (dialog.open() != Window.OK) {
			throw new InterruptedException();
		}
			
		IWorkingSet[] workingSets= dialog.getSelection();
		if (workingSets.length > 0)
			return workingSets;
		return null; // 'no working set' selected
	}

	public IJavaScriptSearchScope createJavaSearchScope(IWorkingSet[] workingSets, boolean includeJRE) {
		return createJavaSearchScope(workingSets, includeJRE ? ALL : NO_JRE);
	}
	
	public IJavaScriptSearchScope createJavaSearchScope(IWorkingSet[] workingSets, int includeMask) {
		if (workingSets == null || workingSets.length < 1)
			return EMPTY_SCOPE;

		Set javaElements= new HashSet(workingSets.length * 10);
		for (int i= 0; i < workingSets.length; i++) {
			IWorkingSet workingSet= workingSets[i];
			if (workingSet.isEmpty() && workingSet.isAggregateWorkingSet()) {
				return createWorkspaceScope(includeMask);
			}
			addJavaElements(javaElements, workingSet);
		}
		return createJavaSearchScope(javaElements, includeMask);
	}
	
	public IJavaScriptSearchScope createJavaSearchScope(IWorkingSet workingSet, boolean includeJRE) {
		return createJavaSearchScope(workingSet, includeJRE ? NO_PROJ : NO_JRE_NO_PROJ);
	}
	
	public IJavaScriptSearchScope createJavaSearchScope(IWorkingSet workingSet, int includeMask) {
		Set javaElements= new HashSet(10);
		if (workingSet.isEmpty() && workingSet.isAggregateWorkingSet()) {
			return createWorkspaceScope(includeMask);
		}
		addJavaElements(javaElements, workingSet);
		return createJavaSearchScope(javaElements, includeMask);
	}
	
	public IJavaScriptSearchScope createJavaSearchScope(IResource[] resources, boolean includeJRE) {
		return createJavaSearchScope(resources, includeJRE ? NO_PROJ : NO_JRE_NO_PROJ);
	}

	public IJavaScriptSearchScope createJavaSearchScope(IResource[] resources, int includeMask) {
		if (resources == null)
			return EMPTY_SCOPE;
		Set javaElements= new HashSet(resources.length);
		addJavaElements(javaElements, resources);
		return createJavaSearchScope(javaElements, includeMask);
	}
		
	public IJavaScriptSearchScope createJavaSearchScope(ISelection selection, boolean includeJRE) {
		return createJavaSearchScope(selection, includeJRE ? NO_PROJ : NO_JRE_NO_PROJ);
	}
	
	public IJavaScriptSearchScope createJavaSearchScope(ISelection selection, int includeMask) {
		return createJavaSearchScope(getJavaElements(selection), includeMask);
	}
	
	public IJavaScriptSearchScope createJavaProjectSearchScope(String[] projectNames, boolean includeJRE) {
		return createJavaProjectSearchScope(projectNames, includeJRE ? NO_PROJ : NO_JRE_NO_PROJ);
	}
	
	public IJavaScriptSearchScope createJavaProjectSearchScope(String[] projectNames, int includeMask) {
		ArrayList res= new ArrayList();
		IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
		for (int i= 0; i < projectNames.length; i++) {
			IJavaScriptProject project= JavaScriptCore.create(root.getProject(projectNames[i]));
			if (project.exists()) {
				res.add(project);
			}
		}
		return createJavaSearchScope(res, includeMask);
	}
	
	public IJavaScriptSearchScope createJavaProjectSearchScope(IJavaScriptProject project, boolean includeJRE) {
		return createJavaProjectSearchScope(project, includeJRE ? NO_PROJ : NO_JRE_NO_PROJ);
	}

	public IJavaScriptSearchScope createJavaProjectSearchScope(IJavaScriptProject project, int includeMask) {
		return SearchEngine.createJavaSearchScope(new IJavaScriptElement[] { project }, getSearchFlags(includeMask));
	}
	
	public IJavaScriptSearchScope createJavaProjectSearchScope(IEditorInput editorInput, boolean includeJRE) {
		return createJavaProjectSearchScope(editorInput, includeJRE ? ALL : NO_JRE);
	}
	
	public IJavaScriptSearchScope createJavaProjectSearchScope(IEditorInput editorInput, int includeMask) {
		IJavaScriptElement elem= JavaScriptUI.getEditorInputJavaElement(editorInput);
		if (elem != null) {
			IJavaScriptProject project= elem.getJavaScriptProject();
			if (project != null) {
				return createJavaProjectSearchScope(project, includeMask);
			}
		}
		return EMPTY_SCOPE;
	}
	
	public String getWorkspaceScopeDescription(boolean includeJRE) {
		return includeJRE ? SearchMessages.WorkspaceScope : SearchMessages.WorkspaceScopeNoJRE; 
	}
	
	public String getWorkspaceScopeDescription(int includeMask) {
		return getWorkspaceScopeDescription((includeMask & JRE) != 0);
	}
	
	public String getProjectScopeDescription(String[] projectNames, int includeMask) {
		if (projectNames.length == 0) {
			return SearchMessages.JavaSearchScopeFactory_undefined_projects;
		}
		boolean includeJRE= (includeMask & JRE) != 0;
		String scopeDescription;
		if (projectNames.length == 1) {
			String label= includeJRE ? SearchMessages.EnclosingProjectScope : SearchMessages.EnclosingProjectScopeNoJRE;
			scopeDescription= Messages.format(label, projectNames[0]);
		} else if (projectNames.length == 2) {
			String label= includeJRE ? SearchMessages.EnclosingProjectsScope2 : SearchMessages.EnclosingProjectsScope2NoJRE;
			scopeDescription= Messages.format(label, new String[] { projectNames[0], projectNames[1]});
		} else {
			String label= includeJRE ? SearchMessages.EnclosingProjectsScope : SearchMessages.EnclosingProjectsScopeNoJRE;
			scopeDescription= Messages.format(label, new String[] { projectNames[0], projectNames[1]});
		}
		return scopeDescription;
	}
	
	public String getProjectScopeDescription(IJavaScriptProject project, boolean includeJRE) {
		if (includeJRE) {
			return Messages.format(SearchMessages.ProjectScope, project.getElementName());
		} else {
			return Messages.format(SearchMessages.ProjectScopeNoJRE, project.getElementName());
		}
	}
	
	public String getProjectScopeDescription(IEditorInput editorInput, boolean includeJRE) {
		IJavaScriptElement elem= JavaScriptUI.getEditorInputJavaElement(editorInput);
		if (elem != null) {
			IJavaScriptProject project= elem.getJavaScriptProject();
			if (project != null) {
				return getProjectScopeDescription(project, includeJRE);
			}
		}
		return Messages.format(SearchMessages.ProjectScope, "");  //$NON-NLS-1$
	}
	
	public String getHierarchyScopeDescription(IType type) {
		return Messages.format(SearchMessages.HierarchyScope, new String[] { type.getElementName() }); 
	}

	public String getSelectionScopeDescription(IJavaScriptElement[] javaElements, int includeMask) {
		return getSelectionScopeDescription(javaElements, (includeMask & JRE) != 0);
	}
	

	public String getSelectionScopeDescription(IJavaScriptElement[] javaElements, boolean includeJRE) {
		if (javaElements.length == 0) {
			return SearchMessages.JavaSearchScopeFactory_undefined_selection;
		}
		String scopeDescription;
		if (javaElements.length == 1) {
			String label= includeJRE ? SearchMessages.SingleSelectionScope : SearchMessages.SingleSelectionScopeNoJRE;
			scopeDescription= Messages.format(label, javaElements[0].getElementName());
		} else if (javaElements.length == 1) {
			String label= includeJRE ? SearchMessages.DoubleSelectionScope : SearchMessages.DoubleSelectionScopeNoJRE;
			scopeDescription= Messages.format(label, new String[] { javaElements[0].getElementName(), javaElements[1].getElementName()});
		}  else {
			String label= includeJRE ? SearchMessages.SelectionScope : SearchMessages.SelectionScopeNoJRE;
			scopeDescription= Messages.format(label, new String[] { javaElements[0].getElementName(), javaElements[1].getElementName()});
		}
		return scopeDescription;
	}
	
	public String getWorkingSetScopeDescription(IWorkingSet[] workingSets, int includeMask) {
		return getWorkingSetScopeDescription(workingSets, (includeMask & JRE) != 0);
	}
	
	public String getWorkingSetScopeDescription(IWorkingSet[] workingSets, boolean includeJRE) {
		if (workingSets.length == 0) {
			return SearchMessages.JavaSearchScopeFactory_undefined_workingsets;
		}
		if (workingSets.length == 1) {
			String label= includeJRE ? SearchMessages.SingleWorkingSetScope : SearchMessages.SingleWorkingSetScopeNoJRE;
			return Messages.format(label, workingSets[0].getLabel());
		}
		Arrays.sort(workingSets, new WorkingSetComparator());
		if (workingSets.length == 2) {
			String label= includeJRE ? SearchMessages.DoubleWorkingSetScope : SearchMessages.DoubleWorkingSetScopeNoJRE;
			return Messages.format(label, new String[] { workingSets[0].getLabel(), workingSets[1].getLabel()});
		}
		String label= includeJRE ? SearchMessages.WorkingSetsScope : SearchMessages.WorkingSetsScopeNoJRE;
		return Messages.format(label, new String[] { workingSets[0].getLabel(), workingSets[1].getLabel()});
	}
	
	public IProject[] getProjects(IJavaScriptSearchScope scope) {
		IPath[] paths= scope.enclosingProjectsAndJars();
		HashSet temp= new HashSet();
		for (int i= 0; i < paths.length; i++) {
			IResource resource= ResourcesPlugin.getWorkspace().getRoot().findMember(paths[i]);
			if (resource != null && resource.getType() == IResource.PROJECT)
				temp.add(resource);
		}
		return (IProject[]) temp.toArray(new IProject[temp.size()]);
	}

	public IJavaScriptElement[] getJavaElements(ISelection selection) {
		if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
			return getJavaElements(((IStructuredSelection)selection).toArray());
		} else {
			return new IJavaScriptElement[0];
		}
	}

	private IJavaScriptElement[] getJavaElements(Object[] elements) {
		if (elements.length == 0)
			return new IJavaScriptElement[0];
		
		Set result= new HashSet(elements.length);
		for (int i= 0; i < elements.length; i++) {
			Object selectedElement= elements[i];
			if (selectedElement instanceof IJavaScriptElement) {
				addJavaElements(result, (IJavaScriptElement) selectedElement);
			} else if (selectedElement instanceof IResource) {
				addJavaElements(result, (IResource) selectedElement);
			} else if (selectedElement instanceof LogicalPackage) {
				addJavaElements(result, (LogicalPackage) selectedElement);
			} else if (selectedElement instanceof IWorkingSet) {
				IWorkingSet ws= (IWorkingSet)selectedElement;
				addJavaElements(result, ws);
			} else if (selectedElement instanceof IAdaptable) {
				IResource resource= (IResource) ((IAdaptable) selectedElement).getAdapter(IResource.class);
				if (resource != null)
					addJavaElements(result, resource);
			}
			
		}
		return (IJavaScriptElement[]) result.toArray(new IJavaScriptElement[result.size()]);
	}
	
	public IJavaScriptSearchScope createJavaSearchScope(IJavaScriptElement[] javaElements, boolean includeJRE) {
		return createJavaSearchScope(javaElements, includeJRE ? NO_PROJ : NO_JRE_NO_PROJ);
	}
	
	public IJavaScriptSearchScope createJavaSearchScope(IJavaScriptElement[] javaElements, int includeMask) {
		if (javaElements.length == 0)
			return EMPTY_SCOPE;
		return SearchEngine.createJavaSearchScope(javaElements, getSearchFlags(includeMask));
	}

	private IJavaScriptSearchScope createJavaSearchScope(Collection javaElements, int includeMask) {
		if (javaElements.isEmpty())
			return EMPTY_SCOPE;
		IJavaScriptElement[] elementArray= (IJavaScriptElement[]) javaElements.toArray(new IJavaScriptElement[javaElements.size()]);
		return SearchEngine.createJavaSearchScope(elementArray, getSearchFlags(includeMask));
	}
	
	private static int getSearchFlags(int includeMask) {
		return includeMask;
	}

	private void addJavaElements(Set javaElements, IResource[] resources) {
		for (int i= 0; i < resources.length; i++)
			addJavaElements(javaElements, resources[i]);
	}

	private void addJavaElements(Set javaElements, IResource resource) {
		IJavaScriptElement javaElement= (IJavaScriptElement)resource.getAdapter(IJavaScriptElement.class);
		if (javaElement == null)
			// not a Java resource
			return;
		
		if (javaElement.getElementType() == IJavaScriptElement.PACKAGE_FRAGMENT) {
			// add other possible package fragments
			try {
				addJavaElements(javaElements, ((IFolder)resource).members());
			} catch (CoreException ex) {
				// don't add elements
			}
		}
			
		javaElements.add(javaElement);
	}

	private void addJavaElements(Set javaElements, IJavaScriptElement javaElement) {
		javaElements.add(javaElement);
	}
	
	private void addJavaElements(Set javaElements, IWorkingSet workingSet) {
		if (workingSet == null)
			return;
		
		if (workingSet.isAggregateWorkingSet() && workingSet.isEmpty()) {
			try {
				IJavaScriptProject[] projects= JavaScriptCore.create(ResourcesPlugin.getWorkspace().getRoot()).getJavaScriptProjects();
				javaElements.addAll(Arrays.asList(projects));
			} catch (JavaScriptModelException e) {
				JavaScriptPlugin.log(e);
			}
			return;
		}
		
		IAdaptable[] elements= workingSet.getElements();
		for (int i= 0; i < elements.length; i++) {
			IJavaScriptElement javaElement=(IJavaScriptElement) elements[i].getAdapter(IJavaScriptElement.class);
			if (javaElement != null) { 
				addJavaElements(javaElements, javaElement);
				continue;
			}
			IResource resource= (IResource)elements[i].getAdapter(IResource.class);
			if (resource != null) {
				addJavaElements(javaElements, resource);
			}
			
			// else we don't know what to do with it, ignore.
		}
	}

	private void addJavaElements(Set javaElements, LogicalPackage selectedElement) {
		IPackageFragment[] packages= selectedElement.getFragments();
		for (int i= 0; i < packages.length; i++)
			addJavaElements(javaElements, packages[i]);
	}
	
	public IJavaScriptSearchScope createWorkspaceScope(boolean includeJRE) {
		return createWorkspaceScope(includeJRE ? ALL : NO_JRE);
	}
	
	public IJavaScriptSearchScope createWorkspaceScope(int includeMask) {
		if ((includeMask & NO_PROJ) != NO_PROJ) {
			try {
				IJavaScriptProject[] projects= JavaScriptCore.create(ResourcesPlugin.getWorkspace().getRoot()).getJavaScriptProjects();
				return SearchEngine.createJavaSearchScope(projects, getSearchFlags(includeMask));
			} catch (JavaScriptModelException e) {
				// ignore, use workspace scope instead
			}
		}
		return SearchEngine.createWorkspaceScope();
	}

	public boolean isInsideJRE(IJavaScriptElement element) {
		IPackageFragmentRoot root= (IPackageFragmentRoot) element.getAncestor(IJavaScriptElement.PACKAGE_FRAGMENT_ROOT);
		if (root != null) {
			try {
				IIncludePathEntry entry= root.getRawIncludepathEntry();
				if (entry.getEntryKind() == IIncludePathEntry.CPE_CONTAINER) {
					IJsGlobalScopeContainer container= JavaScriptCore.getJsGlobalScopeContainer(entry.getPath(), root.getJavaScriptProject());
					return container != null && container.getKind() == IJsGlobalScopeContainer.K_DEFAULT_SYSTEM;
				}
				return false;
			} catch (JavaScriptModelException e) {
				JavaScriptPlugin.log(e);
			}
		}
		return true; // include JRE in doubt
	}
}
