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
/**
 *
 */
package org.eclipse.wst.jsdt.core;

import java.util.ArrayList;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;


/**
 *  
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 *
 */
public class LibrarySuperType {
	IPath cpEntry;
	String superTypeName;
	String libraryName;
	IJavaScriptProject javaProject;

	public static final String SUPER_TYPE_CONTAINER= "org.eclipse.wst.jsdt.ui.superType.container"; //$NON-NLS-1$
	public static final String SUPER_TYPE_NAME= "org.eclipse.wst.jsdt.ui.superType.name"; //$NON-NLS-1$

	/* Only one superTypeName per instance so enforce that */
	public LibrarySuperType(IPath classPathEntry, IJavaScriptProject project, String superTypeName) {
		this.cpEntry = classPathEntry;
		this.superTypeName = superTypeName;
		this.javaProject =  project;
		this.libraryName = initLibraryName();

	}

	public LibrarySuperType(String classPathEntry, IJavaScriptProject project, String superTypeName) {
		this(new Path(classPathEntry),project,superTypeName);
	}
	/* Construct parent */
	public LibrarySuperType(IPath classPathEntry,  IJavaScriptProject project) {
		this(classPathEntry,project, null);
	}

	public IPath getRawContainerPath() {
		return cpEntry;
	}

	public boolean hasChildren() {
		/* defined super type meeans I'm a child */
		if(superTypeName!=null) return false;
		JsGlobalScopeContainerInitializer init = getContainerInitializer();
		if (init == null) return false;
		String[] availableSuperTypes = init.containerSuperTypes();
		return availableSuperTypes!=null && availableSuperTypes.length>0;
	}

	public LibrarySuperType[] getChildren() {
		if(superTypeName!=null) return new LibrarySuperType[0];
		return getFlatLibrarySuperTypes(cpEntry,javaProject);
	}

	public LibrarySuperType getParent() {
		if(superTypeName==null) return null;
		return new LibrarySuperType(cpEntry,javaProject, null);
	}

	public boolean isParent() {
		return getParent()==null;
	}

	public JsGlobalScopeContainerInitializer getContainerInitializer() {
		return getContainerInitializer(cpEntry);
	}

	public IIncludePathEntry[] getClasspathEntries() {
		IJsGlobalScopeContainer container=null;
		try {
			container = JavaScriptCore.getJsGlobalScopeContainer(this.cpEntry, this.javaProject);
		} catch (JavaScriptModelException ex) {
			// TODO Auto-generated catch block
			ex.printStackTrace();
		}
		if(container!=null) return	container.getIncludepathEntries();

		return new IIncludePathEntry[0];
	}

	private static LibrarySuperType[] getFlatLibrarySuperTypes(IPath classPathEntry, IJavaScriptProject javaProject) {
		JsGlobalScopeContainerInitializer init = getContainerInitializer(classPathEntry);
		if (init == null) return new LibrarySuperType[0];
		String[] availableSuperTypes = init.containerSuperTypes();
		LibrarySuperType[] libSupers = new LibrarySuperType[availableSuperTypes.length];
		for (int i = 0; i < availableSuperTypes.length; i++) {
			libSupers[i] = new LibrarySuperType(classPathEntry, javaProject, availableSuperTypes[i]);
		}
		return libSupers;
	}

	public String getSuperTypeName() {
		return superTypeName;
	}

	public String getLibraryName() {
		return libraryName;
	}

	private String initLibraryName() {
		JsGlobalScopeContainerInitializer init = getContainerInitializer();

		/* parent node */
		if(superTypeName==null) {
			if(init==null) {
				return cpEntry.toString();
			}
			return  init.getDescription(cpEntry, javaProject);
		}
		Object parent = getParent();
		if(!(parent instanceof LibrarySuperType)) return null;
		return ((LibrarySuperType)parent).getLibraryName();
	}

	public String toString() {
		//JsGlobalScopeContainerInitializer init = getContainerInitializer();

		/* parent node */
		if(isParent()) {
			return getLibraryName();

		}
		
		return  Messages.getString("LibrarySuperType.0", new Object[]{superTypeName, getLibraryName()}); //$NON-NLS-1$
	}

	public boolean equals(Object o) {
		if(!(o instanceof LibrarySuperType)) return false;

		LibrarySuperType other = (LibrarySuperType)o;



		if(other.cpEntry!=null && !other.cpEntry.equals(cpEntry)) {
			return false;
		}

		if((other.superTypeName==superTypeName)) {
			return true;
		}

		if(other.superTypeName!=null && superTypeName!=null) {
			return other.superTypeName.equals(superTypeName);
		}

		return false;
	}

	public IPackageFragment[] getPackageFragments(){
		IIncludePathEntry[] entries = getClasspathEntries();
		ArrayList allFrags = new ArrayList();

		try {
			for(int i = 0;i<entries.length;i++) {
				IPath path = entries[i].getPath();
				IPackageFragmentRoot root = javaProject.findPackageFragmentRoot(path.makeAbsolute());

				IJavaScriptElement[] children = root.getChildren();
				for(int k = 0;k<children.length;k++) {
					if(children[k] instanceof IPackageFragment) {
						allFrags.add(children[k]);
					}
				}
			}
		} catch (JavaScriptModelException ex) {
			// TODO Auto-generated catch block
			ex.printStackTrace();
		}
		return (IPackageFragment[])allFrags.toArray(new IPackageFragment[allFrags.size()]);
	}

	public static JsGlobalScopeContainerInitializer getContainerInitializer(IPath classPathEntry) {
		if(classPathEntry==null ) return null;
		JsGlobalScopeContainerInitializer initializer= JavaScriptCore.getJsGlobalScopeContainerInitializer(classPathEntry.segment(0));
		return initializer ;
	}
}
