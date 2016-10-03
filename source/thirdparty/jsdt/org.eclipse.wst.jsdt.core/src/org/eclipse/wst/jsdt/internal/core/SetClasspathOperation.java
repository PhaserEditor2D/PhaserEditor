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
package org.eclipse.wst.jsdt.internal.core;

import org.eclipse.core.runtime.IPath;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptModelStatus;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;

/**
 * This operation sets an <code>IJavaScriptProject</code>'s classpath.
 *
 * @see org.eclipse.wst.jsdt.core.IJavaScriptProject
 */
public class SetClasspathOperation extends ChangeClasspathOperation {

	IIncludePathEntry[] newRawClasspath;
	IPath newOutputLocation;
	JavaProject project;

	/**
	 * When executed, this operation sets the raw classpath and output location of the given project.
	 */
	public SetClasspathOperation(
		JavaProject project,
		IIncludePathEntry[] newRawClasspath,
		IPath newOutputLocation,
		boolean canChangeResource) {

		super(new IJavaScriptElement[] { project }, canChangeResource);
		this.project = project;
		this.newRawClasspath = newRawClasspath;
		this.newOutputLocation = newOutputLocation;
	}

	/**
	 * Sets the classpath of the pre-specified project.
	 */
	protected void executeOperation() throws JavaScriptModelException {
		checkCanceled();
		try {
			// set raw classpath and null out resolved info
			this.project.getPerProjectInfo().setClasspath(this.newRawClasspath, this.newOutputLocation, JavaModelStatus.VERIFIED_OK/*format is ok*/, null, null, null, null);

			// if needed, generate delta, update project ref, create markers, ...
			classpathChanged(this.project);

			// write .classpath file
			if (this.canChangeResources && this.project.saveClasspath(this.newRawClasspath, this.newOutputLocation))
				setAttribute(HAS_MODIFIED_RESOURCE_ATTR, TRUE);
		} finally {
			done();
		}
	}

	public String toString(){
		StringBuffer buffer = new StringBuffer(20);
		buffer.append("SetClasspathOperation\n"); //$NON-NLS-1$
		buffer.append(" - classpath : "); //$NON-NLS-1$
		buffer.append("{"); //$NON-NLS-1$
		for (int i = 0; i < this.newRawClasspath.length; i++) {
			if (i > 0) buffer.append(","); //$NON-NLS-1$
			IIncludePathEntry element = this.newRawClasspath[i];
			buffer.append(" ").append(element.toString()); //$NON-NLS-1$
		}
		buffer.append("\n - output location : ");  //$NON-NLS-1$
		buffer.append(this.newOutputLocation.toString());
		return buffer.toString();
	}

	public IJavaScriptModelStatus verify() {
		IJavaScriptModelStatus status = super.verify();
		if (!status.isOK())
			return status;
		return ClasspathEntry.validateClasspath(	this.project, this.newRawClasspath);
	}

}
