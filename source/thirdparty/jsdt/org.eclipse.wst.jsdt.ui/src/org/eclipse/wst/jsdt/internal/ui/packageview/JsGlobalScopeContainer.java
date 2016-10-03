/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.packageview;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IJsGlobalScopeContainer;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.JsGlobalScopeContainerInitializer;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.IJsGlobalScopeContainerInitializerExtension;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.util.JSDScopeUiUtil;

/**
 * Representation of class path containers in Java UI.
 */
public class JsGlobalScopeContainer extends PackageFragmentRootContainer {

	private IIncludePathEntry fClassPathEntry;
	private IJsGlobalScopeContainer fContainer;

	public static class RequiredProjectWrapper implements IAdaptable, IWorkbenchAdapter {

		private final JsGlobalScopeContainer fParent;
		private final IJavaScriptProject fProject;
		
		public RequiredProjectWrapper(JsGlobalScopeContainer parent, IJavaScriptProject project) {
			fParent= parent;
			fProject= project;
		}
		
		public IJavaScriptProject getProject() {
			return fProject; 
		}
		
		public JsGlobalScopeContainer getParentJsGlobalScopeContainer() {
			return fParent; 
		}
		
		public Object getAdapter(Class adapter) {
			if (adapter == IWorkbenchAdapter.class) 
				return this;
			return null;
		}

		public Object[] getChildren(Object o) {
			return new Object[0];
		}

		public ImageDescriptor getImageDescriptor(Object object) {
			return PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(IDE.SharedImages.IMG_OBJ_PROJECT);
		}

		public String getLabel(Object o) {
			return fProject.getElementName();
		}

		public Object getParent(Object o) {
			return fParent;
		}
	}

	public JsGlobalScopeContainer(IJavaScriptProject parent, IIncludePathEntry entry) {
		super(parent);
		fClassPathEntry= entry;
		try {
			fContainer= JavaScriptCore.getJsGlobalScopeContainer(entry.getPath(), parent);
		} catch (JavaScriptModelException e) {
			fContainer= null;
		}
	}

	public boolean equals(Object obj) {
		if (obj instanceof JsGlobalScopeContainer) {
			JsGlobalScopeContainer other = (JsGlobalScopeContainer)obj;
			if (getJavaProject().equals(other.getJavaProject()) &&
				fClassPathEntry.equals(other.fClassPathEntry)) {
				return true;	
			}
			
		}
		return false;
	}

	public int hashCode() {
		return getJavaProject().hashCode()*17+fClassPathEntry.hashCode();
	}

	public IPackageFragmentRoot[] getPackageFragmentRoots() {
		return getJavaProject().findPackageFragmentRoots(fClassPathEntry);
	}

	public IAdaptable[] getChildren() {
		List list= new ArrayList();
		IPackageFragmentRoot[] roots= getPackageFragmentRoots();
		for (int i= 0; i < roots.length; i++) {
			list.add(roots[i]);
		}
		if (fContainer != null) {
			IIncludePathEntry[] classpathEntries= fContainer.getIncludepathEntries();
			if (classpathEntries == null) {
				// invalid implementation of a classpath container
				JavaScriptPlugin.log(new IllegalArgumentException("Invalid classpath container implementation: getClasspathEntries() returns null. " + fContainer.getPath())); //$NON-NLS-1$
			} else {
				IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
				for (int i= 0; i < classpathEntries.length; i++) {
					IIncludePathEntry entry= classpathEntries[i];
					if (entry.getEntryKind() == IIncludePathEntry.CPE_PROJECT) {
						IResource resource= root.findMember(entry.getPath());
						if (resource instanceof IProject)
							list.add(new RequiredProjectWrapper(this, JavaScriptCore.create((IProject) resource)));
					}
				}
			}
		}
		return (IAdaptable[]) list.toArray(new IAdaptable[list.size()]);
	}

	public ImageDescriptor getImageDescriptor() {
		IJsGlobalScopeContainerInitializerExtension init = JSDScopeUiUtil.getContainerUiInitializer(fClassPathEntry.getPath());
		if(init!=null ) {
			IPath entPath = fClassPathEntry.getPath();
			ImageDescriptor image = init.getImage(entPath, fClassPathEntry.toString(), super.getJavaProject());
			if(image!=null) return image;
		}
		return JavaPluginImages.DESC_OBJS_LIBRARY;
	}

	public String getLabel() {
		if (fContainer != null)
			return fContainer.getDescription();
		
		IPath path= fClassPathEntry.getPath();
		String containerId= path.segment(0);
		JsGlobalScopeContainerInitializer initializer= JavaScriptCore.getJsGlobalScopeContainerInitializer(containerId);
		if (initializer != null) {
			String description= initializer.getDescription(path, getJavaProject());
			return Messages.format(PackagesMessages.JsGlobalScopeContainer_unbound_label, description); 
		}
		return Messages.format(PackagesMessages.JsGlobalScopeContainer_unknown_label, path.toString()); 
	}
	
	public IIncludePathEntry getClasspathEntry() {
		return fClassPathEntry;
	}
	
	static boolean contains(IJavaScriptProject project, IIncludePathEntry entry, IPackageFragmentRoot root) {
		IPackageFragmentRoot[] roots= project.findPackageFragmentRoots(entry);
		for (int i= 0; i < roots.length; i++) {
			if (roots[i].equals(root))
				return true;
		}
		return false;
	}

	public String toString() {
		return fClassPathEntry.toString();
	}

	public boolean hasChildren() {
		return getPackageFragmentRoots().length > 0;
	}
}
