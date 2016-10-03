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

import org.eclipse.wst.jsdt.core.IJavaScriptElement;

/**
 * Holds cached structure and properties for a Java element.
 * Subclassed to carry properties for specific kinds of elements.
 */
/* package */ class JavaElementInfo {

	/**
	 * Collection of handles of immediate children of this
	 * object. This is an empty array if this element has
	 * no children.
	 */
	protected IJavaScriptElement[] children;

	/**
	 * Shared empty collection used for efficiency.
	 */
	static Object[] NO_NON_JAVA_RESOURCES = new Object[] {};

	protected JavaElementInfo() {
		this.children = JavaElement.NO_ELEMENTS;
	}
	public void addChild(IJavaScriptElement child) {
		int length = this.children.length;
		if (length == 0) {
			this.children = new IJavaScriptElement[] {child};
		} else {
			for (int i = 0; i < length; i++) {
				if (children[i].equals(child))
					return; // already included
			}
			System.arraycopy(this.children, 0, this.children = new IJavaScriptElement[length+1], 0, length);
			this.children[length] = child;
		}
	}
	public Object clone() {
		try {
			return super.clone();
		}
		catch (CloneNotSupportedException e) {
			throw new Error();
		}
	}
	public IJavaScriptElement[] getChildren() {
		return this.children;
	}
	public void removeChild(IJavaScriptElement child) {
		for (int i = 0, length = this.children.length; i < length; i++) {
			IJavaScriptElement element = this.children[i];
			if (element.equals(child)) {
				if (length == 1) {
					this.children = JavaElement.NO_ELEMENTS;
				} else {
					IJavaScriptElement[] newChildren = new IJavaScriptElement[length-1];
					System.arraycopy(this.children, 0, newChildren , 0, i);
					if (i < length-1)
						System.arraycopy(this.children, i+1, newChildren, i, length-1-i);
					this.children = newChildren;
				}
				break;
			}
		}
	}
	public void setChildren(IJavaScriptElement[] children) {
		this.children = children;
	}
}
