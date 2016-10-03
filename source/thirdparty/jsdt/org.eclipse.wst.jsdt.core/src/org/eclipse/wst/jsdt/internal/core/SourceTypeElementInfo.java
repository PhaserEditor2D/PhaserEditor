/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.internal.compiler.env.ISourceField;
import org.eclipse.wst.jsdt.internal.compiler.env.ISourceImport;
import org.eclipse.wst.jsdt.internal.compiler.env.ISourceMethod;
import org.eclipse.wst.jsdt.internal.compiler.env.ISourceType;

/**
 * Element info for an IType element that originated from source.
 */
public class SourceTypeElementInfo extends MemberElementInfo implements ISourceType {

	protected static final ISourceImport[] NO_IMPORTS = new ISourceImport[0];
	protected static final InitializerElementInfo[] NO_INITIALIZERS = new InitializerElementInfo[0];
	protected static final SourceField[] NO_FIELDS = new SourceField[0];
	protected static final SourceMethod[] NO_METHODS = new SourceMethod[0];
	protected static final SourceType[] NO_TYPES = new SourceType[0];
	/**
	 * The name of the superclass for this type. This name
	 * is fully qualified for binary types and is NOT
	 * fully qualified for source types.
	 */
	protected char[] superclassName;

	/**
	 * The names of the interfaces this type implements or
	 * extends. These names are fully qualified in the case
	 * of a binary type, and are NOT fully qualified in the
	 * case of a source type
	 */
	protected char[][] superInterfaceNames;

	/**
	 * Backpointer to my type handle - useful for translation
	 * from info to handle.
	 */
	protected IType handle = null;

	/*
	 * A map from an IJavaScriptElement (this type or a child of this type) to a String[] (the categories of this element)
	 */
	protected HashMap categories;

	protected boolean isBinary;

	protected boolean isAnonymous;

	public SourceTypeElementInfo(boolean isBinary,boolean anonymous) {
		super();
		this.isBinary=isBinary;
		this.isAnonymous=anonymous;
	}

	public SourceTypeElementInfo(boolean isBinary) {
		this(isBinary,false);
	}

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
 * Returns the ISourceType that is the enclosing type for this
 * type, or <code>null</code> if this type is a top level type.
 */
public ISourceType getEnclosingType() {
	IJavaScriptElement parent= this.handle.getParent();
	if (parent != null && parent.getElementType() == IJavaScriptElement.TYPE) {
		try {
			return (ISourceType)((JavaElement)parent).getElementInfo();
		} catch (JavaScriptModelException e) {
			return null;
		}
	} else {
		return null;
	}
}
/**
 * @see ISourceType
 */
public ISourceField[] getFields() {
	SourceField[] fieldHandles = getFieldHandles();
	int length = fieldHandles.length;
	ISourceField[] fields = new ISourceField[length];
	for (int i = 0; i < length; i++) {
		try {
			ISourceField field = (ISourceField) fieldHandles[i].getElementInfo();
			fields[i] = field;
		} catch (JavaScriptModelException e) {
			// ignore
		}
	}
	return fields;
}
public SourceField[] getFieldHandles() {
	int length = this.children.length;
	if (length == 0) return NO_FIELDS;
	SourceField[] fields = new SourceField[length];
	int fieldIndex = 0;
	for (int i = 0; i < length; i++) {
		IJavaScriptElement child = this.children[i];
		if (child instanceof SourceField)
			fields[fieldIndex++] = (SourceField) child;
	}
	if (fieldIndex == 0) return NO_FIELDS;
	if (fieldIndex < length)
		System.arraycopy(fields, 0, fields = new SourceField[fieldIndex], 0, fieldIndex);
	return fields;
}
/**
 * @see org.eclipse.wst.jsdt.internal.compiler.env.IDependent#getFileName()
 */
public char[] getFileName() {
	return this.handle.getPath().toString().toCharArray();
}
/**
 * Returns the handle for this type info
 */
public IType getHandle() {
	return this.handle;
}
/*
 * Returns the InitializerElementInfos for this type.
 * Returns an empty array if none.
 */
public InitializerElementInfo[] getInitializers() {
	int length = this.children.length;
	if (length == 0) return NO_INITIALIZERS;
	InitializerElementInfo[] initializers = new InitializerElementInfo[length];
	int initializerIndex = 0;
	for (int i = 0; i < length; i++) {
		IJavaScriptElement child = this.children[i];
		if (child instanceof Initializer) {
			try {
				InitializerElementInfo initializer = (InitializerElementInfo)((Initializer)child).getElementInfo();
				initializers[initializerIndex++] = initializer;
			} catch (JavaScriptModelException e) {
				// ignore
			}
		}
	}
	if (initializerIndex == 0) return NO_INITIALIZERS;
	System.arraycopy(initializers, 0, initializers = new InitializerElementInfo[initializerIndex], 0, initializerIndex);
	return initializers;
}
/**
 * @see ISourceType
 */
public char[][] getInterfaceNames() {
	if (this.handle.getElementName().length() == 0) { // if anonymous type
		return null;
	}
	return this.superInterfaceNames;
}

/**
 * @see ISourceType
 */
public ISourceType[] getMemberTypes() {
	SourceType[] memberTypeHandles = getMemberTypeHandles();
	int length = memberTypeHandles.length;
	ISourceType[] memberTypes = new ISourceType[length];
	for (int i = 0; i < length; i++) {
		try {
			ISourceType type = (ISourceType) memberTypeHandles[i].getElementInfo();
			memberTypes[i] = type;
		} catch (JavaScriptModelException e) {
			// ignore
		}
	}
	return memberTypes;
}
public SourceType[] getMemberTypeHandles() {
	int length = this.children.length;
	if (length == 0) return NO_TYPES;
	SourceType[] memberTypes = new SourceType[length];
	int typeIndex = 0;
	for (int i = 0; i < length; i++) {
		IJavaScriptElement child = this.children[i];
		if (child instanceof SourceType)
			memberTypes[typeIndex++] = (SourceType)child;
	}
	if (typeIndex == 0) return NO_TYPES;
	if (typeIndex < length)
		System.arraycopy(memberTypes, 0, memberTypes = new SourceType[typeIndex], 0, typeIndex);
	return memberTypes;
}
/**
 * @see ISourceType
 */
public ISourceMethod[] getMethods() {
	SourceMethod[] methodHandles = getMethodHandles();
	int length = methodHandles.length;
	ISourceMethod[] methods = new ISourceMethod[length];
	int methodIndex = 0;
	for (int i = 0; i < length; i++) {
		try {
			ISourceMethod method = (ISourceMethod) methodHandles[i].getElementInfo();
			methods[methodIndex++] = method;
		} catch (JavaScriptModelException e) {
			// ignore
		}
	}
	return methods;
}
public SourceMethod[] getMethodHandles() {
	int length = this.children.length;
	if (length == 0) return NO_METHODS;
	SourceMethod[] methods = new SourceMethod[length];
	int methodIndex = 0;
	for (int i = 0; i < length; i++) {
		IJavaScriptElement child = this.children[i];
		if (child instanceof SourceMethod)
			methods[methodIndex++] = (SourceMethod) child;
	}
	if (methodIndex == 0) return NO_METHODS;
	if (methodIndex < length)
		System.arraycopy(methods, 0, methods = new SourceMethod[methodIndex], 0, methodIndex);
	return methods;
}
/**
 * @see org.eclipse.wst.jsdt.internal.compiler.env.ISourceType#getName()
 */
public char[] getName() {
	return this.handle.getElementName().toCharArray();
}
/**
 * @see ISourceType
 */
public char[] getSuperclassName() {
	if (this.handle.getElementName().length() == 0) { // if anonymous type
		char[][] interfaceNames = this.superInterfaceNames;
		if (interfaceNames != null && interfaceNames.length > 0) {
			return interfaceNames[0];
		}
	}
	return this.superclassName;
}
/**
 * @see ISourceType
 */
public boolean isBinaryType() {
	return isBinary;
}
/*
 * Returns whether the source type is an anonymous type of a member type.
 */
public boolean isAnonymousMember() {
	return isAnonymous;
}
/**
 * Sets the handle for this type info
 */
protected void setHandle(IType handle) {
	this.handle = handle;
}
/**
 * Sets the (unqualified) name of this type's superclass
 */
protected void setSuperclassName(char[] superclassName) {
	this.superclassName = superclassName;
}
/**
 * Sets the (unqualified) names of the interfaces this type implements or extends
 */
protected void setSuperInterfaceNames(char[][] superInterfaceNames) {
	this.superInterfaceNames = superInterfaceNames;
}
public String toString() {
	return "Info for " + this.handle.toString(); //$NON-NLS-1$
}

public char[][] getPackageName() {
	ISourceType enclosingType = getEnclosingType();
	if (enclosingType!=null)
		return enclosingType.getPackageName();
	return CharOperation.NO_CHAR_CHAR;
}
}
