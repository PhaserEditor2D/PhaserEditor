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

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.wst.jsdt.core.IClassFile;
import org.eclipse.wst.jsdt.core.IIncludePathAttribute;
import org.eclipse.wst.jsdt.core.IJsGlobalScopeContainer;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IField;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptModel;
import org.eclipse.wst.jsdt.core.IJavaScriptModelStatus;
import org.eclipse.wst.jsdt.core.IJavaScriptModelStatusConstants;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IOpenable;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.IParent;
import org.eclipse.wst.jsdt.core.ISourceRange;
import org.eclipse.wst.jsdt.core.ISourceReference;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.LibrarySuperType;
import org.eclipse.wst.jsdt.core.WorkingCopyOwner;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Binding;
import org.eclipse.wst.jsdt.internal.core.util.MementoTokenizer;
import org.eclipse.wst.jsdt.internal.core.util.Util;

/**
 * Root of Java element handle hierarchy.
 *
 * @see IJavaScriptElement
 */
public abstract class JavaElement extends PlatformObject implements IJavaScriptElement {
//	private static final QualifiedName PROJECT_JAVADOC= new QualifiedName(JavaScriptCore.PLUGIN_ID, "project_javadoc_location"); //$NON-NLS-1$

	private static final byte[] CLOSING_DOUBLE_QUOTE = new byte[] { 34 };
	private static final byte[] CHARSET = new byte[] {99, 104, 97, 114, 115, 101, 116, 61 };
	private static final byte[] CONTENT_TYPE = new byte[] { 34, 67, 111, 110, 116, 101, 110, 116, 45, 84, 121, 112, 101, 34 };
	private static final byte[] CONTENT = new byte[] { 99, 111, 110, 116, 101, 110, 116, 61, 34 };
	public static final char JEM_ESCAPE = '\\';
	public static final char JEM_JAVAPROJECT = '=';
	public static final char JEM_PACKAGEFRAGMENTROOT = '/';
	public static final char JEM_PACKAGEFRAGMENT = '<';
	public static final char JEM_FIELD = '^';
	public static final char JEM_METHOD = '~';
	public static final char JEM_INITIALIZER = '|';
	public static final char JEM_COMPILATIONUNIT = '{';
	public static final char JEM_CLASSFILE = '(';
	public static final char JEM_METADATA = '&';
	public static final char JEM_TYPE = '[';
	public static final char JEM_PACKAGEDECLARATION = '%';
	public static final char JEM_IMPORTDECLARATION = '#';
	public static final char JEM_COUNT = '!';
	public static final char JEM_LOCALVARIABLE = '@';
	public static final char JEM_TYPE_PARAMETER = ']';

	/**
	 * This element's parent, or <code>null</code> if this
	 * element does not have a parent.
	 */
	protected JavaElement parent;

	protected static final JavaElement[] NO_ELEMENTS = new JavaElement[0];
	protected static final Object NO_INFO = new Object();

	/**
	 * Constructs a handle for a java element with
	 * the given parent element.
	 *
	 * @param parent The parent of java element
	 *
	 * @exception IllegalArgumentException if the type is not one of the valid
	 *		Java element type constants
	 *
	 */
	protected JavaElement(JavaElement parent) throws IllegalArgumentException {
		this.parent = parent;
	}
	/**
	 * @see IOpenable
	 */
	public void close() throws JavaScriptModelException {
		JavaModelManager.getJavaModelManager().removeInfoAndChildren(this);
	}
	/**
	 * This element is being closed.  Do any necessary cleanup.
	 */
	protected abstract void closing(Object info) throws JavaScriptModelException;
	/*
	 * Returns a new element info for this element.
	 */
	protected abstract Object createElementInfo();
	/**
	 * Returns true if this handle represents the same Java element
	 * as the given handle. By default, two handles represent the same
	 * element if they are identical or if they represent the same type
	 * of element, have equal names, parents, and occurrence counts.
	 *
	 * <p>If a subclass has other requirements for equality, this method
	 * must be overridden.
	 *
	 * @see Object#equals
	 */
	public boolean equals(Object o) {

		if (this == o) return true;

		// Java model parent is null
		if (this.parent == null) return super.equals(o);

		// assume instanceof check is done in subclass
		JavaElement other = (JavaElement) o;
		return getElementName().equals(other.getElementName()) &&
				this.parent.equals(other.parent);
	}
	protected void escapeMementoName(StringBuffer buffer, String mementoName) {
		for (int i = 0, length = mementoName.length(); i < length; i++) {
			char character = mementoName.charAt(i);
			switch (character) {
				case JEM_ESCAPE:
				case JEM_COUNT:
				case JEM_JAVAPROJECT:
				case JEM_PACKAGEFRAGMENTROOT:
				case JEM_PACKAGEFRAGMENT:
				case JEM_FIELD:
				case JEM_METHOD:
				case JEM_INITIALIZER:
				case JEM_COMPILATIONUNIT:
				case JEM_CLASSFILE:
				case JEM_TYPE:
				case JEM_PACKAGEDECLARATION:
				case JEM_IMPORTDECLARATION:
				case JEM_LOCALVARIABLE:
				case JEM_TYPE_PARAMETER:
					buffer.append(JEM_ESCAPE);
			}
			buffer.append(character);
		}
	}
	/**
	 * @see IJavaScriptElement
	 */
	public boolean exists() {

		try {
			getElementInfo();
			return true;
		} catch (JavaScriptModelException e) {
			// element doesn't exist: return false
		}
		return false;
	}

	/**
	 * Returns the <code>ASTNode</code> that corresponds to this <code>JavaElement</code>
	 * or <code>null</code> if there is no corresponding node.
	 */
	public ASTNode findNode(JavaScriptUnit ast) {
		return null; // works only inside a compilation unit
	}
	/**
	 * Generates the element infos for this element, its ancestors (if they are not opened) and its children (if it is an Openable).
	 * Puts the newly created element info in the given map.
	 */
	protected abstract void generateInfos(Object info, HashMap newElements, IProgressMonitor pm) throws JavaScriptModelException;

	/**
	 * @see IJavaScriptElement
	 */
	public IJavaScriptElement getAncestor(int ancestorType) {

		IJavaScriptElement element = this;
		while (element != null) {
			if (element.getElementType() == ancestorType)  return element;
			element= element.getParent();
		}
		return null;
	}
	/**
	 * @see IParent
	 */
	public IJavaScriptElement[] getChildren() throws JavaScriptModelException {
		Object elementInfo = getElementInfo();
		if (elementInfo instanceof JavaElementInfo) {
			return ((JavaElementInfo)elementInfo).getChildren();
		} else {
			return NO_ELEMENTS;
		}
	}
	/**
	 * Returns a collection of (immediate) children of this node of the
	 * specified type.
	 *
	 * @param type - one of the JEM_* constants defined by JavaElement
	 */
	public ArrayList getChildrenOfType(int type) throws JavaScriptModelException {
		IJavaScriptElement[] children = getChildren();
		int size = children.length;
		ArrayList list = new ArrayList(size);
		for (int i = 0; i < size; ++i) {
			JavaElement elt = (JavaElement)children[i];
			if (elt.getElementType() == type) {
				list.add(elt);
			}
		}
		return list;
	}
	/**
	 * @see org.eclipse.wst.jsdt.core.IMember
	 */
	public IClassFile getClassFile() {
		return null;
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
		return null;
	}
	/**
	 * Returns the info for this handle.
	 * If this element is not already open, it and all of its parents are opened.
	 * Does not return null.
	 * NOTE: BinaryType infos are NOT rooted under JavaElementInfo.
	 * @exception JavaScriptModelException if the element is not present or not accessible
	 */
	public Object getElementInfo() throws JavaScriptModelException {
		return getElementInfo(null);
	}
	/**
	 * Returns the info for this handle.
	 * If this element is not already open, it and all of its parents are opened.
	 * Does not return null.
	 * NOTE: BinaryType infos are NOT rooted under JavaElementInfo.
	 * @exception JavaScriptModelException if the element is not present or not accessible
	 */
	public Object getElementInfo(IProgressMonitor monitor) throws JavaScriptModelException {

		JavaModelManager manager = JavaModelManager.getJavaModelManager();
		Object info = manager.getInfo(this);
		if (info != null) return info;
		return openWhenClosed(createElementInfo(), monitor);
	}
	/**
	 * @see org.eclipse.core.runtime.IAdaptable
	 */
	public String getElementName() {
		return ""; //$NON-NLS-1$
	}
	/*
	 * Creates a Java element handle from the given memento.
	 * The given token is the current delimiter indicating the type of the next token(s).
	 * The given working copy owner is used only for compilation unit handles.
	 */
	public abstract IJavaScriptElement getHandleFromMemento(String token, MementoTokenizer memento, WorkingCopyOwner owner);
	/*
	 * Creates a Java element handle from the given memento.
	 * The given working copy owner is used only for compilation unit handles.
	 */
	public IJavaScriptElement getHandleFromMemento(MementoTokenizer memento, WorkingCopyOwner owner) {
		if (!memento.hasMoreTokens()) return this;
		String token = memento.nextToken();
		return getHandleFromMemento(token, memento, owner);
	}
	/**
	 * @see IJavaScriptElement
	 */
	public String getHandleIdentifier() {
		return getHandleMemento();
	}
	/**
	 * @see JavaElement#getHandleMemento()
	 */
	public String getHandleMemento(){
		StringBuffer buff = new StringBuffer();
		getHandleMemento(buff);
		return buff.toString();
	}
	protected void getHandleMemento(StringBuffer buff) {
		((JavaElement)getParent()).getHandleMemento(buff);
		buff.append(getHandleMementoDelimiter());
		escapeMementoName(buff, getElementName());
	}
	/**
	 * Returns the <code>char</code> that marks the start of this handles
	 * contribution to a memento.
	 */
	protected abstract char getHandleMementoDelimiter();
	/**
	 * @see IJavaScriptElement
	 */
	public IJavaScriptModel getJavaScriptModel() {
		IJavaScriptElement current = this;
		do {
			if (current instanceof IJavaScriptModel) return (IJavaScriptModel) current;
		} while ((current = current.getParent()) != null);
		return null;
	}

	/**
	 * @see IJavaScriptElement
	 */
	public IJavaScriptProject getJavaScriptProject() {
		IJavaScriptElement current = this;
		do {
			if (current instanceof IJavaScriptProject) return (IJavaScriptProject) current;
		} while ((current = current.getParent()) != null);
		return null;
	}
	/*
	 * @see IJavaScriptElement
	 */
	public IOpenable getOpenable() {
		return this.getOpenableParent();
	}
	/**
	 * Return the first instance of IOpenable in the parent
	 * hierarchy of this element.
	 *
	 * <p>Subclasses that are not IOpenable's must override this method.
	 */
	public IOpenable getOpenableParent() {
		return (IOpenable)this.parent;
	}
	/**
	 * @see IJavaScriptElement
	 */
	public IJavaScriptElement getParent() {
		return this.parent;
	}
	/*
	 * @see IJavaScriptElement#getPrimaryElement()
	 */
	public IJavaScriptElement getPrimaryElement() {
		return getPrimaryElement(true);
	}
	/*
	 * Returns the primary element. If checkOwner, and the cu owner is primary,
	 * return this element.
	 */
	public IJavaScriptElement getPrimaryElement(boolean checkOwner) {
		return this;
	}
	/**
	 * Returns the element that is located at the given source position
	 * in this element.  This is a helper method for <code>IJavaScriptUnit#getElementAt</code>,
	 * and only works on compilation units and types. The position given is
	 * known to be within this element's source range already, and if no finer
	 * grained element is found at the position, this element is returned.
	 */
	protected IJavaScriptElement getSourceElementAt(int position) throws JavaScriptModelException {
		if (this instanceof ISourceReference) {
			IJavaScriptElement[] children = getChildren();
			for (int i = children.length-1; i >= 0; i--) {
				IJavaScriptElement aChild = children[i];
				if (aChild instanceof SourceRefElement) {
					SourceRefElement child = (SourceRefElement) children[i];
					ISourceRange range = child.getSourceRange();
					int start = range.getOffset();
					int end = start + range.getLength();
					if (start <= position && position <= end) {
						if (child instanceof IField) {
							// check muti-declaration case (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=39943)
							int declarationStart = start;
							SourceRefElement candidate = null;
							do {
								// check name range
								range = ((IField)child).getNameRange();
								if (position <= range.getOffset() + range.getLength()) {
									candidate = child;
								} else {
									return candidate == null ? child.getSourceElementAt(position) : candidate.getSourceElementAt(position);
								}
								child = --i>=0 ? (SourceRefElement) children[i] : null;
							} while (child != null && child.getSourceRange().getOffset() == declarationStart);
							// position in field's type: use first field
							return candidate.getSourceElementAt(position);
						} else if (child instanceof IParent) {
							return child.getSourceElementAt(position);
						} else {
							return child;
						}
					}
				}
			}
		} else {
			// should not happen
			Assert.isTrue(false);
		}
		return this;
	}
	/**
	 * Returns the SourceMapper facility for this element, or
	 * <code>null</code> if this element does not have a
	 * SourceMapper.
	 */
	public SourceMapper getSourceMapper() {
		return ((JavaElement)getParent()).getSourceMapper();
	}
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.IJavaScriptElement#getSchedulingRule()
	 */
	public ISchedulingRule getSchedulingRule() {
		IResource resource = getResource();
		if (resource == null) {
			class NoResourceSchedulingRule implements ISchedulingRule {
				public IPath path;
				public NoResourceSchedulingRule(IPath path) {
					this.path = path;
				}
				public boolean contains(ISchedulingRule rule) {
					if (rule instanceof NoResourceSchedulingRule) {
						return this.path.isPrefixOf(((NoResourceSchedulingRule)rule).path);
					} else {
						return false;
					}
				}
				public boolean isConflicting(ISchedulingRule rule) {
					if (rule instanceof NoResourceSchedulingRule) {
						IPath otherPath = ((NoResourceSchedulingRule)rule).path;
						return this.path.isPrefixOf(otherPath) || otherPath.isPrefixOf(this.path);
					} else {
						return false;
					}
				}
			}
			return new NoResourceSchedulingRule(getPath());
		} else {
			return resource;
		}
	}
	/**
	 * @see IParent
	 */
	public boolean hasChildren() throws JavaScriptModelException {
		// if I am not open, return true to avoid opening (case of a Java project, a compilation unit or a class file).
		// also see https://bugs.eclipse.org/bugs/show_bug.cgi?id=52474
		Object elementInfo = JavaModelManager.getJavaModelManager().getInfo(this);
		if (elementInfo instanceof JavaElementInfo) {
			return ((JavaElementInfo)elementInfo).getChildren().length > 0;
		} else {
			return true;
		}
	}

	/**
	 * Returns the hash code for this Java element. By default,
	 * the hash code for an element is a combination of its name
	 * and parent's hash code. Elements with other requirements must
	 * override this method.
	 */
	public int hashCode() {
		if (this.parent == null) return super.hashCode();
		return Util.combineHashCodes(getElementName().hashCode(), this.parent.hashCode());
	}
	/**
	 * Returns true if this element is an ancestor of the given element,
	 * otherwise false.
	 */
	public boolean isAncestorOf(IJavaScriptElement e) {
		IJavaScriptElement parentElement= e.getParent();
		while (parentElement != null && !parentElement.equals(this)) {
			parentElement= parentElement.getParent();
		}
		return parentElement != null;
	}

	/**
	 * @see IJavaScriptElement
	 */
	public boolean isReadOnly() {
		return false;
	}
	/**
	 * Creates and returns a new not present exception for this element.
	 */
	public JavaScriptModelException newNotPresentException() {
		return new JavaScriptModelException(new JavaModelStatus(IJavaScriptModelStatusConstants.ELEMENT_DOES_NOT_EXIST, this));
	}
	/**
	 * Creates and returns a new Java model exception for this element with the given status.
	 */
	public JavaScriptModelException newJavaModelException(IStatus status) {
		if (status instanceof IJavaScriptModelStatus)
			return new JavaScriptModelException((IJavaScriptModelStatus) status);
		else
			return new JavaScriptModelException(new JavaModelStatus(status.getSeverity(), status.getCode(), status.getMessage()));
	}
	/*
	 * Opens an <code>Openable</code> that is known to be closed (no check for <code>isOpen()</code>).
	 * Returns the created element info.
	 */
	protected Object openWhenClosed(Object info, IProgressMonitor monitor) throws JavaScriptModelException {
		JavaModelManager manager = JavaModelManager.getJavaModelManager();
		boolean hadTemporaryCache = manager.hasTemporaryCache();
		try {
			HashMap newElements = manager.getTemporaryCache();
			generateInfos(info, newElements, monitor);
			if (info == null) {
				info = newElements.get(this);
			}
			if (info == null) { // a source ref element could not be opened
				// close the buffer that was opened for the openable parent
			    // close only the openable's buffer (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=62854)
			    Openable openable = (Openable) getOpenable();
			    if (newElements.containsKey(openable)) {
			        openable.closeBuffer();
			    }
				throw newNotPresentException();
			}
			if (!hadTemporaryCache) {
				manager.putInfos(this, newElements);
			}
		} finally {
			if (!hadTemporaryCache) {
				manager.resetTemporaryCache();
			}
		}
		return info;
	}
	/**
	 */
	public String readableName() {
		return this.getElementName();
	}
	public JavaElement resolved(Binding binding) {
		return this;
	}
	public JavaElement unresolved() {
		return this;
	}
	protected String tabString(int tab) {
		StringBuffer buffer = new StringBuffer();
		for (int i = tab; i > 0; i--)
			buffer.append("  "); //$NON-NLS-1$
		return buffer.toString();
	}
	/**
	 * Debugging purposes
	 */
	public String toDebugString() {
		StringBuffer buffer = new StringBuffer();
		this.toStringInfo(0, buffer, NO_INFO, true/*show resolved info*/);
		return buffer.toString();
	}
	/**
	 *  Debugging purposes
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		toString(0, buffer);
		return buffer.toString();
	}
	/**
	 *  Debugging purposes
	 */
	protected void toString(int tab, StringBuffer buffer) {
		Object info = this.toStringInfo(tab, buffer);
		if (tab == 0) {
			this.toStringAncestors(buffer);
		}
		this.toStringChildren(tab, buffer, info);
	}
	/**
	 *  Debugging purposes
	 */
	public String toStringWithAncestors() {
		return toStringWithAncestors(true/*show resolved info*/);
	}
		/**
	 *  Debugging purposes
	 */
	public String toStringWithAncestors(boolean showResolvedInfo) {
		StringBuffer buffer = new StringBuffer();
		this.toStringInfo(0, buffer, NO_INFO, showResolvedInfo);
		this.toStringAncestors(buffer);
		return buffer.toString();
	}
	/**
	 *  Debugging purposes
	 */
	protected void toStringAncestors(StringBuffer buffer) {
		JavaElement parentElement = (JavaElement)this.getParent();
		if (parentElement != null && parentElement.getParent() != null) {
			buffer.append(" [in "); //$NON-NLS-1$
			parentElement.toStringInfo(0, buffer, NO_INFO, false/*don't show resolved info*/);
			parentElement.toStringAncestors(buffer);
			buffer.append("]"); //$NON-NLS-1$
		}
	}
	/**
	 *  Debugging purposes
	 */
	protected void toStringChildren(int tab, StringBuffer buffer, Object info) {
		if (info == null || !(info instanceof JavaElementInfo)) return;
		IJavaScriptElement[] children = ((JavaElementInfo)info).getChildren();
		for (int i = 0; i < children.length; i++) {
			buffer.append("\n"); //$NON-NLS-1$
			((JavaElement)children[i]).toString(tab + 1, buffer);
		}
	}
	/**
	 *  Debugging purposes
	 */
	public Object toStringInfo(int tab, StringBuffer buffer) {
		Object info = JavaModelManager.getJavaModelManager().peekAtInfo(this);
		this.toStringInfo(tab, buffer, info, true/*show resolved info*/);
		return info;
	}
	/**
	 *  Debugging purposes
	 * @param showResolvedInfo TODO
	 */
	protected void toStringInfo(int tab, StringBuffer buffer, Object info, boolean showResolvedInfo) {
		buffer.append(this.tabString(tab));
		toStringName(buffer);
		if (info == null) {
			buffer.append(" (not open)"); //$NON-NLS-1$
		}
	}
	/**
	 *  Debugging purposes
	 */
	protected void toStringName(StringBuffer buffer) {
		buffer.append(getElementName());
	}

	protected URL getJavadocBaseLocation() throws JavaScriptModelException {
		IPackageFragmentRoot root= (IPackageFragmentRoot) this.getAncestor(IJavaScriptElement.PACKAGE_FRAGMENT_ROOT);
		if (root == null) {
			return null;
		}

		if (root.getKind() == IPackageFragmentRoot.K_BINARY) {
			IIncludePathEntry entry= root.getRawIncludepathEntry();
			if (entry == null) {
				return null;
			}
			if (entry.getEntryKind() == IIncludePathEntry.CPE_CONTAINER) {
				entry= getRealClasspathEntry(root.getJavaScriptProject(), entry.getPath(), root.getPath());
				if (entry == null) {
					return null;
				}
			}
			return getLibraryJavadocLocation(entry);
		}
		return null;
	}

	private static IIncludePathEntry getRealClasspathEntry(IJavaScriptProject jproject, IPath containerPath, IPath libPath) throws JavaScriptModelException {
		IJsGlobalScopeContainer container= JavaScriptCore.getJsGlobalScopeContainer(containerPath, jproject);
		if (container != null) {
			IIncludePathEntry[] entries= container.getIncludepathEntries();
			for (int i= 0; i < entries.length; i++) {
				IIncludePathEntry curr = entries[i];
				if (curr == null) {
					if (JavaModelManager.CP_RESOLVE_VERBOSE) {
						JavaModelManager.getJavaModelManager().verbose_missbehaving_container(jproject, containerPath, entries);
					}
					break;
				}
				IIncludePathEntry resolved= JavaScriptCore.getResolvedIncludepathEntry(curr);
				if (resolved != null && libPath.equals(resolved.getPath())) {
					return curr; // return the real entry
				}
			}
		}
		return null; // not found
	}

	protected static URL getLibraryJavadocLocation(IIncludePathEntry entry) throws JavaScriptModelException {
		switch(entry.getEntryKind()) {
			case IIncludePathEntry.CPE_LIBRARY :
			case IIncludePathEntry.CPE_VARIABLE :
				break;
			default :
				throw new IllegalArgumentException("Entry must be of kind CPE_LIBRARY or CPE_VARIABLE"); //$NON-NLS-1$
		}

		IIncludePathAttribute[] extraAttributes= entry.getExtraAttributes();
		for (int i= 0; i < extraAttributes.length; i++) {
			IIncludePathAttribute attrib= extraAttributes[i];
			if (IIncludePathAttribute.JSDOC_LOCATION_ATTRIBUTE_NAME.equals(attrib.getName())) {
				String value = attrib.getValue();
				try {
					return new URL(value);
				} catch (MalformedURLException e) {
					throw new JavaScriptModelException(new JavaModelStatus(IJavaScriptModelStatusConstants.CANNOT_RETRIEVE_ATTACHED_JSDOC, value));
				}
			}
		}
		return null;
	}

	/*
	 * @see IJavaScriptElement#getAttachedJavadoc(IProgressMonitor)
	 */
	public String getAttachedJavadoc(IProgressMonitor monitor) throws JavaScriptModelException {
		return null;
	}

	int getIndexOf(byte[] array, byte[] toBeFound, int start) {
		if (array == null || toBeFound == null)
			return -1;
		final int toBeFoundLength = toBeFound.length;
		final int arrayLength = array.length;
		if (arrayLength < toBeFoundLength)
			return -1;
		loop: for (int i = start, max = arrayLength - toBeFoundLength + 1; i < max; i++) {
			if (array[i] == toBeFound[0]) {
				for (int j = 1; j < toBeFoundLength; j++) {
					if (array[i + j] != toBeFound[j])
						continue loop;
				}
				return i;
			}
		}
		return -1;
	}
	/*
	 * We don't use getContentEncoding() on the URL connection, because it might leave open streams behind.
	 * See https://bugs.eclipse.org/bugs/show_bug.cgi?id=117890
	 */
	protected String getURLContents(String docUrlValue) throws JavaScriptModelException {
		InputStream stream = null;
		JarURLConnection connection2 = null;
		try {
			URL docUrl = new URL(docUrlValue);
			URLConnection connection = docUrl.openConnection();
			if (connection instanceof JarURLConnection) {
				connection2 = (JarURLConnection) connection;
				// https://bugs.eclipse.org/bugs/show_bug.cgi?id=156307
				connection.setUseCaches(false);
			}
			stream = new BufferedInputStream(connection.getInputStream());
			String encoding = connection.getContentEncoding();
			byte[] contents = org.eclipse.wst.jsdt.internal.compiler.util.Util.getInputStreamAsByteArray(stream, connection.getContentLength());
			if (encoding == null) {
				int index = getIndexOf(contents, CONTENT_TYPE, 0);
				if (index != -1) {
					index = getIndexOf(contents, CONTENT, index);
					if (index != -1) {
						int offset = index + CONTENT.length;
						int index2 = getIndexOf(contents, CLOSING_DOUBLE_QUOTE, offset);
						if (index2 != -1) {
							final int charsetIndex = getIndexOf(contents, CHARSET, offset);
							if (charsetIndex != -1) {
								int start = charsetIndex + CHARSET.length;
								encoding = new String(contents, start, index2 - start, "UTF-8"); //$NON-NLS-1$
							}
						}
					}
				}
			}
			try {
				if (encoding == null) {
					encoding = this.getJavaScriptProject().getProject().getDefaultCharset();
				}
			} catch (CoreException e) {
				// ignore
			}
			if (contents != null) {
				if (encoding != null) {
					return new String(contents, encoding);
				} else {
					// platform encoding is used
					return new String(contents);
				}
			}
 		} catch (MalformedURLException e) {
 			throw new JavaScriptModelException(new JavaModelStatus(IJavaScriptModelStatusConstants.CANNOT_RETRIEVE_ATTACHED_JSDOC, this));
		} catch (FileNotFoundException e) {
			// ignore. see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=120559
		} catch(IOException e) {
			StringWriter stringWriter = new StringWriter();
			PrintWriter writer = new PrintWriter(stringWriter);
			e.printStackTrace(writer);
			writer.flush();
			writer.close();
			throw new JavaScriptModelException(new JavaModelStatus(IJavaScriptModelStatusConstants.CANNOT_RETRIEVE_ATTACHED_JSDOC, this, String.valueOf(stringWriter.getBuffer())));
		} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException e) {
					// ignore
				}
			}
			if (connection2 != null) {
				try {
					connection2.getJarFile().close();
				} catch(IOException e) {
					// ignore
				} catch(IllegalStateException e) {
					/*
					 * ignore. Can happen in case the stream.close() did close the jar file
					 * see https://bugs.eclipse.org/bugs/show_bug.cgi?id=140750
					 */
				}
 			}
		}
		return null;
	}

	/*
	 * Returns a new name lookup. This name lookup first looks in the given working copies.
	 */
	public NameLookup newNameLookup(IJavaScriptUnit[] workingCopies) throws JavaScriptModelException {
		return parent!=null?parent.newNameLookup(workingCopies):getJavaScriptProject().newNameLookup(workingCopies);
	}

	/*
	 * Returns a new name lookup. This name lookup first looks in the working copies of the given owner.
	 */
	public NameLookup newNameLookup(WorkingCopyOwner owner) throws JavaScriptModelException {

		return parent!=null?parent.newNameLookup(owner):getJavaScriptProject().newNameLookup(owner);
	}

	/*
	 * Returns a new search name environment for this project. This name environment first looks in the given working copies.
	 */
	public SearchableEnvironment newSearchableNameEnvironment(IJavaScriptUnit[] workingCopies) throws JavaScriptModelException {
		return parent!=null?parent.newSearchableNameEnvironment(workingCopies):getJavaScriptProject().newSearchableNameEnvironment(workingCopies);
	}

	/*
	 * Returns a new search name environment for this project. This name environment first looks in the working copies
	 * of the given owner.
	 */
	public SearchableEnvironment newSearchableNameEnvironment(WorkingCopyOwner owner) throws JavaScriptModelException {
		return parent!=null?parent.newSearchableNameEnvironment(owner):getJavaScriptProject().newSearchableNameEnvironment(owner);
	}

	public String getDisplayName() {
		return getElementName();
	}

	public boolean isVirtual() {
		return parent.isVirtual();
	}

	public URI getHostPath() {
		if(isVirtual()) return parent.getHostPath();
		return null;
	}
	public LibrarySuperType getCommonSuperType() {
		return null;
	}
}
