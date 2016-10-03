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

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.PerformanceStats;
import org.eclipse.wst.jsdt.core.BufferChangedEvent;
import org.eclipse.wst.jsdt.core.CompletionRequestor;
import org.eclipse.wst.jsdt.core.IBuffer;
import org.eclipse.wst.jsdt.core.IBufferChangedListener;
import org.eclipse.wst.jsdt.core.IBufferFactory;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptModelStatusConstants;
import org.eclipse.wst.jsdt.core.IOpenable;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.WorkingCopyOwner;
import org.eclipse.wst.jsdt.internal.codeassist.CompletionEngine;
import org.eclipse.wst.jsdt.internal.codeassist.SelectionEngine;
import org.eclipse.wst.jsdt.internal.core.util.Util;


/**
 * Abstract class for implementations of java elements which are IOpenable.
 *
 * @see IJavaScriptElement
 * @see IOpenable
 */
public abstract class Openable extends JavaElement implements IOpenable, IBufferChangedListener {

protected Openable(JavaElement parent) {
	super(parent);
}
/**
 * The buffer associated with this element has changed. Registers
 * this element as being out of synch with its buffer's contents.
 * If the buffer has been closed, this element is set as NOT out of
 * synch with the contents.
 *
 * @see IBufferChangedListener
 */
public void bufferChanged(BufferChangedEvent event) {
	if (event.getBuffer().isClosed()) {
		JavaModelManager.getJavaModelManager().getElementsOutOfSynchWithBuffers().remove(this);
		getBufferManager().removeBuffer(event.getBuffer());
	} else {
		JavaModelManager.getJavaModelManager().getElementsOutOfSynchWithBuffers().add(this);
	}
}
/**
 * Builds this element's structure and properties in the given
 * info object, based on this element's current contents (reuse buffer
 * contents if this element has an open buffer, or resource contents
 * if this element does not have an open buffer). Children
 * are placed in the given newElements table (note, this element
 * has already been placed in the newElements table). Returns true
 * if successful, or false if an error is encountered while determining
 * the structure of this element.
 */
protected abstract boolean buildStructure(OpenableElementInfo info, IProgressMonitor pm, Map newElements, IResource underlyingResource) throws JavaScriptModelException;
/*
 * Returns whether this element can be removed from the Java model cache to make space.
 */
public boolean canBeRemovedFromCache() {
	try {
		return !hasUnsavedChanges();
	} catch (JavaScriptModelException e) {
		return false;
	}
}
/*
 * Returns whether the buffer of this element can be removed from the Java model cache to make space.
 */
public boolean canBufferBeRemovedFromCache(IBuffer buffer) {
	return !buffer.hasUnsavedChanges();
}
/**
 * Close the buffer associated with this element, if any.
 */
protected void closeBuffer() {
	if (!hasBuffer()) return; // nothing to do
	IBuffer buffer = getBufferManager().getBuffer(this);
	if (buffer != null) {
		buffer.close();
		buffer.removeBufferChangedListener(this);
	}
}
/**
 * This element is being closed.  Do any necessary cleanup.
 */
protected void closing(Object info) {
	closeBuffer();
}
protected void codeComplete(org.eclipse.wst.jsdt.internal.compiler.env.ICompilationUnit cu, org.eclipse.wst.jsdt.internal.compiler.env.ICompilationUnit unitToSkip, int position, CompletionRequestor requestor, WorkingCopyOwner owner) throws JavaScriptModelException {
	if (requestor == null) {
		throw new IllegalArgumentException("Completion requestor cannot be null"); //$NON-NLS-1$
	}
	PerformanceStats performanceStats = CompletionEngine.PERF
		? PerformanceStats.getStats(JavaModelManager.COMPLETION_PERF, this)
		: null;
	if(performanceStats != null) {
		performanceStats.startRun(new String(cu.getFileName()) + " at " + position); //$NON-NLS-1$
	}
	IBuffer buffer = getBuffer();
	if (buffer == null) {
		return;
	}
	if (position < -1 || position > buffer.getLength()) {
		throw new JavaScriptModelException(new JavaModelStatus(IJavaScriptModelStatusConstants.INDEX_OUT_OF_BOUNDS));
	}
	JavaProject project = (JavaProject) getJavaScriptProject();
	SearchableEnvironment environment = newSearchableNameEnvironment(owner);

	// set unit to skip
	environment.unitToSkip = unitToSkip;

	// code complete
	CompletionEngine engine = new CompletionEngine(environment, requestor, project.getOptions(true), project);
	engine.complete(cu, position, 0);
	if(performanceStats != null) {
		performanceStats.endRun();
	}
	if (NameLookup.VERBOSE) {
		System.out.println(Thread.currentThread() + " TIME SPENT in NameLoopkup#seekTypesInSourcePackage: " + environment.nameLookup.timeSpentInSeekTypesInSourcePackage + "ms");  //$NON-NLS-1$ //$NON-NLS-2$
		System.out.println(Thread.currentThread() + " TIME SPENT in NameLoopkup#seekTypesInBinaryPackage: " + environment.nameLookup.timeSpentInSeekTypesInBinaryPackage + "ms");  //$NON-NLS-1$ //$NON-NLS-2$
	}
}
protected IJavaScriptElement[] codeSelect(org.eclipse.wst.jsdt.internal.compiler.env.ICompilationUnit cu, int offset, int length, WorkingCopyOwner owner) throws JavaScriptModelException {
	PerformanceStats performanceStats = SelectionEngine.PERF
		? PerformanceStats.getStats(JavaModelManager.SELECTION_PERF, this)
		: null;
	if(performanceStats != null) {
		performanceStats.startRun(new String(cu.getFileName()) + " at [" + offset + "," + length + "]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	JavaProject project = (JavaProject)getJavaScriptProject();
	SearchableEnvironment environment = newSearchableNameEnvironment(owner);

	SelectionRequestor requestor= new SelectionRequestor(environment.nameLookup, this);
	IBuffer buffer = getBuffer();
	if (buffer == null) {
		return requestor.getElements();
	}
	int end= buffer.getLength();
	if (offset < 0 || length < 0 || offset + length > end ) {
		throw new JavaScriptModelException(new JavaModelStatus(IJavaScriptModelStatusConstants.INDEX_OUT_OF_BOUNDS));
	}

	// fix for 1FVXGDK
	SelectionEngine engine = new SelectionEngine(environment, requestor, project.getOptions(true));
	engine.select(cu, offset, offset + length - 1);

	if(performanceStats != null) {
		performanceStats.endRun();
	}
	if (NameLookup.VERBOSE) {
		System.out.println(Thread.currentThread() + " TIME SPENT in NameLoopkup#seekTypesInSourcePackage: " + environment.nameLookup.timeSpentInSeekTypesInSourcePackage + "ms");  //$NON-NLS-1$ //$NON-NLS-2$
		System.out.println(Thread.currentThread() + " TIME SPENT in NameLoopkup#seekTypesInBinaryPackage: " + environment.nameLookup.timeSpentInSeekTypesInBinaryPackage + "ms");  //$NON-NLS-1$ //$NON-NLS-2$
	}
	return requestor.getElements();
}
/*
 * Returns a new element info for this element.
 */
protected Object createElementInfo() {
	return new OpenableElementInfo();
}
/**
 * @see IJavaScriptElement
 */
public boolean exists() {
	JavaModelManager manager = JavaModelManager.getJavaModelManager();
	if (manager.getInfo(this) != null) return true;
	if (!parentExists()) return false;
	PackageFragmentRoot root = getPackageFragmentRoot();
	if (root != null
			&& (root == this || !root.isArchive())) {
		return resourceExists();
	}
	return super.exists();
}
public String findRecommendedLineSeparator() throws JavaScriptModelException {
	IBuffer buffer = getBuffer();
	String source = buffer == null ? null : buffer.getContents();
	return Util.getLineSeparator(source, getJavaScriptProject());
}
protected void generateInfos(Object info, HashMap newElements, IProgressMonitor monitor) throws JavaScriptModelException {

	if (JavaModelCache.VERBOSE){
		String element;
		switch (getElementType()) {
			case JAVASCRIPT_PROJECT:
				element = "project"; //$NON-NLS-1$
				break;
			case PACKAGE_FRAGMENT_ROOT:
				element = "root"; //$NON-NLS-1$
				break;
			case PACKAGE_FRAGMENT:
				element = "package"; //$NON-NLS-1$
				break;
			case CLASS_FILE:
				element = "class file"; //$NON-NLS-1$
				break;
			case JAVASCRIPT_UNIT:
				element = "compilation unit"; //$NON-NLS-1$
				break;
			default:
				element = "element"; //$NON-NLS-1$
		}
		System.out.println(Thread.currentThread() +" OPENING " + element + " " + this.toStringWithAncestors()); //$NON-NLS-1$//$NON-NLS-2$
	}

	// open the parent if necessary
	openParent(info, newElements, monitor);
	if (monitor != null && monitor.isCanceled())
		throw new OperationCanceledException();

	 // puts the info before building the structure so that questions to the handle behave as if the element existed
	 // (case of compilation units becoming working copies)
	newElements.put(this, info);

	// build the structure of the openable (this will open the buffer if needed)
	try {
		OpenableElementInfo openableElementInfo = (OpenableElementInfo)info;
		boolean isStructureKnown = buildStructure(openableElementInfo, monitor, newElements, getResource());
		openableElementInfo.setIsStructureKnown(isStructureKnown);
	} catch (JavaScriptModelException e) {
		newElements.remove(this);
		throw e;
	}

	// remove out of sync buffer for this element
	JavaModelManager.getJavaModelManager().getElementsOutOfSynchWithBuffers().remove(this);

	if (JavaModelCache.VERBOSE) {
		System.out.println(JavaModelManager.getJavaModelManager().cacheToString("-> ")); //$NON-NLS-1$
	}
}
/**
 * Note: a buffer with no unsaved changes can be closed by the Java Model
 * since it has a finite number of buffers allowed open at one time. If this
 * is the first time a request is being made for the buffer, an attempt is
 * made to create and fill this element's buffer. If the buffer has been
 * closed since it was first opened, the buffer is re-created.
 *
 * @see IOpenable
 */
public IBuffer getBuffer() throws JavaScriptModelException {
	if (hasBuffer()) {
		// ensure element is open
		Object info = getElementInfo();
		IBuffer buffer = getBufferManager().getBuffer(this);
		if (buffer == null) {
			// try to (re)open a buffer
			buffer = openBuffer(null, info);
		}
		if (buffer instanceof NullBuffer) {
			return null;
		}
		return buffer;
	} else {
		return null;
	}
}
/**
 * Answers the buffer factory to use for creating new buffers
 * @deprecated
 */
public IBufferFactory getBufferFactory(){
	return getBufferManager().getDefaultBufferFactory();
}

/**
 * Returns the buffer manager for this element.
 */
protected BufferManager getBufferManager() {
	return BufferManager.getDefaultBufferManager();
}
/**
 * Return my underlying resource. Elements that may not have a
 * corresponding resource must override this method.
 *
 * @see IJavaScriptElement
 */
public IResource getCorrespondingResource() throws JavaScriptModelException {
	return getUnderlyingResource();
}
/*
 * @see IJavaScriptElement
 */
public IOpenable getOpenable() {
	return this;
}



/**
 * @see IJavaScriptElement
 */
public IResource getUnderlyingResource() throws JavaScriptModelException {
	IResource parentResource = this.parent.getUnderlyingResource();
	if (parentResource == null) {
		return null;
	}
	int type = parentResource.getType();
	if (type == IResource.FOLDER || type == IResource.PROJECT) {
		IContainer folder = (IContainer) parentResource;
		IResource resource = folder.findMember(getElementName());
		if (resource == null) {
			throw newNotPresentException();
		} else {
			return resource;
		}
	} else {
		return parentResource;
	}
}

/**
 * Returns true if this element may have an associated source buffer,
 * otherwise false. Subclasses must override as required.
 */
protected boolean hasBuffer() {
	return false;
}
/**
 * @see IOpenable
 */
public boolean hasUnsavedChanges() throws JavaScriptModelException{

	if (isReadOnly() || !isOpen()) {
		return false;
	}
	IBuffer buf = this.getBuffer();
	if (buf != null && buf.hasUnsavedChanges()) {
		return true;
	}
	// for package fragments, package fragment roots, and projects must check open buffers
	// to see if they have an child with unsaved changes
	int elementType = getElementType();
	if (elementType == PACKAGE_FRAGMENT ||
		elementType == PACKAGE_FRAGMENT_ROOT ||
		elementType == JAVASCRIPT_PROJECT ||
		elementType == JAVASCRIPT_MODEL) { // fix for 1FWNMHH
		Enumeration openBuffers= getBufferManager().getOpenBuffers();
		while (openBuffers.hasMoreElements()) {
			IBuffer buffer= (IBuffer)openBuffers.nextElement();
			if (buffer.hasUnsavedChanges()) {
				IJavaScriptElement owner= (IJavaScriptElement)buffer.getOwner();
				if (isAncestorOf(owner)) {
					return true;
				}
			}
		}
	}

	return false;
}
/**
 * Subclasses must override as required.
 *
 * @see IOpenable
 */
public boolean isConsistent() {
	return true;
}
/**
 *
 * @see IOpenable
 */
public boolean isOpen() {
	return JavaModelManager.getJavaModelManager().getInfo(this) != null;
}
/**
 * Returns true if this represents a source element.
 * Openable source elements have an associated buffer created
 * when they are opened.
 */
protected boolean isSourceElement() {
	return false;
}
/**
 * @see IJavaScriptElement
 */
public boolean isStructureKnown() throws JavaScriptModelException {
	return ((OpenableElementInfo)getElementInfo()).isStructureKnown();
}
/**
 * @see IOpenable
 */
public void makeConsistent(IProgressMonitor monitor) throws JavaScriptModelException {
	// only compilation units can be inconsistent
	// other openables cannot be inconsistent so default is to do nothing
}
/**
 * @see IOpenable
 */
public void open(IProgressMonitor pm) throws JavaScriptModelException {
	getElementInfo(pm);
}

/**
 * Opens a buffer on the contents of this element, and returns
 * the buffer, or returns <code>null</code> if opening fails.
 * By default, do nothing - subclasses that have buffers
 * must override as required.
 */
protected IBuffer openBuffer(IProgressMonitor pm, Object info) throws JavaScriptModelException {
	return null;
}

/**
 * Open the parent element if necessary.
 */
protected void openParent(Object childInfo, HashMap newElements, IProgressMonitor pm) throws JavaScriptModelException {

	Openable openableParent = (Openable)getOpenableParent();
	if (openableParent != null && !openableParent.isOpen()){
		openableParent.generateInfos(openableParent.createElementInfo(), newElements, pm);
	}
}

/**
 *  Answers true if the parent exists (null parent is answering true)
 *
 */
protected boolean parentExists(){

	IJavaScriptElement parentElement = getParent();
	if (parentElement == null) return true;
	return parentElement.exists();
}

/**
 * Returns whether the corresponding resource or associated file exists
 */
protected boolean resourceExists() {
	IWorkspace workspace = ResourcesPlugin.getWorkspace();
	if (workspace == null) return false; // workaround for http://bugs.eclipse.org/bugs/show_bug.cgi?id=34069
	return
		JavaModel.getTarget(
			workspace.getRoot(),
			this.getPath().makeRelative(), // ensure path is relative (see http://dev.eclipse.org/bugs/show_bug.cgi?id=22517)
			true) != null;
}

/**
 * @see IOpenable
 */
public void save(IProgressMonitor pm, boolean force) throws JavaScriptModelException {
	if (isReadOnly()) {
		throw new JavaScriptModelException(new JavaModelStatus(IJavaScriptModelStatusConstants.READ_ONLY, this));
	}
	IBuffer buf = getBuffer();
	if (buf != null) { // some Openables (like a JavaProject) don't have a buffer
		buf.save(pm, force);
		this.makeConsistent(pm); // update the element info of this element
	}
}

/**
 * Find enclosing package fragment root if any
 */
public PackageFragmentRoot getPackageFragmentRoot() {
	return (PackageFragmentRoot) getAncestor(IJavaScriptElement.PACKAGE_FRAGMENT_ROOT);
}

}
