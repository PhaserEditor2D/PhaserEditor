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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.wst.jsdt.core.IBuffer;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptElementDelta;
import org.eclipse.wst.jsdt.core.IJavaScriptModelStatus;
import org.eclipse.wst.jsdt.core.IJavaScriptModelStatusConstants;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.JavaScriptConventions;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.core.util.Messages;
import org.eclipse.wst.jsdt.internal.core.util.Util;

/**
 * <p>This operation creates a compilation unit (CU).
 * If the CU doesn't exist yet, a new compilation unit will be created with the content provided.
 * Otherwise the operation will override the contents of an existing CU with the new content.
 *
 * <p>Note: It is possible to create a CU automatically when creating a
 * class or interface. Thus, the preferred method of creating a CU is
 * to perform a create type operation rather than
 * first creating a CU and secondly creating a type inside the CU.
 *
 * <p>Required Attributes:<ul>
 *  <li>The package fragment in which to create the compilation unit.
 *  <li>The name of the compilation unit.
 *      Do not include the <code>".js"</code> suffix (ex. <code>"Object"</code> -
 * 		the <code>".js"</code> will be added for the name of the compilation unit.)
 *  <li>
  * </ul>
 */
public class CreateCompilationUnitOperation extends JavaModelOperation {

	/**
	 * The name of the compilation unit being created.
	 */
	protected String fName;
	/**
	 * The source code to use when creating the element.
	 */
	protected String fSource= null;
/**
 * When executed, this operation will create a compilation unit with the given name.
 * The name should have the ".js" suffix.
 */
public CreateCompilationUnitOperation(IPackageFragment parentElement, String name, String source, boolean force) {
	super(null, new IJavaScriptElement[] {parentElement}, force);
	fName = name;
	fSource = source;
}
/**
 * Creates a compilation unit.
 *
 * @exception JavaScriptModelException if unable to create the compilation unit.
 */
protected void executeOperation() throws JavaScriptModelException {
	try {
		beginTask(Messages.operation_createUnitProgress, 2);
		JavaElementDelta delta = newJavaElementDelta();
		IJavaScriptUnit unit = getCompilationUnit();
		IPackageFragment pkg = (IPackageFragment) getParentElement();
		IContainer folder = (IContainer) pkg.getResource();
		worked(1);
		IFile compilationUnitFile = folder.getFile(new Path(fName));
		if (compilationUnitFile.exists()) {
			// update the contents of the existing unit if fForce is true
			if (force) {
				IBuffer buffer = unit.getBuffer();
				if (buffer == null) return;
				buffer.setContents(fSource);
				unit.save(new NullProgressMonitor(), false);
				resultElements = new IJavaScriptElement[] {unit};
				if (!Util.isExcluded(unit)
						&& unit.getParent().exists()) {
					for (int i = 0; i < resultElements.length; i++) {
						delta.changed(resultElements[i], IJavaScriptElementDelta.F_CONTENT);
					}
					addDelta(delta);
				}
			} else {
				throw new JavaScriptModelException(new JavaModelStatus(
					IJavaScriptModelStatusConstants.NAME_COLLISION,
					Messages.bind(Messages.status_nameCollision, compilationUnitFile.getFullPath().toString())));
			}
		} else {
			try {
				String encoding = null;
				try {
					encoding = folder.getDefaultCharset(); // get folder encoding as file is not accessible
				}
				catch (CoreException ce) {
					// use no encoding
				}
				InputStream stream = new ByteArrayInputStream(encoding == null ? fSource.getBytes() : fSource.getBytes(encoding));
				createFile(folder, unit.getElementName(), stream, force);
				resultElements = new IJavaScriptElement[] {unit};
				if (!Util.isExcluded(unit)
						&& unit.getParent().exists()) {
					for (int i = 0; i < resultElements.length; i++) {
						delta.added(resultElements[i]);
					}
					addDelta(delta);
				}
			} catch (IOException e) {
				throw new JavaScriptModelException(e, IJavaScriptModelStatusConstants.IO_EXCEPTION);
			}
		}
		worked(1);
	} finally {
		done();
	}
}
/**
 * @see CreateElementInCUOperation#getCompilationUnit()
 */
protected IJavaScriptUnit getCompilationUnit() {
	return ((IPackageFragment)getParentElement()).getJavaScriptUnit(fName);
}
protected ISchedulingRule getSchedulingRule() {
	IResource resource  = getCompilationUnit().getResource();
	IWorkspace workspace = resource.getWorkspace();
	if (resource.exists()) {
		return workspace.getRuleFactory().modifyRule(resource);
	} else {
		return workspace.getRuleFactory().createRule(resource);
	}
}
/**
 * Possible failures: <ul>
 *  <li>NO_ELEMENTS_TO_PROCESS - the package fragment supplied to the operation is
 * 		<code>null</code>.
 *	<li>INVALID_NAME - the compilation unit name provided to the operation
 * 		is <code>null</code> or has an invalid syntax
 *  <li>INVALID_CONTENTS - the source specified for the compiliation unit is null
 * </ul>
 */
public IJavaScriptModelStatus verify() {
	if (getParentElement() == null) {
		return new JavaModelStatus(IJavaScriptModelStatusConstants.NO_ELEMENTS_TO_PROCESS);
	}
	IJavaScriptProject project = getParentElement().getJavaScriptProject();
	if (JavaScriptConventions.validateCompilationUnitName(fName, project.getOption(JavaScriptCore.COMPILER_SOURCE, true), project.getOption(JavaScriptCore.COMPILER_COMPLIANCE, true)).getSeverity() == IStatus.ERROR) {
		return new JavaModelStatus(IJavaScriptModelStatusConstants.INVALID_NAME, fName);
	}
	if (fSource == null) {
		return new JavaModelStatus(IJavaScriptModelStatusConstants.INVALID_CONTENTS);
	}
	return JavaModelStatus.VERIFIED_OK;
}
}
