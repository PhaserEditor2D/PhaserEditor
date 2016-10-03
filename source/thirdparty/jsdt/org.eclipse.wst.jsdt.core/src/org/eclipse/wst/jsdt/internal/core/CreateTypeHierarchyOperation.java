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
package org.eclipse.wst.jsdt.internal.core;

import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptModelStatus;
import org.eclipse.wst.jsdt.core.IJavaScriptModelStatusConstants;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IRegion;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.ITypeHierarchy;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchScope;
import org.eclipse.wst.jsdt.internal.core.hierarchy.RegionBasedTypeHierarchy;
import org.eclipse.wst.jsdt.internal.core.hierarchy.TypeHierarchy;

/**
 * This operation creates an <code>ITypeHierarchy</code> for a specific type within
 * a specified region, or for all types within a region. The specified
 * region limits the number of resolved subtypes (to the subset of
 * types in the specified region). The resolved supertypes may go outside
 * of the specified region in order to reach the root(s) of the type
 * hierarchy. A Java Project is required to provide a context (classpath)
 * to use while resolving supertypes and subtypes.
 *
 * @see ITypeHierarchy
 */

public class CreateTypeHierarchyOperation extends JavaModelOperation {
	/**
	 * The generated type hierarchy
	 */
	protected TypeHierarchy typeHierarchy;

/**
 * Constructs an operation to create a type hierarchy for the
 * given type within the specified region, in the context of
 * the given project.
 */
public CreateTypeHierarchyOperation(IRegion region, IJavaScriptUnit[] workingCopies, IType element, boolean computeSubtypes) {
	super(element);
	this.typeHierarchy = new RegionBasedTypeHierarchy(region, workingCopies, element, computeSubtypes);
}
/**
 * Constructs an operation to create a type hierarchy for the
 * given type and working copies.
 */
public CreateTypeHierarchyOperation(IType element, IJavaScriptUnit[] workingCopies, IJavaScriptSearchScope scope, boolean computeSubtypes) {
	super(element);
	IJavaScriptUnit[] copies;
	if (workingCopies != null) {
		int length = workingCopies.length;
		copies = new IJavaScriptUnit[length];
		System.arraycopy(workingCopies, 0, copies, 0, length);
	} else {
		copies = null;
	}
	this.typeHierarchy = new TypeHierarchy(element, copies, scope, computeSubtypes);
}
/**
 * Constructs an operation to create a type hierarchy for the
 * given type and working copies.
 */
public CreateTypeHierarchyOperation(IType element, IJavaScriptUnit[] workingCopies, IJavaScriptProject project, boolean computeSubtypes) {
	super(element);
	IJavaScriptUnit[] copies;
	if (workingCopies != null) {
		int length = workingCopies.length;
		copies = new IJavaScriptUnit[length];
		System.arraycopy(workingCopies, 0, copies, 0, length);
	} else {
		copies = null;
	}
	this.typeHierarchy = new TypeHierarchy(element, copies, project, computeSubtypes);
}
/**
 * Performs the operation - creates the type hierarchy
 * @exception JavaScriptModelException The operation has failed.
 */
protected void executeOperation() throws JavaScriptModelException {
	this.typeHierarchy.refresh(this);
}
/**
 * Returns the generated type hierarchy.
 */
public ITypeHierarchy getResult() {
	return this.typeHierarchy;
}
/**
 * @see JavaModelOperation
 */
public boolean isReadOnly() {
	return true;
}
/**
 * Possible failures: <ul>
 *	<li>NO_ELEMENTS_TO_PROCESS - at least one of a type or region must
 *			be provided to generate a type hierarchy.
 *	<li>ELEMENT_NOT_PRESENT - the provided type or type's project does not exist
 * </ul>
 */
public IJavaScriptModelStatus verify() {
	IJavaScriptElement elementToProcess= getElementToProcess();
	if (elementToProcess == null && !(this.typeHierarchy instanceof RegionBasedTypeHierarchy)) {
		return new JavaModelStatus(IJavaScriptModelStatusConstants.NO_ELEMENTS_TO_PROCESS);
	}
	if (elementToProcess != null && !elementToProcess.exists()) {
		return new JavaModelStatus(IJavaScriptModelStatusConstants.ELEMENT_DOES_NOT_EXIST, elementToProcess);
	}
	IJavaScriptProject project = this.typeHierarchy.javaProject();
	if (project != null && !project.exists()) {
		return new JavaModelStatus(IJavaScriptModelStatusConstants.ELEMENT_DOES_NOT_EXIST, project);
	}
	return JavaModelStatus.VERIFIED_OK;
}
}
