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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.jsdt.core.IBuffer;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.ILocalVariable;
import org.eclipse.wst.jsdt.core.IOpenable;
import org.eclipse.wst.jsdt.core.ISourceRange;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.Signature;
import org.eclipse.wst.jsdt.core.WorkingCopyOwner;
import org.eclipse.wst.jsdt.internal.core.util.MementoTokenizer;
import org.eclipse.wst.jsdt.internal.core.util.Util;


public class LocalVariable extends SourceRefElement implements ILocalVariable {

	String name;
	public int declarationSourceStart, declarationSourceEnd;
	public int nameStart, nameEnd;
	String typeSignature;

	public LocalVariable(
			JavaElement parent,
			String name,
			int declarationSourceStart,
			int declarationSourceEnd,
			int nameStart,
			int nameEnd,
			String typeSignature) {

		super(parent);
		this.name = name;
		this.declarationSourceStart = declarationSourceStart;
		this.declarationSourceEnd = declarationSourceEnd;
		this.nameStart = nameStart;
		this.nameEnd = nameEnd;
		this.typeSignature = typeSignature;
	}

	protected void closing(Object info) {
		// a local variable has no info
	}

	protected Object createElementInfo() {
		// a local variable has no info
		return null;
	}

	public boolean equals(Object o) {
		if (!(o instanceof LocalVariable)) return false;
		LocalVariable other = (LocalVariable)o;
		return
			this.declarationSourceStart == other.declarationSourceStart
			&& this.declarationSourceEnd == other.declarationSourceEnd
			&& this.nameStart == other.nameStart
			&& this.nameEnd == other.nameEnd
			&& super.equals(o);
	}

	public boolean exists() {
		return this.parent.exists(); // see https://bugs.eclipse.org/bugs/show_bug.cgi?id=46192
	}

	protected void generateInfos(Object info, HashMap newElements, IProgressMonitor pm) {
		// a local variable has no info
	}

	public IJavaScriptElement getHandleFromMemento(String token, MementoTokenizer memento, WorkingCopyOwner owner) {
		switch (token.charAt(0)) {
			case JEM_COUNT:
				return getHandleUpdatingCountFromMemento(memento, owner);
		}
		return this;
	}

	/*
	 * @see JavaElement#getHandleMemento(StringBuffer)
	 */
	protected void getHandleMemento(StringBuffer buff) {
		((JavaElement)getParent()).getHandleMemento(buff);
		buff.append(getHandleMementoDelimiter());
		buff.append(this.name);
		buff.append(JEM_COUNT);
		buff.append(this.declarationSourceStart);
		buff.append(JEM_COUNT);
		buff.append(this.declarationSourceEnd);
		buff.append(JEM_COUNT);
		buff.append(this.nameStart);
		buff.append(JEM_COUNT);
		buff.append(this.nameEnd);
		buff.append(JEM_COUNT);
		buff.append(this.typeSignature);
		if (this.occurrenceCount > 1) {
			buff.append(JEM_COUNT);
			buff.append(this.occurrenceCount);
		}
	}

	protected char getHandleMementoDelimiter() {
		return JavaElement.JEM_LOCALVARIABLE;
	}

	public IResource getCorrespondingResource() {
		return null;
	}

	public String getElementName() {
		return this.name;
	}

	public int getElementType() {
		return LOCAL_VARIABLE;
	}

	public ISourceRange getNameRange() {
		return new SourceRange(this.nameStart, this.nameEnd-this.nameStart+1);
	}

	public IPath getPath() {
		return this.parent.getPath();
	}

	public IResource getResource() {
		return this.parent.getResource();
	}

	/**
	 * @see org.eclipse.wst.jsdt.core.ISourceReference
	 */
	public String getSource() throws JavaScriptModelException {
		IOpenable openable = this.parent.getOpenableParent();
		IBuffer buffer = openable.getBuffer();
		if (buffer == null) {
			return null;
		}
		ISourceRange range = getSourceRange();
		int offset = range.getOffset();
		int length = range.getLength();
		if (offset == -1 || length == 0 ) {
			return null;
		}
		try {
			return buffer.getText(offset, length);
		} catch(RuntimeException e) {
			return null;
		}
	}

	/**
	 * @see org.eclipse.wst.jsdt.core.ISourceReference
	 */
	public ISourceRange getSourceRange() {
		return new SourceRange(this.declarationSourceStart, this.declarationSourceEnd-this.declarationSourceStart+1);
	}

	public String getTypeSignature() {
		return this.typeSignature;
	}

	public IResource getUnderlyingResource() throws JavaScriptModelException {
		return this.parent.getUnderlyingResource();
	}

	public int hashCode() {
		return Util.combineHashCodes(this.parent.hashCode(), this.nameStart);
	}

	public boolean isStructureKnown() throws JavaScriptModelException {
        return true;
    }

	protected void toStringInfo(int tab, StringBuffer buffer, Object info, boolean showResolvedInfo) {
		buffer.append(this.tabString(tab));
		if (info != NO_INFO) {
			buffer.append(Signature.toString(this.getTypeSignature()));
			buffer.append(" "); //$NON-NLS-1$
		}
		toStringName(buffer);
	}
}
