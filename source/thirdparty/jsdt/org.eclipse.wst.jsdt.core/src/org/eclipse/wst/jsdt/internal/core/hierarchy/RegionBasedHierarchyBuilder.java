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
package org.eclipse.wst.jsdt.internal.core.hierarchy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.wst.jsdt.core.IClassFile;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.core.JavaModelManager;
import org.eclipse.wst.jsdt.internal.core.JavaProject;
import org.eclipse.wst.jsdt.internal.core.Openable;
import org.eclipse.wst.jsdt.internal.core.SearchableEnvironment;

public class RegionBasedHierarchyBuilder extends HierarchyBuilder {

	public RegionBasedHierarchyBuilder(TypeHierarchy hierarchy)
		throws JavaScriptModelException {

		super(hierarchy);
	}

public void build(boolean computeSubtypes) {

	JavaModelManager manager = JavaModelManager.getJavaModelManager();
	try {
		// optimize access to zip files while building hierarchy
		manager.cacheZipFiles();

		if (this.hierarchy.focusType == null || computeSubtypes) {
			IProgressMonitor typeInRegionMonitor =
				this.hierarchy.progressMonitor == null ?
					null :
					new SubProgressMonitor(this.hierarchy.progressMonitor, 30);
			HashMap allOpenablesInRegion = determineOpenablesInRegion(typeInRegionMonitor);
			this.hierarchy.initialize(allOpenablesInRegion.size());
			IProgressMonitor buildMonitor =
				this.hierarchy.progressMonitor == null ?
					null :
					new SubProgressMonitor(this.hierarchy.progressMonitor, 70);
			createTypeHierarchyBasedOnRegion(allOpenablesInRegion, buildMonitor);
			((RegionBasedTypeHierarchy)this.hierarchy).pruneDeadBranches();
		} else {
			this.hierarchy.initialize(1);
			this.buildSupertypes();
		}
	} finally {
		manager.flushZipFiles();
	}
}
/**
 * Configure this type hierarchy that is based on a region.
 */
private void createTypeHierarchyBasedOnRegion(HashMap allOpenablesInRegion, IProgressMonitor monitor) {

	try {
		int size = allOpenablesInRegion.size();
		if (monitor != null) monitor.beginTask("", size * 2/* 1 for build binding, 1 for connect hierarchy*/); //$NON-NLS-1$
		this.infoToHandle = new HashMap(size);
		Iterator javaProjects = allOpenablesInRegion.entrySet().iterator();
		while (javaProjects.hasNext()) {
			Map.Entry entry = (Map.Entry) javaProjects.next();
			JavaProject project = (JavaProject) entry.getKey();
			ArrayList allOpenables = (ArrayList) entry.getValue();
			Openable[] openables = new Openable[allOpenables.size()];
			allOpenables.toArray(openables);

			try {
				// resolve
				SearchableEnvironment searchableEnvironment = project.newSearchableNameEnvironment(this.hierarchy.workingCopies);
				this.nameLookup = searchableEnvironment.nameLookup;
				this.hierarchyResolver.resolve(openables, null, monitor);
			} catch (JavaScriptModelException e) {
				// project doesn't exit: ignore
			}
		}
	} finally {
		if (monitor != null) monitor.done();
	}
}

	/**
	 * Returns all of the openables defined in the region of this type hierarchy.
	 * Returns a map from IJavaScriptProject to ArrayList of Openable
	 */
	private HashMap determineOpenablesInRegion(IProgressMonitor monitor) {

		try {
			HashMap allOpenables = new HashMap();
			IJavaScriptElement[] roots =
				((RegionBasedTypeHierarchy) this.hierarchy).region.getElements();
			int length = roots.length;
			if (monitor != null) monitor.beginTask("", length); //$NON-NLS-1$
			for (int i = 0; i <length; i++) {
				IJavaScriptElement root = roots[i];
				IJavaScriptProject javaProject = root.getJavaScriptProject();
				ArrayList openables = (ArrayList) allOpenables.get(javaProject);
				if (openables == null) {
					openables = new ArrayList();
					allOpenables.put(javaProject, openables);
				}
				switch (root.getElementType()) {
					case IJavaScriptElement.JAVASCRIPT_PROJECT :
						injectAllOpenablesForJavaProject((IJavaScriptProject) root, openables);
						break;
					case IJavaScriptElement.PACKAGE_FRAGMENT_ROOT :
						injectAllOpenablesForPackageFragmentRoot((IPackageFragmentRoot) root, openables);
						break;
					case IJavaScriptElement.PACKAGE_FRAGMENT :
						injectAllOpenablesForPackageFragment((IPackageFragment) root, openables);
						break;
					case IJavaScriptElement.CLASS_FILE :
					case IJavaScriptElement.JAVASCRIPT_UNIT :
						openables.add(root);
						break;
					case IJavaScriptElement.TYPE :
						IType type = (IType)root;
						if (type.isBinary()) {
							openables.add(type.getClassFile());
						} else {
							openables.add(type.getJavaScriptUnit());
						}
						break;
					default :
						break;
				}
				worked(monitor, 1);
			}
			return allOpenables;
		} finally {
			if (monitor != null) monitor.done();
		}
	}

	/**
	 * Adds all of the openables defined within this java project to the
	 * list.
	 */
	private void injectAllOpenablesForJavaProject(
		IJavaScriptProject project,
		ArrayList openables) {
		try {
			IPackageFragmentRoot[] devPathRoots =
				((JavaProject) project).getPackageFragmentRoots();
			if (devPathRoots == null) {
				return;
			}
			for (int j = 0; j < devPathRoots.length; j++) {
				IPackageFragmentRoot root = devPathRoots[j];
				injectAllOpenablesForPackageFragmentRoot(root, openables);
			}
		} catch (JavaScriptModelException e) {
			// ignore
		}
	}

	/**
	 * Adds all of the openables defined within this package fragment to the
	 * list.
	 */
	private void injectAllOpenablesForPackageFragment(
		IPackageFragment packFrag,
		ArrayList openables) {

		try {
			IPackageFragmentRoot root = (IPackageFragmentRoot) packFrag.getParent();
			int kind = root.getKind();
			if (kind != 0) {
				boolean isSourcePackageFragment = (kind == IPackageFragmentRoot.K_SOURCE);
				if (isSourcePackageFragment) {
					IJavaScriptUnit[] cus = packFrag.getJavaScriptUnits();
					for (int i = 0, length = cus.length; i < length; i++) {
						openables.add(cus[i]);
					}
				} else {
					IClassFile[] classFiles = packFrag.getClassFiles();
					for (int i = 0, length = classFiles.length; i < length; i++) {
						openables.add(classFiles[i]);
					}
				}
			}
		} catch (JavaScriptModelException e) {
			// ignore
		}
	}

	/**
	 * Adds all of the openables defined within this package fragment root to the
	 * list.
	 */
	private void injectAllOpenablesForPackageFragmentRoot(
		IPackageFragmentRoot root,
		ArrayList openables) {
		try {
			IJavaScriptElement[] packFrags = root.getChildren();
			for (int k = 0; k < packFrags.length; k++) {
				IPackageFragment packFrag = (IPackageFragment) packFrags[k];
				injectAllOpenablesForPackageFragment(packFrag, openables);
			}
		} catch (JavaScriptModelException e) {
			return;
		}
	}

}
