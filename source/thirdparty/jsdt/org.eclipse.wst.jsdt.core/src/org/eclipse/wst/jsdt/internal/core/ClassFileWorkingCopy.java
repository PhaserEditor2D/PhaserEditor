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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.jsdt.core.IBuffer;
import org.eclipse.wst.jsdt.core.IClassFile;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptModelStatusConstants;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.WorkingCopyOwner;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;

/**
 * A working copy on an <code>IClassFile</code>.
 */
public class ClassFileWorkingCopy extends CompilationUnit {

	public IClassFile classFile;

public ClassFileWorkingCopy(IClassFile classFile, WorkingCopyOwner owner) {
	super((PackageFragment) classFile.getParent(), ((BinaryType) ((ClassFile) classFile).getType()).getSourceFileName(null/*no info available*/), owner);
	this.classFile = classFile;
}

public void commitWorkingCopy(boolean force, IProgressMonitor monitor) throws JavaScriptModelException {
	throw new JavaScriptModelException(new JavaModelStatus(IJavaScriptModelStatusConstants.INVALID_ELEMENT_TYPES, this));
}

public IBuffer getBuffer() throws JavaScriptModelException {
	if (isWorkingCopy())
		return super.getBuffer();
	else
		return this.classFile.getBuffer();
}

public char[] getContents() {
	try {
		IBuffer buffer = getBuffer();
		if (buffer == null) return CharOperation.NO_CHAR;
		char[] characters = buffer.getCharacters();
		if (characters == null) return CharOperation.NO_CHAR;
		return characters;
	} catch (JavaScriptModelException e) {
		return CharOperation.NO_CHAR;
	}
}

public IPath getPath() {
	return this.classFile.getPath();
}

public IJavaScriptElement getPrimaryElement(boolean checkOwner) {
	if (checkOwner && isPrimary()) return this;
	return new ClassFileWorkingCopy(this.classFile, DefaultWorkingCopyOwner.PRIMARY);
}

public IResource getResource() {
	return this.classFile.getResource();
}



protected void toStringName(StringBuffer buffer) {
	buffer.append(this.classFile.getElementName());
}

}
