/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
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
import org.eclipse.wst.jsdt.core.IField;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchScope;
import org.eclipse.wst.jsdt.core.search.ModuleMatchRequestor;
import org.eclipse.wst.jsdt.internal.core.LibraryFragmentRoot;
import org.eclipse.wst.jsdt.internal.core.PackageFragmentRoot;
import org.eclipse.wst.jsdt.internal.core.util.HashtableOfArrayToObject;


public class ModuleMatchRequestorWrapper implements IModuleRequestor {
	ModuleMatchRequestor requestor;
	private IJavaScriptSearchScope scope; // scope is needed to retrieve project path for external resource

	/**
	 * Cache package fragment root information to optimize speed performance.
	 */
	private String lastPkgFragmentRootPath;
	private IPackageFragmentRoot lastPkgFragmentRoot;

	/**
	 * Cache package handles to optimize memory.
	 */
	private HashtableOfArrayToObject packageHandles;

	public ModuleMatchRequestorWrapper(ModuleMatchRequestor requestor, IJavaScriptSearchScope scope) {
		this.requestor = requestor;
		this.scope = scope;
	}

	public void acceptType(char[] qualification, char[] simpleName, String path) {
		try {
			IType type = null;
			if (qualification!=null && qualification.length>0 && (CharOperation.indexOf('.',simpleName) == -1)) {
				simpleName=CharOperation.concat(qualification, simpleName, '.');
			}
			int separatorIndex= path.indexOf(IJavaScriptSearchScope.JAR_FILE_ENTRY_SEPARATOR);
			type = separatorIndex == -1 
					? createTypeFromPath(path, new String(simpleName))
					: createTypeFromJar(path, separatorIndex);
			
			if (type != null && !type.isAnonymous()) {
				this.requestor.acceptElementMatch(type);
			}
		} catch (JavaScriptModelException e) {
			// skip
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.core.search.IJSElementRequestor#acceptFunction(char[], char[][], char[][], char[], char[], char[], char[], java.lang.String)
	 */
	public void acceptFunction(char[] signature, char[] declaringQualification, char[] declaringSimpleName, String path) {
		try {
			IType type = null;
			if (declaringQualification!=null && declaringQualification.length>0 && (CharOperation.indexOf('.',declaringSimpleName) == -1)) {
				declaringSimpleName=CharOperation.concat(declaringQualification, declaringSimpleName, '.');
			}
			int separatorIndex= path.indexOf(IJavaScriptSearchScope.JAR_FILE_ENTRY_SEPARATOR);
			type = separatorIndex == -1 
					? createTypeFromPath(path, new String(declaringSimpleName))
					: createTypeFromJar(path, separatorIndex);

			if (type != null) {
				IFunction method = type.getFunction(CharOperation.charToString(signature), new String[0]);
				if (method.exists()) {
					this.requestor.acceptElementMatch(method);
					return;
				}
			}
		}
		catch (JavaScriptModelException e) {
			// skip
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.core.search.IJSElementRequestor#acceptField(char[], char[], char[], char[], char[], java.lang.String)
	 */
	public void acceptField(char[] signature, char[] declaringQualification, char[] declaringSimpleName, String path) {
		try {
			IType type = null;
			if (declaringQualification!=null && declaringQualification.length>0 && (CharOperation.indexOf('.',declaringSimpleName) == -1)) {
				declaringSimpleName=CharOperation.concat(declaringQualification, declaringSimpleName, '.');
			}
			int separatorIndex= path.indexOf(IJavaScriptSearchScope.JAR_FILE_ENTRY_SEPARATOR);
			type = separatorIndex == -1 
					? createTypeFromPath(path, new String(declaringSimpleName))
					: createTypeFromJar(path, separatorIndex);

			if (type != null) {
				IField field = type.getField(CharOperation.charToString(signature));
				if (field.exists()) {
					this.requestor.acceptElementMatch(field);
				}
			}
		}
		catch (JavaScriptModelException e) {
			// skip
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.core.search.IJSElementRequestor#acceptType(org.eclipse.wst.jsdt.core.IType)
	 */
	public void acceptType(IType type) {
		this.requestor.acceptElementMatch(type);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.core.search.IJSElementRequestor#acceptFunction(org.eclipse.wst.jsdt.core.IFunction)
	 */
	public void acceptFunction(IFunction function) {
		this.requestor.acceptElementMatch(function);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.core.search.IJSElementRequestor#acceptField(org.eclipse.wst.jsdt.core.IField)
	 */
	public void acceptField(IField field) {
		this.requestor.acceptElementMatch(field);
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

	private IType createTypeFromPath(String resourcePath, String typeName) throws JavaScriptModelException {
		// path to a file in a directory
		// Optimization: cache package fragment root handle and package handles
		int rootPathLength = -1;
		boolean samePath=false;
		if (resourcePath!=null && this.lastPkgFragmentRoot!=null) {
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
		if (isLibrary) {
			String[] pkgName = new String[] {this.lastPkgFragmentRootPath};
			IPackageFragment pkgFragment= (IPackageFragment) this.packageHandles.get(pkgName);
			if (pkgFragment == null) {
				pkgFragment= ((PackageFragmentRoot) this.lastPkgFragmentRoot).getPackageFragment(pkgName[0]);
				this.packageHandles.put(pkgName, pkgFragment);
			}
			IClassFile classFile= pkgFragment.getClassFile(pkgName[0]);
			return classFile.getType(typeName);
		} else {
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
				IType type = unit.getType(typeName);
				return type;
			} else {
				IClassFile classFile= pkgFragment.getClassFile(simpleName);
				return classFile.getType(typeName);
			}
		}
	}
	
}
