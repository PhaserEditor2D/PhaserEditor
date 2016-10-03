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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.jsdt.core.Flags;
import org.eclipse.wst.jsdt.core.IInitializer;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptModelStatusConstants;
import org.eclipse.wst.jsdt.core.ISourceRange;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.core.util.Util;

/**
 * @see IInitializer
 */

/* package */ class Initializer extends Member implements IInitializer {

protected Initializer(JavaElement parent, int count) {
	super(parent);
	// 0 is not valid: this first occurrence is occurrence 1.
	if (count <= 0)
		throw new IllegalArgumentException();
	this.occurrenceCount = count;
}
public boolean equals(Object o) {
	if (!(o instanceof Initializer)) return false;
	return super.equals(o);
}
/**
 * @see IJavaScriptElement
 */
public int getElementType() {
	return INITIALIZER;
}
/**
 * @see JavaElement#getHandleMemento(StringBuffer)
 */
protected void getHandleMemento(StringBuffer buff) {
	((JavaElement)getParent()).getHandleMemento(buff);
	buff.append(getHandleMementoDelimiter());
	buff.append(this.occurrenceCount);
}
/**
 * @see JavaElement#getHandleMemento()
 */
protected char getHandleMementoDelimiter() {
	return JavaElement.JEM_INITIALIZER;
}
public int hashCode() {
	return Util.combineHashCodes(this.parent.hashCode(), this.occurrenceCount);
}
/**
 */
public String readableName() {

	return ((JavaElement)getDeclaringType()).readableName();
}
/**
 * @see org.eclipse.wst.jsdt.core.ISourceManipulation
 */
public void rename(String newName, boolean force, IProgressMonitor monitor) throws JavaScriptModelException {
	throw new JavaScriptModelException(new JavaModelStatus(IJavaScriptModelStatusConstants.INVALID_ELEMENT_TYPES, this));
}
/**
 * @see org.eclipse.wst.jsdt.core.IMember
 */
public ISourceRange getNameRange() {
	return null;
}
/*
 * @see JavaElement#getPrimaryElement(boolean)
 */
public IJavaScriptElement getPrimaryElement(boolean checkOwner) {
	if (checkOwner) {
		CompilationUnit cu = (CompilationUnit)getAncestor(JAVASCRIPT_UNIT);
		if (cu == null || cu.isPrimary()) return this;
	}
	IJavaScriptElement primaryParent = this.parent.getPrimaryElement(false);
	return ((IType)primaryParent).getInitializer(this.occurrenceCount);
}
/**
 * @private Debugging purposes
 */
protected void toStringInfo(int tab, StringBuffer buffer, Object info, boolean showResolvedInfo) {
	buffer.append(this.tabString(tab));
	if (info == null) {
		buffer.append("<initializer #"); //$NON-NLS-1$
		buffer.append(this.occurrenceCount);
		buffer.append("> (not open)"); //$NON-NLS-1$
	} else if (info == NO_INFO) {
		buffer.append("<initializer #"); //$NON-NLS-1$
		buffer.append(this.occurrenceCount);
		buffer.append(">"); //$NON-NLS-1$
	} else {
		try {
			buffer.append("<"); //$NON-NLS-1$
			if (Flags.isStatic(this.getFlags())) {
				buffer.append("static "); //$NON-NLS-1$
			}
		buffer.append("initializer #"); //$NON-NLS-1$
		buffer.append(this.occurrenceCount);
		buffer.append(">"); //$NON-NLS-1$
		} catch (JavaScriptModelException e) {
			buffer.append("<JavaScriptModelException in toString of " + getElementName()); //$NON-NLS-1$
		}
	}
}
}
