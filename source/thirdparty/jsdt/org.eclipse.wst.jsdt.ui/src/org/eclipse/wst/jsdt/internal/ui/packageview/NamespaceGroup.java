/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/**
 * 
 */
package org.eclipse.wst.jsdt.internal.ui.packageview;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.wst.jsdt.core.IClassFile;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.IParent;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;

public class NamespaceGroup implements IAdaptable {
	String fNamePrefix;
	int fNamePrefixLength;
	private IPackageFragmentRoot fPackageFragmentRoot;
	private PackageFragmentRootContainer fPackageFragmentRootContainer;
	private IJavaScriptUnit fJavaScriptUnit;

	public static final class WorkBenchAdapter implements IWorkbenchAdapter {
		private static final String EMPTY_STRING = ""; //$NON-NLS-1$

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.ui.model.IWorkbenchAdapter#getChildren(java.lang.Object
		 * )
		 */
		public Object[] getChildren(Object o) {
			if (o instanceof NamespaceGroup)
				return ((NamespaceGroup) o).getChildren();
			return new Object[0];
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.ui.model.IWorkbenchAdapter#getImageDescriptor(java.
		 * lang.Object)
		 */
		public ImageDescriptor getImageDescriptor(Object object) {
			return JavaPluginImages.DESC_OBJS_LOGICAL_PACKAGE;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.ui.model.IWorkbenchAdapter#getLabel(java.lang.Object)
		 */
		public String getLabel(Object o) {
			if (o instanceof NamespaceGroup) {
				return ((NamespaceGroup) o).fNamePrefix;
			}
			return EMPTY_STRING;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.ui.model.IWorkbenchAdapter#getParent(java.lang.Object)
		 */
		public Object getParent(Object o) {
			// TODO Auto-generated method stub
			System.out.println("Unimplemented method:WorkBenchAdapter.getParent"); //$NON-NLS-1$
			return null;
		}

	}

	/**
	 * <p>Create a {@link NamespaceGroup} with a {@link IPackageFragmentRoot} as the parent</p>
	 * 
	 * @param root parent of this group
	 * @param prefix the prefix of this group
	 */
	public NamespaceGroup(IPackageFragmentRoot root, String prefix) {
		fPackageFragmentRoot = root;
		fNamePrefix = prefix;
		fNamePrefixLength = fNamePrefix.length();
		this.fJavaScriptUnit = null;
	}

	/**
	 * <p>Create a {@link NamespaceGroup} with a {@link PackageFragmentRootContainer} as the parent</p>
	 * 
	 * @param root parent of this group
	 * @param prefix the prefix of this group
	 */
	public NamespaceGroup(PackageFragmentRootContainer root, String prefix) {
		fPackageFragmentRootContainer = root;
		fNamePrefix = prefix;
		fNamePrefixLength = fNamePrefix.length();
		this.fJavaScriptUnit = null;
	}
	
	/**
	 * <p>Create a {@link NamespaceGroup} with a {@link IJavaScriptUnit} as the parent</p>
	 * 
	 * @param unit parent of this group
	 * @param prefix the prefix of this group
	 */
	public NamespaceGroup(IJavaScriptUnit unit, String prefix) {
		fNamePrefix = prefix;
		fNamePrefixLength = fNamePrefix.length();
		this.fJavaScriptUnit = unit;
	}

	/**
	 * @return If this group has a {@link IPackageFragmentRoot} as its parent then
	 * returns that parent, else returns <code>null</code>
	 */
	public IPackageFragmentRoot getPackageFragmentRoot() {
		return fPackageFragmentRoot;
	}

	/**
	 * @return If this group has a {@link PackageFragmentRootContainer} as its parent then
	 * returns that parent, else returns <code>null</code>
	 */
	public PackageFragmentRootContainer getPackageFragmentRootContainer() {
		return fPackageFragmentRootContainer;
	}
	
	/**
	 * @return If this group has a {@link IJavaScriptUnit} as its parent then
	 * returns that parent, else returns <code>null</code>
	 */
	public IJavaScriptUnit getJavaScriptUnit() {
		return this.fJavaScriptUnit;
	}
	
	Object getParent() {
		if (fPackageFragmentRoot != null)
			return fPackageFragmentRoot;
		if (fPackageFragmentRootContainer != null)
			return fPackageFragmentRootContainer;
		if(fJavaScriptUnit != null) {
			return this.fJavaScriptUnit;
		}
		return null;
	}
	
	private int computeParentHash() {
		if (fPackageFragmentRoot != null)
			return fPackageFragmentRoot.hashCode();
		if (fPackageFragmentRootContainer != null)
			return fPackageFragmentRootContainer.hashCode();
		return 0;
	}

	public String getText() {
		return fNamePrefix;
	}
	
	public boolean equals(Object obj) {
		if (obj instanceof NamespaceGroup && getParent() != null) {
			return fNamePrefix.equals(((NamespaceGroup) obj).fNamePrefix) && getParent().equals(((NamespaceGroup) obj).getParent());
		}
		return super.equals(obj);
	}
	
	public int hashCode() {
		return computeParentHash() + super.hashCode();
	}
	
	/*
	 * Copied from org.eclipse.wst.jsdt.ui.StandardJavaScriptElementContentProvider
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

		List v= new ArrayList();
		for (int i= 0; i < children.length; i++) {
			if (matches(children[i]))
				continue;
			v.add(children[i]);
		}

		IJavaScriptElement[] result = (IJavaScriptElement[]) v.toArray(new IJavaScriptElement[v.size()]);
		return result;
	}

	/*
	 * Copied from org.eclipse.wst.jsdt.ui.StandardJavaScriptElementContentProvider
	 */
	protected boolean matches(IJavaScriptElement element) {
		if (element.getElementType() == IJavaScriptElement.TYPE && (element.getParent().getElementType() == IJavaScriptElement.JAVASCRIPT_UNIT || element.getParent().getElementType() == IJavaScriptElement.CLASS_FILE)) {
			IType type = (IType) element;
			try {
				return type.isAnonymous();
			}
			catch (JavaScriptModelException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return false;
	}

	public Object[] getChildren() {
		Object[] children = null;
		try {
			children = ((IParent)this.getParent()).getChildren();
		}
		catch (JavaScriptModelException ex1) {
			// TODO Auto-generated catch block
			ex1.printStackTrace();
		}
		if (children == null)
			return null;
		List allChildren = new ArrayList();

		boolean unique = false;
		try {
			while (!unique && children != null && children.length > 0) {
				for (int i = 0; i < children.length; i++) {
					String display1 = ((IJavaScriptElement) children[0]).getDisplayName();
					String display2 = ((IJavaScriptElement) children[i]).getDisplayName();
					if (!((display1 == display2) || (display1 != null && display1.compareTo(display2) == 0))) {
						allChildren.addAll(Arrays.asList(children));
						unique = true;
						break;
					}
				}
				List more = new ArrayList();
				for (int i = 0; !unique && i < children.length; i++) {
					if (children[i] instanceof IPackageFragment) {
						more.addAll(Arrays.asList(((IPackageFragment) children[i]).getChildren()));
					}
					else if (children[i] instanceof IPackageFragmentRoot) {
						more.addAll(Arrays.asList(((IPackageFragmentRoot) children[i]).getChildren()));
					}
					else if (children[i] instanceof IClassFile) {
						more.addAll(Arrays.asList(filter(((IClassFile) children[i]).getChildren())));
					}
					else if (children[i] instanceof IJavaScriptUnit) {
						more.addAll(Arrays.asList(filter(((IJavaScriptUnit) children[i]).getChildren())));
					}
					else {
						/* bottomed out, now at javaElement level */
						unique = true;
						break;
					}

				}
				if (!unique)
					children = more.toArray();
			}
		}
		catch (JavaScriptModelException ex) {
			// TODO Auto-generated catch block
			ex.printStackTrace();
		}


		return allChildren.toArray();
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	public Object getAdapter(Class adapter) {
		if (adapter == IWorkbenchAdapter.class) {
			return new WorkBenchAdapter();
		}
//		else if (adapter == IProject.class) {
//			return getParent().getProject().getProject();
//		}

		return Platform.getAdapterManager().getAdapter(this, adapter);
	}
	
	public String toString() {
		return "Namespacegroup: " + fNamePrefix; //$NON-NLS-1$
	}
}
