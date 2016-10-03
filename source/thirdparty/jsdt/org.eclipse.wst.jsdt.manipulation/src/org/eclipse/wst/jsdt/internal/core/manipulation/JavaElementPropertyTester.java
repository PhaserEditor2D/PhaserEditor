/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.wst.jsdt.internal.core.manipulation;

import java.util.regex.Pattern;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.expressions.PropertyTester;

import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;

/**
 * A property tester for various properties of IJavaElements.
 * Might be moved down to jdt.core. See bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=127085
 * 
 * @since 3.3
 */
public class JavaElementPropertyTester extends PropertyTester {

	/**
	 * A property indicating the file name (value <code>"name"</code>). Regular expressions are supported.
	 */
	public static final String NAME = "name"; //$NON-NLS-1$
	
	/**
	 * A property indicating if the element is in a open and existing Java project (value <code>"isInJavaProject"</code>). 
	 */
	public static final String IS_IN_JAVA_PROJECT = "isInJavaProject"; //$NON-NLS-1$

	/**
	 * A property indicating if the element is in a open and existing Java project that also implements the given nature (value <code>"isInJavaProjectWithNature"</code>). 
	 */
	public static final String IS_IN_JAVA_PROJECT_WITH_NATURE = "isInJavaProjectWithNature"; //$NON-NLS-1$

	/**
	 * A property indicating if the element is on the classpath (value <code>"isOnClasspath"</code>). 
	 */
	public static final String IS_ON_CLASSPATH = "isOnClasspath"; //$NON-NLS-1$
	
	/**
	 * A property indicating if the a type of the given qualified name is on the classpath (value <code>"hasTypeOnClasspath"</code>). 
	 */
	public static final String HAS_TYPE_ON_CLASSPATH = "hasTypeOnClasspath"; //$NON-NLS-1$
	
	/**
	 * A property indicating if the element is a source folder or is inside a source folder. (value <code>"inSourceFolder"</code>).
	 * <code>false</code> is returned if the element does not exist.
	 */
	public static final String IN_SOURCE_FOLDER = "inSourceFolder"; //$NON-NLS-1$
	
	/**
	 * A property indicating if the element is an archive or is inside an archive. (value <code>"inArchive"</code>).
	 * <code>false</code> is returned if the element does not exist.
	 */
	public static final String IN_ARCHIVE = "inArchive"; //$NON-NLS-1$
	
	/**
	 * A property indicating if the element is an archive (value <code>"inExternalArchive"</code>).
	 * <code>false</code> is returned if the element does not exist.
	 */
	public static final String IN_EXTERNAL_ARCHIVE = "inExternalArchive"; //$NON-NLS-1$
	
	/**
	 * A property indicating a option in the Java project of the selected element
	 * (value <code>"projectOption"</code>). If two arguments are given,
	 * this treats the first as the option name, and the second as the option
	 * property value. If only one argument (or just the expected value) is
	 * given, this treats it as the property name, and simply tests if the option is
	 * avaiable in the project specific options.
	 */
	public static final String PROJECT_OPTION = "projectOption"; //$NON-NLS-1$
	

	/* (non-Javadoc)
	 * @see org.eclipse.core.expressions.IPropertyTester#test(java.lang.Object, java.lang.String, java.lang.Object[], java.lang.Object)
	 */
	public boolean test(Object receiver, String method, Object[] args, Object expectedValue) {
		if (!(receiver instanceof IJavaScriptElement)) {
			return false;
		}
		IJavaScriptElement res = (IJavaScriptElement) receiver;
		if (method.equals(NAME)) {
			return Pattern.matches(toString(expectedValue), res.getElementName());
		} else if (method.equals(IS_IN_JAVA_PROJECT)) {
			IJavaScriptProject javaProject= res.getJavaScriptProject();
			return javaProject != null && javaProject.exists() && javaProject.getProject().isOpen();
		} else if (method.equals(IS_IN_JAVA_PROJECT_WITH_NATURE)) {
			IJavaScriptProject javaProject= res.getJavaScriptProject();
			if (javaProject != null && javaProject.exists() && javaProject.getProject().isOpen() ) {
				if (expectedValue != null) {
					try {
						return javaProject.getProject().hasNature(toString(expectedValue));
					} catch (CoreException e) {
						return false;
					}
				}
			}
			return false;
		} else if (method.equals(IS_ON_CLASSPATH)) {
			IJavaScriptProject javaProject= res.getJavaScriptProject();
			if (javaProject != null && javaProject.exists()) {
				return javaProject.isOnIncludepath(res);
			}
			return false;
		} else if (method.equals(IN_SOURCE_FOLDER)) {
			IJavaScriptElement root= res.getAncestor(IJavaScriptElement.PACKAGE_FRAGMENT_ROOT);
			if (root != null) {
				try {
					return ((IPackageFragmentRoot) root).getKind() == IPackageFragmentRoot.K_SOURCE;
				} catch (JavaScriptModelException e) {
					// ignore
				}
			}
			return false;
		} else if (method.equals(IN_ARCHIVE)) {
			IJavaScriptElement root= res.getAncestor(IJavaScriptElement.PACKAGE_FRAGMENT_ROOT);
			if (root != null) {
				return ((IPackageFragmentRoot) root).isArchive();
			}
			return false;
		} else if (method.equals(IN_EXTERNAL_ARCHIVE)) {
			IJavaScriptElement root= res.getAncestor(IJavaScriptElement.PACKAGE_FRAGMENT_ROOT);
			if (root != null) {
				return ((IPackageFragmentRoot) root).isExternal();
			}
			return false;
		} else if (method.equals(PROJECT_OPTION)) {
			IJavaScriptProject project= res.getJavaScriptProject();
			if (project != null) {
				if (args.length == 2) {
					String current= project.getOption(toString(args[0]), true);
					return current != null && current.equals(args[1]);
				} else if (args.length == 1) {
					return project.getOption(toString(args[0]), false) != null;
				}
			}
			return false;
		} else if (method.equals(HAS_TYPE_ON_CLASSPATH)) {
			IJavaScriptProject javaProject= res.getJavaScriptProject();
			if (javaProject != null && javaProject.exists()) {
				try {
					return javaProject.findType(toString(expectedValue)) != null;
				} catch (JavaScriptModelException e) {
					return false;
				}
			}
		}
		return false;
	}

	private String toString(Object expectedValue) {
		return expectedValue == null ? "" : expectedValue.toString(); //$NON-NLS-1$
	}
}
