/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptElementDelta;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IOpenable;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.IRegion;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchScope;
import org.eclipse.wst.jsdt.internal.core.CompilationUnit;
import org.eclipse.wst.jsdt.internal.core.JavaElement;
import org.eclipse.wst.jsdt.internal.core.Openable;
import org.eclipse.wst.jsdt.internal.core.Region;
import org.eclipse.wst.jsdt.internal.core.TypeVector;

public class RegionBasedTypeHierarchy extends TypeHierarchy {
	/**
	 * The region of types for which to build the hierarchy
	 */
	protected IRegion region;

/**
 * Creates a TypeHierarchy on the types in the specified region,
 * considering first the given working copies,
 * using the projects in the given region for a name lookup context. If a specific
 * type is also specified, the type hierarchy is pruned to only
 * contain the branch including the specified type.
 */
public RegionBasedTypeHierarchy(IRegion region, IJavaScriptUnit[] workingCopies, IType type, boolean computeSubtypes) {
	super(type, workingCopies, (IJavaScriptSearchScope)null, computeSubtypes);

	Region newRegion = new Region() {
		public void add(IJavaScriptElement element) {
			if (!contains(element)) {
				//"new" element added to region
				removeAllChildren(element);
				fRootElements.add(element);
				if (element.getElementType() == IJavaScriptElement.JAVASCRIPT_PROJECT) {
					// add jar roots as well so that jars don't rely on their parent to know
					// if they are contained in the region
					// (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=146615)
					try {
						IPackageFragmentRoot[] roots = ((IJavaScriptProject) element).getPackageFragmentRoots();
						for (int i = 0, length = roots.length; i < length; i++) {
							if (roots[i].isArchive() && !fRootElements.contains(roots[i]))
								fRootElements.add(roots[i]);
						}
					} catch (JavaScriptModelException e) {
						// project doesn't exist
					}
				}
				fRootElements.trimToSize();
			}
		}
	};
	IJavaScriptElement[] elements = region.getElements();
	for (int i = 0, length = elements.length; i < length; i++) {
		newRegion.add(elements[i]);

	}
	this.region = newRegion;
	if (elements.length > 0)
		this.project = elements[0].getJavaScriptProject();
}
/*
 * @see TypeHierarchy#initializeRegions
 */
protected void initializeRegions() {
	super.initializeRegions();
	IJavaScriptElement[] roots = this.region.getElements();
	for (int i = 0; i < roots.length; i++) {
		IJavaScriptElement root = roots[i];
		if (root instanceof IOpenable) {
			this.files.put(root, new ArrayList());
		} else {
			Openable o = (Openable) ((JavaElement) root).getOpenableParent();
			if (o != null) {
				this.files.put(o, new ArrayList());
			}
		}
		checkCanceled();
	}
}
/**
 * Compute this type hierarchy.
 */
protected void compute() throws JavaScriptModelException, CoreException {
	HierarchyBuilder builder = new RegionBasedHierarchyBuilder(this);
	builder.build(this.computeSubtypes);
}
protected boolean isAffectedByOpenable(IJavaScriptElementDelta delta, IJavaScriptElement element) {
	// change to working copy
	if (element instanceof CompilationUnit && ((CompilationUnit)element).isWorkingCopy()) {
		return super.isAffectedByOpenable(delta, element);
	}

	// if no focus, hierarchy is affected if the element is part of the region
	if (this.focusType == null) {
		return this.region.contains(element);
	} else {
		return super.isAffectedByOpenable(delta, element);
	}
}
/**
 * Returns the java project this hierarchy was created in.
 */
public IJavaScriptProject javaProject() {
	return this.project;
}
public void pruneDeadBranches() {
	pruneDeadBranches(getRootClasses());
}
/*
 * Returns whether all subtypes of the given type have been pruned.
 */
private boolean pruneDeadBranches(IType type) {
	TypeVector subtypes = (TypeVector)this.typeToSubtypes.get(type);
	if (subtypes == null) return true;
	pruneDeadBranches(subtypes.copy().elements());
	subtypes = (TypeVector)this.typeToSubtypes.get(type.getDisplayName());
	return (subtypes == null || subtypes.size == 0);
}
private void pruneDeadBranches(IType[] types) {
	for (int i = 0, length = types.length; i < length; i++) {
		IType type = types[i];
		if (pruneDeadBranches(type) && !this.region.contains(type)) {
			removeType(type);
		}
	}
}
/**
 * Removes all the subtypes of the given type from the type hierarchy,
 * removes its superclass entry and removes the references from its super types.
 */
protected void removeType(IType type) {
	IType[] subtypes = this.getSubclasses(type);
	this.typeToSubtypes.remove(type.getDisplayName());
	if (subtypes != null) {
		for (int i= 0; i < subtypes.length; i++) {
			this.removeType(subtypes[i]);
		}
	}
	IType superclass = (IType)this.classToSuperclass.remove(type);
	if (superclass != null) {
		TypeVector types = (TypeVector)this.typeToSubtypes.get(superclass.getDisplayName());
		if (types != null) types.remove(type);
	}
}

}
