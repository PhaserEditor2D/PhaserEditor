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
package org.eclipse.wst.jsdt.internal.ui;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.search.ui.ISearchPageScoreComputer;
import org.eclipse.team.ui.history.IHistoryPageSource;
import org.eclipse.ui.IContainmentAdapter;
import org.eclipse.ui.IContributorResourceAdapter;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.ide.IContributorResourceAdapter2;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.views.properties.FilePropertySource;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.ResourcePropertySource;
import org.eclipse.ui.views.tasklist.ITaskListResourceAdapter;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.internal.corext.util.JavaElementResourceMapping;
import org.eclipse.wst.jsdt.internal.ui.compare.JavaElementHistoryPageSource;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.IClassFileEditorInput;
import org.eclipse.wst.jsdt.internal.ui.search.JavaSearchPageScoreComputer;
import org.eclipse.wst.jsdt.internal.ui.search.SearchUtil;

/**
 * Implements basic UI support for Java elements.
 * Implements handle to persistent support for Java elements.
 */
public class JavaElementAdapterFactory implements IAdapterFactory, IContributorResourceAdapter, IContributorResourceAdapter2 {
	
	private static Class[] PROPERTIES= new Class[] {
		IPropertySource.class,
		IResource.class,
		IWorkbenchAdapter.class,
		IResourceLocator.class,
		IPersistableElement.class,
		IContributorResourceAdapter.class,
		IContributorResourceAdapter2.class,
		ITaskListResourceAdapter.class,
		IContainmentAdapter.class,
		IHistoryPageSource.class
	};
	
	/*
	 * Do not use real type since this would cause
	 * the Search plug-in to be loaded.
	 */
	private Object fSearchPageScoreComputer;
	private static IResourceLocator fgResourceLocator;
	private static JavaWorkbenchAdapter fgJavaWorkbenchAdapter;
	private static ITaskListResourceAdapter fgTaskListAdapter;
	private static JavaElementContainmentAdapter fgJavaElementContainmentAdapter;
	
	public Class[] getAdapterList() {
		updateLazyLoadedAdapters();
		return PROPERTIES;
	}
	
	public Object getAdapter(Object element, Class key) {
		updateLazyLoadedAdapters();
		IJavaScriptElement java= getJavaElement(element);
		
		if (IPropertySource.class.equals(key)) {
			return getProperties(java);
		} if (IResource.class.equals(key)) {
			return getResource(java);
		} if (fSearchPageScoreComputer != null && ISearchPageScoreComputer.class.equals(key)) {
			return fSearchPageScoreComputer;
		} if (IWorkbenchAdapter.class.equals(key)) {
			return getJavaWorkbenchAdapter();
		} if (IResourceLocator.class.equals(key)) {
			return getResourceLocator();
		} if (IPersistableElement.class.equals(key)) {
			return new PersistableJavaElementFactory(java);
		} if (IContributorResourceAdapter.class.equals(key)) {
			if (getResource(java)!=null)
			return this;
		} if (IContributorResourceAdapter2.class.equals(key)) {
			return this;
		} if (ITaskListResourceAdapter.class.equals(key)) {
			return getTaskListAdapter();
		} if (IContainmentAdapter.class.equals(key)) {
			return getJavaElementContainmentAdapter();
		} if (IHistoryPageSource.class.equals(key) && JavaElementHistoryPageSource.hasEdition(java)) {
			return JavaElementHistoryPageSource.getInstance();
		}
		return null; 
	}
	
	private IResource getResource(IJavaScriptElement element) {
		// can't use IJavaScriptElement.getResource directly as we are interested in the
		// corresponding resource
		switch (element.getElementType()) {
			case IJavaScriptElement.TYPE:
				// top level types behave like the CU
				IJavaScriptElement parent= element.getParent();
				if (parent instanceof IJavaScriptUnit) {
					return ((IJavaScriptUnit) parent).getPrimary().getResource();
				}
				return null;
			case IJavaScriptElement.JAVASCRIPT_UNIT:
				return ((IJavaScriptUnit) element).getPrimary().getResource();
			case IJavaScriptElement.CLASS_FILE:
			case IJavaScriptElement.PACKAGE_FRAGMENT:
				// test if in a archive
				IPackageFragmentRoot root= (IPackageFragmentRoot) element.getAncestor(IJavaScriptElement.PACKAGE_FRAGMENT_ROOT);
				if (!root.isArchive()) {
					return element.getResource();
				}
				return null;
			case IJavaScriptElement.PACKAGE_FRAGMENT_ROOT:
			case IJavaScriptElement.JAVASCRIPT_PROJECT:
			case IJavaScriptElement.JAVASCRIPT_MODEL:
				return element.getResource();
			default:
				return null;
		}		
    }

    public IResource getAdaptedResource(IAdaptable adaptable) {
    	IJavaScriptElement je= getJavaElement(adaptable);
    	if (je != null)
    		return getResource(je);

    	return null;
    }
    
    public ResourceMapping getAdaptedResourceMapping(IAdaptable adaptable) {
    	IJavaScriptElement je= getJavaElement(adaptable);
    	if (je != null)
    		return JavaElementResourceMapping.create(je);

    	return null;
    }
    
	private IJavaScriptElement getJavaElement(Object element) {
		if (element instanceof IJavaScriptElement)
			return (IJavaScriptElement)element;
		if (element instanceof IClassFileEditorInput)
			return ((IClassFileEditorInput)element).getClassFile().getPrimaryElement();

		return null;
	}
	
	private IPropertySource getProperties(IJavaScriptElement element) {
		IResource resource= getResource(element);
		if (resource == null)
			return new JavaElementProperties(element);
		if (resource.getType() == IResource.FILE)
			return new FilePropertySource((IFile) resource);
		return new ResourcePropertySource(resource);
	}

	private void updateLazyLoadedAdapters() {
		if (fSearchPageScoreComputer == null && SearchUtil.isSearchPlugInActivated())
			createSearchPageScoreComputer();
	}

	private void createSearchPageScoreComputer() {
		fSearchPageScoreComputer= new JavaSearchPageScoreComputer();
		PROPERTIES= new Class[] {
			IPropertySource.class,
			IResource.class,
			ISearchPageScoreComputer.class,
			IWorkbenchAdapter.class,
			IResourceLocator.class,
			IPersistableElement.class,
			IProject.class,
			IContributorResourceAdapter.class,
			IContributorResourceAdapter2.class,
			ITaskListResourceAdapter.class,
			IContainmentAdapter.class
		};
	}

	private static IResourceLocator getResourceLocator() {
		if (fgResourceLocator == null)
			fgResourceLocator= new ResourceLocator();
		return fgResourceLocator;
	}
	
	private static JavaWorkbenchAdapter getJavaWorkbenchAdapter() {
		if (fgJavaWorkbenchAdapter == null) 
			fgJavaWorkbenchAdapter= new JavaWorkbenchAdapter();
		return fgJavaWorkbenchAdapter;
	}

	private static ITaskListResourceAdapter getTaskListAdapter() {
		if (fgTaskListAdapter == null)
			fgTaskListAdapter= new JavaTaskListAdapter();
		return fgTaskListAdapter;
	}

	private static JavaElementContainmentAdapter getJavaElementContainmentAdapter() {
		if (fgJavaElementContainmentAdapter == null)
			fgJavaElementContainmentAdapter= new JavaElementContainmentAdapter();
		return fgJavaElementContainmentAdapter;
	}
}
