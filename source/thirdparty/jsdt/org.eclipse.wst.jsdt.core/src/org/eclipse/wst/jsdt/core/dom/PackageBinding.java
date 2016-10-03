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

package org.eclipse.wst.jsdt.core.dom;

import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.internal.compiler.env.INameEnvironment;
import org.eclipse.wst.jsdt.internal.compiler.util.Util;
import org.eclipse.wst.jsdt.internal.core.NameLookup;
import org.eclipse.wst.jsdt.internal.core.SearchableEnvironment;

/**
 * Internal implementation of package bindings.
 */
class PackageBinding implements IPackageBinding {

	private static final String[] NO_NAME_COMPONENTS = CharOperation.NO_STRINGS;
	private static final String UNNAMED = Util.EMPTY_STRING;
	private static final char PACKAGE_NAME_SEPARATOR = '.';

	private org.eclipse.wst.jsdt.internal.compiler.lookup.PackageBinding binding;
	private String name;
//	private BindingResolver resolver;
	private String[] components;

	PackageBinding(org.eclipse.wst.jsdt.internal.compiler.lookup.PackageBinding binding, BindingResolver resolver) {
		this.binding = binding;
//		this.resolver = resolver;
	}


	/*
	 * @see IBinding#getName()
	 */
	public String getName() {
		if (name == null) {
			computeNameAndComponents();
		}
		return name;
	}

	/*
	 * @see IPackageBinding#isUnnamed()
	 */
	public boolean isUnnamed() {
		return getName().equals(UNNAMED);
	}

	/*
	 * @see IPackageBinding#getNameComponents()
	 */
	public String[] getNameComponents() {
		if (components == null) {
			computeNameAndComponents();
		}
		return components;
	}

	/*
	 * @see IBinding#getKind()
	 */
	public int getKind() {
		return IBinding.PACKAGE;
	}

	/*
	 * @see IBinding#getModifiers()
	 */
	public int getModifiers() {
		return Modifier.NONE;
	}

	/*
	 * @see IBinding#isDeprecated()
	 */
	public boolean isDeprecated() {
		return false;
	}

	/**
	 * @see IBinding#isRecovered()
	 */
	public boolean isRecovered() {
		return false;
	}

	/*
	 * @see IBinding#getJavaElement()
	 */
	public IJavaScriptElement getJavaElement() {
		INameEnvironment nameEnvironment = this.binding.environment.nameEnvironment; // a package binding always has a LooupEnvironment set
		if (!(nameEnvironment instanceof SearchableEnvironment)) return null;
		NameLookup nameLookup = ((SearchableEnvironment) nameEnvironment).nameLookup;
		if (nameLookup == null) return null;
		IJavaScriptElement[] pkgs = nameLookup.findPackageFragments(getName(), false/*exact match*/);
		if (pkgs == null) return null;
		return pkgs[0];
	}

	/*
	 * @see IBinding#getKey()
	 */
	public String getKey() {
		return new String(this.binding.computeUniqueKey());
	}

	/*
	 * @see IBinding#isEqualTo(Binding)
	 *  
	 */
	public boolean isEqualTo(IBinding other) {
		if (other == this) {
			// identical binding - equal (key or no key)
			return true;
		}
		if (other == null) {
			// other binding missing
			return false;
		}
		if (!(other instanceof PackageBinding)) {
			return false;
		}
		org.eclipse.wst.jsdt.internal.compiler.lookup.PackageBinding packageBinding2 = ((PackageBinding) other).binding;
		return CharOperation.equals(this.binding.compoundName, packageBinding2.compoundName);
	}

	private void computeNameAndComponents() {
		char[][] compoundName = this.binding.compoundName;
		if (compoundName == CharOperation.NO_CHAR_CHAR || compoundName == null) {
			name = UNNAMED;
			components = NO_NAME_COMPONENTS;
		} else {
			int length = compoundName.length;
			components = new String[length];
			StringBuffer buffer = new StringBuffer();
			for (int i = 0; i < length - 1; i++) {
				components[i] = new String(compoundName[i]);
				buffer.append(compoundName[i]).append(PACKAGE_NAME_SEPARATOR);
			}
			components[length - 1] = new String(compoundName[length - 1]);
			buffer.append(compoundName[length - 1]);
			name = buffer.toString();
		}
	}

	/*
	 * For debugging purpose only.
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return this.binding.toString();
	}
}
