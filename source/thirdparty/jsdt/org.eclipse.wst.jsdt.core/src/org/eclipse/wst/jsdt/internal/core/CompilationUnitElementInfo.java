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

import java.util.HashMap;

import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.ISourceRange;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;

public class CompilationUnitElementInfo extends OpenableElementInfo {

	/**
	 * The length of this compilation unit's source code <code>String</code>
	 */
	protected int sourceLength;

	/**
	 * Timestamp of original resource at the time this element
	 * was opened or last updated.
	 */
	protected long timestamp;

	/*
	 * A map from an IJavaScriptElement (this type or a child of this type) to a String[] (the categories of this element)
	 */
	protected HashMap categories;

protected void addCategories(IJavaScriptElement element, char[][] elementCategories) {
	if (elementCategories == null) return;
	if (this.categories == null)
		this.categories = new HashMap();
	this.categories.put(element, CharOperation.toStrings(elementCategories));
}

/*
 * Return a map from an IJavaScriptElement (this type or a child of this type) to a String[] (the categories of this element)
 */
public HashMap getCategories() {
	return this.categories;
}

/**
 * Returns the length of the source string.
 */
public int getSourceLength() {
	return this.sourceLength;
}
protected ISourceRange getSourceRange() {
	return new SourceRange(0, this.sourceLength);
}
/**
 * Sets the length of the source string.
 */
public void setSourceLength(int newSourceLength) {
	this.sourceLength = newSourceLength;
}
}
