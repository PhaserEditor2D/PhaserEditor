/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.core.util;

import java.util.HashMap;
import java.util.HashSet;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.wst.jsdt.core.IClassFile;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchScope;
import org.eclipse.wst.jsdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.AbstractVariableDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ClassScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.MethodScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Scope;
import org.eclipse.wst.jsdt.internal.core.JavaModel;
import org.eclipse.wst.jsdt.internal.core.JavaModelManager;
import org.eclipse.wst.jsdt.internal.core.JavaProject;
import org.eclipse.wst.jsdt.internal.core.LibraryFragmentRoot;
import org.eclipse.wst.jsdt.internal.core.Openable;
import org.eclipse.wst.jsdt.internal.core.PackageFragmentRoot;
import org.eclipse.wst.jsdt.internal.core.SourceRefElement;

/**
 * Creates java element handles.
 */
public class HandleFactory {

	/**
	 * Cache package fragment root information to optimize speed performance.
	 */
	private String lastPkgFragmentRootPath;
	private PackageFragmentRoot lastPkgFragmentRoot;
	private boolean lastIsFullPath;
	
	/**
	 * Cache package handles to optimize memory.
	 */
	private HashtableOfArrayToObject packageHandles;

	private JavaModel javaModel;

	public HandleFactory() {
		this.javaModel = JavaModelManager.getJavaModelManager().getJavaModel();
	}


	/**
	 * Creates an Openable handle from the given resource path.
	 * The resource path can be a path to a file in the workbench (eg. /Proj/com/ibm/jdt/core/HandleFactory.js)
	 * or a path to a file in a jar file - it then contains the path to the jar file and the path to the file in the jar
	 * (eg. c:/jdk1.2.2/jre/lib/rt.jar|java/lang/Object.class or /Proj/rt.jar|java/lang/Object.class)
	 * NOTE: This assumes that the resource path is the toString() of an IPath,
	 *       in other words, it uses the IPath.SEPARATOR for file path
	 *            and it uses '/' for entries in a zip file.
	 * If not null, uses the given scope as a hint for getting Java project handles.
	 */
	public Openable createOpenable(String resourcePath, IJavaScriptSearchScope scope) {
		int separatorIndex;
		if ((separatorIndex= resourcePath.indexOf(IJavaScriptSearchScope.JAR_FILE_ENTRY_SEPARATOR)) > -1) {
			// path to a class file inside a jar
			// Optimization: cache package fragment root handle and package handles
			int rootPathLength;
			if (this.lastPkgFragmentRootPath == null
					|| (rootPathLength = this.lastPkgFragmentRootPath.length()) != resourcePath.length()
					|| !resourcePath.regionMatches(0, this.lastPkgFragmentRootPath, 0, rootPathLength)) {
				String jarPath= resourcePath.substring(0, separatorIndex);
				PackageFragmentRoot root= (PackageFragmentRoot) this.getJarPkgFragmentRoot(jarPath, scope);
				if (root == null)
					return null; // match is outside classpath
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
			IClassFile classFile= pkgFragment.getClassFile(simpleNames[length]);
			return (Openable) classFile;
		} else {
			// path to a file in a directory
			// Optimization: cache package fragment root handle and package handles
			int rootPathLength = -1;
			//FIXME - may want to add some performance checks back here at some point
			// Fix for https://bugs.eclipse.org/bugs/show_bug.cgi?id=317511
//			if (this.lastPkgFragmentRootPath == null
//				|| !(resourcePath.startsWith(this.lastPkgFragmentRootPath)
//					&& (rootPathLength = this.lastPkgFragmentRootPath.length()) > 0
//					&& resourcePath.charAt(rootPathLength) == '/')) {

				if (resourcePath.endsWith("/")) //$NON-NLS-1$
					resourcePath=resourcePath.substring(0,resourcePath.length()-1);
				PackageFragmentRoot root= this.getPkgFragmentRoot(resourcePath);
				if (root instanceof LibraryFragmentRoot && !((LibraryFragmentRoot)root).isDirectory())
				{
					return (Openable)((LibraryFragmentRoot)root).getPackageFragment(resourcePath).getClassFile(resourcePath);
				}
				if (root == null)
					return null; // match is outside classpath
				this.lastPkgFragmentRoot = root;
				this.lastPkgFragmentRootPath = this.lastIsFullPath ? 
						this.lastPkgFragmentRoot.getLocation().toString() : this.lastPkgFragmentRoot.getPath().toString();
				this.packageHandles = new HashtableOfArrayToObject(5);
			//}
			// create handle
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
			
			if (pkgFragment.isSource() && 
					!Util.isMetadataFileName(simpleName)) {
				IJavaScriptUnit unit= pkgFragment.getJavaScriptUnit(simpleName);
				return (Openable) unit;
			} else {
				IClassFile classFile= pkgFragment.getClassFile(simpleName);
				return (Openable) classFile;
			}
		}
	}

	/**
	 * Returns a handle denoting the class member identified by its scope.
	 */
	public IJavaScriptElement createElement(ClassScope scope, IJavaScriptUnit unit, HashSet existingElements, HashMap knownScopes) {
		return createElement(scope, scope.referenceContext.sourceStart, unit, existingElements, knownScopes);
	}
	/**
	 * Create handle by adding child to parent obtained by recursing into parent scopes.
	 */
	private IJavaScriptElement createElement(Scope scope, int elementPosition, IJavaScriptUnit unit, HashSet existingElements, HashMap knownScopes) {
		IJavaScriptElement newElement = (IJavaScriptElement)knownScopes.get(scope);
		if (newElement != null) return newElement;

		switch(scope.kind) {
			case Scope.COMPILATION_UNIT_SCOPE :
				newElement = unit;
				break;
			case Scope.CLASS_SCOPE :
				IJavaScriptElement parentElement = createElement(scope.parent, elementPosition, unit, existingElements, knownScopes);
				switch (parentElement.getElementType()) {
					case IJavaScriptElement.JAVASCRIPT_UNIT :
						newElement = ((IJavaScriptUnit)parentElement).getType(new String(scope.enclosingSourceType().sourceName));
						break;
					case IJavaScriptElement.TYPE :
						newElement = ((IType)parentElement).getType(new String(scope.enclosingSourceType().sourceName));
						break;
					case IJavaScriptElement.FIELD :
					case IJavaScriptElement.INITIALIZER :
					case IJavaScriptElement.METHOD :
					    IMember member = (IMember)parentElement;
					    if (member.isBinary()) {
					        return null;
					    } else {
							newElement = member.getType(new String(scope.enclosingSourceType().sourceName), 1);
							// increment occurrence count if collision is detected
							if (newElement != null) {
								while (!existingElements.add(newElement)) ((SourceRefElement)newElement).occurrenceCount++;
							}
					    }
						break;
				}
				if (newElement != null) {
					knownScopes.put(scope, newElement);
				}
				break;
			case Scope.METHOD_SCOPE :
				IType parentType = (IType) createElement(scope.parent, elementPosition, unit, existingElements, knownScopes);
				MethodScope methodScope = (MethodScope) scope;
				if (methodScope.isInsideInitializer()) {
					// inside field or initializer, must find proper one
					TypeDeclaration type = methodScope.referenceType();
					int occurenceCount = 1;
					for (int i = 0, length = type.fields.length; i < length; i++) {
						FieldDeclaration field = type.fields[i];
						if (field.declarationSourceStart < elementPosition && field.declarationSourceEnd > elementPosition) {
							switch (field.getKind()) {
								case AbstractVariableDeclaration.FIELD :
									newElement = parentType.getField(new String(field.name));
									break;
								case AbstractVariableDeclaration.INITIALIZER :
									newElement = parentType.getInitializer(occurenceCount);
									break;
							}
							break;
						} else if (field.getKind() == AbstractVariableDeclaration.INITIALIZER) {
							occurenceCount++;
						}
					}
				} else {
					// method element
					AbstractMethodDeclaration method = methodScope.referenceMethod();
					newElement = parentType.getFunction(new String(method.getName()), Util.typeParameterSignatures(method));
					if (newElement != null) {
						knownScopes.put(scope, newElement);
					}
				}
				break;
			case Scope.BLOCK_SCOPE :
				// standard block, no element per se
				newElement = createElement(scope.parent, elementPosition, unit, existingElements, knownScopes);
				break;
		}
		return newElement;
	}
	/**
	 * Returns the package fragment root that corresponds to the given jar path.
	 * See createOpenable(...) for the format of the jar path string.
	 * If not null, uses the given scope as a hint for getting Java project handles.
	 */
	private IPackageFragmentRoot getJarPkgFragmentRoot(String jarPathString, IJavaScriptSearchScope scope) {

		IPath jarPath= new Path(jarPathString);

		Object target = JavaModel.getTarget(ResourcesPlugin.getWorkspace().getRoot(), jarPath, false);
		if (target instanceof IFile) {
			// internal jar: is it on the classpath of its project?
			//  e.g. org.eclipse.swt.win32/ws/win32/swt.jar
			//        is NOT on the classpath of org.eclipse.swt.win32
			IFile jarFile = (IFile)target;
			JavaProject javaProject = (JavaProject) this.javaModel.getJavaProject(jarFile);
			try {
				IIncludePathEntry entry = javaProject.getClasspathEntryFor(jarPath);
				if (entry != null) {
					return javaProject.getPackageFragmentRoot(jarFile);
				}
			} catch (JavaScriptModelException e) {
				// ignore and try to find another project
			}
		}

		// walk projects in the scope and find the first one that has the given jar path in its classpath
		IJavaScriptProject[] projects;
		if (scope != null) {
			IPath[] enclosingProjectsAndJars = scope.enclosingProjectsAndJars();
			int length = enclosingProjectsAndJars.length;
			projects = new IJavaScriptProject[length];
			int index = 0;
			for (int i = 0; i < length; i++) {
				IPath path = enclosingProjectsAndJars[i];
				if (!org.eclipse.wst.jsdt.internal.compiler.util.Util.isArchiveFileName(path.lastSegment())) {
					projects[index++] = this.javaModel.getJavaScriptProject(path.segment(0));
				}
			}
			if (index < length) {
				System.arraycopy(projects, 0, projects = new IJavaScriptProject[index], 0, index);
			}
			IPackageFragmentRoot root = getJarPkgFragmentRoot(jarPath, target, projects);
			if (root != null) {
				return root;
			}
		}

		// not found in the scope, walk all projects
		try {
			projects = this.javaModel.getJavaScriptProjects();
		} catch (JavaScriptModelException e) {
			// java model is not accessible
			return null;
		}
		return getJarPkgFragmentRoot(jarPath, target, projects);
	}

	private IPackageFragmentRoot getJarPkgFragmentRoot(
		IPath jarPath,
		Object target,
		IJavaScriptProject[] projects) {
		for (int i= 0, projectCount= projects.length; i < projectCount; i++) {
			try {
				JavaProject javaProject= (JavaProject)projects[i];
				IIncludePathEntry classpathEntry = javaProject.getClasspathEntryFor(jarPath);
				if (classpathEntry != null) {
					if (target instanceof IFile) {
						// internal jar
						return javaProject.getPackageFragmentRoot((IFile)target);
					} else {
						// external jar
						return javaProject.getPackageFragmentRoot0(jarPath);
					}
				}
			} catch (JavaScriptModelException e) {
				// JavaScriptModelException from getResolvedClasspath - a problem occured while accessing project: nothing we can do, ignore
			}
		}
		return null;
	}

	/**
	 * Returns the package fragment root that contains the given resource path.
	 */
	private PackageFragmentRoot getPkgFragmentRoot(String pathString) {

		IPath path= new Path(pathString);
		IProject[] projects= ResourcesPlugin.getWorkspace().getRoot().getProjects();
		this.lastIsFullPath=false;
		for (int i= 0, max= projects.length; i < max; i++) {
			try {
				IProject project = projects[i];
				if (!project.isAccessible()
					|| !project.hasNature(JavaScriptCore.NATURE_ID)) continue;
				IJavaScriptProject javaProject= this.javaModel.getJavaProject(project);
				IPackageFragmentRoot[] roots= javaProject.getPackageFragmentRoots();
				for (int j= 0, rootCount= roots.length; j < rootCount; j++) {
					if (!(roots[j] instanceof PackageFragmentRoot)) {
						continue;
					}
					PackageFragmentRoot root= (PackageFragmentRoot)roots[j];
					if ( root.getPath().isPrefixOf(path)
							&& !Util.isExcluded(path, root.fullInclusionPatternChars(), root.fullExclusionPatternChars(), false))
					{
						return root;
					}
					
					IPath rootLocation = root.getLocation();
					if (rootLocation != null && !root.isExternal() && rootLocation.isPrefixOf(path)
							&& !Util.isExcluded(path, root.fullInclusionPatternChars(), root.fullExclusionPatternChars(), false)) {
						this.lastIsFullPath=true;
						return root;
					}
				}
			} catch (CoreException e) {
				// CoreException from hasNature - should not happen since we check that the project is accessible
				// JavaScriptModelException from getPackageFragmentRoots - a problem occured while accessing project: nothing we can do, ignore
			}
		}
		return null;
	}

}
