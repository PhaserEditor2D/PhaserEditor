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
package org.eclipse.wst.jsdt.core.search;

import org.eclipse.core.runtime.IPath;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;

/**
 * An <code>IJavaScriptSearchScope</code> defines where search result should be found by a
 * <code>SearchEngine</code>. Clients must pass an instance of this interface
 * to the <code>search(...)</code> methods. Such an instance can be created using the
 * following factory methods on <code>SearchEngine</code>: <code>createHierarchyScope(IType)</code>,
 * <code>createJavaSearchScope(IResource[])</code>, <code>createWorkspaceScope()</code>, or
 * clients may choose to implement this interface.
 * 
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public interface IJavaScriptSearchScope {
/**
 * This constant defines the separator of the resourcePath string of the <code>encloses(String)</code>
 * method. If present in the string, it separates the path to the jar file from the path
 * to the .class file in the jar.
 */
String JAR_FILE_ENTRY_SEPARATOR = "|"; //$NON-NLS-1$
/**
 * Include type constant (bit mask) indicating that source folders should be considered in the search scope.
 *  
 */
int SOURCES = 1;
/**
 * Include type constant (bit mask) indicating that application libraries should be considered in the search scope.
 *  
 */
int APPLICATION_LIBRARIES = 2;
/**
 * Include type constant (bit mask) indicating that system libraries should be considered in the search scope.
 *  
 */
int SYSTEM_LIBRARIES = 4;
/**
 * Include type constant (bit mask) indicating that referenced projects should be considered in the search scope.
 *  
 */
int REFERENCED_PROJECTS = 8;
/**
 * Checks whether the resource at the given path is enclosed by this scope.
 *
 * @param resourcePath if the resource is contained in
 * a JAR file, the path is composed of 2 paths separated
 * by <code>JAR_FILE_ENTRY_SEPARATOR</code>: the first path is the full OS path
 * to the JAR (if it is an external JAR), or the workspace relative <code>IPath</code>
 * to the JAR (if it is an internal JAR),
 * the second path is the path to the resource inside the JAR.
 * @return whether the resource is enclosed by this scope
 */
public boolean encloses(String resourcePath);
/**
 * Checks whether this scope encloses the given element.
 *
 * @param element the given element
 * @return <code>true</code> if the element is in this scope
 */
public boolean encloses(IJavaScriptElement element);
/**
 * Returns the paths to the enclosing projects and JARs for this search scope.
 * <ul>
 * <li> If the path is a project path, this is the full path of the project
 *       (see <code>IResource.getFullPath()</code>).
 *        For example, /MyProject
 * </li>
 * <li> If the path is a JAR path and this JAR is internal to the workspace,
 *        this is the full path of the JAR file (see <code>IResource.getFullPath()</code>).
 *        For example, /MyProject/mylib.jar
 * </li>
 * <li> If the path is a JAR path and this JAR is external to the workspace,
 *        this is the full OS path to the JAR file on the file system.
 *        For example, d:\libs\mylib.jar
 * </li>
 * </ul>
 *
 * @return an array of paths to the enclosing projects and JARS.
 */
IPath[] enclosingProjectsAndJars();
public boolean shouldExclude(String container, String resourceName);
}
