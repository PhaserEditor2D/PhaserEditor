/*******************************************************************************
 * Copyright (c) 2004, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.core;

import org.eclipse.wst.jsdt.core.IField;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.Signature;

public abstract class NamedMember extends Member {

	/*
	 * This element's name, or an empty <code>String</code> if this
	 * element does not have a name.
	 */
	protected String name;

	public NamedMember(JavaElement parent, String name) {
		super(parent);
		this.name = name;
	}

	public String getElementName() {
		return this.name;
	}

	protected String getKey(IField field, boolean forceOpen) throws JavaScriptModelException {
		StringBuffer key = new StringBuffer();

		// declaring class

		IJavaScriptElement parent = field.getParent();
		String declaringKey = "??"; //$NON-NLS-1$
		if (parent instanceof IJavaScriptUnit)
			 declaringKey = getKey((IJavaScriptUnit) parent, forceOpen);
		else if (parent instanceof IType)
		 declaringKey = getKey((IType) parent, forceOpen);
		key.append(declaringKey);

		// field name
		key.append('.');
		key.append(field.getElementName());

		return key.toString();
	}

	protected String getKey(IFunction method, boolean forceOpen) throws JavaScriptModelException {
		StringBuffer key = new StringBuffer();

		// declaring class
		IJavaScriptElement parent = method.getParent();
		String declaringKey = "??"; //$NON-NLS-1$
		if (parent instanceof IJavaScriptUnit)
			 declaringKey = getKey((IJavaScriptUnit) parent, forceOpen);
		else if (parent instanceof IType)
		 declaringKey = getKey((IType) parent, forceOpen);
		key.append(declaringKey);

		// selector
		key.append('.');
		String selector = method.getElementName();
		key.append(selector);

		// parameters
		key.append('(');
		String[] parameters = method.getParameterNames();
		for (int i = 0, length = parameters.length; i < length; i++)
			key.append(parameters[i].replace('.', '/'));
		key.append(')');

		// return type
		if (forceOpen)
		{
			if (method.getReturnType()!=null)
				key.append(method.getReturnType().replace('.', '/'));
		}
		else
			key.append('V');

		return key.toString();
	}

	protected String getKey(IJavaScriptUnit unit, boolean forceOpen) throws JavaScriptModelException {
		StringBuffer key = new StringBuffer();
		key.append('U');
		String packageName = unit.getParent().getElementName();
		key.append(packageName.replace('.', '/'));
		if (packageName.length() > 0)
			key.append('/');
		key.append(unit.getElementName());
		key.append(';');
		return key.toString();
	}

	protected String getKey(IType type, boolean forceOpen) throws JavaScriptModelException {
		StringBuffer key = new StringBuffer();
		key.append('L');
		String packageName = type.getPackageFragment().getElementName();
		key.append(packageName.replace('.', '/'));
		if (packageName.length() > 0)
			key.append('/');
		String typeQualifiedName = type.getTypeQualifiedName();
		IJavaScriptUnit cu = (IJavaScriptUnit) type.getAncestor(IJavaScriptElement.JAVASCRIPT_UNIT);
		if (cu != null) {
			String cuName = cu.getElementName();
			String mainTypeName = cuName.substring(0, cuName.lastIndexOf('.'));
			String topLevelTypeName = typeQualifiedName;
			if (!mainTypeName.equals(topLevelTypeName)) {
				key.append(mainTypeName);
				key.append('~');
			}
		}
		key.append(typeQualifiedName);
		key.append(';');
		return key.toString();
	}

	protected String getFullyQualifiedParameterizedName(String fullyQualifiedName, String uniqueKey) throws JavaScriptModelException {
		String[] typeArguments = new String[0];
		int length = typeArguments.length;
		if (length == 0) return fullyQualifiedName;
		StringBuffer buffer = new StringBuffer();
		buffer.append(fullyQualifiedName);
		buffer.append('<');
		for (int i = 0; i < length; i++) {
			String typeArgument = typeArguments[i];
			buffer.append(Signature.toString(typeArgument));
			if (i < length-1)
				buffer.append(',');
		}
		buffer.append('>');
		return buffer.toString();
	}

	protected IPackageFragment getPackageFragment() {
		return null;
	}

	public String getFullyQualifiedName(char enclosingTypeSeparator, boolean showParameters) throws JavaScriptModelException {
		String packageName = getPackageFragment().getElementName();
		if (packageName.equals(IPackageFragment.DEFAULT_PACKAGE_NAME)) {
			return getTypeQualifiedName(enclosingTypeSeparator, showParameters);
		}
		return packageName + '.' + getTypeQualifiedName(enclosingTypeSeparator, showParameters);
	}

	public String getTypeQualifiedName(char enclosingTypeSeparator, boolean showParameters) throws JavaScriptModelException {
		NamedMember declaringType;
		switch (this.parent.getElementType()) {
			case IJavaScriptElement.JAVASCRIPT_UNIT:
				if (showParameters) {
					StringBuffer buffer = new StringBuffer(this.name);
					return buffer.toString();
				}
				return this.name;
			case IJavaScriptElement.CLASS_FILE:
				String classFileName = this.parent.getElementName();
					// top level class file: name of type is same as name of class file
				String typeName = this.name;
				if (showParameters) {
					StringBuffer buffer = new StringBuffer(typeName);
					return buffer.toString();
				}
				return typeName;
			case IJavaScriptElement.TYPE:
				declaringType = (NamedMember) this.parent;
				break;
			case IJavaScriptElement.FIELD:
			case IJavaScriptElement.INITIALIZER:
			case IJavaScriptElement.METHOD:
				declaringType = (NamedMember) ((IMember) this.parent).getDeclaringType();
				break;
			default:
				return null;
		}
		String typeQualifiedName = declaringType!=null ?
				declaringType.getTypeQualifiedName(enclosingTypeSeparator, showParameters) : ""; //$NON-NLS-1$
		StringBuffer buffer = new StringBuffer(typeQualifiedName);
		buffer.append(enclosingTypeSeparator);
		String simpleName = this.name.length() == 0 ? Integer.toString(this.occurrenceCount) : this.name;
		buffer.append(simpleName);
		return buffer.toString();
	}
}
