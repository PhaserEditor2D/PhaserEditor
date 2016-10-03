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

import java.util.HashMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptModelStatusConstants;
import org.eclipse.wst.jsdt.core.ISourceRange;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;

/**
 * Common functionality for Binary member handles.
 */
public abstract class BinaryMember extends NamedMember {
/*
 * Constructs a binary member.
 */
protected BinaryMember(JavaElement parent, String name) {
	super(parent, name);
}
/*
 * @see ISourceManipulation
 */
public void copy(IJavaScriptElement container, IJavaScriptElement sibling, String rename, boolean force, IProgressMonitor monitor) throws JavaScriptModelException {
	throw new JavaScriptModelException(new JavaModelStatus(IJavaScriptModelStatusConstants.READ_ONLY, this));
}
/*
 * @see JavaElement#generateInfos
 */
protected void generateInfos(Object info, HashMap newElements, IProgressMonitor pm) throws JavaScriptModelException {
	Openable openableParent = (Openable) getOpenableParent();
	if (JavaModelManager.getJavaModelManager().getInfo(openableParent) == null) {
		openableParent.generateInfos(openableParent.createElementInfo(), newElements, pm);
	}
}
public String[] getCategories() throws JavaScriptModelException {
	SourceMapper mapper= getSourceMapper();
	if (mapper != null) {
		// ensure the class file's buffer is open so that categories are computed
		((ClassFile)getClassFile()).getBuffer();

		if (mapper.categories != null) {
			String[] categories = (String[]) mapper.categories.get(this);
			if (categories != null)
				return categories;
		}
	}
	return CharOperation.NO_STRINGS;
}
public String getKey() {
	try {
		return getKey(false/*don't open*/);
	} catch (JavaScriptModelException e) {
		// happen only if force open is true
		return null;
	}
}
/**
 * @see org.eclipse.wst.jsdt.internal.compiler.lookup.Binding#computeUniqueKey()
 */
public abstract String getKey(boolean forceOpen) throws JavaScriptModelException;
/*
 * @see ISourceReference
 */
public ISourceRange getNameRange() throws JavaScriptModelException {
	SourceMapper mapper= getSourceMapper();
	if (mapper != null) {
		// ensure the class file's buffer is open so that source ranges are computed
		((ClassFile)getClassFile()).getBuffer();

		return mapper.getNameRange(this);
	} else {
		return SourceMapper.UNKNOWN_RANGE;
	}
}
/*
 * @see ISourceReference
 */
//public ISourceRange getSourceRange() throws JavaScriptModelException {
//	SourceMapper mapper= getSourceMapper();
//	if (mapper != null) {
//		// ensure the class file's buffer is open so that source ranges are computed
//		((ClassFile)getClassFile()).getBuffer();
//
//		return mapper.getSourceRange(this);
//	} else {
//		return SourceMapper.UNKNOWN_RANGE;
//	}
//}
/*
 * @see IMember
 */
public boolean isBinary() {
	return true;
}
/*
 * @see IJavaScriptElement
 */
public boolean isStructureKnown() throws JavaScriptModelException {
	return ((IJavaScriptElement)getOpenableParent()).isStructureKnown();
}
/*
 * @see ISourceManipulation
 */
public void move(IJavaScriptElement container, IJavaScriptElement sibling, String rename, boolean force, IProgressMonitor monitor) throws JavaScriptModelException {
	throw new JavaScriptModelException(new JavaModelStatus(IJavaScriptModelStatusConstants.READ_ONLY, this));
}
/*
 * @see ISourceManipulation
 */
public void rename(String newName, boolean force, IProgressMonitor monitor) throws JavaScriptModelException {
	throw new JavaScriptModelException(new JavaModelStatus(IJavaScriptModelStatusConstants.READ_ONLY, this));
}
/*
 * Sets the contents of this element.
 * Throws an exception as this element is read only.
 */
public void setContents(String contents, IProgressMonitor monitor) throws JavaScriptModelException {
	throw new JavaScriptModelException(new JavaModelStatus(IJavaScriptModelStatusConstants.READ_ONLY, this));
}
}
