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
package org.eclipse.wst.jsdt.internal.corext.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.mapping.RemoteResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.wst.jsdt.core.IClassFile;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptModel;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.browsing.LogicalPackage;
import org.eclipse.wst.jsdt.internal.ui.model.JavaModelProvider;

/**
 * An abstract super class to describe mappings from a Java element to a
 * set of resources. The class also provides factory methods to create
 * resource mappings.
 * 
 * 
 */
public abstract class JavaElementResourceMapping extends ResourceMapping {
	
	protected JavaElementResourceMapping() {
	}
	
	public IJavaScriptElement getJavaElement() {
		Object o= getModelObject();
		if (o instanceof IJavaScriptElement)
			return (IJavaScriptElement)o;
		return null;
	}
	
	public boolean equals(Object obj) {
		if (!(obj instanceof JavaElementResourceMapping))
			return false;
		return getJavaElement().equals(((JavaElementResourceMapping)obj).getJavaElement());
	}
	
	public int hashCode() {
		IJavaScriptElement javaElement= getJavaElement();
		if (javaElement == null)
			return super.hashCode();
		
		return javaElement.hashCode();
	}
	
	public String getModelProviderId() {
		return JavaModelProvider.JAVA_MODEL_PROVIDER_ID;
	}
	
	public boolean contains(ResourceMapping mapping) {
		if (mapping instanceof JavaElementResourceMapping) {
			JavaElementResourceMapping javaMapping = (JavaElementResourceMapping) mapping;
			IJavaScriptElement element = getJavaElement();
			IJavaScriptElement other = javaMapping.getJavaElement();
			if (other != null && element != null)
				return element.getPath().isPrefixOf(other.getPath());
		}
		return false;
	}
	
	//---- the factory code ---------------------------------------------------------------
	
	private static final class JavaModelResourceMapping extends JavaElementResourceMapping {
		private final IJavaScriptModel fJavaModel;
		private JavaModelResourceMapping(IJavaScriptModel model) {
			Assert.isNotNull(model);
			fJavaModel= model;
		}
		public Object getModelObject() {
			return fJavaModel;
		}
		public IProject[] getProjects() {
			IJavaScriptProject[] projects= null;
			try {
				projects= fJavaModel.getJavaScriptProjects();
			} catch (JavaScriptModelException e) {
				JavaScriptPlugin.log(e);
				return new IProject[0];
			}
			IProject[] result= new IProject[projects.length];
			for (int i= 0; i < projects.length; i++) {
				result[i]= projects[i].getProject();
			}
			return result;
		}
		public ResourceTraversal[] getTraversals(ResourceMappingContext context, IProgressMonitor monitor) throws CoreException {
			IJavaScriptProject[] projects= fJavaModel.getJavaScriptProjects();
			ResourceTraversal[] result= new ResourceTraversal[projects.length];
			for (int i= 0; i < projects.length; i++) {
				result[i]= new ResourceTraversal(new IResource[] {projects[i].getProject()}, IResource.DEPTH_INFINITE, 0);
			}
			return result;
		}
	}
	
	private static final class JavaProjectResourceMapping extends JavaElementResourceMapping {
		private final IJavaScriptProject fProject;
		private JavaProjectResourceMapping(IJavaScriptProject project) {
			Assert.isNotNull(project);
			fProject= project;
		}
		public Object getModelObject() {
			return fProject;
		}
		public IProject[] getProjects() {
			return new IProject[] {fProject.getProject() };
		}
		public ResourceTraversal[] getTraversals(ResourceMappingContext context, IProgressMonitor monitor) throws CoreException {
			return new ResourceTraversal[] {
				new ResourceTraversal(new IResource[] {fProject.getProject()}, IResource.DEPTH_INFINITE, 0)
			};
		}
	}
	
	private static final class PackageFragementRootResourceMapping extends JavaElementResourceMapping {
		private final IPackageFragmentRoot fRoot;
		private PackageFragementRootResourceMapping(IPackageFragmentRoot root) {
			Assert.isNotNull(root);
			fRoot= root;
		}
		public Object getModelObject() {
			return fRoot;
		}
		public IProject[] getProjects() {
			return new IProject[] {fRoot.getJavaScriptProject().getProject() };
		}
		public ResourceTraversal[] getTraversals(ResourceMappingContext context, IProgressMonitor monitor) throws CoreException {
			return new ResourceTraversal[] {
				new ResourceTraversal(new IResource[] {fRoot.getResource()}, IResource.DEPTH_INFINITE, 0)
			};
		}
	}
	
	private static final class LocalPackageFragementTraversal extends ResourceTraversal {
		private final IPackageFragment fPack;
		public LocalPackageFragementTraversal(IPackageFragment pack) throws CoreException {
			super(new IResource[] {pack.getResource()}, IResource.DEPTH_ONE, 0);
			fPack= pack;
		}
		public void accept(IResourceVisitor visitor) throws CoreException {
			IFile[] files= getPackageContent(fPack);
			final IResource resource= fPack.getResource();
			if (resource != null)
				visitor.visit(resource);
			for (int i= 0; i < files.length; i++) {
				visitor.visit(files[i]);
			}
		}
	}
	
	private static final class PackageFragmentResourceMapping extends JavaElementResourceMapping {
		private final IPackageFragment fPack;
		private PackageFragmentResourceMapping(IPackageFragment pack) {
			Assert.isNotNull(pack);
			fPack= pack;
		}
		public Object getModelObject() {
			return fPack;
		}
		public IProject[] getProjects() {
			return new IProject[] { fPack.getJavaScriptProject().getProject() };
		}
		public ResourceTraversal[] getTraversals(ResourceMappingContext context, IProgressMonitor monitor) throws CoreException {
			if (context instanceof RemoteResourceMappingContext) {
				return new ResourceTraversal[] {
					new ResourceTraversal(new IResource[] {fPack.getResource()}, IResource.DEPTH_ONE, 0)
				};
			} else {
				return new ResourceTraversal[] { new LocalPackageFragementTraversal(fPack) };
			}
		}
		public void accept(ResourceMappingContext context, IResourceVisitor visitor, IProgressMonitor monitor) throws CoreException {
			if (context instanceof RemoteResourceMappingContext) {
				super.accept(context, visitor, monitor);
			} else {
				// We assume a local context.
				IFile[] files= getPackageContent(fPack);
				if (monitor == null)
					monitor= new NullProgressMonitor();
				monitor.beginTask("", files.length + 1); //$NON-NLS-1$
				final IResource resource= fPack.getResource();
				if (resource != null)
					visitor.visit(resource);
				monitor.worked(1);
				for (int i= 0; i < files.length; i++) {
					visitor.visit(files[i]);
					monitor.worked(1);
				}
			}
		}
	}
	
	private static IFile[] getPackageContent(IPackageFragment pack) throws CoreException {
		List result= new ArrayList();
		IContainer container= (IContainer)pack.getResource();
		if (container != null) {
			IResource[] members= container.members();
			for (int m= 0; m < members.length; m++) {
				IResource member= members[m];
				if (member instanceof IFile) {
					IFile file= (IFile)member;
					if ("class".equals(file.getFileExtension()) && file.isDerived()) //$NON-NLS-1$
						continue;
					result.add(member);
				}
			}
		}
		return (IFile[])result.toArray(new IFile[result.size()]);
	}
	
	
	private static final class CompilationUnitResourceMapping extends JavaElementResourceMapping {
		private final IJavaScriptUnit fUnit;
		private CompilationUnitResourceMapping(IJavaScriptUnit unit) {
			Assert.isNotNull(unit);
			fUnit= unit;
		}
		public Object getModelObject() {
			return fUnit;
		}
		public IProject[] getProjects() {
			return new IProject[] {fUnit.getJavaScriptProject().getProject() };
		}
		public ResourceTraversal[] getTraversals(ResourceMappingContext context, IProgressMonitor monitor) throws CoreException {
			return new ResourceTraversal[] {
				new ResourceTraversal(new IResource[] {fUnit.getResource()}, IResource.DEPTH_ONE, 0)
			};
		}
	}

	private static final class ClassFileResourceMapping extends JavaElementResourceMapping {
		private final IClassFile fClassFile;
		private ClassFileResourceMapping(IClassFile classFile) {
			fClassFile= classFile;
		}
		public Object getModelObject() {
			return fClassFile;
		}
		public IProject[] getProjects() {
			return new IProject[] { fClassFile.getJavaScriptProject().getProject() };
		}
		public ResourceTraversal[] getTraversals(ResourceMappingContext context, IProgressMonitor monitor) throws CoreException {
			return new ResourceTraversal[] {
				new ResourceTraversal(new IResource[] {fClassFile.getResource()}, IResource.DEPTH_ONE, 0)
			};
		}
	}
	
	private static final class LogicalPackageResourceMapping extends ResourceMapping {
		private final IPackageFragment[] fFragments;
		private LogicalPackageResourceMapping(IPackageFragment[] fragments) {
			fFragments= fragments;
		}
		public Object getModelObject() {
			return fFragments;
		}
		public IProject[] getProjects() {
			Set result= new HashSet();
			for (int i= 0; i < fFragments.length; i++) {
				result.add(fFragments[i].getJavaScriptProject().getProject());
			}
			return (IProject[])result.toArray(new IProject[result.size()]);
		}
		public ResourceTraversal[] getTraversals(ResourceMappingContext context, IProgressMonitor monitor) throws CoreException {
			List result= new ArrayList();
			if (context instanceof RemoteResourceMappingContext) {
				for (int i= 0; i < fFragments.length; i++) {
					result.add(new ResourceTraversal(
						new IResource[] {fFragments[i].getResource()}, IResource.DEPTH_ONE, 0));
				}
			} else {
				for (int i= 0; i < fFragments.length; i++) {
					result.add(new LocalPackageFragementTraversal(fFragments[i]));
				}
			}
			return (ResourceTraversal[])result.toArray(new ResourceTraversal[result.size()]);
		}
		
		public String getModelProviderId() {
			return JavaModelProvider.JAVA_MODEL_PROVIDER_ID;
		}
	}
	
	public static ResourceMapping create(IJavaScriptElement element) {
		switch (element.getElementType()) {
			case IJavaScriptElement.TYPE:
				return create((IType)element);
			case IJavaScriptElement.JAVASCRIPT_UNIT:
				return create((IJavaScriptUnit)element);
			case IJavaScriptElement.CLASS_FILE:
				return create((IClassFile)element);
			case IJavaScriptElement.PACKAGE_FRAGMENT:
				return create((IPackageFragment)element);
			case IJavaScriptElement.PACKAGE_FRAGMENT_ROOT:
				return create((IPackageFragmentRoot)element);
			case IJavaScriptElement.JAVASCRIPT_PROJECT:
				return create((IJavaScriptProject)element);
			case IJavaScriptElement.JAVASCRIPT_MODEL:
				return create((IJavaScriptModel)element);
			default:
				return null;
		}		
		
	}

	public static ResourceMapping create(final IJavaScriptModel model) {
		return new JavaModelResourceMapping(model);
	}
	
	public static ResourceMapping create(final IJavaScriptProject project) {
		return new JavaProjectResourceMapping(project);
	}
	
	public static ResourceMapping create(final IPackageFragmentRoot root) {
		if (root.isExternal())
			return null;
		return new PackageFragementRootResourceMapping(root);
	}
	
	public static ResourceMapping create(final IPackageFragment pack) {
		// test if in an archive
		IPackageFragmentRoot root= (IPackageFragmentRoot)pack.getAncestor(IJavaScriptElement.PACKAGE_FRAGMENT_ROOT);
		if (!root.isArchive()) {
			return new PackageFragmentResourceMapping(pack);
		}
		return null;
	}
	
	public static ResourceMapping create(IJavaScriptUnit unit) {
		if (unit == null)
			return null;
		return new CompilationUnitResourceMapping(unit.getPrimary());
	}
	
	public static ResourceMapping create(IClassFile classFile) {
		// test if in a archive
		IPackageFragmentRoot root= (IPackageFragmentRoot)classFile.getAncestor(IJavaScriptElement.PACKAGE_FRAGMENT_ROOT);
		if (!root.isArchive()) {
			return new ClassFileResourceMapping(classFile);
		}
		return null;
	}
	
	public static ResourceMapping create(IType type) {
		// top level types behave like the CU
		IJavaScriptElement parent= type.getParent();
		if (parent instanceof IJavaScriptUnit) {
			return create((IJavaScriptUnit)parent);
		}
		return null;
	}
	
	public static ResourceMapping create(LogicalPackage logicalPackage) {
		IPackageFragment[] fragments= logicalPackage.getFragments();
		List toProcess= new ArrayList(fragments.length);
		for (int i= 0; i < fragments.length; i++) {
			// only add if not part of an archive
			IPackageFragmentRoot root= (IPackageFragmentRoot)fragments[i].getAncestor(IJavaScriptElement.PACKAGE_FRAGMENT_ROOT);
			if (!root.isArchive()) {
				toProcess.add(fragments[i]);
			}
		}
		if (toProcess.size() == 0)
			return null;
		return new LogicalPackageResourceMapping((IPackageFragment[])toProcess.toArray(new IPackageFragment[toProcess.size()]));
	}
}
