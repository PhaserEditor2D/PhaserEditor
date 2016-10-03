/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.core.search;

import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.IType;

/**
 * A match collected while {@link SearchEngine searching} for
 * all type names methods using a {@link TypeNameRequestor requestor}.
 * <p>
 * The type of this match is available from {@link #getType()}.
 * </p>
 * <p>
 * This class is not intended to be overridden by clients.
 * </p>
 *
 * @see TypeNameMatchRequestor
 * @see SearchEngine#searchAllTypeNames(char[], int, char[], int, int, IJavaScriptSearchScope, TypeNameMatchRequestor, int, org.eclipse.core.runtime.IProgressMonitor)
 * @see SearchEngine#searchAllTypeNames(char[][], char[][], IJavaScriptSearchScope, TypeNameMatchRequestor, int, org.eclipse.core.runtime.IProgressMonitor)
 * 
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public abstract class TypeNameMatch {

/**
 * Returns the matched type's fully qualified name using '.' character
 * as separator (e.g. package name + '.' enclosing type names + '.' simple name).
 *
 * @see #getType()
 * @see IType#getFullyQualifiedName(char)
 *
 * @throws NullPointerException if matched type is <code> null</code>
 * @return Fully qualified type name of the type
 */
public String getFullyQualifiedName() {
	return getType().getFullyQualifiedName('.');
}

/**
 * Returns the modifiers of the matched type.
 * <p>
 * This is a handle-only method as neither JavaScript Model nor includepath
 * initialization is done while calling this method.
 *
 * @return the type modifiers
 */
public abstract int getModifiers();

/**
 * Returns the package fragment root of the stored type.
 * Package fragment root cannot be null and <strong>does</strong> exist.
 *
 * @see #getType()
 * @see IJavaScriptElement#getAncestor(int)
 *
 * @throws NullPointerException if matched type is <code> null</code>
 * @return the existing javascript model package fragment root (ie. cannot be <code>null</code>
 * 	and will return <code>true</code> to <code>exists()</code> message).
 */
public IPackageFragmentRoot getPackageFragmentRoot() {
	return (IPackageFragmentRoot) getType().getAncestor(IJavaScriptElement.PACKAGE_FRAGMENT_ROOT);
}

/**
 * Returns the package name of the stored type.
 *
 * @see #getType()
 * @see IType#getPackageFragment()
 *
 * @throws NullPointerException if matched type is <code> null</code>
 * @return the package name
 */
public String getPackageName() {
	String name=getType().getElementName();
	int index=name.lastIndexOf(".");
	if (index>=0)
		return name.substring(0,index);
	return "";
}

/**
 * Returns the name of the stored type.
 *
 * @see #getType()
 * @see IJavaScriptElement#getElementName()
 *
 * @throws NullPointerException if matched type is <code> null</code>
 * @return the type name
 */
public String getSimpleTypeName() {
	String name=getType().getElementName();
	int index=name.lastIndexOf(".");
	if (index>=0)
		name=name.substring(index+1);
	return name;
}

/**
 * Returns the stored super type names.
 * 
 * @return the stored super type names, or <b>null</b>.
 * @since 1.2
 */
public char[][] getSuperTypeNames() {
	return null;
}

/**
 * Returns a javascript model type handle.
 * This handle may exist or not, but is not supposed to be <code>null</code>.
 * <p>
 * This is a handle-only method as neither JavaScript Model nor includepath
 * initializations are done while calling this method.
 *
 * @see IType
 * @return the non-null handle on matched javascript model type.
 */
public abstract IType getType();

/**
 * Name of the type container using '.' character
 * as separator (e.g. package name + '.' + enclosing type names).
 *
 * @see #getType()
 * @see org.eclipse.wst.jsdt.core.IMember#getDeclaringType()
 *
 * @throws NullPointerException if matched type is <code> null</code>
 * @return name of the type container
 */
public String getTypeContainerName() {
	IType outerType = getType().getDeclaringType();
	if (outerType != null) {
		return outerType.getFullyQualifiedName('.');
	} else {
		return getType().getPackageFragment().getElementName();
	}
}

/**
 * Returns the matched type's type qualified name using '.' character
 * as separator (e.g. enclosing type names + '.' + simple name).
 *
 * @see #getType()
 * @see IType#getTypeQualifiedName(char)
 *
 * @throws NullPointerException if matched type is <code> null</code>
 * @return fully qualified type name of the type
 */
public String getTypeQualifiedName() {
	return getType().getTypeQualifiedName('.');
}

public String getQualifiedName() {
return getType().getElementName();	
}

}
