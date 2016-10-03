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
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptModelStatusConstants;
import org.eclipse.wst.jsdt.core.IOpenable;
import org.eclipse.wst.jsdt.core.ISourceRange;
import org.eclipse.wst.jsdt.core.ISourceReference;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.WorkingCopyOwner;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.internal.core.util.DOMFinder;
import org.eclipse.wst.jsdt.internal.core.util.MementoTokenizer;
import org.eclipse.wst.jsdt.internal.core.util.Messages;

/**
 * Abstract class for Java elements which implement ISourceReference.
 */
public abstract class SourceRefElement extends JavaElement implements ISourceReference {
	/*
	 * A count to uniquely identify this element in the case
	 * that a duplicate named element exists. For example, if
	 * there are two fields in a compilation unit with the
	 * same name, the occurrence count is used to distinguish
	 * them.  The occurrence count starts at 1 (thus the first
	 * occurrence is occurrence 1, not occurrence 0).
	 */
	public int occurrenceCount = 1;

protected SourceRefElement(JavaElement parent) {
	super(parent);
}
/**
 * This element is being closed.  Do any necessary cleanup.
 */
protected void closing(Object info) throws JavaScriptModelException {
	// Do any necessary cleanup
}
/**
 * Returns a new element info for this element.
 */
protected Object createElementInfo() {
	return null; // not used for source ref elements
}
/**
 * @see org.eclipse.wst.jsdt.core.ISourceManipulation
 */
public void copy(IJavaScriptElement container, IJavaScriptElement sibling, String rename, boolean force, IProgressMonitor monitor) throws JavaScriptModelException {
	if (container == null) {
		throw new IllegalArgumentException(Messages.operation_nullContainer);
	}
	IJavaScriptElement[] elements= new IJavaScriptElement[] {this};
	IJavaScriptElement[] containers= new IJavaScriptElement[] {container};
	IJavaScriptElement[] siblings= null;
	if (sibling != null) {
		siblings= new IJavaScriptElement[] {sibling};
	}
	String[] renamings= null;
	if (rename != null) {
		renamings= new String[] {rename};
	}
	getJavaScriptModel().copy(elements, containers, siblings, renamings, force, monitor);
}
/**
 * @see org.eclipse.wst.jsdt.core.ISourceManipulation
 */
public void delete(boolean force, IProgressMonitor monitor) throws JavaScriptModelException {
	IJavaScriptElement[] elements = new IJavaScriptElement[] {this};
	getJavaScriptModel().delete(elements, force, monitor);
}
public boolean equals(Object o) {
	if (!(o instanceof SourceRefElement)) return false;
	return this.occurrenceCount == ((SourceRefElement)o).occurrenceCount &&
			super.equals(o);
}
/**
 * Returns the <code>ASTNode</code> that corresponds to this <code>JavaElement</code>
 * or <code>null</code> if there is no corresponding node.
 */
public ASTNode findNode(JavaScriptUnit ast) {
	DOMFinder finder = new DOMFinder(ast, this, false);
	try {
		return finder.search();
	} catch (JavaScriptModelException e) {
		// receiver doesn't exist
		return null;
	}
}
/*
 * @see JavaElement#generateInfos
 */
protected void generateInfos(Object info, HashMap newElements, IProgressMonitor pm) throws JavaScriptModelException {
	Openable openableParent = (Openable)getOpenableParent();
	if (openableParent == null) return;

	JavaElementInfo openableParentInfo = (JavaElementInfo) JavaModelManager.getJavaModelManager().getInfo(openableParent);
	if (openableParentInfo == null) {
		openableParent.generateInfos(openableParent.createElementInfo(), newElements, pm);
	}
}
/**
 * @see org.eclipse.wst.jsdt.core.IMember
 * @deprecated Use {@link #getJavaScriptUnit()} instead
 */
public IJavaScriptUnit getCompilationUnit() {
	return getJavaScriptUnit();
}
/**
 * @see org.eclipse.wst.jsdt.core.IMember
 */
public IJavaScriptUnit getJavaScriptUnit() {
	return (IJavaScriptUnit) getAncestor(JAVASCRIPT_UNIT);
}
/**
 * Elements within compilation units and class files have no
 * corresponding resource.
 *
 * @see IJavaScriptElement
 */
public IResource getCorrespondingResource() throws JavaScriptModelException {
	if (!exists()) throw newNotPresentException();
	return null;
}
/*
 * @see JavaElement
 */
public IJavaScriptElement getHandleFromMemento(String token, MementoTokenizer memento, WorkingCopyOwner workingCopyOwner) {
	switch (token.charAt(0)) {
		case JEM_COUNT:
			return getHandleUpdatingCountFromMemento(memento, workingCopyOwner);
	}
	return this;
}
protected void getHandleMemento(StringBuffer buff) {
	super.getHandleMemento(buff);
	if (this.occurrenceCount > 1) {
		buff.append(JEM_COUNT);
		buff.append(this.occurrenceCount);
	}
}
/*
 * Update the occurence count of the receiver and creates a Java element handle from the given memento.
 * The given working copy owner is used only for compilation unit handles.
 */
public IJavaScriptElement getHandleUpdatingCountFromMemento(MementoTokenizer memento, WorkingCopyOwner owner) {
	if (!memento.hasMoreTokens()) return this;
	this.occurrenceCount = Integer.parseInt(memento.nextToken());
	if (!memento.hasMoreTokens()) return this;
	String token = memento.nextToken();
	return getHandleFromMemento(token, memento, owner);
}
/*
 * @see org.eclipse.wst.jsdt.core.IMember#getOccurrenceCount()
 */
public int getOccurrenceCount() {
	return this.occurrenceCount;
}
/**
 * Return the first instance of IOpenable in the hierarchy of this
 * type (going up the hierarchy from this type);
 */
public IOpenable getOpenableParent() {
	IJavaScriptElement current = getParent();
	while (current != null){
		if (current instanceof IOpenable){
			return (IOpenable) current;
		}
		current = current.getParent();
	}
	return null;
}
/*
 * @see IJavaScriptElement
 */
public IPath getPath() {
	return this.getParent().getPath();
}
/*
 * @see IJavaScriptElement
 */
public IResource getResource() {
	return this.getParent().getResource();
}
/**
 * @see ISourceReference
 */
public String getSource() throws JavaScriptModelException {
	IOpenable openable = getOpenableParent();
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
 * @see ISourceReference
 */
public ISourceRange getSourceRange() throws JavaScriptModelException {
	SourceRefElementInfo info = (SourceRefElementInfo) getElementInfo();
	return info.getSourceRange();
}
/**
 * @see IJavaScriptElement
 */
public IResource getUnderlyingResource() throws JavaScriptModelException {
	if (!exists()) throw newNotPresentException();
	return getParent().getUnderlyingResource();
}
/**
 * @see org.eclipse.wst.jsdt.core.IParent
 */
public boolean hasChildren() throws JavaScriptModelException {
	return getChildren().length > 0;
}
/**
 * @see IJavaScriptElement
 */
public boolean isStructureKnown() throws JavaScriptModelException {
	// structure is always known inside an openable
	return true;
}
/**
 * @see org.eclipse.wst.jsdt.core.ISourceManipulation
 */
public void move(IJavaScriptElement container, IJavaScriptElement sibling, String rename, boolean force, IProgressMonitor monitor) throws JavaScriptModelException {
	if (container == null) {
		throw new IllegalArgumentException(Messages.operation_nullContainer);
	}
	if (getClassFile()!=null)
		throw new JavaScriptModelException(new JavaModelStatus(IJavaScriptModelStatusConstants.READ_ONLY, this));

	IJavaScriptElement[] elements= new IJavaScriptElement[] {this};
	IJavaScriptElement[] containers= new IJavaScriptElement[] {container};
	IJavaScriptElement[] siblings= null;
	if (sibling != null) {
		siblings= new IJavaScriptElement[] {sibling};
	}
	String[] renamings= null;
	if (rename != null) {
		renamings= new String[] {rename};
	}
	getJavaScriptModel().move(elements, containers, siblings, renamings, force, monitor);
}
/**
 * @see org.eclipse.wst.jsdt.core.ISourceManipulation
 */
public void rename(String newName, boolean force, IProgressMonitor monitor) throws JavaScriptModelException {
	if (newName == null) {
		throw new IllegalArgumentException(Messages.element_nullName);
	}
	if (getClassFile()!=null)
		throw new JavaScriptModelException(new JavaModelStatus(IJavaScriptModelStatusConstants.READ_ONLY, this));

	IJavaScriptElement[] elements= new IJavaScriptElement[] {this};
	IJavaScriptElement[] dests= new IJavaScriptElement[] {this.getParent()};
	String[] renamings= new String[] {newName};
	getJavaScriptModel().rename(elements, dests, renamings, force, monitor);
}
protected void toStringName(StringBuffer buffer) {
	super.toStringName(buffer);
	if (this.occurrenceCount > 1) {
		buffer.append("#"); //$NON-NLS-1$
		buffer.append(this.occurrenceCount);
	}
}
}
