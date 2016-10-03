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
package org.eclipse.wst.jsdt.internal.core;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.core.util.HashtableOfArrayToObject;

public class LookupScopeElementInfo extends PackageFragmentRootInfo {

	private JavaProject javaProject;
	private IPackageFragmentRoot[] rootsInScope;
	private LookupCache cache;

	//public static final String[] SYSTEM_LIBRARIES = {"system.js"};

	/* places imports in the document scope before the classpath entries */
	private static final boolean LOOKUP_LOCAL_SCOPE_FIRST = false;

	static class LookupCache {
		LookupCache(IPackageFragmentRoot[] allPkgFragmentRootsCache, HashtableOfArrayToObject allPkgFragmentsCache, HashtableOfArrayToObject isPackageCache, Map rootToResolvedEntries) {
			this.allPkgFragmentRootsCache = allPkgFragmentRootsCache;
			this.allPkgFragmentsCache = allPkgFragmentsCache;
			this.isPackageCache = isPackageCache;
			this.rootToResolvedEntries = rootToResolvedEntries;
		}

		/*
		 * A cache of all package fragment roots of this project.
		 */
		public IPackageFragmentRoot[] allPkgFragmentRootsCache;

		/*
		 * A cache of all package fragments in this project.
		 * (a map from String[] (the package name) to IPackageFragmentRoot[] (the package fragment roots that contain a package fragment with this name)
		 */
		public HashtableOfArrayToObject allPkgFragmentsCache;

		/*
		 * A set of package names (String[]) that are known to be packages.
		 */
		public HashtableOfArrayToObject isPackageCache;

		public Map rootToResolvedEntries;
	}


	public IPackageFragmentRoot[] getAllRoots() {
		if(LOOKUP_LOCAL_SCOPE_FIRST) return getAllRootsLocalFirst();

		return getAllRootsGlobalFirst();
	}

	private IPackageFragmentRoot[] getAllRootsGlobalFirst() {
		IPackageFragmentRoot[] projectRoots = new IPackageFragmentRoot[0];
		int lastGood = 0;
		try {
			projectRoots = javaProject.getPackageFragmentRoots();


			for(int i =0;i<projectRoots.length;i++) {
				if(projectRoots[i].isLanguageRuntime()) {
					projectRoots[lastGood++]=projectRoots[i];
				}
			}
		} catch (JavaScriptModelException ex) {
			projectRoots = new IPackageFragmentRoot[0];
		}

		IPackageFragmentRoot[]  allRoots = new IPackageFragmentRoot[lastGood + rootsInScope.length ];

		System.arraycopy(projectRoots, 0, allRoots, 0, lastGood);
		System.arraycopy(rootsInScope, 0, allRoots, lastGood, rootsInScope.length);
		return allRoots;
	}

	private IPackageFragmentRoot[] getAllRootsLocalFirst() {
		IPackageFragmentRoot[] projectRoots = new IPackageFragmentRoot[0];
		int lastGood = 0;
		try {
			projectRoots = javaProject.getPackageFragmentRoots();


			for(int i =0;i<projectRoots.length;i++) {
				if(projectRoots[i].isLanguageRuntime()) {
					projectRoots[lastGood++]=projectRoots[i];
				}
			}
		} catch (JavaScriptModelException ex) {
			projectRoots = new IPackageFragmentRoot[0];
		}

		IPackageFragmentRoot[]  allRoots = new IPackageFragmentRoot[lastGood + rootsInScope.length ];

		System.arraycopy(rootsInScope, 0, allRoots, 0, rootsInScope.length);
		System.arraycopy(projectRoots, 0, allRoots, rootsInScope.length, lastGood);
		return allRoots;
	}

	public LookupScopeElementInfo(JavaProject project,IPackageFragmentRoot[] rootsInScope){
		this.javaProject = project;
		this.rootsInScope = rootsInScope;


	}

	NameLookup newNameLookup(IJavaScriptUnit[] workingCopies) {
		BuildLookupScopeCache(getAllRoots());

		return new NameLookup(cache.allPkgFragmentRootsCache, cache.allPkgFragmentsCache, workingCopies, cache.rootToResolvedEntries);
	}

	public LookupCache BuildLookupScopeCache(IPackageFragmentRoot[] rootsInScope) {


		Map reverseMap = new HashMap(3);
//		IPackageFragmentRoot[] roots=null;
//
//		for(int i = 0;i<rootsInScope.length;i++) {
//			try {
//
//				IIncludePathEntry entry = javaProject.getClasspathEntryFor(rootsInScope[i].getPath());
//				reverseMap.put(rootsInScope[i],entry);
//			} catch (JavaScriptModelException ex) {
//				// TODO Auto-generated catch block
//				ex.printStackTrace();
//			}
//
//		}

//		try {
//			roots = javaProject.getAllPackageFragmentRoots(reverseMap);
//		} catch (JavaScriptModelException e) {
//			// project does not exist: cannot happen since this is the info of the project
//			roots = new IPackageFragmentRoot[0];
//			reverseMap.clear();
//		}
		HashtableOfArrayToObject fragmentsCache = new HashtableOfArrayToObject();
		HashtableOfArrayToObject isPackageCache = new HashtableOfArrayToObject();
			for (int i = 0, length = rootsInScope.length; i < length; i++) {
				IPackageFragmentRoot root = rootsInScope[i];
				IJavaScriptElement[] frags = null;
				try {
					if(root instanceof DocumentContextFragmentRoot) {
						LibraryFragmentRootInfo info = new LibraryFragmentRootInfo();
						((DocumentContextFragmentRoot) root).computeChildren(info, new HashMap());
						frags = info.children;
					}else if (root instanceof LibraryFragmentRoot) {
						LibraryFragmentRootInfo info = new LibraryFragmentRootInfo();
						((LibraryFragmentRoot) root).computeChildren(info, new HashMap());
						frags = info.children;
//					} else if (root.isArchive() && !root.isOpen()) {
//						JarPackageFragmentRootInfo info = new JarPackageFragmentRootInfo();
//						((JarPackageFragmentRoot) root).computeChildren(info, new HashMap());
//						frags = info.children;
					} else if (root instanceof PackageFragmentRoot) {
						PackageFragmentRootInfo info = new PackageFragmentRootInfo();
						((PackageFragmentRoot) root).computeChildren(info, new HashMap());
						frags = info.children;
					}else
						frags = root.getChildren();
				} catch (JavaScriptModelException e) {
					// root doesn't exist: ignore
					continue;
				}
				for (int j = 0, length2 = frags.length; j < length2; j++) {
					PackageFragment fragment= (PackageFragment) frags[j];
					/* Keep folders off the classpath */
					//if(fragment.getPath().getFileExtension()==null || !fragment.getPath().getFileExtension().equals(".js")) continue;
					String[] pkgName = fragment.names;
					Object existing = fragmentsCache.get(pkgName);
					if (existing == null) {
						fragmentsCache.put(pkgName, root);
						// cache whether each package and its including packages (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=119161)
						// are actual packages
						addNames(pkgName, isPackageCache);
					} else {
						if (existing instanceof PackageFragmentRoot) {
							fragmentsCache.put(pkgName, new IPackageFragmentRoot[] {(PackageFragmentRoot) existing, root});
						} else {
							IPackageFragmentRoot[] entry= (IPackageFragmentRoot[]) existing;
							IPackageFragmentRoot[] copy= new IPackageFragmentRoot[entry.length + 1];
							System.arraycopy(entry, 0, copy, 0, entry.length);
							copy[entry.length]= root;
							fragmentsCache.put(pkgName, copy);
						}
					}
				}
			}
			cache = new LookupCache(rootsInScope, fragmentsCache, isPackageCache, reverseMap);


		return cache;
	}

	public static void addNames(String[] name, HashtableOfArrayToObject set) {
		set.put(name, name);
		int length = name.length;
		for (int i = length-1; i > 0; i--) {
			String[] superName = new String[i];
			System.arraycopy(name, 0, superName, 0, i);
			set.put(superName, superName);
		}
	}

}
