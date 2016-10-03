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
package org.eclipse.wst.jsdt.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.wst.jsdt.core.IClassFile;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJarEntryResource;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptElementDelta;
import org.eclipse.wst.jsdt.core.IJavaScriptModel;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.IParent;
import org.eclipse.wst.jsdt.core.ISourceReference;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.ITypeRoot;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.ui.packageview.NamespaceGroup;
import org.eclipse.wst.jsdt.internal.ui.packageview.PackageFragmentRootContainer;
 
/**
 * A base content provider for JavaScriptelements. It provides access to the
 * JavaScriptelement hierarchy without listening to changes in the JavaScriptmodel.
 * If updating the presentation on JavaScript model change is required than 
 * clients have to subclass, listen to JavaScript model changes and have to update
 * the UI using corresponding methods provided by the JFace viewers or their 
 * own UI presentation.
 * <p>
 * The following JavaScript element hierarchy is surfaced by this content provider:
 * <p>
 * <pre>
JavaScript model (<code>IJavaScriptModel</code>)
   JavaScript project (<code>IJavaScriptProject</code>)
      package fragment root (<code>IPackageFragmentRoot</code>)
         package fragment (<code>IPackageFragment</code>)
            compilation unit (<code>IJavaScriptUnit</code>)
            binary class file (<code>IClassFile</code>)
 * </pre>
 * </p> 			
 * <p>
 * Note that when the entire JavaScript project is declared to be package fragment root,
 * the corresponding package fragment root element that normally appears between the
 * JavaScript project and the package fragments is automatically filtered out.
 * </p>
 * 
 * 
 */
public class StandardJavaScriptElementContentProvider implements ITreeContentProvider, IWorkingCopyProvider {

	protected static final Object[] NO_CHILDREN= new Object[0];
	protected boolean fProvideMembers;
	protected boolean fProvideWorkingCopy;
	
	/**
	 * Creates a new content provider. The content provider does not
	 * provide members of compilation units or class files.
	 */	
	public StandardJavaScriptElementContentProvider() {
		this(false);
	}
	
	/**
	 * Creates a new <code>StandardJavaScriptElementContentProvider</code>.
	 *
	 * @param provideMembers if <code>true</code> members below compilation units 
	 * and class files are provided. 
	 */
	public StandardJavaScriptElementContentProvider(boolean provideMembers) {
		fProvideMembers= provideMembers;
		fProvideWorkingCopy= provideMembers;
	}
	
	/**
	 * Returns whether members are provided when asking
	 * for a compilation units or class file for its children.
	 * 
	 * @return <code>true</code> if the content provider provides members; 
	 * otherwise <code>false</code> is returned
	 */
	public boolean getProvideMembers() {
		return fProvideMembers;
	}

	/**
	 * Sets whether the content provider is supposed to return members
	 * when asking a compilation unit or class file for its children.
	 * 
	 * @param b if <code>true</code> then members are provided. 
	 * If <code>false</code> compilation units and class files are the
	 * leaves provided by this content provider.
	 */
	public void setProvideMembers(boolean b) {
		//hello
		fProvideMembers= b;
	}
	
	/**
	 * @deprecated Since 3.0 compilation unit children are always provided as working copies. The JavaScript model
	 * does not support the 'original' mode anymore. 
	 */
	public boolean getProvideWorkingCopy() {
		return fProvideWorkingCopy;
	}

	/* (non-Javadoc)
	 * @see IWorkingCopyProvider#providesWorkingCopies()
	 */
	public boolean providesWorkingCopies() {
		return getProvideWorkingCopy();
	}

	/* (non-Javadoc)
	 * Method declared on IStructuredContentProvider.
	 */
	public Object[] getElements(Object parent) {
		return getChildren(parent);
	}
	
	/* (non-Javadoc)
	 * Method declared on IContentProvider.
	 */
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}

	/* (non-Javadoc)
	 * Method declared on IContentProvider.
	 */
	public void dispose() {
	}

	/* (non-Javadoc)
	 * Method declared on ITreeContentProvider.
	 */
	public Object[] getChildren(Object element) {
		if (!exists(element))
			return NO_CHILDREN;
			
		try {
			if (element instanceof IJavaScriptModel) 
				return getJavaProjects((IJavaScriptModel)element);
			
			if (element instanceof IJavaScriptProject) 
				return getPackageFragmentRoots((IJavaScriptProject)element);
			
			if (element instanceof IPackageFragmentRoot) 
				return getPackageFragmentRootContent((IPackageFragmentRoot)element);
			
			if (element instanceof IPackageFragment) 
				return getPackageContent((IPackageFragment)element);
				
			if (element instanceof IFolder)
				return getFolderContent((IFolder)element);
			
			if (element instanceof IJarEntryResource) {
				return ((IJarEntryResource) element).getChildren();
			}
			
			if (getProvideMembers() && element instanceof ISourceReference && element instanceof IParent) {
				
				//@GINO: Anonymous Filter top level anonymous
				if( element instanceof ITypeRoot )
					return filter( ((IParent)element).getChildren() );
				else 
					return ((IParent)element).getChildren();
				
			}
		} catch (CoreException e) {
			return NO_CHILDREN;
		}		
		return NO_CHILDREN;	
	}
	
	/*
	 * @GINO: Anonymous -- matches anonymous types on the top level
	 */
	protected boolean matches(IJavaScriptElement element) {
			
		if (element.getElementType() == IJavaScriptElement.TYPE && (element.getParent().getElementType() == IJavaScriptElement.JAVASCRIPT_UNIT || element.getParent().getElementType() == IJavaScriptElement.CLASS_FILE) ) {
			
			IType type = (IType)element;
			try {
				return type.isAnonymous();
			} catch (JavaScriptModelException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		return false;
	}

	/*
	 * @GINO: Anonymous Filter from top level
	 *
	 */
	protected IJavaScriptElement[] filter(IJavaScriptElement[] children) {
		boolean initializers= false;
		for (int i= 0; i < children.length; i++) {
			if (matches(children[i])) {
				initializers= true;
				break;
			}
		}

		if (!initializers)
			return children;

		Vector v= new Vector();
		for (int i= 0; i < children.length; i++) {
			if (matches(children[i]))
				continue;
			v.addElement(children[i]);
		}

		IJavaScriptElement[] result= new IJavaScriptElement[v.size()];
		v.copyInto(result);
		return result;
	}

	/* (non-Javadoc)
	 * @see ITreeContentProvider
	 */
	public boolean hasChildren(Object element) {
		if (getProvideMembers()) {
			// assume CUs and class files are never empty
			if (element instanceof IJavaScriptUnit ||	element instanceof IClassFile) {
				try {
					if(element instanceof IJavaScriptUnit ) {
						IJavaScriptUnit cu = (IJavaScriptUnit)element;
						return cu.hasChildren();
					}else if(element instanceof IClassFile) {
						IClassFile cf = (IClassFile)element;
						return cf.hasChildren();
					}
				}catch(JavaScriptModelException ex) {
					return false;
				}
				
				
				return true;
			}
		} else {
			// don't allow to drill down into a compilation unit or class file
			if (element instanceof IJavaScriptUnit ||
				element instanceof IClassFile ||
				element instanceof IFile)
			return false;
		}
			
		if (element instanceof IJavaScriptProject) {
			IJavaScriptProject jp= (IJavaScriptProject)element;
			if (!jp.getProject().isOpen()) {
				return false;
			}	
		}
		
		if (element instanceof IParent) {
			try {
				// when we have JavaScript children return true, else we fetch all the children
				if (((IParent)element).hasChildren())
					return true;
			} catch(JavaScriptModelException e) {
				return true;
			}
		}
		
		if(element instanceof NamespaceGroup || element instanceof PackageFragmentRootContainer) {
			return true;
		}
		Object[] children= getChildren(element);
		return (children != null) && children.length > 0;
	}
	 
	/* (non-Javadoc)
	 * Method declared on ITreeContentProvider.
	 */
	public Object getParent(Object element) {
		if (!exists(element))
			return null;
		return internalGetParent(element);			
	}
	
	/**
	 * Evaluates all children of a given {@link IPackageFragmentRoot}. Clients can override this method.
	 * @param root The root to evaluate the children for.
	 * @return The children of the root
	 * @exception JavaScriptModelException if the package fragment root does not exist or if an
	 *      exception occurs while accessing its corresponding resource
	 *      
	 * 
	 */
	protected Object[] getPackageFragmentRootContent(IPackageFragmentRoot root) throws JavaScriptModelException {
		IJavaScriptElement[] fragments= root.getChildren();
		if (isProjectPackageFragmentRoot(root)) {
			return fragments;
		}
		Object[] nonJavaResources= root.getNonJavaScriptResources();
		if (nonJavaResources == null)
			return fragments;
		return concatenate(fragments, nonJavaResources);
	}
	
	/**
	 * Evaluates all children of a given {@link IJavaScriptProject}. Clients can override this method.
	 * @param project The JavaScript project to evaluate the children for.
	 * @return The children of the project. Typically these are package fragment roots but can also be other elements.
	 * @exception JavaScriptModelException if the JavaScript project does not exist or if an
	 *      exception occurs while accessing its corresponding resource
	 */
	protected Object[] getPackageFragmentRoots(IJavaScriptProject project) throws JavaScriptModelException {
		if (!project.getProject().isOpen())
			return NO_CHILDREN;
			
		IPackageFragmentRoot[] roots= project.getPackageFragmentRoots();
		List list= new ArrayList(roots.length);
		// filter out package fragments that correspond to projects and
		// replace them with the package fragments directly
		for (int i= 0; i < roots.length; i++) {
			IPackageFragmentRoot root= roots[i];
			if (isProjectPackageFragmentRoot(root)) {
				Object[] fragments= getPackageFragmentRootContent(root);
				for (int j= 0; j < fragments.length; j++) {
					list.add(fragments[j]);
				}
			} else {
				list.add(root);
			} 
		}
		Object[] resources= project.getNonJavaScriptResources();
		for (int i= 0; i < resources.length; i++) {
			list.add(resources[i]);
		}
		return list.toArray();
	}

	/**
	 * Note: This method is for internal use only. Clients should not call this method.
	 */
	protected Object[] getJavaProjects(IJavaScriptModel jm) throws JavaScriptModelException {
		return jm.getJavaScriptProjects();
	}
	
	/**
	 * Evaluates all children of a given {@link IPackageFragment}. Clients can override this method.
	 * @param fragment The fragment to evaluate the children for.
	 * @return The children of the given package fragment.
	 * @exception JavaScriptModelException if the package fragment does not exist or if an
	 *      exception occurs while accessing its corresponding resource
	 *      
	 * 
	 */
	protected Object[] getPackageContent(IPackageFragment fragment) throws JavaScriptModelException {
		if (fragment.getKind() == IPackageFragmentRoot.K_SOURCE) {
			return concatenate(fragment.getJavaScriptUnits(), fragment.getNonJavaScriptResources());
		}
		return concatenate(fragment.getClassFiles(), fragment.getNonJavaScriptResources());
	}
	
	/**
	 * Evaluates all children of a given {@link IFolder}. Clients can override this method.
	 * @param folder The folder to evaluate the children for.
	 * @return The children of the given package fragment.
	 * @exception CoreException if the folder does not exist.
	 *      
	 * 
	 */
	protected Object[] getFolderContent(IFolder folder) throws CoreException {
		IResource[] members= folder.members();
		IJavaScriptProject javaProject= JavaScriptCore.create(folder.getProject());
		if (javaProject == null || !javaProject.exists())
			return members;
		boolean isFolderOnClasspath = javaProject.isOnIncludepath(folder);
		List nonJavaResources= new ArrayList();
		// Can be on classpath but as a member of non-JavaScript resource folder
		for (int i= 0; i < members.length; i++) {
			IResource member= members[i];
			// A resource can also be a JavaScript element
			// in the case of exclusion and inclusion filters.
			// We therefore exclude JavaScript elements from the list
			// of non-JavaScript resources.
			if (isFolderOnClasspath) {
				if (javaProject.findPackageFragmentRoot(member.getFullPath()) == null) {
					nonJavaResources.add(member);
				} 
			} else if (!javaProject.isOnIncludepath(member)) {
				nonJavaResources.add(member);
			}
		}
		return nonJavaResources.toArray();
	}
	
	/**
	 * Note: This method is for internal use only. Clients should not call this method.
	 */
	protected boolean isClassPathChange(IJavaScriptElementDelta delta) {
		
		// need to test the flags only for package fragment roots
		if (delta.getElement().getElementType() != IJavaScriptElement.PACKAGE_FRAGMENT_ROOT)
			return false;
		
		int flags= delta.getFlags();
		return (delta.getKind() == IJavaScriptElementDelta.CHANGED && 
			((flags & IJavaScriptElementDelta.F_ADDED_TO_CLASSPATH) != 0) ||
			 ((flags & IJavaScriptElementDelta.F_REMOVED_FROM_CLASSPATH) != 0) ||
			 ((flags & IJavaScriptElementDelta.F_REORDER) != 0));
	}
	
	/**
	 * Note: This method is for internal use only. Clients should not call this method.
	 */
	protected Object skipProjectPackageFragmentRoot(IPackageFragmentRoot root) {
		if (isProjectPackageFragmentRoot(root))
			return root.getParent(); 
		return root;
	}
	
	/**
	 * Note: This method is for internal use only. Clients should not call this method.
	 */
	protected boolean isPackageFragmentEmpty(IJavaScriptElement element) throws JavaScriptModelException {
		if (element instanceof IPackageFragment) {
			IPackageFragment fragment= (IPackageFragment)element;
			if (fragment.exists() && !(fragment.hasChildren() || fragment.getNonJavaScriptResources().length > 0) && fragment.hasSubpackages()) 
				return true;
		}
		return false;
	}

	/**
	 * Note: This method is for internal use only. Clients should not call this method.
	 */
	protected boolean isProjectPackageFragmentRoot(IPackageFragmentRoot root) {
		IJavaScriptProject javaProject= root.getJavaScriptProject();
		return javaProject != null && javaProject.getPath().equals(root.getPath());
	}
	
	/**
	 * Note: This method is for internal use only. Clients should not call this method.
	 */
	protected boolean exists(Object element) {
		if (element == null) {
			return false;
		}
		if (element instanceof IResource) {
			return ((IResource)element).exists();
		}
		if (element instanceof IJavaScriptElement) {
			return ((IJavaScriptElement)element).exists();
		}
		return true;
	}
	
	/**
	 * Note: This method is for internal use only. Clients should not call this method.
	 */
	protected Object internalGetParent(Object element) {

		// try to map resources to the containing package fragment
		if (element instanceof IResource) {
			IResource parent= ((IResource)element).getParent();
			IJavaScriptElement jParent= JavaScriptCore.create(parent);
			// http://bugs.eclipse.org/bugs/show_bug.cgi?id=31374
			if (jParent != null && jParent.exists()) 
				return jParent;
			return parent;
		} else if (element instanceof IJavaScriptElement) {
			IJavaScriptElement parent= ((IJavaScriptElement) element).getParent();
			// for package fragments that are contained in a project package fragment
			// we have to skip the package fragment root as the parent.
			if (element instanceof IPackageFragment) {
				return skipProjectPackageFragmentRoot((IPackageFragmentRoot) parent);
			}
			return parent;
		} else if (element instanceof IJarEntryResource) {
			return ((IJarEntryResource) element).getParent();
		}
		return null;
	}
		
	/**
	 * Note: This method is for internal use only. Clients should not call this method.
	 */
	protected static Object[] concatenate(Object[] a1, Object[] a2) {
		int a1Len= a1.length;
		int a2Len= a2.length;
		if (a1Len == 0) return a2;
		if (a2Len == 0) return a1;
		Object[] res= new Object[a1Len + a2Len];
		System.arraycopy(a1, 0, res, 0, a1Len);
		System.arraycopy(a2, 0, res, a1Len, a2Len); 
		return res;
	}


}
