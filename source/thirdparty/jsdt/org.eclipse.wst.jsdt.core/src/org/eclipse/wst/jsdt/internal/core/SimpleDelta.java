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

import org.eclipse.wst.jsdt.core.IJavaScriptElementDelta;

/**
 * A simple Java element delta that remembers the kind of changes only.
 */
public class SimpleDelta {

	/*
	 * @see IJavaScriptElementDelta#getKind()
	 */
	protected int kind = 0;

	/*
	 * @see IJavaScriptElementDelta#getFlags()
	 */
	protected int changeFlags = 0;

	/*
	 * Marks this delta as added
	 */
	public void added() {
		this.kind = IJavaScriptElementDelta.ADDED;
	}

	/*
	 * Marks this delta as changed with the given change flag
	 */
	public void changed(int flags) {
		this.kind = IJavaScriptElementDelta.CHANGED;
		this.changeFlags |= flags;
	}

	/*
	 * @see IJavaScriptElementDelta#getFlags()
	 */
	public int getFlags() {
		return this.changeFlags;
	}

	/*
	 * @see IJavaScriptElementDelta#getKind()
	 */
	public int getKind() {
		return this.kind;
	}

	/*
	 * Mark this delta has a having a modifiers change
	 */
	public void modifiers() {
		changed(IJavaScriptElementDelta.F_MODIFIERS);
	}

	/*
	 * Marks this delta as removed
	 */
	public void removed() {
		this.kind = IJavaScriptElementDelta.REMOVED;
		this.changeFlags = 0;
	}

	/*
	 * Mark this delta has a having a super type change
	 */
	public void superTypes() {
		changed(IJavaScriptElementDelta.F_SUPER_TYPES);
	}

	protected void toDebugString(StringBuffer buffer) {
		buffer.append("["); //$NON-NLS-1$
		switch (getKind()) {
			case IJavaScriptElementDelta.ADDED :
				buffer.append('+');
				break;
			case IJavaScriptElementDelta.REMOVED :
				buffer.append('-');
				break;
			case IJavaScriptElementDelta.CHANGED :
				buffer.append('*');
				break;
			default :
				buffer.append('?');
				break;
		}
		buffer.append("]: {"); //$NON-NLS-1$
		toDebugString(buffer, getFlags());
		buffer.append("}"); //$NON-NLS-1$
	}

	protected boolean toDebugString(StringBuffer buffer, int flags) {
		boolean prev = false;
		if ((flags & IJavaScriptElementDelta.F_MODIFIERS) != 0) {
			if (prev)
				buffer.append(" | "); //$NON-NLS-1$
			buffer.append("MODIFIERS CHANGED"); //$NON-NLS-1$
			prev = true;
		}
		if ((flags & IJavaScriptElementDelta.F_SUPER_TYPES) != 0) {
			if (prev)
				buffer.append(" | "); //$NON-NLS-1$
			buffer.append("SUPER TYPES CHANGED"); //$NON-NLS-1$
			prev = true;
		}
		return prev;
	}

	public String toString() {
		StringBuffer buffer = new StringBuffer();
		toDebugString(buffer);
		return buffer.toString();
	}
}
