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
package org.eclipse.wst.jsdt.internal.core.search;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.wst.jsdt.core.IClassFile;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchScope;
import org.eclipse.wst.jsdt.core.search.TypeNameMatchRequestor;
import org.eclipse.wst.jsdt.internal.compiler.env.AccessRestriction;
import org.eclipse.wst.jsdt.internal.core.LibraryFragmentRoot;
import org.eclipse.wst.jsdt.internal.core.Openable;
import org.eclipse.wst.jsdt.internal.core.PackageFragmentRoot;
import org.eclipse.wst.jsdt.internal.core.util.HandleFactory;
import org.eclipse.wst.jsdt.internal.core.util.HashtableOfArrayToObject;

/**
 * Wrapper used to link {@link IRestrictedAccessTypeRequestor} with {@link org.eclipse.wst.jsdt.core.search.TypeNameRequestor}.
 * This wrapper specifically allows usage of internal method {@link BasicSearchEngine#searchAllTypeNames(
 * 	char[] packageName,
 * 	int packageMatchRule,
 * 	char[] typeName,
 * 	int typeMatchRule,
 * 	int searchFor,
 * 	org.eclipse.wst.jsdt.core.search.IJavaScriptSearchScope scope,
 * 	IRestrictedAccessTypeRequestor nameRequestor,
 * 	int waitingPolicy,
 * 	org.eclipse.core.runtime.IProgressMonitor monitor) }.
 * from  API method {@link org.eclipse.wst.jsdt.core.search.SearchEngine#searchAllTypeNames(
 * 	char[] packageName,
 * 	int packageMatchRule,
 * 	char[] typeName,
 * 	int matchRule,
 * 	int searchFor,
 * 	org.eclipse.wst.jsdt.core.search.IJavaScriptSearchScope scope,
 * 	org.eclipse.wst.jsdt.core.search.TypeNameRequestor nameRequestor,
 * 	int waitingPolicy,
 * 	org.eclipse.core.runtime.IProgressMonitor monitor) }.
 */
public class TypeNameMatchRequestorWrapper implements IRestrictedAccessTypeRequestor {
	TypeNameMatchRequestor requestor;
	private IJavaScriptSearchScope scope; // scope is needed to retrieve project path for external resource
	private HandleFactory handleFactory; // in case of IJavaScriptSearchScope defined by clients, use an HandleFactory instead

	/**
	 * Cache package fragment root information to optimize speed performance.
	 */
	private String lastPkgFragmentRootPath;
	private IPackageFragmentRoot lastPkgFragmentRoot;

	/**
	 * Cache package handles to optimize memory.
	 */
	private HashtableOfArrayToObject packageHandles;

public TypeNameMatchRequestorWrapper(TypeNameMatchRequestor requestor, IJavaScriptSearchScope scope) {
	this.requestor = requestor;
	this.scope = scope;
	if (!(scope instanceof JavaSearchScope)) {
		this.handleFactory = new HandleFactory();
	}
}

/* (non-Javadoc)
 * @see org.eclipse.wst.jsdt.internal.core.search.IRestrictedAccessTypeRequestor#acceptType(int, char[], char[], char[][], java.lang.String, org.eclipse.wst.jsdt.internal.compiler.env.AccessRestriction)
 */
public void acceptType(int modifiers, char[] packageName, char[] simpleTypeName, char[][] superTypeNames, char[][] enclosingTypeNames, String path, AccessRestriction access) {
	try {
		IType type = null;
		
		if (packageName!=null && packageName.length>0 && (CharOperation.indexOf('.',simpleTypeName) == -1))
		{
			simpleTypeName=CharOperation.concat(packageName, simpleTypeName, '.');
		}
		if (this.handleFactory != null) {
			Openable openable = this.handleFactory.createOpenable(path, this.scope);
			if (openable == null) return;
			switch (openable.getElementType()) {
				case IJavaScriptElement.JAVASCRIPT_UNIT:
					IJavaScriptUnit cu = (IJavaScriptUnit) openable;
					if (enclosingTypeNames != null && enclosingTypeNames.length > 0) {
						type = cu.getType(new String(enclosingTypeNames[0]));
						for (int j=1, l=enclosingTypeNames.length; j<l; j++) {
							type = type.getType(new String(enclosingTypeNames[j]));
						}
						type = type.getType(new String(simpleTypeName));
					} else {
						type = cu.getType(new String(simpleTypeName));
					}
					break;
				case IJavaScriptElement.CLASS_FILE:
					type = ((IClassFile)openable).getType();
					break;
			}
		} else {
			int separatorIndex= path.indexOf(IJavaScriptSearchScope.JAR_FILE_ENTRY_SEPARATOR);
			type = separatorIndex == -1
				? createTypeFromPath(path, new String(simpleTypeName), enclosingTypeNames)
				: createTypeFromJar(path, separatorIndex);
		}
		if (type != null) {
			this.requestor.acceptTypeNameMatch(new JavaSearchTypeNameMatch(type, superTypeNames, modifiers));
		}
	} catch (JavaScriptModelException e) {
		// skip
	}
}
private IType createTypeFromJar(String resourcePath, int separatorIndex) throws JavaScriptModelException {
	// path to a class file inside a jar
	// Optimization: cache package fragment root handle and package handles
	if (this.lastPkgFragmentRootPath == null
			|| this.lastPkgFragmentRootPath.length() > resourcePath.length()
			|| !resourcePath.startsWith(this.lastPkgFragmentRootPath)) {
		String jarPath= resourcePath.substring(0, separatorIndex);
		IPackageFragmentRoot root= ((JavaSearchScope)this.scope).packageFragmentRoot(resourcePath);
		if (root == null) return null;
		this.lastPkgFragmentRootPath= jarPath;
		this.lastPkgFragmentRoot= root;
		this.packageHandles= new HashtableOfArrayToObject(5);
	}
	// create handle
	String classFilePath= resourcePath.substring(separatorIndex + 1);
	String[] simpleNames = new Path(classFilePath).segments();
	String[] pkgName;
	int length = simpleNames.length-1;
	if (length > 0) {
		pkgName = new String[length];
		System.arraycopy(simpleNames, 0, pkgName, 0, length);
	} else {
		pkgName = CharOperation.NO_STRINGS;
	}
	IPackageFragment pkgFragment= (IPackageFragment) this.packageHandles.get(pkgName);
	if (pkgFragment == null) {
		pkgFragment= ((PackageFragmentRoot) this.lastPkgFragmentRoot).getPackageFragment(pkgName);
		this.packageHandles.put(pkgName, pkgFragment);
	}
	return pkgFragment.getClassFile(simpleNames[length]).getType();
}
private IType createTypeFromPath(String resourcePath, String simpleTypeName, char[][] enclosingTypeNames) throws JavaScriptModelException {
	// path to a file in a directory
	// Optimization: cache package fragment root handle and package handles
	int rootPathLength = -1;
	boolean samePath=false;
	if(resourcePath!=null && this.lastPkgFragmentRoot!=null) {
		IPath path1 = new Path(resourcePath);
		IPath path2 = new Path(this.lastPkgFragmentRootPath);
		samePath = path1.equals(path2);
	}
	if (!samePath && (this.lastPkgFragmentRootPath == null || !(resourcePath.startsWith(this.lastPkgFragmentRootPath)
			&& ((rootPathLength = this.lastPkgFragmentRootPath.length()) > 0
			&& (rootPathLength<resourcePath.length()))
			&& resourcePath.charAt(rootPathLength) == '/'))) {
		IPackageFragmentRoot root = ((JavaSearchScope)this.scope).packageFragmentRoot(resourcePath);
		if (root == null) return null;
		this.lastPkgFragmentRoot = root;
		this.lastPkgFragmentRootPath = this.lastPkgFragmentRoot.getPath().toString();
		this.packageHandles = new HashtableOfArrayToObject(5);
	}

	
	boolean isLibrary = this.lastPkgFragmentRoot instanceof LibraryFragmentRoot && !((LibraryFragmentRoot)this.lastPkgFragmentRoot).isDirectory();
	// create handle
	if(isLibrary) {
		String[] pkgName = new String[] {this.lastPkgFragmentRootPath};
		IPackageFragment pkgFragment= (IPackageFragment) this.packageHandles.get(pkgName);
		if (pkgFragment == null) {
			pkgFragment= ((PackageFragmentRoot) this.lastPkgFragmentRoot).getPackageFragment(pkgName[0]);
			this.packageHandles.put(pkgName, pkgFragment);
		}
		IClassFile classFile= pkgFragment.getClassFile(pkgName[0]);
		return classFile.getType(simpleTypeName);
	}else {
		resourcePath = resourcePath.substring(this.lastPkgFragmentRootPath.length() + 1);
		String[] simpleNames = new Path(resourcePath).segments();
		String[] pkgName;
		int length = simpleNames.length-1;
		if (length > 0) {
			pkgName = new String[length];
			System.arraycopy(simpleNames, 0, pkgName, 0, length);
		} else {
			pkgName = CharOperation.NO_STRINGS;
		}
		IPackageFragment pkgFragment= (IPackageFragment) this.packageHandles.get(pkgName);
		if (pkgFragment == null) {
			pkgFragment= ((PackageFragmentRoot) this.lastPkgFragmentRoot).getPackageFragment(pkgName);
			this.packageHandles.put(pkgName, pkgFragment);
		}
		String simpleName= simpleNames[length];
		if (org.eclipse.wst.jsdt.internal.core.util.Util.isJavaLikeFileName(simpleName) && pkgFragment.getKind()!=IPackageFragmentRoot.K_BINARY) {
			IJavaScriptUnit unit= pkgFragment.getJavaScriptUnit(simpleName);
			int etnLength = enclosingTypeNames == null ? 0 : enclosingTypeNames.length;
			IType type = (etnLength == 0) ? unit.getType(simpleTypeName) : unit.getType(new String(enclosingTypeNames[0]));
			if (etnLength > 0) {
				for (int i=1; i<etnLength; i++) {
					type = type.getType(new String(enclosingTypeNames[i]));
				}
				type = type.getType(simpleTypeName);
			}
			return type;
		} else {
			IClassFile classFile= pkgFragment.getClassFile(simpleName);
			return classFile.getType(simpleTypeName);
		}
	}

}
}
